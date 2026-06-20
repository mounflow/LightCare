@echo off
REM LightCare · 一键编译 + 安装到手机
REM 用法：在 android/ 目录下双击运行

setlocal
cd /d %~dp0

echo ========================================
echo  LightCare · Build ^& Install
echo ========================================

where java >nul 2>&1
if errorlevel 1 (
  echo [X] 未找到 java，请先安装 JDK 17 并确保在 PATH 中
  pause
  exit /b 1
)

if not exist "%ANDROID_HOME%\platform-tools\adb.exe" (
  if not exist "D:\Android\platform-tools\adb.exe" (
    echo [X] 未找到 adb，请安装 Android SDK platform-tools
    echo     参考 docs\QUICKSTART-WITHOUT-STUDIO.md
    pause
    exit /b 1
  ) else (
    set "ANDROID_HOME=D:\Android"
  )
)

set "PATH=%ANDROID_HOME%\platform-tools;%PATH%"

echo.
echo [1/3] 编译 Debug APK...
call gradlew.bat :app:assembleDebug
if errorlevel 1 (
  echo [X] 编译失败
  pause
  exit /b 1
)

echo.
echo [2/3] 检测设备...
adb devices | findstr /R "device$" >nul
if errorlevel 1 (
  echo [!] 未检测到手机，跳过安装，仅生成 APK
  echo     APK 位置: app\build\outputs\apk\debug\app-debug.apk
  pause
  exit /b 0
)

echo.
echo [3/3] 安装到手机...
adb install -r app\build\outputs\apk\debug\app-debug.apk
if errorlevel 1 (
  echo [X] 安装失败
  pause
  exit /b 1
)

echo.
echo [OK] 完成！APK 已装到手机
pause
