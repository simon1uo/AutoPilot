# AutoPilot — 项目决策记录 (ADR-000)

开源：GitHub @simon1uo
状态：已与用户逐项确认

| # | 项目 | 决定 |
|---|------|------|
| 1 | App 名称 | **AutoPilot** |
| 2 | 包名 (applicationId) | **io.github.simon1uo.autopilot** |
| 3 | 框架 | **Kotlin + Jetpack Compose + MVVM** |
| 4 | 控制/调试通道 | **内建 HTTP Server + ADB 命令直通**（不做 CDP；WebSocket 二期） |
| 5 | 任务模型 | **一条龙：定时→亮屏→打开指定App** |
| 6 | 定时规则 | 每天 HH:mm + 选星期几 + **重复间隔(每N分/时)** |
| 7 | 代码复用 | **开源模板骨架 + 关键库直接依赖**（附清单待确认） |
| 8 | minSdk | **26 (Android 8.0)**，targetSdk 34+ |

测试机：Pixel/Redmi 4a (Android 14/34) + TCL (Android 11/30, adb 无线)。

## 功能范围 (v1)
- 任务列表页：展示已有任务，可启停/删除/立即触发。
- 新建/编辑任务：选时间(时分) + 星期勾选 + 重复间隔 + 选目标App。
- 到点执行：setAlarmClock 唤醒 -> WakeLock 亮屏 -> ACTION_MAIN 启动App。
- 权限引导：精确闹钟权限、通知权限、查询已安装应用。
- HTTP 控制层：本地端口(默认 8080)，JSON API 增删改查+立即触发。
- ADB 直通：am broadcast 命令控制（建任务/触发/查询）。

## 三家核心逻辑落点
- 定时：AlarmManager.setAlarmClock / setExactAndAllowWhileIdle
- 亮屏：PowerManager.newWakeLock(0x3000000A).acquire(1000)
- 打开：Intent(ACTION_MAIN)+setClassName+NEW_TASK
