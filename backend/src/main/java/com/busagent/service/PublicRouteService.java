package com.busagent.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.busagent.common.BusinessException;
import com.busagent.common.ErrorCode;
import com.busagent.dto.RouteView;
import com.busagent.dto.StationView;
import com.busagent.entity.BusRoute;
import com.busagent.mapper.BusRouteMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PublicRouteService {

    private final BusRouteMapper busRouteMapper;
    private final JdbcTemplate jdbcTemplate;

    public PublicRouteService(BusRouteMapper busRouteMapper, JdbcTemplate jdbcTemplate) {
        this.busRouteMapper = busRouteMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<RouteView> listRoutes() {
        return busRouteMapper.selectList(new LambdaQueryWrapper<BusRoute>()
                        .eq(BusRoute::getStatus, 1)
                        .orderByAsc(BusRoute::getId))
                .stream()
                .map(RouteView::from)
                .toList();
    }

    public RouteView getRoute(Long id) {
        BusRoute route = busRouteMapper.selectById(id);
        if (route == null || route.getStatus() == null || route.getStatus() != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "线路不存在");
        }
        return RouteView.from(route);
    }

    public List<StationView> listStations(Long routeId) {
        getRoute(routeId);
        return jdbcTemplate.query("""
                SELECT s.id, s.station_code, s.station_name, rs.station_order, s.longitude, s.latitude
                FROM route_station rs
                JOIN station s ON s.id = rs.station_id
                WHERE rs.route_id = ?
                ORDER BY rs.station_order ASC
                """,
                (rs, rowNum) -> new StationView(
                        rs.getLong("id"),
                        rs.getString("station_code"),
                        rs.getString("station_name"),
                        rs.getInt("station_order"),
                        rs.getBigDecimal("longitude"),
                        rs.getBigDecimal("latitude")
                ),
                routeId);
    }
}
