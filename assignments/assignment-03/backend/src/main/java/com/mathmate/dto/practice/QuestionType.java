package com.mathmate.dto.practice;

public enum QuestionType {
    TABLE_MULTIPLICATION("表内乘法"),
    TABLE_DIVISION("表内除法"),
    ADVANCED_MULTIPLICATION("表外乘法"),
    ADVANCED_DIVISION("表外除法"),
    MIXED_OPERATIONS("混合运算");

    private final String label;

    QuestionType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
