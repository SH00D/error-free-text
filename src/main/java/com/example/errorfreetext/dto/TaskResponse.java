package com.example.errorfreetext.dto;

import com.example.errorfreetext.entity.Task;
import com.example.errorfreetext.entity.TaskStatus;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TaskResponse(TaskStatus status, String correctedText, String errorMessage) {

    public static TaskResponse from(Task task) {
        TaskStatus status = task.getStatus();
        if (status == TaskStatus.COMPLETED) {
            return new TaskResponse(status, task.getCorrectedText(), null);
        }
        if (status == TaskStatus.ERROR) {
            return new TaskResponse(status, null, task.getErrorMessage());
        }
        return new TaskResponse(status, null, null);
    }
}
