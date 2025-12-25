package com.academic.achievement.service;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.academic.achievement.entity.AchievementEntity;
import com.academic.achievement.repository.AchievementRepository;

@Component
public class RedisCountsFlushService implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(RedisCountsFlushService.class);

    private final StringRedisTemplate redis;
    private final AchievementRepository achievementRepository;

    public RedisCountsFlushService(StringRedisTemplate redis, AchievementRepository achievementRepository) {
        this.redis = redis;
        this.achievementRepository = achievementRepository;
    }

    @Scheduled(fixedDelayString = "${achievement.redis.flush-ms:60000}")
    public void periodicFlush() {
        try {
            flushOnce();
        } catch (Exception ex) {
            logger.error("Error flushing Redis counts to DB", ex);
        }
    }

    @Override
    public void destroy() {
        logger.info("Service shutting down - flushing Redis counts to DB");
        try {
            flushOnce();
        } catch (Exception ex) {
            logger.error("Error flushing Redis counts on shutdown", ex);
        }
    }

    public void flushOnce() {
        // collect keys
        Set<String> keys = new HashSet<>();
        try {
            Set<String> dks = redis.keys("achievement:*:downloads");
            if (dks != null) keys.addAll(dks);
            Set<String> cks = redis.keys("achievement:*:collects");
            if (cks != null) keys.addAll(cks);
        } catch (Exception ex) {
            logger.warn("Failed to scan redis keys via KEYS; falling back to empty set", ex);
        }

        for (String key : keys) {
            try {
                String[] parts = key.split(":");
                if (parts.length < 3) continue;
                String id = parts[1];
                AchievementEntity e = achievementRepository.findById(id).orElse(null);
                if (e == null) continue;
                String downloadsKey = String.format("achievement:%s:downloads", id);
                String collectsKey = String.format("achievement:%s:collects", id);
                String dv = redis.opsForValue().get(downloadsKey);
                String cv = redis.opsForValue().get(collectsKey);
                if (dv != null) {
                    try { e.setDownloadCount(Long.parseLong(dv)); } catch (Exception ignore) {}
                }
                if (cv != null) {
                    try { e.setCollectCount(Integer.parseInt(cv)); } catch (Exception ignore) {}
                }
                achievementRepository.save(e);
            } catch (Exception ex) {
                logger.warn("Failed to flush key {}", key, ex);
            }
        }
    }
}
