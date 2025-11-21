package com.example.admin.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    @GetMapping("/getAuthentication")
    public ResponseEntity<Map<String, Object>> getAuthentication(@RequestParam String userId) {
        // demo response
        Map<String, Object> d = new HashMap<>();
        d.put("authenticationId", "auth-" + userId);
        d.put("status", "PENDING");
        d.put("userId", userId);
        return ResponseEntity.ok(d);
    }
}
