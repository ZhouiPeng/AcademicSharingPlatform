package com.academic.analytics.dto;

public class HotTopicsRequest {

    private String timeRange;
    private String domainId;
    private Integer analysisDim;

    public String getTimeRange() {
        return timeRange;
    }

    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }

    public String getDomainId() {
        return domainId;
    }

    public void setDomainId(String domainId) {
        this.domainId = domainId;
    }

    public Integer getAnalysisDim() {
        return analysisDim;
    }

    public void setAnalysisDim(Integer analysisDim) {
        this.analysisDim = analysisDim;
    }
}
