package com.busagent.controller;

import com.busagent.common.ApiResponse;
import com.busagent.common.PageRequest;
import com.busagent.common.PageResult;
import com.busagent.service.AdminDataService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminDataController {

    private final AdminDataService adminDataService;

    public AdminDataController(AdminDataService adminDataService) {
        this.adminDataService = adminDataService;
    }

    @GetMapping("/dashboard/summary")
    public ApiResponse<Map<String, Object>> dashboardSummary() {
        return ApiResponse.ok(adminDataService.dashboardSummary());
    }

    @GetMapping("/routes")
    public ApiResponse<PageResult<Map<String, Object>>> routes(@Valid @ModelAttribute PageRequest pageRequest) {
        return ApiResponse.ok(adminDataService.routes(pageRequest));
    }

    @PostMapping("/routes")
    public ApiResponse<Map<String, Object>> createRoute(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(Map.of("id", adminDataService.createRoute(body)));
    }

    @PutMapping("/routes/{id}")
    public ApiResponse<Void> updateRoute(@PathVariable long id, @RequestBody Map<String, Object> body) {
        adminDataService.updateRoute(id, body);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/routes/{id}")
    public ApiResponse<Void> deactivateRoute(@PathVariable long id) {
        adminDataService.deactivate("bus_route", id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/stations")
    public ApiResponse<PageResult<Map<String, Object>>> stations(@Valid @ModelAttribute PageRequest pageRequest) {
        return ApiResponse.ok(adminDataService.stations(pageRequest));
    }

    @PostMapping("/stations")
    public ApiResponse<Map<String, Object>> createStation(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(Map.of("id", adminDataService.createStation(body)));
    }

    @PutMapping("/stations/{id}")
    public ApiResponse<Void> updateStation(@PathVariable long id, @RequestBody Map<String, Object> body) {
        adminDataService.updateStation(id, body);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/stations/{id}")
    public ApiResponse<Void> deactivateStation(@PathVariable long id) {
        adminDataService.deactivate("station", id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/vehicles")
    public ApiResponse<PageResult<Map<String, Object>>> vehicles(@Valid @ModelAttribute PageRequest pageRequest) {
        return ApiResponse.ok(adminDataService.vehicles(pageRequest));
    }

    @PostMapping("/vehicles")
    public ApiResponse<Map<String, Object>> createVehicle(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(Map.of("id", adminDataService.createVehicle(body)));
    }

    @PutMapping("/vehicles/{id}")
    public ApiResponse<Void> updateVehicle(@PathVariable long id, @RequestBody Map<String, Object> body) {
        adminDataService.updateVehicle(id, body);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/vehicles/{id}")
    public ApiResponse<Void> deactivateVehicle(@PathVariable long id) {
        adminDataService.deactivate("bus_vehicle", id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/drivers")
    public ApiResponse<PageResult<Map<String, Object>>> drivers(@Valid @ModelAttribute PageRequest pageRequest) {
        return ApiResponse.ok(adminDataService.drivers(pageRequest));
    }

    @PostMapping("/drivers")
    public ApiResponse<Map<String, Object>> createDriver(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(Map.of("id", adminDataService.createDriver(body)));
    }

    @PutMapping("/drivers/{id}")
    public ApiResponse<Void> updateDriver(@PathVariable long id, @RequestBody Map<String, Object> body) {
        adminDataService.updateDriver(id, body);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/drivers/{id}")
    public ApiResponse<Void> deactivateDriver(@PathVariable long id) {
        adminDataService.deactivate("driver", id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/schedules")
    public ApiResponse<PageResult<Map<String, Object>>> schedules(@Valid @ModelAttribute PageRequest pageRequest) {
        return ApiResponse.ok(adminDataService.schedules(pageRequest));
    }

    @PostMapping("/schedules")
    public ApiResponse<Map<String, Object>> createSchedule(@RequestBody Map<String, Object> body) {
        return ApiResponse.ok(Map.of("id", adminDataService.createSchedule(body)));
    }

    @PutMapping("/schedules/{id}")
    public ApiResponse<Void> updateSchedule(@PathVariable long id, @RequestBody Map<String, Object> body) {
        adminDataService.updateSchedule(id, body);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/schedules/{id}")
    public ApiResponse<Void> deactivateSchedule(@PathVariable long id) {
        adminDataService.deactivate("bus_schedule", id);
        return ApiResponse.ok(null);
    }

    @GetMapping("/ride-records")
    public ApiResponse<PageResult<Map<String, Object>>> rideRecords(@Valid @ModelAttribute PageRequest pageRequest) {
        return ApiResponse.ok(adminDataService.rideRecords(pageRequest));
    }
}
