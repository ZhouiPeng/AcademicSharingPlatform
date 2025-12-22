package com.academic.file.controller;

import com.academic.file.dto.*;
import com.academic.file.exception.FileException;
import com.academic.file.service.FileService;
import com.academic.file.service.ObsClientService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.IOException;
import java.io.InputStream;
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
	private final ObsClientService obsClientService;

	public FileController(FileService service, ObsClientService obsClientService) {
		this.service = service;
		this.obsClientService = obsClientService;
	}

	@PostMapping(value = "/upload/{uploaderId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<FileUploadDto>> upload(
			@PathVariable @NotBlank String uploaderId,
			@ModelAttribute @Valid FileUploadRequest req) {
		ApiResponse<FileUploadDto> resp = ApiResponse.success(service.uploadFile(uploaderId, req));
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

	@GetMapping("/download")
	public ResponseEntity<ApiResponse<Void>> download(
		@RequestParam @NotBlank String filePath, 
		HttpServletResponse response) {
		try (InputStream input = obsClientService.getObject(filePath)) {
			input.transferTo(response.getOutputStream());
			return ResponseEntity.ok(ApiResponse.success(null));
		} catch (IOException e) {
			throw new FileException("Download failed", e);
		}
	}
}
