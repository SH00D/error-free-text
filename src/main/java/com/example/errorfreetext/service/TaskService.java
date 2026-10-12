package com.example.errorfreetext.service;

import com.example.errorfreetext.dto.CreateTaskRequest;
import com.example.errorfreetext.dto.TaskResponse;
import com.example.errorfreetext.entity.Task;
import com.example.errorfreetext.exception.TaskNotFoundException;
import com.example.errorfreetext.repository.TaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskService.class);

    private final TaskRepository taskRepository;

    public TaskService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional
    public UUID createTask(CreateTaskRequest request) {
        Task task = taskRepository.save(Task.newTask(request.text(), request.language()));
        log.info("Создана задача {} (язык={}, длина={})", task.getId(), task.getLanguage(),
                task.getOriginalText().length());
        return task.getId();
    }

    @Transactional(readOnly = true)
    public TaskResponse getTask(UUID id) {
        return taskRepository.findById(id)
                .map(TaskResponse::from)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }
}
