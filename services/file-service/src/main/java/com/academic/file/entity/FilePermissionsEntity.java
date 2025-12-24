package com.academic.file.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "file_permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FilePermissionsEntity {

    @Id
    @Column(name = "id")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_id", nullable = false, referencedColumnName = "id")
    private FileEntity file;

    @Column(name = "principal_type", nullable = false, length = 16)
    private String principalType;

    @Column(name = "principal_id", nullable = false, length = 128)
    private String principalId;

    @Column(name = "permission", nullable = false, length = 32)
    private String permission;

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
