package com.academic.achievement.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class EnvironmentConfig {

    private final Environment env;

    @Value("${app.env:dev}")
    private String appEnv;

    public EnvironmentConfig(Environment env) {
        this.env = env;
    }

    public boolean isDev() {
        // 优先使用 spring profiles，其次使用 app.env
        for (String p : env.getActiveProfiles()) {
            if ("dev".equalsIgnoreCase(p)) return true;
            if ("prod".equalsIgnoreCase(p)) return false;
        }
        return "dev".equalsIgnoreCase(appEnv);
    }

    public boolean isProd() {
        for (String p : env.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(p)) return true;
            if ("dev".equalsIgnoreCase(p)) return false;
        }
        return "prod".equalsIgnoreCase(appEnv);
    }

    public String getAppEnv() {
        return appEnv;
    }
}
