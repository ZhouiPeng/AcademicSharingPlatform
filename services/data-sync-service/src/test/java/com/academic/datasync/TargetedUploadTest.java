package com.academic.datasync;

import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import com.academic.datasync.client.FileServiceClient;

public class TargetedUploadTest {

    @Test
    public void uploadArxivPdf() {
        WebClient.Builder builder = WebClient.builder();
        FileServiceClient client = new FileServiceClient(builder, "http://localhost:8083");

        // Chosen OA PDF: Attention Is All You Need (arXiv:1706.03762)
        String pdfUrl = "https://arxiv.org/pdf/1706.03762.pdf";
        String filename = "Attention_Is_All_You_Need.pdf";

        System.out.println("Uploading " + pdfUrl + " as " + filename);
        String resp = client.uploadFromUrl("datasync", pdfUrl, filename);
        System.out.println("Upload response: " + resp);
    }
}
