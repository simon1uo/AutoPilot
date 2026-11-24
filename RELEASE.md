# 发包规范 / Release Convention

## 版本号 / Versioning

遵循 [Semantic Versioning 2.0.0](https://semver.org/lang/zh-CN/)：`MAJOR.MINOR.PATCH`

| 位 | 何时递增 / When to bump |
|----|------------------------|
| **MAJOR** | 不兼容的破坏性变更 / breaking changes |
| **MINOR** | 向后兼容的新功能 / new features (backward compatible) |
| **PATCH** | 向后兼容的 bug 修复 / bug fixes |

Android 侧同步维护两个字段（`app/app/build.gradle.kts`）：

```kotlin
versionCode = N     // 单调递增整数，每次发布 +1（即使只是 patch）
versionName = "X.Y.Z"  // 与 git tag 保持一致（去掉 v 前缀）
```

> 规则：**versionName == tag 去掉 `v`**；**versionCode 每次发布严格 +1**。

## 发布流程 / Release Process

1. **更新 CHANGELOG.md**：在 `[Unreleased]` 下整理，或直接生成新版本小节。
2. **更新版本号**：`app/app/build.gradle.kts` 的 `versionCode`+`versionName`。
3. **提交**（Conventional Commits）：`chore(release): vX.Y.Z`
4. **打 tag 并推送**：
   ```bash
   git tag -a vX.Y.Z -m "Release vX.Y.Z"
   git push origin main --tags
   ```
5. **GitHub Actions 自动**：
   - 运行 lint/构建
   - 构建 **release APK**（未签名，供测试）+ **debug APK**
   - 生成对应 **GitHub Release**，上传 APK 作为 assets，正文取自 CHANGELOG 对应小节

## Tag 命名 / Tag Naming

- 版本 tag：`vX.Y.Z`（例 `v0.2.0`）
- 预发布：`vX.Y.Z-beta.1`、`-rc.1`

## 签名 / Signing

- 仓库**不提交**任何 keystore / 密钥。
- Debug 构建使用自动生成的 debug keystore（仅本地/CI 临时）。
- Release 正式签名由维护者在本地或通过仓库 Secrets 完成，签名文件不入库。
- 如需 CI 产出签名包，请在仓库 Settings → Secrets 配置：
  - `KEYSTORE_BASE64`（base64 编码的 keystore）
  - `KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`
  构建脚本检测到这些 secrets 时才启用 release 签名，否则产出 unsigned APK。

## 产物命名 / Artifact Naming

`AutoPilot-vX.Y.Z-<debug|release>.apk`（由 CI 重命名后上传）。
