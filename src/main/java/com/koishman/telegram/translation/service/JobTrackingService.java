package com.koishman.telegram.translation.service;

import com.koishman.telegram.translation.model.TranslationJob;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class JobTrackingService {

    private final Map<String, TranslationJob> jobs = new ConcurrentHashMap<>();

    public TranslationJob createJob(String fileName, String filePath) {
        TranslationJob job = new TranslationJob();
        job.setJobId(UUID.randomUUID().toString());
        job.setFileName(fileName);
        job.setFilePath(filePath);
        job.setStatus(TranslationJob.JobStatus.PENDING);

        jobs.put(job.getJobId(), job);
        log.info("Created job: {}", job.getJobId());

        return job;
    }

    public TranslationJob getJob(String jobId) {
        return jobs.get(jobId);
    }

    public void updateJobStatus(String jobId, TranslationJob.JobStatus status) {
        TranslationJob job = jobs.get(jobId);
        if (job != null) {
            job.setStatus(status);
            if (status == TranslationJob.JobStatus.PROCESSING && job.getStartedAt() == null) {
                job.setStartedAt(java.time.LocalDateTime.now());
            } else if (status == TranslationJob.JobStatus.COMPLETED || status == TranslationJob.JobStatus.FAILED) {
                job.setCompletedAt(java.time.LocalDateTime.now());
            }
            log.info("Job {} status updated to: {}", jobId, status);
        }
    }

    public void addJobOutput(String jobId, String key, String filename) {
        TranslationJob job = jobs.get(jobId);
        if (job != null) {
            job.getOutputs().put(key, filename);
            log.debug("Added output to job {}: {} -> {}", jobId, key, filename);
        }
    }

    public void setJobError(String jobId, String errorMessage) {
        TranslationJob job = jobs.get(jobId);
        if (job != null) {
            job.setStatus(TranslationJob.JobStatus.FAILED);
            job.setErrorMessage(errorMessage);
            job.setCompletedAt(java.time.LocalDateTime.now());
            log.error("Job {} failed: {}", jobId, errorMessage);
        }
    }

    public void deleteJob(String jobId) {
        jobs.remove(jobId);
        log.info("Deleted job: {}", jobId);
    }
}
