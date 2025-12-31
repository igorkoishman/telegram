package com.koishman.telegram.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "aliexpress")
public class AliExpressConfig {
    private String gateway;
    private String appKey;
    private String appSecret;
    private String trackingId;
    private boolean enabled;
}
