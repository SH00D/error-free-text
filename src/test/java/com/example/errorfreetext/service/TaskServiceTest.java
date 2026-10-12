package com.example.errorfreetext.service;

import com.example.errorfreetext.dto.CreateTaskRequest;
import com.example.errorfreetext.dto.TaskResponse;
import com.example.errorfreetext.entity.Language;
import com.example.errorfreetext.entity.Task;
import com.example.errorfreetext.entity.TaskStatus;
import com.example.errorfreetext.exception.TaskNotFoundException;
import com.example.errorfreetext.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void createTaskSavesNewTaskAndReturnsId() {
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UUID id = taskService.createTask(new CreateTaskRequest("Превет мир", Language.RU));

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task saved = captor.getValue();
        assertThat(saved.getId()).isEqualTo(id);
        assertThat(saved.getStatus()).isEqualTo(TaskStatus.NEW);
        assertThat(saved.getOriginalText()).isEqualTo("Превет мир");
        assertThat(saved.getLanguage()).isEqualTo(Language.RU);
    }

    @Test
    void getTaskReturnsCorrectedTextForCompletedTask() {
        Task task = Task.newTask("helo", Language.EN);
        task.complete("hello");
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        TaskResponse response = taskService.getTask(task.getId());

        assertThat(response).isEqualTo(new TaskResponse(TaskStatus.COMPLETED, "hello", null));
    }

    @Test
    void getTaskReturnsOnlyStatusForTaskInProgress() {
        Task task = Task.newTask("helo", Language.EN);
        task.markInProgress();
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        assertThat(taskService.getTask(task.getId()))
                .isEqualTo(new TaskResponse(TaskStatus.IN_PROGRESS, null, null));
    }

    @Test
    void getTaskReturnsErrorMessageForFailedTask() {
        Task task = Task.newTask("helo", Language.EN);
        task.fail("API error");
        when(taskRepository.findById(task.getId())).thenReturn(Optional.of(task));

        assertThat(taskService.getTask(task.getId()))
                .isEqualTo(new TaskResponse(TaskStatus.ERROR, null, "API error"));
    }

    @Test
    void getTaskThrowsWhenTaskNotFound() {
        UUID id = UUID.randomUUID();
        when(taskRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTask(id))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessage("Задача с id: " + id + " не найдена");
    }
}
