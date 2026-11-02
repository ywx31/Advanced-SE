-- 为已有错题本补齐题型标签。
ALTER TABLE wrong_questions
    ADD COLUMN question_type VARCHAR(40) NULL AFTER expression;

UPDATE wrong_questions w
JOIN (
    SELECT canonical_key, MAX(question_type) AS question_type
    FROM practice_questions
    GROUP BY canonical_key
) q ON q.canonical_key = w.canonical_key
SET w.question_type = q.question_type
WHERE w.question_type IS NULL;

UPDATE wrong_questions
SET question_type = 'MIXED_OPERATIONS'
WHERE question_type IS NULL;

ALTER TABLE wrong_questions
    MODIFY COLUMN question_type VARCHAR(40) NOT NULL;
