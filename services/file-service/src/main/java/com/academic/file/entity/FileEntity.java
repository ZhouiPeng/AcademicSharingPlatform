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

    @Column(name = "type", length = 64, nullable = false) // PAPER, APPLICATION etc.
    private String type;

    @Column(name = "name", length = 255, nullable = false)
    private String name;

    @Column(name = "uploader_id", length = 128)
    private String uploaderId;

     @Column(name = "url", length = 1024)
    private String url;

    @Column(name = "object_key", length = 512, unique = true)
    private String objectKey;

    @Column(name = "size")
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
