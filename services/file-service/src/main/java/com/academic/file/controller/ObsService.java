package com.academic.file.controller;

import com.academic.file.dto.ApiResponse;
import com.academic.file.service.ObsClientService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import java.io.IOException;
import com.academic.file.exception.FileException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.validation.annotation.Validated;

@Validated
@RestController
@RequestMapping("/api/obs")
public class ObsService {

	private final ObsClientService obsClientService;

	public ObsService(ObsClientService obsClientService) {
		this.obsClientService = obsClientService;
	}

	@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<Void>> upload(
		@RequestParam @NotBlank String filePath,
		@RequestPart("file") @NotNull MultipartFile file) {
		obsClientService.uploadPdf(file, filePath);
		return ResponseEntity.status(201).body(ApiResponse.success(null));
	
	}

	@DeleteMapping("/delete")
	public ResponseEntity<ApiResponse<Void>> delete(@RequestParam @NotBlank String filePath) {
		obsClientService.delete(filePath);
		return ResponseEntity.ok(ApiResponse.success(null));
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
