package com.academic.file.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "obs")
public class ObsProperties {
    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
}
