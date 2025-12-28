package com.academic.admin.repository;

import com.academic.admin.entity.AchievementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AchievementRepository extends JpaRepository<AchievementEntity, String> {

	List<AchievementEntity> findByProceedingAdminIdOrderByCreatedAtDesc(String proceedingAdminId);

	List<AchievementEntity> findByUserIdOrderByCreatedAtDesc(String userId);

}
