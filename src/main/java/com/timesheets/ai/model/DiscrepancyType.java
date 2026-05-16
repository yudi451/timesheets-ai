package com.timesheets.ai.model;

/**
 * Categorises a highlighted cell in the Weekly Timesheet report.
 * The colour is set by the upstream timesheet-generation step; this app only reads it.
 */
public enum DiscrepancyType {
    /** RED: beeline hours < fusion hours (contractor under-billed). */
    UNDER_REPORT("Under-report (Beeline < Fusion)"),
    /** YELLOW: beeline hours > fusion hours (contractor over-billed). */
    OVER_REPORT("Over-report (Beeline > Fusion)"),
    /** TEAL / cyan NA: no matching record for the week. */
    NO_RECORD_TEAL("No record (teal NA)"),
    /** GREY NA: no matching record (older format). */
    NO_RECORD_GREY("No record (grey NA)");

    private final String label;

    DiscrepancyType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
