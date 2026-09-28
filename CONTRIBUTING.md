# 参与贡献

感谢你愿意改进牛马日记。

## 开始之前

1. 搜索现有 Issue，避免重复工作；
2. Bug 请尽量附上 Android 版本、机型、复现步骤与必要截图；
3. 大功能建议先开 Issue 讨论数据模型和交互，避免与现有方向冲突。

## 本地开发

需要 JDK 17、Android SDK 35 与最新版稳定版 Android Studio。克隆项目后执行：

```bash
./gradlew testDebugUnitTest lintDebug assembleDebug
```

Windows 可使用 `gradlew.bat`。提交前请确保以上任务全部通过。

## 提交约定

- 一次提交只解决一个明确问题；
- 不提交 `local.properties`、SDK、构建产物、签名文件或真实日记数据；
- 修改 Room 实体时要提交更新后的 schema，并提供兼容旧数据的迁移与测试；
- UI 改动需兼顾软键盘、全面屏安全区域、小屏与大字体；
- 用户可见文案优先使用简洁自然的中文。

## Pull Request

请说明改动目的、实现方式、测试结果以及是否影响数据库或覆盖安装。视觉改动建议附修改前后截图。

所有贡献按仓库的 [MIT License](LICENSE) 发布。
