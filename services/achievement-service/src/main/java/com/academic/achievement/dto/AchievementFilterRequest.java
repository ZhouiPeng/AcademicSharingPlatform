package com.academic.achievement.dto;

public class AchievementFilterRequest {
    private String keywords;
    private String classification;
    private Integer fromYear;
    private Integer toYear;
    private String title;
    private String userId;
    private String fileId;
    private Integer type;
    private java.util.List<String> authors;
    private java.util.List<String> categories;
    private Integer pageNum;
    private Integer pageSize;
    private String sortBy;
    private String order;

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification;
    }

    public Integer getFromYear() {
        return fromYear;
    }



    public void setFromYear(Integer fromYear) {
        this.fromYear = fromYear;
    }

    public Integer getToYear() {
        return toYear;
    }

    public void setToYear(Integer toYear) {
        this.toYear = toYear;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getFileId() {
        return fileId;
    }

    public void setFileId(String fileId) {
        this.fileId = fileId;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public java.util.List<String> getAuthors() {
        return authors;
    }

    public void setAuthors(java.util.List<String> authors) {
        this.authors = authors;
    }

    public java.util.List<String> getCategories() {
        return categories;
    }

    public void setCategories(java.util.List<String> categories) {
        this.categories = categories;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }

    public String getSortBy() {
        return sortBy;
    }

    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }

    public String getOrder() {
        return order;
    }

    public void setOrder(String order) {
        this.order = order;
    }
}
