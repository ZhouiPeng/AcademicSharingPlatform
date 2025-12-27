package com.academic.achievement.service.impl;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import com.academic.achievement.dto.AchievementDto;
import com.academic.achievement.dto.AchievementFilterRequest;
import com.academic.achievement.dto.CollectionFolderDto;
import com.academic.achievement.entity.AchievementEntity;
import com.academic.achievement.entity.FolderEntity;
import com.academic.achievement.repository.AchievementRepository;
import com.academic.achievement.repository.FolderRepository;
import com.academic.achievement.service.AchievementService;

@Service
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final FolderRepository folderRepository;
    private final StringRedisTemplate redis;

    private final DefaultRedisScript<Long> decrIfPositiveScript;

    public AchievementServiceImpl(AchievementRepository achievementRepository, FolderRepository folderRepository, StringRedisTemplate redis) {
        this.achievementRepository = achievementRepository;
        this.folderRepository = folderRepository;
        this.redis = redis;
        this.decrIfPositiveScript = new DefaultRedisScript<>();
        this.decrIfPositiveScript.setScriptText("local v = redis.call('get', KEYS[1]); if (not v) or (tonumber(v) <= 0) then return tonumber(v) or 0; else return redis.call('decr', KEYS[1]); end");
        this.decrIfPositiveScript.setResultType(Long.class);
    }

    @Override
    public String upload(AchievementDto dto) {
        AchievementEntity e = toEntity(dto);
        if (e.getId() == null || e.getId().isEmpty()) e.setId("ach-" + System.currentTimeMillis());
        if (e.getCreatedAt() == null) e.setCreatedAt(System.currentTimeMillis());
        achievementRepository.save(e);
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
        if (opt.isEmpty()) return;
        AchievementEntity e = opt.get();
        if (dto.getTitle() != null) e.setTitle(dto.getTitle());
        if (dto.getUserId() != null) e.setAuthorId(dto.getUserId());
        if (dto.getFileId() != null) e.setFileId(dto.getFileId());
        if (dto.getAuthors() != null) e.setAuthors(String.join(",", dto.getAuthors()));
        if (dto.getType() != null) e.setType(dto.getType());
        if (dto.getAbstractText() != null) e.setAbstractText(dto.getAbstractText());
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
    public String generateDownloadLink(String achId) {
        // use Redis INCR for atomic increment, then persist to DB
        String key = String.format("achievement:%s:downloads", achId);
        Long newVal = redis.opsForValue().increment(key, 1);
        // only increment in Redis; periodic flush will persist to DB
        return String.format("/internal/files/%s/download?ts=%d", achId, Instant.now().toEpochMilli());
    }

    @Override
    public CollectionFolderDto createFolder(CollectionFolderDto dto) {
        FolderEntity f = new FolderEntity();
        if (dto.getId() == null || dto.getId().isEmpty()) f.setId("folder-" + System.currentTimeMillis());
        else f.setId(dto.getId());
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
        if (optA.isEmpty() || optF.isEmpty()) return;
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
        if (optA.isEmpty() || optF.isEmpty()) return;
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
            return achievementRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
        }
        final String keywordLower = keyword.toLowerCase();
        return achievementRepository.findAll().stream()
            .filter(e -> containsIgnoreCase(e.getTitle(), keywordLower)
                || containsIgnoreCase(e.getAuthors(), keywordLower)
                || containsIgnoreCase(e.getAbstractText(), keywordLower))
            .map(this::toDto)
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
                .collect(Collectors.toList());
    }

    @Override
    public List<AchievementDto> listByCategory(String catId) {
        AchievementFilterRequest criteria = new AchievementFilterRequest();
        criteria.setClassification(catId);
        return filter(criteria);
    }

    @Override
    public List<AchievementDto> searchWithSort(String sortBy, String order) {
        List<AchievementDto> list = search(null);
        if (list.isEmpty()) return list;

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

    private AchievementDto toDto(AchievementEntity e) {
        AchievementDto d = new AchievementDto();
        d.setId(e.getId());
        d.setTitle(e.getTitle());
        d.setUserId(e.getAuthorId());
        d.setFileId(e.getFileId());
        if (e.getAuthors() != null && !e.getAuthors().isEmpty()) d.setAuthors(List.of(e.getAuthors().split(",")));
        if (e.getType() != null) d.setType(e.getType());
        d.setAbstractText(e.getAbstractText());
        d.setCreatedAt(e.getCreatedAt());
        // map categories stored as comma-separated string to DTO list
        if (e.getCategories() != null && !e.getCategories().isEmpty()) {
            d.setCategories(List.of(e.getCategories().split("\\s*,\\s*")));
        }
        // populate counts if stored in entity (Redis may hold latest counts)
        if (e.getDownloadCount() != null) d.setDownloadCount(e.getDownloadCount());
        if (e.getCollectCount() != null) d.setCollectCount(e.getCollectCount());
        return d;
    }

    private AchievementEntity toEntity(AchievementDto d) {
        AchievementEntity e = new AchievementEntity();
        e.setId(d.getId());
        e.setTitle(d.getTitle());
        e.setAuthorId(d.getUserId());
        e.setFileId(d.getFileId());
        if (d.getAuthors() != null && !d.getAuthors().isEmpty()) e.setAuthors(String.join(",", d.getAuthors()));
        if (d.getCategories() != null && !d.getCategories().isEmpty()) e.setCategories(String.join(",", d.getCategories()));
        if (d.getType() != null) e.setType(d.getType());
        e.setAbstractText(d.getAbstractText());
        return e;
    }

    private boolean containsIgnoreCase(String source, String keywordLower) {
        if (source == null || source.isEmpty()) return false;
        return source.toLowerCase().contains(keywordLower);
    }

    private boolean matchKeywords(AchievementEntity entity, String keyword) {
        if (keyword == null || keyword.isEmpty()) return true;
        String lower = keyword.toLowerCase();
        return containsIgnoreCase(entity.getTitle(), lower)
                || containsIgnoreCase(entity.getAuthors(), lower)
                || containsIgnoreCase(entity.getAbstractText(), lower);
    }

    private boolean matchClassification(AchievementEntity entity, String classification) {
        if (classification == null || classification.isEmpty()) return true;
        String categories = entity.getCategories();
        return categories != null && categories.contains(classification);
    }

    private boolean matchYearRange(AchievementEntity entity, Integer fromYear, Integer toYear) {
        if (fromYear == null && toYear == null) return true;
        if (entity.getCreatedAt() == null) return false;
        Integer createdYear = extractYear(entity.getCreatedAt());
        if (createdYear == null) return false;
        if (fromYear != null && createdYear < fromYear) return false;
        if (toYear != null && createdYear > toYear) return false;
        return true;
    }

    private Integer extractYear(Long epochMillis) {
        try {
            return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).getYear();
        } catch (Exception ex) {
            return null;
        }
    }
}
