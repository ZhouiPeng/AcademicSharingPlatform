package com.academic.achievement.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import com.academic.achievement.dto.AchievementDto;
import com.academic.achievement.dto.ApiResponse;
import com.academic.achievement.repository.AchievementRepository;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.redis.host=localhost",
        "spring.redis.port=6379",
        "spring.data.redis.host=localhost",
        "spring.data.redis.port=6379",
        "spring.flyway.enabled=false"
    })
class AchievementUploadUserIdTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AchievementRepository achievementRepository;

    @Test
    void uploadWithoutUserId_usesHeaderUserId() {
        achievementRepository.deleteAll();

        AchievementDto dto = new AchievementDto();
        dto.setTitle("测试上传-无userId");
        dto.setType(1);
        dto.setAbstractText("测试描述");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.add("X-User-Id", "test-header-user");

        ResponseEntity<String> response = restTemplate.exchange(
            "/api/achievements",
            HttpMethod.POST,
            new HttpEntity<>(dto, headers),
            new ParameterizedTypeReference<String>() {
            });

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        String bodyStr = response.getBody();
        assertThat(bodyStr).isNotNull();
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
        String achievementId;
        try {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> parsed = mapper.readValue(bodyStr, java.util.Map.class);
            @SuppressWarnings("unchecked")
            java.util.Map<String, String> data = (java.util.Map<String, String>) parsed.get("data");
            achievementId = data.get("achievementId");
        } catch (Exception ex) {
            throw new RuntimeException("failed to parse response JSON", ex);
        }
        assertThat(achievementId).isNotBlank();

        achievementRepository.findById(achievementId).ifPresent(entity -> {
            assertThat(entity.getAuthorId()).isEqualTo("test-header-user");
        });
    }
}
