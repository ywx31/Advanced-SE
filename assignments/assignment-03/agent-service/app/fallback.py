from __future__ import annotations

from .schemas import WeeklyFacts, WeeklyNarrativeResponse


def build_fallback(
    facts: WeeklyFacts,
    *,
    moderation_status: str = "PASSED",
    error_message: str | None = None,
) -> WeeklyNarrativeResponse:
    overview = facts.overview
    if overview.question_count == 0:
        return WeeklyNarrativeResponse(
            summary=f"{facts.week_start} 至 {facts.week_end} 还没有完成练习。先从一个轻量的乘除热身开始，重新建立本周节奏。",
            highlights=["本周暂无已提交练习", "完成下一次练习后，周报会自动更新学习数据"],
            suggestions=["完成 20 道表内乘除练习", "优先保证准确率，再逐步提升速度"],
            source="TEMPLATE",
            model=None,
            moderation_status=moderation_status,
            error_message=error_message,
        )

    delta = overview.accuracy_delta
    if delta > 0:
        comparison = f"较上周提升 {abs(delta):.0f} 个百分点"
    elif delta < 0:
        comparison = f"较上周下降 {abs(delta):.0f} 个百分点"
    else:
        comparison = "与上周持平"

    summary = (
        f"本周共完成 {overview.session_count} 次练习、{overview.question_count} 道题，"
        f"正确率 {overview.accuracy:.1f}%，{comparison}。"
    )
    highlights = [
        f"累计学习约 {overview.study_minutes} 分钟，连续练习 {facts.streak_days} 天",
    ]
    if facts.weak_types:
        weakest = facts.weak_types[0]
        highlights.append(f"最值得关注的是{weakest.label}，本周正确率 {weakest.accuracy:.1f}%")
    else:
        highlights.append("本周题型样本正在积累，暂未形成稳定的薄弱题型判断")

    suggestions = [facts.recommendation.reason]
    if facts.frequent_mistakes:
        suggestions.append(
            f"复盘高频错题“{facts.frequent_mistakes[0].expression}”，再做同类题确认是否真正掌握"
        )
    else:
        suggestions.append("保持每天 10 分钟练习，并逐步缩短同等题量下的用时")

    return WeeklyNarrativeResponse(
        summary=summary,
        highlights=highlights,
        suggestions=suggestions,
        source="TEMPLATE",
        model=None,
        moderation_status=moderation_status,
        error_message=error_message,
    )
