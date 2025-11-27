package com.example.achievement.client;

import com.example.achievement.dto.UserSummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class UserClient {

    private final WebClient webClient;

    public UserClient(@Value("${user.service.url:http://localhost:8081}") String baseUrl) {
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public UserSummary getUser(String userId) {
        try {
                return webClient.get()
                    .uri("/api/users/{id}", userId)
                    .retrieve()
                    .onStatus(status -> status.isError(), r -> r.createException())
                    .bodyToMono(UserSummary.class)
                    .block();
        } catch (Exception e) {
            return null;
        }
    }
}
