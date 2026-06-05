package com.busagent.controller;

import com.busagent.common.ApiResponse;
import com.busagent.common.PageRequest;
import com.busagent.common.PageResult;
import com.busagent.service.DispatchAdviceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/dispatch-advice")
public class DispatchAdviceController {

    private final DispatchAdviceService dispatchAdviceService;

    public DispatchAdviceController(DispatchAdviceService dispatchAdviceService) {
        this.dispatchAdviceService = dispatchAdviceService;
    }

    @PostMapping("/generate")
    public ApiResponse<Map<String, Object>> generate(@RequestBody DispatchAdviceService.GenerateRequest request) {
        return ApiResponse.ok(dispatchAdviceService.generate(request));
    }

    @GetMapping
    public ApiResponse<PageResult<Map<String, Object>>> list(@Valid @ModelAttribute PageRequest pageRequest) {
        return ApiResponse.ok(dispatchAdviceService.list(pageRequest));
    }
}
