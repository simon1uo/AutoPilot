# 发布指南 / Publishing

当前发布渠道：**GitHub Releases**（应用内自更新）。

## 1. 两个 flavor

| Flavor | 用途 | 自更新 |
|---|---|---|
| `direct` | GitHub / 酷安 / 官网直发 | ✅ 有 |
| `foss` | F-Droid / 应用商店（以后） | ❌ 无 |

`BuildConfig.SELF_UPDATE` 控制是否在应用内检查/下载更新。

## 2. 发版流程

1. 更新 `app/app/build.gradle.kts` 的 `versionCode`（+1）与 `versionName`。
2. 更新 `CHANGELOG.md`。
3. 提交：`git commit -m "chore(release): vX.Y.Z"`。
4. 打 tag 并推送：
   ```bash
   git tag -a vX.Y.Z -m "Release vX.Y.Z"
   git push origin main --tags
   ```
5. CI（`release.yml`）自动：构建 `direct` 的 release(签名) + debug，校验签名，创建 GitHub Release 并附 APK。

## 3. 应用内自更新原理

1. App 启动（或设置页手动）调用 GitHub Releases API，比较 `versionName`。
2. 发现新版本：从 release 资产里选 `release` 且非 `debug` 的 `.apk` 下载地址。
3. 用户点「立即更新」→ `AppUpdater` 下载到应用缓存 → 拉起系统安装器。
4. **系统安装器会自动校验新 APK 与已安装应用签名一致**，签名不符会被拒绝——这是最后一道安全防线。

首次自更新需要用户授予「安装未知应用」权限（Android 8+），App 会引导跳转。

## 4. 国内访问加速（可选）

GitHub 直连在国内较慢，可在发布页额外提供镜像下载链接，例如：

- `https://ghproxy.net/https://github.com/simon1uo/AutoPilot/releases/download/<tag>/<file>.apk`
- 或南大镜像 `https://mirror.nju.edu.cn/github-release/simon1uo/AutoPilot/`

应用内更新默认走 GitHub API；如需走镜像，可在后续把 `UpdateChecker` 的固定 URL 换成可配置。

## 5. 客户端验签

用户/你可核对下载的 APK 是否由官方 key 签名：

```bash
apksigner verify --print-certs AutoPilot-vX.Y.Z-release.apk
```

证书 SHA-256 指纹应与 `docs/SIGNING.md` 中记录的官方指纹一致。
