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
        sendMessage(chatId, text, null);
    }

    public void sendMessage(Long chatId, String text, Object replyMarkup) {
        String url = config.getApiBase() + "/bot" + config.getBotToken() + "/sendMessage";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", chatId);
        requestBody.put("text", text);
        if (replyMarkup != null) {
            requestBody.put("reply_markup", replyMarkup);
        }

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

    public void editMessageText(Long chatId, Integer messageId, String text, Object replyMarkup) {
        String url = config.getApiBase() + "/bot" + config.getBotToken() + "/editMessageText";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("chat_id", chatId);
        requestBody.put("message_id", messageId);
        requestBody.put("text", text);
        if (replyMarkup != null) {
            requestBody.put("reply_markup", replyMarkup);
        }

        try {
            String json = objectMapper.writeValueAsString(requestBody);

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(json));

            httpClient.execute(httpPost, response -> {
                log.info("Message edited for chatId {}: {} (status: {})", chatId, text, response.getCode());
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to edit message for chatId {}", chatId, e);
        }
    }

    public void answerCallbackQuery(String callbackQueryId, String text) {
        String url = config.getApiBase() + "/bot" + config.getBotToken() + "/answerCallbackQuery";

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("callback_query_id", callbackQueryId);
        if (text != null) {
            requestBody.put("text", text);
        }

        try {
            String json = objectMapper.writeValueAsString(requestBody);

            HttpPost httpPost = new HttpPost(url);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(json));

            httpClient.execute(httpPost, response -> {
                log.debug("Callback query answered (status: {})", response.getCode());
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to answer callback query", e);
        }
    }

    public java.io.File downloadFile(String fileId, String outputPath) {
        try {
            // First, get file path
            String getFileUrl = config.getApiBase() + "/bot" + config.getBotToken() + "/getFile";
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("file_id", fileId);

            String json = objectMapper.writeValueAsString(requestBody);
            HttpPost httpPost = new HttpPost(getFileUrl);
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setEntity(new StringEntity(json));

            String filePath = httpClient.execute(httpPost, response -> {
                String responseBody = org.apache.hc.core5.http.io.entity.EntityUtils.toString(response.getEntity());
                com.fasterxml.jackson.databind.JsonNode jsonNode = objectMapper.readTree(responseBody);
                return jsonNode.get("result").get("file_path").asText();
            });

            // Download the file
            String downloadUrl = config.getApiBase() + "/file/bot" + config.getBotToken() + "/" + filePath;
            org.apache.hc.client5.http.classic.methods.HttpGet httpGet = new org.apache.hc.client5.http.classic.methods.HttpGet(downloadUrl);

            java.io.File outputFile = new java.io.File(outputPath);
            outputFile.getParentFile().mkdirs();

            httpClient.execute(httpGet, response -> {
                try (java.io.InputStream inputStream = response.getEntity().getContent();
                     java.io.FileOutputStream outputStream = new java.io.FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                log.info("File downloaded: {}", outputFile.getAbsolutePath());
                return outputFile;
            });

            return outputFile;
        } catch (Exception e) {
            log.error("Failed to download file {}", fileId, e);
            throw new RuntimeException("File download failed", e);
        }
    }

    public void sendVideo(Long chatId, java.io.File videoFile, String caption) {
        String url = config.getApiBase() + "/bot" + config.getBotToken() + "/sendVideo";

        try {
            var entity = org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder.create()
                    .addBinaryBody("video", videoFile, org.apache.hc.core5.http.ContentType.APPLICATION_OCTET_STREAM, videoFile.getName())
                    .addTextBody("chat_id", String.valueOf(chatId));

            if (caption != null) {
                entity.addTextBody("caption", caption);
            }

            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(entity.build());

            httpClient.execute(httpPost, response -> {
                log.info("Video sent to chatId {} (status: {})", chatId, response.getCode());
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to send video to chatId {}", chatId, e);
        }
    }

    public void sendDocument(Long chatId, java.io.File documentFile, String caption) {
        String url = config.getApiBase() + "/bot" + config.getBotToken() + "/sendDocument";

        try {
            var entity = org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder.create()
                    .addBinaryBody("document", documentFile, org.apache.hc.core5.http.ContentType.APPLICATION_OCTET_STREAM, documentFile.getName())
                    .addTextBody("chat_id", String.valueOf(chatId));

            if (caption != null) {
                entity.addTextBody("caption", caption);
            }

            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(entity.build());

            httpClient.execute(httpPost, response -> {
                log.info("Document sent to chatId {} (status: {})", chatId, response.getCode());
                return null;
            });
        } catch (Exception e) {
            log.error("Failed to send document to chatId {}", chatId, e);
        }
    }
}
