CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(100) NOT NULL,
    display_name VARCHAR(50) NOT NULL,
    avatar_url VARCHAR(500) NULL,
    avatar_object_key VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS practice_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(120) NOT NULL,
    config_json TEXT NOT NULL,
    total_count INT NOT NULL,
    correct_count INT NOT NULL DEFAULT 0,
    accuracy DECIMAL(5,2) NOT NULL DEFAULT 0,
    duration_seconds INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at DATETIME NOT NULL,
    submitted_at DATETIME NULL,
    CONSTRAINT fk_session_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_session_user_time (user_id, started_at),
    INDEX idx_session_user_status (user_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS practice_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL,
    sort_order INT NOT NULL,
    expression VARCHAR(255) NOT NULL,
    correct_answer INT NOT NULL,
    question_type VARCHAR(40) NOT NULL,
    canonical_key VARCHAR(255) NOT NULL,
    user_answer INT NULL,
    is_correct BOOLEAN NULL,
    CONSTRAINT fk_question_session FOREIGN KEY (session_id) REFERENCES practice_sessions(id) ON DELETE CASCADE,
    INDEX idx_question_session_order (session_id, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS wrong_questions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    canonical_key VARCHAR(255) NOT NULL,
    expression VARCHAR(255) NOT NULL,
    question_type VARCHAR(40) NOT NULL,
    correct_answer INT NOT NULL,
    last_wrong_answer INT NULL,
    wrong_count INT NOT NULL DEFAULT 1,
    correct_streak INT NOT NULL DEFAULT 0,
    mastered BOOLEAN NOT NULL DEFAULT FALSE,
    last_practiced_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_wrong_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    UNIQUE KEY uk_wrong_user_key (user_id, canonical_key),
    INDEX idx_wrong_user_mastered (user_id, mastered, last_practiced_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS weekly_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    week_start DATE NOT NULL,
    week_end DATE NOT NULL,
    metrics_json LONGTEXT NOT NULL,
    narrative_json LONGTEXT NOT NULL,
    narrative_source VARCHAR(20) NOT NULL,
    agent_model VARCHAR(80) NULL,
    recommendation_json LONGTEXT NOT NULL,
    generated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME NULL,
    UNIQUE KEY uk_weekly_report_user_week (user_id, week_start),
    CONSTRAINT fk_weekly_report_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS agent_runs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    report_id BIGINT NULL,
    provider VARCHAR(40) NOT NULL,
    model VARCHAR(80) NULL,
    status VARCHAR(20) NOT NULL,
    latency_ms INT NULL,
    moderation_status VARCHAR(20) NULL,
    error_message VARCHAR(500) NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_agent_run_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_agent_run_report FOREIGN KEY (report_id) REFERENCES weekly_reports(id) ON DELETE SET NULL,
    INDEX idx_agent_run_user_time (user_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
