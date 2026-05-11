CREATE DATABASE IF NOT EXISTS weather_analysis;
USE weather_analysis;

-- 用户表（认证用）
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(200) NOT NULL,
    nickname VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户关注城市表
CREATE TABLE IF NOT EXISTS followed_city (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    city VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_city (user_id, city)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 天气数据表
CREATE TABLE IF NOT EXISTS weather_data (
    id INT PRIMARY KEY AUTO_INCREMENT,
    city VARCHAR(50),
    obs_time DATETIME,
    temp FLOAT,
    feels_like FLOAT,
    humidity FLOAT,
    wind_speed FLOAT,
    pressure FLOAT,
    visibility FLOAT,
    weather_text VARCHAR(50),
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 默认管理员账号：admin / admin123
INSERT IGNORE INTO user (username, password, nickname) VALUES ('admin', MD5('admin123'), '管理员');
