# Timesheets AI

AI-powered timesheet discrepancy detection for Beeline vs Fusion reconciliation.
Reads a `Beeline_Fusion_Report.xlsx`, finds highlighted rows (red / yellow / teal-NA / grey-NA),
asks Claude to draft a manager-ready summary, and ships it via SendGrid. Comes with a
**React dashboard**, **MCP server** for chat-driven access, and a CLI for scheduled jobs.

**Stack:** Java 17+ · Spring Boot 3.5 · Spring AI 1.0.5 · Apache POI · SendGrid ·
React 19 · Vite · Tailwind · Recharts.

---

## Prerequisites

| Tool | Version | Install |
| --- | --- | --- |
| **Java JDK** | 17+ | `brew install openjdk@17` or [Temurin](https://adoptium.net/) |
| **Node.js** | 20+ | `brew install node` |
| **Anthropic API key** | — | Create at https://console.anthropic.com/settings/keys (free $5 credit on signup) |
| **SendGrid API key + verified sender** | — | https://signup.sendgrid.com (free 100 emails/day) — see [SendGrid setup](#sendgrid-setup) below |

Maven is **not** required — the repo ships with `mvnw` (Maven wrapper).

---

## Quick start (5 minutes)

```bash
# 1. Clone
git clone https://github.com/yudi451/timesheets-ai.git
cd timesheets-ai

# 2. Configure environment
cp .env.example .env
# Open .env and paste your real ANTHROPIC_API_KEY, SENDGRID_API_KEY, EMAIL_FROM

# 3. Generate the sample report (one-time, file is gitignored)
./mvnw -q compile
./mvnw -q dependency:build-classpath -Dmdep.outputFile=.cp.txt 2>/dev/null
java -cp "target/classes:$(cat .cp.txt)" \
  com.timesheets.ai.sample.SampleReportGenerator ./sample-data/sample-beeline-fusion-report.xlsx
rm .cp.txt

# 4. Start the backend (terminal 1)
set -a; source .env; set +a
./mvnw spring-boot:run

# 5. Start the frontend (terminal 2)
cd frontend
npm install
npm run dev

# 6. Open the dashboard
open http://localhost:5173
```

You should see KPI cards, charts, an AI-generated insights panel, and a contractor
breakdown table.

---

## SendGrid setup

SendGrid won't deliver email from an unverified address. Do this once:

1. Sign up at https://signup.sendgrid.com
2. **Settings → Sender Authentication → Single Sender Verification → Create New Sender**
3. Use an email you actually control (e.g. your gmail). Fill in name + company fields.
4. Check your inbox for the verification link → click it.
5. **Settings → API Keys → Create API Key** → "Restricted Access" → grant **Mail Send: Full Access** only.
6. Copy the key (starts with `SG.`) into `.env` as `SENDGRID_API_KEY=...`.
7. Set `EMAIL_FROM=` in `.env` to the address you just verified.

If `SENDGRID_API_KEY` is left blank, the app runs in **dry-run mode** and just logs
the email body instead of sending — useful for testing without burning credits.

---

## Running modes

### A. Dashboard (default — what most users want)

`./mvnw spring-boot:run` on the backend, `npm run dev` in `frontend/`, browse to
`http://localhost:5173`. The dashboard hits `/api/dashboard/summary` once on load.

### B. CLI one-shot (for scheduled jobs or smoke tests)

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="\
  --app.cli.run=true \
  --app.cli.excel-path=./sample-data/sample-beeline-fusion-report.xlsx \
  --app.cli.recipient=manager@example.com"
```

This boots the app, scans the report, sends the email, and exits.

### C. MCP server (for Claude Desktop / Claude Code chat)

Start the backend normally (`./mvnw spring-boot:run`) — the MCP endpoint is at
`http://localhost:8080/sse`. Wire it into your MCP client:

**Claude Desktop** — edit `~/Library/Application Support/Claude/claude_desktop_config.json`:

```json
{
  "mcpServers": {
    "timesheets-ai": { "url": "http://localhost:8080/sse" }
  }
}
```

Restart Claude Desktop, then chat:

> Use timesheets-ai to scan today's report and tell me which contractor is most at risk.

**Claude Code**:
```bash
claude mcp add --transport sse timesheets-ai http://localhost:8080/sse
```

---

## What's in the repo

```
backend (Spring Boot)
  src/main/java/com/timesheets/ai/
    TimesheetsAiApplication.java     boot + MCP tool registration
    cli/         CLI runner for one-shot mode
    config/      AppProperties, ChatClientConfig
    dashboard/   DashboardService, AiInsightsService, DTOs (for React)
    excel/       CellColorClassifier, TimesheetExcelParser
    mcp/         TimesheetTools  ← @Tool methods exposed via MCP
    model/       DiscrepancyType / Row / ContractorSummary
    sample/      SampleReportGenerator (writes a fixture .xlsx)
    service/     DiscrepancyDetectionService, EmailDraftingService, SendGridEmailService
    web/         DashboardController, CorsConfig
  src/main/resources/
    application.yml
    prompts/discrepancy-email.st     LLM prompt for the email body

frontend (React + Vite)
  frontend/src/
    App.tsx
    api/dashboard.ts                 fetch + TS types matching backend DTOs
    components/                      KpiCards, DiscrepancyPie, WeeklyTrendLine,
                                     ContractorGrid, AiInsightsPanel, ReportsTabs
```

---

## What's real vs mocked

Designed for v1 where the upstream timesheet generator only emits per-row
discrepancy highlights. Anything we don't yet receive is mocked **in one place**
(top of `DashboardService.java`) so swapping in real data is a single-file edit.

**Real:** discrepancy counts (Missing / Under / Over), contractor list, AI Insights
(Claude-generated per load), email subject + body, SendGrid delivery.

**Mocked:** Revenue at Risk ($95/hr assumed), Resolved %, PTO Mismatch, Billing
Mismatch, Manager / Reports To hierarchy, weekly trend line (8-week curve), report
tab descriptions.

---

## Configuration

All in [`src/main/resources/application.yml`](src/main/resources/application.yml):

| Property | Default | Purpose |
| --- | --- | --- |
| `spring.ai.model.chat` | `anthropic` | LLM provider — `anthropic` or `openai` |
| `app.email.default-recipient` | `uday.rajpurohit@gmail.com` | Where the email goes if no override |
| `app.email.from-address` | from `EMAIL_FROM` | Verified SendGrid sender |
| `app.excel.default-path` | `./sample-data/sample-beeline-fusion-report.xlsx` | Used by MCP tools when no path is passed |
| `app.excel.sheet-name` | `Weekly Timesheet` | Worksheet to scan |
| `app.excel.fusion-hours-column-index` | `6` | 0-indexed column where the highlighted cell lives |

To switch from Claude to OpenAI: change `spring.ai.model.chat: openai` and set
`OPENAI_API_KEY` in `.env`. No code change needed.

---

## Tests

```bash
./mvnw test
```

`TimesheetExcelParserTest` generates a fixture workbook, runs the parser, and
asserts all four colour buckets are detected. No API keys, no network.

---

## Troubleshooting

| Symptom | Fix |
| --- | --- |
| `OpenAI API key must be set` on startup | `spring.ai.model.chat` is set to `openai` but no key. Either set `OPENAI_API_KEY` or switch back to `anthropic`. |
| `401 x-api-key header is required` | `ANTHROPIC_API_KEY` is empty or wrong. Verify `.env` has the full key (starts `sk-ant-api03-`, ~110 chars). |
| `SendGrid 403 — from address does not match verified Sender Identity` | Your `EMAIL_FROM` isn't verified in SendGrid yet. See [SendGrid setup](#sendgrid-setup). |
| Dashboard shows "Couldn't load" | Backend isn't running. Start it with `./mvnw spring-boot:run`. |
| `Port 8080 already in use` | Another instance is running. Stop it (or change `server.port` in `application.yml`). |

---

## Notes

- The cell-colour classifier uses RGB-distance bucketing — shading variants of the
  same hue still match. Tune centroids in [`CellColorClassifier.java`](src/main/java/com/timesheets/ai/excel/CellColorClassifier.java) for different upstream colour schemes.
- The LLM email prompt is at [`src/main/resources/prompts/discrepancy-email.st`](src/main/resources/prompts/discrepancy-email.st) — edit to change tone or structure.
- The AI Insights prompt is inline at the top of [`AiInsightsService.java`](src/main/java/com/timesheets/ai/dashboard/AiInsightsService.java).
