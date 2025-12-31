package com.koishman.telegram.service;

import com.koishman.telegram.config.AliExpressConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.TrustAllStrategy;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Service;

import javax.net.ssl.SSLContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AliExpressApiClient {

    private final AliExpressConfig config;
    private static final CloseableHttpClient httpClient = createHttpClient();

    /**
     * Create HTTP client with SSL trust configuration (for Netskope compatibility)
     */
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

    /**
     * Execute API request to Taobao/AliExpress gateway
     * @param params Request parameters (will be signed automatically)
     * @return API response as JSON string
     */
    public String executeApiRequest(Map<String, String> params) {
        try {
            // Add common parameters
            Map<String, String> allParams = new TreeMap<>(params);
            allParams.put("app_key", config.getAppKey());
            allParams.put("format", "json");
            allParams.put("v", "2.0");
            allParams.put("sign_method", "md5");
            allParams.put("timestamp", String.valueOf(System.currentTimeMillis()));

            // Generate signature
            String signature = generateSignature(allParams);
            allParams.put("sign", signature);

            // Build URL with parameters
            StringBuilder urlBuilder = new StringBuilder(config.getGateway());
            urlBuilder.append("?");
            for (Map.Entry<String, String> entry : allParams.entrySet()) {
                urlBuilder.append(entry.getKey())
                        .append("=")
                        .append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8))
                        .append("&");
            }
            String url = urlBuilder.substring(0, urlBuilder.length() - 1);

            log.info("Executing API request to: {}", config.getGateway());
            log.info("API method: {}", params.get("method"));
            log.info("Full request URL: {}", url);
            log.info("Request parameters: {}", allParams);

            // Execute POST request with longer timeout
            RequestConfig requestConfig = RequestConfig.custom()
                    .setConnectionRequestTimeout(Timeout.ofSeconds(30))
                    .setResponseTimeout(Timeout.ofSeconds(30))
                    .setConnectTimeout(Timeout.ofSeconds(30))
                    .build();

            HttpPost httpPost = new HttpPost(url);
            httpPost.setConfig(requestConfig);
            httpPost.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");

            log.info("Sending HTTP POST request...");
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                int statusCode = response.getCode();
                String responseBody = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);

                log.info("API response status: {}", statusCode);
                log.info("API response body (first 500 chars): {}",
                    responseBody.length() > 500 ? responseBody.substring(0, 500) + "..." : responseBody);

                return responseBody;
            }

        } catch (Exception e) {
            log.error("Failed to execute API request", e);
            return null;
        }
    }

    /**
     * Generate MD5 signature for Taobao/AliExpress API
     * Based on TOP (Taobao Open Platform) signature algorithm
     */
    private String generateSignature(Map<String, String> params) {
        try {
            // Sort parameters alphabetically
            TreeMap<String, String> sortedParams = new TreeMap<>(params);

            // Build the string to sign: secret + key1value1key2value2... + secret
            StringBuilder signString = new StringBuilder(config.getAppSecret());
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                // Skip the sign parameter itself
                if (!"sign".equals(entry.getKey())) {
                    signString.append(entry.getKey()).append(entry.getValue());
                }
            }
            signString.append(config.getAppSecret());

            // Calculate MD5 hash
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hashBytes = md.digest(signString.toString().getBytes(StandardCharsets.UTF_8));

            // Convert to uppercase hex string
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }

            String signature = hexString.toString().toUpperCase();
            log.debug("Generated signature: {}", signature);
            return signature;

        } catch (Exception e) {
            log.error("Failed to generate signature", e);
            return "";
        }
    }

    /**
     * Alternative: Generate HMAC-SHA256 signature (if needed)
     */
    private String generateHmacSignature(Map<String, String> params) {
        try {
            TreeMap<String, String> sortedParams = new TreeMap<>(params);

            StringBuilder signString = new StringBuilder();
            for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
                if (!"sign".equals(entry.getKey())) {
                    signString.append(entry.getKey()).append(entry.getValue());
                }
            }

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(
                    config.getAppSecret().getBytes(StandardCharsets.UTF_8),
                    "HmacSHA256"
            );
            mac.init(secretKey);

            byte[] signData = mac.doFinal(signString.toString().getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : signData) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString().toUpperCase();

        } catch (Exception e) {
            log.error("Failed to generate HMAC signature", e);
            return "";
        }
    }
}
