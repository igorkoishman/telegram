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
            List<String> allOutput = new ArrayList<>();
            int jsonStartIndex = -1;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    allOutput.add(line);
                    log.debug("Whisper output: {}", line);

                    // Mark the start of JSON output (look for any line starting with {)
                    if (jsonStartIndex == -1 && line.trim().startsWith("{")) {
                        jsonStartIndex = allOutput.size() - 1;
                    }
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Whisper transcription failed with exit code: " + exitCode +
                    "\nOutput: " + String.join("\n", allOutput));
            }

            // Extract JSON from the output (everything from the opening { to the end)
            if (jsonStartIndex == -1) {
                throw new RuntimeException("No JSON output found in Whisper output. Full output:\n" +
                    String.join("\n", allOutput));
            }

            // Collect all lines from the JSON start to the end
            StringBuilder jsonBuilder = new StringBuilder();
            for (int i = jsonStartIndex; i < allOutput.size(); i++) {
                jsonBuilder.append(allOutput.get(i)).append("\n");
            }
            String jsonResult = jsonBuilder.toString().trim();

            // Parse JSON output
            JsonNode result;
            try {
                result = objectMapper.readTree(jsonResult);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse JSON output. JSON:\n" + jsonResult +
                    "\nFull output:\n" + String.join("\n", allOutput), e);
            }

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
