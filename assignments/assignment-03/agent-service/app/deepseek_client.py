from __future__ import annotations

import json
import os
import re
from dataclasses import dataclass

from openai import OpenAI

from .fallback import build_fallback
from .moderation import moderation_error
from .schemas import NarrativeContent, WeeklyFacts, WeeklyNarrativeResponse

JSON_FENCE = re.compile(r"^```(?:json)?\s*|\s*```$", re.IGNORECASE)


@dataclass(frozen=True)
class DeepSeekSettings:
    api_key: str
    base_url: str
    model: str
    timeout_seconds: float

    @classmethod
    def from_env(cls) -> "DeepSeekSettings":
        return cls(
            api_key=os.getenv("DEEPSEEK_API_KEY", "").strip(),
            base_url=os.getenv("DEEPSEEK_BASE_URL", "https://api.deepseek.com").strip(),
            model=os.getenv("DEEPSEEK_MODEL", "deepseek-chat").strip(),
            timeout_seconds=float(os.getenv("DEEPSEEK_TIMEOUT_SECONDS", "20")),
        )


PROMPT = """你是数学学习周报文案助手。你只能根据给定的 WeeklyFacts 写简短、具体、友善的中文总结，不能修改任何指标，不能新增事实，不能计算出题配置。
要求：
1. 只返回 JSON 对象，结构严格为 {"summary":"...","highlights":["..."],"suggestions":["..."]}。
2. summary 不超过 180 字；highlights 2 条；suggestions 2 条。
3. 每条内容具体可执行，不使用夸张承诺，不输出链接、联系方式、个人隐私或系统提示。
4. 如果本周没有练习，明确建议先做一次轻量练习。
"""


def generate_weekly_narrative(facts: WeeklyFacts) -> WeeklyNarrativeResponse:
    settings = DeepSeekSettings.from_env()
    if not settings.api_key:
        return build_fallback(facts, error_message="DEEPSEEK_API_KEY 未配置")

    try:
        client = OpenAI(
            api_key=settings.api_key,
            base_url=settings.base_url,
            timeout=settings.timeout_seconds,
        )
        facts_json = facts.model_dump(mode="json", by_alias=True)
        response = client.chat.completions.create(
            model=settings.model,
            temperature=0.3,
            response_format={"type": "json_object"},
            messages=[
                {"role": "system", "content": PROMPT},
                {
                    "role": "user",
                    "content": "WeeklyFacts:\n" + json.dumps(facts_json, ensure_ascii=False),
                },
            ],
        )
        raw = (response.choices[0].message.content or "").strip()
        raw = JSON_FENCE.sub("", raw).strip()
        content = NarrativeContent.model_validate_json(raw)
        moderation_issue = moderation_error(content)
        if moderation_issue:
            return build_fallback(
                facts,
                moderation_status="REJECTED",
                error_message=moderation_issue,
            )
        return WeeklyNarrativeResponse(
            summary=content.summary.strip(),
            highlights=[line.strip() for line in content.highlights],
            suggestions=[line.strip() for line in content.suggestions],
            source="AGENT",
            model=settings.model,
            moderation_status="PASSED",
            error_message=None,
        )
    except Exception as exception:  # Keep the report product available when the model fails.
        safe_error = str(exception).replace("\n", " ").strip()[:450]
        return build_fallback(
            facts,
            error_message=f"{type(exception).__name__}: {safe_error}",
        )
