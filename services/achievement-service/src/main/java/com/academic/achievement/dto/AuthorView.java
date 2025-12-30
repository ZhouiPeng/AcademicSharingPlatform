package com.academic.achievement.dto;

public class AuthorView {
    private String username;
    private String userId; // may be null

    public AuthorView() {}

    public AuthorView(String username, String userId) {
        this.username = username;
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
