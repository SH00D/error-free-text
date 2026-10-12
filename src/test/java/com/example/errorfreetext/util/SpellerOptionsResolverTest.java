package com.example.errorfreetext.util;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class SpellerOptionsResolverTest {

    private final SpellerOptionsResolver resolver = new SpellerOptionsResolver();

    @ParameterizedTest(name = "[{index}] \"{0}\" -> {1}")
    @CsvSource(delimiter = '|', value = {
            "Просто текст без ничего       | 0",
            "У меня 2 кота                 | 2",
            "Смотри https://example.com    | 4",
            "Глава 3 на https://site.ru     | 6",
            "Сайт www.yandex без протокола | 4",
            "Зайди на google.com срочно    | 4",
            "Ссылка http://site/page и всё | 4",
            "Hello World                   | 0"
    })
    void resolvesOptions(String text, int expectedOptions) {
        assertThat(resolver.resolve(text)).isEqualTo(expectedOptions);
    }

    @ParameterizedTest
    @CsvSource({"Find repeat repeat words", "lowercase AND UPPERCASE"})
    void repeatWordsAndCapitalizationOptionsAreNeverEnabled(String text) {
        int options = resolver.resolve(text);

        assertThat(options & 8).isZero();
        assertThat(options & 512).isZero();
    }
}
