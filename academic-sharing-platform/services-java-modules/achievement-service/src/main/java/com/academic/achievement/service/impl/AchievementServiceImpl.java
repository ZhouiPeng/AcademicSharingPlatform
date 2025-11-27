package com.academic.achievement.service.impl;

import java.util.Arrays;
import java.util.List;

import org.springframework.stereotype.Service;

import com.academic.achievement.dto.AchievementDto;
import com.academic.achievement.dto.CollectionFolderDto;
import com.academic.achievement.service.AchievementService;

@Service
public class AchievementServiceImpl implements AchievementService {

    @Override
    public void upload(AchievementDto dto) {
        System.out.println("achievement upload stub: " + dto.getTitle());
    }

    @Override
    public void uploadUncertified(AchievementDto dto) {
        System.out.println("achievement upload uncertified stub: " + dto.getTitle());
    }

    @Override
    public AchievementDto get(String achId) {
        AchievementDto dto = new AchievementDto();
        dto.setId(achId);
        dto.setTitle("Demo");
        dto.setAuthorId("author-1");
        dto.setFileId("file-" + achId);
        dto.setCategories(Arrays.asList("cat1", "cat2"));
        return dto;
    }

    @Override
    public void update(String achId, AchievementDto dto) {
        System.out.println("update stub " + achId + " -> " + dto.getTitle());
    }

    @Override
    public void delete(String achId) {
        System.out.println("delete stub " + achId);
    }

    @Override
    public List<AchievementDto> listByAuthor(String authorId) {
        AchievementDto dto = get("a-1");
        dto.setAuthorId(authorId);
        return Arrays.asList(dto);
    }

    @Override
    public String generateDownloadLink(String achId) {
        return "http://localhost:8080/internal/files/" + achId + "/download";
    }

    @Override
    public CollectionFolderDto createFolder(CollectionFolderDto dto) {
        if (dto.getId() == null) dto.setId("folder-1");
        return dto;
    }

    @Override
    public void collect(String achId, String folderId) {
        System.out.println("collect stub " + achId + " into " + folderId);
    }

    @Override
    public void uncollect(String achId, String folderId) {
        System.out.println("uncollect stub " + achId + " from " + folderId);
    }

    @Override
    public void deleteFolder(String folderId) {
        System.out.println("delete folder stub " + folderId);
    }

    @Override
    public List<CollectionFolderDto> listCollections() {
        CollectionFolderDto f = new CollectionFolderDto();
        f.setId("folder-1");
        f.setName("默认收藏夹");
        return Arrays.asList(f);
    }

    @Override
    public List<AchievementDto> search(String q) {
        return listByAuthor("author-1");
    }

    @Override
    public List<AchievementDto> filter(String filter) {
        return listByAuthor("author-1");
    }

    @Override
    public List<AchievementDto> listByCategory(String catId) {
        return listByAuthor("author-1");
    }

    @Override
    public List<AchievementDto> searchWithSort(String sort) {
        return listByAuthor("author-1");
    }
}
