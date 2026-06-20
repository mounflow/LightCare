# LightCare · Android 客户端

> 单端 Android 原生应用（iOS 暂不发）。Kotlin + Jetpack Compose + Hilt + Room + Retrofit。

## ✅ 在 Android Studio 中打开并编译

### 一次性环境

1. **安装 Android Studio**（Iguana 2023.2.1+ 或更新版本）
   下载：https://developer.android.com/studio
2. **首次启动 Studio 时安装**：
   - JDK 17（Studio 自带，不要用本机其他 JDK）
   - Android SDK Platform 34
   - Android SDK Build-Tools 34.0.0
   - Android Virtual Device（如果想用模拟器跑）
3. 设备准备：开启**开发者模式 → USB 调试**的真机 / 或 Studio 里建一个 AVD

### 打开工程

1. 启动 Android Studio
2. **File → Open** → 选 `D:\AI_Practice\LightCare\android` 这个目录
3. 第一次打开时 Studio 会做以下事情（**全自动，不需要你操作**）：
   - 提示 "Gradle Sync" → 同意
   - 提示 "Use Gradle wrapper from this project" → 选 "OK"（会下载 Gradle 8.7）
   - 提示 "Android SDK location" → 选默认路径（会写 `local.properties`）
   - 下载 Maven 依赖（Hilt / Compose / CameraX / Retrofit 等，约 2-5 分钟）
4. 同步完成后，左下角状态栏应显示 **"Gradle sync finished"** ✅

### 跑起来

> **前提：先启动后端。** 见 `server/README.md`，本机 `./gradlew bootRun` 起 8080。
> App 通过 `http://10.0.2.2:8080/` 连本机 server（模拟器映射宿主机）。
> 真机调试要把 `NetworkModule.kt` 的 baseUrl 改成本机局域网 IP。

- **真机**：连上手机 → 顶部工具栏下拉选你的设备 → 点绿色 ▶ Run
- **模拟器**：Tools → Device Manager → Create Device → 选 Pixel 6 → API 34 → 启动后跑

### 验证（本地化流程）

1. **首启**：App 直接进「选/建档案」页（不再有登录页）。
2. **建档**：输入称呼（如"我"）→ 点"开始" → 自动进首页（profileId 已存本地）。
3. **记录一餐**：底部中央橙色「＋」→ 选食物（从本地食物库搜索）→ 确认 → 回首页，营养余量实时变化。
4. **食物库管理**：底部「设置」→「我的食物库」→ 可新增/删除自定义食物。
5. **切换档案**：设置 →「切换/退出档案」→ 回选档页，可为家人再建（上限 4）。
6. **杀进程重进**：自动进首页（本地记住上次档案）。

> 视觉为**温暖圆润卡通风**（奶油底 / 暖橙 / 柔绿 / 大圆角 / Quicksand 字体）。

### 常见问题

| 现象 | 解决 |
|---|---|
| 同步报错 "SDK location not found" | 手动建 `android/local.properties`，写 `sdk.dir=C\:\\Users\\<你>\\AppData\\Local\\Android\\Sdk` |
| Gradle 下载慢 | Settings → Appearance & Behavior → System Settings → HTTP Proxy 配镜像 |
| 编译报 "Compose Compiler version" 不匹配 | 已用 `kotlinCompilerExtensionVersion = "1.5.14"` 匹配 Kotlin 1.9.24，无需改 |
| 模拟器装不上 | 真机调试，或 Tools → SDK Manager → SDK Tools 勾 Intel x86 Emulator Accelerator |
| Hilt 报 "Symbol HiltAndroidApp not found" | Build → Clean Project → Rebuild Project |

### 目录结构

```
android/
├── app/
│   ├── src/main/java/com/lightcare/app/
│   │   ├── LightCareApplication.kt    # @HiltAndroidApp
│   │   ├── MainActivity.kt            # 5 Tab + FAB + HomeScreen
│   │   ├── ui/
│   │   │   ├── home/                  # HomeScreen + HomeUiState
│   │   │   ├── shared/                # RingProgress / MainScaffold / RecommendCard
│   │   │   └── theme/                 # Color / Type / Theme
│   ├── src/main/res/                  # 配色 / 字符串 / 主题 / 启动图标
│   └── build.gradle.kts
├── build.gradle.kts                   # 根
├── settings.gradle.kts
├── gradle.properties
├── gradle/wrapper/gradle-wrapper.properties   # Gradle 8.7
├── local.properties.example           # SDK 路径示例
└── .gitignore
```

### 当前进度

- [x] **P1** Gradle 工程骨架、Compose 主题、Hilt 入口
- [x] **P2** 首页（3 环色环 + 5 Tab + FAB + 推荐卡占位）✅ 当前
- [ ] P3 数据层（Room + Retrofit + 后端接口）
- [ ] P4 记录主路径（拍照/语音/搜索 + 入库 + 色环动效）
- [ ] P5 推荐 + 周报

### 视觉系统

颜色、字体、形状参数全部源自 `stitch_greeneats/organic_wellness_system/DESIGN.md`。
中文字体在后续阶段会嵌入 Quicksand / Plus Jakarta Sans .ttf 资源（P2 当前用系统字体先跑通）。
