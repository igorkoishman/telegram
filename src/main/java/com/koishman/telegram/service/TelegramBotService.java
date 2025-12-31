package com.koishman.telegram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koishman.telegram.model.AliExpressProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramBotService {

    private final TelegramApiClient apiClient;
    private final AliExpressService aliExpressService;
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

        // Handle commands
        if (text.toLowerCase().startsWith("/search ")) {
            String query = text.substring(8).trim();
            if (query.isEmpty()) {
                response = "❌ Please provide a search query!\nExample: /search wireless headphones";
            } else {
                handleSearchCommand(chatId, query);
                return; // Don't send response here, handleSearchCommand will send it
            }
        } else {
            switch (text.toLowerCase()) {
                case "/start":
                    response = String.format("Hello %s! 👋\n\n" +
                            "I'm your AliExpress Shopping Assistant! 🛍️\n\n" +
                            "Use /search <product> to find products on AliExpress.\n" +
                            "Example: /search wireless headphones\n\n" +
                            "Use /help to see all commands.", firstName);
                    break;
                case "/help":
                    response = "📋 Available commands:\n\n" +
                            "/start - Start the bot\n" +
                            "/help - Show this help message\n" +
                            "/search <query> - Search for products on AliExpress\n\n" +
                            "Examples:\n" +
                            "• /search bluetooth speaker\n" +
                            "• /search phone case\n" +
                            "• /search wireless mouse";
                    break;
                default:
                    response = "💡 Tip: Use /search <product> to find items on AliExpress!\n" +
                            "For example: /search " + text;
                    break;
            }
        }

        apiClient.sendMessage(chatId, response);
    }

    private void handleSearchCommand(Long chatId, String query) {
        log.info("Searching AliExpress for: {}", query);

        // Send "searching" message
        apiClient.sendMessage(chatId, "🔍 Searching AliExpress for: " + query + "...");

        try {
            // Search for products
            List<AliExpressProduct> products = aliExpressService.searchProducts(query);

            // Format and send results
            String resultsMessage = aliExpressService.formatProductList(products, query);
            apiClient.sendMessage(chatId, resultsMessage);

        } catch (Exception e) {
            log.error("Error searching AliExpress", e);
            apiClient.sendMessage(chatId, "❌ Sorry, something went wrong while searching. Please try again later.");
        }
    }
}
