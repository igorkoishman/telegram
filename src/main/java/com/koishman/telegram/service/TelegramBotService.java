package com.koishman.telegram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final TelegramApiClient apiClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void processUpdate(String updateJson) {
        try {
            JsonNode update = objectMapper.readTree(updateJson);

            if (update.has("message")) {
                JsonNode message = update.get("message");
                Long chatId = message.get("chat").get("id").asLong();

                if (message.has("text")) {
                    String text = message.get("text").asText();
                    String firstName = message.get("from").has("first_name")
                        ? message.get("from").get("first_name").asText()
                        : "there";

                    log.info("Received message from {}: {}", firstName, text);

                    handleTextMessage(chatId, text, firstName);
                }
            }
        } catch (Exception e) {
            log.error("Error processing update: {}", updateJson, e);
        }
    }

    private void handleTextMessage(Long chatId, String text, String firstName) {
        String response;

        switch (text.toLowerCase()) {
            case "/start":
                response = String.format("Hello %s! Welcome to the bot. Send me any message and I'll echo it back!", firstName);
                break;
            case "/help":
                response = "Available commands:\n" +
                          "/start - Start the bot\n" +
                          "/help - Show this help message\n" +
                          "Send any text and I'll echo it back!";
                break;
            default:
                response = String.format("You said: %s", text);
                break;
        }

        apiClient.sendMessage(chatId, response);
    }
}
