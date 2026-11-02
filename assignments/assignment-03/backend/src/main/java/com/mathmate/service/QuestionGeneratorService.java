package com.mathmate.service;

import com.mathmate.dto.practice.GeneratedQuestion;
import com.mathmate.dto.practice.PracticeConfig;
import com.mathmate.dto.practice.QuestionType;
import com.mathmate.exception.BusinessException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class QuestionGeneratorService {
    private static final int RESULT_MIN = 0;
    private static final int RESULT_MAX = 100;

    public List<GeneratedQuestion> generate(PracticeConfig config) {
        return generate(config, Set.of());
    }

    public List<GeneratedQuestion> generate(PracticeConfig config, Set<String> excludedCanonicalKeys) {
        if (config.count() <= 0) {
            throw new BusinessException("题目数量必须大于 0");
        }
        List<QuestionType> types = config.types().stream().distinct().toList();
        if (types.isEmpty()) {
            throw new BusinessException("至少选择一种题型");
        }
        if (types.contains(QuestionType.ADVANCED_MULTIPLICATION) && config.maxFactor() <= 9) {
            throw new BusinessException("表外乘法要求乘法因子上限大于 9");
        }
        if (types.contains(QuestionType.ADVANCED_DIVISION) && config.maxFactor() <= 9 && config.maxNumber() <= 81) {
            throw new BusinessException("当前范围无法生成表外除法，请提高因子上限或数值上限");
        }

        List<GeneratedQuestion> questions = new ArrayList<>();
        Set<String> usedKeys = new HashSet<>(excludedCanonicalKeys);
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int attempts = 0;
        int maxAttempts = Math.max(10_000, config.count() * 500);

        while (questions.size() < config.count() && attempts < maxAttempts) {
            attempts++;
            QuestionType type = types.get(random.nextInt(types.size()));
            GeneratedQuestion candidate = switch (type) {
                case TABLE_MULTIPLICATION -> tableMultiplication(random);
                case TABLE_DIVISION -> tableDivision(random);
                case ADVANCED_MULTIPLICATION -> advancedMultiplication(random, config);
                case ADVANCED_DIVISION -> advancedDivision(random, config);
                case MIXED_OPERATIONS -> mixedOperations(random, config);
            };
            if (candidate != null && usedKeys.add(candidate.canonicalKey())) {
                questions.add(candidate);
            }
        }

        if (questions.size() != config.count()) {
            throw new BusinessException(
                    "当前题型和范围最多只能生成约 " + questions.size()
                            + " 道不重复题。请增加题型、提高范围，或减少题目数量。"
            );
        }
        return questions;
    }

    private GeneratedQuestion tableMultiplication(ThreadLocalRandom random) {
        int a = random.nextInt(1, 10);
        int b = random.nextInt(1, 10);
        int low = Math.min(a, b);
        int high = Math.max(a, b);
        return new GeneratedQuestion(
                a + " × " + b,
                a * b,
                QuestionType.TABLE_MULTIPLICATION,
                "TM:" + low + ":" + high
        );
    }

    private GeneratedQuestion tableDivision(ThreadLocalRandom random) {
        int divisor = random.nextInt(1, 10);
        int quotient = random.nextInt(1, 10);
        int dividend = divisor * quotient;
        return new GeneratedQuestion(
                dividend + " ÷ " + divisor,
                quotient,
                QuestionType.TABLE_DIVISION,
                "TD:" + dividend + ":" + divisor
        );
    }

    private GeneratedQuestion advancedMultiplication(ThreadLocalRandom random, PracticeConfig config) {
        int a = random.nextInt(2, config.maxFactor() + 1);
        int b = random.nextInt(2, config.maxFactor() + 1);
        if (a <= 9 && b <= 9) {
            return null;
        }
        long answer = (long) a * b;
        if (answer > config.maxNumber() || answer > RESULT_MAX) {
            return null;
        }
        int low = Math.min(a, b);
        int high = Math.max(a, b);
        return new GeneratedQuestion(
                a + " × " + b,
                (int) answer,
                QuestionType.ADVANCED_MULTIPLICATION,
                "AM:" + low + ":" + high
        );
    }

    private GeneratedQuestion advancedDivision(ThreadLocalRandom random, PracticeConfig config) {
        int divisor = random.nextInt(2, config.maxFactor() + 1);
        int quotient = random.nextInt(2, config.maxFactor() + 1);
        long dividend = (long) divisor * quotient;
        boolean outsideTable = divisor > 9 || quotient > 9 || dividend > 81;
        if (!outsideTable || dividend > config.maxNumber() || quotient > RESULT_MAX) {
            return null;
        }
        return new GeneratedQuestion(
                dividend + " ÷ " + divisor,
                quotient,
                QuestionType.ADVANCED_DIVISION,
                "AD:" + dividend + ":" + divisor
        );
    }

    private GeneratedQuestion mixedOperations(ThreadLocalRandom random, PracticeConfig config) {
        int operandCount = Math.min(6, Math.max(2, config.operandCount()));
        int firstResult = random.nextInt(RESULT_MIN, RESULT_MAX + 1);
        Step first = generateStep(firstResult, config.maxNumber(), random);
        if (first == null) {
            return null;
        }

        Node root = new Binary(first.op(), new Leaf(first.left()), new Leaf(first.right()));
        int current = firstResult;
        for (int index = 2; index < operandCount; index++) {
            boolean appended = false;
            List<Character> operations = new ArrayList<>(List.of('+', '-', '*', '/'));
            java.util.Collections.shuffle(operations);
            for (char op : operations) {
                List<Integer> candidates = validOperands(op, current, config.maxNumber());
                if (candidates.isEmpty()) {
                    continue;
                }
                if ((op == '*' || op == '/') && candidates.size() == 1 && candidates.get(0) == 1) {
                    continue;
                }
                int operand = candidates.get(random.nextInt(candidates.size()));
                Integer answer = applyOperation(current, op, operand);
                if (answer == null) {
                    continue;
                }
                Node candidateRoot = new Binary(op, root, new Leaf(operand));
                String candidateExpression = renderExpression(candidateRoot, null, false);
                if (!config.allowParentheses() && candidateExpression.contains("(")) {
                    continue;
                }
                root = candidateRoot;
                current = answer;
                appended = true;
                break;
            }
            if (!appended) {
                return null;
            }
        }

        String expression = renderExpression(root, null, false);
        if (!config.allowParentheses() && expression.contains("(")) {
            return null;
        }
        return new GeneratedQuestion(
                expression,
                current,
                QuestionType.MIXED_OPERATIONS,
                "MX:" + canonicalExpression(root)
        );
    }

    private List<Integer> validOperands(char op, int current, int maxNumber) {
        int upper = switch (op) {
            case '+' -> Math.min(maxNumber, RESULT_MAX - current);
            case '-' -> Math.min(maxNumber, current);
            case '*' -> current == 0 ? maxNumber : Math.min(maxNumber, RESULT_MAX / current);
            case '/' -> current == 0 ? maxNumber : current;
            default -> 0;
        };
        if (upper < 1) {
            return List.of();
        }
        List<Integer> candidates = new ArrayList<>();
        for (int operand = 1; operand <= upper; operand++) {
            if (op != '/' || current % operand == 0) {
                // 优先避免无意义的 ×1、÷1，但保留唯一可行解。
                if ((op == '*' || op == '/') && operand == 1 && upper > 1) {
                    continue;
                }
                candidates.add(operand);
            }
        }
        if (candidates.isEmpty() && upper >= 1) {
            candidates.add(1);
        }
        return candidates;
    }

    private String renderExpression(Node node, Character parentOp, boolean isRight) {
        if (node instanceof Leaf leaf) {
            return String.valueOf(leaf.value());
        }
        Binary binary = (Binary) node;
        String text = renderExpression(binary.left(), binary.op(), false)
                + " " + symbol(binary.op()) + " "
                + renderExpression(binary.right(), binary.op(), true);
        if (parentOp != null) {
            int ownPrecedence = precedence(binary.op());
            int parentPrecedence = precedence(parentOp);
            boolean needParentheses = ownPrecedence < parentPrecedence;
            if (ownPrecedence == parentPrecedence && isRight) {
                needParentheses = parentOp == '-' || parentOp == '/' || binary.op() != parentOp;
            }
            if (needParentheses) {
                text = "(" + text + ")";
            }
        }
        return text;
    }

    private String canonicalExpression(Node node) {
        if (node instanceof Leaf leaf) {
            return String.valueOf(leaf.value());
        }
        Binary binary = (Binary) node;
        if (binary.op() == '+' || binary.op() == '*') {
            List<String> parts = new ArrayList<>();
            flattenSameOperation(binary, binary.op(), parts);
            parts.sort(String::compareTo);
            return binary.op() + "(" + String.join(",", parts) + ")";
        }
        return binary.op() + "(" + canonicalExpression(binary.left())
                + "," + canonicalExpression(binary.right()) + ")";
    }

    private void flattenSameOperation(Node node, char op, List<String> parts) {
        if (node instanceof Binary binary && binary.op() == op) {
            flattenSameOperation(binary.left(), op, parts);
            flattenSameOperation(binary.right(), op, parts);
        } else {
            parts.add(canonicalExpression(node));
        }
    }

    private Step generateStep(int target, int maxNumber, ThreadLocalRandom random) {
        List<Character> operations = new ArrayList<>(List.of('+', '-', '*', '/'));
        java.util.Collections.shuffle(operations);
        for (char op : operations) {
            Step step = switch (op) {
                case '+' -> {
                    if (target < 2) {
                        yield null;
                    }
                    int left = random.nextInt(1, target);
                    yield new Step(op, left, target - left);
                }
                case '-' -> {
                    if (target >= maxNumber) {
                        yield null;
                    }
                    int right = random.nextInt(1, maxNumber - target + 1);
                    yield new Step(op, target + right, right);
                }
                case '*' -> {
                    if (target < 2) {
                        yield null;
                    }
                    List<int[]> factors = factors(target, maxNumber).stream()
                            .filter(pair -> pair[0] > 1 && pair[1] > 1)
                            .toList();
                    if (factors.isEmpty()) {
                        yield null;
                    }
                    int[] pair = factors.get(random.nextInt(factors.size()));
                    yield new Step(op, pair[0], pair[1]);
                }
                case '/' -> {
                    if (target < 1) {
                        yield null;
                    }
                    int maxRight = Math.min(maxNumber, maxNumber / target);
                    if (maxRight < 2) {
                        yield null;
                    }
                    int right = random.nextInt(2, maxRight + 1);
                    yield new Step(op, target * right, right);
                }
                default -> null;
            };
            if (step != null) {
                return step;
            }
        }
        return null;
    }

    private List<int[]> factors(int value, int maxNumber) {
        List<int[]> pairs = new ArrayList<>();
        for (int left = 1; left <= Math.sqrt(value); left++) {
            if (value % left != 0) {
                continue;
            }
            int right = value / left;
            if (right <= maxNumber) {
                pairs.add(new int[]{left, right});
            }
        }
        return pairs;
    }

    private Integer applyOperation(int left, char op, int right) {
        Long result = switch (op) {
            case '+' -> (long) left + right;
            case '-' -> (long) left - right;
            case '*' -> (long) left * right;
            case '/' -> right != 0 && left % right == 0 ? (long) left / right : null;
            default -> null;
        };
        if (result == null || result < RESULT_MIN || result > RESULT_MAX) {
            return null;
        }
        return result.intValue();
    }

    private int precedence(char op) {
        return op == '+' || op == '-' ? 1 : 2;
    }

    private String symbol(char op) {
        return switch (op) {
            case '+' -> "+";
            case '-' -> "−";
            case '*' -> "×";
            case '/' -> "÷";
            default -> throw new IllegalArgumentException("未知运算符");
        };
    }

    private sealed interface Node permits Leaf, Binary {
    }

    private record Leaf(int value) implements Node {
    }

    private record Binary(char op, Node left, Node right) implements Node {
    }

    private record Step(char op, int left, int right) {
    }
}
