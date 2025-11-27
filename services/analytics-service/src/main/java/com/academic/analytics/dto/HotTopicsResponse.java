package com.academic.analytics.dto;

import java.util.List;

public class HotTopicsResponse {

    private String reportId;
    private List<HotTopicItem> hotTopics;
    private List<String> emergingDirs;

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public List<HotTopicItem> getHotTopics() {
        return hotTopics;
    }

    public void setHotTopics(List<HotTopicItem> hotTopics) {
        this.hotTopics = hotTopics;
    }

    public List<String> getEmergingDirs() {
        return emergingDirs;
    }

    public void setEmergingDirs(List<String> emergingDirs) {
        this.emergingDirs = emergingDirs;
    }
}
