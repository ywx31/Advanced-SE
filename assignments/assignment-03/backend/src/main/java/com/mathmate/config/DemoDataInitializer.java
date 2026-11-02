package com.mathmate.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathmate.dto.practice.GeneratedQuestion;
import com.mathmate.dto.practice.PracticeConfig;
import com.mathmate.dto.practice.QuestionType;
import com.mathmate.entity.PracticeQuestion;
import com.mathmate.entity.PracticeSession;
import com.mathmate.entity.User;
import com.mathmate.entity.WrongQuestion;
import com.mathmate.mapper.PracticeQuestionMapper;
import com.mathmate.mapper.PracticeSessionMapper;
import com.mathmate.mapper.UserMapper;
import com.mathmate.mapper.WrongQuestionMapper;
import com.mathmate.service.QuestionGeneratorService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements ApplicationRunner {
    private final UserMapper userMapper;
    private final PracticeSessionMapper sessionMapper;
    private final PracticeQuestionMapper questionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final QuestionGeneratorService generatorService;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public DemoDataInitializer(
            UserMapper userMapper,
            PracticeSessionMapper sessionMapper,
            PracticeQuestionMapper questionMapper,
            WrongQuestionMapper wrongQuestionMapper,
            QuestionGeneratorService generatorService,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper
    ) {
        this.userMapper = userMapper;
        this.sessionMapper = sessionMapper;
        this.questionMapper = questionMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.generatorService = generatorService;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        User demo = userMapper.findByUsername("demo");
        boolean newlyCreated = false;
        if (demo == null) {
            demo = new User();
            demo.setUsername("demo");
            demo.setPasswordHash(passwordEncoder.encode("demo123"));
            demo.setDisplayName("小明");
            userMapper.insert(demo);
            newlyCreated = true;
        }
        // 只在新账号首次创建时写入演示历史；用户清空数据后重启不会再次灌入。
        if (!newlyCreated || sessionMapper.countByUser(demo.getId()) > 0) {
            return;
        }

        seed(demo.getId(), 10, "乘法与除法热身", 67,
                List.of(QuestionType.TABLE_MULTIPLICATION, QuestionType.TABLE_DIVISION), false);
        seed(demo.getId(), 6, "表外计算进阶", 75,
                List.of(QuestionType.ADVANCED_MULTIPLICATION, QuestionType.ADVANCED_DIVISION), false);
        seed(demo.getId(), 3, "带括号混合运算", 83,
                List.of(QuestionType.MIXED_OPERATIONS), true);
        seed(demo.getId(), 1, "综合能力挑战", 92,
                List.of(
                        QuestionType.TABLE_MULTIPLICATION,
                        QuestionType.TABLE_DIVISION,
                        QuestionType.ADVANCED_MULTIPLICATION,
                        QuestionType.ADVANCED_DIVISION,
                        QuestionType.MIXED_OPERATIONS
                ), true);
    }

    private void seed(
            Long userId,
            int daysAgo,
            String title,
            int targetAccuracy,
            List<QuestionType> types,
            boolean parentheses
    ) throws JsonProcessingException {
        int count = 12;
        PracticeConfig config = new PracticeConfig(
                title, count, types, 3, parentheses, 100, 20
        );
        List<GeneratedQuestion> generated = generatorService.generate(config);
        LocalDateTime submittedAt = LocalDateTime.now().minusDays(daysAgo).withHour(19).withMinute(30);
        LocalDateTime startedAt = submittedAt.minusMinutes(9);

        int correctCount = 0;
        boolean[] correctFlags = new boolean[generated.size()];
        for (int index = 0; index < generated.size(); index++) {
            boolean correct = ((index * 17 + daysAgo * 13) % 100) < targetAccuracy;
            correctFlags[index] = correct;
            if (correct) {
                correctCount++;
            }
        }
        if (correctCount == generated.size()) {
            correctFlags[generated.size() - 1] = false;
            correctCount--;
        }
        if (correctCount == 0) {
            correctFlags[0] = true;
            correctCount++;
        }

        BigDecimal accuracy = BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setTitle(title);
        session.setConfigJson(objectMapper.writeValueAsString(config));
        session.setTotalCount(count);
        session.setCorrectCount(correctCount);
        session.setAccuracy(accuracy);
        session.setDurationSeconds(360 + daysAgo * 15);
        session.setStatus("SUBMITTED");
        session.setStartedAt(startedAt);
        session.setSubmittedAt(submittedAt);
        sessionMapper.insertWithSubmission(session);

        for (int index = 0; index < generated.size(); index++) {
            GeneratedQuestion item = generated.get(index);
            boolean correct = correctFlags[index];
            Integer userAnswer = correct ? item.answer() : item.answer() + 3;

            PracticeQuestion question = new PracticeQuestion();
            question.setSessionId(session.getId());
            question.setSortOrder(index + 1);
            question.setExpression(item.expression());
            question.setCorrectAnswer(item.answer());
            question.setQuestionType(item.type().name());
            question.setCanonicalKey(item.canonicalKey());
            question.setUserAnswer(userAnswer);
            question.setCorrect(correct);
            questionMapper.insert(question);

            if (!correct) {
                recordWrong(userId, item, userAnswer, submittedAt);
            }
        }
    }

    private void recordWrong(Long userId, GeneratedQuestion item, Integer wrongAnswer, LocalDateTime time) {
        WrongQuestion existing = wrongQuestionMapper.findByUserAndKey(userId, item.canonicalKey());
        if (existing == null) {
            WrongQuestion wrong = new WrongQuestion();
            wrong.setUserId(userId);
            wrong.setCanonicalKey(item.canonicalKey());
            wrong.setExpression(item.expression());
            wrong.setQuestionType(item.type().name());
            wrong.setCorrectAnswer(item.answer());
            wrong.setLastWrongAnswer(wrongAnswer);
            wrong.setWrongCount(1);
            wrong.setCorrectStreak(0);
            wrong.setMastered(false);
            wrong.setLastPracticedAt(time);
            wrongQuestionMapper.insert(wrong);
            return;
        }
        existing.setExpression(item.expression());
        existing.setQuestionType(item.type().name());
        existing.setCorrectAnswer(item.answer());
        existing.setLastWrongAnswer(wrongAnswer);
        existing.setLastPracticedAt(time);
        wrongQuestionMapper.updateAfterWrong(existing);
    }
}
