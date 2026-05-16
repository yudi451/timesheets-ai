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
 * Generates a realistic-looking Beeline_Fusion_Report .xlsx with TWO sheets:
 *   - "Weekly Timesheet" — 17 columns, both Fusion and Beeline sides per row
 *   - "PTO report"       — one row per resource with weekly hours + comments
 *
 * The data mirrors the production layout the project actually parses, so the
 * dashboard renders the same shape it would on a real report.
 *
 * Run: java -cp target/classes:DEPS com.timesheets.ai.sample.SampleReportGenerator [out.xlsx]
 */
public class SampleReportGenerator {

    // ------------------- Weekly Timesheet sheet -------------------

    private static final String[] WEEKLY_HEADER = {
            "SOW Number", "CIO", "SOW Name", "Employee Code", "Employee Name",
            "Week Start Date", "Week End Date", "Fusion Reported Hours", "Status",
            "Type", "Beeline Contractor Name", "AU",
            "Week Start Date", "Week End Date", "Beeline Reported Hours", "Status",
            "Email ID"
    };

    /**
     * Rows for the Weekly Timesheet sheet. Each row has 17 cells + a "colour" code
     * for the Fusion Reported Hours cell (column index 7). "" means no highlight.
     */
    private static final String[][] WEEKLY_ROWS = {
            // Bhaveth Vanaparthy — no Beeline records → teal NA across the board
            row("NA", "Elliott, Charles", "NA", "97315", "Bhaveth Vanaparthy",
                "2-Oct-2023", "8-Oct-2023", "NA", "NA",
                "Contractor", "Vanaparthy, Bhaveth", "NA",
                "10/2/2023", "10/8/2023", "NA", "NA",
                "bhaveth.v@example.com", "teal"),
            row("NA", "Elliott, Charles", "NA", "97315", "Bhaveth Vanaparthy",
                "9-Oct-2023", "15-Oct-2023", "NA", "NA",
                "Contractor", "Vanaparthy, Bhaveth", "NA",
                "10/9/2023", "10/15/2023", "NA", "NA",
                "bhaveth.v@example.com", "teal"),
            row("NA", "Elliott, Charles", "NA", "97315", "Bhaveth Vanaparthy",
                "16-Oct-2023", "22-Oct-2023", "NA", "NA",
                "Contractor", "Vanaparthy, Bhaveth", "NA",
                "10/16/2023", "10/22/2023", "NA", "NA",
                "bhaveth.v@example.com", "grey"),

            // Rahil Ali — Fusion NA but Beeline shows 0 hours (timesheet not yet entered)
            row("6421", "MUKARTIHAL, MAHANTESH", "WFT CCIBT PTCT Bundle and Bid Onshore - Persistent, SR509322427",
                "34613", "Rahil Ali", "2-Oct-2023", "8-Oct-2023", "NA", "NA",
                "FTE - 5.2", "Ali, Rahil", "0245583",
                "10/2/2023", "10/8/2023", "0", "Locked",
                "rahil.ali@example.com", "teal"),
            row("6421", "MUKARTIHAL, MAHANTESH", "WFT CCIBT PTCT Bundle and Bid Onshore - Persistent, SR509322427",
                "34613", "Rahil Ali", "9-Oct-2023", "15-Oct-2023", "NA", "NA",
                "FTE - 5.2", "Ali, Rahil", "0245583",
                "10/9/2023", "10/15/2023", "0", "Locked",
                "rahil.ali@example.com", "teal"),

            // Abdul Tawab Chaudhary — over and under report scenarios
            row("6421", "MUKARTIHAL, MAHANTESH", "WFT CCIBT PTCT Bundle and Bid US - Persistent, SR509322427",
                "47570", "Abdul Tawab Chaudhary", "2-Oct-2023", "8-Oct-2023", "32", "APPROVED",
                "Contractor", "Chaudhary, Abdul Tawab", "0233166",
                "10/2/2023", "10/8/2023", "40", "Approved",
                "abdul.c@example.com", "yellow"),
            row("6421", "MUKARTIHAL, MAHANTESH", "WFT CCIBT PTCT Bundle and Bid US - Persistent, SR509322427",
                "47570", "Abdul Tawab Chaudhary", "9-Oct-2023", "15-Oct-2023", "40", "APPROVED",
                "Contractor", "Chaudhary, Abdul Tawab", "0233166",
                "10/9/2023", "10/15/2023", "32", "Approved",
                "abdul.c@example.com", "red"),
            row("6421", "MUKARTIHAL, MAHANTESH", "WFT CCIBT PTCT Bundle and Bid US - Persistent, SR509322427",
                "47570", "Abdul Tawab Chaudhary", "16-Oct-2023", "22-Oct-2023", "NA", "NA",
                "Contractor", "Chaudhary, Abdul Tawab", "0233166",
                "10/16/2023", "10/22/2023", "NA", "NA",
                "abdul.c@example.com", "teal"),
            row("6421", "MUKARTIHAL, MAHANTESH", "WFT CCIBT PTCT Bundle and Bid US - Persistent, SR509322427",
                "47570", "Abdul Tawab Chaudhary", "23-Oct-2023", "29-Oct-2023", "40", "APPROVED",
                "Contractor", "Chaudhary, Abdul Tawab", "0233166",
                "10/23/2023", "10/29/2023", "40", "Approved",
                "abdul.c@example.com", ""),

            // Chungan Chen — consistent under-report
            row("6421", "MUKARTIHAL, MAHANTESH", "WFT CCIBT PTCT Bundle and Bid US - Persistent, SR509322427",
                "61836", "Chungan Chen", "30-Oct-2023", "5-Nov-2023", "40", "APPROVED",
                "Contractor", "Chen, Chungan", "0231701",
                "10/30/2023", "11/5/2023", "32", "Approved",
                "chungan.c@example.com", "red"),
            row("6421", "MUKARTIHAL, MAHANTESH", "WFT CCIBT PTCT Bundle and Bid US - Persistent, SR509322427",
                "61836", "Chungan Chen", "6-Nov-2023", "12-Nov-2023", "40", "APPROVED",
                "Contractor", "Chen, Chungan", "0231701",
                "11/6/2023", "11/12/2023", "32", "Approved",
                "chungan.c@example.com", "red"),

            // Mayank Vanza — over report
            row("6421", "MUKARTIHAL, MAHANTESH", "WFT CCIBT PTCT Bundle and Bid US Extension - Persistent",
                "39524", "Mayank Vanza", "2-Oct-2023", "8-Oct-2023", "32", "APPROVED",
                "Contractor", "Vanza, Mayank", "0241108",
                "10/2/2023", "10/8/2023", "40", "Approved",
                "mayank.v@example.com", "yellow"),
    };

    // ------------------- PTO Report sheet -------------------

    private static final String[] PTO_HEADER = {
            "SOW Number", "SOW Name", "Employee Code", "Employee Name",
            "PTOs (No. of Days)",
            "2-Oct-2023 - 8-Oct-2023 Hours",
            "9-Oct-2023 - 15-Oct-2023 Hours",
            "16-Oct-2023 - 22-Oct-2023 Hours",
            "23-Oct-2023 - 29-Oct-2023 Hours",
            "30-Oct-2023 - 5-Nov-2023 Hours",
            "6-Nov-2023 - 12-Nov-2023 Hours",
            "13-Nov-2023 - 19-Nov-2023 Hours",
            "20-Nov-2023 - 26-Nov-2023 Hours",
            "27-Nov-2023 - 3-Dec-2023 Hours",
            "4-Dec-2023 - 10-Dec-2023 Hours",
            "11-Dec-2023 - 17-Dec-2023 Hours",
            "18-Dec-2023 - 24-Dec-2023 Hours",
            "25-Dec-2023 - 31-Dec-2023 Hours",
            "1-Jan-2024 - 7-Jan-2024 Hours",
            "8-Jan-2024 - 14-Jan-2024 Hours",
            "15-Jan-2024 - 21-Jan-2024 Hours",
            "22-Jan-2024 - 28-Jan-2024 Hours",
            "29-Jan-2024 - 4-Feb-2024 Hours",
            "PTO Dates", "Comments"
    };

    /**
     * PTO Report rows. 25 columns total: 5 metadata + 18 weekly hour columns + 2 trailing.
     * Empty Comments means no mismatch; any non-empty comment is a mismatch the dashboard counts.
     */
    private static final String[][] PTO_ROWS = {
            ptoRow("6421", "WFT CCIBT PTCT Bundle and Bid US Extension - Persistent",
                   "47570", "Chaudhary, Abdul Tawab", "13",
                   new String[]{"NA","16","40","40","40","40","40","32","40","40","40","0","0","32","NA","NA","NA","NA"},
                   "10/11/2023, 11/23/2023, 12/18/2023, 12/19/2023, 12/20/2023",
                   ""),
            ptoRow("6421", "WFT CCIBT PTCT Bundle and Bid US Extension - Persistent",
                   "61836", "Chen, Chungan", "78",
                   new String[]{"0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","NA"},
                   "10/2/2023 .. 1/19/2024 (extended leave)",
                   ""),
            ptoRow("6421", "WFT CCIBT PTCT Bundle and Bid US Extension - Persistent",
                   "39527", "Dendukuri, Abhishek Varma", "74",
                   new String[]{"NA","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","NA","NA"},
                   "10/10/2023 .. 1/19/2024",
                   ""),
            ptoRow("6421", "WFT CCIBT PTCT Bundle and Bid US Extension - Persistent",
                   "39519", "Sohel, Abrar", "0",
                   new String[]{"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","32","40","36","45","NA"},
                   "",
                   ""),
            ptoRow("NA", "NA",
                   "36983", "Nguyen, Eddie", "0",
                   new String[]{"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA"},
                   "NA",
                   "Data not found for this resource in Beeline Timesheet Report"),
            ptoRow("6421", "WFT CCIBT PTCT Bundle and Bid US Extension - Persistent",
                   "43140", "Gunturu, Jahnavi Swetha", "85",
                   new String[]{"0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0"},
                   "10/2/2023 .. 1/26/2024",
                   "BSD does not exist for this resource in Beeline and Resource Mapping file"),
            ptoRow("6421", "WFT CCIBT PTCT Bundle and Bid US Extension - Persistent",
                   "60555", "Ma, Kaiyu", "80",
                   new String[]{"0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","0","NA"},
                   "10/2/2023 .. 1/19/2024",
                   "Calculated BSD 2/1/2023 from Beeline. Please update BSD in Resource Mapping file"),
            ptoRow("6421", "WFT CCIBT PTCT Bundle and Bid US Extension - Persistent",
                   "38290", "Pendem, Madhavi", "8",
                   new String[]{"NA","NA","NA","NA","NA","NA","NA","16","40","40","40","40","0","32","40","32","NA","NA"},
                   "11/23/2023, 12/25/2023, 1/1/2024, 1/15/2024",
                   "Calculated BSD 11/22/2023 from Beeline. Please update BSD in Resource Mapping file"),
            ptoRow("6421", "WFT CCIBT PTCT Bundle and Bid US Extension - Persistent",
                   "53535", "Jangidi, Maruthi", "0",
                   new String[]{"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","8","40","32","40","NA"},
                   "",
                   "Calculated BSD 1/5/2024 from Beeline. Please update BSD in Resource Mapping file"),
            ptoRow("6421", "WFT CCIBT PTCT Bundle and Bid US Extension - Persistent",
                   "38301", "Li, Minkun", "9",
                   new String[]{"40","32","40","40","40","40","40","32","40","40","40","40","0","32","40","32","NA","NA"},
                   "10/9/2023, 11/23/2023, 12/25/2023 .. 1/15/2024",
                   "Calculated BSD 10/3/2022 from Beeline. Please update BSD in Resource Mapping file"),
            ptoRow("6421", "WFT CCIBT PTCT Bundle and Bid US Extension - Persistent",
                   "39524", "Vanza, Mayank", "7",
                   new String[]{"40","32","40","40","40","40","40","32","40","40","40","40","0","32","40","32","NA","NA"},
                   "11/23/2023, 12/25/2023 .. 1/1/2024",
                   "Calculated BSD 4/4/2022 from Beeline. Please update BSD in Resource Mapping file"),
            ptoRow("NA", "NA",
                   "60857", "Singh, Amitpal", "0",
                   new String[]{"NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA","NA"},
                   "NA",
                   "Data not found for this resource in Beeline Timesheet Report"),
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
            writeWeeklyTimesheet(wb);
            writePtoReport(wb);
            try (FileOutputStream out = new FileOutputStream(output.toFile())) {
                wb.write(out);
            }
        }
    }

    private static void writeWeeklyTimesheet(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet("Weekly Timesheet");

        XSSFCellStyle redStyle    = fillStyle(wb, new Color(0xFF, 0x00, 0x00));
        XSSFCellStyle yellowStyle = fillStyle(wb, new Color(0xFF, 0xFF, 0x00));
        XSSFCellStyle tealStyle   = fillStyle(wb, new Color(0x1A, 0xBC, 0x9C));
        XSSFCellStyle greyStyle   = fillStyle(wb, new Color(0xBF, 0xBF, 0xBF));

        Row header = sheet.createRow(0);
        for (int i = 0; i < WEEKLY_HEADER.length; i++) {
            header.createCell(i).setCellValue(WEEKLY_HEADER[i]);
        }

        for (int r = 0; r < WEEKLY_ROWS.length; r++) {
            Row row = sheet.createRow(r + 1);
            String[] data = WEEKLY_ROWS[r];
            // last element of data is the colour code; cells 0..16 are real columns
            for (int c = 0; c < WEEKLY_HEADER.length; c++) {
                row.createCell(c).setCellValue(data[c]);
            }
            String colour = data[WEEKLY_HEADER.length]; // index 17
            XSSFCell hoursCell = (XSSFCell) row.getCell(7); // Fusion Reported Hours
            XSSFCellStyle style = switch (colour) {
                case "red"    -> redStyle;
                case "yellow" -> yellowStyle;
                case "teal"   -> tealStyle;
                case "grey"   -> greyStyle;
                default       -> null;
            };
            if (style != null) hoursCell.setCellStyle(style);
        }

        for (int c = 0; c < WEEKLY_HEADER.length; c++) sheet.autoSizeColumn(c);
    }

    private static void writePtoReport(XSSFWorkbook wb) {
        Sheet sheet = wb.createSheet("PTO report");

        Row header = sheet.createRow(0);
        for (int i = 0; i < PTO_HEADER.length; i++) {
            header.createCell(i).setCellValue(PTO_HEADER[i]);
        }
        for (int r = 0; r < PTO_ROWS.length; r++) {
            Row row = sheet.createRow(r + 1);
            String[] data = PTO_ROWS[r];
            for (int c = 0; c < PTO_HEADER.length; c++) {
                row.createCell(c).setCellValue(data[c]);
            }
        }
        for (int c = 0; c < PTO_HEADER.length; c++) sheet.autoSizeColumn(c);
    }

    // ---- helpers ---------------------------------------------------------------------

    private static XSSFCellStyle fillStyle(XSSFWorkbook wb, Color color) {
        XSSFCellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(color, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private static String[] row(String sowNum, String cio, String sowName,
                                String empCode, String empName,
                                String fWeekStart, String fWeekEnd, String fHours, String fStatus,
                                String type, String beelineName, String au,
                                String bWeekStart, String bWeekEnd, String bHours, String bStatus,
                                String email, String colour) {
        return new String[]{sowNum, cio, sowName, empCode, empName,
                            fWeekStart, fWeekEnd, fHours, fStatus,
                            type, beelineName, au,
                            bWeekStart, bWeekEnd, bHours, bStatus,
                            email, colour};
    }

    private static String[] ptoRow(String sowNum, String sowName,
                                   String empCode, String empName, String ptoDays,
                                   String[] weeklyHours, String ptoDates, String comments) {
        String[] out = new String[PTO_HEADER.length];
        out[0] = sowNum; out[1] = sowName; out[2] = empCode; out[3] = empName; out[4] = ptoDays;
        for (int i = 0; i < 18; i++) out[5 + i] = weeklyHours[i];
        out[23] = ptoDates; out[24] = comments;
        return out;
    }
}
