package com.academic.analytics.dto;

public class TypeDistributionItem {

    private int type;
    private String typeName;
    private int count;
    private int rate;

    public TypeDistributionItem() {
    }

    public TypeDistributionItem(int type, String typeName, int count, int rate) {
        this.type = type;
        this.typeName = typeName;
        this.count = count;
        this.rate = rate;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }
}
