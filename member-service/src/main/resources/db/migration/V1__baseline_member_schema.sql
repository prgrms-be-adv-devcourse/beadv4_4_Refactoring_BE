CREATE TABLE `member_members`
(
    `id`             bigint       NOT NULL AUTO_INCREMENT,
    `account_holder` varchar(255) DEFAULT NULL,
    `account_number` varchar(255) DEFAULT NULL,
    `bank_code`      varchar(255) DEFAULT NULL,
    `email`          varchar(255) NOT NULL,
    `name`           varchar(255) DEFAULT NULL,
    `role`           enum('ADMIN','SELLER','USER') NOT NULL,
    `state`          enum('ACTIVE','INACTIVE','WITHDRAWN') NOT NULL,
    `created_at`     datetime(6) DEFAULT NULL,
    `updated_at`     datetime(6) DEFAULT NULL,
    `last_login_at`  datetime(6) DEFAULT NULL,
    `withdrawn_at`   datetime(6) DEFAULT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `UKfdlqci4e0rb89jeh5k39uokkj` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `member_credentials`
(
    `member_id`     bigint       NOT NULL,
    `password_hash` varchar(255) NOT NULL,
    `updated_at`    datetime(6) NOT NULL,
    PRIMARY KEY (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `member_refresh_tokens`
(
    `id`         bigint      NOT NULL AUTO_INCREMENT,
    `expires_at` datetime(6) NOT NULL,
    `member_id`  bigint      NOT NULL,
    `revoked_at` datetime(6) DEFAULT NULL,
    `token_hash` varchar(64) NOT NULL,
    PRIMARY KEY (`id`),
    UNIQUE KEY `idx_refresh_token_hash` (`token_hash`),
    KEY          `idx_refresh_member_id` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE `member_login_histories`
(
    `id`           bigint NOT NULL AUTO_INCREMENT,
    `logged_in_at` datetime(6) NOT NULL,
    `member_id`    bigint NOT NULL,
    `success`      bit(1) NOT NULL,
    PRIMARY KEY (`id`),
    KEY            `idx_login_history_member_id` (`member_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;