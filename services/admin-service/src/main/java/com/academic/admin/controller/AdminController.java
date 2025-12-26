package com.academic.admin.controller;

import com.academic.admin.dto.*;
import com.academic.admin.service.AdminService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import reactor.core.publisher.Mono;

@Validated
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Admin Service", description = "系统相关接口")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @PostMapping("/authentication/{userId}")
    @Operation(summary = "创建认证申请")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> createAuthentication(
            @PathVariable @NotBlank String userId,
            @RequestBody @Valid AuthRequest requestBody) {
        return adminService.createAuthentication(userId, requestBody)
                .map(status -> ResponseEntity.status(201).body(ApiResponse.success(Map.of("status", status))));
    }

    @GetMapping("/authentication/{userId}")
    @Operation(summary = "获取认证申请列表")
    public ResponseEntity<ApiResponse<List<AuthDto>>> listAuthentications(@PathVariable @NotBlank String userId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.listAuthentications(userId)));
    }

    @PutMapping("/authentication/{formId}")
    @Operation(summary = "处理认证申请")
    public ResponseEntity<ApiResponse<Map<String, String>>> processAuthentication(
            @PathVariable @NotBlank String formId,
            @RequestBody @Valid ProcessRequest requestBody) {
        String status = adminService.processAuthentication(formId, requestBody);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", status)));
    }

    @PostMapping("/report/{reporterId}")
    @Operation(summary = "创建举报")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> createReport(
            @PathVariable @NotBlank String reporterId,
            @RequestBody @Valid ReportRequest req) {
        return adminService.createReport(reporterId, req)
                .map(status -> ResponseEntity.status(201).body(ApiResponse.success(Map.of("status", status))));
    }

    @GetMapping("/report/{reporterId}")
    @Operation(summary = "获取举报列表")
    public ResponseEntity<ApiResponse<List<ReportDto>>> listReports(@PathVariable @NotBlank String reporterId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.listReports(reporterId)));
    }

    @PutMapping("/report/{reportId}")
    @Operation(summary = "处理举报")
    public ResponseEntity<ApiResponse<Map<String, String>>> processReport(
            @PathVariable @NotBlank String reportId,
            @RequestBody @Valid ProcessRequest req) {
        String status = adminService.processReport(reportId, req);
        return ResponseEntity.ok(ApiResponse.success(Map.of("status", status)));
    }

    @PostMapping("/information")
    @Operation(summary = "发送系统消息")
    public Mono<ResponseEntity<ApiResponse<Void>>> sendInformation(@RequestBody @Valid SendInfoRequest req) {
        return adminService.sendInformation(req)
                .then(Mono.just(ResponseEntity.ok(ApiResponse.success(null))));
    }

    @PutMapping("/information/{userId}")
    @Operation(summary = "标记系统消息为已读")
    public ResponseEntity<ApiResponse<Void>> readInformation(
            @PathVariable @NotBlank String userId,
            @RequestBody @Valid RODInfoRequest req) {
        adminService.readInformation(userId, req.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/information")
    @Operation(summary = "删除系统消息")
    public ResponseEntity<ApiResponse<Void>> deleteInformation(
            @RequestBody @Valid RODInfoRequest req) {
        adminService.deleteInformation(req.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/information/{userId}")
    @Operation(summary = "删除个人系统消息")
    public ResponseEntity<ApiResponse<Void>> deletePersonalInformation(
            @PathVariable @NotBlank String userId,
            @RequestBody @Valid RODInfoRequest req) {
        adminService.deletePersonalInformation(userId, req.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/information/{userId}")
    @Operation(summary = "获取系统消息列表")
    public ResponseEntity<ApiResponse<List<InformationDto>>> getInformation(@PathVariable @NotBlank String userId) {
        return ResponseEntity.ok(ApiResponse.success(adminService.getInformation(userId)));
    }
}
