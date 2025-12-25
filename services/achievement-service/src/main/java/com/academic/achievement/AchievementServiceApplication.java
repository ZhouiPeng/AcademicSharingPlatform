package com.academic.achievement;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AchievementServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AchievementServiceApplication.class, args);
    }
}
