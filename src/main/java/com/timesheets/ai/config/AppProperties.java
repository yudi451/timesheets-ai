package com.timesheets.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Email email, Excel excel) {

    public record Email(
            String sendgridApiKey,
            String fromAddress,
            String fromName,
            String defaultRecipient) {}

    public record Excel(
            String defaultPath,
            String sheetName,
            int fusionHoursColumnIndex) {}
}
