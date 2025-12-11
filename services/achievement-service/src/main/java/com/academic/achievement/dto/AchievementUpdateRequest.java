package com.academic.achievement.dto;

import java.util.Map;

public class AchievementUpdateRequest {
    private String achievementId;
    private Map<String, Object> data;

    public String getAchievementId() {
        return achievementId;
    }

    public void setAchievementId(String achievementId) {
        this.achievementId = achievementId;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
}
