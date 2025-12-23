package com.academic.datasync.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class AchievementServiceClient {

    private final WebClient webClient;

    public AchievementServiceClient(WebClient.Builder builder,
            @Value("${achievement.service.url:http://achievement-service:8082}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public String createAchievement(String jsonPayload) {
        try {
            String resp = webClient.post()
                    .uri("/api/achievements")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(jsonPayload)
                    .retrieve()
                    .onStatus(status -> !status.is2xxSuccessful(), clientResponse
                            -> clientResponse.bodyToMono(String.class).map(body -> new RuntimeException("achievement service returned non-2xx: " + body)))
                    .bodyToMono(String.class)
                    .block();
            if (resp == null) {
                return null;
            }
            ObjectMapper om = new ObjectMapper();
            JsonNode root = om.readTree(resp);
            JsonNode data = root.path("data");
            String achId = data.path("achievementId").asText(null);
            return achId;
        } catch (Exception e) {
            return null;
        }
    }
}
