package com.busagent.config;

import com.busagent.common.ApiResponse;
import com.busagent.common.BusinessException;
import com.busagent.common.ErrorCode;
import cn.dev33.satoken.exception.NotLoginException;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ApiResponse<Void> handleBusinessException(BusinessException ex) {
        return ApiResponse.fail(ex.errorCode().code(), ex.getMessage());
    }

    @ExceptionHandler(NotLoginException.class)
    public ApiResponse<Void> handleNotLoginException(NotLoginException ex) {
        return ApiResponse.fail(ErrorCode.UNAUTHORIZED);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.code(), message);
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBindException(BindException ex) {
        String message = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("; "));
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.code(), message);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ApiResponse<Void> handleConstraintViolationException(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations()
                .stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .collect(Collectors.joining("; "));
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.code(), message);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            HttpMessageNotReadableException.class
    })
    public ApiResponse<Void> handleBadRequestException(Exception ex) {
        return ApiResponse.fail(ErrorCode.BAD_REQUEST.code(), ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleException(Exception ex) {
        return ApiResponse.fail(ErrorCode.INTERNAL_ERROR);
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }
}
