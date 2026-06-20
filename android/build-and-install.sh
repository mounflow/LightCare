#!/usr/bin/env bash
# LightCare · 一键编译 + 安装到手机（macOS / Linux）
# 用法：在 android/ 目录下执行 ./build-and-install.sh

set -e
cd "$(dirname "$0")"

echo "========================================"
echo " LightCare · Build & Install"
echo "========================================"

if ! command -v java >/dev/null 2>&1; then
  echo "[X] 未找到 java，请先安装 JDK 17 并确保在 PATH 中"
  exit 1
fi

if [ -z "$ANDROID_HOME" ]; then
  if [ -d "$HOME/Android/Sdk" ]; then
    export ANDROID_HOME="$HOME/Android/Sdk"
  elif [ -d "/usr/local/android-sdk" ]; then
    export ANDROID_HOME="/usr/local/android-sdk"
  else
    echo "[X] 未设置 ANDROID_HOME，请安装 Android SDK"
    echo "    参考 docs/QUICKSTART-WITHOUT-STUDIO.md"
    exit 1
  fi
fi

export PATH="$ANDROID_HOME/platform-tools:$PATH"

echo
echo "[1/3] 编译 Debug APK..."
./gradlew :app:assembleDebug

echo
echo "[2/3] 检测设备..."
DEVICES=$(adb devices | grep -E "device$" || true)
if [ -z "$DEVICES" ]; then
  echo "[!] 未检测到手机，跳过安装，仅生成 APK"
  echo "    APK 位置: app/build/outputs/apk/debug/app-debug.apk"
  exit 0
fi

echo
echo "[3/3] 安装到手机..."
adb install -r app/build/outputs/apk/debug/app-debug.apk

echo
echo "[OK] 完成！APK 已装到手机"
