package com.koishman.telegram.translation.controller;

import com.koishman.telegram.translation.model.MediaTrackInfo;
import com.koishman.telegram.translation.model.TranslationJob;
import com.koishman.telegram.translation.model.TranslationJobRequest;
import com.koishman.telegram.translation.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/translation")
@RequiredArgsConstructor
public class TranslationController {

    private final FileStorageService fileStorageService;
    private final JobTrackingService jobTrackingService;
    private final SubtitleProcessingService subtitleProcessingService;
    private final FFmpegService ffmpegService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadVideo(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "langs", required = false) String langs,
            @RequestParam(value = "model", defaultValue = "large") String model,
            @RequestParam(value = "model_type", defaultValue = "faster-whisper") String modelType,
            @RequestParam(value = "whisper_backend", defaultValue = "faster-whisper") String whisperBackend,
            @RequestParam(value = "translation_model", defaultValue = "m2m100") String translationModel,
            @RequestParam(value = "subtitle_burn_type", defaultValue = "hard") String subtitleBurnType,
            @RequestParam(value = "use_subtitles_only", defaultValue = "false") Boolean useSubtitlesOnly,
            @RequestParam(value = "original_lang", required = false) String originalLang,
            @RequestParam(value = "audio_track", required = false) Integer audioTrack,
            @RequestParam(value = "subtitle_track", required = false) Integer subtitleTrack,
            @RequestParam(value = "align_output", defaultValue = "true") Boolean alignOutput
    ) {
        try {
            log.info("Received upload request: file={}, langs={}, model={}", file.getOriginalFilename(), langs, model);

            // Store uploaded file
            File uploadedFile = fileStorageService.storeUploadedFile(file);

            // Create job
            TranslationJob job = jobTrackingService.createJob(file.getOriginalFilename(), uploadedFile.getAbsolutePath());

            // Prepare request
            TranslationJobRequest request = new TranslationJobRequest();
            if (langs != null && !langs.isEmpty()) {
                request.setTargetLanguages(Arrays.asList(langs.split("\\s+")));
            }
            request.setWhisperModel(model);
            request.setWhisperModelType(modelType);
            request.setWhisperBackend(whisperBackend);
            request.setTranslationModel(translationModel);
            request.setSubtitleBurnType(subtitleBurnType);
            request.setUseSubtitlesOnly(useSubtitlesOnly);
            request.setOriginalLanguage(originalLang);
            request.setAudioTrack(audioTrack);
            request.setSubtitleTrack(subtitleTrack);
            request.setAlignOutput(alignOutput);

            job.setRequest(request);

            // Start processing asynchronously
            subtitleProcessingService.processJob(job.getJobId());

            Map<String, String> response = new HashMap<>();
            response.put("job_id", job.getJobId());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Upload failed", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/status/{jobId}")
    public ResponseEntity<?> getJobStatus(@PathVariable String jobId) {
        try {
            TranslationJob job = jobTrackingService.getJob(jobId);

            if (job == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Job not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("status", job.getStatus() == TranslationJob.JobStatus.COMPLETED ? "done" : "processing");
            response.put("outputs", job.getOutputs());
            response.put("duration_seconds", String.valueOf(job.getDurationSeconds()));

            if (job.getStatus() == TranslationJob.JobStatus.FAILED) {
                response.put("error", job.getErrorMessage());
            }

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to get job status", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @GetMapping("/download/{filename}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename, @RequestParam(required = false) String jobId) {
        try {
            // Extract job ID from filename if not provided
            if (jobId == null && filename.contains("/")) {
                String[] parts = filename.split("/", 2);
                jobId = parts[0];
                filename = parts[1];
            }

            File file;
            if (jobId != null) {
                file = fileStorageService.getOutputFile(jobId, filename);
            } else {
                // Try to find in all job directories
                file = findFileInOutputs(filename);
            }

            if (file == null || !file.exists()) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new FileSystemResource(file);

            String contentType = determineContentType(filename);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);

        } catch (Exception e) {
            log.error("Download failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyzeMedia(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Analyzing media file: {}", file.getOriginalFilename());

            // Store file temporarily
            File uploadedFile = fileStorageService.storeUploadedFile(file);

            // Analyze tracks
            List<MediaTrackInfo> tracks = ffmpegService.analyzeMedia(uploadedFile);

            Map<String, Object> response = new HashMap<>();
            response.put("tracks", tracks);

            // Clean up temporary file
            uploadedFile.delete();

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Media analysis failed", e);
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    private File findFileInOutputs(String filename) {
        File outputDir = new File(fileStorageService.getOutputDir());
        if (!outputDir.exists() || !outputDir.isDirectory()) {
            return null;
        }

        for (File jobDir : outputDir.listFiles()) {
            if (jobDir.isDirectory()) {
                File targetFile = new File(jobDir, filename);
                if (targetFile.exists()) {
                    return targetFile;
                }
            }
        }

        return null;
    }

    private String determineContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".mp4")) {
            return "video/mp4";
        } else if (lower.endsWith(".mkv")) {
            return "video/x-matroska";
        } else if (lower.endsWith(".srt")) {
            return "text/plain";
        } else {
            return "application/octet-stream";
        }
    }
}
