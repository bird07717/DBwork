from __future__ import annotations

import json
import os
from pathlib import Path
from typing import Any
from urllib import error, request

from .schemas import AnalyzeFlowRequest, AnalyzeFlowResponse


SUPPORTED_MODELS = ("deepseek-v4-flash", "deepseek-v4-pro")
DEFAULT_MODEL = "deepseek-v4-flash"
DEFAULT_BASE_URL = "https://api.deepseek.com"
ENV_LOADED = False


class DeepSeekError(RuntimeError):
    pass


def analyze_with_deepseek(payload: AnalyzeFlowRequest) -> AnalyzeFlowResponse | None:
    api_key = _api_key()
    if not api_key:
        return None

    model = _safe_model(payload.model)
    body = {
        "model": model,
        "messages": [
            {"role": "system", "content": _system_prompt()},
            {"role": "user", "content": _user_prompt(payload)},
        ],
        "temperature": 0.2,
        "max_tokens": 900,
        "response_format": {"type": "json_object"},
    }
    try:
        result = _post_chat_completion(body, api_key)
        content = result["choices"][0]["message"]["content"]
        parsed = json.loads(content)
        return AnalyzeFlowResponse(
            summary=str(parsed.get("summary") or "已生成公交客流调度分析。"),
            keyFindings=_string_list(parsed.get("keyFindings")),
            trend=str(parsed.get("trend") or "已结合当前线路统计数据完成趋势判断。"),
            suggestion=str(parsed.get("suggestion") or "建议结合满载率、热点站点和高峰时段优化班次。"),
            fallback=False,
        )
    except (DeepSeekError, KeyError, IndexError, TypeError, json.JSONDecodeError, ValueError):
        return None


def _api_key() -> str:
    _load_local_env()
    return os.getenv("DEEPSEEK_API_KEY") or os.getenv("LLM_API_KEY") or ""


def _base_url() -> str:
    _load_local_env()
    return (os.getenv("DEEPSEEK_BASE_URL") or os.getenv("LLM_BASE_URL") or DEFAULT_BASE_URL).rstrip("/")


def _safe_model(model: str | None) -> str:
    _load_local_env()
    configured = model or os.getenv("DEEPSEEK_MODEL") or os.getenv("LLM_MODEL") or DEFAULT_MODEL
    return configured if configured in SUPPORTED_MODELS else DEFAULT_MODEL


def llm_configured() -> bool:
    return bool(_api_key())


def _load_local_env() -> None:
    global ENV_LOADED
    if ENV_LOADED:
        return
    ENV_LOADED = True
    service_root = Path(__file__).resolve().parents[1]
    for filename in (".env", ".env.local"):
        path = service_root / filename
        if not path.exists():
            continue
        for raw_line in path.read_text(encoding="utf-8").splitlines():
            line = raw_line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            os.environ.setdefault(key.strip(), value.strip().strip("\"'"))


def _post_chat_completion(body: dict[str, Any], api_key: str) -> dict[str, Any]:
    data = json.dumps(body, ensure_ascii=False).encode("utf-8")
    req = request.Request(
        f"{_base_url()}/chat/completions",
        data=data,
        method="POST",
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
            "Accept": "application/json",
        },
    )
    try:
        with request.urlopen(req, timeout=30) as response:
            return json.loads(response.read().decode("utf-8"))
    except error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="replace")
        raise DeepSeekError(f"deepseek http {exc.code}: {detail}") from exc
    except (error.URLError, TimeoutError, json.JSONDecodeError) as exc:
        raise DeepSeekError("deepseek request failed") from exc


def _system_prompt() -> str:
    return (
        "你是公交客流调度分析助手。必须只基于用户提供的线路、日期范围和统计数据回答，"
        "不能编造不存在的线路、站点、日期、车辆或客流。"
        "输出必须是 JSON 对象，字段固定为 summary、keyFindings、trend、suggestion。"
        "keyFindings 必须是 3 到 5 条中文字符串。"
        "回答要面向公交调度人员，形成公交客流调度分析报告。"
        "summary 写报告摘要；keyFindings 覆盖关键指标、热点站点、时段压力和异常判断；"
        "trend 写趋势或异常判断；suggestion 必须包含具体调度建议、风险提示和数据依据。"
        "项目调度阈值固定为：高峰满载率大于等于 85% 建议增加班次，60% 到 85% 建议维持并观察，"
        "正常时段满载率小于 30% 建议减少班次或延长发车间隔；不得使用 80% 红线等输入中不存在的阈值。"
        "不得声称接入实时 GPS、真实刷卡支付或输入中不存在的数据。"
    )


def _user_prompt(payload: AnalyzeFlowRequest) -> str:
    compact = {
        "question": payload.question,
        "selectedModel": _safe_model(payload.model),
        "route": payload.route,
        "dateRange": {"startDate": payload.startDate, "endDate": payload.endDate},
        "statistics": _compact_statistics(payload.statistics),
    }
    return "请基于以下公交客流数据生成调度分析报告：\n" + json.dumps(compact, ensure_ascii=False, default=str)


def _compact_statistics(statistics: dict[str, Any]) -> dict[str, Any]:
    return {
        key: _limit_rows(value, 12)
        for key, value in statistics.items()
        if key in {"routeRanking", "routeFlow", "stationFlow", "peakAnalysis", "loadRate"}
    }


def _limit_rows(value: Any, limit: int) -> Any:
    if isinstance(value, list):
        return value[:limit]
    return value


def _string_list(value: Any) -> list[str]:
    if isinstance(value, list):
        items = [str(item).strip() for item in value if str(item).strip()]
        if items:
            return items[:5]
    return ["已基于当前线路、日期范围和客流统计生成分析。"]
