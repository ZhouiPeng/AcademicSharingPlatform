package com.academic.achievement.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AchievementDto {

    @JsonProperty("achievementId")
    private String id;

    private String userId; // 提交者ID，接口字段名为 userId

    private String title;

    private Integer type; // achievementType

    private List<String> authors;

    private List<String> categories; // comma separated

    @JsonProperty("abstract")
    private String abstractText;

    private String fileId; // 存储到 File Service 的文件标识
    private Long createdAt;
    private Long downloadCount;
    private Integer collectCount;
    private Integer citedCount;

    public void setCategories(List<String> categories) {
        this.categories = categories;
    }

    public List<String> getCategories() {
        return categories;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(List<String> authors) {
        this.authors = authors;
    }

    public String getAbstractText() {
        return abstractText;
    }

    public void setAbstractText(String abstractText) {
        this.abstractText = abstractText;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
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
}
