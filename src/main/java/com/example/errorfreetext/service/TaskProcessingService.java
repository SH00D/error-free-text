package com.example.errorfreetext.service;

import com.example.errorfreetext.entity.Task;
import com.example.errorfreetext.exception.SpellerException;
import com.example.errorfreetext.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class TaskProcessingService {

    private static final Logger log = LoggerFactory.getLogger(TaskProcessingService.class);

    private final TaskRepository taskRepository;
    private final TextCorrectionService textCorrectionService;

    public TaskProcessingService(TaskRepository taskRepository, TextCorrectionService textCorrectionService) {
        this.taskRepository = taskRepository;
        this.textCorrectionService = textCorrectionService;
    }

    @Transactional
    public List<UUID> claimNewTasks(int batchSize) {
        List<Task> tasks = taskRepository.findNewTasksForUpdate(batchSize);
        tasks.forEach(Task::markInProgress);
        return tasks.stream().map(Task::getId).toList();
    }

    public void process(UUID taskId) {
        Task task = taskRepository.findById(taskId).orElse(null);
        if (task == null) {
            log.warn("Задача {} исчезла до обработки", taskId);
            return;
        }
        log.info("Обработка задачи {}", taskId);
        try {
            String correctedText = textCorrectionService.correct(task.getOriginalText(), task.getLanguage());
            task.complete(correctedText);
            log.info("Задача {} завершена", taskId);
        } catch (SpellerException e) {
            task.fail(e.getMessage());
            log.error("Задача {} завершилась ошибкой: {}", taskId, e.getMessage());
        } catch (RuntimeException e) {
            task.fail("Неожиданная ошибка при корректировке текста");
            log.error("Задача {} завершилась неожиданной ошибкой", taskId, e);
        }
        taskRepository.save(task);
    }

    @Transactional
    @EventListener(ApplicationReadyEvent.class)
    public void recoverInterruptedTasks() {
        int count = taskRepository.resetInProgressTasks();
        if (count > 0) {
            log.info("Возвращено {} прерванных задач в очередь", count);
        }
    }
}
