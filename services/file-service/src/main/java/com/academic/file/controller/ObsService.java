package com.academic.file.controller;

import com.academic.file.dto.ApiResponse;
import com.academic.file.service.ObsClientService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/internal/obs")
public class ObsService {

	private final ObsClientService obsClientService;

	public ObsService(ObsClientService obsClientService) {
		this.obsClientService = obsClientService;
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<String>> upload(@RequestPart("file") MultipartFile file) {
		try {
			String url = obsClientService.uploadPdf(file);
			return ResponseEntity.status(201).body(ApiResponse.success(url));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(ApiResponse.error(500, "Upload failed: " + e.getMessage()));
		}
	}

	@DeleteMapping("/{filename}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String filename) {
		try {
			obsClientService.delete(filename);
			return ResponseEntity.ok(ApiResponse.success(null));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(ApiResponse.error(500, "Delete failed: " + e.getMessage()));
		}
	}
}
