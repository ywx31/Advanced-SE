CREATE DATABASE IF NOT EXISTS mathmate CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'mathmate'@'localhost' IDENTIFIED BY 'mathmate';
GRANT ALL PRIVILEGES ON mathmate.* TO 'mathmate'@'localhost';
FLUSH PRIVILEGES;
