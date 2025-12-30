package com.academic.achievement.service;

import java.util.List;

import com.academic.achievement.dto.AchievementDto;
import com.academic.achievement.dto.AchievementFilterRequest;
import com.academic.achievement.dto.CollectionFolderDto;

public interface AchievementService {

    String upload(AchievementDto dto, String userRoleHeader);

    AchievementDto get(String achId);

    void update(String achId, AchievementDto dto);

    void delete(String achId);

    List<AchievementDto> listByAuthor(String authorId);


    void collect(String achId, String folderId);

    void uncollect(String achId, String folderId);

    void deleteFolder(String folderId);

    CollectionFolderDto createFolder(CollectionFolderDto dto, String ownerId);

    List<CollectionFolderDto> listCollections(String ownerId);

    List<AchievementDto> search(String q);

    List<AchievementDto> search(String q, String sortBy, String order);

    List<AchievementDto> filter(AchievementFilterRequest filterRequest, String sortBy, String order);

    List<AchievementDto> listByCategory(String catId);

    List<AchievementDto> listByFolder(String folderId);

    void cite(String achId);

    List<AchievementDto> searchWithSort(String sortBy, String order);

    reactor.core.publisher.Mono<java.util.List<AchievementDto>> getReviews(String userIdHeader, String userRoleHeader);
}
