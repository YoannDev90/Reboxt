# HyperOS/MIUI Power Scheduling Discovery Process

This document details the reverse-engineering process used to identify the programmatic way to schedule Power On/Off events on Xiaomi devices running HyperOS (MIUI).

## 1. Initial Research & Hypotheses
Standard Android uses `AlarmManager`, but Xiaomi devices maintain power-on functionality even when forced off or in "Deep Sleep". This implies a hardware-level real-time clock (RTC) bridge.

### Target Identification
The investigation started by listing all providers and services related to power management:
```bash
adb shell pm query-content-providers | grep -i power
adb shell dumpsys package com.miui.securitycenter | grep -i bootshutdown
```
We identified `com.miui.powercenter.provider.PowerCenterProvider` as the primary data holder, but found that querying it directly was blocked by `SecurityException` due to missing `com.miui.powercenter.permission.POWER_DATA` permission.

## 2. Discovery Steps: The "Smoking Gun" Strategy

### A. Real-time Behavior Analysis (Logcat)
Instead of static analysis (obfuscated code), we switched to dynamic monitoring. By manually scheduling an event in the MIUI "Power" settings, we captured the exact sequence of internal calls.

```bash
# Monitoring the bridge between UI and Background Services
adb shell logcat -v time | grep -iE "BootAlarmIntentService|RESET_BOOT_TIME|setwakeuptime"
```

**Key Discovery:**
1.  **UI Level:** The fragment `PowerShutdownOnTime$PowerShutDownFragment` finishes with result data.
2.  **Service Level:** `BootAlarmIntentService` receives an Intent with action `com.miui.powercenter.RESET_BOOT_TIME`.
3.  **Hardware Level:** Immediately after, the system logs `setwakeuptime [timestamp]`.

### B. APK String Extraction (Key Hunt)
Because the codebase is obfuscated (methods like `ec.a.d`), we pulled the binary to find the literal "extra" keys that the service expects.

```bash
# 1. Identify and pull the APK
APK_PATH=$(adb shell pm path com.miui.securitycenter | head -n 1 | cut -d: -f2)
adb pull "$APK_PATH" securitycenter.apk

# 2. Extract technical keys
strings securitycenter.apk | grep -iE "boot_time|shutdown_time|boot_alarm"
```

**Results revealed:**
*   `boot_time`: The primary timestamp key.
*   `boot_time_day` / `boot_time_day_tomorrow`: Flags for repeating/single alarms.
*   `com.miui.powercenter.RESET_BOOT_TIME`: The exact action needed to trigger the update.

### C. The Hardware Bridge (Qualcomm Integration)
The Final step was understanding how MIUI talks to the hardware. HyperOS/MIUI acts as a proxy for the `com.qualcomm.qti.poweroffalarm` package.

*   **Sequence:** Reboxt -> SecurityCenter -> Qualcomm Bridge -> RTC.
*   **Action:** `org.codeaurora.poweroffalarm.action.SET_ALARM`
*   **Extra:** `time` (Long, Milliseconds)

## 3. Implementation Summary
To programmatically schedule an event, you must bypass the UI and talk directly to the `BootAlarmIntentService`.

### Final Intent Signature
*   **Action:** `com.miui.powercenter.RESET_BOOT_TIME`
*   **Component:** `com.miui.securitycenter/com.miui.powercenter.bootshutdown.BootAlarmIntentService`
*   **Extra:** `boot_time` (Unix Long in ms)

### Critical Findings for Reproduction
1.  **Resolved/Unresolved Diffs:** We used a `DiffEngine` to monitor `settings.db` (Global/System/Secure). We found that while some flags like `auto_shutdown_ontime` move, the *actual* hardware schedule is **not** stored in system settings but in the private `PowerCenterProvider.db` accessed via the service.
2.  **Permission Gap:** Standard apps cannot grant themselves `DEVICE_POWER`. For Reboxt, we implement this as an "Explorer" tool that generates the ADB command for the user or executes it if ROOT is granted.

### Programmatic Intent (System/Root only)
```kotlin
val intent = Intent("com.miui.powercenter.RESET_BOOT_TIME").apply {
    component = ComponentName("com.miui.securitycenter", "com.miui.powercenter.bootshutdown.BootAlarmIntentService")
    putExtra("boot_time", epochMillis)
}
context.startService(intent)
```

### ADB Command (Verification)
```bash
adb shell am start-service -a com.miui.powercenter.RESET_BOOT_TIME --el boot_time 1777611600000 com.miui.securitycenter/com.miui.powercenter.bootshutdown.BootAlarmIntentService
```

## 4. Nuances & Limitations
*   **Permissions:** Requires `android.permission.DEVICE_POWER` or `com.miui.powercenter.permission.POWER_DATA`.
*   **User ID:** On some HyperOS versions, you must specify the user (e.g., `--user 0` or `--user 10`) for the intent to be accepted.
*   **Precision:** The input is in milliseconds, but the hardware RTC often rounds to the nearest second.
