package com.example.errorfreetext.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

    @Id
    private UUID id;

    @Column(name = "original_text", nullable = false)
    private String originalText;

    @Column(name = "corrected_text")
    private String correctedText;

    @Enumerated(EnumType.STRING)
    @Column(name = "language", nullable = false, length = 2)
    private Language language;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private TaskStatus status;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Task() {
    }

    public static Task newTask(String originalText, Language language) {
        Task task = new Task();
        task.id = UUID.randomUUID();
        task.originalText = originalText;
        task.language = language;
        task.status = TaskStatus.NEW;
        return task;
    }

    public void markInProgress() {
        this.status = TaskStatus.IN_PROGRESS;
    }

    public void complete(String correctedText) {
        this.correctedText = correctedText;
        this.errorMessage = null;
        this.status = TaskStatus.COMPLETED;
    }

    public void fail(String errorMessage) {
        this.errorMessage = errorMessage;
        this.status = TaskStatus.ERROR;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public String getOriginalText() {
        return originalText;
    }

    public String getCorrectedText() {
        return correctedText;
    }

    public Language getLanguage() {
        return language;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
