package com.example.errorfreetext.dto;

import com.example.errorfreetext.entity.Language;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTaskRequest(
        @NotBlank(message = "текст не может быть пустым")
        @Size(min = 3, message = "текст должен содержать минимум 3 символа")
        @Pattern(regexp = "(?s).*\\p{L}.*", message = "текст должен содержать хотя бы одну букву")
        String text,

        @NotNull(message = "язык должен быть указан (EN или RU)")
        Language language) {
}
