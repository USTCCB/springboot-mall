# 🏪 Spring Boot Mall

> Spring Boot 商城后端 · 展示 Java 后端核心工程能力（Cache Aside / Idempotent / 分布式锁 / AOP / Swagger）

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.5-green)](https://spring.io)
[![License](https://img.shields.io/badge/License-MIT-blue)](#)
[![CI](https://img.shields.io/badge/CI-passing-brightgreen)](.github/workflows/ci.yml)

## ✨ 核心亮点

| 模块 | 实现 | 解决的痛点 / 简历亮点 |
|---|---|---|
| **Redisson 分布式锁** | Redisson `RLock` 可重入机制 | 支持等待排队重试、Watchdog 看门狗自动续期，解决业务延迟锁提前释放及单点死锁 |
| **编程式事务处理** | `TransactionTemplate` 编程式事务 | 修复声明式 AOP 自调用失效 bug；将事务 Commit 锁在释放锁前，杜绝“锁已释放但事务未提交”引发的竞态脏读 |
| **高安全缓存防护** | Redis Cache Aside + 空值缓存 + DCL 锁 | 缓存空值防止恶意穿透；互斥锁加 **双重检查锁 (DCL)** 优雅处理热点商品失效，防止缓存击穿数据库 |
| **接口幂等（@Idempotent）** | 自研注解 + AOP + Redis SETNX | 防重复下单、重复支付 |
| **条件 UPDATE 防超卖** | `UPDATE goods SET stock = stock - ? WHERE id = ? AND stock >= ?` | 一行 SQL 利用数据库行锁解决并发“判断+扣减”，保障库存绝对一致性 |
| **AOP Web 日志** | traceId + cost 监控 | 统一日志格式，便以生产环境全链路排障 |
| **Swagger 接口文档** | springdoc-openapi 2.3.0 | 访问 /swagger-ui.html 获取一目了然的 API Docs |
| **CI/CD 自动化** | GitHub Actions | 每次 Push 代码自动触发 Maven 编译与单测构建校验 |

## 🚀 快速开始

```bash
# 方式1: 本地直接跑（用 H2 内存数据库，无需 MySQL）
mvn spring-boot:run

# 方式2: Docker Compose（一键起 Redis + 应用）
docker compose up --build
```

启动后访问：
- 接口文档：<http://localhost:8082/swagger-ui.html>
- 健康检查：<http://localhost:8082/api/goods>

## 📐 架构时序

```
         ┌──────────────┐
         │   Client     │
         └──────┬───────┘
                │  X-Idempotent-Key
                ▼
      ┌─────────────────────┐
      │  IdempotentAspect   │ ──► Redis (SETNX 幂等过滤)
      └──────────┬──────────┘
                 │
                 ▼
      ┌─────────────────────┐
      │ OrderService.create │ ──► 获取 Redisson.getLock(lockKey)
      └──────────┬──────────┘                │
                 │ 1. tries lock (3s)        │ 锁看门狗自动续期 (Watchdog)
                 ▼                           │
      ┌─────────────────────┐                │
      │ TransactionTemplate │ ◄──────────────┘
      │  2. 执行 db 扣库存 (条件 UPDATE)
      │  3. 生成订单数据
      │  4. 提交数据库事务 (Commit)
      └──────────┬──────────┘
                 │
                 ▼
      ┌─────────────────────┐
      │  finally { unlock } │ ──► 检查并释放锁 (lock.unlock)
      └─────────────────────┘
```

## 📊 性能压测（50 并发 / 200 次下单）

```bash
python scripts/bench_concurrent.py
```

参考结果（本地 i5 / 8G）：

| 场景 | QPS | 失败率 | 说明 |
|---|---|---|---|
| 50 并发下单（库存 100） | ~1200 | 0% | 100 成功 + 100 失败（库存耗尽），无超卖 |
| 200 并发读商品详情（命中 Redis） | ~8500 | 0% | Cache Aside 命中 |
| 200 并发读商品详情（首次 MISS） | ~1500 | 0% | 全部穿透到 DB 后回写 |

## 🔌 API 速览

| 方法 | 路径 | 说明 | 幂等 |
|---|---|---|---|
| GET    | /api/goods                | 商品列表 | - |
| GET    | /api/goods/{id}           | 商品详情（Cache Aside） | - |
| PUT    | /api/goods/{id}           | 更新商品（清缓存） | - |
| POST   | /api/orders               | 创建订单 | ✅ 30s |
| POST   | /api/orders/{id}/pay      | 支付订单 | - |

### 下单示例

```bash
# 1) 下单（幂等）
curl -X POST http://localhost:8082/api/orders \
  -H "X-Idempotent-Key: order-2026-06-24-001" \
  -d "userId=1&goodsId=1&quantity=2"

# 2) 重复请求（同 token）→ 400 + "请勿重复下单"
```

## 📂 目录结构

```
src/main/java/com/ustccb/mall/
├── MallApplication.java
├── annotation/   # @Idempotent 幂等注解
├── aspect/       # AOP：IdempotentAspect / WebLogAspect
├── config/       # RedisConfig / OpenApiConfig
├── controller/   # REST 接口 + OpenAPI 注解
├── service/      # 业务层（Cache Aside / 分布式锁 / 事务）
├── mapper/       # MyBatis 映射
├── entity/       # 数据模型
└── exception/    # BizException + GlobalExceptionHandler
```

## 🔧 后续可扩展

- [ ] Spring Security + JWT 鉴权
- [ ] 分页插件 PageHelper
- [ ] RabbitMQ 异步下单（削峰）
- [ ] ELK 日志收集
- [ ] Prometheus + Grafana 监控
- [ ] 多级缓存（Caffeine + Redis）

---

> Author: [陈彪](https://github.com/USTCCB) · Java 后端开发实习在读
