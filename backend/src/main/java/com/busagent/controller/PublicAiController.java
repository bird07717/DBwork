package com.busagent.controller;

import com.busagent.common.ApiResponse;
import com.busagent.service.AiClientService;
import com.busagent.service.PublicStatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/public/ai")
public class PublicAiController {

    private final PublicStatsService publicStatsService;
    private final AiClientService aiClientService;

    public PublicAiController(PublicStatsService publicStatsService, AiClientService aiClientService) {
        this.publicStatsService = publicStatsService;
        this.aiClientService = aiClientService;
    }

    @PostMapping("/analyze")
    public ApiResponse<Map<String, Object>> analyze(@RequestBody AnalyzeRequest request) {
        LocalDate startDate = request.startDate() == null ? LocalDate.now().minusDays(6) : request.startDate();
        LocalDate endDate = request.endDate() == null ? LocalDate.now() : request.endDate();

        Map<String, Object> route = request.routeId() <= 0
                ? Map.of("routeId", 0, "routeName", "全部线路")
                : publicStatsService.routeInfo(request.routeId());
        Map<String, Object> statistics = new LinkedHashMap<>();
        if (request.routeId() <= 0) {
            statistics.put("routeRanking", publicStatsService.allRouteSummary(startDate, endDate));
        } else {
            statistics.put("routeFlow", publicStatsService.routeFlow(request.routeId(), startDate, endDate));
            statistics.put("stationFlow", publicStatsService.stationFlow(request.routeId(), startDate, endDate, 10));
            statistics.put("peakAnalysis", publicStatsService.peakAnalysis(request.routeId(), startDate, endDate));
            statistics.put("loadRate", publicStatsService.loadRate(request.routeId(), startDate, endDate));
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("question", request.question());
        payload.put("route", route);
        payload.put("startDate", startDate.toString());
        payload.put("endDate", endDate.toString());
        payload.put("statistics", statistics);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("statistics", statistics);
        data.put("analysis", aiClientService.analyzeFlow(payload));
        return ApiResponse.ok(data);
    }

    public record AnalyzeRequest(
            String question,
            long routeId,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
    }
}
