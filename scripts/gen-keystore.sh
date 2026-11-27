#!/usr/bin/env bash
# 生成 AutoPilot 发布签名 keystore，并输出 GitHub Secrets 需要的内容
set -euo pipefail

OUT="${1:-release.jks}"
ALIAS="${2:-autopilot}"

if [ -e "$OUT" ]; then
  echo "错误：$OUT 已存在，为避免覆盖已停止。请换文件名或先备份。" >&2
  exit 1
fi

read -r -s -p "设置 keystore 密码: " STOREPASS; echo
read -r -s -p "再次输入密码: " STOREPASS2; echo
if [ "$STOREPASS" != "$STOREPASS2" ]; then echo "两次密码不一致" >&2; exit 1; fi

keytool -genkeypair -v \
  -keystore "$OUT" -alias "$ALIAS" \
  -keyalg RSA -keysize 4096 -validity 10000 \
  -storepass "$STOREPASS" -keypass "$STOREPASS" \
  -dname "CN=simon1uo, OU=AutoPilot, O=AutoPilot, C=CN"

echo
echo "✅ 已生成 $OUT"
echo "⚠️  请立即离线备份至少 3 份（U 盘 / 密码管理器 / 纸质口令）。丢失后无法再发布更新！"
echo
echo "=== 加入 GitHub 仓库 Settings → Secrets and variables → Actions ==="
echo "KEYSTORE_BASE64 =  (下面这串)"
base64 < "$OUT" | tr -d '\n'; echo
echo "KEYSTORE_PASSWORD = 你刚才设置的密码"
echo "KEY_ALIAS = $ALIAS"
echo "KEY_PASSWORD = 你刚才设置的密码"
