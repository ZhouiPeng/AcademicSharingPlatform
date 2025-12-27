package com.academic.achievement.service;

public class DuplicateAchievementException extends RuntimeException {
    public DuplicateAchievementException(String message) {
        super(message);
    }
}
