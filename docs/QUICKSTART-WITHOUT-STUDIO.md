# LightCare · 不装 Android Studio 直接编译到手机

> 适用：Windows / macOS / Linux 任意一台能跑 JDK 的机器。

## 1. 一次性环境

| 工具 | 推荐版本 | 用途 |
|---|---|---|
| JDK | 17 (Temurin LTS) | 编译 Kotlin |
| Android SDK | platform-tools + platforms;android-34 + build-tools;34.0.0 | adb / aapt2 / d8 |
| 设备 | Android 7.0+ (API 24+) | 运行 APK |

### Windows 安装步骤

1. **JDK 17**：到 https://adoptium.net/ 下 `Temurin 17 (LTS) - Windows x64`。
   安装时勾"Set JAVA_HOME"和"Add to PATH"。

2. **Android 命令行工具**：到 https://developer.android.com/studio#command-line-tools-only
   下 `commandlinetools-win-*.zip`，解压到 `D:\Android\cmdline-tools\latest\`。

3. **设置环境变量**（系统 → 高级 → 环境变量）：
   ```
   ANDROID_HOME=D:\Android
   PATH 追加：%ANDROID_HOME%\cmdline-tools\latest\bin;%ANDROID_HOME%\platform-tools
   ```

4. **安装 SDK 组件**（开 cmd 跑）：
   ```bash
   sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   ```
   首次会问同意协议，输 `y`。

5. **手机开启 USB 调试**：
   - 设置 → 关于手机 → 连点 7 次"版本号"
   - 设置 → 系统 → 开发者选项 → USB 调试 = 开
   - 数据线连电脑，手机弹"是否允许调试"→ 勾"始终允许" → 确定

6. **验证**：
   ```bash
   adb devices
   ```
   应该看到一行 `xxxxxxxx    device`。

### macOS 安装步骤

```bash
brew install openjdk@17
brew install --cask android-commandlinetools
export ANDROID_HOME=$HOME/Library/Android/sdk
export PATH=$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools:$PATH

sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"
adb devices
```

## 2. 编译并安装

进入工程目录：

```bash
cd D:\AI_Practice\LightCare\android
```

**方式 A：一键脚本**
- Windows：双击 `build-and-install.bat`
- macOS / Linux：`./build-and-install.sh`

**方式 B：手动命令**
```bash
gradlew.bat :app:assembleDebug          # Windows
./gradlew :app:assembleDebug            # macOS / Linux

adb install -r app\build\outputs\apk\debug\app-debug.apk
```

> 第一次会下载 Gradle 8.7 + 全部 Maven 依赖，约 5-10 分钟。
> 之后增量编译只需几秒到几十秒。

## 3. 在手机上启动

桌面找到"轻养"图标 → 点击。如果首启看不到网络数据，先确认 server 端已启动（见 `server/README.md`），且模拟器/手机能访问到本机（模拟器用 `10.0.2.2:8080`，真机连同 WiFi 用电脑 LAN IP）。

## 4. 重新装

```bash
adb install -r app\build\outputs\apk\debug\app-debug.apk
```

`-r` 表示覆盖安装，保留数据。

## 5. 看日志（调试用）

```bash
adb logcat -s LightCare:V AndroidRuntime:E
```

## 常见问题

| 现象 | 解决 |
|---|---|
| `adb: command not found` | 检查 `ANDROID_HOME` / 重开 cmd |
| `BUILD FAILED` 报 license | `sdkmanager --licenses` 全 y |
| `INSTALL_FAILED_UPDATE_INCOMPATIBLE` | 先卸载：`adb uninstall com.lightcare.app` 再装 |
| `INSTALL_FAILED_USER_RESTRICTED` | 开发者选项里允许 USB 安装 |
| 手机连不上 | 换数据线（必须是数据线不是充电线）/ 重启 adb：`adb kill-server && adb start-server` |
| 装上但打开就闪退 | 跑 `adb logcat *:E` 看崩溃栈，多半是缺少 SO 库（暂未遇到） |
| 装上但功能跑不通 | 多半是后端没起，先按 `server/README.md` 跑后端 |
