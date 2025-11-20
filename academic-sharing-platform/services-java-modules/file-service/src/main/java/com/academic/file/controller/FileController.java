package com.academic.file.controller;

import com.academic.file.service.FileService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/files")
public class FileController {

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> upload(@RequestBody String body) {
        service.upload(body);
        return ResponseEntity.status(201).body("uploaded");
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<String> download(@PathVariable String fileId) {
        return ResponseEntity.ok(service.generateDownloadLink(fileId));
    }
}
