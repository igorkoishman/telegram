package com.koishman.telegram.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.koishman.telegram.config.TelegramBotConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PreDestroy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "telegram.mode", havingValue = "polling", matchIfMissing = false)
public class TelegramLongPollingService {

    private final TelegramBotConfig config;
    private final EnhancedTelegramBotService botService;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicLong lastUpdateId = new AtomicLong(0);

    @EventListener(ApplicationReadyEvent.class)
    public void startPolling() {
        if (running.compareAndSet(false, true)) {
            log.info("🚀 Starting Telegram Long Polling mode...");

            // Delete webhook to ensure polling works
            deleteWebhook();

            // Start polling in background
            pollUpdates();
        }
    }

    @PreDestroy
    public void stopPolling() {
        log.info("🛑 Stopping Telegram Long Polling...");
        running.set(false);
    }

    private void deleteWebhook() {
        try {
            String url = config.getApiBase() + "/bot" + config.getBotToken() + "/deleteWebhook";
            Map<String, Object> request = new HashMap<>();
            request.put("drop_pending_updates", false);

            String response = restTemplate.postForObject(url, request, String.class);
            log.info("Webhook deleted: {}", response);
        } catch (Exception e) {
            log.warn("Failed to delete webhook (this is OK if no webhook was set): {}", e.getMessage());
        }
    }

    @Async
    public void pollUpdates() {
        log.info("📡 Long Polling started. Waiting for updates...");

        while (running.get()) {
            try {
                String url = config.getApiBase() + "/bot" + config.getBotToken() + "/getUpdates";

                Map<String, Object> params = new HashMap<>();
                params.put("offset", lastUpdateId.get() + 1);
                params.put("timeout", 30); // Long polling timeout
                params.put("allowed_updates", new String[]{"message", "callback_query"});

                String response = restTemplate.postForObject(url, params, String.class);
                JsonNode root = objectMapper.readTree(response);

                if (root.get("ok").asBoolean()) {
                    JsonNode updates = root.get("result");

                    if (updates.isArray() && updates.size() > 0) {
                        log.info("📥 Received {} update(s)", updates.size());

                        for (JsonNode update : updates) {
                            long updateId = update.get("update_id").asLong();
                            lastUpdateId.set(updateId);

                            // Process update asynchronously
                            processUpdateAsync(update.toString());
                        }
                    }
                } else {
                    log.error("Error from Telegram API: {}", response);
                    Thread.sleep(5000); // Wait before retrying
                }

            } catch (InterruptedException e) {
                log.info("Polling interrupted");
                Thread.currentThread().interrupt();
                break;
            } catch (org.springframework.web.client.ResourceAccessException e) {
                // Long polling timeout is expected - it means no new messages arrived within the timeout period
                if (e.getCause() instanceof java.net.SocketTimeoutException) {
                    log.trace("Long polling timeout (expected) - continuing to next poll");
                    // Continue immediately to next poll
                } else {
                    log.error("Network error during polling: {}", e.getMessage());
                    try {
                        Thread.sleep(5000); // Wait before retrying on real network errors
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                log.error("Error during polling: {}", e.getMessage(), e);
                try {
                    Thread.sleep(5000); // Wait before retrying
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        log.info("📡 Long Polling stopped");
    }

    @Async
    protected void processUpdateAsync(String updateJson) {
        try {
            botService.processUpdate(updateJson);
        } catch (Exception e) {
            log.error("Error processing update: {}", updateJson, e);
        }
    }
}
