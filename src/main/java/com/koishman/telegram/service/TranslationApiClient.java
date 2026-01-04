package com.koishman.telegram.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.koishman.telegram.config.TelegramBotConfig;
import com.koishman.telegram.model.JobResponse;
import com.koishman.telegram.model.JobStatus;
import com.koishman.telegram.model.MediaAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslationApiClient {

    private final TelegramBotConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CloseableHttpClient httpClient = createHttpClient();

    private static CloseableHttpClient createHttpClient() {
        try {
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

    public MediaAnalysis analyzeMedia(File videoFile) {
        String url = config.getTranslationApiBase() + "/api/translation/analyze";

        try {
            var entity = MultipartEntityBuilder.create()
                    .addBinaryBody("file", videoFile, ContentType.APPLICATION_OCTET_STREAM, videoFile.getName())
                    .build();

            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(entity);

            return httpClient.execute(httpPost, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("Media analysis response: {}", responseBody);
                return objectMapper.readValue(responseBody, MediaAnalysis.class);
            });
        } catch (Exception e) {
            log.error("Failed to analyze media", e);
            throw new RuntimeException("Media analysis failed", e);
        }
    }

    public JobResponse uploadVideo(File videoFile,
                                   String targetLanguages,
                                   String whisperModel,
                                   String whisperModelType,
                                   String whisperBackend,
                                   Boolean alignOutput,
                                   String translationModel,
                                   String subtitleBurnType,
                                   Boolean useSubtitlesOnly,
                                   String originalLanguage,
                                   Integer audioTrack,
                                   Integer subtitleTrack,
                                   Long chatId) {
        String url = config.getTranslationApiBase() + "/api/translation/upload";

        try {
            var builder = MultipartEntityBuilder.create()
                    .addBinaryBody("file", videoFile, ContentType.APPLICATION_OCTET_STREAM, videoFile.getName())
                    .addTextBody("langs", targetLanguages)
                    .addTextBody("model", whisperModel)
                    .addTextBody("model_type", whisperModelType)
                    .addTextBody("whisper_backend", whisperBackend != null ? whisperBackend : "faster-whisper")
                    .addTextBody("align_output", alignOutput != null && alignOutput ? "true" : "false")
                    .addTextBody("translation_model", translationModel != null ? translationModel : "m2m100")
                    .addTextBody("subtitle_burn_type", subtitleBurnType)
                    .addTextBody("align", "True");

            if (chatId != null) {
                builder.addTextBody("chat_id", String.valueOf(chatId));
            }

            if (useSubtitlesOnly != null && useSubtitlesOnly) {
                builder.addTextBody("use_subtitles_only", "true");
            }

            if (originalLanguage != null && !originalLanguage.isEmpty()) {
                builder.addTextBody("original_lang", originalLanguage);
            }

            if (audioTrack != null) {
                builder.addTextBody("audio_track", String.valueOf(audioTrack));
            }

            if (subtitleTrack != null) {
                builder.addTextBody("subtitle_track", String.valueOf(subtitleTrack));
            }

            HttpPost httpPost = new HttpPost(url);
            httpPost.setEntity(builder.build());

            return httpClient.execute(httpPost, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.info("Upload response: {}", responseBody);
                return objectMapper.readValue(responseBody, JobResponse.class);
            });
        } catch (Exception e) {
            log.error("Failed to upload video", e);
            throw new RuntimeException("Video upload failed", e);
        }
    }

    public JobStatus getJobStatus(String jobId) {
        String url = config.getTranslationApiBase() + "/api/translation/status/" + jobId;

        try {
            HttpGet httpGet = new HttpGet(url);

            return httpClient.execute(httpGet, response -> {
                String responseBody = EntityUtils.toString(response.getEntity());
                log.debug("Job status response for {}: {}", jobId, responseBody);
                return objectMapper.readValue(responseBody, JobStatus.class);
            });
        } catch (Exception e) {
            log.error("Failed to get job status for {}", jobId, e);
            throw new RuntimeException("Failed to get job status", e);
        }
    }

    public File downloadFile(String filename, String outputPath) {
        String url = config.getTranslationApiBase() + "/api/translation/download/" + filename;

        try {
            HttpGet httpGet = new HttpGet(url);
            File outputFile = new File(outputPath);

            httpClient.execute(httpGet, response -> {
                try (InputStream inputStream = response.getEntity().getContent();
                     FileOutputStream outputStream = new FileOutputStream(outputFile)) {
                    byte[] buffer = new byte[8192];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }
                log.info("Downloaded file: {}", outputFile.getAbsolutePath());
                return outputFile;
            });

            return outputFile;
        } catch (Exception e) {
            log.error("Failed to download file: {}", filename, e);
            throw new RuntimeException("File download failed", e);
        }
    }
}
