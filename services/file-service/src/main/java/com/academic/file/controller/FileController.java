package com.academic.file.controller;

import com.academic.file.dto.*;
import com.academic.file.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/files")
public class FileController {

	private final FileService service;

	public FileController(FileService service) {
		this.service = service;
	}

	@PostMapping(value = "/upload/{uploaderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<FileDto>> upload(
			@PathVariable @NotBlank String uploaderId,
			@ModelAttribute @Valid FileRequest req) {
		ApiResponse<FileDto> resp = ApiResponse.success(service.uploadFile(uploaderId, req));
		return ResponseEntity.status(201).body(resp);
	}

	@DeleteMapping("/delete/{fileId}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable @NotBlank String fileId) {
		service.deleteFile(fileId);
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	@PutMapping("/modify/{uploaderId}/{fileId}")
	public ResponseEntity<ApiResponse<FileDto>> modify(
		@PathVariable @NotBlank String uploaderId,
		@PathVariable @NotBlank String fileId,
		@ModelAttribute @Valid FileRequest req) {
		ApiResponse<FileDto> resp = ApiResponse.success(service.modifyFile(uploaderId, fileId, req));
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/check/{fileId}")
	public ResponseEntity<ApiResponse<FileDto>> check(@PathVariable @NotBlank String fileId) {
		ApiResponse<FileDto> resp = ApiResponse.success(service.checkFile(fileId));
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/download/{fileId}")
	public ResponseEntity<ApiResponse<Void>> download(
		@PathVariable @NotBlank String fileId, 
		HttpServletResponse response) {
		service.downloadFile(fileId, response);
		ApiResponse<Void> resp = ApiResponse.success(null);
		return ResponseEntity.ok(resp);
	}
}
