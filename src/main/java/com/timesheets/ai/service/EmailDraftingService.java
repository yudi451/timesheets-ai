package com.timesheets.ai.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timesheets.ai.model.ContractorDiscrepancySummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Asks the configured LLM to draft a manager-ready email summarising the discrepancies.
 * Returns a DraftedEmail with subject + body parsed out of the model's response.
 */
@Service
public class EmailDraftingService {

    private static final Logger log = LoggerFactory.getLogger(EmailDraftingService.class);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;
    private final String promptTemplate;

    public EmailDraftingService(
            @Lazy ChatClient chatClient,
            ObjectMapper objectMapper,
            @Value("classpath:prompts/discrepancy-email.st") Resource promptResource) throws IOException {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
        this.promptTemplate = promptResource.getContentAsString(StandardCharsets.UTF_8);
    }

    public DraftedEmail draft(
            List<ContractorDiscrepancySummary> summaries,
            int totalRows,
            String reportFileName) {

        String json;
        try {
            json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(summaries);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise summaries", e);
        }

        String response = chatClient.prompt()
                .user(u -> u.text(promptTemplate)
                        .params(Map.of(
                                "discrepancySummariesJson", json,
                                "totalRows", String.valueOf(totalRows),
                                "totalContractors", String.valueOf(summaries.size()),
                                "reportFileName", reportFileName)))
                .call()
                .content();

        log.debug("Raw LLM response:\n{}", response);
        return splitSubjectAndBody(response);
    }

    private DraftedEmail splitSubjectAndBody(String response) {
        String trimmed = response == null ? "" : response.trim();
        String subject = "Timesheet discrepancies flagged";
        String body = trimmed;

        // Expect: "Subject: ...\n\n<body>"
        if (trimmed.toLowerCase().startsWith("subject:")) {
            int newline = trimmed.indexOf('\n');
            if (newline > 0) {
                subject = trimmed.substring("subject:".length(), newline).trim();
                body = trimmed.substring(newline + 1).trim();
            }
        }
        return new DraftedEmail(subject, body);
    }

    public record DraftedEmail(String subject, String body) {}
}
