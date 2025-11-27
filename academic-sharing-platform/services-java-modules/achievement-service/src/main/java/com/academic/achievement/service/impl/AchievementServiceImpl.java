package com.academic.achievement.service.impl;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.academic.achievement.dto.AchievementDto;
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

    public AchievementServiceImpl(AchievementRepository achievementRepository, FolderRepository folderRepository) {
        this.achievementRepository = achievementRepository;
        this.folderRepository = folderRepository;
    }

    @Override
    public void upload(AchievementDto dto) {
        AchievementEntity e = toEntity(dto);
        if (e.getId() == null || e.getId().isEmpty()) e.setId("ach-" + System.currentTimeMillis());
        achievementRepository.save(e);
    }

    @Override
    public void uploadUncertified(AchievementDto dto) {
        if (dto.getAuthorId() == null) dto.setAuthorId("uncertified");
        upload(dto);
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
        if (dto.getAuthorId() != null) e.setAuthorId(dto.getAuthorId());
        if (dto.getFileId() != null) e.setFileId(dto.getFileId());
        if (dto.getCategories() != null) e.setCategories(String.join(",", dto.getCategories()));
        achievementRepository.save(e);
    }

    @Override
    public void delete(String achId) {
        achievementRepository.deleteById(achId);
    }

    @Override
    public List<AchievementDto> listByAuthor(String authorId) {
        return achievementRepository.findByAuthorId(authorId).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public String generateDownloadLink(String achId) {
        return String.format("/internal/files/%s/download?ts=%d", achId, Instant.now().toEpochMilli());
    }

    @Override
    public CollectionFolderDto createFolder(CollectionFolderDto dto) {
        FolderEntity f = new FolderEntity();
        if (dto.getId() == null || dto.getId().isEmpty()) f.setId("folder-" + System.currentTimeMillis());
        else f.setId(dto.getId());
        f.setName(dto.getName());
        FolderEntity saved = folderRepository.save(f);
        CollectionFolderDto out = new CollectionFolderDto();
        out.setId(saved.getId());
        out.setName(saved.getName());
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
        achievementRepository.save(a);
    }

    @Override
    public void uncollect(String achId, String folderId) {
        Optional<AchievementEntity> optA = achievementRepository.findById(achId);
        Optional<FolderEntity> optF = folderRepository.findById(folderId);
        if (optA.isEmpty() || optF.isEmpty()) return;
        AchievementEntity a = optA.get();
        FolderEntity f = optF.get();
        a.getFolders().remove(f);
        achievementRepository.save(a);
    }

    @Override
    public void deleteFolder(String folderId) {
        folderRepository.deleteById(folderId);
    }

    @Override
    public List<CollectionFolderDto> listCollections() {
        return folderRepository.findAll().stream().map(f -> {
            CollectionFolderDto dto = new CollectionFolderDto();
            dto.setId(f.getId());
            dto.setName(f.getName());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<AchievementDto> search(String q) {
        if (q == null || q.isEmpty()) return achievementRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
        return achievementRepository.findByTitleContainingIgnoreCase(q).stream().map(this::toDto).collect(Collectors.toList());
    }

    @Override
    public List<AchievementDto> filter(String filter) {
        if (filter == null || filter.isEmpty()) return search(null);
        return achievementRepository.findAll().stream()
                .filter(e -> e.getCategories() != null && e.getCategories().contains(filter))
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<AchievementDto> listByCategory(String catId) {
        return filter(catId);
    }

    @Override
    public List<AchievementDto> searchWithSort(String sort) {
        List<AchievementDto> list = search(null);
        if ("title:asc".equals(sort)) list.sort((a, b) -> a.getTitle().compareToIgnoreCase(b.getTitle()));
        else if ("title:desc".equals(sort)) list.sort((a, b) -> b.getTitle().compareToIgnoreCase(a.getTitle()));
        return list;
    }

    private AchievementDto toDto(AchievementEntity e) {
        AchievementDto d = new AchievementDto();
        d.setId(e.getId());
        d.setTitle(e.getTitle());
        d.setAuthorId(e.getAuthorId());
        d.setFileId(e.getFileId());
        if (e.getCategories() != null && !e.getCategories().isEmpty()) d.setCategories(List.of(e.getCategories().split(",")));
        return d;
    }

    private AchievementEntity toEntity(AchievementDto d) {
        AchievementEntity e = new AchievementEntity();
        e.setId(d.getId());
        e.setTitle(d.getTitle());
        e.setAuthorId(d.getAuthorId());
        e.setFileId(d.getFileId());
        if (d.getCategories() != null && !d.getCategories().isEmpty()) e.setCategories(String.join(",", d.getCategories()));
        return e;
    }
}
