package com.academic.datasync.client;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Mono;

@Component
public class FileServiceClient {

    private static final Logger log = LoggerFactory.getLogger(FileServiceClient.class);

    private final WebClient webClient;

    public FileServiceClient(WebClient.Builder builder,
            @Value("${file.service.url:http://localhost:8083}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Call file-service check endpoint for a given fileId. Returns the body as
     * String (could be JSON) or null on error.
     */
    public String checkFile(String fileId) {
        try {
            Mono<String> mono = webClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/internal/files/check/{fileId}").build(fileId))
                    .retrieve()
                    .bodyToMono(String.class);
            return mono.block();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Upload a binary file (multipart) to the file-service upload endpoint.
     * Returns the raw response body (JSON) or null on error.
     */
    public String uploadFile(String uploaderId, byte[] content, String filename) {
        try {
            ByteArrayResource resource = new ByteArrayResource(content) {
                @Override
                public String getFilename() {
                    return filename;
                }
            };

            MultiValueMap<String, Object> parts = new org.springframework.util.LinkedMultiValueMap<>();
            parts.add("uploaderId", uploaderId);
            parts.add("file", resource);

            Mono<String> mono = webClient.post()
                    .uri("/internal/files/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(parts))
                    .retrieve()
                    .bodyToMono(String.class);

            return mono.block();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Download a remote URL into memory (no local disk) and upload it to
     * file-service. This avoids writing files to disk; both download and upload
     * happen in memory.
     */
    public String uploadFromUrl(String uploaderId, String pdfUrl, String filename) {
        try {
            // Stream remote PDF via InputStreamResource into file-service multipart endpoint
            URL url = new URL(pdfUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "DataSyncService/1.0");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setInstanceFollowRedirects(true);

            InputStream in = conn.getInputStream();
            InputStreamResource resource = new InputStreamResource(in) {
                @Override
                public String getFilename() {
                    return filename;
                }

                @Override
                public long contentLength() {
                    return -1; // unknown, let client use chunked transfer
                }
            };

            MultiValueMap<String, Object> parts = new org.springframework.util.LinkedMultiValueMap<>();
            parts.add("uploaderId", uploaderId);
            // also include filename as a separate form field in case the server expects it
            parts.add("filename", filename);
            parts.add("file", resource);

            // Use exchangeToMono to capture status and body for better diagnostics
            return webClient.post()
                    .uri("/internal/files/upload")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(BodyInserters.fromMultipartData(parts))
                    .exchangeToMono(response -> {
                        int status = response.statusCode().value();
                        if (response.statusCode().is2xxSuccessful()) {
                            return response.bodyToMono(String.class);
                        } else {
                            return response.bodyToMono(String.class)
                                    .defaultIfEmpty("")
                                    .map(body -> {
                                        log.error("file-service upload returned status {} body={}", status, body);
                                        return null;
                                    });
                        }
                    })
                    .block(Duration.ofSeconds(120));
        } catch (Exception e) {
            log.error("uploadFromUrl failed for url {} filename {}: {}", pdfUrl, filename, e.getMessage(), e);
            return null;
        }
    }
}
