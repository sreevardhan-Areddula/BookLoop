package com.bookloop.mail;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class GmailEmailSender {

    private final String mode;
    private final String username;
    private final String appPassword;
    private final String fromAddress;
    private final GoogleOAuthTokenProvider oauthTokenProvider;

    public GmailEmailSender(
            @Value("${bookloop.mail.mode:APP_PASSWORD}") String mode,
            @Value("${bookloop.mail.username:}") String username,
            @Value("${bookloop.mail.app-password:}") String appPassword,
            @Value("${bookloop.mail.from:}") String fromAddress,
            GoogleOAuthTokenProvider oauthTokenProvider
    ) {
        this.mode = mode.trim().toUpperCase();
        this.username = username;
        this.appPassword = appPassword;
        this.fromAddress = fromAddress;
        this.oauthTokenProvider = oauthTokenProvider;
    }

    /** Sends plain-text mail through Gmail SMTP using the selected auth mode. */
    public void send(String recipient, String subject, String body) {
        boolean oauth = "OAUTH2".equals(mode);
        if (!oauth && !"APP_PASSWORD".equals(mode)) {
            throw new IllegalStateException("BOOKLOOP_MAIL_MODE must be APP_PASSWORD or OAUTH2.");
        }
        if (username.isBlank()) {
            throw new IllegalStateException("Gmail mail username is not configured.");
        }
        String smtpCredential = oauth ? oauthTokenProvider.accessToken() : appPassword;
        if (smtpCredential.isBlank()) {
            throw new IllegalStateException("Gmail SMTP credentials are not configured.");
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost("smtp.gmail.com");
        sender.setPort(587);
        sender.setUsername(username);
        sender.setPassword(smtpCredential);
        sender.setProtocol("smtp");

        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.starttls.required", "true");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        if (oauth) {
            properties.put("mail.smtp.auth.mechanisms", "XOAUTH2");
            properties.put("mail.smtp.auth.login.disable", "true");
            properties.put("mail.smtp.auth.plain.disable", "true");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromAddress.isBlank() ? username : fromAddress);
        message.setTo(recipient);
        message.setSubject(subject);
        message.setText(body);
        sender.send(message);
    }
}
