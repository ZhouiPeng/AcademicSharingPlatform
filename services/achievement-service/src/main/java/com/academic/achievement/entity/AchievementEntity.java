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
    private String userId;
    private String fileId;
    private String categories; // comma separated (保留兼容)
    private Integer type;
    private String authors; // comma separated
    private String abstractText;

    private Long createdAt;
    private Long downloadCount;
    private Integer collectCount;
    private Integer citedCount;

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
        return userId;
    }

    public void setAuthorId(String authorId) {
        this.userId = authorId;
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

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getAuthors() {
        return authors;
    }

    public void setAuthors(String authors) {
        this.authors = authors;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }

    public Long getDownloadCount() {
        return downloadCount == null ? 0L : downloadCount;
    }

    public void setDownloadCount(Long downloadCount) {
        this.downloadCount = downloadCount;
    }

    public Integer getCollectCount() {
        return collectCount == null ? 0 : collectCount;
    }

    public void setCollectCount(Integer collectCount) {
        this.collectCount = collectCount;
    }

    public Integer getCitedCount() {
        return citedCount == null ? 0 : citedCount;
    }

    public void setCitedCount(Integer citedCount) {
        this.citedCount = citedCount;
    }

    public Set<FolderEntity> getFolders() {
        return folders;
    }

    public void setFolders(Set<FolderEntity> folders) {
        this.folders = folders;
    }
}
