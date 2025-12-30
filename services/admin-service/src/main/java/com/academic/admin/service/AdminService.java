package com.academic.admin.service;

import java.util.List;

import com.academic.admin.dto.*;
import reactor.core.publisher.Mono;

public interface AdminService {
    Mono<String> createAuthentication(String userId, AuthRequest req);
    List<AuthDto> listAuthentications(String userId);
    String processAuthentication(String formId, ProcessRequest req);
    Mono<String> createReport(String reporterId, ReportRequest req);
    List<ReportDto> listReports(String reporterId);
    String processReport(String reporterId, ProcessRequest req);
    Mono<Void> sendInformation(SendInfoRequest req);
    void readInformation(String userId, String id);
    void deleteInformation(String req);
    void deletePersonalInformation(String userId, String id);
    List<InformationDto> getInformation(String userId);
    Mono<String> applyAchievement(String userId, String achievementId);
    List<AchievementDto> getAchievementReview(String userId);
    List<AchievementDto> getAchievement(String userId);
    String processAchievement(String formId, ProcessRequest req);
    String checkAchievement(String achievementId);
}
