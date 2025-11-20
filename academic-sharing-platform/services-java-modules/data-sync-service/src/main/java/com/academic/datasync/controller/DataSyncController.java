package com.academic.datasync.controller;

import com.academic.datasync.service.DataSyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sync")
public class DataSyncController {

    private final DataSyncService service;

    public DataSyncController(DataSyncService service) {
        this.service = service;
    }

    @PostMapping("/public-db")
    public ResponseEntity<String> pull() {
        service.pullFromPublicDb();
        return ResponseEntity.ok("started");
    }
}
