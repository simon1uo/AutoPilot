# Release Convention (English)

## Versioning
Follows [SemVer 2.0.0](https://semver.org/): `MAJOR.MINOR.PATCH`.
- MAJOR: breaking changes
- MINOR: backward-compatible features
- PATCH: backward-compatible fixes

Android fields in `app/app/build.gradle.kts`:
```kotlin
versionCode = N        // strictly +1 on every release
versionName = "X.Y.Z"  // equals tag without 'v'
```
Rule: **versionName == tag minus `v`**; **versionCode strictly increments** per release.

## Release Process
1. Update `CHANGELOG.md` (new version section).
2. Bump `versionCode` + `versionName` in `app/app/build.gradle.kts`.
3. Commit: `chore(release): vX.Y.Z`.
4. Tag & push:
   ```bash
   git tag -a vX.Y.Z -m "Release vX.Y.Z"
   git push origin main --tags
   ```
5. GitHub Actions then: builds release + debug APK, creates a GitHub Release, attaches APKs, body taken from the matching CHANGELOG section.

## Tag naming
Version tags: `vX.Y.Z`. Pre-releases: `vX.Y.Z-beta.1`, `-rc.1` (auto-marked prerelease).

## Signing
- No keystore/secrets are committed.
- Debug builds use an auto-generated debug keystore (local/CI temporary).
- Release signing is done by the maintainer locally or via repo Secrets:
  - `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`
  When these are absent, CI produces an unsigned release APK.

## Artifact naming
`AutoPilot-vX.Y.Z-<debug|release>.apk` (renamed + uploaded by CI).
