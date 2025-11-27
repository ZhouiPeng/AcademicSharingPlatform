package com.academic.analytics.dto;

import java.util.List;

public class AchievementsStatsResponse {

    private int totalCount;
    private List<TypeDistributionItem> typeDistribution;
    private List<YearlyGrowthItem> yearlyGrowth;

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }

    public List<TypeDistributionItem> getTypeDistribution() {
        return typeDistribution;
    }

    public void setTypeDistribution(List<TypeDistributionItem> typeDistribution) {
        this.typeDistribution = typeDistribution;
    }

    public List<YearlyGrowthItem> getYearlyGrowth() {
        return yearlyGrowth;
    }

    public void setYearlyGrowth(List<YearlyGrowthItem> yearlyGrowth) {
        this.yearlyGrowth = yearlyGrowth;
    }
}
