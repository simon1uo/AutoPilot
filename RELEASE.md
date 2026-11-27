# 发包规范 / Release Convention

> English: [RELEASE.en.md](RELEASE.en.md)

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
5. **GitHub Actions 自动**（`.github/workflows/release.yml`）：
   - 构建 `direct` flavor 的 **签名 release APK** + **debug APK**
   - 用 `apksigner` 校验签名
   - 生成对应 **GitHub Release**，上传 APK 作为 assets，正文取自 CHANGELOG 对应小节
   （未配置 keystore Secrets 时产出 unsigned 包，仅用于无法获取密钥的 fork）

## Tag 命名 / Tag Naming

- 版本 tag：`vX.Y.Z`（例 `v0.2.0`）
- 预发布：`vX.Y.Z-beta.1`、`-rc.1`

## 签名 / Signing

完整说明见 [docs/SIGNING.md](docs/SIGNING.md)。要点：

- 采用**开发者自签名**（self-signed release key），与主流开源应用一致。
- 仓库**不提交**任何 keystore / 密钥；`*.jks`、`*.keystore`、`keystore.properties` 已在 `.gitignore`。
- 本地：复制 `keystore.properties.example` → `keystore.properties` 后即可签名。
- CI：在仓库 Settings → Secrets 配置 `KEYSTORE_BASE64`、`KEYSTORE_PASSWORD`、`KEY_ALIAS`、`KEY_PASSWORD`；检测到即签名并校验，否则产出 unsigned 包。
- **keystore 一旦丢失将无法再发布更新**，务必离线多份备份。
- 分发包为 `direct` flavor（带应用内自更新）；`foss` flavor 不含自更新，供 F-Droid/商店使用。

## 产物命名 / Artifact Naming

`AutoPilot-vX.Y.Z-<debug|release>.apk`（由 CI 重命名后上传）。