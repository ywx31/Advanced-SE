from __future__ import annotations

import logging
import os

from dotenv import load_dotenv
from fastapi import FastAPI

from .deepseek_client import configured_api_key, generate_weekly_narrative
from .schemas import WeeklyFacts, WeeklyNarrativeResponse

load_dotenv()
logging.basicConfig(level=os.getenv("LOG_LEVEL", "INFO"))
logger = logging.getLogger("mathmate.agent")

app = FastAPI(
    title="MathMate Weekly Narrative Agent",
    version="1.0.0",
    description="将确定性 WeeklyFacts 转换为受约束的中文周报文案。",
)


@app.get("/health")
def health() -> dict[str, str | bool]:
    return {
        "status": "ok",
        "service": "mathmate-weekly-agent",
        "deepseekConfigured": bool(configured_api_key()),
    }


@app.post("/v1/weekly-narrative", response_model=WeeklyNarrativeResponse)
def weekly_narrative(facts: WeeklyFacts) -> WeeklyNarrativeResponse:
    result = generate_weekly_narrative(facts)
    logger.info(
        "weekly narrative generated source=%s model=%s moderation=%s",
        result.source,
        result.model,
        result.moderation_status,
    )
    return result
