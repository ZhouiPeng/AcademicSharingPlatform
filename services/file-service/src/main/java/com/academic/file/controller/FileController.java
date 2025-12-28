package com.academic.file.controller;

import com.academic.file.dto.*;
import com.academic.file.service.FileService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "File Service", description = "文件相关接口")
public class FileController {

	private final FileService service;

	public FileController(FileService service) {
		this.service = service;
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@Operation(summary = "上传文件")
	public ResponseEntity<ApiResponse<FileDto>> upload(
			@RequestHeader(name = "X-User-Id") String userIdHeader,
			@ModelAttribute @Valid FileRequest req) {
		ApiResponse<FileDto> resp = ApiResponse.success(service.uploadFile(userIdHeader, req));
		return ResponseEntity.status(201).body(resp);
	}

	@DeleteMapping("/delete/{fileId}")
	@Operation(summary = "删除文件")
	public ResponseEntity<ApiResponse<Void>> delete(@PathVariable @NotBlank String fileId) {
		service.deleteFile(fileId);
		return ResponseEntity.ok(ApiResponse.success(null));
	}

	@PutMapping("/modify/{fileId}")
	@Operation(summary = "修改文件信息")
	public ResponseEntity<ApiResponse<FileDto>> modify(
		@RequestHeader(name = "X-User-Id") String userIdHeader,
		@PathVariable @NotBlank String fileId,
		@ModelAttribute @Valid FileRequest req) {
		ApiResponse<FileDto> resp = ApiResponse.success(service.modifyFile(userIdHeader, fileId, req));
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/check/{fileId}")
	@Operation(summary = "查询文件信息")
	public ResponseEntity<ApiResponse<FileDto>> check(@PathVariable @NotBlank String fileId) {
		ApiResponse<FileDto> resp = ApiResponse.success(service.checkFile(fileId));
		return ResponseEntity.ok(resp);
	}

	@GetMapping("/download/{fileId}")
	@Operation(summary = "下载文件")
	public ResponseEntity<ApiResponse<Void>> download(
		@PathVariable @NotBlank String fileId, 
		HttpServletResponse response) {
		service.downloadFile(fileId, response);
		ApiResponse<Void> resp = ApiResponse.success(null);
		return ResponseEntity.ok(resp);
	}
}
