package com.busagent.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AiClientService {

    private final RestClient restClient;
    private final String baseUrl;

    public AiClientService(RestClient.Builder restClientBuilder,
                           @Value("${ai-service.base-url:http://localhost:8000}") String baseUrl) {
        this.restClient = restClientBuilder.build();
        this.baseUrl = baseUrl;
    }

    public Map<String, Object> analyzeFlow(Map<String, Object> payload) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/ai/analyze-flow")
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return response == null ? fallbackAnalyze(payload) : response;
        } catch (RuntimeException ex) {
            return fallbackAnalyze(payload);
        }
    }

    public Map<String, Object> dispatchAdvice(Map<String, Object> payload) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri(baseUrl + "/ai/dispatch-advice")
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });
            return response == null ? fallbackDispatch(payload) : response;
        } catch (RuntimeException ex) {
            return fallbackDispatch(payload);
        }
    }

    private Map<String, Object> fallbackAnalyze(Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", "AI 服务暂不可用，已基于后端统计数据生成规则分析。");
        result.put("keyFindings", payload.getOrDefault("statistics", Map.of()));
        result.put("trend", "请结合线路日客流、站点排行和时段对比判断客流变化。");
        result.put("suggestion", "优先关注高峰时段满载率和客流集中的站点。");
        result.put("fallback", true);
        return result;
    }

    private Map<String, Object> fallbackDispatch(Map<String, Object> payload) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("advice", payload.getOrDefault("ruleAdvice", "建议参考规则结果调整班次。"));
        result.put("reason", "AI 服务暂不可用，当前返回后端规则建议。");
        result.put("risk", "需结合实际道路拥堵、车辆可用数和司机排班进一步确认。");
        result.put("priority", payload.getOrDefault("adviceLevel", "medium"));
        result.put("fallback", true);
        return result;
    }
}
