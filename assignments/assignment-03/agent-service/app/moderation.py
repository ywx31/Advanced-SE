from __future__ import annotations

import re

from .schemas import NarrativeContent

UNSAFE_PATTERNS = (
    re.compile(r"https?://|www\.", re.IGNORECASE),
    re.compile(r"\b\d{11}\b"),
    re.compile(r"[\w.+-]+@[\w.-]+\.[A-Za-z]{2,}"),
    re.compile(r"忽略(之前|以上)|系统提示|system\s*prompt|api[_ -]?key", re.IGNORECASE),
    re.compile(r"(自杀|自残|暴力|色情|赌博)", re.IGNORECASE),
)


def moderation_error(content: NarrativeContent) -> str | None:
    lines = [content.summary, *content.highlights, *content.suggestions]
    for line in lines:
        if not line.strip():
            return "存在空白内容"
        if len(line) > 300:
            return "单条内容过长"
        if any(pattern.search(line) for pattern in UNSAFE_PATTERNS):
            return "内容触发安全规则"
    return None
