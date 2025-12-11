package com.academic.user.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.academic.user.service.mail.MailService;

@RestController
@RequestMapping("/admin")
public class AdminMailController {

    private final MailService mailService;

    @Value("${mail.test.enabled:false}")
    private boolean testEnabled;

    @Value("${mail.test.token:}")
    private String testToken;

    public AdminMailController(MailService mailService) {
        this.mailService = mailService;
    }

    @PostMapping("/send-test-email")
    public ResponseEntity<String> sendTestEmail(@RequestParam("to") String to,
            @RequestHeader(value = "X-ADMIN-TOKEN", required = false) String token) {
        if (!testEnabled) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Test email endpoint is disabled");
        }

        if (testToken != null && !testToken.isEmpty()) {
            if (token == null || !testToken.equals(token)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Invalid admin token");
            }
        }

        String subject = "[TEST] user-service email";
        String text = "This is a test email from user-service. If you received this, SMTP is configured correctly.";
        mailService.sendSimple(to, subject, text);
        return ResponseEntity.ok("Test email queued (or logged) successfully");
    }
}
