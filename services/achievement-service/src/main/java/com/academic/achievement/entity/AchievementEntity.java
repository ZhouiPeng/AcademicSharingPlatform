package com.academic.achievement.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "achievements")
public class AchievementEntity {
    @Id
    private String id;
    private String title;
    private String authorId;
    private String fileId;
    private String categories; // comma separated

    @ManyToMany
    private Set<FolderEntity> folders = new HashSet<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public String getCategories() {
        return categories;
    }

    public void setCategories(String categories) {
        this.categories = categories;
    }

    public Set<FolderEntity> getFolders() {
        return folders;
    }

    public void setFolders(Set<FolderEntity> folders) {
        this.folders = folders;
    }
}
