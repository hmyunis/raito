CREATE TABLE IF NOT EXISTS synced_buckets (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    user_id BIGINT UNSIGNED NOT NULL,
    client_bucket_id INT UNSIGNED NOT NULL,
    name VARCHAR(160) NOT NULL,
    discipline VARCHAR(80) NULL,
    companion_id VARCHAR(80) NULL,
    aura_ink VARCHAR(32) NULL,
    deadline VARCHAR(80) NULL,
    is_completed TINYINT(1) NOT NULL DEFAULT 0,
    client_timestamp BIGINT UNSIGNED NULL,
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_synced_buckets_user_client_bucket (user_id, client_bucket_id),
    KEY idx_synced_buckets_user_id (user_id),
    KEY idx_synced_buckets_synced_at (synced_at),

    CONSTRAINT fk_synced_buckets_user
        FOREIGN KEY (user_id)
        REFERENCES app_users(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS synced_bucket_tasks (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
    synced_bucket_id BIGINT UNSIGNED NOT NULL,
    client_task_id INT UNSIGNED NOT NULL,
    name VARCHAR(240) NOT NULL,
    time_remaining VARCHAR(40) NULL,
    is_completed TINYINT(1) NOT NULL DEFAULT 0,
    is_overdue TINYINT(1) NOT NULL DEFAULT 0,
    description TEXT NULL,
    due_datetime VARCHAR(80) NULL,
    is_pinned TINYINT(1) NOT NULL DEFAULT 0,
    client_created_at BIGINT UNSIGNED NULL,
    synced_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    UNIQUE KEY uq_synced_bucket_tasks_bucket_client_task (synced_bucket_id, client_task_id),
    KEY idx_synced_bucket_tasks_bucket_id (synced_bucket_id),
    KEY idx_synced_bucket_tasks_sort (synced_bucket_id, is_pinned, is_completed, client_created_at),

    CONSTRAINT fk_synced_bucket_tasks_bucket
        FOREIGN KEY (synced_bucket_id)
        REFERENCES synced_buckets(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
