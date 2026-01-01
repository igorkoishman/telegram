package com.koishman.telegram.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Map;

@Data
public class JobStatus {
    private String status;
    private Map<String, String> outputs;

    @JsonProperty("duration_seconds")
    private String durationSeconds;

    private String error;
}
