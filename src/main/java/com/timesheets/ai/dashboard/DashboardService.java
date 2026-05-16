package com.timesheets.ai.dashboard;

import com.timesheets.ai.config.AppProperties;
import com.timesheets.ai.dashboard.DashboardDtos.AiInsights;
import com.timesheets.ai.dashboard.DashboardDtos.ContractorRow;
import com.timesheets.ai.dashboard.DashboardDtos.DashboardSummary;
import com.timesheets.ai.dashboard.DashboardDtos.DiscrepancyBreakdown;
import com.timesheets.ai.dashboard.DashboardDtos.Kpis;
import com.timesheets.ai.dashboard.DashboardDtos.PtoRow;
import com.timesheets.ai.dashboard.DashboardDtos.ReportTab;
import com.timesheets.ai.dashboard.DashboardDtos.WeeklyTrendPoint;
import com.timesheets.ai.model.ContractorDiscrepancySummary;
import com.timesheets.ai.model.DiscrepancyRow;
import com.timesheets.ai.model.DiscrepancyType;
import com.timesheets.ai.model.PtoReportRow;
import com.timesheets.ai.service.DiscrepancyDetectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds the dashboard payload for the React app.
 *
 * Real data: contractor counts, discrepancy breakdown, top offenders, contractor
 * emails (from the Beeline side), PTO mismatches from the PTO Report sheet.
 *
 * Mocked data (clearly tagged below): Manager/ReportsTo hierarchy, Revenue at Risk,
 * Resolved %, weekly trend line, Billing mismatch slice, report tabs. Replace each
 * MOCK block when the upstream generator emits the underlying fields.
 */
@Service
public class DashboardService {

    private static final Logger log = LoggerFactory.getLogger(DashboardService.class);

    // ---- MOCK constants — tune or swap when real data lands ---------------------------
    private static final double MOCK_HOURLY_RATE_USD = 95.0;
    private static final int    MOCK_RESOLVED_PERCENT = 32;
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
        List<PtoReportRow> ptoRows = detection.detectPto(path);
        int ptoMismatchCount = (int) ptoRows.stream().filter(PtoReportRow::isMismatch).count();

        Map<String, String> emailByEmpCode = buildEmailLookup(rows);

        Kpis kpis = buildKpis(rows, contractors, ptoMismatchCount);
        List<DiscrepancyBreakdown> breakdown = buildBreakdown(rows, ptoMismatchCount);
        List<WeeklyTrendPoint> trend = buildMockTrend(rows.size());
        List<ContractorRow> contractorRows = buildContractorRows(contractors, emailByEmpCode);
        List<PtoRow> ptoMismatches = buildPtoMismatchRows(ptoRows);
        // Cache AI insights by (path + mtime) — same report file → reuse the LLM response.
        String insightsCacheKey;
        try {
            insightsCacheKey = path.toString() + "@" + Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            insightsCacheKey = null;
        }
        AiInsights insights = insightsService.generate(contractors, rows.size(), insightsCacheKey);
        List<ReportTab> tabs = buildReportTabs(rows.size(), ptoMismatchCount);

        log.info("Dashboard summary built: {} discrepancy rows, {} contractors, {} PTO mismatches",
                rows.size(), contractors.size(), ptoMismatchCount);

        return new DashboardSummary(
                path.toString(), kpis, breakdown, trend,
                contractorRows, ptoMismatches, insights, tabs);
    }

    // ---- real metrics ----------------------------------------------------------------

    private Kpis buildKpis(
            List<DiscrepancyRow> rows,
            List<ContractorDiscrepancySummary> contractors,
            int ptoMismatchCount) {
        int missing = (int) rows.stream()
                .filter(r -> r.discrepancyType() == DiscrepancyType.NO_RECORD_TEAL
                          || r.discrepancyType() == DiscrepancyType.NO_RECORD_GREY)
                .count();
        int under = (int) rows.stream().filter(r -> r.discrepancyType() == DiscrepancyType.UNDER_REPORT).count();

        int critical = missing + under + ptoMismatchCount;
        double revenueAtRisk = critical * 40.0 * MOCK_HOURLY_RATE_USD;

        return new Kpis(
                contractors.size(),
                rows.size() + ptoMismatchCount,
                critical,
                revenueAtRisk,
                MOCK_RESOLVED_PERCENT
        );
    }

    private List<DiscrepancyBreakdown> buildBreakdown(List<DiscrepancyRow> rows, int ptoMismatchCount) {
        Map<DiscrepancyType, Integer> counts = new HashMap<>();
        for (DiscrepancyType t : DiscrepancyType.values()) counts.put(t, 0);
        for (DiscrepancyRow r : rows) counts.merge(r.discrepancyType(), 1, Integer::sum);

        int missing = counts.get(DiscrepancyType.NO_RECORD_TEAL)
                    + counts.get(DiscrepancyType.NO_RECORD_GREY);

        List<DiscrepancyBreakdown> slices = new ArrayList<>();
        slices.add(new DiscrepancyBreakdown("Missing",          missing));
        slices.add(new DiscrepancyBreakdown("Under-report",     counts.get(DiscrepancyType.UNDER_REPORT)));
        slices.add(new DiscrepancyBreakdown("Over-report",      counts.get(DiscrepancyType.OVER_REPORT)));
        slices.add(new DiscrepancyBreakdown("PTO Mismatch",     ptoMismatchCount));     // REAL now
        slices.add(new DiscrepancyBreakdown("Billing Mismatch", MOCK_BILLING_MISMATCH)); // MOCK
        return slices;
    }

    private Map<String, String> buildEmailLookup(List<DiscrepancyRow> rows) {
        Map<String, String> lookup = new HashMap<>();
        for (DiscrepancyRow r : rows) {
            if (!r.emailId().isEmpty()) lookup.putIfAbsent(r.employeeCode(), r.emailId());
        }
        return lookup;
    }

    private List<ContractorRow> buildContractorRows(
            List<ContractorDiscrepancySummary> contractors,
            Map<String, String> emailByEmpCode) {
        List<ContractorRow> result = new ArrayList<>();
        int i = 0;
        for (ContractorDiscrepancySummary c : contractors) {
            String manager = MOCK_MANAGERS[i % MOCK_MANAGERS.length];
            i++;

            double impact = (c.underReportCount() + c.noRecordCount()) * 40.0 * MOCK_HOURLY_RATE_USD;
            String status = c.total() >= 3 ? "At Risk" : "Action Needed";
            String email = emailByEmpCode.getOrDefault(c.employeeCode(), "");

            result.add(new ContractorRow(
                    c.employeeCode(),
                    c.employeeName(),
                    MOCK_REPORTS_TO,
                    manager,
                    email,
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

    private List<PtoRow> buildPtoMismatchRows(List<PtoReportRow> ptoRows) {
        List<PtoRow> result = new ArrayList<>();
        for (PtoReportRow r : ptoRows) {
            if (!r.isMismatch()) continue;
            result.add(new PtoRow(
                    r.employeeCode(),
                    r.employeeName(),
                    r.sowName(),
                    r.ptoDays(),
                    r.comments()
            ));
        }
        return result;
    }

    // ---- mocked metrics --------------------------------------------------------------

    private List<WeeklyTrendPoint> buildMockTrend(int currentTotal) {
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

    private List<ReportTab> buildReportTabs(int discrepancyRows, int ptoMismatchCount) {
        return List.of(
                new ReportTab("Weekly Timesheet", "All highlighted rows from the latest report", discrepancyRows),
                new ReportTab("PTO Report",       "Rows flagged with a Comments note",            ptoMismatchCount),
                new ReportTab("Invoiced Amount",  "Billed vs. reported hours",                    MOCK_BILLING_MISMATCH),
                new ReportTab("Timesheet Status", "Submitted / Approved / Rejected breakdown",    discrepancyRows + 12)
        );
    }
}
