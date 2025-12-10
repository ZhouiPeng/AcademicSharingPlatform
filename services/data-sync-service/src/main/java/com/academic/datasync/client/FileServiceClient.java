package com.academic.datasync.client;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

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
                    .uri(uriBuilder -> uriBuilder.path("/api/files/check/{fileId}").build(fileId))
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
            // uploaderId is expected as path variable by the server endpoint
            parts.add("file", resource);

            Mono<String> mono = webClient.post()
                    .uri(uriBuilder -> uriBuilder.path("/api/files/upload/{uploaderId}").build(uploaderId))
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
        // Download into memory with try-with-resources to always close the remote stream
        URL url;
        try {
            url = new URL(pdfUrl);
        } catch (Exception e) {
            log.error("Invalid PDF URL {}: {}", pdfUrl, e.getMessage());
            return null;
        }

        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestProperty("User-Agent", "DataSyncService/1.0");
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(120000); // increase read timeout to 120s
            conn.setInstanceFollowRedirects(true);

            try (InputStream in = conn.getInputStream()) {
                // Read fully into memory (small PDFs expected). This ensures the InputStream is closed.
                java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    baos.write(buffer, 0, read);
                }
                byte[] content = baos.toByteArray();

                ByteArrayResource resource = new ByteArrayResource(content) {
                    @Override
                    public String getFilename() {
                        return filename;
                    }
                };

                MultiValueMap<String, Object> parts = new org.springframework.util.LinkedMultiValueMap<>();
                parts.add("file", resource);

                return webClient.post()
                        .uri(uriBuilder -> uriBuilder.path("/api/files/upload/{uploaderId}").build(uploaderId))
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
                        .block(Duration.ofSeconds(180));
            }
        } catch (Exception e) {
            log.error("uploadFromUrl failed for url {} filename {}: {}", pdfUrl, filename, e.getMessage(), e);
            return null;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    /**
     * Delete a file by id from file-service. Returns true if deletion
     * succeeded.
     */
    public boolean deleteFile(String fileId) {
        try {
            return webClient.delete()
                    .uri(uriBuilder -> uriBuilder.path("/api/files/delete/{fileId}").build(fileId))
                    .retrieve()
                    .toBodilessEntity()
                    .map(resp -> resp.getStatusCode().is2xxSuccessful())
                    .block(Duration.ofSeconds(10));
        } catch (Exception e) {
            log.error("deleteFile failed for {}: {}", fileId, e.getMessage());
            return false;
        }
    }
}
