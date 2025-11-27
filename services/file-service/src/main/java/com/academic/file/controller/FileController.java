package com.academic.file.controller;

import com.academic.file.dto.*;
import com.academic.file.service.FileService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/internal/files")
public class FileController {

    private final FileService service;

    public FileController(FileService service) {
        this.service = service;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FileUploadDto>> upload(
            @RequestPart("uploaderId") String uploaderId,
            @RequestPart("file") MultipartFile file) {
        ApiResponse<FileUploadDto> resp = ApiResponse.success(service.uploadFile(uploaderId, file));
        return ResponseEntity.status(201).body(resp);
    }

    @GetMapping("/{fileId}/download")
    public ResponseEntity<ApiResponse<FileDownloadDto>> download(@PathVariable String fileId) {
        ApiResponse<FileDownloadDto> resp = ApiResponse.success(service.generateDownloadLink(fileId));
        return ResponseEntity.status(200).body(resp);
    }
}
