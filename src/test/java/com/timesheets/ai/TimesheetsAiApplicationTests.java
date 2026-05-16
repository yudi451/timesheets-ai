package com.timesheets.ai;

import com.timesheets.ai.config.AppProperties;
import com.timesheets.ai.excel.CellColorClassifier;
import com.timesheets.ai.excel.TimesheetExcelParser;
import com.timesheets.ai.model.DiscrepancyRow;
import com.timesheets.ai.model.DiscrepancyType;
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
 * Unit test for the Excel parser using a generated sample workbook.
 * No Spring context, no LLM, no network — runs in CI without secrets.
 */
class TimesheetExcelParserTest {

    @Test
    void detectsAllFourHighlightColours(@TempDir Path tmp) throws Exception {
        Path xlsx = tmp.resolve("sample.xlsx");
        SampleReportGenerator.write(xlsx);

        AppProperties props = new AppProperties(
                new AppProperties.Email("", "noreply@example.com", "Timesheets AI", "uday.rajpurohit@gmail.com"),
                new AppProperties.Excel(xlsx.toString(), "Weekly Timesheet", 6));

        TimesheetExcelParser parser = new TimesheetExcelParser(new CellColorClassifier(), props);
        List<DiscrepancyRow> rows = parser.parse(xlsx);

        assertTrue(rows.size() >= 8, "expected at least 8 flagged rows, got " + rows.size());

        Map<DiscrepancyType, Long> byType = rows.stream()
                .collect(Collectors.groupingBy(DiscrepancyRow::type, Collectors.counting()));

        assertTrue(byType.getOrDefault(DiscrepancyType.UNDER_REPORT, 0L) > 0, "no UNDER_REPORT detected");
        assertTrue(byType.getOrDefault(DiscrepancyType.OVER_REPORT, 0L)  > 0, "no OVER_REPORT detected");
        assertTrue(byType.getOrDefault(DiscrepancyType.NO_RECORD_TEAL, 0L) > 0, "no NO_RECORD_TEAL detected");
        assertTrue(byType.getOrDefault(DiscrepancyType.NO_RECORD_GREY, 0L) > 0, "no NO_RECORD_GREY detected");

        // Spot-check: the first under-report row in the sample is Abdul Tawab Chaudhary, week of 2-Oct.
        DiscrepancyRow firstRed = rows.stream()
                .filter(r -> r.type() == DiscrepancyType.UNDER_REPORT)
                .findFirst()
                .orElseThrow();
        assertEquals("47570", firstRed.employeeCode());
    }
}
