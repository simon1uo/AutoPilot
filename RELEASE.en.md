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
5. GitHub Actions (`.github/workflows/release.yml`) then:
   - builds the `direct` flavor **signed release** + **debug** APK,
   - verifies the signature with `apksigner`,
   - creates a GitHub Release, attaches the APKs, body taken from the matching CHANGELOG section.
   If keystore secrets are absent it produces an unsigned APK (fork/test only).

## Tag naming
Version tags: `vX.Y.Z`. Pre-releases: `vX.Y.Z-beta.1`, `-rc.1` (auto-marked prerelease).

## Signing
See [docs/SIGNING.md](docs/SIGNING.md). Key points:
- Uses a **developer self-signed release key**, like most FOSS Android apps.
- No keystore/secrets are committed; `*.jks`, `*.keystore`, `keystore.properties` are git-ignored.
- Local: copy `keystore.properties.example` to `keystore.properties`.
- CI: set `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` in repo Secrets; when present CI signs and verifies, otherwise it builds unsigned.
- **Losing the keystore means you can never ship updates again** — keep multiple offline backups.
- Distribution build is the `direct` flavor (in-app self-update); `foss` has no self-update (for F-Droid/stores).

## Artifact naming
`AutoPilot-vX.Y.Z-<debug|release>.apk` (renamed + uploaded by CI).
