package com.academic.achievement.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academic.achievement.dto.AchievementDto;
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

    @GetMapping("/{achId}")
    public ResponseEntity<AchievementDto> get(@PathVariable String achId) {
        return ResponseEntity.ok(service.get(achId));
    }
}
