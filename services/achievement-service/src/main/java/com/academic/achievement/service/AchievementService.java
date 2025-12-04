package com.academic.achievement.service;

import java.util.List;

import com.academic.achievement.dto.AchievementDto;
import com.academic.achievement.dto.CollectionFolderDto;

public interface AchievementService {

    String upload(AchievementDto dto);


    AchievementDto get(String achId);

    void update(String achId, AchievementDto dto);

    void delete(String achId);

    List<AchievementDto> listByAuthor(String authorId);

    String generateDownloadLink(String achId);

    CollectionFolderDto createFolder(CollectionFolderDto dto);

    void collect(String achId, String folderId);

    void uncollect(String achId, String folderId);

    void deleteFolder(String folderId);

    List<CollectionFolderDto> listCollections();

    List<AchievementDto> search(String q);

    List<AchievementDto> filter(String filter);

    List<AchievementDto> listByCategory(String catId);

    List<AchievementDto> searchWithSort(String sort);
}
