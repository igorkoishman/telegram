package com.koishman.telegram.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koishman.telegram.config.TelegramBotConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramApiClient {

    private final TelegramBotConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = createHttpClient();

    private static CloseableHttpClient createHttpClient() {
        try {
            // Create SSL context that trusts all certificates (for Netskope compatibility)
            SSLContext sslContext = SSLContextBuilder.create()
                    .loadTrustMaterial(new TrustAllStrategy())
                    .build();

            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContext);

            return HttpClients.custom()
                    .setConnectionManager(PoolingHttpClientConnectionManagerBuilder.create()
                            .setSSLSocketFactory(sslSocketFactory)
                            .build())
                    .build();
        } catch (Exception e) {
            log.error("Failed to create HTTP client with custom SSL", e);
            return HttpClients.createDefault();
        }
    }

    public void sendMessage(Long chatId, String text) {
        String url = config.getApiBase() + "/bot" + config.getBotToken() + "/sendMessage";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", text);

        try {
            String json = objectMapper.writeValueAsString(requestBody);

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(json));

            httpClient.execute(httpPost, response -> {
                log.info("Message sent to chatId {}: {} (status: {})", chatId, text, response.getCode());
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to send message to chatId {}", chatId, e);
        }
    }
}
