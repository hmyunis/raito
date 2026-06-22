CREATE TABLE IF NOT EXISTS telegram_task_operations (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    telegram_account_id BIGINT UNSIGNED NULL,
    source_remote_panel_id BIGINT UNSIGNED NULL,
    operation_type ENUM('create_task', 'set_task_completion') NOT NULL,
    target_bucket_client_id INT UNSIGNED NOT NULL,
    target_task_client_id INT UNSIGNED NULL,
    task_name VARCHAR(240) NULL,
    desired_completion TINYINT(1) NULL,
    status ENUM('pending', 'applied', 'failed', 'ignored') NOT NULL DEFAULT 'pending',
    client_created_task_id INT UNSIGNED NULL,
    error_message VARCHAR(500) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    applied_at TIMESTAMP NULL DEFAULT NULL,
    failed_at TIMESTAMP NULL DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_telegram_task_operations_public_id (public_id),
    UNIQUE KEY uq_telegram_task_operations_source_panel (source_remote_panel_id),
    KEY idx_telegram_task_operations_user_status (user_id, status, created_at),
    KEY idx_telegram_task_operations_bucket_task (user_id, target_bucket_client_id, target_task_client_id),

    CONSTRAINT fk_telegram_task_operations_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_telegram_task_operations_telegram_account
        FOREIGN KEY (telegram_account_id)
        REFERENCES telegram_accounts(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_telegram_task_operations_source_panel
        FOREIGN KEY (source_remote_panel_id)
        REFERENCES remote_panels(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
