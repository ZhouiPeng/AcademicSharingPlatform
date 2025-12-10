package com.academic.file.repository;

import com.academic.file.entity.FileEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.repository.Query;

public interface FileRepository extends JpaRepository<FileEntity, String> {
    Optional<FileEntity> findByObjectKey(String objectKey);
    
    @Modifying
    @Transactional
    @Query("delete from FileEntity f where f.objectKey = ?1")
    int deleteByObjectKey(String objectKey);
}
