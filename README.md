# Keep4

<p align="center">
  <img src="app/src/main/ic_launcher-playstore.png" width="128" alt="Keep4 图标">
</p>

[![Android CI](https://github.com/aimmarc/keep_4/actions/workflows/android-ci.yml/badge.svg)](https://github.com/aimmarc/keep_4/actions/workflows/android-ci.yml)
[![Release](https://img.shields.io/github/v/release/aimmarc/keep_4?include_prereleases)](https://github.com/aimmarc/keep_4/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-146C43.svg)](LICENSE)

Keep4 是面向联想小新 Pad Pro 12.7 一代第三方 ROM 的开源 Android 工具。它在媒体播放期间启用录音通信路由，以维持设备的四扬声器输出。

> [!IMPORTANT]
> Keep4 依赖特定设备和 ROM 的音频路由行为。其他设备即使可以安装，也不代表功能有效。使用前请阅读[免责声明](DISCLAIMER.md)。

## 2.0 开发版

- Jetpack Compose 原生界面和独立二级设置页
- 麦克风类型前台服务与低打扰常驻通知
- 服务被普通系统回收后的状态恢复
- 开机恢复；Android 14 及以上通过通知提示用户恢复
- 动态隐藏最近任务，不终止后台服务
- 可选无障碍辅助恢复
- 可选 Shizuku 待机白名单与后台 AppOps 优化
- DataStore 本地设置持久化
- 新应用 ID：`io.github.aimmarc.keep4`

2.0 更换了应用 ID，Android 会将其视为不同应用。安装 2.0 前建议先停止并卸载 1.x，旧版设置不会自动迁移。

## 权限与用途

| 权限或能力 | 用途 | 是否可选 |
| --- | --- | --- |
| 麦克风 | 启用 `VOICE_COMMUNICATION` 音频路由 | 核心功能必需 |
| 通知 | 展示前台服务状态与恢复入口 | Android 13+ 由用户授权 |
| 开机广播 | 在设备重启后恢复用户设置 | 可关闭 |
| 无障碍服务 | 系统重新绑定时辅助恢复已开启的服务 | 可选 |
| Shizuku | 申请待机白名单和后台 AppOps | 可选 |

Keep4 不申请联网权限，不包含广告、统计 SDK 或远程配置。录音输出只写入应用缓存，并在停止后立即删除。完整说明见[隐私政策](PRIVACY.md)。

## 安装

从 [Releases](https://github.com/aimmarc/keep_4/releases) 下载已签名 APK。首次运行时授予麦克风和通知权限，再根据 ROM 设置后台自启动与电池白名单。

GitHub Actions 生成的 Debug APK 仅用于测试。Release APK 使用项目的长期发布密钥签名，并在上传前校验证书指纹。

## 构建

环境要求：

- JDK 17
- Android SDK 34
- Gradle Wrapper 8.9

```bash
./gradlew lintDebug testDebugUnitTest assembleDebug
```

仓库的 [Android CI](.github/workflows/android-ci.yml) 会执行同样的检查，并上传 Debug APK、Lint 报告和测试报告。

## 架构

- `MainActivity`：Compose 界面、权限请求和任务可见性
- `Keep4ForegroundService`：常驻通知、播放检测和录音路由
- `AudioRouteEngine`：串行管理 `MediaRecorder` 生命周期
- `SettingsRepository`：DataStore 设置持久化
- `BootReceiver`：开机恢复策略
- `Keep4AccessibilityService`：可选的系统重绑定恢复
- `Keep4ShizukuService`：Shizuku UserService 后台优化

## 系统限制

- Android 不提供绝对“永不被杀”的后台能力。
- 用户强行停止应用后，服务和开机广播都会被系统禁止，直到再次手动启动。
- Android 14 及以上限制从开机广播直接启动麦克风前台服务，因此 Keep4 会发送恢复通知。
- 无障碍和 Shizuku 都是可选增强能力，不能替代前台服务。
- 四扬声器路由、厂商后台策略和 Shizuku 命令必须在目标平板真机验证。

## 参与贡献

欢迎提交问题、文档、测试结果和代码改进。提交前请阅读[贡献指南](CONTRIBUTING.md)和[行为准则](CODE_OF_CONDUCT.md)。安全问题请按[安全政策](SECURITY.md)私下报告。

项目变更记录见 [CHANGELOG.md](CHANGELOG.md)，第三方组件许可见 [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md)。

## 许可证

Keep4 采用 [MIT License](LICENSE)。允许学习、修改、分发和商业使用，但必须保留原始版权与许可证声明。项目名称、作者身份和贡献者署名不得被冒充。
