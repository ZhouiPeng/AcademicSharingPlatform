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
    private final WebClient userWebClient;
    // simple per-service cache to avoid repeated remote lookups in same JVM call
    private final java.util.concurrent.ConcurrentHashMap<String, String> usernameCache = new java.util.concurrent.ConcurrentHashMap<>();

    // Defensive limit to avoid MySQL Data truncation when schema uses VARCHAR(255)
    private static final int AUTHORS_MAX_LEN = 255;
    private final WebClient adminWebClient;
    private static final Logger logger = LoggerFactory.getLogger(AchievementServiceImpl.class);

    public AchievementServiceImpl(AchievementRepository achievementRepository, FolderRepository folderRepository, StringRedisTemplate redis, WebClient.Builder webClientBuilder, @Value("http://admin-service:8085") String adminServiceUrl) {
        this.achievementRepository = achievementRepository;
        this.folderRepository = folderRepository;
        this.redis = redis;
        this.decrIfPositiveScript = new DefaultRedisScript<>();
        this.decrIfPositiveScript.setScriptText("local v = redis.call('get', KEYS[1]); if (not v) or (tonumber(v) <= 0) then return tonumber(v) or 0; else return redis.call('decr', KEYS[1]); end");
        this.decrIfPositiveScript.setResultType(Long.class);
        this.userWebClient = webClientBuilder.baseUrl("http://user-service:8081").build();
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
    public CollectionFolderDto createFolder(CollectionFolderDto dto, String ownerId) {
        FolderEntity f = new FolderEntity();
        if (dto.getId() == null || dto.getId().isEmpty()) {
            f.setId("folder-" + System.currentTimeMillis());
        } else {
            f.setId(dto.getId());
        }
        f.setName(dto.getName());
        f.setDescription(dto.getDescription());
        f.setOwnerId(ownerId);
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
        // persist relation change immediately
        achievementRepository.save(a);
        String key = String.format("achievement:%s:collects", achId);
        try {
            redis.opsForValue().increment(key, 1);
        } catch (Exception ex) {
            // Redis unavailable: fallback to update DB counter directly
            try {
                Integer curr = a.getCollectCount();
                a.setCollectCount(curr == null ? 1 : curr + 1);
                achievementRepository.save(a);
            } catch (Exception ignore) {
            }
        }
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
        // persist relation change immediately
        achievementRepository.save(a);
        String key = String.format("achievement:%s:collects", achId);
        try {
            redis.execute(decrIfPositiveScript, Collections.singletonList(key));
        } catch (Exception ex) {
            // Redis unavailable: fallback to update DB counter directly (ensure non-negative)
            try {
                Integer curr = a.getCollectCount();
                int next = (curr == null ? 0 : curr) - 1;
                if (next < 0) next = 0;
                a.setCollectCount(next);
                achievementRepository.save(a);
            } catch (Exception ignore) {
            }
        }
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
    public List<CollectionFolderDto> listCollections(String ownerId) {
        java.util.List<FolderEntity> folders;
        if (ownerId == null || ownerId.isBlank()) {
            folders = folderRepository.findAll();
        } else {
            folders = folderRepository.findByOwnerId(ownerId);
        }
        return folders.stream().map(f -> {
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
                .filter(this::keepIfNotReviewed)
                .collect(Collectors.toList());
    }

    @Override
    public List<AchievementDto> filter(AchievementFilterRequest filterRequest) {
        AchievementFilterRequest criteria = filterRequest == null ? new AchievementFilterRequest() : filterRequest;
        String keyword = criteria.getKeywords() == null ? null : criteria.getKeywords().trim();
        String classification = criteria.getClassification() == null ? null : criteria.getClassification().trim();
        Integer fromYear = criteria.getFromYear();
        Integer toYear = criteria.getToYear();

        String title = criteria.getTitle() == null ? null : criteria.getTitle().trim();
        String userId = criteria.getUserId() == null ? null : criteria.getUserId().trim();
        String fileId = criteria.getFileId() == null ? null : criteria.getFileId().trim();
        Integer type = criteria.getType();
        java.util.List<String> authors = criteria.getAuthors();
        java.util.List<String> categories = criteria.getCategories();
        return achievementRepository.findAll().stream()
                .filter(e -> matchKeywords(e, keyword))
                .filter(e -> matchClassification(e, classification))
                .filter(e -> matchYearRange(e, fromYear, toYear))
            .filter(e -> matchTitle(e, title))
                .filter(e -> matchUserId(e, userId))
                .filter(e -> matchFileId(e, fileId))
                .filter(e -> matchType(e, type))
                .filter(e -> matchAuthorsList(e, authors))
                .filter(e -> matchCategoriesList(e, categories))
                .map(this::toDto)
                .filter(this::keepIfNotReviewed)
                .collect(Collectors.toList());
    }

    private boolean matchTitle(AchievementEntity e, String title) {
        if (title == null || title.isEmpty()) return true;
        return containsIgnoreCase(e.getTitle(), title.toLowerCase());
    }

    private boolean matchUserId(AchievementEntity e, String userId) {
        if (userId == null || userId.isEmpty()) return true;
        String uid = e.getAuthorId();
        return uid != null && uid.equals(userId);
    }

    private boolean matchFileId(AchievementEntity e, String fileId) {
        if (fileId == null || fileId.isEmpty()) return true;
        String fid = e.getFileId();
        return fid != null && fid.equals(fileId);
    }

    private boolean matchType(AchievementEntity e, Integer type) {
        if (type == null) return true;
        return e.getType() != null && e.getType().equals(type);
    }

    private boolean matchAuthorsList(AchievementEntity e, java.util.List<String> authors) {
        if (authors == null || authors.isEmpty()) return true;
        if (e.getAuthors() == null || e.getAuthors().isEmpty()) return false;
        java.util.Set<String> existing = java.util.Arrays.stream(e.getAuthors().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toSet());
        for (String a : authors) {
            if (existing.contains(a)) return true;
        }
        return false;
    }

    private boolean matchCategoriesList(AchievementEntity e, java.util.List<String> categories) {
        if (categories == null || categories.isEmpty()) return true;
        if (e.getCategories() == null || e.getCategories().isEmpty()) return false;
        java.util.Set<String> existing = java.util.Arrays.stream(e.getCategories().split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).collect(java.util.stream.Collectors.toSet());
        for (String c : categories) {
            if (existing.contains(c)) return true;
        }
        return false;
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
                .filter(this::keepIfNotReviewed)
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
            java.util.List<String> raw = java.util.List.of(e.getAuthors().split(","));
            d.setAuthors(raw);
            // build presentation view by resolving username -> userId via user-service
            java.util.List<com.academic.achievement.dto.AuthorView> av = raw.stream().map(name -> {
                String uname = name == null ? null : name.trim();
                if (uname == null || uname.isEmpty()) return new com.academic.achievement.dto.AuthorView(uname, null);
                String cached = usernameCache.get(uname);
                if (cached != null) return new com.academic.achievement.dto.AuthorView(uname, cached.isEmpty() ? null : cached);
                try {
                    // call user-service lookup endpoint
                    java.util.Map resp = this.userWebClient.get()
                            .uri(uriBuilder -> uriBuilder.path("/api/users/lookup/{username}").build(uname))
                            .retrieve()
                            .bodyToMono(java.util.Map.class)
                            .block();
                    if (resp != null && resp.get("data") instanceof java.util.Map) {
                        Object uid = ((java.util.Map) resp.get("data")).get("userId");
                        String uids = uid == null ? "" : String.valueOf(uid);
                        usernameCache.put(uname, uids);
                        return new com.academic.achievement.dto.AuthorView(uname, uids.isEmpty() ? null : uids);
                    }
                } catch (Exception ex) {
                    // ignore, fallback to null userId
                }
                // store empty string as sentinel for "unknown" to avoid null values in ConcurrentHashMap
                usernameCache.putIfAbsent(uname, "");
                return new com.academic.achievement.dto.AuthorView(uname, null);
            }).collect(Collectors.toList());
            d.setAuthorsView(av);
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

    private boolean keepIfNotReviewed(AchievementDto d) {
        if (d == null || d.getId() == null) return false;
        try {
            return !searchFromReview(d.getId());
        } catch (Exception ex) {
            logger.warn("searchFromReview failed for {}: {}", d.getId(), ex.getMessage());
            // on error, keep the item instead of failing the whole request
            return true;
        }
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
