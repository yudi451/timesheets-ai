package com.timesheets.ai.model;

import java.util.List;

public record ContractorDiscrepancySummary(
        String employeeCode,
        String employeeName,
        int underReportCount,
        int overReportCount,
        int noRecordCount,
        List<DiscrepancyRow> rows) {

    public int total() {
        return rows.size();
    }
}
