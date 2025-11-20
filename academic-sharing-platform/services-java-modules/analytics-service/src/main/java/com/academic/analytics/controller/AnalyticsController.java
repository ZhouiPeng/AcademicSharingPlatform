package com.academic.analytics.controller;

import com.academic.analytics.dto.*;
import com.academic.analytics.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analysis")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @GetMapping("/hot-topics")
    public ResponseEntity<ApiResponse<HotTopicsResponse>> hotTopics(@RequestBody(required = false) HotTopicsRequest request) {
        HotTopicsResponse resp = service.hotTopics(request == null ? new HotTopicsRequest() : request);
        return ResponseEntity.ok(new ApiResponse<>(resp));
    }

    @GetMapping("/report/{reportId}")
    public ResponseEntity<ApiResponse<ReportExportResponse>> report(@PathVariable String reportId,
            @RequestBody(required = false) ReportExportRequest request) {
        ReportExportResponse resp = service.getReport(reportId, request == null ? new ReportExportRequest() : request);
        return ResponseEntity.ok(new ApiResponse<>(resp));
    }

    @GetMapping("/achievements")
    public ResponseEntity<ApiResponse<AchievementsStatsResponse>> achievements(@RequestBody(required = false) AchievementsStatsRequest request) {
        AchievementsStatsResponse resp = service.achievementsStats(request == null ? new AchievementsStatsRequest() : request);
        return ResponseEntity.ok(new ApiResponse<>(resp));
    }
}
