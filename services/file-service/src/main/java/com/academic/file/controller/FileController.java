package com.academic.file.controller;

import com.academic.file.dto.*;
import com.academic.file.service.FileService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequestMapping("/api/files")
public class FileController {

	private final FileService service;

	public FileController(FileService service) {
		this.service = service;
	}

	@PostMapping(value = "/upload/{uploaderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<FileUploadDto>> upload(
			@PathVariable @NotBlank String uploaderId,
			@RequestPart("file") @NotNull MultipartFile file) {
		ApiResponse<FileUploadDto> resp = ApiResponse.success(service.uploadFile(uploaderId, file));
		return ResponseEntity.status(201).body(resp);
	}

	@DeleteMapping("/delete/{fileId}")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable @NotBlank String fileId) {
		service.deleteFile(fileId);
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	@PutMapping("/rename/{fileId}")
	public ResponseEntity<ApiResponse<FileUploadDto>> rename(
		@PathVariable @NotBlank String fileId,
		@Valid @RequestBody RenameRequest req) {
		ApiResponse<FileUploadDto> resp = ApiResponse.success(service.renameFile(fileId, req.getFileName()));
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/check/{fileId}")
	public ResponseEntity<ApiResponse<FileCheckDto>> check(@PathVariable @NotBlank String fileId) {
		ApiResponse<FileCheckDto> resp = ApiResponse.success(service.checkFile(fileId));
		return ResponseEntity.ok(resp);
	}
}
