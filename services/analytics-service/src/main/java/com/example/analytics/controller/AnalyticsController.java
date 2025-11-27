package com.example.analytics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/analysis")
public class AnalyticsController {

    @GetMapping("/hot-topics")
    public ResponseEntity<Map<String, Object>> hotTopics() {
        return ResponseEntity.ok(Map.of("hot", "AI", "score", 0.95));
    }
}
