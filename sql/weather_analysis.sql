CREATE DATABASE IF NOT EXISTS weather_analysis;
USE weather_analysis;

-- 用户表（认证用）
CREATE TABLE IF NOT EXISTS user (
    id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(200) NOT NULL,
    nickname VARCHAR(50),
    permission INT DEFAULT 1 COMMENT '权限: 1=普通用户, 2=管理员',
    status INT DEFAULT 1 COMMENT '状态: 0=禁用, 1=正常',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户关注城市表
CREATE TABLE IF NOT EXISTS followed_city (
    id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    city VARCHAR(50) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_city (user_id, city),
    INDEX idx_user_id (user_id)
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

-- 天气预报数据表
CREATE TABLE IF NOT EXISTS weather_forecast (
    id INT PRIMARY KEY AUTO_INCREMENT,
    city VARCHAR(50) NOT NULL,
    forecast_date DATE NOT NULL,
    temp_max FLOAT,
    temp_min FLOAT,
    weather_text_day VARCHAR(50),
    weather_text_night VARCHAR(50),
    humidity FLOAT,
    wind_speed FLOAT,
    create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_city_date (city, forecast_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 城市ID对照表（和风天气城市代码）
CREATE TABLE IF NOT EXISTS city_list (
    id INT PRIMARY KEY AUTO_INCREMENT,
    city_name VARCHAR(50) NOT NULL UNIQUE,
    city_code VARCHAR(20) NOT NULL,
    province VARCHAR(50)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 常用城市ID数据
/*INSERT IGNORE INTO city_list (city_name, city_code, province) VALUES
('北京', '101010100', '北京'),
('上海', '101020100', '上海'),
('广州', '101280101', '广东'),
('深圳', '101280601', '广东'),
('成都', '101270101', '四川'),
('杭州', '101210101', '浙江'),
('武汉', '101200101', '湖北'),
('西安', '101110101', '陕西'),
('南京', '101190101', '江苏'),
('重庆', '101040100', '重庆'),
('天津', '101030100', '天津'),
('苏州', '101190401', '江苏'),
('长沙', '101250101', '湖南'),
('郑州', '101180101', '河南'),
('济南', '101120101', '山东'),
('青岛', '101120201', '山东'),
('大连', '101070201', '辽宁'),
('厦门', '101230201', '福建'),
('福州', '101230101', '福建'),
('昆明', '101290101', '云南'),
('合肥', '101220101', '安徽'),
('哈尔滨', '101050101', '黑龙江'),
('沈阳', '101070101', '辽宁'),
('长春', '101060101', '吉林'),
('石家庄', '101090101', '河北'),
('太原', '101100101', '山西'),
('南昌', '101240101', '江西'),
('景德镇', '101240801', '江西'),
('贵阳', '101260101', '贵州'),
('南宁', '101300101', '广西'),
('海口', '101310101', '海南'),
('兰州', '101160101', '甘肃'),
('乌鲁木齐', '101130101', '新疆'),
('呼和浩特', '101080101', '内蒙古'),
('拉萨', '101140101', '西藏'),
('银川', '101170101', '宁夏'),
('西宁', '101150101', '青海');*/

-- 默认管理员账号：admin / admin123
INSERT IGNORE INTO user (username, password, nickname, permission, status) VALUES ('admin', MD5('admin123'), '管理员', 2, 1);
