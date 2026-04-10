SET @add_reserved_stock = (
      SELECT IF(
          EXISTS (
              SELECT 1
              FROM information_schema.COLUMNS
              WHERE TABLE_SCHEMA = DATABASE()
                AND TABLE_NAME = 'products'
                AND COLUMN_NAME = 'reserved_stock'
          ),
          'SELECT 1',
          'ALTER TABLE products ADD COLUMN reserved_stock INT NOT NULL DEFAULT 0'
      )
  );

PREPARE stmt FROM @add_reserved_stock;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;