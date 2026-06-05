package com.busagent.dto;

import com.busagent.entity.BusRoute;

public record RouteView(
        Long id,
        String routeCode,
        String routeName,
        String direction,
        String startStationName,
        String endStationName,
        Integer status
) {
    public static RouteView from(BusRoute route) {
        return new RouteView(
                route.getId(),
                route.getRouteCode(),
                route.getRouteName(),
                route.getDirection(),
                route.getStartStationName(),
                route.getEndStationName(),
                route.getStatus()
        );
    }
}
