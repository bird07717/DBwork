package com.busagent.common;

import java.time.LocalDateTime;

public record ApiResponse<T>(
        int code,
        String message,
        T data,
        LocalDateTime timestamp
) {
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(ErrorCode.SUCCESS.code(), ErrorCode.SUCCESS.message(), data, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> fail(int code, String message) {
        return new ApiResponse<>(code, message, null, LocalDateTime.now());
    }

    public static <T> ApiResponse<T> fail(ErrorCode errorCode) {
        return fail(errorCode.code(), errorCode.message());
    }
}
