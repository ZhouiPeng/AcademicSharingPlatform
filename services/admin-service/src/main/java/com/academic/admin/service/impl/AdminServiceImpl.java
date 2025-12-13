package com.academic.admin.service.impl;

import com.academic.admin.dto.*;
import com.academic.admin.entity.Message;
import com.academic.admin.entity.UserMessageState;
import com.academic.admin.repository.MessageRepository;
import com.academic.admin.repository.UserMessageStateRepository;
import com.academic.admin.service.AdminService;
import com.academic.admin.entity.AuthRequestEntity;
import com.academic.admin.repository.AuthRequestRepository;
import com.academic.admin.entity.ReportEntity;
import com.academic.admin.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class AdminServiceImpl implements AdminService {
    private final MessageRepository messageRepository;
    private final UserMessageStateRepository stateRepository;
    private final WebClient userWebClient;
    private final AuthRequestRepository authRequestRepository;
    private final ReportRepository reportRepository;

    public AdminServiceImpl(MessageRepository messageRepository,
                            UserMessageStateRepository stateRepository,
                            AuthRequestRepository authRequestRepository,
                            ReportRepository reportRepository,
                            @Value("${USER_SERVICE_URL:http://localhost:8081}") String userServiceUrl) {
        this.messageRepository = messageRepository;
        this.stateRepository = stateRepository;
        this.authRequestRepository = authRequestRepository;
        this.reportRepository = reportRepository;
        this.userWebClient = WebClient.builder().baseUrl(userServiceUrl).build();
    }

    @Override
    public String createAuthentication(String userId, AuthRequest req) {
        String assignedAdmin = assignedAdmin();
        String status = "PENDING";

        AuthRequestEntity auth= AuthRequestEntity.builder()
            .id(UUID.randomUUID().toString())
            .applicantUserId(userId)
            .assignedAdminId(assignedAdmin)
            .realName(req.getRealName())
            .idNumber(req.getIdNumber())
            .phone(req.getPhone())
            .organization(req.getOrganization())
            .position(req.getPosition())
            .applicationReason(req.getApplicationReason())
            .authType(req.getAuthType())
            .attachments(req.getAttachments())
            .status(status)
            .build();
        authRequestRepository.save(auth);

        InformationRequest info = new InformationRequest();
        info.setTargetGroup(assignedAdmin);
        info.setTitle("门户认证申请通知");
        info.setContent("用户(" + userId + ")申请门户认证");
        sendInformation(info);

        return status;
    }

    private String assignedAdmin() {
        List<Map<String, String>> json = fetchUsersByGroup("ADMIN");
        List<String> choices = new ArrayList<>();
        for (Map<String, String> user : json) {
            choices.add(user.get("userId"));
        }
        String assignedAdmin = null;
        if (!choices.isEmpty()) {
            assignedAdmin = choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
        }
        return assignedAdmin;
    }

    @Override
    public List<AuthDto> listAuthentications(String userId) {
        if (userId == null || userId.isEmpty()) return null;

        List<AuthRequestEntity> ents = authRequestRepository.findByApplicantUserIdOrderByCreatedAtDesc(userId);
        if (ents == null || ents.isEmpty()) return null;
        List<AuthDto> res = new ArrayList<>(ents.size());
        for (AuthRequestEntity e : ents) {
            if (e == null) continue;
            String st = e.getStatus();
            if (st == null || !"PENDING".equalsIgnoreCase(st)) continue;
            AuthDto d = AuthDto.builder()
                .userId(userId)
                .formId(e.getId())
                .realName(e.getRealName())
                .idNumber(e.getIdNumber())
                .phone(e.getPhone())
                .organization(e.getOrganization())
                .position(e.getPosition())
                .applicationReason(e.getApplicationReason())
                .authType(e.getAuthType())
                .attachments(e.getAttachments())
                .createdAt(e.getCreatedAt().toString())
                .build();
            res.add(d);
        }
        return res;
    }

    @Override
    public String processAuthentication(String formId, ProcessRequest req) {
        if (formId == null || formId.isEmpty() || req == null || req.getStatus() == null || req.getStatus().isEmpty()) {
            return null;
        }

        AuthRequestEntity ent = authRequestRepository.findById(formId).orElse(null);
        ent.setStatus(req.getStatus());
        authRequestRepository.save(ent);

        InformationRequest info = new InformationRequest();
        info.setTargetGroup(req.getUserId());
        info.setTitle("门户认证申请处理结果通知");
        info.setContent("申请结果: " + req.getStatus() + "\n备注: " + req.getRemarks());
        sendInformation(info);

        return req.getStatus();
    }

    @Override
    public String createReport(String reporterId, ReportRequest req) {
        String assignedAdmin = assignedAdmin();
        String status = "PENDING";

        ReportEntity r = ReportEntity.builder()
                .id(UUID.randomUUID().toString())
                .reporterId(reporterId)
                .type(req.getType())
                .targetId(req.getTargetId())
                .reason(req.getReason())
                .status(status)
                .build();
        reportRepository.save(r);

        InformationRequest info = new InformationRequest();
        info.setTargetGroup(assignedAdmin);
        info.setTitle("举报通知");
        String target;
        if (req.getTargetId().equals("USER")) {
            target = "用户";
        } else if (req.getTargetId().equals("CONTENT")) {
            target = "内容";
        } else {
            target = "未知对象";
        }
        info.setContent("用户(" + reporterId + ")举报" + target + "(" + req.getTargetId() + ")");
        sendInformation(info);

        return status;
    }

    @Override
    public List<ReportDto> listReports(String reporterId) {
        if (reporterId == null || reporterId.isEmpty()) return null;

        List<ReportEntity> ents = reportRepository.findByReporterIdOrderByCreatedAtDesc(reporterId);
        if (ents == null || ents.isEmpty()) return null;
        List<ReportDto> res = new ArrayList<>(ents.size());
        for (ReportEntity e : ents) {
            if (e == null) continue;
            String st = e.getStatus();
            if (st == null || !"PENDING".equalsIgnoreCase(st)) continue;
            ReportDto d = ReportDto.builder()
                .reporterId(e.getReporterId())
                .reportId(e.getId())
                .type(e.getType())
                .targetId(e.getTargetId())
                .reason(e.getReason())
                .createdAt(e.getCreatedAt().toString())
                .build();
            res.add(d);
        }
        return res;
    }

    @Override
    public String processReport(String reportId, ProcessRequest req) {
        if (reportId == null || reportId.isEmpty() || req == null || req.getStatus() == null || req.getStatus().isEmpty()) {
            return null;
        }

        ReportEntity ent = reportRepository.findById(reportId).orElse(null);
        ent.setStatus(req.getStatus());
        reportRepository.save(ent);

        // notify reporter
        InformationRequest info = new InformationRequest();
        info.setTargetGroup(ent.getReporterId());
        info.setTitle("举报处理结果通知");
        info.setContent("举报处理结果: " + req.getStatus() + "\n备注: " + req.getRemarks());
        sendInformation(info);

        return req.getStatus();
    }

    @Override
    public void sendInformation(InformationRequest req) {
        if(req.getTargetGroup() == null || req.getTargetGroup().isEmpty()) {
            throw new IllegalArgumentException("Target group cannot be null or empty");
        }
        String target = req.getTargetGroup();

        // Create message document (single message stored)
        Message msg = new Message();
        msg.setUserId(target);
        msg.setContent(req.getContent());
        msg.setTitle(req.getTitle());
        Message saved = messageRepository.save(msg);

        List<String> recipients;
        if ("ALL".equals(target)) {
            List<Map<String, String>> json = fetchAllUsers();
            recipients = new ArrayList<>();
            for (Map<String, String> user : json) {
                recipients.add(user.get("userId"));
            }
        } else if (target.startsWith("GROUP_")) {
            String groupName = target.substring("GROUP_".length());
            List<Map<String, String>> json = fetchUsersByGroup(groupName);
            recipients = new ArrayList<>();
            for (Map<String, String> user : json) {
                recipients.add(user.get("userId"));
            }
        } else {
            Map<String, String> json = fetchUserById(target);
            recipients = List.of(json.get("userId"));
        }

        for (String userId : recipients) {
            UserMessageState state = UserMessageState.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .messageId(saved.getId())
                .state("UNREAD")
                .build();
            stateRepository.save(state);
        }
    }

    private Map<String, String> fetchUserById(String userId) {
        Map<String, Object> resp = userWebClient.get()
                .uri("/users/{userId}", userId)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        if (resp == null) throw new IllegalStateException("user-service returned null for user " + userId);
        Map<String, String> data = (Map<String, String>)resp.get("data");
        if (data == null) throw new IllegalStateException("user data missing for " + userId);
        return data;
    }

    private List<Map<String, String>> fetchAllUsers() {
        Map<String, Object> resp = userWebClient.get()
                .uri("/users")
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        if (resp == null) throw new IllegalStateException("user-service returned null");
        List<Map<String, String>> data = (List<Map<String, String>>)resp.get("data");
        if (data == null) throw new IllegalStateException("user data missing");
        return data;
    }

    private List<Map<String, String>> fetchUsersByGroup(String group) {
        Map<String, Object> resp = userWebClient.get()
                .uri(uriBuilder -> uriBuilder.path("/groups/{group}/users").build(group))
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();
        if (resp == null) throw new IllegalStateException("user-service returned null for group");
        List<Map<String, String>> data = (List<Map<String, String>>)resp.get("data");
        if (data == null) throw new IllegalStateException("user data missing for group");
        return data;
    }

    @Override
    public List<InformationDto> getInformation(String userId) {
        List<UserMessageState> states = stateRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        List<InformationDto> res = new ArrayList<>(states.size());
        for (UserMessageState s : states) {
            if (s == null || s.getMessageId() == null) continue;
            Message ent = messageRepository.findById(s.getMessageId()).orElse(null);
            InformationDto dto = new InformationDto();
            dto.setId(ent.getId());
            dto.setTitle(ent.getTitle());
            dto.setContent(ent.getContent());
            dto.setCreatedAt(ent.getCreatedAt().toString());
            res.add(dto);
        }
        return res;
    }
}
