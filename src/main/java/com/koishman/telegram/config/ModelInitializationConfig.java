package com.koishman.telegram.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class ModelInitializationConfig {

    @Value("${translation.python.executable:python3}")
    private String pythonExecutable;

    @Value("${translation.models.auto-download:true}")
    private boolean autoDownload;

    @Value("${translation.models.whisper-models:large-v3}")
    private String whisperModels;

    @Value("${translation.models.whisper-backends:faster-whisper,openai-whisper}")
    private String whisperBackends;

    @Value("${translation.models.translation-models:m2m100,nllb}")
    private String translationModels;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        if (autoDownload) {
            log.info("=== MODEL INITIALIZATION STARTED ===");
            log.info("Application is ready. Starting background model downloads...");
            downloadModelsAsync();
        } else {
            log.info("Auto-download is disabled. Models will be downloaded on first use.");
        }
    }

    @Async
    public void downloadModelsAsync() {
        try {
            List<String> modelList = Arrays.asList(whisperModels.split(","));
            List<String> backendList = Arrays.asList(whisperBackends.split(","));
            List<String> translationModelList = Arrays.asList(translationModels.split(","));

            log.info("Downloading models: whisper={}, backends={}, translation={}",
                    modelList, backendList, translationModelList);

            // Download Whisper models
            for (String backend : backendList) {
                for (String model : modelList) {
                    downloadWhisperModel(backend.trim(), model.trim());
                }
            }

            // Download translation models
            for (String model : translationModelList) {
                downloadTranslationModel(model.trim());
            }

            log.info("=== MODEL INITIALIZATION COMPLETED ===");
            log.info("All models downloaded successfully!");

        } catch (Exception e) {
            log.error("Error during model initialization", e);
        }
    }

    private void downloadWhisperModel(String backend, String modelSize) {
        try {
            log.info("Downloading Whisper model: backend={}, size={}", backend, modelSize);

            String pythonScript;
            if ("openai-whisper".equals(backend)) {
                pythonScript = String.format(
                    "import whisper; " +
                    "print('Downloading OpenAI Whisper %s...'); " +
                    "whisper.load_model('%s'); " +
                    "print('✅ OpenAI Whisper %s downloaded')",
                    modelSize, modelSize, modelSize
                );
            } else {
                pythonScript = String.format(
                    "from faster_whisper import WhisperModel; " +
                    "print('Downloading Faster-Whisper %s...'); " +
                    "WhisperModel('%s', device='cpu', compute_type='int8'); " +
                    "print('✅ Faster-Whisper %s downloaded')",
                    modelSize, modelSize, modelSize
                );
            }

            List<String> command = Arrays.asList(pythonExecutable, "-c", pythonScript);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("  {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Failed to download {} {} (exit code: {})", backend, modelSize, exitCode);
            }

        } catch (Exception e) {
            log.error("Error downloading Whisper model: backend={}, size={}", backend, modelSize, e);
        }
    }

    private void downloadTranslationModel(String modelName) {
        try {
            log.info("Downloading translation model: {}", modelName);

            String modelId = modelName.equals("m2m100") 
                ? "facebook/m2m100_418M" 
                : "facebook/nllb-200-distilled-600M";

            String pythonScript = String.format(
                "from transformers import M2M100ForConditionalGeneration, M2M100Tokenizer; " +
                "print('Downloading %s...'); " +
                "M2M100ForConditionalGeneration.from_pretrained('%s'); " +
                "M2M100Tokenizer.from_pretrained('%s'); " +
                "print('✅ %s downloaded')",
                modelName, modelId, modelId, modelName
            );

            List<String> command = Arrays.asList(pythonExecutable, "-c", pythonScript);
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("  {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                log.error("Failed to download {} (exit code: {})", modelName, exitCode);
            }

        } catch (Exception e) {
            log.error("Error downloading translation model: {}", modelName, e);
        }
    }
}
