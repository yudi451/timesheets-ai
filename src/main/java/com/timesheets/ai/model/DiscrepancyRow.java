package com.timesheets.ai.model;

/**
 * One highlighted row from the Weekly Timesheet sheet.
 * weekStart / weekEnd are stored as the raw display strings from the workbook so
 * the email reads the same as the spreadsheet, regardless of Excel's date formatting.
 */
public record DiscrepancyRow(
        int rowNumber,
        String sowNumber,
        String sowName,
        String employeeCode,
        String employeeName,
        String weekStart,
        String weekEnd,
        String fusionReportedHours,
        String status,
        DiscrepancyType type) {
}
