package com.academic.admin.controller;
import com.academic.admin.dto.*;
import com.academic.admin.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

@Validated
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/authentication/{userId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> createAuthentication(
        @PathVariable @NotBlank String userId,
        @RequestBody AuthRequest requestBody) {
        String status = adminService.createAuthentication(userId, requestBody);
        return ResponseEntity.status(201).body(ApiResponse.success(Map.of("status", status)));
    }

    @GetMapping("/authentication/{userId}")
    public ResponseEntity<ApiResponse<List<AuthDto>>> listAuthentications( @PathVariable @NotBlank String userId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.listAuthentications(userId)));
    }

    @PutMapping("/authentication/{formId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> processAuthentication(
        @PathVariable @NotBlank String formId,
        @Valid @RequestBody ProcessRequest requestBody) {
        String status = adminService.processAuthentication(formId, requestBody);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", status)));
    }

    @PostMapping("/report/{reporterId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> createReport(
        @PathVariable @NotBlank String reporterId, 
        @Valid @RequestBody ReportRequest req) {
        String status = adminService.createReport(reporterId, req);
        return ResponseEntity.status(201).body(ApiResponse.success(Map.of("status", status)));
    }

    @GetMapping("/report/{reporterId}")
    public ResponseEntity<ApiResponse<List<ReportDto>>> listReports(@PathVariable @NotBlank String reporterId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.listReports(reporterId)));
    }

    @PutMapping("/report/{reportId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> processReport(
        @PathVariable @NotBlank String reportId,
        @Valid @RequestBody ProcessRequest req) {
        String status = adminService.processReport(reportId, req);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", status)));
    }


    @PostMapping("/information")
    public ResponseEntity<ApiResponse<Void>> sendInformation(@Valid @RequestBody InformationRequest req) {
        adminService.sendInformation(req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PutMapping("/information/{userId}")
    public ResponseEntity<ApiResponse<Void>> readInformation(@Valid @RequestBody InformationRequest req) {
        adminService.readInformation(req);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/information/{userId}")
    public ResponseEntity<ApiResponse<List<InformationDto>>> getInformation(@PathVariable @NotBlank String userId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getInformation(userId)));
    }
}
