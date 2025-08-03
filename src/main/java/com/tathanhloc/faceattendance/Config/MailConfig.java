package com.tathanhloc.faceattendance.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

/**
 * Mail Configuration for Face Attendance System
 * FIXED: Single bean configuration with conditional logic
 */
@Configuration
public class MailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean auth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean starttls;

    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    /**
     * Single JavaMailSender bean - conditionally returns real or mock implementation
     */
    @Bean
    @Primary
    public JavaMailSender javaMailSender() {
        if (mailEnabled) {
            // Return real mail sender for production
            return createRealMailSender();
        } else {
            // Return mock mail sender for development/testing
            return createMockMailSender();
        }
    }

    /**
     * Create real JavaMailSender for production use
     */
    private JavaMailSender createRealMailSender() {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        mailSender.setHost(host);
        mailSender.setPort(port);

        // Only set credentials if provided
        if (username != null && !username.isEmpty()) {
            mailSender.setUsername(username);
        }

        if (password != null && !password.isEmpty()) {
            mailSender.setPassword(password);
        }

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", auth);
        props.put("mail.smtp.starttls.enable", starttls);
        props.put("mail.debug", "false");

        // Additional properties for better compatibility
        props.put("mail.smtp.ssl.trust", host);
        props.put("mail.smtp.connectiontimeout", "10000");
        props.put("mail.smtp.timeout", "10000");

        System.out.println("📧 Real JavaMailSender configured for: " + host);
        return mailSender;
    }

    /**
     * Create mock JavaMailSender for development/testing
     */
    private JavaMailSender createMockMailSender() {
        System.out.println("📧 Mock JavaMailSender configured (app.mail.enabled=false)");
        return new MockJavaMailSender();
    }
}

/**
 * Mock JavaMailSender implementation for development/testing
 */
class MockJavaMailSender implements JavaMailSender {

    @Override
    public jakarta.mail.internet.MimeMessage createMimeMessage() {
        try {
            Properties props = new Properties();
            jakarta.mail.Session session = jakarta.mail.Session.getDefaultInstance(props);
            return new jakarta.mail.internet.MimeMessage(session);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create mock MimeMessage", e);
        }
    }

    @Override
    public jakarta.mail.internet.MimeMessage createMimeMessage(java.io.InputStream contentStream)
            throws org.springframework.mail.MailParseException {
        try {
            Properties props = new Properties();
            jakarta.mail.Session session = jakarta.mail.Session.getDefaultInstance(props);
            return new jakarta.mail.internet.MimeMessage(session, contentStream);
        } catch (Exception e) {
            throw new org.springframework.mail.MailParseException("Failed to parse mock message", e);
        }
    }

    @Override
    public void send(jakarta.mail.internet.MimeMessage mimeMessage)
            throws org.springframework.mail.MailException {
        try {
            System.out.println("📧 MOCK EMAIL: MimeMessage would be sent");
            System.out.println("    Subject: " + (mimeMessage.getSubject() != null ? mimeMessage.getSubject() : "No Subject"));
            if (mimeMessage.getAllRecipients() != null && mimeMessage.getAllRecipients().length > 0) {
                System.out.println("    To: " + mimeMessage.getAllRecipients()[0]);
            }
        } catch (Exception e) {
            System.out.println("📧 MOCK EMAIL: MimeMessage would be sent (details unavailable)");
        }
    }

    @Override
    public void send(jakarta.mail.internet.MimeMessage... mimeMessages)
            throws org.springframework.mail.MailException {
        for (jakarta.mail.internet.MimeMessage msg : mimeMessages) {
            send(msg);
        }
    }

    @Override
    public void send(org.springframework.mail.javamail.MimeMessagePreparator mimeMessagePreparator)
            throws org.springframework.mail.MailException {
        try {
            jakarta.mail.internet.MimeMessage message = createMimeMessage();
            mimeMessagePreparator.prepare(message);
            send(message);
        } catch (Exception e) {
            System.out.println("📧 MOCK EMAIL: Email would be sent via preparator (error in prep: " + e.getMessage() + ")");
        }
    }

    @Override
    public void send(org.springframework.mail.javamail.MimeMessagePreparator... mimeMessagePreparators)
            throws org.springframework.mail.MailException {
        for (org.springframework.mail.javamail.MimeMessagePreparator prep : mimeMessagePreparators) {
            send(prep);
        }
    }

    @Override
    public void send(org.springframework.mail.SimpleMailMessage simpleMessage)
            throws org.springframework.mail.MailException {
        System.out.println("📧 MOCK EMAIL: Simple email would be sent");
        if (simpleMessage.getTo() != null && simpleMessage.getTo().length > 0) {
            System.out.println("    To: " + simpleMessage.getTo()[0]);
        }
        if (simpleMessage.getSubject() != null) {
            System.out.println("    Subject: " + simpleMessage.getSubject());
        }
        if (simpleMessage.getText() != null) {
            String preview = simpleMessage.getText().length() > 50 ?
                    simpleMessage.getText().substring(0, 50) + "..." :
                    simpleMessage.getText();
            System.out.println("    Text: " + preview);
        }
    }

    @Override
    public void send(org.springframework.mail.SimpleMailMessage... simpleMessages)
            throws org.springframework.mail.MailException {
        for (org.springframework.mail.SimpleMailMessage msg : simpleMessages) {
            send(msg);
        }
    }
}