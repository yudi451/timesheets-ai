package com.timesheets.ai.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.timesheets.ai.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SendGridEmailService {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailService.class);

    private final AppProperties props;

    public SendGridEmailService(AppProperties props) {
        this.props = props;
    }

    public SendResult send(String toAddress, String subject, String body) {
        String apiKey = props.email().sendgridApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SENDGRID_API_KEY is not set — skipping actual send. Logging the email instead.");
            log.info("== DRY-RUN EMAIL ==\nTo: {}\nSubject: {}\n\n{}", toAddress, subject, body);
            return new SendResult(true, 0, "dry-run (no SENDGRID_API_KEY)");
        }

        Email from = new Email(props.email().fromAddress(), props.email().fromName());
        Email to = new Email(toAddress);
        Content content = new Content("text/plain", body);
        Mail mail = new Mail(from, subject, to, content);

        SendGrid sg = new SendGrid(apiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            boolean ok = response.getStatusCode() >= 200 && response.getStatusCode() < 300;
            if (!ok) {
                log.error("SendGrid returned {} — body: {}", response.getStatusCode(), response.getBody());
            }
            return new SendResult(ok, response.getStatusCode(), response.getBody());
        } catch (IOException e) {
            log.error("SendGrid request failed", e);
            return new SendResult(false, -1, e.getMessage());
        }
    }

    public record SendResult(boolean success, int statusCode, String message) {}
}
