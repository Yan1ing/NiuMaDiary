# 牛马日记 · NiuMaDiary

[![Android CI](https://github.com/Yan1ing/NiuMaDiary/actions/workflows/android.yml/badge.svg)](https://github.com/Yan1ing/NiuMaDiary/actions/workflows/android.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-7c5cfc.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Android-10%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/)

一款离线优先的 Android 上班精神状态日记。打开应用直接进入“今天”，可以随手记录吐槽、心情和标签；下班后写总结，并在日历与统计页回看自己的工作生活。

> 数据默认只保存在手机本地，无账号、无广告、无联网权限。

## 功能

- **今日速记**：每条记录保存精确时间，可编辑、删除并添加精神状态与标签。
- **下班总结**：按日期保存当天总结和最终精神状态。
- **工作倒计时**：默认 `Asia/Shanghai`、09:00–18:00、固定双休、法定节假日休息。
- **本周进度**：只累计有效上班时段，显示已上班时长、剩余时长与完成比例。
- **可选调休**：可以在设置中切换是否跟随国务院周末补班安排。
- **精神状态日历**：42 格月历、每日代表状态、记录数、工作日/休息日；支持回到本月以及按年月快速跳转。
- **趣味统计**：计划上班天数、留下记录的工作日、工作第 N 天、状态分布与常用标签。
- **丰富选项**：8 档哈吉米精神状态、17 个内置工作标签，也支持自定义标签。
- **本地存储**：Room 数据库与 DataStore 设置，升级应用时保留已有记录。

## 预览与设计

界面使用 Jetpack Compose + Material 3，晚霞与蓝调背景贯穿四个主页面和底部导航。布局针对竖屏手机、全面屏安全区域及软键盘弹出场景做了适配。

## 技术栈

- Kotlin 2.1、Jetpack Compose、Material 3
- 单 Activity + Navigation Compose
- ViewModel、StateFlow、单向数据流
- Room、DataStore、Hilt
- minSdk 29、targetSdk 35、JDK 17、Gradle 8.7

项目的竞品比较、产品规则与架构取舍见 [调研与架构方案](docs/牛马日记-调研与架构方案.md)。参考项目仅用于功能和架构研究，没有复制 GPL/AGPL 项目源码。

## 构建

准备 JDK 17 与 Android SDK 35 后：

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows PowerShell：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

APK 位于 `app/build/outputs/apk/debug/app-debug.apk`。CI 也会为每次提交构建可下载的 debug APK artifact。

## 数据与升级

- 应用 ID 固定为 `com.niumadiary.app`。
- Room schema 会随源码提交，并通过兼容性测试防止意外破坏旧数据。
- 想覆盖安装旧版本时，必须使用与旧版本相同的签名；自行构建的 debug 包只能覆盖同一台开发环境签名的版本。
- 卸载应用会清除 Android 本地数据，请先确认不再需要历史记录。

详细说明见 [隐私说明](PRIVACY.md)。

## 贡献

欢迎提交 Issue 和 Pull Request。开始前请阅读 [CONTRIBUTING.md](CONTRIBUTING.md)。

## 当前边界

当前版本是可安装的 MVP。通知提醒、JSON/Markdown 导入导出、隐私锁与正式 release 签名尚未接入；设置页会明确标注未实现功能。

## 开源许可

本项目使用 [MIT License](LICENSE)。提交贡献即表示你同意以相同许可证发布你的贡献。
