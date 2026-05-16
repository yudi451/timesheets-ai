package com.timesheets.ai.dashboard;

import com.timesheets.ai.config.AppProperties;
import com.timesheets.ai.dashboard.DashboardDtos.AiInsights;
import com.timesheets.ai.dashboard.DashboardDtos.ContractorRow;
import com.timesheets.ai.dashboard.DashboardDtos.DashboardSummary;
import com.timesheets.ai.dashboard.DashboardDtos.DiscrepancyBreakdown;
import com.timesheets.ai.dashboard.DashboardDtos.Kpis;
import com.timesheets.ai.dashboard.DashboardDtos.ReportTab;
import com.timesheets.ai.dashboard.DashboardDtos.WeeklyTrendPoint;
import com.timesheets.ai.model.ContractorDiscrepancySummary;
import com.timesheets.ai.model.DiscrepancyRow;
import com.timesheets.ai.model.DiscrepancyType;
import com.timesheets.ai.service.DiscrepancyDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the dashboard payload for the React app.
 *
 * Real data: contractor counts, discrepancy breakdown, top offenders.
 * Mocked data (clearly tagged below): Manager/ReportsTo hierarchy, Revenue at Risk,
 * Resolved %, weekly trend line, PTO mismatch / Billing mismatch slices, report tabs.
 * Replace each MOCK block when the upstream generator emits the underlying fields.
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    // ---- MOCK constants — tune or swap when real data lands ---------------------------
    private static final double MOCK_HOURLY_RATE_USD = 95.0;
    private static final int    MOCK_RESOLVED_PERCENT = 32;
    private static final int    MOCK_PTO_MISMATCH = 7;
    private static final int    MOCK_BILLING_MISMATCH = 4;
    private static final String[] MOCK_MANAGERS = {
            "Sarah Chen", "Marcus Patel", "Elena Vasquez", "David Kim"
    };
    private static final String MOCK_REPORTS_TO = "VP — Delivery Ops";
    // -----------------------------------------------------------------------------------

    private final DiscrepancyDetectionService detection;
    private final AiInsightsService insightsService;
    private final AppProperties props;

    public DashboardService(
            DiscrepancyDetectionService detection,
            AiInsightsService insightsService,
            AppProperties props) {
        this.detection = detection;
        this.insightsService = insightsService;
        this.props = props;
    }

    public DashboardSummary buildSummary(String excelPathOverride) throws IOException {
        Path path = (excelPathOverride == null || excelPathOverride.isBlank())
                ? Paths.get(props.excel().defaultPath())
                : Paths.get(excelPathOverride);

        List<DiscrepancyRow> rows = detection.detect(path);
        List<ContractorDiscrepancySummary> contractors = detection.groupByContractor(rows);

        Kpis kpis = buildKpis(rows, contractors);
        List<DiscrepancyBreakdown> breakdown = buildBreakdown(rows);
        List<WeeklyTrendPoint> trend = buildMockTrend(rows.size());
        List<ContractorRow> contractorRows = buildContractorRows(contractors);
        AiInsights insights = insightsService.generate(contractors, rows.size());
        List<ReportTab> tabs = buildReportTabs(rows.size());

        log.info("Dashboard summary built: {} rows, {} contractors", rows.size(), contractors.size());

        return new DashboardSummary(
                path.toString(), kpis, breakdown, trend, contractorRows, insights, tabs);
    }

    // ---- real metrics ----------------------------------------------------------------

    private Kpis buildKpis(List<DiscrepancyRow> rows, List<ContractorDiscrepancySummary> contractors) {
        int missing = (int) rows.stream()
                .filter(r -> r.type() == DiscrepancyType.NO_RECORD_TEAL
                          || r.type() == DiscrepancyType.NO_RECORD_GREY)
                .count();
        int under = (int) rows.stream().filter(r -> r.type() == DiscrepancyType.UNDER_REPORT).count();

        // Critical = anything that costs money: missing weeks (uninvoiced) + under-reports.
        int critical = missing + under;
        // MOCK: assume each flagged row is one missed week at 40h × hourly rate.
        double revenueAtRisk = critical * 40.0 * MOCK_HOURLY_RATE_USD;

        return new Kpis(
                contractors.size(),
                rows.size(),
                critical,
                revenueAtRisk,
                MOCK_RESOLVED_PERCENT
        );
    }

    private List<DiscrepancyBreakdown> buildBreakdown(List<DiscrepancyRow> rows) {
        Map<DiscrepancyType, Integer> counts = new HashMap<>();
        for (DiscrepancyType t : DiscrepancyType.values()) counts.put(t, 0);
        for (DiscrepancyRow r : rows) counts.merge(r.type(), 1, Integer::sum);

        // Combine the two NO_RECORD variants into one "Missing" slice for the pie.
        int missing = counts.get(DiscrepancyType.NO_RECORD_TEAL)
                    + counts.get(DiscrepancyType.NO_RECORD_GREY);

        List<DiscrepancyBreakdown> slices = new ArrayList<>();
        slices.add(new DiscrepancyBreakdown("Missing",          missing));
        slices.add(new DiscrepancyBreakdown("Under-report",     counts.get(DiscrepancyType.UNDER_REPORT)));
        slices.add(new DiscrepancyBreakdown("Over-report",      counts.get(DiscrepancyType.OVER_REPORT)));
        slices.add(new DiscrepancyBreakdown("PTO Mismatch",     MOCK_PTO_MISMATCH));     // MOCK
        slices.add(new DiscrepancyBreakdown("Billing Mismatch", MOCK_BILLING_MISMATCH)); // MOCK
        return slices;
    }

    private List<ContractorRow> buildContractorRows(List<ContractorDiscrepancySummary> contractors) {
        List<ContractorRow> result = new ArrayList<>();
        int i = 0;
        for (ContractorDiscrepancySummary c : contractors) {
            // MOCK: rotate through fake manager names.
            String manager = MOCK_MANAGERS[i % MOCK_MANAGERS.length];
            i++;

            double impact = (c.underReportCount() + c.noRecordCount()) * 40.0 * MOCK_HOURLY_RATE_USD;
            String status = c.total() >= 3 ? "At Risk" : "Action Needed";

            result.add(new ContractorRow(
                    c.employeeCode(),
                    c.employeeName(),
                    MOCK_REPORTS_TO,
                    manager,
                    c.underReportCount(),
                    c.overReportCount(),
                    c.noRecordCount(),
                    c.total(),
                    impact,
                    status
            ));
        }
        return result;
    }

    // ---- mocked metrics --------------------------------------------------------------

    /** MOCK: fabricates 8 weekly data points trending toward the current total. */
    private List<WeeklyTrendPoint> buildMockTrend(int currentTotal) {
        // Eight-week curve ending at currentTotal, with some noise to look real.
        int[] shape = {3, 8, 6, 14, 11, 17, 12, Math.max(currentTotal, 9)};
        String[] labels = {
                "Wk -7", "Wk -6", "Wk -5", "Wk -4", "Wk -3", "Wk -2", "Wk -1", "This Week"
        };
        List<WeeklyTrendPoint> result = new ArrayList<>();
        for (int i = 0; i < shape.length; i++) {
            result.add(new WeeklyTrendPoint(labels[i], shape[i]));
        }
        return result;
    }

    /** MOCK: pretend the Reports section has these tabs with these row counts. */
    private List<ReportTab> buildReportTabs(int discrepancyRows) {
        return List.of(
                new ReportTab("Weekly Timesheet", "All highlighted rows from the latest report", discrepancyRows),
                new ReportTab("PTO Report",       "PTO days vs. Fusion / Beeline records",       MOCK_PTO_MISMATCH),
                new ReportTab("Invoiced Amount",  "Billed vs. reported hours",                   MOCK_BILLING_MISMATCH),
                new ReportTab("Timesheet Status", "Submitted / Approved / Rejected breakdown",   discrepancyRows + 12)
        );
    }
}
