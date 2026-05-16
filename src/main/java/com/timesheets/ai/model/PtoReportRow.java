package com.timesheets.ai.model;

/**
 * One row from the PTO Report sheet.
 *
 * Mismatch detection is driven entirely by the Comments column — any non-empty,
 * non-OK comment is treated as a mismatch the manager needs to act on. Typical
 * comments seen in production reports:
 *   - "Data not found for this resource in Beeline Timesheet Report"
 *   - "BSD does not exist for this resource in Beeline and Resource Mapping file"
 *   - "Calculated BSD <date> from Beeline. Please update BSD in Resource Mapping file"
 */
public record PtoReportRow(
        int rowNumber,
        String sowNumber,
        String sowName,
        String employeeCode,
        String employeeName,
        int ptoDays,
        String ptoDates,
        String comments,
        boolean isMismatch) {
}
