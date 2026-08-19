# 变更记录

本项目采用语义化版本思路记录重要变更。

## [2.0.0-alpha01] - 未发布

### 新增

- Jetpack Compose 原生界面和二级设置页；
- 麦克风前台服务与常驻状态通知；
- DataStore 设置持久化；
- 开机恢复策略和 Android 14+ 恢复通知；
- 动态隐藏最近任务；
- 可选无障碍辅助恢复；
- Shizuku UserService 后台优化；
- GitHub Actions 编译、Lint、测试与 APK Artifact。
- 新的 Material 自适应图标和 Android 13 主题图标；
- 应用 ID 更新为 `io.github.aimmarc.keep4`。

### 改进

- 将录音逻辑从 Activity 迁移到独立状态机；
- 确保启动、停止和异常路径释放 MediaRecorder；
- 删除 WebView、前端资源、模板代码和未使用测试音频；
- 增加隐私政策、免责声明、贡献指南、安全政策和第三方许可说明。

## [1.1.0] - 2024-12-13

- 优化旧版 WebView 界面；
- 增加普通常驻通知实验。

## [1.0.0] - 2024-12-07

- 首个公开版本。
