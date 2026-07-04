#!/bin/bash

# API额度监控应用编译脚本
# 使用方法: 在Termux中运行此脚本

echo "开始编译API额度监控应用..."

# 1. 更新包管理器
echo "1. 更新包管理器..."
pkg update -y

# 2. 安装必要的包
echo "2. 安装必要的包..."
pkg install -y openjdk-17
pkg install -y wget
pkg install -y unzip

# 3. 下载Android SDK
echo "3. 下载Android SDK..."
mkdir -p ~/android-sdk
cd ~/android-sdk
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip

# 4. 设置环境变量
echo "4. 设置环境变量..."
export ANDROID_HOME=~/android-sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools

# 5. 接受许可
echo "5. 接受许可..."
yes | sdkmanager --licenses

# 6. 安装SDK组件
echo "6. 安装SDK组件..."
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 7. 下载Gradle
echo "7. 下载Gradle..."
cd ~
wget https://services.gradle.org/distributions/gradle-8.4-bin.zip
unzip gradle-8.4-bin.zip
export PATH=$PATH:~/gradle-8.4/bin

# 8. 编译项目
echo "8. 编译项目..."
cd /sdcard/Download/ApiMonitorSimple
gradle assembleDebug

# 9. 复制APK
echo "9. 复制APK..."
if [ -f app/build/outputs/apk/debug/app-debug.apk ]; then
    cp app/build/outputs/apk/debug/app-debug.apk /sdcard/Download/ApiMonitor.apk
    echo "编译完成！APK已保存到: /sdcard/Download/ApiMonitor.apk"
    echo "请在手机上安装此APK文件。"
else
    echo "编译失败，请检查错误信息。"
fi