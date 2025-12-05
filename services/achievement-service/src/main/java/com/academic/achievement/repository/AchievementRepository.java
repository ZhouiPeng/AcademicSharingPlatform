package com.academic.achievement.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.academic.achievement.entity.AchievementEntity;

public interface AchievementRepository extends JpaRepository<AchievementEntity, String> {
    List<AchievementEntity> findByUserId(String userId);
    List<AchievementEntity> findByTitleContainingIgnoreCase(String title);
}
