from __future__ import annotations

from typing import Any

from .schemas import (
    AnalyzeFlowRequest,
    AnalyzeFlowResponse,
    DispatchAdviceRequest,
    DispatchAdviceResponse,
)
from .deepseek_client import analyze_with_deepseek


PERIOD_NAMES = {
    "morning_peak": "早高峰",
    "evening_peak": "晚高峰",
    "normal": "正常时段",
}


def analyze_flow(request: AnalyzeFlowRequest) -> AnalyzeFlowResponse:
    deepseek_response = analyze_with_deepseek(request)
    if deepseek_response:
        return deepseek_response

    route_name = _route_name(request.route)
    stats = request.statistics
    question = request.question or ""
    route_ranking = stats.get("routeRanking") or []
    route_flow = stats.get("routeFlow") or []
    station_flow = stats.get("stationFlow") or []
    peak_analysis = stats.get("peakAnalysis") or []
    load_rate = stats.get("loadRate") or []

    if route_ranking:
        return _analyze_route_ranking(request, route_ranking, question)

    total_passengers = sum(_number(row.get("passengerCount")) for row in route_flow)
    avg_load = _average(_number(row.get("loadRate")) for row in load_rate)
    top_station = station_flow[0].get("stationName") if station_flow else "暂无站点"
    busiest_period = _busiest_period(peak_analysis)

    key_findings = [
        f"关键指标：{request.startDate} 至 {request.endDate}，{route_name}累计客流约 {int(total_passengers)} 人次。",
        f"站点压力：客流最高站点为 {top_station}。",
        f"时段判断：客流最高时段为 {busiest_period}。",
        f"满载率依据：班次平均满载率约 {avg_load:.2f}%。",
    ]
    if avg_load >= 85:
        suggestion = "调度建议：满载率偏高，建议优先评估高峰加密班次或缩短发车间隔。"
        trend = "异常判断：线路存在明显拥挤压力，已超过高峰加车关注阈值。"
    elif avg_load >= 60:
        suggestion = "调度建议：建议维持当前班次，并持续观察重点站点客流。"
        trend = "异常判断：线路客流处于中高水平，需保持调度关注。"
    else:
        suggestion = "调度建议：当前满载率不高，可重点关注站点客流分布和低峰资源利用。"
        trend = "异常判断：线路整体承载压力可控。"

    summary = _question_summary(question, route_name, trend, top_station, busiest_period, avg_load)
    return AnalyzeFlowResponse(
        summary=summary,
        keyFindings=key_findings,
        trend=trend,
        suggestion=f"{suggestion} 风险提示：当前客流为演示模拟数据，未接入实时车辆 GPS，建议结合更长周期和实际排班复核。数据依据：线路日客流、站点排行、时段对比和满载率统计。",
        fallback=True,
    )


def _analyze_route_ranking(
    request: AnalyzeFlowRequest, rows: list[dict[str, Any]], question: str
) -> AnalyzeFlowResponse:
    top = rows[0]
    top_name = _route_name(top)
    top_count = int(_number(top.get("passengerCount")))
    total = int(sum(_number(row.get("passengerCount")) for row in rows))
    ranking_text = "；".join(
        f"{_route_name(row)} {int(_number(row.get('passengerCount')))} 人次" for row in rows
    )
    if "最低" in question or "最少" in question or "低" in question:
        bottom = min(rows, key=lambda row: _number(row.get("passengerCount")))
        summary = f"{request.startDate} 至 {request.endDate}，客流最低的是 {_route_name(bottom)}。"
    else:
        summary = f"{request.startDate} 至 {request.endDate}，客流最高的是 {top_name}，约 {top_count} 人次。"
    return AnalyzeFlowResponse(
        summary=summary,
        keyFindings=[
            f"关键指标：全部线路累计客流约 {total} 人次。",
            f"线路排行：{ranking_text}。",
            f"异常判断：最高客流线路为 {top_name}，需优先关注其高峰班次和热点站点。",
        ],
        trend="异常判断：已按线路汇总客流排名，线路间客流差异可作为调度优先级依据。",
        suggestion="调度建议：优先关注客流最高线路的高峰班次和重点站点压力。风险提示：全线路模式未展开单站明细，建议选择具体线路复核站点压力。数据依据：全线路客流排行统计。",
        fallback=True,
    )


def _question_summary(
    question: str, route_name: str, trend: str, top_station: str, busiest_period: str, avg_load: float
) -> str:
    if "站点" in question or "车站" in question:
        return f"{route_name}上车客流最高的站点是 {top_station}。"
    if "早高峰" in question or "晚高峰" in question or "高峰" in question:
        return f"{route_name}客流最高时段为 {busiest_period}，可重点检查该时段班次。"
    if "拥挤" in question or "满载" in question or "利用率" in question:
        return f"{route_name}平均满载率约 {avg_load:.2f}%，{trend}"
    return f"{route_name}在所选时间范围内客流表现为：{trend}"


def dispatch_advice(request: DispatchAdviceRequest) -> DispatchAdviceResponse:
    route_name = _route_name(request.route)
    period_name = PERIOD_NAMES.get(request.periodType, request.periodType)
    avg_load = _number(request.metrics.get("avgLoadRate"))
    passenger_count = int(_number(request.metrics.get("passengerCount")))
    schedule_count = int(_number(request.metrics.get("scheduleCount")))
    top_station = request.stationFlow[0].get("stationName") if request.stationFlow else "重点站点"

    reason = (
        f"{route_name}{period_name}共 {schedule_count} 个班次，承载 {passenger_count} 人次，"
        f"平均满载率约 {avg_load:.2f}%，客流集中站点为 {top_station}。"
    )
    risk = "调度前需核对车辆可用数、司机排班、道路拥堵和临时客流事件。"
    return DispatchAdviceResponse(
        advice=request.ruleAdvice,
        reason=reason,
        risk=risk,
        priority=request.adviceLevel,
        fallback=True,
    )


def _route_name(route: dict[str, Any]) -> str:
    return str(route.get("routeName") or route.get("route_name") or "所选线路")


def _busiest_period(rows: list[dict[str, Any]]) -> str:
    if not rows:
        return "暂无数据"
    row = max(rows, key=lambda item: _number(item.get("passengerCount")))
    return str(row.get("periodName") or PERIOD_NAMES.get(str(row.get("periodType")), "未知时段"))


def _number(value: Any) -> float:
    if isinstance(value, bool):
        return 0.0
    if isinstance(value, (int, float)):
        return float(value)
    try:
        return float(str(value))
    except (TypeError, ValueError):
        return 0.0


def _average(values: Any) -> float:
    nums = list(values)
    return sum(nums) / len(nums) if nums else 0.0
