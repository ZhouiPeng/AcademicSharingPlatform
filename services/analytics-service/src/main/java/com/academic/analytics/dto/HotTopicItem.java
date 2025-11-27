package com.academic.analytics.dto;

import java.util.List;

public class HotTopicItem {

    private String keyword;
    private Integer count;
    private List<Integer> trendData;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public Integer getCount() {
        return count;
    }

    public void setCount(Integer count) {
        this.count = count;
    }

    public List<Integer> getTrendData() {
        return trendData;
    }

    public void setTrendData(List<Integer> trendData) {
        this.trendData = trendData;
    }
}
