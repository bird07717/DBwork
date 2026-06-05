from typing import Any

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from .ai_engine import analyze_flow, dispatch_advice
from .deepseek_client import llm_configured
from .schemas import AnalyzeFlowRequest, AnalyzeFlowResponse, DispatchAdviceRequest, DispatchAdviceResponse


app = FastAPI(title="Bus Agent AI Service", version="0.1.0")

app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.get("/health")
def health() -> dict[str, Any]:
    return {
        "service": "bus-agent-ai-service",
        "status": "up",
        "llmConfigured": llm_configured(),
    }


@app.post("/ai/analyze-flow", response_model=AnalyzeFlowResponse)
def analyze_flow_endpoint(request: AnalyzeFlowRequest) -> AnalyzeFlowResponse:
    return analyze_flow(request)


@app.post("/ai/dispatch-advice", response_model=DispatchAdviceResponse)
def dispatch_advice_endpoint(request: DispatchAdviceRequest) -> DispatchAdviceResponse:
    return dispatch_advice(request)
