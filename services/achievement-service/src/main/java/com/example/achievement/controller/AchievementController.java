package com.example.achievement.controller;

import com.example.achievement.client.UserClient;
import com.example.achievement.dto.AchievementRequest;
import com.example.achievement.dto.AchievementResponse;
import com.example.achievement.dto.UserSummary;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/achievements")
public class AchievementController {

    @Autowired
    private UserClient userClient;

    @PostMapping
    public ResponseEntity<AchievementResponse> upload(@RequestBody AchievementRequest req) {
        // demo: fetch user info from user-service
        UserSummary u = userClient.getUser(req.getUserId());
        AchievementResponse r = new AchievementResponse();
        r.setAchId("ach-" + System.currentTimeMillis());
        r.setUploaderName(u != null ? u.getDisplayName() : "unknown");
        return ResponseEntity.ok(r);
    }
}
