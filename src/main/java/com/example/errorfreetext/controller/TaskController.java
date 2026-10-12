package com.example.errorfreetext.controller;

import com.example.errorfreetext.dto.CreateTaskRequest;
import com.example.errorfreetext.dto.CreateTaskResponse;
import com.example.errorfreetext.dto.TaskResponse;
import com.example.errorfreetext.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping
    public ResponseEntity<CreateTaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request,
                                                         UriComponentsBuilder uriBuilder) {
        UUID id = taskService.createTask(request);
        URI location = uriBuilder.path("/tasks/{id}").buildAndExpand(id).toUri();
        return ResponseEntity.created(location).body(new CreateTaskResponse(id));
    }

    @GetMapping("/{id}")
    public TaskResponse getTask(@PathVariable UUID id) {
        return taskService.getTask(id);
    }
}
