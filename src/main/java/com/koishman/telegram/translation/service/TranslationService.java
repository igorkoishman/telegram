package com.koishman.telegram.translation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
public class TranslationService {

    private final ObjectMapper objectMapper;

    @Value("${translation.python.executable:python3}")
    private String pythonExecutable;

    public String translate(String text, String sourceLang, String targetLang) {
        return translate(text, sourceLang, targetLang, "m2m100");
    }

    public String translate(String text, String sourceLang, String targetLang, String model) {
        try {
            // Select script based on model
            String scriptName;
            switch (model.toLowerCase()) {
                case "nllb":
                    scriptName = "nllb_translate.py";
                    break;
                case "m2m100":
                default:
                    scriptName = "translate_text.py";
                    break;
            }

            String scriptPath = getScriptPath(scriptName);

            List<String> command = new ArrayList<>();
            command.add(pythonExecutable);
            command.add(scriptPath);

            // NLLB script uses different argument format
            if (model.equalsIgnoreCase("nllb")) {
                command.add("--text");
                command.add(text);
                command.add("--src-lang");
                command.add(sourceLang);
                command.add("--tgt-lang");
                command.add(targetLang);
            } else {
                command.add(text);
                command.add("--source");
                command.add(sourceLang);
                command.add("--target");
                command.add(targetLang);
            }

            log.debug("Translating with {}: {} -> {}", model, sourceLang, targetLang);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);

            Process process = pb.start();
            List<String> allOutput = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    allOutput.add(line);
                    log.debug("Translation output: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Translation failed with exit code: " + exitCode +
                    "\nOutput: " + String.join("\n", allOutput));
            }

            // Find JSON output - look for a line that starts with { (could be single-line or multi-line)
            String jsonResult = null;
            int jsonStartIndex = -1;

            for (int i = allOutput.size() - 1; i >= 0; i--) {
                String line = allOutput.get(i).trim();
                if (line.startsWith("{")) {
                    // Check if it's single-line JSON (starts and ends with {})
                    if (line.endsWith("}")) {
                        jsonResult = line;
                        break;
                    } else {
                        // Multi-line JSON - collect from this line to the end
                        jsonStartIndex = i;
                        StringBuilder jsonBuilder = new StringBuilder();
                        for (int j = jsonStartIndex; j < allOutput.size(); j++) {
                            jsonBuilder.append(allOutput.get(j)).append("\n");
                        }
                        jsonResult = jsonBuilder.toString().trim();
                        break;
                    }
                }
            }

            if (jsonResult == null) {
                throw new RuntimeException("No JSON output found in translation output. Full output:\n" +
                    String.join("\n", allOutput));
            }

            // Parse JSON output
            JsonNode result;
            try {
                result = objectMapper.readTree(jsonResult);
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse JSON output. JSON:\n" + jsonResult +
                    "\nFull output:\n" + String.join("\n", allOutput), e);
            }

            if (result.has("error")) {
                throw new RuntimeException("Translation error: " + result.get("error").asText());
            }

            // Different models return different field names
            if (result.has("translated")) {
                return result.get("translated").asText();
            } else if (result.has("translated_text")) {
                return result.get("translated_text").asText();
            } else {
                throw new RuntimeException("Unexpected translation response format");
            }

        } catch (Exception e) {
            log.error("Translation failed: {} -> {}", sourceLang, targetLang, e);
            throw new RuntimeException("Translation failed", e);
        }
    }

    private String getScriptPath(String scriptName) {
        String resourcePath = "src/main/resources/python/" + scriptName;
        File scriptFile = new File(resourcePath);

        if (scriptFile.exists()) {
            return scriptFile.getAbsolutePath();
        }

        return "python/" + scriptName;
    }
}
