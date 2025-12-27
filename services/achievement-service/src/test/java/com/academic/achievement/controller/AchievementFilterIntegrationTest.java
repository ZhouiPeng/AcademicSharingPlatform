package com.academic.achievement.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
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
import com.academic.achievement.dto.AchievementFilterRequest;
import com.academic.achievement.dto.ApiResponse;
import com.academic.achievement.dto.PageResult;
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
        "spring.redis.port=6379"
    })
class AchievementFilterIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AchievementRepository achievementRepository;

    @Test
    void uploadSamples_thenFilterByKeywordClassificationAndYear() {
        achievementRepository.deleteAll();

        String mlId = uploadSample(
                "机器学习公平性评估",
                List.of("Li Hua", "Chen Yu"),
                "从公平性角度重新审视联邦学习。",
                Instant.parse("2021-03-01T00:00:00Z").toEpochMilli(),
                "1001");

        String cvId = uploadSample(
                "高分辨率计算机视觉模型",
                List.of("Zhao Ming"),
                "提出高分辨率 Transformer，聚焦视觉识别。",
                Instant.parse("2023-07-15T00:00:00Z").toEpochMilli(),
                "1002");

        String materialId = uploadSample(
                "纳米复合材料耐久性研究",
                List.of("Wang Wei"),
                "评估纳米复合材料在极端气候下的耐久性。",
                Instant.parse("2019-11-20T00:00:00Z").toEpochMilli(),
                "2001");

        assertThat(mlId).isNotNull();
        assertThat(cvId).isNotNull();
        assertThat(materialId).isNotNull();

        AchievementFilterRequest fairMlReq = new AchievementFilterRequest();
        fairMlReq.setKeywords("机器学习");
        fairMlReq.setClassification("1001");
        fairMlReq.setFromYear(2020);
        fairMlReq.setToYear(2022);
        fairMlReq.setPageNum(1);
        fairMlReq.setPageSize(10);

        ApiResponse<PageResult<AchievementDto>> fairMlResult = executeFilter(fairMlReq);
        assertThat(fairMlResult.getCode()).isEqualTo(1);
        assertThat(fairMlResult.getData().getTotal()).isEqualTo(1);
        assertThat(fairMlResult.getData().getItems().get(0).getTitle()).contains("公平性");

        AchievementFilterRequest cvReq = new AchievementFilterRequest();
        cvReq.setClassification("1002");
        cvReq.setPageNum(1);
        cvReq.setPageSize(10);

        ApiResponse<PageResult<AchievementDto>> cvResult = executeFilter(cvReq);
        assertThat(cvResult.getData().getTotal()).isEqualTo(1);
        assertThat(cvResult.getData().getItems().get(0).getTitle()).contains("计算机视觉");

        AchievementFilterRequest strictYearReq = new AchievementFilterRequest();
        strictYearReq.setKeywords("纳米");
        strictYearReq.setFromYear(2020);
        strictYearReq.setPageNum(1);
        strictYearReq.setPageSize(10);

        ApiResponse<PageResult<AchievementDto>> strictYearResult = executeFilter(strictYearReq);
        assertThat(strictYearResult.getData().getTotal()).isZero();

        AchievementFilterRequest relaxedYearReq = new AchievementFilterRequest();
        relaxedYearReq.setKeywords("纳米");
        relaxedYearReq.setFromYear(2015);
        relaxedYearReq.setToYear(2020);
        relaxedYearReq.setPageNum(1);
        relaxedYearReq.setPageSize(10);

        ApiResponse<PageResult<AchievementDto>> relaxedYearResult = executeFilter(relaxedYearReq);
        assertThat(relaxedYearResult.getData().getTotal()).isEqualTo(1);
        assertThat(relaxedYearResult.getData().getItems().get(0).getTitle()).contains("纳米复合材料");
    }

    private String uploadSample(String title, List<String> authors, String abstractText, long createdAt, String classification) {
        AchievementDto dto = new AchievementDto();
        dto.setTitle(title);
        dto.setUserId("test-user");
        dto.setType(1);
        dto.setAuthors(authors);
        dto.setCategories(List.of(classification));
        dto.setAbstractText(abstractText);
        dto.setFileId("file-" + System.nanoTime());

        ResponseEntity<ApiResponse<Map<String, String>>> response = restTemplate.exchange(
                "/api/achievements",
                HttpMethod.POST,
                new HttpEntity<>(dto, jsonHeaders()),
                new ParameterizedTypeReference<ApiResponse<Map<String, String>>>() {
                });

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        ApiResponse<Map<String, String>> body = response.getBody();
        assertThat(body).isNotNull();
        assertThat(body.getCode()).isEqualTo(1);
        String achievementId = body.getData().get("achievementId");
        assertThat(achievementId).isNotBlank();

        achievementRepository.findById(achievementId).ifPresent(entity -> {
            entity.setCreatedAt(createdAt);
            achievementRepository.save(entity);
            // verify categories persisted to DB (stored as comma-separated string)
            assertThat(entity.getCategories()).isNotNull();
            assertThat(entity.getCategories()).contains(classification);
        });

        return achievementId;
    }

    private ApiResponse<PageResult<AchievementDto>> executeFilter(AchievementFilterRequest request) {
        ResponseEntity<ApiResponse<PageResult<AchievementDto>>> response = restTemplate.exchange(
                "/api/achievements/filter",
                HttpMethod.POST,
                new HttpEntity<>(request, jsonHeaders()),
                new ParameterizedTypeReference<ApiResponse<PageResult<AchievementDto>>>() {
                });

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        ApiResponse<PageResult<AchievementDto>> body = response.getBody();
        assertThat(body).isNotNull();
        return body;
    }

    private HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
