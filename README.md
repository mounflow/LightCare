# 轻养 LightCare

> 全家人共用的饮食与作息陪伴 APP。Android 单端 + Java Spring Boot 后端。

## 目录

```
LightCare/
├── PRD.md                           # 产品设计文档
├── stitch_greeneats/                # UI 高保真设计稿 (6 屏)
├── docs/                            # 技术决策 / API / 设计借鉴
├── android/                         # Android 客户端 (Kotlin + Compose)
└── server/                          # Spring Boot 后端 (Java 21)
```

## 阶段

| 阶段 | 状态 | 内容 |
|---|---|---|
| P1 工程骨架 | ✅ | Gradle 骨架、Hello 端点、Compose 主题 |
| P2 视觉系统 + 首页占位 | ⏳ | 3 环色环 + 5 Tab + FAB |
| P3 数据模型 + API 骨架 | ⏳ | JPA / Flyway / Auth / Retrofit |
| P4 记录主路径 | ⏳ | 拍照/语音/搜索 + 入库 + 色环动效 |
| P5 推荐 + 周报 | ⏳ | 规则引擎 + 周报 + 分享图 |

## 关键技术决策

| 项 | 选定 | 备注 |
|---|---|---|
| UI 复用 | stitch_greeneats 配色 / 字体 / 形状 | `DESIGN.md` 完整复刻 |
| 局部借鉴 | 小宇宙的时间戳鼓励 / 极简留白 / 收藏 | 见 PRD §0 与 `docs/` |
| Android | Kotlin + Jetpack Compose + Hilt | 单端 iOS 暂不发 |
| 后端 | Java 21 + Spring Boot 3 | PostgreSQL + Flyway + Redis |
| 数据库 | PostgreSQL 16 | |
| 鉴权 | JWT（P3 引入） | 手机号 + 微信登录 |

## 快速跑通（P1）

### Android
```bash
cd android
./gradlew :app:assembleDebug          # 编译 APK
# 设备调试：
./gradlew :app:installDebug
```

### Server
```bash
cd server
./gradlew bootRun                     # 默认 http://localhost:8080
curl http://localhost:8080/v1/hello   # 探针
open http://localhost:8080/swagger-ui.html
```

需要本地起 PostgreSQL（默认 `jdbc:postgresql://localhost:5432/lightcare`）和 Redis。可用 docker：
```bash
docker run -d --name lc-pg -p 5432:5432 \
  -e POSTGRES_USER=lightcare -e POSTGRES_PASSWORD=lightcare -e POSTGRES_DB=lightcare \
  postgres:16
docker run -d --name lc-redis -p 6379:6379 redis:7
```

## 待细化清单

见 `PRD.md` §7 —— 包含 10 项需要后续重审的占位决策（注册流程、注销冷静期、食物数据源、菜谱池大小、推送时机、徽章解锁条件等）。
