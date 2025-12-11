package com.academic.user.dto;

import com.academic.user.common.Role;
import com.fasterxml.jackson.annotation.JsonProperty;

public class User {

    private String userId;
    private String username;
    private String email;
    @JsonProperty(value = "password", access = JsonProperty.Access.WRITE_ONLY)
    private String passwordHash;
    private Role role;
    //这是什么鬼字段？
    private String displayName;
    private String avatarUrl;
    private String createTime;
    private String updateTime;

    public User() {
    }

    public User(String userId, String username, String email, String passwordHash,
            Role role, String authorName, String avatarUrl,
            String createTime, String updateTime) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.displayName = authorName;
        this.avatarUrl = avatarUrl;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public User(String username, String email, String passwordHash) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public User(String username, String email, String passwordHash, String displayName) {
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public String getCreateTime() {
        return createTime;
    }

    public void setCreateTime(String createTime) {
        this.createTime = createTime;
    }

    public String getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(String updateTime) {
        this.updateTime = updateTime;
    }

    @Override
    public String toString() {
        return "{"
                + "userId='" + userId + '\''
                + ", username='" + username + '\''
                + ", email='" + email + '\''
                + ", passwordHash='" + passwordHash + '\''
                + ", role=" + role
                + ", authorName='" + displayName + '\''
                + ", avatarUrl='" + avatarUrl + '\''
                + ", createTime=" + createTime
                + ", updateTime=" + updateTime
                + '}';
    }
}
