CREATE TABLE IF NOT EXISTS app_users (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL,
    display_name VARCHAR(120) NULL,
    device_label VARCHAR(160) NULL,
    device_token_hash CHAR(64) NOT NULL,
    status ENUM('active', 'blocked', 'deleted') NOT NULL DEFAULT 'active',
    last_seen_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_app_users_public_id (public_id),
    UNIQUE KEY uq_app_users_device_token_hash (device_token_hash),
    KEY idx_app_users_status (status),
    KEY idx_app_users_last_seen_at (last_seen_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS telegram_accounts (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    telegram_user_id BIGINT NOT NULL,
    username VARCHAR(120) NULL,
    first_name VARCHAR(120) NULL,
    last_name VARCHAR(120) NULL,
    status ENUM('active', 'unlinked', 'blocked') NOT NULL DEFAULT 'active',
    linked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_telegram_accounts_telegram_user_id (telegram_user_id),
    KEY idx_telegram_accounts_user_id (user_id),
    KEY idx_telegram_accounts_status (status),

    CONSTRAINT fk_telegram_accounts_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS pairing_codes (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    code_hash CHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP NULL DEFAULT NULL,
    used_by_telegram_user_id BIGINT NULL,
    attempt_count INT UNSIGNED NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_pairing_codes_code_hash (code_hash),
    KEY idx_pairing_codes_user_id (user_id),
    KEY idx_pairing_codes_expires_at (expires_at),
    KEY idx_pairing_codes_used_at (used_at),

    CONSTRAINT fk_pairing_codes_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS remote_panels (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    public_id CHAR(36) NOT NULL,
    user_id BIGINT UNSIGNED NOT NULL,
    telegram_account_id BIGINT UNSIGNED NULL,
    source ENUM('telegram') NOT NULL DEFAULT 'telegram',
    source_chat_id BIGINT NULL,
    source_message_id BIGINT NULL,
    content TEXT NOT NULL,
    content_hash CHAR(64) NOT NULL,
    status ENUM('pending', 'imported', 'discarded') NOT NULL DEFAULT 'pending',
    imported_at TIMESTAMP NULL DEFAULT NULL,
    discarded_at TIMESTAMP NULL DEFAULT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_remote_panels_public_id (public_id),
    UNIQUE KEY uq_remote_panels_telegram_message (source_chat_id, source_message_id),
    KEY idx_remote_panels_user_status (user_id, status),
    KEY idx_remote_panels_created_at (created_at),
    KEY idx_remote_panels_content_hash (content_hash),

    CONSTRAINT fk_remote_panels_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_remote_panels_telegram_account
        FOREIGN KEY (telegram_account_id)
        REFERENCES telegram_accounts(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sync_batches (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    public_id CHAR(36) NOT NULL,
    status ENUM('started', 'completed', 'failed') NOT NULL DEFAULT 'started',
    imported_count INT UNSIGNED NOT NULL DEFAULT 0,
    app_version VARCHAR(60) NULL,
    error_message VARCHAR(500) NULL,
    started_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP NULL DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_sync_batches_public_id (public_id),
    KEY idx_sync_batches_user_id (user_id),
    KEY idx_sync_batches_status (status),
    KEY idx_sync_batches_started_at (started_at),

    CONSTRAINT fk_sync_batches_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS usage_events (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NULL,
    event_name VARCHAR(80) NOT NULL,
    event_payload JSON NULL,
    client_time_at TIMESTAMP NULL DEFAULT NULL,
    server_time_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_hash CHAR(64) NULL,
    user_agent_hash CHAR(64) NULL,

    PRIMARY KEY (id),
    KEY idx_usage_events_user_id (user_id),
    KEY idx_usage_events_event_name (event_name),
    KEY idx_usage_events_server_time_at (server_time_at),

    CONSTRAINT fk_usage_events_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bot_update_log (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    update_id BIGINT NOT NULL,
    status ENUM('received', 'processed', 'ignored', 'failed') NOT NULL DEFAULT 'received',
    error_message VARCHAR(500) NULL,
    payload_json JSON NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP NULL DEFAULT NULL,

    PRIMARY KEY (id),
    UNIQUE KEY uq_bot_update_log_update_id (update_id),
    KEY idx_bot_update_log_status (status),
    KEY idx_bot_update_log_received_at (received_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
