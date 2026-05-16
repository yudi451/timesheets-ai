package com.timesheets.ai.sample;

import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.awt.Color;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Generates a sample Beeline_Fusion_Report .xlsx so you can run the pipeline end-to-end
 * without a real report. Mirrors the column layout from the screenshots:
 *   A SOW Number | B CIO | C SOW Name | D Employee Code | E Employee Name |
 *   F Week Start | G Week End | H Fusion Reported Hours | I Status ...
 *
 * Highlights H (Fusion Reported Hours, column index 7) with the four colours the parser detects.
 *
 * Run: ./mvnw -q exec:java -Dexec.mainClass=com.timesheets.ai.sample.SampleReportGenerator
 * Or:  java -cp target/timesheets-ai-0.0.1-SNAPSHOT.jar \
 *        com.timesheets.ai.sample.SampleReportGenerator ./sample-data/sample-beeline-fusion-report.xlsx
 */
public class SampleReportGenerator {

    // Layout matches the "no CIO column" variant (second screenshot).
    // Fusion Reported Hours sits at column index 6 — keep in sync with
    // app.excel.fusion-hours-column-index in application.yml.
    private static final String[] HEADER = {
            "SOW Number", "SOW Name", "Employee Code", "Employee Name",
            "Week Start Date", "Week End Date", "Fusion Reported Hours", "Status"
    };

    // Rows: {sow, sowName, empCode, empName, weekStart, weekEnd, hours, status, colour}
    // colour: "red" | "yellow" | "teal" | "grey" | ""
    private static final String[][] ROWS = {
            {"6421", "WFT CCIBT PTCT Bundle and Bid US - Persistent",
             "47570", "Abdul Tawab Chaudhary", "2-Oct-2023", "8-Oct-2023", "32", "APPROVED", "red"},
            {"6421", "WFT CCIBT PTCT Bundle and Bid US - Persistent",
             "47570", "Abdul Tawab Chaudhary", "9-Oct-2023", "15-Oct-2023", "40", "APPROVED", "yellow"},
            {"6421", "WFT CCIBT PTCT Bundle and Bid US - Persistent",
             "47570", "Abdul Tawab Chaudhary", "16-Oct-2023", "22-Oct-2023", "NA", "NA", "teal"},
            {"6421", "WFT CCIBT PTCT Bundle and Bid US - Persistent",
             "47570", "Abdul Tawab Chaudhary", "23-Oct-2023", "29-Oct-2023", "40", "APPROVED", ""},
            {"6421", "WFT CCIBT PTCT Bundle and Bid US - Persistent",
             "47570", "Abdul Tawab Chaudhary", "30-Oct-2023", "5-Nov-2023", "40", "APPROVED", "red"},
            {"NA",   "NA",
             "97315", "Bhaveth Vanaparthy", "2-Oct-2023", "8-Oct-2023", "NA", "NA", "teal"},
            {"NA",   "NA",
             "97315", "Bhaveth Vanaparthy", "9-Oct-2023", "15-Oct-2023", "NA", "NA", "teal"},
            {"NA",   "NA",
             "97315", "Bhaveth Vanaparthy", "16-Oct-2023", "22-Oct-2023", "NA", "NA", "grey"},
            {"6421", "WFT CCIBT PTCT Bundle and Bid Onshore - Persistent",
             "34613", "Rahil Ali", "2-Oct-2023", "8-Oct-2023", "40", "APPROVED", "yellow"},
            {"6421", "WFT CCIBT PTCT Bundle and Bid Onshore - Persistent",
             "34613", "Rahil Ali", "9-Oct-2023", "15-Oct-2023", "40", "APPROVED", ""},
            {"6421", "WFT CCIBT PTCT Bundle and Bid Onshore - Persistent",
             "34613", "Rahil Ali", "16-Oct-2023", "22-Oct-2023", "32", "APPROVED", "red"},
            {"6421", "WFT CCIBT PTCT Bundle and Bid US - Persistent",
             "61836", "Chungan Chen", "30-Oct-2023", "5-Nov-2023", "40", "APPROVED", "red"},
            {"6421", "WFT CCIBT PTCT Bundle and Bid US - Persistent",
             "61836", "Chungan Chen", "6-Nov-2023", "12-Nov-2023", "40", "APPROVED", "red"},
    };

    public static void main(String[] args) throws IOException {
        Path output = Paths.get(args.length > 0 ? args[0] : "./sample-data/sample-beeline-fusion-report.xlsx");
        if (output.getParent() != null) {
            Files.createDirectories(output.getParent());
        }
        write(output);
        System.out.println("Wrote sample report: " + output.toAbsolutePath());
    }

    public static void write(Path output) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Weekly Timesheet");

            XSSFCellStyle redStyle    = fillStyle(wb, new Color(0xFF, 0x00, 0x00));
            XSSFCellStyle yellowStyle = fillStyle(wb, new Color(0xFF, 0xFF, 0x00));
            XSSFCellStyle tealStyle   = fillStyle(wb, new Color(0x1A, 0xBC, 0x9C));
            XSSFCellStyle greyStyle   = fillStyle(wb, new Color(0xBF, 0xBF, 0xBF));

            Row header = sheet.createRow(0);
            for (int i = 0; i < HEADER.length; i++) {
                header.createCell(i).setCellValue(HEADER[i]);
            }

            for (int r = 0; r < ROWS.length; r++) {
                Row row = sheet.createRow(r + 1);
                String[] data = ROWS[r];
                for (int c = 0; c < HEADER.length; c++) {
                    row.createCell(c).setCellValue(data[c]);
                }
                // Apply highlight to the Fusion Reported Hours cell (column index 6).
                XSSFCell hoursCell = (XSSFCell) row.getCell(6);
                XSSFCellStyle style = switch (data[8]) {
                    case "red"    -> redStyle;
                    case "yellow" -> yellowStyle;
                    case "teal"   -> tealStyle;
                    case "grey"   -> greyStyle;
                    default       -> null;
                };
                if (style != null) {
                    hoursCell.setCellStyle(style);
                }
            }

            for (int c = 0; c < HEADER.length; c++) {
                sheet.autoSizeColumn(c);
            }

            try (FileOutputStream out = new FileOutputStream(output.toFile())) {
                wb.write(out);
            }
        }
    }

    private static XSSFCellStyle fillStyle(XSSFWorkbook wb, Color color) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(color, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }
}
