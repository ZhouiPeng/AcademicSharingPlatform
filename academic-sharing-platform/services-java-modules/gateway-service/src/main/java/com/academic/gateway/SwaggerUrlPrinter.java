package com.academic.gateway;

import org.springframework.boot.web.context.WebServerInitializedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class SwaggerUrlPrinter implements ApplicationListener<WebServerInitializedEvent> {

    @Override
    public void onApplicationEvent(WebServerInitializedEvent event) {
        int port = event.getWebServer().getPort();
        String url = "http://localhost:" + port + "/swagger-ui/index.html";
        System.out.println();
        System.out.println("==================================================");
        System.out.println("Swagger UI: " + url);
        System.out.println("Click the link to open the API docs.");
        System.out.println("==================================================");
    }
}
