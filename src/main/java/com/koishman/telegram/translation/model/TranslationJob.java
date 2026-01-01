package com.koishman.telegram.translation.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Data
public class TranslationJob {
    private String jobId;
    private String fileName;
    private String filePath;
    private JobStatus status = JobStatus.PENDING;
    private Map<String, String> outputs = new HashMap<>();
    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private TranslationJobRequest request;

    public enum JobStatus {
        PENDING,
        PROCESSING,
        COMPLETED,
        FAILED
    }

    public long getDurationSeconds() {
        if (startedAt == null || completedAt == null) {
            return 0;
        }
        return java.time.Duration.between(startedAt, completedAt).getSeconds();
    }
}
