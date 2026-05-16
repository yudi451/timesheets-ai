package com.timesheets.ai.model;

/**
 * One highlighted row from the Weekly Timesheet sheet.
 *
 * Captures both the Fusion side (left half of the sheet) and the Beeline side
 * (right half) so downstream code can compute under/over numerically rather than
 * relying solely on the cell colour the upstream generator set.
 *
 * Date fields are stored as the raw display strings from the workbook so the email
 * reads the same as the spreadsheet, regardless of Excel's date formatting.
 */
public record DiscrepancyRow(
        int rowNumber,
        // Fusion side
        String sowNumber,
        String cio,
        String sowName,
        String employeeCode,
        String employeeName,
        String weekStart,
        String weekEnd,
        String fusionReportedHours,
        String fusionStatus,
        // Beeline side
        String type,
        String beelineContractorName,
        String au,
        String beelineWeekStart,
        String beelineWeekEnd,
        String beelineReportedHours,
        String beelineStatus,
        String emailId,
        // Classification
        DiscrepancyType discrepancyType) {
}
