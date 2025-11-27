package com.academic.achievement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academic.achievement.entity.AchievementEntity;

public interface AchievementRepository extends JpaRepository<AchievementEntity, String> {
    List<AchievementEntity> findByAuthorId(String authorId);
    List<AchievementEntity> findByTitleContainingIgnoreCase(String title);
}
