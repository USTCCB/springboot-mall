DROP TABLE IF EXISTS user_account;
CREATE TABLE user_account (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) UNIQUE NOT NULL,
    password VARCHAR(128) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
DROP TABLE IF EXISTS goods;
CREATE TABLE goods (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    stock INT NOT NULL DEFAULT 0,
    description VARCHAR(512)
);
DROP TABLE IF EXISTS mall_order;
CREATE TABLE mall_order (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    goods_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status VARCHAR(16) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_order_user ON mall_order(user_id);
CREATE INDEX idx_goods_stock ON goods(stock);
INSERT INTO user_account(username, password) VALUES ('demo', '123456');
INSERT INTO goods(title, price, stock, description) VALUES ('Spring Boot 实战', 89.00, 100, '经典入门书');
INSERT INTO goods(title, price, stock, description) VALUES ('机械键盘', 499.00, 30, '红轴');
INSERT INTO goods(title, price, stock, description) VALUES ('无线鼠标', 129.00, 200, '静音');
