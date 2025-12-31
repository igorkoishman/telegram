package com.koishman.telegram.util;

import lombok.extern.slf4j.Slf4j;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;

/**
 * SSL Helper for bypassing SSL certificate validation
 * Required for corporate proxies like Netskope that perform SSL inspection
 */
@Slf4j
public class SslHelper {

    /**
     * Get an SSLSocketFactory that trusts all certificates
     * WARNING: This should only be used in corporate environments with trusted proxies
     */
    public static SSLSocketFactory getTrustAllSocketFactory() {
        try {
            // Create a trust manager that trusts all certificates
            TrustManager[] trustAllCerts = new TrustManager[]{
                new X509TrustManager() {
                    public X509Certificate[] getAcceptedIssuers() {
                        return new X509Certificate[0];
                    }
                    public void checkClientTrusted(X509Certificate[] certs, String authType) {
                    }
                    public void checkServerTrusted(X509Certificate[] certs, String authType) {
                    }
                }
            };

            // Install the all-trusting trust manager
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            return sslContext.getSocketFactory();

        } catch (Exception e) {
            log.error("Failed to create trust-all SSL socket factory", e);
            return (SSLSocketFactory) SSLSocketFactory.getDefault();
        }
    }

    /**
     * Get a HostnameVerifier that accepts all hostnames
     */
    public static HostnameVerifier getTrustAllHostnameVerifier() {
        return (hostname, session) -> true;
    }
}
