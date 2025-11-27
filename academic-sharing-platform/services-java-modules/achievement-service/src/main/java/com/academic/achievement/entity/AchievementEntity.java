package com.academic.achievement.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "achievements")
public class AchievementEntity {

    @Id
    private String id;

    private String title;

    @Column(name = "author_id")
    private String authorId;

    @Column(name = "file_id")
    private String fileId;

    // store categories as comma separated values for simplicity
    private String categories;

    @ManyToMany
    @JoinTable(
        name = "folder_members",
        joinColumns = @JoinColumn(name = "achievement_id"),
        inverseJoinColumns = @JoinColumn(name = "folder_id")
    )
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
