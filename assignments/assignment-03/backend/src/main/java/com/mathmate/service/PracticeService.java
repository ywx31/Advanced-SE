package com.mathmate.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mathmate.common.PageResult;
import com.mathmate.dto.practice.AnswerItem;
import com.mathmate.dto.practice.GeneratedQuestion;
import com.mathmate.dto.practice.PracticeConfig;
import com.mathmate.dto.practice.PracticeConfigRequest;
import com.mathmate.dto.practice.QuestionType;
import com.mathmate.dto.practice.QuestionView;
import com.mathmate.dto.practice.SessionDetail;
import com.mathmate.dto.practice.SessionSummary;
import com.mathmate.dto.practice.SubmitPracticeRequest;
import com.mathmate.dto.practice.WorksheetQuestion;
import com.mathmate.dto.practice.WorksheetView;
import com.mathmate.entity.PracticeQuestion;
import com.mathmate.entity.PracticeSession;
import com.mathmate.entity.WrongQuestion;
import com.mathmate.exception.BusinessException;
import com.mathmate.mapper.PracticeQuestionMapper;
import com.mathmate.mapper.PracticeSessionMapper;
import com.mathmate.mapper.WrongQuestionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class PracticeService {
    private final QuestionGeneratorService generatorService;
    private final PracticeSessionMapper sessionMapper;
    private final PracticeQuestionMapper questionMapper;
    private final WrongQuestionMapper wrongQuestionMapper;
    private final ObjectMapper objectMapper;

    public PracticeService(
            QuestionGeneratorService generatorService,
            PracticeSessionMapper sessionMapper,
            PracticeQuestionMapper questionMapper,
            WrongQuestionMapper wrongQuestionMapper,
            ObjectMapper objectMapper
    ) {
        this.generatorService = generatorService;
        this.sessionMapper = sessionMapper;
        this.questionMapper = questionMapper;
        this.wrongQuestionMapper = wrongQuestionMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public SessionDetail createSession(Long userId, PracticeConfigRequest request) {
        PracticeConfig config = normalizeConfig(request);
        List<GeneratedQuestion> generated = generatorService.generate(config);
        return persistSession(userId, config, generated);
    }

    public WorksheetView createWorksheet(PracticeConfigRequest request) {
        PracticeConfig config = normalizeConfig(request);
        List<GeneratedQuestion> generated = generatorService.generate(config);
        List<WorksheetQuestion> questions = new ArrayList<>();
        int index = 1;
        for (GeneratedQuestion item : generated) {
            questions.add(new WorksheetQuestion(
                    index++,
                    item.expression(),
                    item.type(),
                    item.answer()
            ));
        }
        return new WorksheetView(
                config.title(),
                config,
                LocalDateTime.now(),
                questions.size(),
                questions
        );
    }

    @Transactional
    public SessionDetail createCustomSession(
            Long userId,
            String title,
            List<QuestionType> types,
            int operandCount,
            List<GeneratedQuestion> generated
    ) {
        if (generated.isEmpty()) {
            throw new BusinessException("没有可生成的题目");
        }
        PracticeConfig config = new PracticeConfig(
                title,
                generated.size(),
                types.stream().distinct().toList(),
                Math.min(6, Math.max(2, operandCount)),
                true,
                100,
                20
        );
        return persistSession(userId, config, generated);
    }

    private SessionDetail persistSession(
            Long userId,
            PracticeConfig config,
            List<GeneratedQuestion> generated
    ) {
        PracticeSession session = new PracticeSession();
        session.setUserId(userId);
        session.setTitle(config.title());
        session.setConfigJson(toJson(config));
        session.setTotalCount(generated.size());
        session.setCorrectCount(0);
        session.setAccuracy(BigDecimal.ZERO);
        session.setDurationSeconds(0);
        session.setStatus("IN_PROGRESS");
        session.setStartedAt(LocalDateTime.now());
        sessionMapper.insert(session);

        int index = 1;
        for (GeneratedQuestion item : generated) {
            PracticeQuestion question = new PracticeQuestion();
            question.setSessionId(session.getId());
            question.setSortOrder(index++);
            question.setExpression(item.expression());
            question.setCorrectAnswer(item.answer());
            question.setQuestionType(item.type().name());
            question.setCanonicalKey(item.canonicalKey());
            questionMapper.insert(question);
        }
        return getSession(userId, session.getId());
    }

    public SessionDetail getSession(Long userId, Long sessionId) {
        PracticeSession session = requireSession(userId, sessionId);
        PracticeConfig config = fromJson(session.getConfigJson());
        boolean submitted = "SUBMITTED".equals(session.getStatus());
        List<QuestionView> questions = questionMapper.findBySessionId(sessionId).stream()
                .map(question -> new QuestionView(
                        question.getId(),
                        question.getSortOrder(),
                        question.getExpression(),
                        QuestionType.valueOf(question.getQuestionType()),
                        question.getUserAnswer(),
                        submitted ? question.getCorrectAnswer() : null,
                        submitted ? question.getCorrect() : null
                ))
                .toList();
        return new SessionDetail(
                session.getId(),
                session.getTitle(),
                config,
                session.getTotalCount(),
                session.getCorrectCount(),
                session.getAccuracy(),
                session.getDurationSeconds(),
                session.getStatus(),
                session.getStartedAt(),
                session.getSubmittedAt(),
                questions
        );
    }

    @Transactional
    public SessionDetail submit(Long userId, Long sessionId, SubmitPracticeRequest request) {
        PracticeSession session = requireSession(userId, sessionId);
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException("该练习已经提交，不能重复提交", HttpStatus.CONFLICT);
        }

        List<PracticeQuestion> questions = questionMapper.findBySessionId(sessionId);
        Map<Long, Integer> answers = new HashMap<>();
        for (AnswerItem item : request.answers()) {
            if (item != null && item.questionId() != null) {
                answers.put(item.questionId(), item.answer());
            }
        }

        int correctCount = 0;
        LocalDateTime now = LocalDateTime.now();
        for (PracticeQuestion question : questions) {
            Integer userAnswer = answers.get(question.getId());
            boolean correct = userAnswer != null && userAnswer.equals(question.getCorrectAnswer());
            questionMapper.updateAnswer(sessionId, question.getId(), userAnswer, correct);
            if (correct) {
                correctCount++;
                recordCorrect(userId, sessionId, question, now);
            } else {
                recordWrong(userId, question, userAnswer, now);
            }
        }

        int total = questions.size();
        BigDecimal accuracy = BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
        long seconds = Math.max(1, Duration.between(session.getStartedAt(), now).getSeconds());
        int duration = (int) Math.min(seconds, 86_400);
        int updated = sessionMapper.updateSubmission(
                sessionId, userId, correctCount, accuracy, duration, now
        );
        if (updated == 0) {
            throw new BusinessException("提交失败，请刷新后重试", HttpStatus.CONFLICT);
        }
        return getSession(userId, sessionId);
    }

    public PageResult<SessionSummary> listSessions(Long userId, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        long total = sessionMapper.countByUser(userId);
        List<SessionSummary> items = sessionMapper.findPage(
                        userId, (safePage - 1) * safeSize, safeSize
                ).stream()
                .map(this::toSummary)
                .toList();
        return new PageResult<>(items, total, safePage, safeSize);
    }

    public List<SessionSummary> recentSessions(Long userId, int limit) {
        return sessionMapper.findRecent(userId, Math.min(Math.max(limit, 1), 10)).stream()
                .map(this::toSummary)
                .toList();
    }

    private void recordWrong(Long userId, PracticeQuestion question, Integer userAnswer, LocalDateTime now) {
        WrongQuestion existing = wrongQuestionMapper.findByUserAndKey(userId, question.getCanonicalKey());
        if (existing == null) {
            WrongQuestion wrong = new WrongQuestion();
            wrong.setUserId(userId);
            wrong.setCanonicalKey(question.getCanonicalKey());
            wrong.setExpression(question.getExpression());
            wrong.setQuestionType(question.getQuestionType());
            wrong.setCorrectAnswer(question.getCorrectAnswer());
            wrong.setLastWrongAnswer(userAnswer);
            wrong.setWrongCount(1);
            wrong.setCorrectStreak(0);
            wrong.setMastered(false);
            wrong.setLastPracticedAt(now);
            wrongQuestionMapper.insert(wrong);
            return;
        }
        existing.setExpression(question.getExpression());
        existing.setQuestionType(question.getQuestionType());
        existing.setCorrectAnswer(question.getCorrectAnswer());
        existing.setLastWrongAnswer(userAnswer);
        existing.setLastPracticedAt(now);
        wrongQuestionMapper.updateAfterWrong(existing);
    }

    private void recordCorrect(Long userId, Long sessionId, PracticeQuestion question, LocalDateTime now) {
        WrongQuestion existing = wrongQuestionMapper.findByUserAndKey(userId, question.getCanonicalKey());
        if (existing == null) {
            return;
        }
        int streak = existing.getCorrectStreak() + 1;
        boolean mastered = streak >= 2;
        wrongQuestionMapper.updateAfterCorrect(existing.getId(), streak, mastered, now);
    }

    private PracticeSession requireSession(Long userId, Long sessionId) {
        PracticeSession session = sessionMapper.findByIdAndUser(sessionId, userId);
        if (session == null) {
            throw new BusinessException("练习记录不存在", HttpStatus.NOT_FOUND);
        }
        return session;
    }

    private PracticeConfig normalizeConfig(PracticeConfigRequest request) {
        List<QuestionType> types = request.types() == null
                ? List.of()
                : request.types().stream().distinct().toList();
        String title = request.title() == null || request.title().isBlank()
                ? "四则运算练习"
                : request.title().trim();
        int maxNumber = request.maxNumber() == null ? 100 : request.maxNumber();
        int maxFactor = request.maxFactor() == null ? 20 : request.maxFactor();
        int operandCount = request.operandCount() == null ? 3 : request.operandCount();
        return new PracticeConfig(
                title,
                request.count(),
                types,
                Math.min(6, Math.max(2, operandCount)),
                Boolean.TRUE.equals(request.allowParentheses()),
                maxNumber,
                maxFactor
        );
    }

    private SessionSummary toSummary(PracticeSession session) {
        PracticeConfig config = fromJson(session.getConfigJson());
        return new SessionSummary(
                session.getId(),
                session.getTitle(),
                session.getTotalCount(),
                session.getCorrectCount(),
                session.getAccuracy(),
                session.getDurationSeconds(),
                session.getStatus(),
                session.getStartedAt(),
                session.getSubmittedAt(),
                config.types()
        );
    }

    private String toJson(PracticeConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("练习配置序列化失败", exception);
        }
    }

    private PracticeConfig fromJson(String json) {
        try {
            PracticeConfig config = objectMapper.readValue(json, PracticeConfig.class);
            if (config.operandCount() < 2) {
                return new PracticeConfig(
                        config.title(), config.count(), config.types(), 3,
                        config.allowParentheses(), config.maxNumber(), config.maxFactor()
                );
            }
            return config;
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("练习配置解析失败", exception);
        }
    }
}
