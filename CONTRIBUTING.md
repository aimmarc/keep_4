# 参与贡献

感谢参与 Keep4。项目欢迎代码、文档、兼容性报告、测试结果和设计改进。

## 提交问题

请先搜索现有 Issue，并尽量提供：

- 设备完整型号；
- Android 与 ROM 名称、版本；
- Keep4 版本；
- 是否启用无障碍或 Shizuku；
- 可复现步骤、预期结果和实际结果；
- 已移除个人信息的日志。

安全和隐私漏洞不要提交公开 Issue，请遵循 [SECURITY.md](SECURITY.md)。

## 开发流程

1. 从最新 `main` 创建功能分支。
2. 保持改动聚焦，不提交生成目录、密钥或本机配置。
3. 执行 `./gradlew lintDebug testDebugUnitTest assembleDebug`。
4. 在目标平板验证涉及音频路由、后台、开机、无障碍或 Shizuku 的改动。
5. 提交 Pull Request，说明行为变化、验证环境和剩余风险。

贡献代码默认按项目的 MIT License 授权，并保留合理的作者与贡献者署名。
