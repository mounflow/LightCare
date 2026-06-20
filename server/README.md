# LightCare · 后端服务

> Java 21 + Spring Boot 3。PostgreSQL + Flyway。

**定位（2026-06 起）：本地运行。** 不再走云端账号/登录，App 通过模拟器连本机 8080。
首次建档用无鉴权的 `POST /v1/profiles/bootstrap`，之后所有请求带 `X-LightCare-User-Id` 头（=建档返回的 userId）。

## 跑起来

### 1. 准备 PostgreSQL

本地起一个 PostgreSQL（16+），建库 + 用户：

```sql
CREATE DATABASE lightcare;
CREATE USER lightcare WITH PASSWORD 'lightcare';
GRANT ALL PRIVILEGES ON DATABASE lightcare TO lightcare;
```

> 表由 Flyway 自动建（`db/migration/V1__init.sql`），无需手动建表。

### 2. 启动 server

```bash
# 在 server/ 目录
./gradlew bootRun
# 或 Windows: gradlew.bat bootRun
```

首次构建会自动下载 JDK 21（foojay toolchain，约 200MB，需联网一次）。
Gradle daemon 强制用 JDK 17（见 `gradle.properties` 的 `org.gradle.java.home`），别删这行否则会报 Spring Boot 插件 "compatible with Java 8" 解析失败。

### 3. 验证

```bash
# 健康探针
curl http://localhost:8080/v1/hello

# 本地建档（无鉴权）—— App 首启时调的就是这个
curl -X POST http://localhost:8080/v1/profiles/bootstrap \
  -H "Content-Type: application/json" \
  -d '{"displayName":"我"}'
# → {"code":0,"data":{"userId":1,"profile":{...}}}

# 用返回的 userId 查档案
curl -H "X-LightCare-User-Id: 1" http://localhost:8080/v1/profiles

# Swagger UI
# 浏览器打开 http://localhost:8080/swagger-ui.html
```

## 主要端点

| 方法 | 路径 | 鉴权 | 说明 |
|---|---|---|---|
| POST | `/v1/profiles/bootstrap` | 无 | 本地建档，返回 userId+profile |
| GET | `/v1/profiles` | User-Id 头 | 列当前用户档案 |
| POST | `/v1/profiles` | User-Id 头 | 新建档案（上限 4） |
| PATCH | `/v1/profiles/{id}/goals` | User-Id 头 | 改营养目标 |
| GET/POST/DELETE | `/v1/meals[?profileId&date]` | User-Id 头 | 餐次记录 |
| GET | `/v1/recommend/today?profileId=` | User-Id 头 | 推荐卡 |
| GET | `/v1/reports/weekly?profileId=` | User-Id 头 | 周报 |

> 鉴权现状：`SecurityConfig` 全 `permitAll()`；`CurrentUserResolver` 从 `X-LightCare-User-Id` 头读明文 userId，`mustOwn` 校验档案归属。本地单机够用。

## 环境变量

| 变量 | 默认 | 说明 |
|---|---|---|
| `DB_URL` | `jdbc:postgresql://localhost:5432/lightcare` | 数据库 |
| `DB_USER` | `lightcare` | |
| `DB_PASSWORD` | `lightcare` | |

## 目录

```
server/
├── src/main/java/com/lightcare/server/
│   ├── LightCareServerApplication.java
│   ├── common/      # ApiResponse / ApiException / CurrentUserResolver
│   ├── config/      # SecurityConfig（全开放）
│   ├── profile/     # Profile + User（含 bootstrap 端点）
│   ├── meal/        # Meal + AiEstimateService（本地 SEED 估算）
│   ├── exercise/    # 运动记录
│   ├── recommend/   # 推荐引擎
│   └── report/      # 周报
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/V1__init.sql
└── settings.gradle.kts   # 含 foojay resolver（自动下 JDK21）+ 阿里云镜像
```
