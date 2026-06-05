package com.busagent.dto;

public record LoginResponse(
        String tokenName,
        String tokenValue,
        AdminUserView admin
) {
}
