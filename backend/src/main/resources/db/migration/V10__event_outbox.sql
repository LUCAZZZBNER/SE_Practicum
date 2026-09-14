CREATE TABLE event_outbox (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    event_id CHAR(36) NOT NULL,
    topic VARCHAR(150) NOT NULL,
    aggregate_type VARCHAR(80) NOT NULL,
    aggregate_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(120) NOT NULL,
    schema_version INT NOT NULL DEFAULT 1,
    payload JSON NOT NULL,
    occurred_at TIMESTAMP(6) NOT NULL,
    published_at TIMESTAMP(6) NULL,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(1000) NULL,
    UNIQUE KEY uk_event_outbox_event_id (event_id),
    KEY idx_event_outbox_pending (published_at, id),
    KEY idx_event_outbox_aggregate (aggregate_type, aggregate_id, id)
);
