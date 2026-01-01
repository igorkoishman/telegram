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
            StringBuilder output = new StringBuilder();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("Translation failed with exit code: " + exitCode);
            }

            // Parse JSON output
            JsonNode result = objectMapper.readTree(output.toString());

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
