package com.academic.analytics.dto;

public class YearlyGrowthItem {

    private int year;
    private int count;
    private int growthRate;

    public YearlyGrowthItem() {
    }

    public YearlyGrowthItem(int year, int count, int growthRate) {
        this.year = year;
        this.count = count;
        this.growthRate = growthRate;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getGrowthRate() {
        return growthRate;
    }

    public void setGrowthRate(int growthRate) {
        this.growthRate = growthRate;
    }
}
