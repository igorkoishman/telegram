package com.koishman.telegram.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class JobResponse {
    @JsonProperty("job_id")
    private String jobId;
}
