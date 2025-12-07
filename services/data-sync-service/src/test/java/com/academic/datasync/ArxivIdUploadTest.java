package com.academic.datasync;

import com.academic.datasync.service.impl.DataSyncServiceImpl;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class ArxivIdUploadTest {

    @Test
    public void testUploadFromArxivById() throws Exception {
        // Create real clients pointing to localhost services (ensure file-service is running)
        org.springframework.web.reactive.function.client.WebClient.Builder builder = org.springframework.web.reactive.function.client.WebClient.builder();
        com.academic.datasync.client.AchievementServiceClient ach = new com.academic.datasync.client.AchievementServiceClient(builder, "http://localhost:8082");
        com.academic.datasync.client.FileServiceClient fileClient = new com.academic.datasync.client.FileServiceClient(builder, "http://localhost:8083");

        DataSyncServiceImpl svc = new DataSyncServiceImpl(builder, ach, fileClient);
        // Known arXiv id with PDF: 2106.14881 (example). Replace if you prefer another id.
        String arxivId = "2106.14881";
        // Call method directly to validate arXiv Atom parsing and upload path.
        String resp = svc.uploadFromArxivById(arxivId);
        System.out.println("file-service response: " + resp);
        // Expect a non-null response from file-service when it's running
        assertNotNull(resp, "Expected file-service to return a response body (JSON)");
    }
}
