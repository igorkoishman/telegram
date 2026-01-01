package com.koishman.telegram.translation.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Slf4j
@Service
public class FileStorageService {

    @Value("${translation.storage.upload-dir:./uploads}")
    private String uploadDir;

    @Value("${translation.storage.output-dir:./outputs}")
    private String outputDir;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            Files.createDirectories(Paths.get(outputDir));
            log.info("Storage directories initialized: upload={}, output={}", uploadDir, outputDir);
        } catch (IOException e) {
            log.error("Failed to create storage directories", e);
            throw new RuntimeException("Could not initialize storage", e);
        }
    }

    public File storeUploadedFile(MultipartFile file) throws IOException {
        String originalFilename = file.getOriginalFilename();
        String safeFilename = UUID.randomUUID() + "_" + originalFilename;
        Path targetPath = Paths.get(uploadDir, safeFilename);

        file.transferTo(targetPath);
        log.info("File uploaded: {}", targetPath);

        return targetPath.toFile();
    }

    public File getOutputFile(String jobId, String filename) {
        Path outputPath = Paths.get(outputDir, jobId, filename);
        return outputPath.toFile();
    }

    public File createOutputDirectory(String jobId) throws IOException {
        Path jobOutputDir = Paths.get(outputDir, jobId);
        Files.createDirectories(jobOutputDir);
        return jobOutputDir.toFile();
    }

    public File getUploadFile(String filename) {
        return Paths.get(uploadDir, filename).toFile();
    }

    public void deleteJobFiles(String jobId) {
        try {
            Path jobOutputDir = Paths.get(outputDir, jobId);
            if (Files.exists(jobOutputDir)) {
                FileUtils.deleteDirectory(jobOutputDir.toFile());
                log.info("Deleted job files for: {}", jobId);
            }
        } catch (IOException e) {
            log.error("Failed to delete job files for: {}", jobId, e);
        }
    }

    public String getOutputDir() {
        return outputDir;
    }

    public String getUploadDir() {
        return uploadDir;
    }
}
