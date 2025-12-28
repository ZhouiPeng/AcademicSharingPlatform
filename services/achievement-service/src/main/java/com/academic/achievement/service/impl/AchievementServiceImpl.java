package com.academic.achievement.service.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.academic.achievement.dto.AchievementDto;
import com.academic.achievement.dto.AchievementFilterRequest;
import com.academic.achievement.dto.CollectionFolderDto;
import com.academic.achievement.entity.AchievementEntity;
import com.academic.achievement.entity.FolderEntity;
import com.academic.achievement.repository.AchievementRepository;
import com.academic.achievement.repository.FolderRepository;
import com.academic.achievement.service.AchievementService;

import reactor.core.publisher.Mono;

@Service
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final FolderRepository folderRepository;
    private final StringRedisTemplate redis;
    private final DefaultRedisScript<Long> decrIfPositiveScript;
    private static final int AUTHORS_MAX_LEN = 255;
    private final WebClient adminWebClient;
    private static final Logger logger = LoggerFactory.getLogger(AchievementServiceImpl.class);

    public AchievementServiceImpl(AchievementRepository achievementRepository, FolderRepository folderRepository, StringRedisTemplate redis, @Value("http://admin-service:8085") String adminServiceUrl) {
        this.achievementRepository = achievementRepository;
        this.folderRepository = folderRepository;
        this.redis = redis;
        this.decrIfPositiveScript = new DefaultRedisScript<>();
        this.decrIfPositiveScript.setScriptText("local v = redis.call('get', KEYS[1]); if (not v) or (tonumber(v) <= 0) then return tonumber(v) or 0; else return redis.call('decr', KEYS[1]); end");
        this.decrIfPositiveScript.setResultType(Long.class);
        this.adminWebClient = WebClient.builder().baseUrl(adminServiceUrl).build();
    }

    @Override
    public String upload(AchievementDto dto, String userRoleHeader) {
        // duplicate detection: same title AND (same userId OR overlapping authors)
        String title = dto.getTitle() == null ? "" : dto.getTitle().trim();
        if (!title.isEmpty()) {
            java.util.List<com.academic.achievement.entity.AchievementEntity> candidates = achievementRepository.findByTitleContainingIgnoreCase(title);
            for (com.academic.achievement.entity.AchievementEntity cand : candidates) {
                if (cand.getTitle() == null) {
                    continue;
                }
                if (!cand.getTitle().equalsIgnoreCase(title)) {
                    continue;
                }
                // check same userId
                if (dto.getUserId() != null && dto.getUserId().equals(cand.getAuthorId())) {
                    throw new com.academic.achievement.service.DuplicateAchievementException("检测到重复：相同标题且上传用户相同");
                }
                // check overlapping authors
                if (dto.getAuthors() != null && !dto.getAuthors().isEmpty() && cand.getAuthors() != null && !cand.getAuthors().isEmpty()) {
                    java.util.Set<String> existing = java.util.Arrays.stream(cand.getAuthors().split(","))
                            .map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toSet());
                    for (String a : dto.getAuthors()) {
                        if (existing.contains(a)) {
                            throw new com.academic.achievement.service.DuplicateAchievementException("检测到重复：相同标题且作者重合");
                        }
                    }
                }
            }
        }

        AchievementEntity e = toEntity(dto);
        if (e.getId() == null || e.getId().isEmpty()) {
            e.setId("ach-" + System.currentTimeMillis());
        }
        if (e.getCreatedAt() == null) {
            e.setCreatedAt(System.currentTimeMillis());
        }
        achievementRepository.save(e);
        if (!userRoleHeader.equals("ADMIN")) {
            applyReview(e.getId(), dto.getUserId());
        }
        return e.getId();
    }

    @Override
    public AchievementDto get(String achId) {
        Optional<AchievementEntity> opt = achievementRepository.findById(achId);
        return opt.map(this::toDto).orElse(null);
    }

    @Override
    public void update(String achId, AchievementDto dto) {
        Optional<AchievementEntity> opt = achievementRepository.findById(achId);
        if (opt.isEmpty()) {
            return;
        }
        AchievementEntity e = opt.get();
        if (dto.getTitle() != null) {
            e.setTitle(dto.getTitle());
        }
        if (dto.getUserId() != null) {
            e.setAuthorId(dto.getUserId());
        }
        if (dto.getFileId() != null) {
            e.setFileId(dto.getFileId());
        }
        if (dto.getAuthors() != null) {
            e.setAuthors(limitLen(String.join(",", dto.getAuthors()), AUTHORS_MAX_LEN));
        }
        if (dto.getCategories() != null) {
            e.setCategories(String.join(",", dto.getCategories()));
        }
        if (dto.getType() != null) {
            e.setType(dto.getType());
        }
        if (dto.getAbstractText() != null) {
            e.setAbstractText(dto.getAbstractText());
        }
        achievementRepository.save(e);
    }

    @Override
    public void delete(String achId) {
        achievementRepository.deleteById(achId);
    }

    @Override
    public List<AchievementDto> listByAuthor(String authorId) {
        return achievementRepository.findByUserId(authorId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public void cite(String achId) {
        String key = String.format("achievement:%s:citeds", achId);
        try {
            redis.opsForValue().increment(key, 1);
        } catch (Exception ex) {
            // fallback: update DB directly if Redis is unavailable
            Optional<AchievementEntity> opt = achievementRepository.findById(achId);
            if (opt.isPresent()) {
                AchievementEntity e = opt.get();
                Integer curr = e.getCitedCount();
                e.setCitedCount(curr == null ? 1 : curr + 1);
                achievementRepository.save(e);
            }
        }
    }

    @Override
    public CollectionFolderDto createFolder(CollectionFolderDto dto) {
        FolderEntity f = new FolderEntity();
        if (dto.getId() == null || dto.getId().isEmpty()) {
            f.setId("folder-" + System.currentTimeMillis());
        } else {
            f.setId(dto.getId());
        }
        f.setName(dto.getName());
        f.setDescription(dto.getDescription());
        FolderEntity saved = folderRepository.save(f);
        CollectionFolderDto out = new CollectionFolderDto();
        out.setId(saved.getId());
        out.setName(saved.getName());
        out.setDescription(saved.getDescription());
        return out;
    }

    @Override
    public void collect(String achId, String folderId) {
        Optional<AchievementEntity> optA = achievementRepository.findById(achId);
        Optional<FolderEntity> optF = folderRepository.findById(folderId);
        if (optA.isEmpty() || optF.isEmpty()) {
            return;
        }
        AchievementEntity a = optA.get();
        FolderEntity f = optF.get();
        a.getFolders().add(f);
        // persist relation change immediately, but keep counter in Redis
        achievementRepository.save(a);
        String key = String.format("achievement:%s:collects", achId);
        redis.opsForValue().increment(key, 1);
    }

    @Override
    public void uncollect(String achId, String folderId) {
        Optional<AchievementEntity> optA = achievementRepository.findById(achId);
        Optional<FolderEntity> optF = folderRepository.findById(folderId);
        if (optA.isEmpty() || optF.isEmpty()) {
            return;
        }
        AchievementEntity a = optA.get();
        FolderEntity f = optF.get();
        a.getFolders().remove(f);
        // persist relation change immediately, but decrement counter in Redis only
        achievementRepository.save(a);
        String key = String.format("achievement:%s:collects", achId);
        redis.execute(decrIfPositiveScript, Collections.singletonList(key));
    }

    @Override
    public void deleteFolder(String folderId) {
        // when deleting a folder, decrement collectCount for all achievements inside
        Optional<FolderEntity> optF = folderRepository.findById(folderId);
        if (optF.isPresent()) {
            // use repository method to find related achievements
            List<AchievementEntity> related = achievementRepository.findByFolders_Id(folderId);
            for (AchievementEntity a : related) {
                String key = String.format("achievement:%s:collects", a.getId());
                redis.execute(decrIfPositiveScript, Collections.singletonList(key));
                a.getFolders().removeIf(ff -> folderId.equals(ff.getId()));
                achievementRepository.save(a);
            }
        }
        folderRepository.deleteById(folderId);
    }

    @Override
    public List<CollectionFolderDto> listCollections() {
        return folderRepository.findAll().stream().map(f -> {
            CollectionFolderDto dto = new CollectionFolderDto();
            dto.setId(f.getId());
            dto.setName(f.getName());
            dto.setDescription(f.getDescription());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<AchievementDto> search(String q) {
        String keyword = q == null ? "" : q.trim();
        if (keyword.isEmpty()) {
            return achievementRepository.findAll().stream()
                    .map(this::toDto)
                    .filter(d -> d != null && d.getId() != null && !searchFromReview(d.getId()))
                    .collect(Collectors.toList());
        }
        final String keywordLower = keyword.toLowerCase();
        return achievementRepository.findAll().stream()
                .filter(e -> containsIgnoreCase(e.getTitle(), keywordLower)
                || containsIgnoreCase(e.getAuthors(), keywordLower)
                || containsIgnoreCase(e.getAbstractText(), keywordLower))
                .map(this::toDto)
                .filter(d -> d != null && d.getId() != null && !searchFromReview(d.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AchievementDto> filter(AchievementFilterRequest filterRequest) {
        AchievementFilterRequest criteria = filterRequest == null ? new AchievementFilterRequest() : filterRequest;
        String keyword = criteria.getKeywords() == null ? null : criteria.getKeywords().trim();
        String classification = criteria.getClassification() == null ? null : criteria.getClassification().trim();
        Integer fromYear = criteria.getFromYear();
        Integer toYear = criteria.getToYear();

        return achievementRepository.findAll().stream()
                .filter(e -> matchKeywords(e, keyword))
                .filter(e -> matchClassification(e, classification))
                .filter(e -> matchYearRange(e, fromYear, toYear))
                .map(this::toDto)
                .filter(d -> d != null && d.getId() != null && !searchFromReview(d.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AchievementDto> listByCategory(String catId) {
        AchievementFilterRequest criteria = new AchievementFilterRequest();
        criteria.setClassification(catId);
        return filter(criteria);
    }

    @Override
    public List<AchievementDto> listByFolder(String folderId) {
        return achievementRepository.findByFolders_Id(folderId).stream()
                .map(this::toDto)
                .filter(d -> d != null && d.getId() != null && !searchFromReview(d.getId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<AchievementDto> searchWithSort(String sortBy, String order) {
        List<AchievementDto> list = search(null);
        if (list.isEmpty()) {
            return list;
        }

        String normalizedSort = sortBy == null ? "date" : sortBy.trim().toLowerCase();
        Comparator<AchievementDto> comparator;
        switch (normalizedSort) {
            case "title":
                comparator = Comparator.comparing(dto -> dto.getTitle() == null ? "" : dto.getTitle(), String.CASE_INSENSITIVE_ORDER);
                break;
            case "date":
            case "createdat":
            default:
                comparator = Comparator.comparing(dto -> dto.getCreatedAt() == null ? 0L : dto.getCreatedAt());
                break;
        }

        if (!"asc".equalsIgnoreCase(order)) {
            comparator = comparator.reversed();
        }

        list.sort(comparator);
        return list;
    }

    private void applyReview(String achId, String userId) {
        try {
            java.util.Map<String, String> body = java.util.Collections.singletonMap("achievementId", achId);
            adminWebClient.post()
                    .uri("/api/admin/achievement")
                    .header("X-User-Id", userId == null ? "" : userId)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(Void.class)
                    .onErrorResume(e -> {
                        logger.warn("applyReview failed for {}: {}", achId, e.getMessage());
                        return Mono.empty();
                    })
                    .subscribe();
        } catch (Exception ex) {
            logger.warn("applyReview invocation failed for {}: {}", achId, ex.getMessage());
        }
    }

    @Override
    public Mono<java.util.List<AchievementDto>> getReviews(String userIdHeader, String userRoleHeader) {
        ParameterizedTypeReference<java.util.Map<String, Object>> typeRef = new ParameterizedTypeReference<>() {
        };
        return adminWebClient.get()
                .uri("/api/admin/achievement")
                .header("X-User-Id", userIdHeader == null ? "" : userIdHeader)
                .header("X-User-Role", userRoleHeader == null ? "" : userRoleHeader)
                .retrieve()
                .bodyToMono(typeRef)
                .onErrorReturn(java.util.Collections.emptyMap())
                .flatMap(resp -> reactor.core.publisher.Mono.fromCallable(() -> {
            if (resp == null || resp.isEmpty()) {
                return java.util.Collections.<AchievementDto>emptyList();
            }
            Object dataObj = resp.get("data");
            if (!(dataObj instanceof java.util.List)) {
                return java.util.Collections.<AchievementDto>emptyList();
            }
            @SuppressWarnings("unchecked")
            java.util.List<Object> dataList = (java.util.List<Object>) dataObj;
            java.util.List<AchievementDto> out = new java.util.ArrayList<>();
            for (Object itemObj : dataList) {
                if (!(itemObj instanceof java.util.Map)) {
                    continue;
                }
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> item = (java.util.Map<String, Object>) itemObj;
                Object achIdObj = item.get("achievementId");
                if (achIdObj == null) {
                    continue;
                }
                String achId = String.valueOf(achIdObj);
                if (achId.isBlank()) {
                    continue;
                }
                Optional<AchievementEntity> opt = achievementRepository.findById(achId);
                opt.ifPresent(e -> out.add(toDto(e)));
            }
            return out;
        }).subscribeOn(reactor.core.scheduler.Schedulers.boundedElastic()))
                .onErrorReturn(java.util.Collections.emptyList());
    }

    private AchievementDto toDto(AchievementEntity e) {
        AchievementDto d = new AchievementDto();
        d.setId(e.getId());
        d.setTitle(e.getTitle());
        d.setUserId(e.getAuthorId());
        d.setFileId(e.getFileId());
        if (e.getAuthors() != null && !e.getAuthors().isEmpty()) {
            d.setAuthors(List.of(e.getAuthors().split(",")));
        }
        if (e.getCategories() != null && !e.getCategories().isEmpty()) {
            d.setCategories(List.of(e.getCategories().split(",")));
        }
        if (e.getType() != null) {
            d.setType(e.getType());
        }
        d.setAbstractText(e.getAbstractText());
        d.setCreatedAt(e.getCreatedAt());
        // map categories stored as comma-separated string to DTO list
        if (e.getCategories() != null && !e.getCategories().isEmpty()) {
            d.setCategories(List.of(e.getCategories().split("\\s*,\\s*")));
        }
        // populate counts if stored in entity (Redis may hold latest counts)
        if (e.getDownloadCount() != null) {
            d.setDownloadCount(e.getDownloadCount());
        }
        if (e.getCollectCount() != null) {
            d.setCollectCount(e.getCollectCount());
        }
        if (e.getCitedCount() != null) {
            d.setCitedCount(e.getCitedCount());
        }

        return d;
    }

    private AchievementEntity toEntity(AchievementDto d) {
        AchievementEntity e = new AchievementEntity();
        e.setId(d.getId());
        e.setTitle(d.getTitle());
        e.setAuthorId(d.getUserId());
        e.setFileId(d.getFileId());

        if (d.getAuthors() != null && !d.getAuthors().isEmpty()) {
            e.setAuthors(String.join(",", d.getAuthors()));
        }
        if (d.getCategories() != null && !d.getCategories().isEmpty()) {
            e.setCategories(String.join(",", d.getCategories()));
        }
        if (d.getType() != null) {
            e.setType(d.getType());
        }
        e.setAbstractText(d.getAbstractText());
        return e;
    }

    private static String limitLen(String s, int maxLen) {
        if (s == null) {
            return null;
        }
        if (maxLen <= 0) {
            return "";
        }
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private boolean containsIgnoreCase(String source, String keywordLower) {
        if (source == null || source.isEmpty()) {
            return false;
        }
        return source.toLowerCase().contains(keywordLower);
    }

    private boolean matchKeywords(AchievementEntity entity, String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return true;
        }
        String lower = keyword.toLowerCase();
        return containsIgnoreCase(entity.getTitle(), lower)
                || containsIgnoreCase(entity.getAuthors(), lower)
                || containsIgnoreCase(entity.getAbstractText(), lower);
    }

    private boolean matchClassification(AchievementEntity entity, String classification) {
        if (classification == null || classification.isEmpty()) {
            return true;
        }
        String categories = entity.getCategories();
        return categories != null && categories.contains(classification);
    }

    private boolean matchYearRange(AchievementEntity entity, Integer fromYear, Integer toYear) {
        if (fromYear == null && toYear == null) {
            return true;
        }
        if (entity.getCreatedAt() == null) {
            return false;
        }
        Integer createdYear = extractYear(entity.getCreatedAt());
        if (createdYear == null) {
            return false;
        }
        if (fromYear != null && createdYear < fromYear) {
            return false;
        }
        if (toYear != null && createdYear > toYear) {
            return false;
        }
        return true;
    }

    private Integer extractYear(Long epochMillis) {
        try {
            return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).getYear();
        } catch (Exception ex) {
            return null;
        }
    }

    private Boolean searchFromReview(String achId) {
        if (achId == null || achId.isBlank()) {
            return false;
        }
        try {
            java.util.Map<String, String> body = java.util.Collections.singletonMap("achievementId", achId);
            ParameterizedTypeReference<java.util.Map<String, Object>> typeRef = new ParameterizedTypeReference<>() {
            };
            java.util.Map<String, Object> resp = adminWebClient.method(org.springframework.http.HttpMethod.GET)
                    .uri("/api/admin/achievement/check")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(typeRef)
                    .onErrorReturn(java.util.Collections.emptyMap())
                    .block();

            if (resp == null || resp.isEmpty()) {
                return false;
            }
            Object dataObj = resp.get("data");
            if (!(dataObj instanceof java.util.Map)) {
                return false;
            }
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) dataObj;
            Object status = data.get("status");
            return status != null;
        } catch (Exception ex) {
            logger.warn("searchFromReview failed for {}: {}", achId, ex.getMessage());
            return false;
        }
    }
}
