from __future__ import annotations

from typing import List

from pydantic import BaseModel, ConfigDict, Field


class ApiModel(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="ignore")


class WeeklyOverview(ApiModel):
    session_count: int = Field(alias="sessionCount")
    question_count: int = Field(alias="questionCount")
    correct_count: int = Field(alias="correctCount")
    accuracy: float
    previous_accuracy: float = Field(alias="previousAccuracy")
    accuracy_delta: float = Field(alias="accuracyDelta")
    study_minutes: int = Field(alias="studyMinutes")


class WeeklyTypeStat(ApiModel):
    type: str
    label: str
    total: int
    correct: int
    accuracy: float


class WeeklyMistake(ApiModel):
    expression: str
    type: str
    type_label: str = Field(alias="typeLabel")
    error_count: int = Field(alias="errorCount")
    correct_answer: int = Field(alias="correctAnswer")


class WeeklyDailyPoint(ApiModel):
    date: str
    question_count: int = Field(alias="questionCount")
    accuracy: float


class PracticeConfig(ApiModel):
    title: str
    count: int
    types: List[str]
    operand_count: int = Field(alias="operandCount")
    allow_parentheses: bool = Field(alias="allowParentheses")
    max_number: int = Field(alias="maxNumber")
    max_factor: int = Field(alias="maxFactor")


class RecommendedPractice(ApiModel):
    config: PracticeConfig
    type_quotas: dict[str, int] = Field(default_factory=dict, alias="typeQuotas")
    reason: str


class WeeklyFacts(ApiModel):
    week_start: str = Field(alias="weekStart")
    week_end: str = Field(alias="weekEnd")
    overview: WeeklyOverview
    weak_types: List[WeeklyTypeStat] = Field(alias="weakTypes")
    frequent_mistakes: List[WeeklyMistake] = Field(alias="frequentMistakes")
    daily: List[WeeklyDailyPoint]
    streak_days: int = Field(alias="streakDays")
    recommendation: RecommendedPractice


class NarrativeContent(BaseModel):
    summary: str = Field(min_length=1, max_length=800)
    highlights: List[str] = Field(min_length=1, max_length=5)
    suggestions: List[str] = Field(min_length=1, max_length=5)


class WeeklyNarrativeResponse(ApiModel):
    summary: str
    highlights: List[str]
    suggestions: List[str]
    source: str
    model: str | None = None
    moderation_status: str = Field(alias="moderationStatus")
    error_message: str | None = Field(default=None, alias="errorMessage")
