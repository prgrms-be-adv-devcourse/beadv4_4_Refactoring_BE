CREATE TABLE products
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    created_at      DATETIME(6) DEFAULT NULL,
    updated_at      DATETIME(6) DEFAULT NULL,
    category        ENUM('KEYBOARD') DEFAULT NULL,
    description     LONGTEXT,
    detail          JSON                  DEFAULT NULL,
    image_url       VARCHAR(255)          DEFAULT NULL,
    name            VARCHAR(255) NOT NULL,
    price           BIGINT                DEFAULT NULL,
    reserved_stock  INT          NOT NULL DEFAULT 0,
    sale_ended_at   DATETIME(6) DEFAULT NULL,
    sale_price      BIGINT                DEFAULT NULL,
    sale_started_at DATETIME(6) DEFAULT NULL,
    seller_id       BIGINT       NOT NULL,
    state           ENUM('ON_SALE','SOLD_OUT','STOPPED') DEFAULT NULL,
    stock           INT                   DEFAULT NULL,
    view_count      BIGINT                DEFAULT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE inbox_event
(
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    consumer_group  VARCHAR(100) NOT NULL,
    created_at      DATETIME(6) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    topic           VARCHAR(100) NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_inbox_idempotency_key_consumer_group (idempotency_key, consumer_group),
    KEY             idx_inbox_topic_consumer_group (topic, consumer_group),
    KEY             idx_inbox_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;