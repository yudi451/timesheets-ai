I'm Uday Rajpurohit, working on a personal/learning project called Timesheets AI.
I've been building it with Claude Code (CLI) on my laptop and want to keep
discussing it on mobile while I'm away from the keyboard. You can't edit files
or run commands — just help me think through next steps, explain concepts, and
review code I paste in.

## The project

A Spring Boot service that reads a "Beeline_Fusion_Report.xlsx" — a weekly
timesheet reconciliation report — finds the rows highlighted as discrepancies
(red = under-report, yellow = over-report, teal/grey "NA" = missing record),
asks Claude to draft a manager-ready summary email, and ships it via SendGrid.

It also has a "PTO report" sheet — rows where the Comments column is non-empty
are flagged as PTO mismatches.

## Repo

https://github.com/yudi451/timesheets-ai  (public)

## Stack

- Java 21, Spring Boot 3.5, Spring AI 1.0.5
- Anthropic Claude Sonnet 4.6 (via Spring AI ChatClient) — drafts email body + insights
- Apache POI 5.4 for .xlsx parsing
- SendGrid for email delivery
- React 19 + Vite + TypeScript + Tailwind + Recharts for a dashboard
- MCP server (Spring AI starter) exposes 3 tools so Claude Desktop / Code can
  drive the same actions conversationally

## What's working today

1. **CLI mode**: --app.cli.run=true reads the .xlsx, drafts the email,
   sends it. Used for scheduled jobs.
2. **MCP server**: 3 tools — checkDiscrepancies, sendDiscrepancyEmail,
   getContractorSummary. Reachable at http://localhost:8080/sse.
3. **REST API + Dashboard**: /api/dashboard/summary returns KPIs, pie chart
   data, contractor grid, AI insights. React dashboard at localhost:5173 renders
   them with KPI cards, pie chart, line chart, contractor table, AI insights
   panel, PTO mismatch panel, reports tabs.
4. **"Email summary to manager" button** on the dashboard — POST
   /api/email/send-summary. Has confirm → loading → success/error states.
5. **Multi-recipient**: default-recipient in application.yml accepts
   comma-separated addresses; all go on the To: line.

## What's mocked (clearly labeled in DashboardService.java)

- Revenue at Risk (assumes $95/hr × 40h × flagged weeks)
- Resolved %
- Billing Mismatch slice in pie chart
- Manager / Reports To hierarchy (rotates through 4 fake names)
- Weekly trend line (8-week shape ending at real current count)

Real data: discrepancy counts, contractor list + emails, PTO mismatch count.

## Open issues I'm thinking about

1. **Emails landing in spam** — using SendGrid Single Sender Verification with
   a gmail.com from-address. Real fix needs SendGrid Domain Authentication on
   a domain I control (or Persistent.com subdomain via IT ticket).
2. **Sample data is fake** — real reports need to come from upstream timesheet
   generator (assumed to exist).
3. **Spring AI starter has a sub-model bug** — had to disable embedding/image/
   audio sub-models in application.yml or OpenAI starter fails to load.
4. **Cell color classifier uses RGB-distance bucketing** — if upstream changes
   shades, centroids in CellColorClassifier.java need tuning.

## Things I might ask you about

- How to set up SendGrid Domain Authentication
- How MCP works (server vs client side, STDIO vs HTTP/SSE)
- How to deploy this (Docker? cloud? on-prem at Persistent?)
- How to add new MCP tools
- How to schedule the email send (cron, Quartz, etc.)
- Code review on snippets I paste

When I come back to my laptop, I'll resume the live Claude Code session with
`claude --continue` from the project directory. Until then, please help me
think through whatever comes up.
