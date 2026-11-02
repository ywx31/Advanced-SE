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
