# 签名指南 / APK Signing

AutoPilot 采用**开发者自签名**（self-signed release key），与绝大多数开源 Android 应用一致。Android 不要求 CA，但要求「同一包名 + 同一签名」才能覆盖升级。

## 1. 一次性生成 keystore（务必先备份）

```bash
# 方式 A：用脚本（推荐）
bash scripts/gen-keystore.sh release.jks autopilot

# 方式 B：手动
keytool -genkeypair -v -keystore release.jks -alias autopilot \
  -keyalg RSA -keysize 4096 -validity 10000
```

- 有效期 10000 天（≈27 年），满足 Google Play 的 25 年要求。
- **丢失 keystore = 永远无法再发布更新**（用户只能卸载重装）。至少离线备份 3 份。
- keystore 与密码**绝不进 Git 仓库**（`.gitignore` 已忽略 `*.jks`、`*.keystore`、`keystore.properties`）。

## 2. 本地签名

复制 `keystore.properties.example` 为 `keystore.properties` 并填好；构建时 `build.gradle.kts` 会自动读取。

```bash
cd app
./gradlew assembleDirectRelease
# 产物：app/build/outputs/apk/direct/release/app-direct-release.apk（已签名）
```

## 3. CI 签名（GitHub Actions）

在仓库 **Settings → Secrets and variables → Actions** 添加：

| Secret | 内容 |
|---|---|
| `KEYSTORE_BASE64` | base64 编码后的 keystore（`scripts/gen-keystore.sh` 会直接打印） |
| `KEYSTORE_PASSWORD` | keystore 密码 |
| `KEY_ALIAS` | `autopilot` |
| `KEY_PASSWORD` | key 密码（通常同上） |

`release.yml` 检测到 `KEYSTORE_BASE64` 时会解码、签名并 `apksigner verify`；没有则产出 unsigned 包（仅用于无法获取密钥的 fork）。

## 4. 签名方案

- `minSdk 26`，使用 `apksigner` 默认签名方案（v2 + v3）即可；**不要用已废弃的 `jarsigner`**。
- 验证：`apksigner verify --print-certs app-direct-release.apk`。

## 5. 与 F-Droid 的关系（若以后上架）

- F-Droid 默认会用**它自己的 key** 重新签名，导致 F-Droid 版与 GitHub 版签名不同、不能互相覆盖。
- 想让全渠道同一签名：在 fdroiddata 元数据里做可复现构建，并填 `AllowedAPKSigningKeys: <你的证书 SHA-256 指纹>`（`apksigner verify --print-certs` 可拿到）。
