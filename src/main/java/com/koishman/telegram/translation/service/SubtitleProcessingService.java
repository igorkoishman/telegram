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
    private final com.koishman.telegram.service.TelegramApiClient telegramApiClient;

    @Async
    public void processJob(String jobId) {
        log.info("Starting job processing: {}", jobId);

        Integer progressMessageId = null;

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

            // Send initial progress message
            if (job.getChatId() != null) {
                String initialProgress = buildProgressMessage(job, 0, 0, 0, 0);
                progressMessageId = telegramApiClient.sendProgressMessage(job.getChatId(), initialProgress);
            }

            // Step 1: Extract audio
            log.info("Step 1: Extracting audio");
            if (job.getChatId() != null && progressMessageId != null) {
                telegramApiClient.updateProgressMessage(job.getChatId(), progressMessageId,
                        buildProgressMessage(job, 1, 0, 0, 0));
            }
            File audioFile = new File(outputDir, "audio.wav");
            ffmpegService.extractAudio(inputFile, audioFile, request.getAudioTrack());

            // Step 2: Transcribe
            log.info("Step 2: Transcribing audio with {} backend, align={}",
                    request.getWhisperBackend(), request.isAlignOutput());
            if (job.getChatId() != null && progressMessageId != null) {
                telegramApiClient.updateProgressMessage(job.getChatId(), progressMessageId,
                        buildProgressMessage(job, 2, 0, 0, 0));
            }
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
                if (job.getChatId() != null && progressMessageId != null) {
                    telegramApiClient.updateProgressMessage(job.getChatId(), progressMessageId,
                            buildProgressMessage(job, 3, 0, 0, 0));
                }
                File origVideoFile = new File(outputDir, baseName + "_orig.mp4");
                ffmpegService.burnSubtitles(inputFile, originalSrtFile, origVideoFile);
                jobTrackingService.addJobOutput(jobId, "orig", origVideoFile.getName());
            }

            // Step 4: Translate to each target language
            List<File> translatedSrtFiles = new ArrayList<>();
            List<String> languages = new ArrayList<>();

            int totalLanguages = request.getTargetLanguages().size();
            int currentLangIndex = 0;

            for (String targetLang : request.getTargetLanguages()) {
                currentLangIndex++;
                log.info("Step 4: Translating to {}", targetLang);
                if (job.getChatId() != null && progressMessageId != null) {
                    telegramApiClient.updateProgressMessage(job.getChatId(), progressMessageId,
                            buildProgressMessage(job, 4, currentLangIndex, totalLanguages, 0));
                }

                List<SubtitleSegment> translatedSegments = new ArrayList<>();
                int index = 1;
                int totalSegments = originalSegments.size();

                for (SubtitleSegment segment : originalSegments) {
                    // Update progress for every 10 segments
                    if (job.getChatId() != null && progressMessageId != null && index % 10 == 0) {
                        telegramApiClient.updateProgressMessage(job.getChatId(), progressMessageId,
                                buildProgressMessage(job, 4, currentLangIndex, totalLanguages,
                                        (int)((double)index / totalSegments * 100)));
                    }

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
                    if (job.getChatId() != null && progressMessageId != null) {
                        telegramApiClient.updateProgressMessage(job.getChatId(), progressMessageId,
                                buildProgressMessage(job, 5, currentLangIndex, totalLanguages, 0));
                    }
                    File translatedVideoFile = new File(outputDir, baseName + "_" + targetLang + ".mp4");
                    ffmpegService.burnSubtitles(inputFile, translatedSrtFile, translatedVideoFile);
                    jobTrackingService.addJobOutput(jobId, targetLang, translatedVideoFile.getName());
                }
            }

            // Step 6: Create soft subtitle version if requested
            if ("soft".equals(request.getSubtitleBurnType()) || "both".equals(request.getSubtitleBurnType())) {
                log.info("Step 6: Creating soft subtitle version");
                if (job.getChatId() != null && progressMessageId != null) {
                    telegramApiClient.updateProgressMessage(job.getChatId(), progressMessageId,
                            buildProgressMessage(job, 6, 0, 0, 0));
                }

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

            // Send final completion message
            if (job.getChatId() != null && progressMessageId != null) {
                telegramApiClient.updateProgressMessage(job.getChatId(), progressMessageId,
                        buildCompletionMessage(job));
            }

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

    private String buildProgressMessage(TranslationJob job, int currentStep, int currentLang, int totalLangs, int segmentProgress) {
        String[] stepNames = {
            "Initializing",
            "Extracting Audio",
            "Transcribing Speech",
            "Burning Original Subtitles",
            "Translating Subtitles",
            "Burning Translated Subtitles",
            "Creating Multi-Language Video"
        };

        String[] stepEmojis = {
            "\uD83D\uDD04", // 🔄 Initializing
            "\uD83C\uDFA7", // 🎧 Extracting Audio
            "\uD83C\uDF99\uFE0F", // 🎙️ Transcribing
            "\uD83C\uDFA5", // 🎥 Burning Original
            "\uD83C\uDF0D", // 🌍 Translating
            "\uD83D\uDD25", // 🔥 Burning Translated
            "\uD83C\uDFAC"  // 🎬 Creating Multi-Language
        };

        StringBuilder message = new StringBuilder();
        message.append("\uD83C\uDFA5 <b>Video Processing Progress</b>\n\n");
        message.append("\uD83D\uDCC1 File: ").append(job.getFileName()).append("\n\n");

        // Build checklist
        for (int i = 0; i <= 6; i++) {
            if (i < currentStep) {
                message.append("✅ ").append(stepEmojis[i]).append(" ").append(stepNames[i]).append("\n");
            } else if (i == currentStep) {
                message.append("▶️ ").append(stepEmojis[i]).append(" <b>").append(stepNames[i]).append("</b>\n");

                // Add sub-progress for translation
                if (i == 4 && totalLangs > 0) {
                    String langName = getLanguageName(job.getRequest().getTargetLanguages().get(currentLang - 1));
                    message.append("   \uD83D\uDD39 Language ").append(currentLang).append("/").append(totalLangs)
                           .append(": ").append(langName).append("\n");

                    if (segmentProgress > 0) {
                        message.append("   ").append(buildProgressBar(segmentProgress)).append(" ")
                               .append(segmentProgress).append("%\n");
                    }
                } else if (i == 5 && totalLangs > 0) {
                    String langName = getLanguageName(job.getRequest().getTargetLanguages().get(currentLang - 1));
                    message.append("   \uD83D\uDD39 Language ").append(currentLang).append("/").append(totalLangs)
                           .append(": ").append(langName).append("\n");
                }
            } else {
                message.append("⏳ ").append(stepEmojis[i]).append(" ").append(stepNames[i]).append("\n");
            }
        }

        // Overall progress bar
        int overallProgress = calculateOverallProgress(currentStep, currentLang, totalLangs, segmentProgress);
        message.append("\n").append(buildProgressBar(overallProgress)).append(" <b>")
               .append(overallProgress).append("%</b>");

        return message.toString();
    }

    private String buildCompletionMessage(TranslationJob job) {
        StringBuilder message = new StringBuilder();
        message.append("✅ <b>Processing Complete!</b>\n\n");
        message.append("\uD83C\uDFC6 <b>").append(job.getFileName()).append("</b>\n\n");

        message.append("✅ \uD83C\uDFA7 Audio Extracted\n");
        message.append("✅ \uD83C\uDF99\uFE0F Speech Transcribed\n");
        message.append("✅ \uD83C\uDFA5 Original Subtitles Burned\n");

        for (String lang : job.getRequest().getTargetLanguages()) {
            String langName = getLanguageName(lang);
            message.append("✅ \uD83C\uDF0D Translated to ").append(langName).append("\n");
            message.append("✅ \uD83D\uDD25 ").append(langName).append(" Subtitles Burned\n");
        }

        message.append("\n\uD83C\uDF89 Your videos are being sent now!");

        return message.toString();
    }

    private String buildProgressBar(int percentage) {
        int filledBlocks = percentage / 10;
        int emptyBlocks = 10 - filledBlocks;

        StringBuilder bar = new StringBuilder();
        for (int i = 0; i < filledBlocks; i++) {
            bar.append("█");
        }
        for (int i = 0; i < emptyBlocks; i++) {
            bar.append("░");
        }

        return bar.toString();
    }

    private int calculateOverallProgress(int currentStep, int currentLang, int totalLangs, int segmentProgress) {
        if (totalLangs == 0) totalLangs = 1;

        // Weight each major step
        // Steps: 0=Init(5%), 1=Audio(10%), 2=Transcribe(20%), 3=BurnOrig(10%),
        //        4=Translate(30%), 5=BurnTranslated(20%), 6=CreateMulti(5%)

        int progress = 0;

        if (currentStep >= 1) progress += 5;  // Init done
        if (currentStep >= 2) progress += 10; // Audio extraction done
        if (currentStep >= 3) progress += 20; // Transcription done
        if (currentStep >= 4) progress += 10; // Original subtitles done

        if (currentStep == 4) {
            // Translation in progress
            int translationBaseProgress = 40;
            int translationStepWeight = 30 / totalLangs;
            int completedLangs = currentLang - 1;
            progress += translationStepWeight * completedLangs;

            // Add current segment progress
            if (segmentProgress > 0) {
                progress += (translationStepWeight * segmentProgress) / 100;
            }
        } else if (currentStep >= 5) {
            progress += 30; // All translations done
        }

        if (currentStep == 5) {
            // Burning subtitles
            int burnBaseProgress = 70;
            int burnStepWeight = 20 / totalLangs;
            int completedBurns = currentLang - 1;
            progress += burnStepWeight * completedBurns;
        } else if (currentStep >= 6) {
            progress += 20; // All burning done
        }

        if (currentStep >= 6) progress += 5; // Multi-language video creation

        return Math.min(100, progress);
    }

    private String getLanguageName(String langCode) {
        switch (langCode.toLowerCase()) {
            case "en": return "English";
            case "es": return "Spanish";
            case "fr": return "French";
            case "de": return "German";
            case "it": return "Italian";
            case "ru": return "Russian";
            case "he": return "Hebrew";
            case "ar": return "Arabic";
            case "zh": return "Chinese";
            case "ja": return "Japanese";
            case "ko": return "Korean";
            case "pt": return "Portuguese";
            default: return langCode.toUpperCase();
        }
    }
}
