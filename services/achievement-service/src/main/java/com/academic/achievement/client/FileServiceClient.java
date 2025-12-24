package com.academic.achievement.client;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

@Component
public class FileServiceClient {

    private final WebClient webClient;

    public FileServiceClient(WebClient.Builder builder) {
        // Local development uses localhost:8083; Docker Compose can replace this host.
        this.webClient = builder.baseUrl("http://file-service:8083").build();
    }

    /**
     * Call file-service to get a pre-signed download link for a fileId.
     */
    public String getDownloadLink(String fileId) {
        try {
            Mono<String> mono = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/internal/files/{fileId}/download").build(fileId))
                    .retrieve()
                    .bodyToMono(String.class);
            return mono.block();
        } catch (Exception e) {
            return null;
        }
    }
}
