from app.deepseek_client import generate_weekly_narrative
from app.moderation import moderation_error
from app.schemas import NarrativeContent, WeeklyFacts


def facts_payload() -> dict:
    return {
        "weekStart": "2026-10-26",
        "weekEnd": "2026-11-01",
        "overview": {
            "sessionCount": 3,
            "questionCount": 40,
            "correctCount": 31,
            "accuracy": 77.5,
            "previousAccuracy": 70.0,
            "accuracyDelta": 7.5,
            "studyMinutes": 24,
        },
        "weakTypes": [
            {
                "type": "TABLE_DIVISION",
                "label": "表内除法",
                "total": 15,
                "correct": 9,
                "accuracy": 60.0,
            }
        ],
        "frequentMistakes": [
            {
                "expression": "56 ÷ 8",
                "type": "TABLE_DIVISION",
                "typeLabel": "表内除法",
                "errorCount": 2,
                "correctAnswer": 7,
            }
        ],
        "daily": [{"date": "10-26", "questionCount": 40, "accuracy": 77.5}],
        "streakDays": 4,
        "recommendation": {
            "config": {
                "title": "周报薄弱题型提升",
                "count": 20,
                "types": ["TABLE_DIVISION"],
                "operandCount": 2,
                "allowParentheses": False,
                "maxNumber": 100,
                "maxFactor": 20,
            },
            "typeQuotas": {"TABLE_DIVISION": 20},
            "reason": "建议集中完成 20 道表内除法练习。",
        },
    }


def test_missing_key_returns_template(monkeypatch):
    monkeypatch.delenv("DEEPSEEK_API_KEY", raising=False)
    facts = WeeklyFacts.model_validate(facts_payload())
    result = generate_weekly_narrative(facts)
    assert result.source == "TEMPLATE"
    assert result.moderation_status == "PASSED"
    assert result.model is None
    assert "表内除法" in " ".join(result.highlights)


def test_moderation_rejects_private_or_unsafe_content():
    content = NarrativeContent(
        summary="请联系 13800138000 获取答案",
        highlights=["正常内容"],
        suggestions=["正常建议"],
    )
    assert moderation_error(content) is not None


def test_configured_model_response_is_validated(monkeypatch):
    monkeypatch.setenv("DEEPSEEK_API_KEY", "test-key")
    monkeypatch.setenv("DEEPSEEK_MODEL", "deepseek-chat")

    class Message:
        content = '{"summary":"本周练习稳定，表内除法需要继续巩固。","highlights":["完成 40 道题","学习 24 分钟"],"suggestions":["完成 20 道表内除法","复盘两道高频错题"]}'

    class Choice:
        message = Message()

    class Completions:
        def create(self, **kwargs):
            class Response:
                choices = [Choice()]
            return Response()

    class Chat:
        completions = Completions()

    class FakeOpenAI:
        def __init__(self, **kwargs):
            self.chat = Chat()

    monkeypatch.setattr("app.deepseek_client.OpenAI", FakeOpenAI)
    facts = WeeklyFacts.model_validate(facts_payload())
    result = generate_weekly_narrative(facts)
    assert result.source == "AGENT"
    assert result.model == "deepseek-chat"
    assert result.moderation_status == "PASSED"
    assert "表内除法" in result.summary


def test_placeholder_key_is_treated_as_missing(monkeypatch):
    monkeypatch.setenv("DEEPSEEK_API_KEY", "填写自己密钥")
    facts = WeeklyFacts.model_validate(facts_payload())
    result = generate_weekly_narrative(facts)
    assert result.source == "TEMPLATE"
    assert result.error_message == "DEEPSEEK_API_KEY 未配置"
