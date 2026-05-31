ALTER TABLE usage_events
    ADD COLUMN client_event_id VARCHAR(80) NULL AFTER event_name,
    ADD UNIQUE KEY uq_usage_events_user_client_event (user_id, client_event_id);
