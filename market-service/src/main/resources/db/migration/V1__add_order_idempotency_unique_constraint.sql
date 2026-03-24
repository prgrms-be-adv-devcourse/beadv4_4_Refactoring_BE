ALTER TABLE market_orders
    MODIFY COLUMN buyer_id BIGINT NOT NULL;

-- idempotency_key 없을 때만 추가
SET @add_idempotency_key = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'market_orders'
              AND COLUMN_NAME = 'idempotency_key'
        ),
        'SELECT 1',
        'ALTER TABLE market_orders ADD COLUMN idempotency_key VARCHAR(100) NULL'
    )
);

PREPARE stmt FROM @add_idempotency_key;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 복합 유니크 없을때만 추가
SET @add_unique_constraint = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.STATISTICS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'market_orders'
              AND INDEX_NAME = 'uk_orders_buyer_id_idempotency_key'
        ),
        'SELECT 1',
        'ALTER TABLE market_orders ADD CONSTRAINT uk_orders_buyer_id_idempotency_key UNIQUE (buyer_id, idempotency_key)'
    )
);

PREPARE stmt FROM @add_unique_constraint;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
