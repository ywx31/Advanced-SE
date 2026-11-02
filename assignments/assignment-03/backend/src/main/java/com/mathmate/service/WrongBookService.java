package com.mathmate.service;

import com.mathmate.common.PageResult;
import com.mathmate.dto.practice.GeneratedQuestion;
import com.mathmate.dto.practice.PracticeConfig;
import com.mathmate.dto.practice.QuestionType;
import com.mathmate.dto.practice.SessionDetail;
import com.mathmate.dto.wrongbook.WrongPracticeMode;
import com.mathmate.dto.wrongbook.WrongPracticeRequest;
import com.mathmate.dto.wrongbook.WrongQuestionView;
import com.mathmate.entity.WrongQuestion;
import com.mathmate.exception.BusinessException;
import com.mathmate.mapper.WrongQuestionMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class WrongBookService {
    private static final Pattern NUMBER_PATTERN = Pattern.compile("\\d+");

    private final WrongQuestionMapper mapper;
    private final QuestionGeneratorService generatorService;
    private final PracticeService practiceService;

    public WrongBookService(
            WrongQuestionMapper mapper,
            QuestionGeneratorService generatorService,
            PracticeService practiceService
    ) {
        this.mapper = mapper;
        this.generatorService = generatorService;
        this.practiceService = practiceService;
    }

    public PageResult<WrongQuestionView> list(Long userId, Boolean mastered, int page, int size) {
        int safePage = Math.max(page, 1);
        int safeSize = Math.min(Math.max(size, 1), 50);
        long total = mapper.countPage(userId, mastered);
        List<WrongQuestionView> items = mapper.findPage(
                        userId, mastered, (safePage - 1) * safeSize, safeSize
                ).stream()
                .map(this::toView)
                .toList();
        return new PageResult<>(items, total, safePage, safeSize);
    }

    @Transactional
    public WrongQuestionView setMastered(Long userId, Long id, boolean mastered) {
        int updated = mapper.updateMastered(id, userId, mastered);
        if (updated == 0) {
            throw new BusinessException("错题不存在", HttpStatus.NOT_FOUND);
        }
        WrongQuestion item = mapper.findByIdAndUser(id, userId);
        if (item == null) {
            throw new BusinessException("错题不存在", HttpStatus.NOT_FOUND);
        }
        return toView(item);
    }

    @Transactional
    public SessionDetail createPractice(Long userId, WrongPracticeRequest request) {
        List<Long> ids = request.wrongQuestionIds() == null
                ? List.of()
                : request.wrongQuestionIds().stream().filter(id -> id != null).distinct().toList();
        List<WrongQuestion> seeds;
        if (ids.isEmpty()) {
            seeds = mapper.findActiveForPractice(userId);
        } else {
            List<WrongQuestion> found = mapper.findByIdsAndUser(userId, ids);
            if (found.size() != ids.size()) {
                throw new BusinessException("部分错题不存在或不属于当前账号", HttpStatus.NOT_FOUND);
            }
            Map<Long, WrongQuestion> byId = new LinkedHashMap<>();
            found.forEach(item -> byId.put(item.getId(), item));
            seeds = ids.stream().map(byId::get).toList();
        }

        if (seeds.isEmpty()) {
            throw new BusinessException("当前没有可用于练习的错题");
        }

        return request.mode() == WrongPracticeMode.ORIGINAL
                ? createOriginalPractice(userId, seeds)
                : createSimilarPractice(userId, seeds, request.count() == null ? 10 : request.count());
    }

    private SessionDetail createOriginalPractice(Long userId, List<WrongQuestion> seeds) {
        List<GeneratedQuestion> generated = seeds.stream()
                .map(item -> new GeneratedQuestion(
                        item.getExpression(),
                        item.getCorrectAnswer(),
                        parseType(item.getQuestionType()),
                        item.getCanonicalKey()
                ))
                .toList();
        List<QuestionType> types = generated.stream().map(GeneratedQuestion::type).distinct().toList();
        return practiceService.createCustomSession(
                userId,
                "错题重做",
                types,
                maxOperandCount(seeds),
                generated
        );
    }

    private SessionDetail createSimilarPractice(Long userId, List<WrongQuestion> seeds, int count) {
        Map<QuestionType, Integer> sourceCounts = new LinkedHashMap<>();
        Map<QuestionType, Integer> operandCounts = new LinkedHashMap<>();
        for (WrongQuestion item : seeds) {
            QuestionType type = parseType(item.getQuestionType());
            sourceCounts.merge(type, 1, Integer::sum);
            operandCounts.merge(type, countNumbers(item.getExpression()), Math::max);
        }

        Map<QuestionType, Integer> quotas = allocateCounts(sourceCounts, count);
        Set<String> excludedKeys = new HashSet<>(mapper.findAllCanonicalKeys(userId));
        List<GeneratedQuestion> generated = new ArrayList<>();

        for (Map.Entry<QuestionType, Integer> entry : quotas.entrySet()) {
            QuestionType type = entry.getKey();
            int quota = entry.getValue();
            PracticeConfig config = new PracticeConfig(
                    "错题同类练习",
                    quota,
                    List.of(type),
                    Math.min(6, Math.max(2, operandCounts.getOrDefault(type, 3))),
                    true,
                    100,
                    20
            );
            List<GeneratedQuestion> batch = generatorService.generate(config, excludedKeys);
            generated.addAll(batch);
            batch.forEach(item -> excludedKeys.add(item.canonicalKey()));
        }

        List<QuestionType> types = generated.stream().map(GeneratedQuestion::type).distinct().toList();
        return practiceService.createCustomSession(
                userId,
                "错题同类练习",
                types,
                maxOperandCount(seeds),
                generated
        );
    }

    private Map<QuestionType, Integer> allocateCounts(
            Map<QuestionType, Integer> sourceCounts,
            int total
    ) {
        Map<QuestionType, Integer> quotas = new LinkedHashMap<>();
        List<Map.Entry<QuestionType, Integer>> ranked = sourceCounts.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getValue(), a.getValue()))
                .toList();

        if (total <= ranked.size()) {
            ranked.stream().limit(total).forEach(entry -> quotas.put(entry.getKey(), 1));
            return quotas;
        }

        ranked.forEach(entry -> quotas.put(entry.getKey(), 1));
        int remaining = total - ranked.size();
        int totalWeight = ranked.stream().mapToInt(Map.Entry::getValue).sum();
        List<Map.Entry<QuestionType, Double>> fractions = new ArrayList<>();
        int used = 0;
        for (Map.Entry<QuestionType, Integer> entry : ranked) {
            double exact = (double) remaining * entry.getValue() / totalWeight;
            int add = (int) Math.floor(exact);
            quotas.merge(entry.getKey(), add, Integer::sum);
            used += add;
            fractions.add(Map.entry(entry.getKey(), exact - add));
        }
        fractions.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        for (int index = 0; index < remaining - used; index++) {
            quotas.merge(fractions.get(index % fractions.size()).getKey(), 1, Integer::sum);
        }
        return quotas;
    }

    private int maxOperandCount(List<WrongQuestion> seeds) {
        return seeds.stream()
                .mapToInt(item -> countNumbers(item.getExpression()))
                .max()
                .orElse(3);
    }

    private int countNumbers(String expression) {
        long count = NUMBER_PATTERN.matcher(expression).results().count();
        return (int) Math.min(6, Math.max(2, count));
    }

    private QuestionType parseType(String value) {
        try {
            return QuestionType.valueOf(value);
        } catch (IllegalArgumentException | NullPointerException exception) {
            return QuestionType.MIXED_OPERATIONS;
        }
    }

    private WrongQuestionView toView(WrongQuestion item) {
        return new WrongQuestionView(
                item.getId(),
                item.getExpression(),
                parseType(item.getQuestionType()),
                item.getCorrectAnswer(),
                item.getLastWrongAnswer(),
                item.getWrongCount(),
                item.getCorrectStreak(),
                Boolean.TRUE.equals(item.getMastered()),
                item.getLastPracticedAt()
        );
    }
}
