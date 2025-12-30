package com.academic.datasync.client;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
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
            @Value("${file.service.url:http://file-service:8083}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    /**
     * Download a remote URL into memory (no local disk) and upload it to
     * file-service. This avoids writing files to disk; both download and upload
     * happen in memory.
     */
    public String uploadFromUrl(String uploaderId, String id, String filename) {
        String safeUploaderId = (uploaderId == null || uploaderId.isBlank()) ? "system" : uploaderId;
        if (!safeUploaderId.equals(uploaderId)) {
            log.warn("uploadFromUrl called with blank uploaderId; using '{}'", safeUploaderId);
        }
        String safeFilename = (filename == null || filename.isBlank()) ? "file.pdf" : filename;

        log.info("Info: Uploading to file-service: uploaderId={}, urlOrId={}, fileName={}", safeUploaderId, id, safeFilename);

        MultipartBodyBuilder baseBuilder = new MultipartBodyBuilder();
        baseBuilder.part("fileType", "PAPER");
        baseBuilder.part("fileName", safeFilename);
        baseBuilder.part("url", id);

        return sendMultipart(safeUploaderId, baseBuilder.build())
                .block(Duration.ofSeconds(20));
    }

    private Mono<String> sendMultipart(String uploaderId, MultiValueMap<String, HttpEntity<?>> parts) {
        return webClient.post()
                .uri(uriBuilder -> uriBuilder.path("/api/files/upload").build()) // 移除路径参数
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .header("X-User-Id", uploaderId) // 添加请求头
                .body(BodyInserters.fromMultipartData(parts))
                .exchangeToMono(clientResponse -> clientResponse.bodyToMono(String.class)
                .defaultIfEmpty("")
                .map(body -> {
                    if (!clientResponse.statusCode().is2xxSuccessful()) {
                        log.warn("file-service returned status {} body={}", clientResponse.statusCode(), body);
                    } else {
                        log.debug("file-service upload response: {}", body);
                    }
                    return body;
                }));
    }
}
