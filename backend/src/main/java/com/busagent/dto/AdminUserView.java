package com.busagent.dto;

import com.busagent.entity.AdminUser;

import java.time.LocalDateTime;

public record AdminUserView(
        Long id,
        String username,
        String realName,
        Integer status,
        LocalDateTime lastLoginTime
) {
    public static AdminUserView from(AdminUser user) {
        return new AdminUserView(
                user.getId(),
                user.getUsername(),
                user.getRealName(),
                user.getStatus(),
                user.getLastLoginTime()
        );
    }
}
