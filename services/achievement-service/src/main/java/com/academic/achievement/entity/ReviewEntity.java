package com.academic.achievement.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "reviews")
public class ReviewEntity {

    @Id
    private String achId;
    private String userId;

    public String getAchId() {
        return achId;
    }

    public void setAchId(String achId) {
        this.achId = achId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }
}
