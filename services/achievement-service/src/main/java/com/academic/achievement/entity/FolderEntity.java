package com.academic.achievement.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "folders")
public class FolderEntity {
    @Id
    private String id;
    private String name;
    private String description;
    private String ownerId;

    @ManyToMany(mappedBy = "folders")
    private Set<AchievementEntity> achievements = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(String ownerId) {
        this.ownerId = ownerId;
    }

    public Set<AchievementEntity> getAchievements() {
        return achievements;
    }

    public void setAchievements(Set<AchievementEntity> achievements) {
        this.achievements = achievements;
    }
}
