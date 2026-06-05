package com.busagent.controller;

import com.busagent.common.ApiResponse;
import com.busagent.dto.AdminUserView;
import com.busagent.dto.LoginRequest;
import com.busagent.dto.LoginResponse;
import com.busagent.service.AdminAuthService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.ok(adminAuthService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<AdminUserView> me() {
        return ApiResponse.ok(adminAuthService.currentAdmin());
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        adminAuthService.logout();
        return ApiResponse.ok(null);
    }
}
