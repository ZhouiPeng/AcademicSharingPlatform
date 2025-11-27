package com.academic.analytics.dto;

public class AchievementsStatsRequest {

    private String timeRange;
    private String institution;
    private Integer achievementType;

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public String getInstitution() {
        return institution;
    }

    public void setInstitution(String institution) {
        this.institution = institution;
    }

    public Integer getAchievementType() {
        return achievementType;
    }

    public void setAchievementType(Integer achievementType) {
        this.achievementType = achievementType;
    }
}
