package com.example.ELearningBE.integration.mail;

public interface EmailService {

    boolean sendEmail(String to, String subject, String content, boolean isHtml);

    default boolean sendEmail(String to, String subject, String content) {
        return sendEmail(to, subject, content, true);
    }
}
