package com.koishman.telegram.translation.service;

import com.koishman.telegram.translation.model.SubtitleSegment;
import com.koishman.telegram.translation.model.TranslationJob;
import com.koishman.telegram.translation.model.TranslationJobRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubtitleProcessingService {

    private final WhisperService whisperService;
    private final TranslationService translationService;
    private final FFmpegService ffmpegService;
    private final FileStorageService fileStorageService;
    private final JobTrackingService jobTrackingService;

    @Async
    public void processJob(String jobId) {
        log.info("Starting job processing: {}", jobId);

        try {
            TranslationJob job = jobTrackingService.getJob(jobId);
            if (job == null) {
                log.error("Job not found: {}", jobId);
                return;
            }

            jobTrackingService.updateJobStatus(jobId, TranslationJob.JobStatus.PROCESSING);

            File inputFile = new File(job.getFilePath());
            File outputDir = fileStorageService.createOutputDirectory(jobId);
            TranslationJobRequest request = job.getRequest();

            // Step 1: Extract audio
            log.info("Step 1: Extracting audio");
            File audioFile = new File(outputDir, "audio.wav");
            ffmpegService.extractAudio(inputFile, audioFile, request.getAudioTrack());

            // Step 2: Transcribe
            log.info("Step 2: Transcribing audio with {} backend, align={}",
                    request.getWhisperBackend(), request.isAlignOutput());
            List<SubtitleSegment> originalSegments = whisperService.transcribe(
                    audioFile,
                    request.getWhisperModel(),
                    request.getOriginalLanguage(),
                    request.getWhisperBackend() != null ? request.getWhisperBackend() : "faster-whisper",
                    request.isAlignOutput()
            );

            // Save original SRT
            String baseName = job.getFileName().replaceAll("\\.[^.]+$", "");
            File originalSrtFile = new File(outputDir, baseName + "_orig.srt");
            writeSRT(originalSegments, originalSrtFile);
            jobTrackingService.addJobOutput(jobId, "orig_srt", originalSrtFile.getName());

            // Step 3: Create original video with original subtitles (if requested)
            if ("hard".equals(request.getSubtitleBurnType()) || "both".equals(request.getSubtitleBurnType())) {
                log.info("Step 3: Burning original subtitles");
                File origVideoFile = new File(outputDir, baseName + "_orig.mp4");
                ffmpegService.burnSubtitles(inputFile, originalSrtFile, origVideoFile);
                jobTrackingService.addJobOutput(jobId, "orig", origVideoFile.getName());
            }

            // Step 4: Translate to each target language
            List<File> translatedSrtFiles = new ArrayList<>();
            List<String> languages = new ArrayList<>();

            for (String targetLang : request.getTargetLanguages()) {
                log.info("Step 4: Translating to {}", targetLang);

                List<SubtitleSegment> translatedSegments = new ArrayList<>();
                int index = 1;

                for (SubtitleSegment segment : originalSegments) {
                    String translatedText = translationService.translate(
                            segment.getText(),
                            request.getOriginalLanguage() != null ? request.getOriginalLanguage() : "en",
                            targetLang,
                            request.getTranslationModel() != null ? request.getTranslationModel() : "m2m100"
                    );

                    SubtitleSegment translatedSegment = new SubtitleSegment(
                            index++,
                            segment.getStartTime(),
                            segment.getEndTime(),
                            translatedText
                    );
                    translatedSegments.add(translatedSegment);
                }

                // Save translated SRT
                File translatedSrtFile = new File(outputDir, baseName + "_" + targetLang + ".srt");
                writeSRT(translatedSegments, translatedSrtFile);
                jobTrackingService.addJobOutput(jobId, targetLang + "_srt", translatedSrtFile.getName());

                translatedSrtFiles.add(translatedSrtFile);
                languages.add(targetLang);

                // Burn translated subtitles if requested
                if ("hard".equals(request.getSubtitleBurnType()) || "both".equals(request.getSubtitleBurnType())) {
                    log.info("Step 5: Burning {} subtitles", targetLang);
                    File translatedVideoFile = new File(outputDir, baseName + "_" + targetLang + ".mp4");
                    ffmpegService.burnSubtitles(inputFile, translatedSrtFile, translatedVideoFile);
                    jobTrackingService.addJobOutput(jobId, targetLang, translatedVideoFile.getName());
                }
            }

            // Step 6: Create soft subtitle version if requested
            if ("soft".equals(request.getSubtitleBurnType()) || "both".equals(request.getSubtitleBurnType())) {
                log.info("Step 6: Creating soft subtitle version");

                // Add original subtitle to the list
                translatedSrtFiles.add(0, originalSrtFile);
                languages.add(0, request.getOriginalLanguage() != null ? request.getOriginalLanguage() : "en");

                File softSubFile = new File(outputDir, baseName + "_multi_soft.mkv");
                ffmpegService.muxSoftSubtitles(inputFile, translatedSrtFiles, languages, softSubFile);
                jobTrackingService.addJobOutput(jobId, "multi_soft", softSubFile.getName());
            }

            // Cleanup
            audioFile.delete();

            // Mark job as completed
            jobTrackingService.updateJobStatus(jobId, TranslationJob.JobStatus.COMPLETED);
            log.info("Job completed successfully: {}", jobId);

        } catch (Exception e) {
            log.error("Job processing failed: {}", jobId, e);
            jobTrackingService.setJobError(jobId, e.getMessage());
        }
    }

    private void writeSRT(List<SubtitleSegment> segments, File outputFile) throws IOException {
        try (FileWriter writer = new FileWriter(outputFile)) {
            for (SubtitleSegment segment : segments) {
                writer.write(segment.toSRT());
                writer.write("\n");
            }
        }
        log.info("SRT file written: {}", outputFile.getAbsolutePath());
    }
}
