package com.academic.analytics.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academic.analytics.dto.AchievementsStatsRequest;
import com.academic.analytics.dto.AchievementsStatsResponse;
import com.academic.analytics.dto.ApiResponse;
import com.academic.analytics.dto.HotTopicsRequest;
import com.academic.analytics.dto.HotTopicsResponse;
import com.academic.analytics.dto.ReportExportRequest;
import com.academic.analytics.dto.ReportExportResponse;
import com.academic.analytics.service.AnalyticsService;

import io.swagger.v3.oas.annotations.Operation;

@RestController
@RequestMapping("/api/analysis")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @PostMapping("/collect-search")
    @Operation(summary = "记录搜索关键词")
    public ResponseEntity<ApiResponse<Object>> collectSearch(@RequestBody(required = false) java.util.Map<String, String> body) {
        if (body == null || !body.containsKey("term")) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("missing term"));
        }
        String term = body.get("term");
        try {
            service.collectSearchTerm(term);
            return ResponseEntity.ok(new ApiResponse<>(java.util.Map.of("status", "ok")));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse<>("error"));
        }
    }

    @PostMapping("/author-relationship")
    @Operation(summary = "收集并合并用户与作者关系")
    public ResponseEntity<ApiResponse<Object>> authorRelationship(@RequestBody(required = false) java.util.Map<String, String> body) {
        if (body == null || !body.containsKey("userId")) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("missing userId"));
        }
        String userId = body.get("userId");
        String authors = body.getOrDefault("authors", "");
        try {
            service.collectAuthorRelationship(userId, authors);
            return ResponseEntity.ok(new ApiResponse<>(java.util.Map.of("status", "ok")));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse<>("error"));
        }
    }

    @GetMapping("/return-relationship/{userId}")
    @Operation(summary = "返回指定用户的作者关系字符串")
    public ResponseEntity<ApiResponse<Object>> returnRelationship(@PathVariable(name = "userId") String userId) {
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(new ApiResponse<>("missing userId"));
        }
        try {
            String authors = service.getAuthorRelationship(userId);
            return ResponseEntity.ok(new ApiResponse<>(java.util.Map.of("userId", userId, "authors", authors)));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new ApiResponse<>("error"));
        }
    }

    @GetMapping("/hot-topics")
    @Operation(summary = "获取热门话题列表")
    public ResponseEntity<ApiResponse<HotTopicsResponse>> hotTopics(@RequestBody(required = false) HotTopicsRequest request) {
        HotTopicsResponse resp = service.hotTopics(request == null ? new HotTopicsRequest() : request);
        return ResponseEntity.ok(new ApiResponse<>(resp));
    }

    @GetMapping("/report/{reportId}")
    @Operation(summary = "获取指定报告的导出信息（URL/过期时间）")
    public ResponseEntity<ApiResponse<ReportExportResponse>> report(@PathVariable String reportId,
            @RequestBody(required = false) ReportExportRequest request) {
        ReportExportResponse resp = service.getReport(reportId, request == null ? new ReportExportRequest() : request);
        return ResponseEntity.ok(new ApiResponse<>(resp));
    }

    @GetMapping("/achievements")
    @Operation(summary = "获取成就统计信息")
    public ResponseEntity<ApiResponse<AchievementsStatsResponse>> achievements(@RequestBody(required = false) AchievementsStatsRequest request) {
        AchievementsStatsResponse resp = service.achievementsStats(request == null ? new AchievementsStatsRequest() : request);
        return ResponseEntity.ok(new ApiResponse<>(resp));
    }
}
