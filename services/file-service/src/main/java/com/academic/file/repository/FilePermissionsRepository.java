package com.academic.file.repository;

import com.academic.file.entity.FilePermissionsEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FilePermissionsRepository extends JpaRepository<FilePermissionsEntity, String> {
    // custom queries if needed
}
