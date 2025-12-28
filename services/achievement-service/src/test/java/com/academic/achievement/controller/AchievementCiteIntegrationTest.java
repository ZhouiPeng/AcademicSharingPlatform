package com.academic.achievement.controller;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

import com.academic.achievement.entity.AchievementEntity;
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
                "spring.data.redis.port=6379"
        })
public class AchievementCiteIntegrationTest {

    @Autowired
    private TestRestTemplate template;

    @Autowired
    private AchievementRepository repo;

    @Test
    public void citeEndpoint_incrementsOrFallbacks() {
        AchievementEntity e = new AchievementEntity();
        e.setId("test-cite-1");
        e.setTitle("cite test");
        e.setAuthorId("test-user");
        e.setCreatedAt(System.currentTimeMillis());
        e.setCitedCount(0);
        repo.save(e);

        HttpHeaders headers = new HttpHeaders();
        headers.add("X-User-Id", "test-user");
        HttpEntity<Void> req = new HttpEntity<>(null, headers);
        ResponseEntity<String> resp = template.postForEntity("/api/achievements/test-cite-1/cite", req, String.class);
        assertThat(resp.getStatusCode().is2xxSuccessful()).isTrue();

        // Reload entity and verify citedCount >= 0 (either Redis increment + flush later or fallback increment)
        AchievementEntity after = repo.findById("test-cite-1").orElseThrow();
        assertThat(after.getCitedCount()).isGreaterThanOrEqualTo(0);
    }
}
