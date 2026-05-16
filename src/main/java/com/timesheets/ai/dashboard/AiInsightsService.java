package com.timesheets.ai.dashboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.timesheets.ai.dashboard.DashboardDtos.AiInsights;
import com.timesheets.ai.model.ContractorDiscrepancySummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Asks Claude (via Spring AI) to turn the discrepancy summary into three bulleted lists
 * the dashboard's "AI Insights" panel renders: top issues, repeat offenders, high-risk projects.
 *
 * Uses @Lazy on ChatClient for the same reason as EmailDraftingService — breaks the
 * tool-callback ⇆ ChatModel cycle that the MCP server's auto-config introduces.
 */
@Service
public class AiInsightsService {

    private static final Logger log = LoggerFactory.getLogger(AiInsightsService.class);

    private static final String PROMPT = """
            You analyse weekly Beeline-vs-Fusion timesheet reconciliations. Produce THREE short
            bulleted lists for an ops dashboard. Be specific, concrete, and grounded in the data.
            Each bullet should be one line, no more than ~120 characters. No filler.

            Output format — exactly this, no preamble, no extra sections:

            ### TOP_ISSUES
            - <bullet>
            - <bullet>
            - <bullet>

            ### REPEAT_OFFENDERS
            - <bullet>
            - <bullet>
            - <bullet>

            ### HIGH_RISK_PROJECTS
            - <bullet>
            - <bullet>
            - <bullet>

            Input data (JSON, sorted by total discrepancies descending):
            {summariesJson}

            Total flagged rows: {totalRows}
            Total contractors flagged: {totalContractors}
            """;

    private static final Pattern SECTION_RE = Pattern.compile(
            "###\\s*(TOP_ISSUES|REPEAT_OFFENDERS|HIGH_RISK_PROJECTS)\\s*\\n([\\s\\S]*?)(?=###|$)",
            Pattern.MULTILINE);

    private final ChatClient chatClient;
    private final ObjectMapper objectMapper;

    public AiInsightsService(@Lazy ChatClient chatClient, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.objectMapper = objectMapper;
    }

    public AiInsights generate(List<ContractorDiscrepancySummary> summaries, int totalRows) {
        if (summaries.isEmpty()) {
            return new AiInsights(
                    List.of("No discrepancies in the latest report — nothing to flag."),
                    List.of(),
                    List.of());
        }

        String json;
        try {
            json = objectMapper.writeValueAsString(summaries);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise summaries for insights", e);
        }

        String response;
        try {
            response = chatClient.prompt()
                    .user(u -> u.text(PROMPT).params(Map.of(
                            "summariesJson", json,
                            "totalRows", String.valueOf(totalRows),
                            "totalContractors", String.valueOf(summaries.size()))))
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("AI insights call failed, falling back to deterministic summary: {}", e.getMessage());
            return fallback(summaries);
        }

        log.debug("AI insights raw response:\n{}", response);
        return parse(response, summaries);
    }

    private AiInsights parse(String response, List<ContractorDiscrepancySummary> summaries) {
        List<String> topIssues = new ArrayList<>();
        List<String> repeatOffenders = new ArrayList<>();
        List<String> highRiskProjects = new ArrayList<>();

        if (response != null) {
            Matcher m = SECTION_RE.matcher(response);
            while (m.find()) {
                String section = m.group(1);
                List<String> bullets = extractBullets(m.group(2));
                switch (section) {
                    case "TOP_ISSUES"         -> topIssues.addAll(bullets);
                    case "REPEAT_OFFENDERS"   -> repeatOffenders.addAll(bullets);
                    case "HIGH_RISK_PROJECTS" -> highRiskProjects.addAll(bullets);
                }
            }
        }

        // If the model returned an unparseable shape, fall back so the UI is never empty.
        if (topIssues.isEmpty() && repeatOffenders.isEmpty() && highRiskProjects.isEmpty()) {
            return fallback(summaries);
        }
        return new AiInsights(topIssues, repeatOffenders, highRiskProjects);
    }

    private List<String> extractBullets(String section) {
        List<String> bullets = new ArrayList<>();
        for (String line : section.split("\\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("-")) {
                bullets.add(trimmed.substring(1).trim());
            }
        }
        return bullets;
    }

    /** Deterministic fallback when the LLM call fails or returns junk. */
    private AiInsights fallback(List<ContractorDiscrepancySummary> summaries) {
        List<String> top = new ArrayList<>();
        List<String> repeat = new ArrayList<>();
        List<String> risk = new ArrayList<>();

        summaries.stream().limit(3).forEach(c -> {
            if (c.noRecordCount() > 0) {
                top.add(c.employeeName() + " has " + c.noRecordCount() + " missing week(s) — likely uninvoiced.");
            }
            if (c.total() >= 3) {
                repeat.add(c.employeeName() + " (" + c.employeeCode() + ") — " + c.total() + " flagged rows.");
            }
        });
        if (top.isEmpty()) top.add("Most discrepancies are over/under reports, not missing records.");
        if (repeat.isEmpty()) repeat.add("No repeat offenders this week.");
        risk.add("Highest-volume contractors are the priority for follow-up.");
        return new AiInsights(top, repeat, risk);
    }
}
