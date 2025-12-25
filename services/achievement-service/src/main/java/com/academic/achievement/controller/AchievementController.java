package com.academic.achievement.controller;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;

import com.academic.achievement.config.EnvironmentConfig;
import com.academic.achievement.dto.AchievementDto;
import com.academic.achievement.dto.AchievementFilterRequest;
import com.academic.achievement.dto.AchievementUpdateRequest;
import com.academic.achievement.dto.ApiResponse;
import com.academic.achievement.dto.CollectionFolderDto;
import com.academic.achievement.service.AchievementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/achievements")
@Tag(name = "Achievement Service", description = "成就相关接口")
public class AchievementController {

    private final AchievementService service;
    private final EnvironmentConfig envConfig;
    private final WebClient analyticsClient;

    public AchievementController(AchievementService service, EnvironmentConfig envConfig, WebClient.Builder webClientBuilder) {
        this.service = service;
        this.envConfig = envConfig;
        this.analyticsClient = webClientBuilder.baseUrl("http://localhost:8084").build();
    }

    @PostMapping
    @Operation(summary = "上传成就")
    public ResponseEntity<ApiResponse<Object>> upload(@RequestBody AchievementDto dto) {
        String id = service.upload(dto);
        java.util.Map<String, String> data = java.util.Collections.singletonMap("achievementId", id);
        // report author relationship to analytics-service asynchronously (fire-and-forget)
        try {
            String userId = dto.getUserId();
            String authors = "";
            if (dto.getAuthors() != null && !dto.getAuthors().isEmpty()) {
                authors = dto.getAuthors().stream().map(Object::toString).collect(java.util.stream.Collectors.joining(","));
            }
            if (userId != null && !userId.isBlank() && (authors != null && !authors.isBlank())) {
                java.util.Map<String, String> body = java.util.Map.of("userId", userId, "authors", authors);
                analyticsClient.post()
                        .uri("/api/analysis/author-relationship")
                        .bodyValue(body)
                        .retrieve()
                        .toBodilessEntity()
                        .subscribe();
            }
        } catch (Exception ignore) {
        }

        return ResponseEntity.status(201).body(ApiResponse.success(data, "上传成功"));
    }

    @PutMapping("/{achId}")
    @Operation(summary = "更新成就（部分字段）")
    public ResponseEntity<ApiResponse<Object>> update(@PathVariable String achId, @RequestBody AchievementUpdateRequest req) {
        // Accept body: { "achievementId": "...", "data": { ... } }
        Map<String, Object> data = req.getData();
        if (data == null || data.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("data is empty"));
        }

        // allowed keys must match Achievement properties
        java.util.Set<String> allowed = java.util.Set.of("title", "userId", "fileId", "type", "authors", "abstract", "categories");
        java.util.List<String> invalid = new java.util.ArrayList<>();
        for (String k : data.keySet()) {
            if (!allowed.contains(k)) {
                invalid.add(k);
            }
        }
        if (!invalid.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("invalid data keys: " + String.join(",", invalid)));
        }

        AchievementDto dto = new AchievementDto();
        // map allowed fields from data (with type handling)
        if (data.containsKey("title")) {
            dto.setTitle((String) data.get("title"));
        }
        if (data.containsKey("userId")) {
            dto.setUserId((String) data.get("userId"));
        }
        if (data.containsKey("fileId")) {
            dto.setFileId((String) data.get("fileId"));
        }
        if (data.containsKey("categories")) {
            Object c = data.get("categories");
            if (c instanceof java.util.List) {
                dto.setCategories(((java.util.List<?>) c).stream().map(Object::toString).toList());
            } else if (c instanceof String) {
                dto.setCategories(java.util.List.of(((String) c).split(",")));
            }
        }
        if (data.containsKey("type")) {
            Object t = data.get("type");
            if (t instanceof Number) {
                dto.setType(((Number) t).intValue());
            } else {
                dto.setType(Integer.valueOf(String.valueOf(t)));
            }
        }
        if (data.containsKey("authors")) {
            Object a = data.get("authors");
            if (a instanceof java.util.List) {
                dto.setAuthors(((java.util.List<?>) a).stream().map(Object::toString).toList());
            } else if (a instanceof String) {
                dto.setAuthors(java.util.List.of(((String) a).split(",")));
            }
        }
        if (data.containsKey("abstract")) {
            dto.setAbstractText((String) data.get("abstract"));
        }

        service.update(achId, dto);
        return ResponseEntity.ok(ApiResponse.success(null, "修改成功"));
    }

    @DeleteMapping("/{achId}")
    @Operation(summary = "删除成就")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable String achId) {
        service.delete(achId);
        return ResponseEntity.ok(ApiResponse.success(null, "删除成功"));
    }

    @GetMapping("/{achId}")
    @Operation(summary = "获取成就详情")
    public ResponseEntity<ApiResponse<AchievementDto>> get(@PathVariable String achId) {
        AchievementDto d = service.get(achId);
        return ResponseEntity.ok(ApiResponse.success(d));
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "按作者列出成就")
    public ResponseEntity<ApiResponse<com.academic.achievement.dto.PageResult<AchievementDto>>> listByAuthor(
            @PathVariable String authorId,
            @RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize) {
        java.util.List<AchievementDto> list = service.listByAuthor(authorId);
        int total = list.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        java.util.List<AchievementDto> items = from < to ? list.subList(from, to) : java.util.List.of();
        com.academic.achievement.dto.PageResult<AchievementDto> page = new com.academic.achievement.dto.PageResult<>(total, items);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/mine")
    @Operation(summary = "列出当前用户上传的成就（支持 X-User-Id header 或 userId query 参数）")
    public ResponseEntity<ApiResponse<com.academic.achievement.dto.PageResult<AchievementDto>>> myAchievements(
            @RequestHeader(value = "X-User-Id", required = false) String userIdHeader,
            @RequestParam(value = "userId", required = false) String userIdParam,
            @RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize) {
        String userId = (userIdHeader != null && !userIdHeader.isBlank()) ? userIdHeader : userIdParam;
        if (userId == null || userId.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("missing user id (provide X-User-Id header or userId query param)"));
        }
        java.util.List<AchievementDto> list = service.listByAuthor(userId);
        int total = list.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        java.util.List<AchievementDto> items = from < to ? list.subList(from, to) : java.util.List.of();
        com.academic.achievement.dto.PageResult<AchievementDto> page = new com.academic.achievement.dto.PageResult<>(total, items);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/{achId}/download")
    @Operation(summary = "生成成就下载链接")
    public ResponseEntity<ApiResponse<java.util.Map<String, Object>>> download(@PathVariable String achId) {
        String url = service.generateDownloadLink(achId);
        java.util.Map<String, Object> data = new java.util.HashMap<>();
        data.put("downloadUrl", url);
        data.put("expiresAt", Instant.now().plusSeconds(3600).toEpochMilli());
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    // 收藏相关
    @PostMapping("/folders")
    @Operation(summary = "创建收藏夹")
    public ResponseEntity<ApiResponse<com.academic.achievement.dto.FolderIdDto>> createFolder(@RequestBody CollectionFolderDto dto) {
        CollectionFolderDto created = service.createFolder(dto);
        com.academic.achievement.dto.FolderIdDto out = new com.academic.achievement.dto.FolderIdDto(created.getId());
        return ResponseEntity.status(201).body(ApiResponse.success(out, "创建成功"));
    }

    @PostMapping("/{achId}/collect/{folderId}")
    @Operation(summary = "将成就收藏到文件夹")
    public ResponseEntity<ApiResponse<Object>> collect(@PathVariable String achId, @PathVariable String folderId) {
        service.collect(achId, folderId);
        return ResponseEntity.status(201).body(ApiResponse.success(null, "收藏成功"));
    }

    @DeleteMapping("/{achId}/collect")
    @Operation(summary = "从文件夹取消收藏成就")
    public ResponseEntity<ApiResponse<Object>> uncollect(@PathVariable String achId, @RequestParam(required = false) String folderId) {
        service.uncollect(achId, folderId);
        return ResponseEntity.ok(ApiResponse.success(null, "取消收藏成功"));
    }

    @DeleteMapping("/collect/{folderId}")
    @Operation(summary = "删除收藏文件夹")
    public ResponseEntity<ApiResponse<Object>> deleteFolder(@PathVariable String folderId) {
        service.deleteFolder(folderId);
        return ResponseEntity.ok(ApiResponse.success(null, "删除收藏夹成功"));
    }

    @GetMapping("/collections")
    @Operation(summary = "列出所有收藏夹")
    public ResponseEntity<ApiResponse<java.util.List<CollectionFolderDto>>> listCollections() {
        return ResponseEntity.ok(ApiResponse.success(service.listCollections()));
    }

    // 检索与筛选
    @GetMapping("/search")
    @Operation(summary = "搜索成就")
    public ResponseEntity<ApiResponse<com.academic.achievement.dto.PageResult<AchievementDto>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize) {
        // report search term to analytics-service asynchronously (fire-and-forget)
        try {
            if (q != null && !q.isBlank()) {
                java.util.Map<String, String> body = java.util.Map.of("term", q);
                analyticsClient.post()
                        .uri("/api/analysis/collect-search")
                        .bodyValue(body)
                        .retrieve()
                        .toBodilessEntity()
                        .subscribe();
            }
        } catch (Exception ignore) {
        }
        java.util.List<AchievementDto> list = service.search(q);
        int total = list.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        java.util.List<AchievementDto> items = from < to ? list.subList(from, to) : java.util.List.of();
        com.academic.achievement.dto.PageResult<AchievementDto> page = new com.academic.achievement.dto.PageResult<>(total, items);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @PostMapping("/filter")
    @Operation(summary = "按过滤条件筛选成就")
    public ResponseEntity<ApiResponse<com.academic.achievement.dto.PageResult<AchievementDto>>> filter(
            @RequestBody(required = false) AchievementFilterRequest filterRequest) {
        AchievementFilterRequest criteria = filterRequest == null ? new AchievementFilterRequest() : filterRequest;
        int pageNum = criteria.getPageNum() == null || criteria.getPageNum() < 1 ? 1 : criteria.getPageNum();
        int pageSize = criteria.getPageSize() == null || criteria.getPageSize() < 1 ? 10 : criteria.getPageSize();

        java.util.List<AchievementDto> list = service.filter(criteria);
        int total = list.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        java.util.List<AchievementDto> items = from < to ? list.subList(from, to) : java.util.List.of();
        com.academic.achievement.dto.PageResult<AchievementDto> page = new com.academic.achievement.dto.PageResult<>(total, items);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/category/{catId}")
    @Operation(summary = "按分类列出成就")
    public ResponseEntity<ApiResponse<com.academic.achievement.dto.PageResult<AchievementDto>>> category(
            @PathVariable String catId,
            @RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize) {
        java.util.List<AchievementDto> list = service.listByCategory(catId);
        int total = list.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        java.util.List<AchievementDto> items = from < to ? list.subList(from, to) : java.util.List.of();
        com.academic.achievement.dto.PageResult<AchievementDto> page = new com.academic.achievement.dto.PageResult<>(total, items);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/search/sort")
    @Operation(summary = "带排序的搜索")
    public ResponseEntity<ApiResponse<com.academic.achievement.dto.PageResult<AchievementDto>>> searchSort(
            @RequestParam(name = "sortBy", required = false, defaultValue = "date") String sortBy,
            @RequestParam(name = "order", required = false, defaultValue = "desc") String order,
            @RequestParam(name = "pageNum", required = false, defaultValue = "1") int pageNum,
            @RequestParam(name = "pageSize", required = false, defaultValue = "10") int pageSize) {
        java.util.List<AchievementDto> list = service.searchWithSort(sortBy, order);
        int total = list.size();
        int from = Math.max(0, (pageNum - 1) * pageSize);
        int to = Math.min(total, from + pageSize);
        java.util.List<AchievementDto> items = from < to ? list.subList(from, to) : java.util.List.of();
        com.academic.achievement.dto.PageResult<AchievementDto> page = new com.academic.achievement.dto.PageResult<>(total, items);
        return ResponseEntity.ok(ApiResponse.success(page));
    }

    @GetMapping("/env")
    @Operation(summary = "查看环境信息")
    public ResponseEntity<ApiResponse<Map<String, String>>> env() {
        Map<String, String> out = new HashMap<>();
        out.put("app.env", envConfig.getAppEnv());
        out.put("isDev", String.valueOf(envConfig.isDev()));
        out.put("isProd", String.valueOf(envConfig.isProd()));
        return ResponseEntity.ok(ApiResponse.success(out));
    }
}
