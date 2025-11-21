package com.academic.achievement.service;

import com.academic.achievement.dto.AchievementDto;

public interface AchievementService {



    void upload(AchievementDto dto);

    AchievementDto get(String achId);
}
