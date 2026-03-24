CREATE TABLE product_outbox_event
(
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    topic      VARCHAR(100) NOT NULL,
    event_type VARCHAR(200) NOT NULL,
    event_key  VARCHAR(100) NOT NULL,
    payload    LONGTEXT     NOT NULL,
    status     VARCHAR(20)  NOT NULL,
    created_at DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    KEY idx_product_outbox_status_created (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;