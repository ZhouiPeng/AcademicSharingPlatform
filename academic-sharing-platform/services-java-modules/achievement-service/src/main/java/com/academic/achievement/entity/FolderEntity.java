package com.academic.achievement.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "folders")
public class FolderEntity {

    @Id
    private String id;

    @Column(name = "name")
    private String name;

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

    public Set<AchievementEntity> getAchievements() {
        return achievements;
    }

    public void setAchievements(Set<AchievementEntity> achievements) {
        this.achievements = achievements;
    }
}
