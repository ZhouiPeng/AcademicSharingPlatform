package com.academic.admin.repository;

import com.academic.admin.entity.AuthRequestEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuthRequestRepository extends JpaRepository<AuthRequestEntity, String> {
    List<AuthRequestEntity> findByProceedingAdminIdOrderByCreatedAtDesc(String proceedingAdminId);
}
