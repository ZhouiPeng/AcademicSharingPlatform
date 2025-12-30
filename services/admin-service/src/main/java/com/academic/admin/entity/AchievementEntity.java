package com.academic.admin.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "achievement_reviews")
public class AchievementEntity {
    @Id
    @Column(length = 36)
    private String id;

    @Column(name = "proceeding_admin_id", length = 128)
    private String proceedingAdminId;

    @Column(name = "user_id", nullable = false, length = 128)
    private String userId;

    @Column(name = "achievement_id", nullable = false, length = 32)
    private String achievementId;

    @Column(name = "status", length = 32)
    private String status;

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
