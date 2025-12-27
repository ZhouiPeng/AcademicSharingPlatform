package com.academic.achievement.service;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RedisCountsFlushService implements DisposableBean {

    private static final Logger logger = LoggerFactory.getLogger(RedisCountsFlushService.class);

    private final StringRedisTemplate redis;
    private final ApplicationContext ctx;

    public RedisCountsFlushService(StringRedisTemplate redis, ApplicationContext ctx) {
        this.redis = redis;
        this.ctx = ctx;
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
        Set<String> keys = new HashSet<>();
        try {
            Set<String> dks = redis.keys("achievement:*:downloads");
            if (dks != null) keys.addAll(dks);
            Set<String> cks = redis.keys("achievement:*:collects");
            if (cks != null) keys.addAll(cks);
        } catch (Exception ex) {
            logger.warn("Failed to scan redis keys via KEYS; falling back to empty set", ex);
        }

        Object repo = null;
        try {
            repo = ctx.getBean("achievementRepository");
        } catch (Exception ex) {
            logger.warn("AchievementRepository bean not found; skipping flush", ex);
            return;
        }

        for (String key : keys) {
            try {
                String[] parts = key.split(":");
                if (parts.length < 3) continue;
                String id = parts[1];

                Method findById = repo.getClass().getMethod("findById", Object.class);
                java.util.Optional<?> opt = (java.util.Optional<?>) findById.invoke(repo, id);
                if (opt == null || !opt.isPresent()) continue;
                Object entity = opt.get();

                String downloadsKey = String.format("achievement:%s:downloads", id);
                String collectsKey = String.format("achievement:%s:collects", id);
                String dv = redis.opsForValue().get(downloadsKey);
                String cv = redis.opsForValue().get(collectsKey);

                if (dv != null) {
                    try {
                        Method m = entity.getClass().getMethod("setDownloadCount", Long.class);
                        m.invoke(entity, Long.parseLong(dv));
                    } catch (NoSuchMethodException e) {
                        // ignore
                    }
                }
                if (cv != null) {
                    try {
                        Method m2 = entity.getClass().getMethod("setCollectCount", Integer.class);
                        m2.invoke(entity, Integer.parseInt(cv));
                    } catch (NoSuchMethodException e) {
                        // ignore
                    }
                }

                Method save = repo.getClass().getMethod("save", Object.class);
                save.invoke(repo, entity);
            } catch (Exception ex) {
                logger.warn("Failed to flush key {}", key, ex);
            }
        }
    }
}
