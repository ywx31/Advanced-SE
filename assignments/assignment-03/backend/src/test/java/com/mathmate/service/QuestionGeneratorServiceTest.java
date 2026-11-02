package com.mathmate.service;

import com.mathmate.dto.practice.GeneratedQuestion;
import com.mathmate.dto.practice.PracticeConfig;
import com.mathmate.dto.practice.QuestionType;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionGeneratorServiceTest {
    private final QuestionGeneratorService service = new QuestionGeneratorService();

    @Test
    void generatesRequestedNumberOfUniqueLegalQuestions() {
        PracticeConfig config = new PracticeConfig(
                "综合测试",
                200,
                List.of(
                        QuestionType.TABLE_MULTIPLICATION,
                        QuestionType.TABLE_DIVISION,
                        QuestionType.ADVANCED_MULTIPLICATION,
                        QuestionType.ADVANCED_DIVISION,
                        QuestionType.MIXED_OPERATIONS
                ),
                3,
                true,
                100,
                20
        );

        List<GeneratedQuestion> questions = service.generate(config);

        assertEquals(200, questions.size());
        assertEquals(200, new HashSet<>(questions.stream().map(GeneratedQuestion::canonicalKey).toList()).size());
        for (GeneratedQuestion question : questions) {
            assertFalse(question.expression().isBlank());
            assertNotNull(question.type(), "每道题都必须带有题型标签");
            assertTrue(question.answer() >= 0 && question.answer() <= 100);
            assertTrue(question.expression().matches(".*[+−×÷].*"));
        }
    }

    @Test
    void generatesThreeHundredUniqueMixedWorksheets() {
        PracticeConfig config = new PracticeConfig(
                "作业2 300题",
                300,
                List.of(QuestionType.MIXED_OPERATIONS),
                3,
                false,
                100,
                20
        );

        List<GeneratedQuestion> questions = service.generate(config);

        assertEquals(300, questions.size());
        assertEquals(300, new HashSet<>(questions.stream().map(GeneratedQuestion::canonicalKey).toList()).size());
        assertTrue(questions.stream().allMatch(item -> item.answer() >= 0 && item.answer() <= 100));
    }

    @Test
    void supportsSixOperandsAndFiveOperators() {
        PracticeConfig config = new PracticeConfig(
                "六个数混合运算",
                60,
                List.of(QuestionType.MIXED_OPERATIONS),
                6,
                true,
                100,
                20
        );

        List<GeneratedQuestion> questions = service.generate(config);
        assertEquals(60, questions.size());
        for (GeneratedQuestion question : questions) {
            long numberCount = java.util.regex.Pattern.compile("\\d+")
                    .matcher(question.expression()).results().count();
            long operatorCount = question.expression().chars()
                    .filter(ch -> "+−×÷".indexOf(ch) >= 0)
                    .count();
            assertEquals(6, numberCount, question.expression());
            assertEquals(5, operatorCount, question.expression());
        }
    }

    @Test
    void excludesCanonicalKeysUsedBySimilarPractice() {
        PracticeConfig config = new PracticeConfig(
                "同类练习",
                20,
                List.of(QuestionType.TABLE_MULTIPLICATION),
                2,
                false,
                100,
                20
        );
        Set<String> excluded = Set.of("TM:1:2", "TM:2:3", "TM:3:4");

        List<GeneratedQuestion> questions = service.generate(config, excluded);

        assertEquals(20, questions.size());
        assertTrue(questions.stream().noneMatch(item -> excluded.contains(item.canonicalKey())));
    }

    @Test
    void generatesTableQuestionsWithoutLeavingTable() {
        PracticeConfig config = new PracticeConfig(
                "表内练习",
                30,
                List.of(QuestionType.TABLE_MULTIPLICATION, QuestionType.TABLE_DIVISION),
                3,
                false,
                100,
                20
        );

        List<GeneratedQuestion> questions = service.generate(config);
        assertEquals(30, questions.size());
        assertTrue(questions.stream().noneMatch(question -> question.expression().contains("(")));
    }
}
