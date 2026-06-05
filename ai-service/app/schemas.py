from typing import Any

from pydantic import BaseModel, Field


class AnalyzeFlowRequest(BaseModel):
    question: str | None = None
    route: dict[str, Any] = Field(default_factory=dict)
    startDate: str
    endDate: str
    statistics: dict[str, Any] = Field(default_factory=dict)


class AnalyzeFlowResponse(BaseModel):
    summary: str
    keyFindings: list[str]
    trend: str
    suggestion: str
    fallback: bool = True


class DispatchAdviceRequest(BaseModel):
    route: dict[str, Any] = Field(default_factory=dict)
    startDate: str
    endDate: str
    periodType: str
    metrics: dict[str, Any] = Field(default_factory=dict)
    stationFlow: list[dict[str, Any]] = Field(default_factory=list)
    adviceLevel: str = "medium"
    ruleAdvice: str


class DispatchAdviceResponse(BaseModel):
    advice: str
    reason: str
    risk: str
    priority: str
    fallback: bool = True
