package com.academic.datasync.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
public class AchievementServiceClient {

    private final WebClient webClient;

    public AchievementServiceClient(WebClient.Builder builder,
            @Value("${achievement.service.url:http://localhost:8082}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public String createAchievement(String jsonPayload) {
        try {
            Mono<String> mono = webClient.post()
                    .uri("/api/achievements")
                    .header("Content-Type", "application/json")
                    .bodyValue(jsonPayload)
                    .retrieve()
                    .bodyToMono(String.class);
            return mono.block();
        } catch (Exception e) {
            return null;
        }
    }
}
