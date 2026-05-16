package com.timesheets.ai.cli;

import com.timesheets.ai.config.AppProperties;
import com.timesheets.ai.mcp.TimesheetTools;
import com.timesheets.ai.mcp.TimesheetTools.SendDiscrepancyEmailResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs the full pipeline once at boot when --app.cli.run=true is passed.
 * Lets you test the workflow without wiring up an MCP client.
 *
 * Usage:
 *   ./mvnw spring-boot:run -Dspring-boot.run.arguments="--app.cli.run=true \
 *     --app.cli.excel-path=./sample-data/sample-beeline-fusion-report.xlsx \
 *     --app.cli.recipient=uday.rajpurohit@gmail.com"
 */
@Component
@ConditionalOnProperty(name = "app.cli.run", havingValue = "true")
public class DiscrepancyCliRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DiscrepancyCliRunner.class);

    private final TimesheetTools tools;
    private final AppProperties props;

    public DiscrepancyCliRunner(TimesheetTools tools, AppProperties props) {
        this.tools = tools;
        this.props = props;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String excelPath = firstOrNull(args, "app.cli.excel-path");
        String recipient = firstOrNull(args, "app.cli.recipient");

        log.info("CLI mode: scanning report and emailing {}",
                recipient == null ? props.email().defaultRecipient() : recipient);

        SendDiscrepancyEmailResult result = tools.sendDiscrepancyEmail(excelPath, recipient);

        log.info("=== Result ===");
        log.info("Recipient            : {}", result.recipient());
        log.info("Flagged rows         : {}", result.totalDiscrepancyRows());
        log.info("Flagged contractors  : {}", result.totalContractorsFlagged());
        log.info("Email subject        : {}", result.subject());
        log.info("Send success         : {}", result.sent());
        log.info("Status               : {}", result.statusMessage());
        if (result.body() != null) {
            log.info("---- Email body ----\n{}", result.body());
        }
    }

    private static String firstOrNull(ApplicationArguments args, String name) {
        if (!args.containsOption(name)) return null;
        var values = args.getOptionValues(name);
        return values == null || values.isEmpty() ? null : values.get(0);
    }
}
