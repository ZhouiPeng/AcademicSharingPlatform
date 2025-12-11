package com.academic.file.controller;

import com.academic.file.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseBody;
import com.academic.file.exception.FileException;

@ControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	@ResponseBody
	public ResponseEntity<ApiResponse<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
		String message = ex.getBindingResult().getFieldErrors().stream()
				.map(err -> err.getField() + ": " + err.getDefaultMessage())
				.collect(Collectors.joining("; "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, message));
	}

	@ExceptionHandler(ConstraintViolationException.class)
	@ResponseBody
	public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException ex) {
		String message = ex.getConstraintViolations().stream()
				.map(cv -> cv.getPropertyPath() + ": " + cv.getMessage())
				.collect(Collectors.joining("; "));
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, message));
	}

	@ExceptionHandler(MissingServletRequestPartException.class)
	@ResponseBody
	public ResponseEntity<ApiResponse<Void>> handleMissingPart(MissingServletRequestPartException ex) {
		String name = ex.getRequestPartName();
		String message = "Missing request part" + (name != null ? ": " + name : "");
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, message));
	}

	@ExceptionHandler(MaxUploadSizeExceededException.class)
	@ResponseBody
	public ResponseEntity<ApiResponse<Void>> handleMaxUpload(MaxUploadSizeExceededException ex) {
		return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(ApiResponse.error(413, "Uploaded file is too large"));
	}

	@ExceptionHandler(FileException.class)
	@ResponseBody
	public ResponseEntity<ApiResponse<Void>> handleFileStorage(FileException ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, ex.getMessage()));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseBody
	public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(400, ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	@ResponseBody
	public ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(500, "Internal server error: " + ex.getMessage()));
	}
}
