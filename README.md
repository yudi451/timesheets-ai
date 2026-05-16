# Timesheets AI — MCP server for Beeline vs Fusion discrepancies

Java 21 + Spring Boot 3.5 + Spring AI 1.0.5. Scans a `Beeline_Fusion_Report.xlsx`,
finds the rows that are highlighted (red / yellow / teal-NA / grey-NA), asks an
LLM to draft a manager-ready email, and ships it via SendGrid.

The whole pipeline is exposed as **MCP tools**, so you can drive it conversationally
from Claude Desktop, Claude Code, or any MCP-aware client.

## How the discrepancies map

The upstream timesheet-generation step (your code, assumed already in place) writes
the report with these fill colours on the **Fusion Reported Hours** cell:

| Colour            | Meaning                                       | Tool reports as     |
| ----------------- | --------------------------------------------- | ------------------- |
| Red               | Beeline hours **<** Fusion hours              | `UNDER_REPORT`      |
| Yellow            | Beeline hours **>** Fusion hours              | `OVER_REPORT`       |
| Teal / cyan `NA`  | No matching Beeline record this week          | `NO_RECORD_TEAL`    |
| Grey `NA`         | No matching record (older format)             | `NO_RECORD_GREY`    |

## What's in the box

```
src/main/java/com/timesheets/ai/
├── TimesheetsAiApplication.java     boot + ToolCallbackProvider registration
├── config/
│   ├── AppProperties.java           binds `app.*` properties
│   └── ChatClientConfig.java        picks Anthropic or OpenAI by app.llm.provider
├── excel/
│   ├── CellColor.java
│   ├── CellColorClassifier.java     POI ARGB → bucket by RGB distance
│   └── TimesheetExcelParser.java    reads "Weekly Timesheet" sheet
├── model/                           DiscrepancyType, DiscrepancyRow, ContractorSummary
├── service/
│   ├── DiscrepancyDetectionService.java   parser + group-by-contractor
│   ├── EmailDraftingService.java          Spring AI ChatClient + prompt template
│   └── SendGridEmailService.java          SendGrid REST send
├── mcp/
│   └── TimesheetTools.java          @Tool methods exposed via MCP
├── cli/
│   └── DiscrepancyCliRunner.java    one-shot run when --app.cli.run=true
└── sample/
    └── SampleReportGenerator.java   writes a sample .xlsx so you can test end-to-end
```

## Setup

```bash
cd /Users/uday/WORKSPACES/Timesheets-AI
cp .env.example .env
# Edit .env and set ANTHROPIC_API_KEY (or OPENAI_API_KEY) and optionally SENDGRID_API_KEY.
# If SENDGRID_API_KEY is blank the app logs the email instead of sending it (dry-run).

# Generate a sample report so you have something to scan.
./mvnw -q compile
./mvnw -q exec:java \
  -Dexec.mainClass=com.timesheets.ai.sample.SampleReportGenerator \
  -Dexec.classpathScope=compile
```

Set environment variables before running:

```bash
set -a; source .env; set +a
```

## Run modes

### 1. CLI (no MCP client needed)

Useful for smoke-testing the full pipeline.

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="\
  --app.cli.run=true \
  --app.cli.excel-path=./sample-data/sample-beeline-fusion-report.xlsx \
  --app.cli.recipient=uday.rajpurohit@gmail.com"
```

You'll see the LLM-drafted subject + body in the logs and, if SendGrid is configured,
the email will land in the recipient's inbox.

### 2. MCP server

Just start the app — the MCP server is on by default at `http://localhost:8080/sse`.

```bash
./mvnw spring-boot:run
```

The server exposes three tools:

| Tool                       | What it does                                                                 |
| -------------------------- | ---------------------------------------------------------------------------- |
| `checkDiscrepancies`       | Scan the report, return all flagged rows grouped by contractor.              |
| `sendDiscrepancyEmail`     | Scan → LLM draft → SendGrid send. Recipient defaults to the configured one.  |
| `getContractorSummary`     | Drill into one contractor by employee code.                                  |

#### Wire it into Claude Desktop

Edit `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "timesheets-ai": {
      "url": "http://localhost:8080/sse"
    }
  }
}
```

Restart Claude Desktop. Then in any chat:

> Use the timesheets-ai tools to scan today's report and email me a summary.

#### Wire it into Claude Code

```bash
claude mcp add --transport sse timesheets-ai http://localhost:8080/sse
```

## Switching LLM providers

```yaml
# application.yml
app:
  llm:
    provider: openai   # or: anthropic
```

Make sure the matching API key is exported. Both starters are on the classpath; the
`ChatClientConfig` bean picks one based on this property and fails fast with a clear
message if the chosen provider's API key is missing.

## Configuration knobs

All under `app.*` in `application.yml`:

| Property                            | What it controls                                              |
| ----------------------------------- | ------------------------------------------------------------- |
| `app.llm.provider`                  | `anthropic` or `openai`                                       |
| `app.email.default-recipient`       | Where `sendDiscrepancyEmail` ships to when no override given. |
| `app.email.from-address`            | Verified SendGrid sender.                                     |
| `app.excel.default-path`            | Used when an MCP tool is called without an explicit path.     |
| `app.excel.sheet-name`              | Worksheet to scan. Defaults to `Weekly Timesheet`.            |
| `app.excel.fusion-hours-column-index` | 0-indexed column holding the highlighted cell. Default `7`. |

## Tests

```bash
./mvnw test
```

The bundled `TimesheetExcelParserTest` generates a fixture workbook, runs it through
the parser, and asserts that all four colour buckets are detected. It needs no API
keys and no network.

## Notes

- The cell-colour classifier uses RGB-distance bucketing, so shading variants of the
  same hue (e.g. light vs deep red) still match. Tune the centroids in
  `CellColorClassifier` if your upstream generator uses different shades.
- The LLM prompt template lives at `src/main/resources/prompts/discrepancy-email.st` —
  edit it to change the email tone, length, or structure.
