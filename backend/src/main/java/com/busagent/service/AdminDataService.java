package com.busagent.service;

import com.busagent.common.PageRequest;
import com.busagent.common.PageResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminDataService {

    private final JdbcTemplate jdbcTemplate;

    public AdminDataService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> dashboardSummary() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("routeCount", count("bus_route"));
        data.put("stationCount", count("station"));
        data.put("vehicleCount", count("bus_vehicle"));
        data.put("driverCount", count("driver"));
        data.put("scheduleCount", count("bus_schedule"));
        data.put("rideCount", count("ride_record"));
        return data;
    }

    public PageResult<Map<String, Object>> routes(PageRequest pageRequest) {
        return page("SELECT * FROM bus_route ORDER BY id DESC", "SELECT COUNT(*) FROM bus_route", pageRequest);
    }

    public long createRoute(Map<String, Object> body) {
        jdbcTemplate.update("""
                INSERT INTO bus_route
                  (route_code, route_name, direction, start_station_name, end_station_name, operation_start_time, operation_end_time, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, COALESCE(?, 1))
                """,
                body.get("routeCode"), body.get("routeName"), body.get("direction"),
                body.get("startStationName"), body.get("endStationName"),
                body.get("operationStartTime"), body.get("operationEndTime"), body.get("status"));
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void updateRoute(long id, Map<String, Object> body) {
        jdbcTemplate.update("""
                UPDATE bus_route
                SET route_name = ?, direction = ?, start_station_name = ?, end_station_name = ?,
                    operation_start_time = ?, operation_end_time = ?, status = COALESCE(?, status)
                WHERE id = ?
                """,
                body.get("routeName"), body.get("direction"), body.get("startStationName"),
                body.get("endStationName"), body.get("operationStartTime"),
                body.get("operationEndTime"), body.get("status"), id);
    }

    public PageResult<Map<String, Object>> stations(PageRequest pageRequest) {
        return page("SELECT * FROM station ORDER BY id DESC", "SELECT COUNT(*) FROM station", pageRequest);
    }

    public long createStation(Map<String, Object> body) {
        jdbcTemplate.update("""
                INSERT INTO station (station_code, station_name, area_name, longitude, latitude, status)
                VALUES (?, ?, ?, ?, ?, COALESCE(?, 1))
                """,
                body.get("stationCode"), body.get("stationName"), body.get("areaName"),
                body.get("longitude"), body.get("latitude"), body.get("status"));
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void updateStation(long id, Map<String, Object> body) {
        jdbcTemplate.update("""
                UPDATE station
                SET station_name = ?, area_name = ?, longitude = ?, latitude = ?, status = COALESCE(?, status)
                WHERE id = ?
                """,
                body.get("stationName"), body.get("areaName"), body.get("longitude"),
                body.get("latitude"), body.get("status"), id);
    }

    public PageResult<Map<String, Object>> vehicles(PageRequest pageRequest) {
        return page("SELECT * FROM bus_vehicle ORDER BY id DESC", "SELECT COUNT(*) FROM bus_vehicle", pageRequest);
    }

    public long createVehicle(Map<String, Object> body) {
        jdbcTemplate.update("""
                INSERT INTO bus_vehicle (vehicle_code, plate_no, capacity, status)
                VALUES (?, ?, COALESCE(?, 50), COALESCE(?, 1))
                """,
                body.get("vehicleCode"), body.get("plateNo"), body.get("capacity"), body.get("status"));
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void updateVehicle(long id, Map<String, Object> body) {
        jdbcTemplate.update("""
                UPDATE bus_vehicle
                SET plate_no = ?, capacity = COALESCE(?, capacity), status = COALESCE(?, status)
                WHERE id = ?
                """,
                body.get("plateNo"), body.get("capacity"), body.get("status"), id);
    }

    public PageResult<Map<String, Object>> drivers(PageRequest pageRequest) {
        return page("SELECT * FROM driver ORDER BY id DESC", "SELECT COUNT(*) FROM driver", pageRequest);
    }

    public long createDriver(Map<String, Object> body) {
        jdbcTemplate.update("""
                INSERT INTO driver (employee_no, driver_name, phone, status)
                VALUES (?, ?, ?, COALESCE(?, 1))
                """,
                body.get("employeeNo"), body.get("driverName"), body.get("phone"), body.get("status"));
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void updateDriver(long id, Map<String, Object> body) {
        jdbcTemplate.update("""
                UPDATE driver
                SET driver_name = ?, phone = ?, status = COALESCE(?, status)
                WHERE id = ?
                """,
                body.get("driverName"), body.get("phone"), body.get("status"), id);
    }

    public PageResult<Map<String, Object>> schedules(PageRequest pageRequest) {
        return page("""
                SELECT bs.*, br.route_name, bv.vehicle_code, d.driver_name
                FROM bus_schedule bs
                JOIN bus_route br ON br.id = bs.route_id
                JOIN bus_vehicle bv ON bv.id = bs.vehicle_id
                JOIN driver d ON d.id = bs.driver_id
                ORDER BY bs.depart_time DESC
                """, "SELECT COUNT(*) FROM bus_schedule", pageRequest);
    }

    public long createSchedule(Map<String, Object> body) {
        jdbcTemplate.update("""
                INSERT INTO bus_schedule (route_id, vehicle_id, driver_id, schedule_date, depart_time, period_type, status)
                VALUES (?, ?, ?, ?, ?, ?, COALESCE(?, 1))
                """,
                body.get("routeId"), body.get("vehicleId"), body.get("driverId"),
                body.get("scheduleDate"), body.get("departTime"), body.get("periodType"), body.get("status"));
        return jdbcTemplate.queryForObject("SELECT LAST_INSERT_ID()", Long.class);
    }

    public void updateSchedule(long id, Map<String, Object> body) {
        jdbcTemplate.update("""
                UPDATE bus_schedule
                SET route_id = ?, vehicle_id = ?, driver_id = ?, schedule_date = ?,
                    depart_time = ?, period_type = ?, status = COALESCE(?, status)
                WHERE id = ?
                """,
                body.get("routeId"), body.get("vehicleId"), body.get("driverId"),
                body.get("scheduleDate"), body.get("departTime"), body.get("periodType"),
                body.get("status"), id);
    }

    public PageResult<Map<String, Object>> rideRecords(PageRequest pageRequest) {
        return page("""
                SELECT rr.*, br.route_name, bs.depart_time, s.station_name AS boarding_station_name
                FROM ride_record rr
                JOIN bus_route br ON br.id = rr.route_id
                LEFT JOIN bus_schedule bs ON bs.id = rr.schedule_id
                JOIN station s ON s.id = rr.boarding_station_id
                ORDER BY rr.ride_time DESC
                """, "SELECT COUNT(*) FROM ride_record", pageRequest);
    }

    public void deactivate(String tableName, long id) {
        jdbcTemplate.update("UPDATE " + tableName + " SET status = 0 WHERE id = ?", id);
    }

    private long count(String tableName) {
        Long value = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        return value == null ? 0 : value;
    }

    private PageResult<Map<String, Object>> page(String sql, String countSql, PageRequest pageRequest) {
        long total = countQuery(countSql);
        List<Map<String, Object>> records = jdbcTemplate.queryForList(
                sql + " LIMIT ? OFFSET ?",
                pageRequest.getSize(),
                pageRequest.offset()
        );
        return PageResult.of(records, total, pageRequest.getPage(), pageRequest.getSize());
    }

    private long countQuery(String sql) {
        Long value = jdbcTemplate.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }
}
