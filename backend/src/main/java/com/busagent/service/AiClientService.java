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
        result.put("summary", "AI 服务暂不可用，已基于后端统计数据生成公交客流调度分析报告。");
        result.put("keyFindings", java.util.List.of(
                "关键指标：已汇总线路日客流、站点排行、时段对比和满载率统计。",
                "异常判断：请优先检查平均满载率是否超过 85% 以及高峰班均客流是否明显高于平峰。",
                "站点压力：请结合站点上车客流排行识别热点站点。",
                "数据依据：当前结论来自后端统计接口，未使用实时 GPS 或真实刷卡支付数据。"
        ));
        result.put("trend", "异常判断：AI 服务不可用时，系统保留规则分析能力，可继续支撑演示。");
        result.put("suggestion", "调度建议：优先关注高峰时段满载率和客流集中的站点；风险提示：当前运营客流为演示模拟数据，建议结合实际排班复核。");
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
