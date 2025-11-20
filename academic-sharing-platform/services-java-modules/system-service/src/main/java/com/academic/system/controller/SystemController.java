package com.academic.system.controller;

import com.academic.system.service.SystemService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class SystemController {

    private final SystemService service;

    public SystemController(SystemService service) {
        this.service = service;
    }

    @PostMapping("/categories")
    public ResponseEntity<String> createCategory(@RequestBody String body) {
        service.createCategory(body);
        return ResponseEntity.status(201).body("created");
    }
}
