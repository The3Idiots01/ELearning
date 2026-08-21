package com.example.ELearningBE.integration.mail;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class SendGridEmailServiceImpl implements EmailService {

    private final SendGrid sendGrid;

    @Value("${sendgrid.from-email:}")
    private String defaultFromEmail;

    @Override
    public boolean sendEmail(String to, String subject, String content, boolean isHtml) {
        if (defaultFromEmail == null || defaultFromEmail.isBlank()) {
            log.error("SendGrid sender email is not configured in sendgrid.from-email");
            throw new IllegalStateException("SendGrid from-email is not configured");
        }

        log.info("Sending email via SendGrid from: [{}] to: [{}] with subject: [{}]", defaultFromEmail, to, subject);

        Email from = new Email(defaultFromEmail);
        Email recipient = new Email(to);
        String contentType = isHtml ? "text/html" : "text/plain";
        Content emailContent = new Content(contentType, content);
        Mail mail = new Mail(from, subject, recipient, emailContent);

        try {
            Request request = new Request();
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());

            Response response = sendGrid.api(request);
            log.info("SendGrid email response status: {}, body: {}", response.getStatusCode(), response.getBody());

            // 200, 202 are success statuses in SendGrid API
            return response.getStatusCode() >= 200 && response.getStatusCode() < 300;
        } catch (IOException ex) {
            log.error("Failed to send email to {} via SendGrid", to, ex);
            return false;
        }
    }
}
