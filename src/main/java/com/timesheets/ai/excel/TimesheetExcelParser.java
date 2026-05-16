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
 * Reads the Weekly Timesheet sheet and returns the rows whose Fusion-Hours cell is highlighted.
 *
 * Column layout (with CIO column — production layout, 17 columns):
 *   A  SOW Number                (0)
 *   B  CIO                        (1)
 *   C  SOW Name                   (2)
 *   D  Employee Code              (3)
 *   E  Employee Name              (4)
 *   F  Week Start Date (Fusion)   (5)
 *   G  Week End Date   (Fusion)   (6)
 *   H  Fusion Reported Hours      (7)   ← highlighted cell; configurable index
 *   I  Status (Fusion)            (8)
 *   J  Type                       (9)
 *   K  Beeline Contractor Name    (10)
 *   L  AU                         (11)
 *   M  Week Start Date (Beeline)  (12)
 *   N  Week End Date   (Beeline)  (13)
 *   O  Beeline Reported Hours     (14)
 *   P  Status (Beeline)           (15)
 *   Q  Email ID                   (16)
 *
 * All metadata columns are read relative to the configured fusion-hours index, so the
 * parser stays aligned if the whole layout shifts left/right.
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

            int fusionHoursCol = props.excel().fusionHoursColumnIndex();
            // Offsets from fusionHoursCol — match the 17-col layout above.
            int sowNumCol      = fusionHoursCol - 7;
            int cioCol         = fusionHoursCol - 6;
            int sowNameCol     = fusionHoursCol - 5;
            int empCodeCol     = fusionHoursCol - 4;
            int empNameCol     = fusionHoursCol - 3;
            int weekStartCol   = fusionHoursCol - 2;
            int weekEndCol     = fusionHoursCol - 1;
            int fusionStatusCol     = fusionHoursCol + 1;
            int typeCol             = fusionHoursCol + 2;
            int beelineNameCol      = fusionHoursCol + 3;
            int auCol               = fusionHoursCol + 4;
            int beelineWeekStartCol = fusionHoursCol + 5;
            int beelineWeekEndCol   = fusionHoursCol + 6;
            int beelineHoursCol     = fusionHoursCol + 7;
            int beelineStatusCol    = fusionHoursCol + 8;
            int emailCol            = fusionHoursCol + 9;

            List<DiscrepancyRow> rows = new ArrayList<>();

            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null) continue;

                Cell hoursCell = row.getCell(fusionHoursCol);
                if (hoursCell == null) continue;

                CellColor color = classifier.classify(hoursCell);
                Optional<DiscrepancyType> type = toDiscrepancyType(color);
                if (type.isEmpty()) continue;

                rows.add(new DiscrepancyRow(
                        r + 1,
                        readString(row, sowNumCol),
                        readString(row, cioCol),
                        readString(row, sowNameCol),
                        readString(row, empCodeCol),
                        readString(row, empNameCol),
                        readString(row, weekStartCol),
                        readString(row, weekEndCol),
                        readString(row, fusionHoursCol),
                        readString(row, fusionStatusCol),
                        readString(row, typeCol),
                        readString(row, beelineNameCol),
                        readString(row, auCol),
                        readString(row, beelineWeekStartCol),
                        readString(row, beelineWeekEndCol),
                        readString(row, beelineHoursCol),
                        readString(row, beelineStatusCol),
                        readString(row, emailCol),
                        type.get()
                ));
            }

            log.info("Parsed {} highlighted rows from {} ({} sheet)",
                    rows.size(), xlsxPath.getFileName(), sheetName);
            return rows;
        }
    }

    private String readString(Row row, int col) {
        if (col < 0) return "";
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
