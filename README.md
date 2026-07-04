# API额度监控 - 简化版 Android应用

这是一个简化版的API额度监控应用，包含：
1. **WebView监控页面** - 显示余额信息
2. **通知栏常驻** - 每5分钟自动更新，常驻通知栏
3. **桌面小组件** - 显示余额，可手动刷新

## 功能特性

- ✅ 通知栏常驻显示余额
- ✅ 桌面小组件显示余额
- ✅ WebView监控页面
- ✅ 自动/手动刷新
- ✅ 支持三方中转和DeepSeek

## 编译方法

### 方法1：使用在线编译服务（推荐）

1. 将整个项目文件夹打包成ZIP
2. 访问 https://html2app.dev/ 或类似服务
3. 上传ZIP文件
4. 配置应用信息
5. 下载APK文件

### 方法2：使用GitHub Actions

1. 创建GitHub仓库
2. 上传项目代码
3. 配置GitHub Actions自动编译
4. 下载编译好的APK

### 方法3：使用Android Studio

1. 用Android Studio打开项目
2. 等待Gradle同步
3. 编译APK

## 使用说明

1. 安装应用
2. 输入API Key
3. 点击"保存配置"
4. 点击"启动监控"
5. 通知栏会常驻显示余额
6. 添加桌面小组件

## 项目结构

```
ApiMonitorSimple/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/apimonitorsimple/
│   │   │   ├── MainActivity.java
│   │   │   ├── BalanceNotificationService.java
│   │   │   └── BalanceWidget.java
│   │   ├── res/
│   │   │   ├── layout/
│   │   │   ├── drawable/
│   │   │   └── values/
│   │   ├── assets/
│   │   │   └── monitor.html
│   │   └── AndroidManifest.xml
│   └── build.gradle
├── build.gradle
└── README.md
```

## 技术栈

- Java
- WebView
- 通知栏服务
- OkHttp
- Gson

## 注意事项

1. 需要Android 8.0+（API 26）
2. 需要网络权限
3. 需要通知权限