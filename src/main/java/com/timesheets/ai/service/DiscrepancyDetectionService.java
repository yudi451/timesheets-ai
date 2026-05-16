package com.timesheets.ai.service;

import com.timesheets.ai.excel.TimesheetExcelParser;
import com.timesheets.ai.model.ContractorDiscrepancySummary;
import com.timesheets.ai.model.DiscrepancyRow;
import com.timesheets.ai.model.DiscrepancyType;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DiscrepancyDetectionService {

    private final TimesheetExcelParser parser;

    public DiscrepancyDetectionService(TimesheetExcelParser parser) {
        this.parser = parser;
    }

    public List<DiscrepancyRow> detect(Path xlsxPath) throws IOException {
        return parser.parse(xlsxPath);
    }

    /**
     * Group highlighted rows by contractor (employee code) and tally each discrepancy type.
     * Returns contractors ordered by total discrepancy count, descending — the worst
     * offenders surface first in any downstream report.
     */
    public List<ContractorDiscrepancySummary> groupByContractor(List<DiscrepancyRow> rows) {
        Map<String, List<DiscrepancyRow>> byCode = new LinkedHashMap<>();
        for (DiscrepancyRow row : rows) {
            byCode.computeIfAbsent(row.employeeCode(), k -> new ArrayList<>()).add(row);
        }

        List<ContractorDiscrepancySummary> summaries = new ArrayList<>();
        for (Map.Entry<String, List<DiscrepancyRow>> entry : byCode.entrySet()) {
            List<DiscrepancyRow> contractorRows = entry.getValue();
            String name = contractorRows.get(0).employeeName();

            int under = 0, over = 0, missing = 0;
            for (DiscrepancyRow r : contractorRows) {
                DiscrepancyType t = r.type();
                if (t == DiscrepancyType.UNDER_REPORT) under++;
                else if (t == DiscrepancyType.OVER_REPORT) over++;
                else missing++; // NO_RECORD_TEAL + NO_RECORD_GREY
            }

            summaries.add(new ContractorDiscrepancySummary(
                    entry.getKey(), name, under, over, missing, contractorRows));
        }

        summaries.sort(Comparator.comparingInt(ContractorDiscrepancySummary::total).reversed());
        return summaries;
    }
}
