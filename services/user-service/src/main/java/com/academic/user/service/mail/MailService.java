package com.academic.user.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class MailService {

    private static final Logger log = LoggerFactory.getLogger(MailService.class);

    private final JavaMailSender mailSender;

    public MailService(ObjectProvider<JavaMailSender> mailSenderProvider) {
        this.mailSender = mailSenderProvider.getIfAvailable();
    }

    public void sendResetCode(String to, String code) {
        if (to == null || to.isEmpty()) {
            log.warn("Attempted to send reset code to empty email");
            return;
        }
        String subject = "密码重置验证码";
        String text = String.format("您的验证码为：%s 。该验证码 10 分钟内有效。如非本人操作请忽略。", code);

        if (mailSender == null) {
            // No mail sender available on the classpath/config — log the message as a fallback.
            log.info("[SEND EMAIL - LOG ONLY] To={} Subject={} Text={}", to, subject, text);
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
            log.info("Sent reset code email to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}. Falling back to logging.", to, e);
            log.info("[SEND EMAIL - FALLBACK] To={} Subject={} Text={}", to, subject, text);
        }
    }

    /**
     * Send a simple email with custom subject and text. Uses the same
     * mailSender (or logging fallback) as the reset code method.
     */
    public void sendSimple(String to, String subject, String text) {
        if (to == null || to.isEmpty()) {
            log.warn("Attempted to send email to empty address");
            return;
        }

        if (mailSender == null) {
            log.info("[SEND EMAIL - LOG ONLY] To={} Subject={} Text={}", to, subject, text);
            return;
        }

        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(text);
            mailSender.send(msg);
            log.info("Sent email to {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}. Falling back to logging.", to, e);
            log.info("[SEND EMAIL - FALLBACK] To={} Subject={} Text={}", to, subject, text);
        }
    }
}
