# AutoPilot

极简 Android 自动化：**定时 → 唤醒屏幕 → 打开指定 App** 一条龙。
开源、免费、无订阅。一个专注于「定时 → 唤醒屏幕 → 打开 App」的极简自动化工具。

## 功能 (v1)
- 📋 任务列表：启停 / 立即触发 / 删除
- ⏰ 定时规则：每天 HH:mm + 选星期几，或 **每 N 分钟/小时** 重复
- 🌅 到点执行：精确闹钟唤醒 → WakeLock 亮屏 → 启动目标 App
- 🌐 内建 HTTP 控制服务（默认 :8080），脚本化增删改查、立即触发
- 📡 ADB 直通控制（am broadcast）
- 🔋 开机自动恢复所有任务

## 快速开始（开发）
```bash
# 需要 JDK 21、Android SDK (build-tools 35/36, platform 36)
export JAVA_HOME=/path/to/jdk-21
export ANDROID_HOME=$HOME/Library/Android/sdk
cd app
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

## 使用
1. 首次打开授予**精确闹钟**权限（Android 12+）。
2. 点「新建任务」→ 选时间/星期 → 选目标 App → 保存。
3. 到点自动亮屏并打开 App。

## HTTP 控制 API
服务启动后在手机同网段访问 `http://<手机IP>:8080`：
| 方法 | 路径 | 说明 |
|---|---|---|
| GET | /api/health | 健康检查 |
| GET | /api/tasks | 列出所有任务 |
| POST | /api/tasks | 新建任务(body为Task JSON) |
| POST | /api/tasks/{id}/trigger | 立即触发 |
| POST | /api/tasks/{id}/toggle | 启停切换 |
| DELETE | /api/tasks/{id} | 删除 |

启动服务：
```bash
adb shell am startservice -n io.github.simon1uo.autopilot/.control.HttpServerService
```

## ADB 直通
```bash
# 列出任务
adb shell am broadcast -a io.github.simon1uo.autopilot.CONTROL \
  -n io.github.simon1uo.autopilot/.control.ControlReceiver --es cmd list
# 立即触发
adb shell am broadcast -a io.github.simon1uo.autopilot.CONTROL \
  -n io.github.simon1uo.autopilot/.control.ControlReceiver --es cmd trigger --el id 123
# 新建任务
adb shell am broadcast -a io.github.simon1uo.autopilot.CONTROL \
  -n io.github.simon1uo.autopilot/.control.ControlReceiver --es cmd create \
  --es name "起床" --ei hour 7 --ei minute 0 \
  --es pkg com.android.chrome
```

## 技术栈
Kotlin · Jetpack Compose · MVVM · NanoHTTPD · kotlinx.serialization · minSdk 26

## 实现要点
- 定时：`AlarmManager.setAlarmClock` / `setExactAndAllowWhileIdle`（精确闹钟，可在待机中唤醒）
- 亮屏：`PowerManager.newWakeLock(SCREEN_BRIGHT_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE)`
- 启动：`Intent(ACTION_MAIN) + setClassName + FLAG_ACTIVITY_NEW_TASK`

## License
MIT
