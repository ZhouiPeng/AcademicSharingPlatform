package com.academic.gateway.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.academic.gateway.config.SwaggerProperties;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class SwaggerConfigController {

    private final SwaggerProperties swaggerProperties;

    public SwaggerConfigController(SwaggerProperties swaggerProperties) {
        this.swaggerProperties = swaggerProperties;
    }

    @GetMapping(value = "/v3/api-docs/gateway-swagger-config", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> swaggerConfig() {
        List<String> swaggerUrls = swaggerProperties.getUrls();
        Map<String, Object> cfg = new HashMap<>();
        List<Map<String, String>> urls = new ArrayList<>();

        if (swaggerUrls != null && !swaggerUrls.isEmpty()) {
            for (String entry : swaggerUrls) {
                String name = entry;
                String url = entry;
                if (entry.contains("|")) {
                    String[] parts = entry.split("\\|", 2);
                    name = parts[0].trim();
                    url = parts[1].trim();
                }
                Map<String, String> m = new HashMap<>();
                m.put("name", name);
                m.put("url", url);
                urls.add(m);
            }
        }

        cfg.put("configUrl", "/v3/api-docs/gateway-swagger-config");
        cfg.put("urls", urls);
        cfg.put("validatorUrl", "");
        return cfg;
    }
}
