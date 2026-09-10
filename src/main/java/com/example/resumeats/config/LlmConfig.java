package com.example.resumeats.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * LLM API configuration properties and beans.
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
@Getter
@Setter
public class LlmConfig {

    private String apiKey;
    private String baseUrl;
    private String model;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
