package com.academic.datasync;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PreviewPdfUrlsTest {

    @Test
    public void listPdfUrls() throws Exception {
        WebClient.Builder builder = WebClient.builder();
        WebClient client = builder.baseUrl("https://api.openalex.org").build();
        ObjectMapper mapper = new ObjectMapper();

        String body = client.get()
                .uri(uriBuilder -> uriBuilder.path("/works").queryParam("filter", "is_oa:true").queryParam("per-page", "20").build())
                .retrieve()
                .bodyToMono(String.class)
                .block(java.time.Duration.ofSeconds(10));

        if (body == null) {
            System.out.println("OpenAlex returned empty body");
            return;
        }

        JsonNode root = mapper.readTree(body);
        JsonNode results = root.path("results");
        int idx = 0;
        for (JsonNode work : results) {
            idx++;
            String id = work.path("id").asText();
            String title = work.path("title").asText("(untitled)");
            // Reuse the same extraction logic used in DataSyncServiceImpl by simple heuristics here
            String pdfUrl = null;
            JsonNode best = work.path("best_oa_location");
            if (!best.isMissingNode()) {
                pdfUrl = best.path("url_for_pdf").asText(null);
                if (pdfUrl == null || pdfUrl.isEmpty()) {
                    pdfUrl = best.path("url").asText(null);
                }
            }
            if (pdfUrl == null || pdfUrl.isEmpty()) {
                JsonNode primary = work.path("primary_location");
                if (!primary.isMissingNode()) {
                    pdfUrl = primary.path("url_for_pdf").asText(null);
                    if (pdfUrl == null || pdfUrl.isEmpty()) {
                        pdfUrl = primary.path("url").asText(null);
                    }
                }
            }
            if (pdfUrl == null || pdfUrl.isEmpty()) {
                JsonNode oaLocations = work.path("oa_locations");
                if (oaLocations != null && oaLocations.isArray()) {
                    for (JsonNode loc : oaLocations) {
                        String u = loc.path("url_for_pdf").asText(null);
                        if (u != null && !u.isEmpty()) {
                            pdfUrl = u;
                            break;
                        }
                        u = loc.path("url").asText(null);
                        if (u != null && !u.isEmpty()) {
                            pdfUrl = u;
                            break;
                        }
                    }
                }
            }

            System.out.println(String.format("[%02d] %s - %s -> %s", idx, id, title, (pdfUrl == null ? "(no-pdf)" : pdfUrl)));
        }
    }
}
