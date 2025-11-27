package com.academic.achievement.service.impl;

import org.springframework.stereotype.Service;

import com.academic.achievement.dto.AchievementDto;
import com.academic.achievement.service.AchievementService;

@Service
public class AchievementServiceImpl implements AchievementService {


    @Override
    public void upload(AchievementDto dto) {
        System.out.println("achievement upload stub: " + dto.getTitle());
    }

    @Override
    public AchievementDto get(String achId) {
        AchievementDto dto = new AchievementDto();
        dto.setId(achId);
        dto.setTitle("Demo");
        return dto;
    }
}
