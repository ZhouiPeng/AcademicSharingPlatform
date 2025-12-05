package com.academic.file.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "files")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileEntity {

    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "uploader_id")
    private String uploaderId;

    @Column(nullable = false)
    private String bucket;

    @Column(name = "object_key", length = 512, nullable = false, unique = true)
    private String objectKey;

    @Column
    private Long size;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
