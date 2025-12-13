package com.academic.admin.service;

import java.util.List;

import com.academic.admin.dto.*;

public interface AdminService {
    String createAuthentication(String userId, AuthRequest req);
    List<AuthDto> listAuthentications(String userId);
    String processAuthentication(String formId, ProcessRequest req);
    String createReport(String reporterId, ReportRequest req);
    List<ReportDto> listReports(String reporterId);
    String processReport(String reporterId, ProcessRequest req);
    void sendInformation(InformationRequest req);
    List<InformationDto> getInformation(String userId);
}
