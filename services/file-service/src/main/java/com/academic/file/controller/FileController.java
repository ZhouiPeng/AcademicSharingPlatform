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
        try {
            ApiResponse<FileUploadDto> resp = ApiResponse.success(service.uploadFile(uploaderId, file));
            return ResponseEntity.status(201).body(resp);
        } catch (Exception e) {
			return ResponseEntity.status(500).body(ApiResponse.error(500, "Upload failed: " + e.getMessage()));
		}
    }

    @DeleteMapping("/delete/{fileId}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String fileId) {
		try {
			service.deleteFile(fileId);
			return ResponseEntity.ok(ApiResponse.success(null));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(ApiResponse.error(500, "Delete failed: " + e.getMessage()));
		}
	}

    @PutMapping("/modify/{fileId}")
	public ResponseEntity<ApiResponse<FileCheckDto>> modify(@PathVariable String fileId) {
		try {
            ApiResponse<FileCheckDto> resp = ApiResponse.success(service.modifyFile(fileId));
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(ApiResponse.error(500, "Modify failed: " + e.getMessage()));
		}
	}


    @GetMapping("/check/{fileId}")
	public ResponseEntity<ApiResponse<FileCheckDto>> check(@PathVariable String fileId) {
		try {
            ApiResponse<FileCheckDto> resp = ApiResponse.success(service.checkFile(fileId));
			return ResponseEntity.ok(resp);
		} catch (Exception e) {
			return ResponseEntity.status(500).body(ApiResponse.error(500, "Check failed: " + e.getMessage()));
		}
	}
}
