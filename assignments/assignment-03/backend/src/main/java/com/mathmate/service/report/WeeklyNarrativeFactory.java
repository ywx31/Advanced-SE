package com.mathmate.service.report;

import com.mathmate.dto.report.WeeklyFacts;
import com.mathmate.dto.report.WeeklyNarrative;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class WeeklyNarrativeFactory {
    private static final DateTimeFormatter RANGE = DateTimeFormatter.ofPattern("M月d日");

    public WeeklyNarrative create(WeeklyFacts facts) {
        String range = facts.weekStart().format(RANGE) + "—" + facts.weekEnd().format(RANGE);
        if (facts.overview().questionCount() == 0) {
            return new WeeklyNarrative(
                    range + "还没有完成练习。先从一个轻量的乘除热身开始，重新建立本周节奏。",
                    List.of("本周暂无已提交练习", "学习数据会从下一次完成练习后开始累计"),
                    List.of("完成 20 道表内乘除练习", "练习时优先保证准确率，再逐步提升速度"),
                    "TEMPLATE",
                    null,
                    "PASSED"
            );
        }

        String comparison = comparisonText(facts.overview().accuracyDelta());
        String summary = "本周共完成 " + facts.overview().sessionCount() + " 次练习、"
                + facts.overview().questionCount() + " 道题，正确率 "
                + facts.overview().accuracy().stripTrailingZeros().toPlainString() + "%，"
                + comparison + "。";

        List<String> highlights = new ArrayList<>();
        highlights.add("累计学习约 " + facts.overview().studyMinutes() + " 分钟，连续练习 "
                + facts.streakDays() + " 天");
        if (!facts.weakTypes().isEmpty()) {
            var weakest = facts.weakTypes().get(0);
            highlights.add("当前最值得关注的是" + weakest.label() + "，本周正确率 "
                    + weakest.accuracy().stripTrailingZeros().toPlainString() + "%");
        } else {
            highlights.add("本周题型样本正在积累，暂未形成稳定的薄弱题型判断");
        }

        List<String> suggestions = new ArrayList<>();
        suggestions.add(facts.recommendation().reason());
        if (!facts.frequentMistakes().isEmpty()) {
            suggestions.add("复盘高频错题“" + facts.frequentMistakes().get(0).expression()
                    + "”，再做同类题确认是否真正掌握");
        } else {
            suggestions.add("保持每天 10 分钟练习，并尝试逐步缩短同等题量下的用时");
        }

        return new WeeklyNarrative(summary, highlights, suggestions, "TEMPLATE", null, "PASSED");
    }

    private String comparisonText(BigDecimal delta) {
        int result = delta.compareTo(BigDecimal.ZERO);
        if (result > 0) {
            return "较上周提升 " + delta.abs().stripTrailingZeros().toPlainString() + " 个百分点";
        }
        if (result < 0) {
            return "较上周下降 " + delta.abs().stripTrailingZeros().toPlainString() + " 个百分点";
        }
        return "与上周持平";
    }
}
