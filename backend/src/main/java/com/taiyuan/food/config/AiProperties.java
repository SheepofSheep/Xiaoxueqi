package com.taiyuan.food.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "app.deepseek")
@Data
public class AiProperties {
    private boolean enabled = false;
    private String apiKey = "";
    private String baseUrl = "https://api.deepseek.com";
    private String model = "deepseek-chat";
    private int timeoutSeconds = 15;
    private boolean reasonPolishEnabled = false;
}
