package com.academic.datasync.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
public class AchievementServiceClient {

    private final WebClient webClient;

    public AchievementServiceClient(WebClient.Builder builder) {
        // Local development uses localhost:8082 for achievement-service
        this.webClient = builder.baseUrl("http://localhost:8082").build();
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
