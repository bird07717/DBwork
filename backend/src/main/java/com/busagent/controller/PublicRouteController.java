package com.busagent.controller;

import com.busagent.common.ApiResponse;
import com.busagent.dto.RouteView;
import com.busagent.dto.StationView;
import com.busagent.service.PublicRouteService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/public/routes")
public class PublicRouteController {

    private final PublicRouteService publicRouteService;

    public PublicRouteController(PublicRouteService publicRouteService) {
        this.publicRouteService = publicRouteService;
    }

    @GetMapping
    public ApiResponse<List<RouteView>> listRoutes() {
        return ApiResponse.ok(publicRouteService.listRoutes());
    }

    @GetMapping("/{id}")
    public ApiResponse<RouteView> getRoute(@PathVariable Long id) {
        return ApiResponse.ok(publicRouteService.getRoute(id));
    }

    @GetMapping("/{id}/stations")
    public ApiResponse<List<StationView>> listStations(@PathVariable Long id) {
        return ApiResponse.ok(publicRouteService.listStations(id));
    }
}
