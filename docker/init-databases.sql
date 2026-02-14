-- Create all databases for microservices
CREATE DATABASE IF NOT EXISTS thock_member_db;
CREATE DATABASE IF NOT EXISTS thock_product_db;
CREATE DATABASE IF NOT EXISTS thock_market_db;
CREATE DATABASE IF NOT EXISTS thock_payment_db;
CREATE DATABASE IF NOT EXISTS thock_settlement_db;

-- Create users for each service
CREATE USER IF NOT EXISTS 'thock_member_db_user'@'%' IDENTIFIED BY 'thock_member_db_password';
CREATE USER IF NOT EXISTS 'thock_product_db_user'@'%' IDENTIFIED BY 'thock_product_db_password';
CREATE USER IF NOT EXISTS 'thock_market_db_user'@'%' IDENTIFIED BY 'thock_market_db_password';
CREATE USER IF NOT EXISTS 'thock_payment_db_user'@'%' IDENTIFIED BY 'thock_payment_db_password';
CREATE USER IF NOT EXISTS 'thock_settlement_db_user'@'%' IDENTIFIED BY 'thock_settlement_db_password';

-- Grant privileges (each user can only access their own database)
GRANT ALL PRIVILEGES ON thock_member_db.* TO 'thock_member_db_user'@'%';
GRANT ALL PRIVILEGES ON thock_product_db.* TO 'thock_product_db_user'@'%';
GRANT ALL PRIVILEGES ON thock_market_db.* TO 'thock_market_db_user'@'%';
GRANT ALL PRIVILEGES ON thock_payment_db.* TO 'thock_payment_db_user'@'%';
GRANT ALL PRIVILEGES ON thock_settlement_db.* TO 'thock_settlement_db_user'@'%';

FLUSH PRIVILEGES;
