# Error Free Text

REST-сервис автоматической корректировки текста. Принимает текст, сохраняет задачу в PostgreSQL,
планировщик в фоне исправляет опечатки через [Яндекс.Спеллер](https://yandex.ru/dev/speller/doc/ru/reference/checkTexts)
и сохраняет результат.

## Стек

Java 17, Spring Boot 3, Gradle, PostgreSQL, Flyway, Docker / docker-compose, JUnit 5, Mockito, Testcontainers, WireMock.

## Запуск

### Всё в Docker

```bash
docker compose up --build
```

Приложение: `http://localhost:8080`, PostgreSQL: `localhost:5432` (`postgres` / `postgres`, БД `error_free_text`).

### Локально (БД в Docker, приложение из IDE)

```bash
docker compose up -d postgres
./gradlew bootRun
```

### Тесты

```bash
./gradlew test
```

Интеграционный тест (`TaskFlowIntegrationTest`) поднимает PostgreSQL в Testcontainers и заглушку Яндекс API
на WireMock. Если Docker недоступен, он пропускается.

## База данных

- Docker-контейнер `postgres` при первом старте сам создаёт базу `error_free_text`
  (переменные `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD`). Данные хранятся в volume `postgres-data`.
- Таблицы создаёт **Flyway** при старте приложения из миграций в `src/main/resources/db/migration`.
  Hibernate схему не генерирует (`ddl-auto: none`).
- Чтобы изменить схему, добавьте новую миграцию `V2__....sql`, существующие не редактируются.

Таблица `tasks`:

| Колонка          | Тип         | Описание                                   |
|------------------|-------------|--------------------------------------------|
| `id`             | UUID        | идентификатор задачи                       |
| `original_text`  | TEXT        | исходный текст                             |
| `corrected_text` | TEXT        | исправленный текст                         |
| `language`       | VARCHAR(2)  | `EN` / `RU`                                |
| `status`         | VARCHAR(20) | `NEW`, `IN_PROGRESS`, `COMPLETED`, `ERROR` |
| `error_message`  | TEXT        | описание ошибки                            |
| `created_at`     | TIMESTAMPTZ | время создания                             |
| `updated_at`     | TIMESTAMPTZ | время последнего изменения                 |

Сбросить БД полностью: `docker compose down -v`.

## API

### Создать задачу

```bash
curl -i -X POST http://localhost:8080/tasks \
  -H "Content-Type: application/json" \
  -d '{"text": "Превет, как дила?", "language": "RU"}'
```

```
HTTP/1.1 201 Created
Location: http://localhost:8080/tasks/3d32a602-633a-47e2-9bea-0a7a467e9322

{"id": "3d32a602-633a-47e2-9bea-0a7a467e9322"}
```

Валидация:
- `language`: только `EN` или `RU`;
- `text`: минимум 3 символа, должен содержать хотя бы одну букву (не только цифры и спецсимволы).

### Получить результат

```bash
curl http://localhost:8080/tasks/3d32a602-633a-47e2-9bea-0a7a467e9322
```

| Состояние задачи | Ответ |
|---|---|
| завершена | `{"status": "COMPLETED", "correctedText": "Привет, как дела?"}` |
| в процессе | `{"status": "NEW"}` или `{"status": "IN_PROGRESS"}` |
| ошибка | `{"status": "ERROR", "errorMessage": "Ошибка запроса к API Яндекс.Спеллера: ..."}` |

### Формат ошибок

```json
{
  "errorMessage": "Задача с id: 44bd78dc-d08c-41c6-b87d-fb82046bd470 не найдена",
  "errorCode": 40401,
  "timestamp": "2025-08-09T18:31:42.201",
  "path": "/tasks/44bd78dc-d08c-41c6-b87d-fb82046bd470"
}
```

| errorCode | HTTP | Причина |
|---|---|---|
| 40001 | 400 | ошибка валидации (текст, язык) |
| 40002 | 400 | некорректный JSON |
| 40003 | 400 | некорректный параметр пути (например, не UUID) |
| 40401 | 404 | задача не найдена |
| 40402 | 404 | ресурс не найден |
| 40501 | 405 | метод не поддерживается |
| 41501 | 415 | неподдерживаемый Content-Type |
| 50001 | 500 | внутренняя ошибка |

## Веб-интерфейс

Для ручной проверки есть простая страница (обычный HTML + JavaScript, без сборки):
откройте `http://localhost:8080/` после запуска приложения. Она отправляет текст на
`POST /tasks` и опрашивает `GET /tasks/{id}` до готовности.

## Как работает корректировка

1. `TaskProcessingScheduler` с периодом `app.scheduler.delay-ms` берёт пачку задач `NEW`
   (`SELECT ... FOR UPDATE SKIP LOCKED`, чтобы несколько инстансов не взяли одну задачу) и переводит их в `IN_PROGRESS`.
2. `SpellerOptionsResolver` определяет опции: есть цифры → `IGNORE_DIGITS`, есть URL → `IGNORE_URLS`.
   `FIND_REPEAT_WORDS` и `IGNORE_CAPITALIZATION` всегда выключены.
3. `TextSplitter` режет текст на части ≤ 10 000 символов по пробельным символам, не разрывая слова.
4. Каждая часть отправляется в `checkTexts`. `CorrectionApplier` заменяет слова на первый вариант,
   двигаясь с конца, чтобы не сбивать позиции.
5. Результат сохраняется со статусом `COMPLETED`. При ошибке API задача получает статус `ERROR`
   с описанием, обработка прекращается.
6. При старте приложения задачи, «зависшие» в `IN_PROGRESS` (например, после падения), возвращаются в `NEW`.

## Конфигурация

| Переменная | По умолчанию | Описание |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/error_free_text` | URL БД |
| `DB_USERNAME` / `DB_PASSWORD` | `postgres` / `postgres` | доступ к БД |
| `SPELLER_URL` | `https://speller.yandex.net/services/spellservice.json` | адрес Яндекс.Спеллера |
| `SCHEDULER_DELAY_MS` | `10000` | пауза между запусками планировщика |
| `SCHEDULER_BATCH_SIZE` | `10` | задач за один запуск |

## Структура

```
controller   REST-контроллер
dto          запросы и ответы API
entity       сущность Task, статусы, языки
repository   Spring Data JPA
service      создание задач, обработка, корректировка текста
scheduler    планировщик
client       клиент Яндекс.Спеллера
util         разбиение текста, выбор опций, применение исправлений
exception    исключения и глобальный обработчик ошибок
config       конфигурация и свойства
static       простая веб-страница для ручной проверки
```
