package com.academic.admin.service.impl;

import com.academic.admin.dto.*;
import com.academic.admin.entity.Message;
import com.academic.admin.entity.UserMessageState;
import com.academic.admin.repository.MessageRepository;
import com.academic.admin.repository.UserMessageStateRepository;
import com.academic.admin.repository.AchievementRepository;
import com.academic.admin.service.AdminService;
import com.academic.admin.entity.AchievementEntity;
import com.academic.admin.entity.AuthRequestEntity;
import com.academic.admin.repository.AuthRequestRepository;
import com.academic.admin.entity.ReportEntity;
import com.academic.admin.repository.ReportRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;
import java.time.Duration;
import java.util.HashMap;
import java.util.stream.Collectors;
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
    private final AchievementRepository achievementRepository;
    private final ReportRepository reportRepository;

    public AdminServiceImpl(MessageRepository messageRepository,
            UserMessageStateRepository stateRepository,
            AuthRequestRepository authRequestRepository,
            AchievementRepository achievementRepository,
            ReportRepository reportRepository,
            @Value("http://user-service:8081") String userServiceUrl) {
        this.messageRepository = messageRepository;
        this.stateRepository = stateRepository;
        this.authRequestRepository = authRequestRepository;
        this.achievementRepository = achievementRepository;
        this.reportRepository = reportRepository;
        this.userWebClient = WebClient.builder().baseUrl(userServiceUrl).build();
    }

    @Override
    public Mono<String> createAuthentication(String userId, AuthRequest req) {
        String status = "PENDING";
        return assignedAdmin()
                .switchIfEmpty(Mono.error(new IllegalStateException("no assigned admin found")))
                .flatMap(assignedAdmin -> {
                    AuthRequestEntity auth = AuthRequestEntity.builder()
                            .id(UUID.randomUUID().toString())
                            .applicantUserId(userId)
                            .proceedingAdminId(assignedAdmin)
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

                    return Mono.fromCallable(() -> authRequestRepository.save(auth))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(saved -> {
                                SendInfoRequest info = new SendInfoRequest();
                                info.setTargetGroup(assignedAdmin);
                                info.setTitle("门户认证申请通知");
                                info.setContent("用户(" + userId + ")申请门户认证");
                                return sendInformation(info).then(Mono.just(status));
                            });
                });
    }

    

    @Override
    public List<AuthDto> listAuthentications(String userId) {
        List<AuthRequestEntity> ents = authRequestRepository.findByProceedingAdminIdOrderByCreatedAtDesc(userId);
        List<AuthDto> res = new ArrayList<>(ents.size());
        for (AuthRequestEntity e : ents) {
            String st = e.getStatus();
            if (st == null || !"PENDING".equalsIgnoreCase(st)) {
                continue;
            }
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
        AuthRequestEntity ent = authRequestRepository.findById(formId).orElseThrow(() -> new IllegalStateException("authentication form not found: " + formId));
        ent.setStatus(req.getStatus());
        authRequestRepository.save(ent);

        SendInfoRequest info = new SendInfoRequest();
        info.setTargetGroup(ent.getApplicantUserId());
        info.setTitle("门户认证申请处理结果通知");
        info.setContent("申请结果: " + req.getStatus() + "\n备注: " + req.getRemarks());
        sendInformation(info).subscribe();

        return req.getStatus();
    }

    @Override
    public Mono<String> createReport(String reporterId, ReportRequest req) {
        String status = "PENDING";
        return assignedAdmin()
                .switchIfEmpty(Mono.error(new IllegalStateException("no assigned admin found")))
                .flatMap(assignedAdmin -> {
                    ReportEntity r = ReportEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .proceedingAdminId(assignedAdmin)
                        .reporterId(reporterId)
                        .type(req.getType())
                        .targetId(req.getTargetId())
                        .reason(req.getReason())
                        .status(status)
                        .build();

                    return Mono.fromCallable(() -> reportRepository.save(r))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(saved -> {
                                SendInfoRequest info = new SendInfoRequest();
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
                                return sendInformation(info).then(Mono.just(status));
                            });
                });
    }

    @Override
    public List<ReportDto> listReports(String userId) {
        List<ReportEntity> ents = reportRepository.findByProceedingAdminIdOrderByCreatedAtDesc(userId);
        if (ents == null || ents.isEmpty()) {
            throw new IllegalStateException("no reports found" + userId);
        }
        List<ReportDto> res = new ArrayList<>(ents.size());
        for (ReportEntity e : ents) {
            String st = e.getStatus();
            if (st == null || !"PENDING".equalsIgnoreCase(st)) {
                continue;
            }
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
        ReportEntity ent = reportRepository.findById(reportId).orElseThrow(() -> new IllegalStateException("report not found: " + reportId));
        ent.setStatus(req.getStatus());
        reportRepository.save(ent);

        SendInfoRequest info = new SendInfoRequest();
        info.setTargetGroup(ent.getReporterId());
        info.setTitle("举报处理结果通知");
        info.setContent("举报处理结果: " + req.getStatus() + "\n备注: " + req.getRemarks());
        sendInformation(info).subscribe();

        return req.getStatus();
    }

    @Override
    public Mono<Void> sendInformation(SendInfoRequest req) {
        if (req.getTargetGroup() == null || req.getTargetGroup().isEmpty()) {
            return Mono.error(new IllegalArgumentException("Target group cannot be null or empty"));
        }
        String target = req.getTargetGroup();

        Mono<Message> savedMono = Mono.fromCallable(() -> {
            Message msg = new Message();
            msg.setUserId(target);
            msg.setContent(req.getContent());
            msg.setTitle(req.getTitle());
            return messageRepository.save(msg);
        }).subscribeOn(Schedulers.boundedElastic());

        Mono<List<String>> recipientsMono;
        if ("ALL".equals(target)) {
            recipientsMono = fetchAllUsers()
                    .map(list -> list.stream().map(m -> m.get("userId")).collect(Collectors.toList()));
        } else if (target.startsWith("GROUP_")) {
            String groupName = target.substring("GROUP_".length());
            recipientsMono = fetchUsersByGroup(groupName)
                    .map(list -> list.stream().map(m -> m.get("userId")).collect(Collectors.toList()));
        } else {
            recipientsMono = fetchUserById(target)
                    .map(m -> List.of(m.get("userId")));
        }

        return savedMono.flatMapMany(saved ->
                recipientsMono.flatMapMany(Flux::fromIterable)
                        .flatMap(userId -> Mono.fromCallable(() -> {
                            UserMessageState state = UserMessageState.builder()
                                    .id(UUID.randomUUID().toString())
                                    .userId(userId)
                                    .messageId(saved.getId())
                                    .state("UNREAD")
                                    .build();
                            return stateRepository.save(state);
                        }).subscribeOn(Schedulers.boundedElastic()))
                        .then()
        ).then();
    }

    private Mono<Map<String, String>> fetchUserById(String userId) {
        return userWebClient.get()
                .uri("/api/users/{userId}", userId)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).flatMap(body -> Mono.error(new IllegalStateException("user-service error: " + resp.statusCode() + " " + body))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .flatMap(resp -> {
                    Object d = resp.get("data");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> obj = (Map<String, Object>) d;
                    Map<String, String> map = new HashMap<>();
                    for (Map.Entry<String, Object> e : obj.entrySet()) map.put(e.getKey(), e.getValue() == null ? null : String.valueOf(e.getValue()));
                    return Mono.just(map);
                })
                .timeout(Duration.ofSeconds(10));
    }

    private Mono<String> assignedAdmin() {
        return fetchUsersByGroup("ADMIN")
                .flatMap(list -> {
                    if (list == null || list.isEmpty()) return Mono.empty();
                    List<String> choices = list.stream().map(m -> m.get("userId")).collect(Collectors.toList());
                    String selected = choices.get(ThreadLocalRandom.current().nextInt(choices.size()));
                    return Mono.just(selected);
                });
    }

    private Mono<List<Map<String, String>>> fetchAllUsers() {
        return userWebClient.get()
                .uri("/api/users")
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).flatMap(body -> Mono.error(new IllegalStateException("user-service error: " + resp.statusCode() + " " + body))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .flatMap(resp -> {
                    Object d = resp.get("data");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> records = (Map<String, Object>) d;
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> objList = (List<Map<String, Object>>) records.get("records");
                    List<Map<String, String>> converted = objList.stream().map(m -> {
                        Map<String, String> map = new HashMap<>();
                        for (Map.Entry<String, Object> e : m.entrySet()) map.put(e.getKey(), e.getValue() == null ? null : String.valueOf(e.getValue()));
                        return map;
                    }).collect(Collectors.toList());
                    return Mono.just(converted);
                })
                .timeout(Duration.ofSeconds(10));
    }

    private Mono<List<Map<String, String>>> fetchUsersByGroup(String group) {
        return userWebClient.get()
                .uri("/api/users/role/{role}", group)
                .retrieve()
                .onStatus(status -> !status.is2xxSuccessful(), resp -> resp.bodyToMono(String.class).flatMap(body -> Mono.error(new IllegalStateException("user-service error: " + resp.statusCode() + " " + body))))
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                })
                .flatMap(resp -> {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> d = (Map<String, Object>) resp.get("data");
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> objList = (List<Map<String, Object>>) d.get("records");
                    List<Map<String, String>> converted = objList.stream().map(m -> {
                        Map<String, String> map = new HashMap<>();
                        for (Map.Entry<String, Object> e : m.entrySet()) map.put(e.getKey(), e.getValue() == null ? null : String.valueOf(e.getValue()));
                        return map;
                    }).collect(Collectors.toList());
                    return Mono.just(converted);
                })
                .timeout(Duration.ofSeconds(10));
    }

    @Override
    public void readInformation(String userId, String id) {
        UserMessageState s = stateRepository.findByUserIdAndMessageId(userId, id);
        s.setState("READ");
        stateRepository.save(s);
    }

    @Override
    public void deleteInformation(String id) {
        if (messageRepository.existsById(id)) {
            messageRepository.deleteById(id);
        }
        List<UserMessageState> states = stateRepository.findByMessageId(id);
        if (states != null && !states.isEmpty()) {
            for (UserMessageState s : states) {
                s.setState("DELETED");
            }
            stateRepository.saveAll(states);
        }
    }

    @Override
    @Transactional
    public void deletePersonalInformation(String userId, String id) {
        UserMessageState s = stateRepository.findByUserIdAndMessageId(userId, id);
        if (s != null) {
            stateRepository.delete(s);
        }
    }

    @Override
    public List<InformationDto> getInformation(String userId) {
        List<UserMessageState> states = stateRepository.findByUserIdOrderByUpdatedAtDesc(userId);
        List<InformationDto> res = new ArrayList<>(states.size());
        for (UserMessageState s : states) {
            Message ent = messageRepository.findById(s.getMessageId()).orElseThrow(() -> new IllegalStateException("message not found: " + s.getMessageId()));
            InformationDto dto = new InformationDto();
            dto.setId(ent.getId());
            dto.setTitle(ent.getTitle());
            dto.setContent(ent.getContent());
            dto.setState(s.getState());
            dto.setUpdatedAt(ent.getCreatedAt().toString());
            res.add(dto);
        }
        return res;
    }

    @Override
    public Mono<String> applyAchievement(String userId, String achievementId) {
        String status = "PENDING";
        return assignedAdmin()
                .switchIfEmpty(Mono.error(new IllegalStateException("no assigned admin found")))
                .flatMap(assignedAdmin -> {
                    AchievementEntity r = AchievementEntity.builder()
                        .id(UUID.randomUUID().toString())
                        .proceedingAdminId(assignedAdmin)
                        .userId(userId)
                        .achievementId(achievementId)
                        .status(status)
                        .build();

                    return Mono.fromCallable(() -> achievementRepository.save(r))
                            .subscribeOn(Schedulers.boundedElastic())
                            .flatMap(saved -> {
                                SendInfoRequest info = new SendInfoRequest();
                                info.setTargetGroup(assignedAdmin);
                                info.setTitle("成果上传审核通知");
                                info.setContent("用户(" + userId + ")申请审核成果(" + achievementId + ")");
                                return sendInformation(info).then(Mono.just(status));
                            });
                });
    }

    @Override
    public List<AchievementDto> getAchievementReview(String userId) {
        List<AchievementEntity> ents = achievementRepository.findByProceedingAdminIdOrderByCreatedAtDesc(userId);
        if (ents == null || ents.isEmpty()) {
            throw new IllegalStateException("no achievements found" + userId);
        }
        List<AchievementDto> res = new ArrayList<>(ents.size());
        for (AchievementEntity e : ents) {
            String st = e.getStatus();
            if (st == null || !"PENDING".equalsIgnoreCase(st)) {
                continue;
            }
            AchievementDto d = AchievementDto.builder()
                    .achievementId(e.getAchievementId())
                    .createdAt(e.getCreatedAt().toString())
                    .build();
            res.add(d);
        }
        return res;
    }

    @Override
    public List<AchievementDto> getAchievement(String userId) {
        List<AchievementEntity> ents = achievementRepository.findByUserIdOrderByCreatedAtDesc(userId);
        if (ents == null || ents.isEmpty()) {
            throw new IllegalStateException("no achievements found" + userId);
        }
        List<AchievementDto> res = new ArrayList<>(ents.size());
        for (AchievementEntity e : ents) {
            AchievementDto d = AchievementDto.builder()
                    .achievementId(e.getAchievementId())
                    .createdAt(e.getCreatedAt().toString())
                    .build();
            res.add(d);
        }
        return res;
    }

    @Override
    public String processAchievement(String formId, ProcessRequest req) {
        AchievementEntity ent = achievementRepository.findById(formId).orElseThrow(() -> new IllegalStateException("achievement form not found: " + formId));
        ent.setStatus(req.getStatus());
        achievementRepository.save(ent);

        SendInfoRequest info = new SendInfoRequest();
        info.setTargetGroup(ent.getUserId());
        info.setTitle("成果审核处理结果通知");
        info.setContent("审核结果: " + req.getStatus() + "\n备注: " + req.getRemarks());
        sendInformation(info).subscribe();

        return req.getStatus();
    }

    @Override
    public String checkAchievement(String achievementId) {
        AchievementEntity ent = achievementRepository.findById(achievementId).orElse(null);
        if (ent != null && ent.getStatus().equals("PENDING")) return "PENDING";
        return null;
    }
}