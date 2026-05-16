package com.timesheets.ai.web;

import com.timesheets.ai.mcp.TimesheetTools;
import com.timesheets.ai.mcp.TimesheetTools.SendDiscrepancyEmailResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

/**
 * REST endpoint the dashboard button uses to send the manager email.
 *
 * Delegates to the same TimesheetTools.sendDiscrepancyEmail() that MCP clients call,
 * so a press of the dashboard button and a chat command from Claude Desktop run
 * the exact same code path.
 */
@RestController
@RequestMapping("/api/email")
public class EmailController {

    private final TimesheetTools tools;

    public EmailController(TimesheetTools tools) {
        this.tools = tools;
    }

    @PostMapping("/send-summary")
    public SendDiscrepancyEmailResult sendSummary(
            @RequestParam(name = "path", required = false) String excelPath,
            @RequestParam(name = "recipient", required = false) String recipient) throws IOException {
        return tools.sendDiscrepancyEmail(excelPath, recipient);
    }
}
