package com.busagent.controller;

import com.busagent.common.ApiResponse;
import com.busagent.common.PageRequest;
import com.busagent.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/page")
    public ApiResponse<PageResult<String>> page(@Valid @ModelAttribute PageRequest request) {
        PageResult<String> result = PageResult.of(
                List.of("sample"),
                1,
                request.getPage(),
                request.getSize()
        );
        return ApiResponse.ok(result);
    }
}
