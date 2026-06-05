package com.busagent.controller;

import com.busagent.common.ApiResponse;
import com.busagent.service.PublicStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/statistics")
public class AdminStatsController {

    private final PublicStatsService publicStatsService;

    public AdminStatsController(PublicStatsService publicStatsService) {
        this.publicStatsService = publicStatsService;
    }

    @GetMapping("/route-flow")
    public ApiResponse<List<Map<String, Object>>> routeFlow(
            @RequestParam long routeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(publicStatsService.routeFlow(routeId, startDate, endDate));
    }

    @GetMapping("/station-flow")
    public ApiResponse<List<Map<String, Object>>> stationFlow(
            @RequestParam long routeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "10") int limit) {
        return ApiResponse.ok(publicStatsService.stationFlow(routeId, startDate, endDate, limit));
    }

    @GetMapping({"/peak-summary", "/peak-analysis"})
    public ApiResponse<List<Map<String, Object>>> peakSummary(
            @RequestParam long routeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(publicStatsService.peakAnalysis(routeId, startDate, endDate));
    }

    @GetMapping("/load-rate")
    public ApiResponse<List<Map<String, Object>>> loadRate(
            @RequestParam long routeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ApiResponse.ok(publicStatsService.loadRate(routeId, startDate, endDate));
    }
}
