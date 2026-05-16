package com.timesheets.ai.excel;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.stereotype.Component;

/**
 * Reads the fill colour of an .xlsx cell and buckets it into the categories
 * the discrepancy workflow uses. Excel cells can carry colour as:
 *   - an indexed palette value
 *   - a theme colour
 *   - an explicit ARGB hex
 * Apache POI exposes the resolved ARGB via XSSFColor; we then compute nearest-bucket
 * by Euclidean distance in RGB space so light/dark variants of the same hue still match.
 */
@Component
public class CellColorClassifier {

    // Bucket centroids (R,G,B). Tuned to the colours visible in the sample reports.
    private static final int[] RED    = {0xFF, 0x00, 0x00};
    private static final int[] YELLOW = {0xFF, 0xFF, 0x00};
    private static final int[] TEAL   = {0x1A, 0xBC, 0x9C}; // the cyan-ish NA in the screenshot
    private static final int[] GREY   = {0xBF, 0xBF, 0xBF};

    // Max distance from a centroid for a cell to be classified into that bucket.
    // 110 ≈ wide enough to absorb shading variants while staying clear of one another.
    private static final double MAX_DISTANCE = 110.0;

    public CellColor classify(Cell cell) {
        if (!(cell instanceof XSSFCell xssfCell)) {
            return CellColor.NONE;
        }
        XSSFCellStyle style = xssfCell.getCellStyle();
        if (style == null) {
            return CellColor.NONE;
        }
        XSSFColor fill = style.getFillForegroundColorColor();
        if (fill == null) {
            return CellColor.NONE;
        }
        byte[] argb = fill.getARGB();
        if (argb == null || argb.length < 4) {
            return CellColor.NONE;
        }
        int alpha = argb[0] & 0xFF;
        // Treat fully transparent fills as no colour.
        if (alpha == 0) {
            return CellColor.NONE;
        }
        int r = argb[1] & 0xFF;
        int g = argb[2] & 0xFF;
        int b = argb[3] & 0xFF;

        // White / near-white is "no highlight" — skip the bucketing entirely.
        if (r > 240 && g > 240 && b > 240) {
            return CellColor.NONE;
        }

        return nearestBucket(r, g, b);
    }

    private CellColor nearestBucket(int r, int g, int b) {
        double dRed    = distance(r, g, b, RED);
        double dYellow = distance(r, g, b, YELLOW);
        double dTeal   = distance(r, g, b, TEAL);
        double dGrey   = distance(r, g, b, GREY);

        double min = Math.min(Math.min(dRed, dYellow), Math.min(dTeal, dGrey));
        if (min > MAX_DISTANCE) {
            return CellColor.NONE;
        }
        if (min == dRed)    return CellColor.RED;
        if (min == dYellow) return CellColor.YELLOW;
        if (min == dTeal)   return CellColor.TEAL;
        return CellColor.GREY;
    }

    private static double distance(int r, int g, int b, int[] target) {
        int dr = r - target[0];
        int dg = g - target[1];
        int db = b - target[2];
        return Math.sqrt(dr * dr + dg * dg + db * db);
    }
}
