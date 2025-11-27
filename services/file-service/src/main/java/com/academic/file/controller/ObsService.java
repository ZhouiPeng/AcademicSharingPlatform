package com.academic.file.controller;

import com.academic.file.dto.ApiResponse;
import com.academic.file.service.ObsClientService;

import jakarta.servlet.http.HttpServletResponse;

import java.io.InputStream;

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
	public ResponseEntity<ApiResponse<Void>> upload(
		@RequestPart("file") MultipartFile file,
		@RequestPart("filePath") String filePath) {
		try {
			obsClientService.uploadPdf(file, filePath);
			return ResponseEntity.status(201).body(ApiResponse.success(null));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(ApiResponse.error(500, "Upload failed: " + e.getMessage()));
		}
	}

	@DeleteMapping("/delete/{filePath}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String filePath) {
		try {
			obsClientService.delete(filePath);
			return ResponseEntity.ok(ApiResponse.success(null));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(ApiResponse.error(500, "Delete failed: " + e.getMessage()));
		}
	}

	@GetMapping("/download/{filePath}")
	public ResponseEntity<ApiResponse<Void>> download(
		@PathVariable String filePath, 
		HttpServletResponse response) {
		try {
			InputStream input = obsClientService.getObject(filePath);
			input.transferTo(response.getOutputStream());
			return ResponseEntity.ok(ApiResponse.success(null));
		} catch (Exception e) {
			return ResponseEntity.status(500).body(ApiResponse.error(500, "Download failed: " + e.getMessage()));
		}
	}
}
