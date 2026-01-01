package com.koishman.telegram.translation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koishman.telegram.translation.model.SubtitleSegment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class WhisperService {

    private final ObjectMapper objectMapper;

    @Value("${translation.python.executable:python3}")
    private String pythonExecutable;

    @Value("${translation.python.scripts-dir:classpath:python}")
    private String scriptsDir;

    public List<SubtitleSegment> transcribe(File audioFile, String modelSize, String language) {
        return transcribe(audioFile, modelSize, language, "faster-whisper", true);
    }

    public List<SubtitleSegment> transcribe(File audioFile, String modelSize, String language,
                                           String backend, boolean alignOutput) {
        try {
            // Select script based on backend
            String scriptName = backend.equals("openai-whisper")
                ? "openai_whisper_transcribe.py"
                : "whisper_transcribe.py";
            String scriptPath = getScriptPath(scriptName);

            List<String> command = new ArrayList<>();
            command.add(pythonExecutable);
            command.add(scriptPath);
            command.add(audioFile.getAbsolutePath());
            command.add("--model");
            command.add(modelSize);

            if (language != null && !language.isEmpty()) {
                command.add("--language");
                command.add(language);
            }

            if (alignOutput) {
                command.add("--align");
            }

            log.info("Starting Whisper transcription: backend={}, model={}, language={}, align={}",
                    backend, modelSize, language, alignOutput);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            StringBuilder jsonOutput = new StringBuilder();
            String lastJsonLine = null;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // Log all output for debugging
                    log.debug("Whisper output: {}", line);

                    // Capture only JSON lines (starting with '{' or '[')
                    if (line.trim().startsWith("{") || line.trim().startsWith("[")) {
                        lastJsonLine = line;
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Whisper transcription failed with exit code: " + exitCode);
            }

            if (lastJsonLine == null) {
                throw new RuntimeException("No JSON output received from Whisper");
            }

            // Parse JSON output (use the last JSON line which should be the result)
            JsonNode result = objectMapper.readTree(lastJsonLine);

            if (result.has("error")) {
                throw new RuntimeException("Whisper error: " + result.get("error").asText());
            }

            List<SubtitleSegment> segments = new ArrayList<>();
            JsonNode segmentsNode = result.get("segments");
            int index = 1;

            for (JsonNode segmentNode : segmentsNode) {
                SubtitleSegment segment = new SubtitleSegment(
                        index++,
                        segmentNode.get("start").asDouble(),
                        segmentNode.get("end").asDouble(),
                        segmentNode.get("text").asText()
                );
                segments.add(segment);
            }

            log.info("Transcription completed: {} segments", segments.size());
            return segments;

        } catch (Exception e) {
            log.error("Whisper transcription failed", e);
            throw new RuntimeException("Transcription failed", e);
        }
    }

    private String getScriptPath(String scriptName) {
        // Try to find script in classpath resources
        String resourcePath = "src/main/resources/python/" + scriptName;
        File scriptFile = new File(resourcePath);

        if (scriptFile.exists()) {
            return scriptFile.getAbsolutePath();
        }

        // Fallback to assuming it's in the working directory
        return "python/" + scriptName;
    }
}
