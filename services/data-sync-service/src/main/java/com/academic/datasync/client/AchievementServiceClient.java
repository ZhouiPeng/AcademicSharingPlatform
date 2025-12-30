package com.academic.datasync.client;

import java.time.Duration;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
public class AchievementServiceClient {

    private static final Logger log = LoggerFactory.getLogger(AchievementServiceClient.class);

    private final WebClient webClient;

    public AchievementServiceClient(WebClient.Builder builder,
            @Value("${achievement.service.url:http://achievement-service:8082}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    public Mono<String> createAchievement(String jsonPayload) {
        // call achievement-service and parse structured response like admin-service
        return webClient.post()
                .uri("/api/achievements")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(jsonPayload)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), clientResponse -> clientResponse.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> {
                    String bodyPreview = body;
                    if (bodyPreview != null && bodyPreview.length() > 800) {
                        bodyPreview = bodyPreview.substring(0, 800) + "...";
                    }
                    log.warn("achievement-service non-2xx: status={} bodyPreview={}", clientResponse.statusCode(), bodyPreview);
                    return new RuntimeException(
                            "achievement service error: " + clientResponse.statusCode() + " " + bodyPreview);
                }))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .timeout(Duration.ofSeconds(10))
                .flatMap(resp -> {
                    if (resp == null) {
                        return Mono.empty();
                    }
                    Object d = resp.get("data");
                    if (d instanceof Map) {
                        Object achId = ((Map<?, ?>) d).get("achievementId");
                        return achId == null ? Mono.empty() : Mono.just(String.valueOf(achId));
                    }
                    return Mono.empty();
                })
                // keep old behavior: swallow errors and return "no id"
                .doOnError(e -> log.warn("achievement-service call failed (swallowed): {}", e.toString()))
                .onErrorResume(e -> Mono.empty());
    }
}
