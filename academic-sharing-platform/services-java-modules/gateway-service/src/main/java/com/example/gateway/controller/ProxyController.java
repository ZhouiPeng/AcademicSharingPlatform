package com.example.gateway.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
public class ProxyController {

    private final WebClient webClient = WebClient.builder().build();

    @Value("${user.service.url:http://localhost:8081}")
    private String userServiceUrl;

    @Value("${achievement.service.url:http://localhost:8082}")
    private String achievementServiceUrl;

    @RequestMapping(value = "/proxy/user/get", method = RequestMethod.GET)
    public Mono<ResponseEntity<Map>> proxyUser(@RequestParam String userId) {
        return webClient.get()
                .uri(userServiceUrl + "/api/users/{id}", userId)
                .retrieve()
                .toEntity(Map.class);
    }

    @RequestMapping(value = "/proxy/achievement/create", method = RequestMethod.POST, consumes = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<Map>> proxyCreateAchievement(@RequestBody Map<String, Object> body) {
        return webClient.post()
                .uri(achievementServiceUrl + "/api/achievements")
                .bodyValue(body)
                .retrieve()
                .toEntity(Map.class);
    }
}
