package com.busagent.service;

import com.busagent.common.BusinessException;
import com.busagent.common.ErrorCode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
public class PublicStatsService {

    private final JdbcTemplate jdbcTemplate;

    public PublicStatsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> routeFlow(long routeId, LocalDate startDate, LocalDate endDate) {
        validateRange(routeId, startDate, endDate);
        return jdbcTemplate.queryForList("""
                SELECT
                  br.id AS routeId,
                  br.route_code AS routeCode,
                  br.route_name AS routeName,
                  DATE(rr.ride_time) AS statDate,
                  COUNT(rr.id) AS passengerCount
                FROM bus_route br
                LEFT JOIN ride_record rr
                  ON rr.route_id = br.id
                  AND rr.ride_time >= ?
                  AND rr.ride_time < DATE_ADD(?, INTERVAL 1 DAY)
                WHERE br.id = ?
                GROUP BY br.id, br.route_code, br.route_name, DATE(rr.ride_time)
                HAVING statDate IS NOT NULL
                ORDER BY statDate
                """, startDate, endDate, routeId);
    }

    public List<Map<String, Object>> stationFlow(long routeId, LocalDate startDate, LocalDate endDate, int limit) {
        validateRange(routeId, startDate, endDate);
        int safeLimit = Math.max(1, Math.min(limit, 50));
        return jdbcTemplate.queryForList("""
                SELECT
                  br.id AS routeId,
                  br.route_code AS routeCode,
                  br.route_name AS routeName,
                  rs.station_order AS stationOrder,
                  s.id AS stationId,
                  s.station_name AS stationName,
                  s.longitude,
                  s.latitude,
                  COUNT(rr.id) AS boardingCount
                FROM bus_route br
                JOIN route_station rs ON rs.route_id = br.id
                JOIN station s ON s.id = rs.station_id
                LEFT JOIN ride_record rr
                  ON rr.route_id = br.id
                  AND rr.boarding_station_id = s.id
                  AND rr.ride_time >= ?
                  AND rr.ride_time < DATE_ADD(?, INTERVAL 1 DAY)
                WHERE br.id = ?
                GROUP BY br.id, br.route_code, br.route_name, rs.station_order, s.id, s.station_name, s.longitude, s.latitude
                ORDER BY boardingCount DESC, rs.station_order ASC
                LIMIT ?
                """, startDate, endDate, routeId, safeLimit);
    }

    public List<Map<String, Object>> peakAnalysis(long routeId, LocalDate startDate, LocalDate endDate) {
        validateRange(routeId, startDate, endDate);
        return jdbcTemplate.queryForList("""
                SELECT
                  br.id AS routeId,
                  br.route_code AS routeCode,
                  br.route_name AS routeName,
                  schedule_load.periodType,
                  CASE schedule_load.periodType
                    WHEN 'morning_peak' THEN '早高峰'
                    WHEN 'evening_peak' THEN '晚高峰'
                    WHEN 'normal' THEN '正常时段'
                    ELSE '其他'
                  END AS periodName,
                  SUM(schedule_load.passengerCount) AS passengerCount,
                  COUNT(schedule_load.scheduleId) AS scheduleCount,
                  ROUND(SUM(schedule_load.passengerCount) / COUNT(schedule_load.scheduleId), 2) AS avgPassengerPerSchedule,
                  ROUND(AVG(schedule_load.passengerCount / schedule_load.capacity * 100), 2) AS avgLoadRate
                FROM bus_route br
                JOIN (
                  SELECT
                    bs.id AS scheduleId,
                    bs.route_id AS routeId,
                    bs.period_type AS periodType,
                    bv.capacity,
                    COUNT(rr.id) AS passengerCount
                  FROM bus_schedule bs
                  JOIN bus_vehicle bv ON bv.id = bs.vehicle_id
                  LEFT JOIN ride_record rr ON rr.schedule_id = bs.id
                  WHERE bs.route_id = ?
                    AND bs.schedule_date BETWEEN ? AND ?
                  GROUP BY bs.id, bs.route_id, bs.period_type, bv.capacity
                ) schedule_load ON schedule_load.routeId = br.id
                WHERE br.id = ?
                GROUP BY br.id, br.route_code, br.route_name, schedule_load.periodType
                ORDER BY FIELD(schedule_load.periodType, 'morning_peak', 'evening_peak', 'normal')
                """, routeId, startDate, endDate, routeId);
    }

    public List<Map<String, Object>> loadRate(long routeId, LocalDate startDate, LocalDate endDate) {
        validateRange(routeId, startDate, endDate);
        return jdbcTemplate.queryForList("""
                SELECT
                  bs.id AS scheduleId,
                  br.id AS routeId,
                  br.route_code AS routeCode,
                  br.route_name AS routeName,
                  bs.schedule_date AS scheduleDate,
                  TIME(bs.depart_time) AS departTime,
                  bs.period_type AS periodType,
                  bv.vehicle_code AS vehicleCode,
                  bv.plate_no AS plateNo,
                  bv.capacity,
                  COUNT(rr.id) AS passengerCount,
                  ROUND(COUNT(rr.id) / bv.capacity * 100, 2) AS loadRate
                FROM bus_schedule bs
                JOIN bus_route br ON br.id = bs.route_id
                JOIN bus_vehicle bv ON bv.id = bs.vehicle_id
                LEFT JOIN ride_record rr ON rr.schedule_id = bs.id
                WHERE br.id = ?
                  AND bs.schedule_date BETWEEN ? AND ?
                GROUP BY bs.id, br.id, br.route_code, br.route_name, bs.schedule_date,
                         bs.depart_time, bs.period_type, bv.vehicle_code, bv.plate_no, bv.capacity
                ORDER BY bs.schedule_date, bs.depart_time
                """, routeId, startDate, endDate);
    }

    public Map<String, Object> routeInfo(long routeId) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id AS routeId, route_code AS routeCode, route_name AS routeName,
                       start_station_name AS startStationName, end_station_name AS endStationName
                FROM bus_route
                WHERE id = ?
                """, routeId);
        if (rows.isEmpty()) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "线路不存在");
        }
        return rows.get(0);
    }

    public List<Map<String, Object>> allRouteSummary(LocalDate startDate, LocalDate endDate) {
        validateRange(1, startDate, endDate);
        return jdbcTemplate.queryForList("""
                SELECT
                  br.id AS routeId,
                  br.route_code AS routeCode,
                  br.route_name AS routeName,
                  COUNT(rr.id) AS passengerCount
                FROM bus_route br
                LEFT JOIN ride_record rr
                  ON rr.route_id = br.id
                  AND rr.ride_time >= ?
                  AND rr.ride_time < DATE_ADD(?, INTERVAL 1 DAY)
                WHERE br.status = 1
                GROUP BY br.id, br.route_code, br.route_name
                ORDER BY passengerCount DESC, br.id ASC
                """, startDate, endDate);
    }

    private void validateRange(long routeId, LocalDate startDate, LocalDate endDate) {
        if (routeId <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "线路 ID 无效");
        }
        if (startDate == null || endDate == null || endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "日期范围无效");
        }
        if (startDate.plusDays(366).isBefore(endDate)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "日期范围不能超过 366 天");
        }
    }
}
