package com.timesheets.ai;

import com.timesheets.ai.config.AppProperties;
import com.timesheets.ai.excel.CellColorClassifier;
import com.timesheets.ai.excel.PtoReportParser;
import com.timesheets.ai.excel.TimesheetExcelParser;
import com.timesheets.ai.model.DiscrepancyRow;
import com.timesheets.ai.model.DiscrepancyType;
import com.timesheets.ai.model.PtoReportRow;
import com.timesheets.ai.sample.SampleReportGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests using a generated sample workbook. No Spring context, no LLM, no network.
 */
class TimesheetsAiApplicationTests {

    @Test
    void parserDetectsAllFourHighlightColours(@TempDir Path tmp) throws Exception {
        Path xlsx = tmp.resolve("sample.xlsx");
        SampleReportGenerator.write(xlsx);

        AppProperties props = new AppProperties(
                new AppProperties.Email("", "noreply@example.com", "Timesheets AI", "uday.rajpurohit@gmail.com"),
                new AppProperties.Excel(xlsx.toString(), "Weekly Timesheet", 7));

        TimesheetExcelParser parser = new TimesheetExcelParser(new CellColorClassifier(), props);
        List<DiscrepancyRow> rows = parser.parse(xlsx);

        assertTrue(rows.size() >= 8, "expected at least 8 flagged rows, got " + rows.size());

        Map<DiscrepancyType, Long> byType = rows.stream()
                .collect(Collectors.groupingBy(DiscrepancyRow::discrepancyType, Collectors.counting()));

        assertTrue(byType.getOrDefault(DiscrepancyType.UNDER_REPORT, 0L)  > 0, "no UNDER_REPORT detected");
        assertTrue(byType.getOrDefault(DiscrepancyType.OVER_REPORT, 0L)   > 0, "no OVER_REPORT detected");
        assertTrue(byType.getOrDefault(DiscrepancyType.NO_RECORD_TEAL, 0L) > 0, "no NO_RECORD_TEAL detected");
        assertTrue(byType.getOrDefault(DiscrepancyType.NO_RECORD_GREY, 0L) > 0, "no NO_RECORD_GREY detected");

        // Spot-check: under-reports include Abdul Tawab Chaudhary (47570) per the sample.
        DiscrepancyRow firstUnder = rows.stream()
                .filter(r -> r.discrepancyType() == DiscrepancyType.UNDER_REPORT)
                .findFirst().orElseThrow();
        assertEquals("47570", firstUnder.employeeCode());

        // Beeline-side columns should be populated (not empty) for at least one row.
        assertTrue(rows.stream().anyMatch(r -> !r.emailId().isEmpty()),
                "expected at least one row to carry a Beeline-side email");
        assertTrue(rows.stream().anyMatch(r -> !r.beelineReportedHours().isEmpty()),
                "expected at least one row to carry Beeline reported hours");
    }

    @Test
    void ptoParserFlagsCommentedRowsAsMismatch(@TempDir Path tmp) throws Exception {
        Path xlsx = tmp.resolve("sample.xlsx");
        SampleReportGenerator.write(xlsx);

        PtoReportParser parser = new PtoReportParser();
        List<PtoReportRow> rows = parser.parse(xlsx);

        assertTrue(rows.size() >= 10, "expected at least 10 PTO rows, got " + rows.size());

        long mismatches = rows.stream().filter(PtoReportRow::isMismatch).count();
        assertTrue(mismatches >= 5,
                "expected at least 5 mismatches (rows with non-empty Comments), got " + mismatches);

        // Spot-check: "Data not found" comment should be flagged.
        assertTrue(rows.stream().anyMatch(r ->
                        r.isMismatch() && r.comments().toLowerCase().contains("data not found")),
                "expected a 'Data not found' mismatch row");
    }
}
