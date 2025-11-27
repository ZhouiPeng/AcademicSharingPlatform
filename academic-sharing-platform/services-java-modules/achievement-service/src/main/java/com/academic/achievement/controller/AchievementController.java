package com.academic.achievement.controller;

import java.util.List;

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

import com.academic.achievement.dto.AchievementDto;
import com.academic.achievement.dto.CollectionFolderDto;
import com.academic.achievement.service.AchievementService;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    private final AchievementService service;

    public AchievementController(AchievementService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<String> upload(@RequestBody AchievementDto dto) {
        service.upload(dto);
        return ResponseEntity.status(201).body("uploaded");
    }

    @PostMapping("/uncertified")
    public ResponseEntity<String> uploadUncertified(@RequestBody AchievementDto dto) {
        service.uploadUncertified(dto);
        return ResponseEntity.status(201).body("uploaded-uncertified");
    }

    @PutMapping("/{achId}")
    public ResponseEntity<String> update(@PathVariable String achId, @RequestBody AchievementDto dto) {
        service.update(achId, dto);
        return ResponseEntity.ok("updated");
    }

    @DeleteMapping("/{achId}")
    public ResponseEntity<String> delete(@PathVariable String achId) {
        service.delete(achId);
        return ResponseEntity.ok("deleted");
    }

    // 保留原有简单查阅接口
    @GetMapping("/achievements/{achId}")
    public ResponseEntity<AchievementDto> viewDetail(@PathVariable String achId) {
        return ResponseEntity.ok(service.get(achId));
    }

    @GetMapping("/{achId}")
    public ResponseEntity<AchievementDto> get(@PathVariable String achId) {
        return ResponseEntity.ok(service.get(achId));
    }

    @GetMapping("/author/{authorId}")
    public ResponseEntity<List<AchievementDto>> listByAuthor(@PathVariable String authorId) {
        return ResponseEntity.ok(service.listByAuthor(authorId));
    }

    @GetMapping("/{achId}/download")
    public ResponseEntity<String> download(@PathVariable String achId) {
        String url = service.generateDownloadLink(achId);
        return ResponseEntity.ok(url);
    }

    // 收藏相关
    @PostMapping("/folders")
    public ResponseEntity<CollectionFolderDto> createFolder(@RequestBody CollectionFolderDto dto) {
        return ResponseEntity.status(201).body(service.createFolder(dto));
    }

    @PostMapping("/{achId}/collect/{folderId}")
    public ResponseEntity<String> collect(@PathVariable String achId, @PathVariable String folderId) {
        service.collect(achId, folderId);
        return ResponseEntity.status(201).body("collected");
    }

    @DeleteMapping("/{achId}/collect")
    public ResponseEntity<String> uncollect(@PathVariable String achId, @RequestParam(required = false) String folderId) {
        service.uncollect(achId, folderId);
        return ResponseEntity.ok("uncollected");
    }

    @DeleteMapping("/collect/{folderId}")
    public ResponseEntity<String> deleteFolder(@PathVariable String folderId) {
        service.deleteFolder(folderId);
        return ResponseEntity.ok("folder-deleted");
    }

    @GetMapping("/collections")
    public ResponseEntity<List<CollectionFolderDto>> listCollections() {
        return ResponseEntity.ok(service.listCollections());
    }

    // 检索与筛选
    @GetMapping("/search")
    public ResponseEntity<List<AchievementDto>> search(@RequestParam(required = false) String q) {
        return ResponseEntity.ok(service.search(q));
    }

    @GetMapping("/filter")
    public ResponseEntity<List<AchievementDto>> filter(@RequestParam(required = false) String filter) {
        return ResponseEntity.ok(service.filter(filter));
    }

    @GetMapping("/category/{catId}")
    public ResponseEntity<List<AchievementDto>> category(@PathVariable String catId) {
        return ResponseEntity.ok(service.listByCategory(catId));
    }

    @GetMapping("/search/sort")
    public ResponseEntity<List<AchievementDto>> searchSort(@RequestParam(required = false) String sort) {
        return ResponseEntity.ok(service.searchWithSort(sort));
    }
}
