package com.academic.achievement.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academic.achievement.config.EnvironmentConfig;
import com.academic.achievement.dto.AchievementDto;
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

    public AchievementController(AchievementService service, EnvironmentConfig envConfig) {
        this.service = service;
        this.envConfig = envConfig;
    }

    @PostMapping
    @Operation(summary = "上传成就")
    public ResponseEntity<String> upload(@RequestBody AchievementDto dto) {
        service.upload(dto);
        return ResponseEntity.status(201).body("uploaded");
    }

    @PostMapping("/uncertified")
    @Operation(summary = "上传未认证成就")
    public ResponseEntity<String> uploadUncertified(@RequestBody AchievementDto dto) {
        service.uploadUncertified(dto);
        return ResponseEntity.status(201).body("uploaded-uncertified");
    }

    @PutMapping("/{achId}")
    @Operation(summary = "更新成就")
    public ResponseEntity<String> update(@PathVariable String achId, @RequestBody AchievementDto dto) {
        service.update(achId, dto);
        return ResponseEntity.ok("updated");
    }

    @DeleteMapping("/{achId}")
    @Operation(summary = "删除成就")
    public ResponseEntity<String> delete(@PathVariable String achId) {
        service.delete(achId);
        return ResponseEntity.ok("deleted");
    }

    // 保留原有简单查阅接口
    @GetMapping("/achievements/{achId}")
    @Operation(summary = "查看成就详情（备用路径）")
    public ResponseEntity<AchievementDto> viewDetail(@PathVariable String achId) {
        return ResponseEntity.ok(service.get(achId));
    }

    @GetMapping("/{achId}")
    @Operation(summary = "获取成就详情")
    public ResponseEntity<AchievementDto> get(@PathVariable String achId) {
        return ResponseEntity.ok(service.get(achId));
    }

    @GetMapping("/author/{authorId}")
    @Operation(summary = "按作者列出成就")
    public ResponseEntity<List<AchievementDto>> listByAuthor(@PathVariable String authorId) {
        return ResponseEntity.ok(service.listByAuthor(authorId));
    }

    @GetMapping("/{achId}/download")
    @Operation(summary = "生成成就下载链接")
    public ResponseEntity<String> download(@PathVariable String achId) {
        String url = service.generateDownloadLink(achId);
        return ResponseEntity.ok(url);
    }

    // 收藏相关
    @PostMapping("/folders")
    @Operation(summary = "创建收藏夹")
    public ResponseEntity<CollectionFolderDto> createFolder(@RequestBody CollectionFolderDto dto) {
        return ResponseEntity.status(201).body(service.createFolder(dto));
    }

    @PostMapping("/{achId}/collect/{folderId}")
    @Operation(summary = "将成就收藏到文件夹")
    public ResponseEntity<String> collect(@PathVariable String achId, @PathVariable String folderId) {
        service.collect(achId, folderId);
        return ResponseEntity.status(201).body("collected");
    }

    @DeleteMapping("/{achId}/collect")
    @Operation(summary = "从文件夹取消收藏成就")
    public ResponseEntity<String> uncollect(@PathVariable String achId, @RequestParam(required = false) String folderId) {
        service.uncollect(achId, folderId);
        return ResponseEntity.ok("uncollected");
    }

    @DeleteMapping("/collect/{folderId}")
    @Operation(summary = "删除收藏文件夹")
    public ResponseEntity<String> deleteFolder(@PathVariable String folderId) {
        service.deleteFolder(folderId);
        return ResponseEntity.ok("folder-deleted");
    }

    @GetMapping("/collections")
    @Operation(summary = "列出所有收藏夹")
    public ResponseEntity<List<CollectionFolderDto>> listCollections() {
        return ResponseEntity.ok(service.listCollections());
    }

    // 检索与筛选
    @GetMapping("/search")
    @Operation(summary = "搜索成就")
    public ResponseEntity<List<AchievementDto>> search(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(service.search(q));
    }

    @GetMapping("/filter")
    @Operation(summary = "按过滤条件筛选成就")
    public ResponseEntity<List<AchievementDto>> filter(@RequestParam(required = false) String filter) {
        return ResponseEntity.ok(service.filter(filter));
    }

    @GetMapping("/category/{catId}")
    @Operation(summary = "按分类列出成就")
    public ResponseEntity<List<AchievementDto>> category(@PathVariable String catId) {
        return ResponseEntity.ok(service.listByCategory(catId));
    }

    @GetMapping("/search/sort")
    @Operation(summary = "带排序的搜索")
    public ResponseEntity<List<AchievementDto>> searchSort(@RequestParam(required = false) String sort) {
        return ResponseEntity.ok(service.searchWithSort(sort));
    }

    @GetMapping("/env")
    @Operation(summary = "查看环境信息")
    public ResponseEntity<Map<String, String>> env() {
        Map<String, String> out = new HashMap<>();
        out.put("app.env", envConfig.getAppEnv());
        out.put("isDev", String.valueOf(envConfig.isDev()));
        out.put("isProd", String.valueOf(envConfig.isProd()));
        return ResponseEntity.ok(out);
    }
}
