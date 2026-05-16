package com.timesheets.ai.mcp;

import com.timesheets.ai.config.AppProperties;
import com.timesheets.ai.model.ContractorDiscrepancySummary;
import com.timesheets.ai.model.DiscrepancyRow;
import com.timesheets.ai.service.DiscrepancyDetectionService;
import com.timesheets.ai.service.EmailDraftingService;
import com.timesheets.ai.service.EmailDraftingService.DraftedEmail;
import com.timesheets.ai.service.SendGridEmailService;
import com.timesheets.ai.service.SendGridEmailService.SendResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * MCP-exposed tools. Each public @Tool method becomes callable by any MCP client
 * (Claude Desktop, Claude Code, Cursor, etc.) once they connect to this server.
 */
@Service
public class TimesheetTools {

    private static final Logger log = LoggerFactory.getLogger(TimesheetTools.class);

    private final DiscrepancyDetectionService detection;
    private final EmailDraftingService drafter;
    private final SendGridEmailService mailer;
    private final AppProperties props;

    public TimesheetTools(
            DiscrepancyDetectionService detection,
            EmailDraftingService drafter,
            SendGridEmailService mailer,
            AppProperties props) {
        this.detection = detection;
        this.drafter = drafter;
        this.mailer = mailer;
        this.props = props;
    }

    @Tool(description = """
            Scan a Beeline_Fusion_Report .xlsx file and return every flagged timesheet row,
            grouped by contractor. Flags include: under-report (red), over-report (yellow),
            and missing record (teal/grey NA). Use this to understand what needs attention
            before deciding whether to email the manager.""")
    public CheckResult checkDiscrepancies(
            @ToolParam(required = false,
                    description = "Absolute path to the .xlsx report. If omitted, uses app.excel.default-path.")
            String excelPath) throws IOException {

        Path path = resolvePath(excelPath);
        List<DiscrepancyRow> rows = detection.detect(path);
        List<ContractorDiscrepancySummary> summaries = detection.groupByContractor(rows);

        log.info("checkDiscrepancies({}) → {} rows across {} contractors",
                path.getFileName(), rows.size(), summaries.size());

        return new CheckResult(
                path.toString(),
                rows.size(),
                summaries.size(),
                summaries);
    }

    @Tool(description = """
            End-to-end action: scan the report, draft a manager-ready email with the LLM,
            and send it via SendGrid. Returns the subject, body, and send status so the
            caller can confirm what went out. Recipient defaults to app.email.default-recipient.""")
    public SendDiscrepancyEmailResult sendDiscrepancyEmail(
            @ToolParam(required = false,
                    description = "Absolute path to the .xlsx report. If omitted, uses app.excel.default-path.")
            String excelPath,
            @ToolParam(required = false,
                    description = "Recipient email. If omitted, uses app.email.default-recipient.")
            String recipient) throws IOException {

        Path path = resolvePath(excelPath);
        String to = (recipient == null || recipient.isBlank())
                ? props.email().defaultRecipient()
                : recipient.trim();

        List<DiscrepancyRow> rows = detection.detect(path);
        if (rows.isEmpty()) {
            return new SendDiscrepancyEmailResult(
                    to, 0, 0, null, null, false,
                    "No discrepancies found — nothing was sent.");
        }

        List<ContractorDiscrepancySummary> summaries = detection.groupByContractor(rows);
        DraftedEmail draft = drafter.draft(summaries, rows.size(), path.getFileName().toString());
        SendResult send = mailer.send(to, draft.subject(), draft.body());

        log.info("sendDiscrepancyEmail → to={} subject={} success={} status={}",
                to, draft.subject(), send.success(), send.statusCode());

        return new SendDiscrepancyEmailResult(
                to,
                rows.size(),
                summaries.size(),
                draft.subject(),
                draft.body(),
                send.success(),
                send.message());
    }

    @Tool(description = """
            Drill into a single contractor by employee code. Returns every flagged row
            for that contractor across the report. Useful when the manager replies asking
            'show me everything for employee 47570'.""")
    public ContractorDiscrepancySummary getContractorSummary(
            @ToolParam(description = "Contractor's employee code as it appears in the report.")
            String employeeCode,
            @ToolParam(required = false,
                    description = "Absolute path to the .xlsx report. If omitted, uses app.excel.default-path.")
            String excelPath) throws IOException {

        Path path = resolvePath(excelPath);
        List<ContractorDiscrepancySummary> all = detection.groupByContractor(detection.detect(path));
        return all.stream()
                .filter(s -> s.employeeCode().equalsIgnoreCase(employeeCode.trim()))
                .findFirst()
                .orElse(new ContractorDiscrepancySummary(employeeCode, "(not found)", 0, 0, 0, List.of()));
    }

    private Path resolvePath(String excelPath) {
        if (excelPath == null || excelPath.isBlank()) {
            return Paths.get(props.excel().defaultPath());
        }
        return Paths.get(excelPath.trim());
    }

    public record CheckResult(
            String reportPath,
            int totalDiscrepancyRows,
            int totalContractorsFlagged,
            List<ContractorDiscrepancySummary> contractors) {}

    public record SendDiscrepancyEmailResult(
            String recipient,
            int totalDiscrepancyRows,
            int totalContractorsFlagged,
            String subject,
            String body,
            boolean sent,
            String statusMessage) {}
}
