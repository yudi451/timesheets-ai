package com.timesheets.ai.service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content;
import com.sendgrid.helpers.mail.objects.Email;
import com.sendgrid.helpers.mail.objects.Personalization;
import com.timesheets.ai.config.AppProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class SendGridEmailService {

    private static final Logger log = LoggerFactory.getLogger(SendGridEmailService.class);

    private final AppProperties props;

    public SendGridEmailService(AppProperties props) {
        this.props = props;
    }

    /**
     * Ships the email. {@code toAddress} may be a single address or a comma-separated list
     * — every address ends up on the To: line and all recipients see each other.
     */
    public SendResult send(String toAddress, String subject, String body) {
        String apiKey = props.email().sendgridApiKey();
        List<String> recipients = parseRecipients(toAddress);

        if (recipients.isEmpty()) {
            log.error("No valid recipient parsed from '{}' — aborting send.", toAddress);
            return new SendResult(false, -1, "no recipients");
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("SENDGRID_API_KEY is not set — skipping actual send. Logging the email instead.");
            log.info("== DRY-RUN EMAIL ==\nTo: {}\nSubject: {}\n\n{}", recipients, subject, body);
            return new SendResult(true, 0, "dry-run (no SENDGRID_API_KEY)");
        }

        Email from = new Email(props.email().fromAddress(), props.email().fromName());
        Content content = new Content("text/plain", body);

        Mail mail = new Mail();
        mail.setFrom(from);
        mail.setSubject(subject);
        mail.addContent(content);

        Personalization personalization = new Personalization();
        for (String addr : recipients) {
            personalization.addTo(new Email(addr));
        }
        mail.addPersonalization(personalization);

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
            } else {
                log.info("SendGrid accepted email to {} recipient(s): {}", recipients.size(), recipients);
            }
            return new SendResult(ok, response.getStatusCode(), response.getBody());
        } catch (IOException e) {
            log.error("SendGrid request failed", e);
            return new SendResult(false, -1, e.getMessage());
        }
    }

    /** Splits a comma-separated address list, trims whitespace, drops blanks. */
    private List<String> parseRecipients(String raw) {
        List<String> out = new ArrayList<>();
        if (raw == null) return out;
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty() && trimmed.contains("@")) out.add(trimmed);
        }
        return out;
    }

    public record SendResult(boolean success, int statusCode, String message) {}
}
