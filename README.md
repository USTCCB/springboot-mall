# 🏪 Spring Boot Mall

> Spring Boot 商城后端学习项目 · 展示 Java 后端核心工程能力

## 简介

一个完整的 Spring Boot 商城后端 Demo，包含商品管理、订单、用户、Redis 缓存、分布式锁、全局异常处理等典型场景。**用于系统展示 Java 后端工程能力，非生产可用**。

## 技术栈

- Java 17 + Spring Boot 3.2.5
- MyBatis 3.0.3 + H2
- Spring Data Redis（限流 / 缓存）
- Lombok / Validation / JUnit5
- Maven 构建

## 核心亮点

- ✅ RESTful API 设计与分层架构
- ✅ MyBatis XML 映射（生产环境主流写法）
- ✅ Redis 分布式限流（防刷）
- ✅ 条件更新防超卖
- ✅ @Transactional 事务管理
- ✅ @ControllerAdvice 全局异常处理
- ✅ 单元测试

## 快速开始

`ash
mvn spring-boot:run
# 监听 8082
# H2 控制台: http://localhost:8082/h2-console
`

## API 速览

| 方法 | 路径 | 说明 |
| --- | --- | --- |
| GET  | /api/goods | 商品列表 |
| GET  | /api/goods/{id} | 商品详情 |
| POST | /api/orders | 下单 |
| POST | /api/orders/{id}/pay | 支付 |

## 示例

`ash
# 下单
curl -X POST http://localhost:8082/api/orders \
  -H "Content-Type: application/json" \
  -d "{\"userId\":1,\"goodsId\":1,\"quantity\":1}"
`

## 目录结构

`
src/main/java/com/ustccb/mall/
├── MallApplication.java
├── controller/
├── service/
├── mapper/
├── entity/
└── exception/     # BizException + GlobalExceptionHandler
`

## 这个项目展示了哪些能力

- MyBatis 在 Spring Boot 中的完整配置（XML 映射、typeAliases、mapper scan）
- Redis 在 Java 后端的常见用法（限流、缓存）
- 全局异常处理（自定义业务异常 + 通用兜底）
- 事务边界设计（下单 = 扣库存 + 落单 + 清缓存）
- 测试驱动（@SpringBootTest + 真实 Service 注入）

## 后续可扩展

- [ ] Spring Security + JWT
- [ ] 分页插件 PageHelper
- [ ] RabbitMQ 异步下单
- [ ] ELK 日志收集
- [ ] Docker Compose
