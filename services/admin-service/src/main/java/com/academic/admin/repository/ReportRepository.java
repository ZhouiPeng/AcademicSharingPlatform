package com.academic.admin.repository;

import com.academic.admin.entity.ReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository 
public interface ReportRepository extends JpaRepository<ReportEntity, String> {
	List<ReportEntity> findByReporterIdOrderByCreatedAtDesc(String reporterId);
}
