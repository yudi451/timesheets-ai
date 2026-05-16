package com.timesheets.ai.dashboard;

import java.util.List;

/**
 * Wire-format records returned to the React dashboard. Field names are kept
 * camelCase to match TypeScript conventions on the frontend.
 *
 * Anything tagged "mocked" in the brief lives here too — clearly named so we
 * remember to replace it once the upstream data is available.
 */
public final class DashboardDtos {

    private DashboardDtos() {}

    /** Top-of-page summary the React app calls once on load. */
    public record DashboardSummary(
            String reportPath,
            Kpis kpis,
            List<DiscrepancyBreakdown> discrepancyBreakdown,
            List<WeeklyTrendPoint> weeklyTrend,
            List<ContractorRow> contractors,
            AiInsights aiInsights,
            List<ReportTab> reportTabs
    ) {}

    /** The 5 cards at the top. underReport/overReport/missing are real; rest are mocked. */
    public record Kpis(
            int totalResources,
            int totalDiscrepancies,
            int criticalIssues,
            double revenueAtRiskUsd,
            int resolvedPercent
    ) {}

    /** Pie chart slices. */
    public record DiscrepancyBreakdown(String label, int count) {}

    /** One point on the weekly trend line. */
    public record WeeklyTrendPoint(String weekLabel, int discrepancies) {}

    /** One row in the detailed grid. */
    public record ContractorRow(
            String employeeCode,
            String employeeName,
            String reportsTo,
            String manager,
            int underReport,
            int overReport,
            int missing,
            int total,
            double impactUsd,
            String status
    ) {}

    /** AI-generated bullet points. */
    public record AiInsights(
            List<String> topIssues,
            List<String> repeatOffenders,
            List<String> highRiskProjects
    ) {}

    /** Stub data for the Reports section tabs. */
    public record ReportTab(String label, String description, int rowCount) {}
}
