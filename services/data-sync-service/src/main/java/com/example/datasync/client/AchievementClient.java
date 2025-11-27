package com.example.datasync.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Component
public class AchievementClient {

    private final WebClient webClient;

    public AchievementClient(@Value("${achievement.service.url:http://localhost:8082}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public Map<String, Object> uploadUncertified(Map<String, Object> payload) {
        try {
            return webClient.post()
                    .uri("/api/achievements/uncertified")
                    .bodyValue(payload)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
        } catch (Exception e) {
            return Map.of("error", "unavailable");
        }
    }
}
