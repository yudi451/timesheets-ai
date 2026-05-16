package com.timesheets.ai.web;

import com.timesheets.ai.dashboard.DashboardDtos.DashboardSummary;
import com.timesheets.ai.dashboard.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService service;

    public DashboardController(DashboardService service) {
        this.service = service;
    }

    /**
     * Single endpoint the React app calls on load. Returns everything the dashboard needs:
     * KPIs, pie-chart breakdown, weekly trend line, contractor grid, AI insights, report tabs.
     *
     * Query param 'path' is optional — if omitted, uses app.excel.default-path.
     */
    @GetMapping("/summary")
    public DashboardSummary summary(@RequestParam(name = "path", required = false) String path)
            throws IOException {
        return service.buildSummary(path);
    }
}
