package com.timesheets.ai.excel;

import com.timesheets.ai.config.AppProperties;
import com.timesheets.ai.model.DiscrepancyRow;
import com.timesheets.ai.model.DiscrepancyType;
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
import java.util.Optional;

/**
 * Reads the Weekly Timesheet sheet of a Beeline_Fusion_Report workbook
 * and returns the rows whose Fusion-Hours cell is highlighted.
 *
 * Column layout (matches the sample report; no-CIO variant from the second screenshot):
 *   A  SOW Number      (0)
 *   B  SOW Name        (1)
 *   C  Employee Code   (2)
 *   D  Employee Name   (3)
 *   E  Week Start Date (4)
 *   F  Week End Date   (5)
 *   G  Fusion Reported Hours  (6)   <-- the highlighted cell, configurable
 *   H  Status                  (7)
 *
 * The exact column index of the highlighted "comparison" cell is configurable
 * (app.excel.fusion-hours-column-index) because different versions of the report
 * have shifted columns. The metadata columns (SOW Number, Employee Code, etc.)
 * are read relative to that index, so the parser stays aligned if the whole
 * layout shifts left or right by a known amount.
 */
@Component
public class TimesheetExcelParser {

    private static final Logger log = LoggerFactory.getLogger(TimesheetExcelParser.class);

    private final CellColorClassifier classifier;
    private final AppProperties props;
    private final DataFormatter formatter = new DataFormatter();

    public TimesheetExcelParser(CellColorClassifier classifier, AppProperties props) {
        this.classifier = classifier;
        this.props = props;
    }

    public List<DiscrepancyRow> parse(Path xlsxPath) throws IOException {
        if (!Files.exists(xlsxPath)) {
            throw new IOException("Excel file not found: " + xlsxPath.toAbsolutePath());
        }

        try (FileInputStream in = new FileInputStream(xlsxPath.toFile());
             Workbook workbook = new XSSFWorkbook(in)) {

            String sheetName = props.excel().sheetName();
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IOException(
                        "Sheet '" + sheetName + "' not found in " + xlsxPath.getFileName()
                                + ". Available: " + listSheetNames(workbook));
            }

            int hoursCol = props.excel().fusionHoursColumnIndex();
            // Other metadata columns are positioned relative to the hours column,
            // matching the no-CIO layout: hours-6=SOW#, hours-5=SOW Name, etc.
            int sowNumCol    = hoursCol - 6;
            int sowNameCol   = hoursCol - 5;
            int empCodeCol   = hoursCol - 4;
            int empNameCol   = hoursCol - 3;
            int weekStartCol = hoursCol - 2;
            int weekEndCol   = hoursCol - 1;
            int statusCol    = hoursCol + 1;

            List<DiscrepancyRow> rows = new ArrayList<>();

            // Skip header row (index 0).
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Cell hoursCell = row.getCell(hoursCol);
                if (hoursCell == null) continue;

                CellColor color = classifier.classify(hoursCell);
                Optional<DiscrepancyType> type = toDiscrepancyType(color);
                if (type.isEmpty()) continue;

                rows.add(new DiscrepancyRow(
                        r + 1, // 1-indexed for human display
                        readString(row, sowNumCol),
                        readString(row, sowNameCol),
                        readString(row, empCodeCol),
                        readString(row, empNameCol),
                        readString(row, weekStartCol),
                        readString(row, weekEndCol),
                        readString(row, hoursCol),
                        readString(row, statusCol),
                        type.get()
                ));
            }

            log.info("Parsed {} highlighted rows from {} ({} sheet)",
                    rows.size(), xlsxPath.getFileName(), sheetName);
            return rows;
        }
    }

    private String readString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return formatter.formatCellValue(cell).trim();
    }

    private Optional<DiscrepancyType> toDiscrepancyType(CellColor color) {
        return switch (color) {
            case RED    -> Optional.of(DiscrepancyType.UNDER_REPORT);
            case YELLOW -> Optional.of(DiscrepancyType.OVER_REPORT);
            case TEAL   -> Optional.of(DiscrepancyType.NO_RECORD_TEAL);
            case GREY   -> Optional.of(DiscrepancyType.NO_RECORD_GREY);
            case NONE   -> Optional.empty();
        };
    }

    private String listSheetNames(Workbook workbook) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(workbook.getSheetName(i));
        }
        return sb.append("]").toString();
    }
}
