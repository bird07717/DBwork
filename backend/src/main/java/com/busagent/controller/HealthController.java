package com.busagent.controller;

import com.busagent.common.ApiResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private final JdbcTemplate jdbcTemplate;
    private final StringRedisTemplate redisTemplate;

    public HealthController(JdbcTemplate jdbcTemplate, StringRedisTemplate redisTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.redisTemplate = redisTemplate;
    }

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("service", "bus-agent-backend");
        data.put("database", checkDatabase());
        data.put("redis", checkRedis());
        return ApiResponse.ok(data);
    }

    private String checkDatabase() {
        Integer result = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        return result != null && result == 1 ? "up" : "unknown";
    }

    private String checkRedis() {
        String pong = redisTemplate.getConnectionFactory()
                .getConnection()
                .ping();
        return "PONG".equalsIgnoreCase(pong) ? "up" : "unknown";
    }
}
