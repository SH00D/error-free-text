package com.example.errorfreetext.service;

import com.example.errorfreetext.entity.Language;
import com.example.errorfreetext.entity.Task;
import com.example.errorfreetext.entity.TaskStatus;
import com.example.errorfreetext.exception.SpellerException;
import com.example.errorfreetext.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskProcessingServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TextCorrectionService textCorrectionService;

    @InjectMocks
    private TaskProcessingService processingService;

    @Test
    void claimNewTasksMarksThemInProgress() {
        Task first = Task.newTask("first", Language.EN);
        Task second = Task.newTask("second", Language.EN);
        when(taskRepository.findNewTasksForUpdate(10)).thenReturn(List.of(first, second));

        List<UUID> ids = processingService.claimNewTasks(10);

        assertThat(ids).containsExactly(first.getId(), second.getId());
        assertThat(first.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(second.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    @Test
    void processCompletesTaskWithCorrectedText() {
        Task task = Task.newTask("helo", Language.EN);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(textCorrectionService.correct("helo", Language.EN)).thenReturn("hello");

        processingService.process(task.getId());

        assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        assertThat(task.getCorrectedText()).isEqualTo("hello");
        verify(taskRepository).save(task);
    }

    @Test
    void processMarksTaskAsErrorWhenSpellerFails() {
        Task task = Task.newTask("helo", Language.EN);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(textCorrectionService.correct("helo", Language.EN))
                .thenThrow(new SpellerException("Ошибка запроса к API Яндекс.Спеллера: 503"));

        processingService.process(task.getId());

        assertThat(task.getStatus()).isEqualTo(TaskStatus.ERROR);
        assertThat(task.getErrorMessage()).isEqualTo("Ошибка запроса к API Яндекс.Спеллера: 503");
        assertThat(task.getCorrectedText()).isNull();
        verify(taskRepository).save(task);
    }

    @Test
    void processMarksTaskAsErrorOnUnexpectedException() {
        Task task = Task.newTask("helo", Language.EN);
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));
        when(textCorrectionService.correct("helo", Language.EN)).thenThrow(new IllegalStateException("boom"));

        processingService.process(task.getId());

        assertThat(task.getStatus()).isEqualTo(TaskStatus.ERROR);
        verify(taskRepository).save(task);
    }

    @Test
    void processSkipsMissingTask() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        processingService.process(id);

        verify(taskRepository, never()).save(any());
    }
}
