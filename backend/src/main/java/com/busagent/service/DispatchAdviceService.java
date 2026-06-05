package com.busagent.service;

import cn.dev33.satoken.stp.StpUtil;
import com.busagent.common.BusinessException;
import com.busagent.common.ErrorCode;
import com.busagent.common.PageRequest;
import com.busagent.common.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DispatchAdviceService {

    private final JdbcTemplate jdbcTemplate;
    private final PublicStatsService publicStatsService;
    private final AiClientService aiClientService;

    public DispatchAdviceService(JdbcTemplate jdbcTemplate,
                                 PublicStatsService publicStatsService,
                                 AiClientService aiClientService) {
        this.jdbcTemplate = jdbcTemplate;
        this.publicStatsService = publicStatsService;
        this.aiClientService = aiClientService;
    }

    @Transactional
    public Map<String, Object> generate(GenerateRequest request) {
        validateRequest(request);
        Map<String, Object> route = publicStatsService.routeInfo(request.routeId());
        Map<String, Object> metrics = metrics(request);
        String adviceLevel = adviceLevel(request.periodType(), toDouble(metrics.get("avgLoadRate")));
        String ruleAdvice = ruleAdvice(request.periodType(), toDouble(metrics.get("avgLoadRate")),
                toLong(metrics.get("passengerCount")), toLong(metrics.get("scheduleCount")));

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("route", route);
        payload.put("startDate", request.startDate().toString());
        payload.put("endDate", request.endDate().toString());
        payload.put("periodType", request.periodType());
        payload.put("metrics", metrics);
        payload.put("stationFlow", publicStatsService.stationFlow(request.routeId(), request.startDate(), request.endDate(), 10));
        payload.put("adviceLevel", adviceLevel);
        payload.put("ruleAdvice", ruleAdvice);

        Map<String, Object> aiResult = aiClientService.dispatchAdvice(payload);
        String aiSummary = String.valueOf(aiResult.getOrDefault("reason", ""));
        String adviceContent = String.valueOf(aiResult.getOrDefault("advice", ruleAdvice));

        jdbcTemplate.update("""
                INSERT INTO dispatch_advice
                  (route_id, start_date, end_date, period_type, avg_load_rate, passenger_count,
                   advice_level, advice_content, ai_summary)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """,
                request.routeId(),
                request.startDate(),
                request.endDate(),
                request.periodType(),
                metrics.get("avgLoadRate"),
                metrics.get("passengerCount"),
                adviceLevel,
                adviceContent,
                aiSummary);
        Long id = jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
        Long adminId = StpUtil.isLogin() ? StpUtil.getLoginIdAsLong() : null;
        jdbcTemplate.update("""
                INSERT INTO operation_log (admin_id, operation_type, operation_content, request_path)
                VALUES (?, 'generate_dispatch_advice', ?, '/api/admin/dispatch-advice/generate')
                """, adminId, "生成线路 " + route.get("routeName") + " 调度建议");

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", id);
        result.put("route", route);
        result.put("metrics", metrics);
        result.put("adviceLevel", adviceLevel);
        result.put("ruleAdvice", ruleAdvice);
        result.put("adviceContent", adviceContent);
        result.put("aiSummary", aiSummary);
        result.put("aiResult", aiResult);
        return result;
    }

    public PageResult<Map<String, Object>> list(PageRequest pageRequest) {
        long total = count();
        List<Map<String, Object>> records = jdbcTemplate.queryForList("""
                SELECT da.*, br.route_code AS routeCode, br.route_name AS routeName
                FROM dispatch_advice da
                JOIN bus_route br ON br.id = da.route_id
                ORDER BY da.create_time DESC
                LIMIT ? OFFSET ?
                """, pageRequest.getSize(), pageRequest.offset());
        return PageResult.of(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    private Map<String, Object> metrics(GenerateRequest request) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT
                  COUNT(*) AS scheduleCount,
                  COALESCE(SUM(passengerCount), 0) AS passengerCount,
                  ROUND(COALESCE(AVG(loadRate), 0), 2) AS avgLoadRate,
                  ROUND(COALESCE(MAX(loadRate), 0), 2) AS maxLoadRate
                FROM (
                  SELECT bs.id, COUNT(rr.id) AS passengerCount, COUNT(rr.id) / bv.capacity * 100 AS loadRate
                  FROM bus_schedule bs
                  JOIN bus_vehicle bv ON bv.id = bs.vehicle_id
                  LEFT JOIN ride_record rr ON rr.schedule_id = bs.id
                  WHERE bs.route_id = ?
                    AND bs.schedule_date BETWEEN ? AND ?
                    AND bs.period_type = ?
                  GROUP BY bs.id, bv.capacity
                ) schedule_load
                """, request.routeId(), request.startDate(), request.endDate(), request.periodType());
        Map<String, Object> row = rows.isEmpty() ? Map.of() : rows.get(0);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("scheduleCount", row.getOrDefault("scheduleCount", 0));
        metrics.put("passengerCount", row.getOrDefault("passengerCount", 0));
        metrics.put("avgLoadRate", row.getOrDefault("avgLoadRate", BigDecimal.ZERO));
        metrics.put("maxLoadRate", row.getOrDefault("maxLoadRate", BigDecimal.ZERO));
        return metrics;
    }

    private void validateRequest(GenerateRequest request) {
        if (request.routeId() <= 0 || request.startDate() == null || request.endDate() == null
                || request.endDate().isBefore(request.startDate())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "调度建议参数无效");
        }
        if (!List.of("morning_peak", "evening_peak", "normal").contains(request.periodType())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "时段类型无效");
        }
    }

    private String adviceLevel(String periodType, double avgLoadRate) {
        if (!"normal".equals(periodType) && avgLoadRate >= 85) {
            return "high";
        }
        if (!"normal".equals(periodType) && avgLoadRate >= 60) {
            return "medium";
        }
        if ("normal".equals(periodType) && avgLoadRate < 30) {
            return "medium";
        }
        return "low";
    }

    private String ruleAdvice(String periodType, double avgLoadRate, long passengerCount, long scheduleCount) {
        String periodName = switch (periodType) {
            case "morning_peak" -> "早高峰";
            case "evening_peak" -> "晚高峰";
            default -> "正常时段";
        };
        if (!"normal".equals(periodType) && avgLoadRate >= 85) {
            return periodName + "平均满载率较高，建议增加班次或缩短发车间隔。";
        }
        if (!"normal".equals(periodType) && avgLoadRate >= 60) {
            return periodName + "客流处于可控偏高水平，建议维持班次并关注重点站点。";
        }
        if ("normal".equals(periodType) && avgLoadRate < 30) {
            return "正常时段车辆利用率偏低，建议适当延长发车间隔或减少班次。";
        }
        return periodName + "客流相对平稳，当前 " + scheduleCount + " 个班次承载 "
                + passengerCount + " 人次，建议维持当前调度。";
    }

    private long count() {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM dispatch_advice", Long.class);
        return value == null ? 0 : value;
    }

    private long toLong(Object value) {
        return value instanceof Number number ? number.longValue() : 0;
    }

    private double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0;
    }

    public record GenerateRequest(
            long routeId,
            LocalDate startDate,
            LocalDate endDate,
            String periodType
    ) {
    }
}
