package com.timesheets.ai.excel;

import com.timesheets.ai.model.PtoReportRow;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Reads the "PTO report" sheet. Returns every row, with each marked as a mismatch
 * when its Comments cell is non-empty (the upstream generator only writes a comment
 * when something needs the manager's attention — e.g. resource not in Beeline,
 * BSD mismatch, outdated mapping file).
 *
 * Column layout (25 columns):
 *   A  SOW Number       (0)
 *   B  SOW Name         (1)
 *   C  Employee Code    (2)
 *   D  Employee Name    (3)
 *   E  PTOs (No. of Days) (4)
 *   F–W  18 weekly hour columns (5..22)
 *   X  PTO Dates        (23)
 *   Y  Comments         (24)
 */
@Component
public class PtoReportParser {

    private static final Logger log = LoggerFactory.getLogger(PtoReportParser.class);

    private static final String DEFAULT_SHEET = "PTO report";

    private final DataFormatter formatter = new DataFormatter();

    public List<PtoReportRow> parse(Path xlsxPath) throws IOException {
        if (!Files.exists(xlsxPath)) {
            throw new IOException("Excel file not found: " + xlsxPath.toAbsolutePath());
        }

        try (FileInputStream in = new FileInputStream(xlsxPath.toFile());
             Workbook workbook = new XSSFWorkbook(in)) {

            Sheet sheet = workbook.getSheet(DEFAULT_SHEET);
            if (sheet == null) {
                log.info("No '{}' sheet in {} — skipping PTO parsing.", DEFAULT_SHEET, xlsxPath.getFileName());
                return List.of();
            }

            List<PtoReportRow> rows = new ArrayList<>();
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                String empCode = readString(row, 2);
                if (empCode.isEmpty()) continue;

                String comments = readString(row, 24);
                boolean mismatch = !comments.isEmpty() && !comments.equalsIgnoreCase("NA");

                rows.add(new PtoReportRow(
                        r + 1,
                        readString(row, 0),
                        readString(row, 1),
                        empCode,
                        readString(row, 3),
                        readInt(row, 4),
                        readString(row, 23),
                        comments,
                        mismatch
                ));
            }

            long mismatches = rows.stream().filter(PtoReportRow::isMismatch).count();
            log.info("Parsed PTO report: {} rows, {} flagged with comments", rows.size(), mismatches);
            return rows;
        }
    }

    private String readString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private int readInt(Row row, int col) {
        String s = readString(row, col);
        if (s.isEmpty() || s.equalsIgnoreCase("NA")) return 0;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            try {
                return (int) Double.parseDouble(s);
            } catch (NumberFormatException e2) {
                return 0;
            }
        }
    }
}
