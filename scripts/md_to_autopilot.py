#!/usr/bin/env python3
"""
MacroDroid (.mdr JSON backup) -> AutoPilot tasks JSON 转换器

支持映射:
  TimerTrigger            -> 每天 HH:mm + 星期 + 间隔
  ScreenOnAction          -> wake_screen
  LaunchActivityAction/LaunchAppAction -> launch_app (pkg)
  WaitAction              -> wait
  SetVolumeAction         -> set_volume
  ToggleWifiAction        -> toggle_wifi
  SetBTStateAction        -> toggle_bluetooth
  NotificationAction/DisplayNotificationAction -> notify

用法:
  python3 md_to_autopilot.py <backup.mdr> [out.json]
"""
import json, sys

def map_action(a):
    ct = a.get("m_classType", "")
    if ct == "ScreenOnAction":
        return {"type": "wake_screen"}
    if ct in ("LaunchActivityAction", "LaunchAppAction"):
        return {"type": "launch_app",
                "packageName": a.get("m_packageToLaunch", ""),
                "appLabel": a.get("m_applicationName", "")}
    if ct == "WaitAction":
        return {"type": "wait", "waitSeconds": int(a.get("m_seconds", a.get("m_secondsDelay", 5)) or 5)}
    if ct == "SetVolumeAction":
        # MacroDroid volume is 0-32767 ratio-ish; clamp to percent if available
        v = a.get("m_volume")
        pct = 50
        if isinstance(v, (int, float)):
            pct = max(0, min(100, round(v / 327.67)))
        return {"type": "set_volume", "volumePercent": pct}
    if ct in ("ToggleWifiAction", "SetWifiStateAction"):
        on = a.get("m_enable", a.get("m_state", 1))
        return {"type": "toggle_wifi", "turnOn": bool(on)}
    if ct in ("SetBTStateAction",):
        return {"type": "toggle_bluetooth", "turnOn": bool(a.get("m_state", 1))}
    if ct in ("NotificationAction", "DisplayNotificationAction"):
        return {"type": "notify", "message": a.get("m_text", a.get("m_title", "MacroDroid 通知"))}
    return None

def convert_macro(m):
    trig = None
    for t in m.get("m_triggerList", []):
        if t.get("m_classType") == "TimerTrigger":
            trig = t; break
    if trig is None:
        return None  # 只转换定时任务
    actions = []
    for a in m.get("m_actionList", []):
        mapped = map_action(a)
        if mapped:
            actions.append(mapped)
    if not actions:
        actions = [{"type": "wake_screen"}]
    return {
        "name": m.get("m_name", "MacroDroid任务"),
        "enabled": bool(m.get("m_enabled", True)),
        "hour": int(trig.get("m_hour", 7)),
        "minute": int(trig.get("m_minute", 0)),
        "daysOfWeek": trig.get("m_daysOfWeek", [True]*7),
        "intervalMinutes": 0,
        "actions": actions,
    }

def main():
    if len(sys.argv) < 2:
        print("usage: md_to_autopilot.py <backup.mdr> [out.json]"); sys.exit(1)
    data = json.load(open(sys.argv[1], encoding="utf-8"))
    macros = data.get("macroList", [])
    tasks, skipped = [], 0
    for m in macros:
        if m.get("isActionBlock") or m.get("isExtra"):
            continue
        t = convert_macro(m)
        if t: tasks.append(t)
        else: skipped += 1
    out = sys.argv[2] if len(sys.argv) > 2 else "autopilot_tasks.json"
    json.dump(tasks, open(out, "w", encoding="utf-8"), ensure_ascii=False, indent=2)
    print(f"转换完成: {len(tasks)} 个任务, 跳过 {skipped} 个(非定时/动作块) -> {out}")
    for t in tasks:
        print(f"  - {t['name']}  {t['hour']:02d}:{t['minute']:02d}  actions={[a['type'] for a in t['actions']]}")

if __name__ == "__main__":
    main()
