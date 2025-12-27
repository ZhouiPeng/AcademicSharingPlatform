package com.academic.datasync.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academic.datasync.service.DataSyncService;

@RestController
@RequestMapping("/api/sync")
public class DataSyncController {

    private final DataSyncService service;

    public DataSyncController(DataSyncService service) {
        this.service = service;
    }

    @PostMapping("/public-db")
    public ResponseEntity<String> pull() {
        try {
            service.pullFromPublicDb();
        } catch (Exception e) {
            return ResponseEntity.status(500).body("error");
        }
        return ResponseEntity.ok("started");
    }
}
