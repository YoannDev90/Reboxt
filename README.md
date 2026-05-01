# Reboxt

> [!TIP]
> **Elevated Access Support**: Reboxt now supports **Root** and **Shizuku** (ADB) to bypass OEM restrictions and directly modify system power schedules on Xiaomi/HyperOS and other devices.

A powerful, FOSS (Free and Open Source Software) Android application built with Jetpack Compose to schedule native system power on/off events.

## Features
- **Minimalist UI**: Modern Jetpack Compose interface.
- **Root & Shizuku Integration**: Unified shell execution engine to bypass permission denials for system services.
- **Advanced Explorer**: Search and compare hidden system settings (`Secure`, `Global`, `System`) to find hardware-specific power properties.
- **Unified Logging**: Persistent system-level activity logs with easy export/share capabilities.
- **Device Support**: Optimized for Xiaomi/HyperOS, MediaTek-based devices (OPPO, Realme, Vivo), and generic Android system power alarms.

## How it works
Reboxt targets the hardware-level power scheduler by:
1.  **Setting Provider Values**: Modifying `power_on_alarm_time` and `power_off_alarm_time` in system settings.
2.  **Service Interaction**: Using Shizuku/Root to start internal system services like `BootAlarmIntentService` on Xiaomi devices, which standard apps cannot access (UID 1000 required).
3.  **Broadcasts**: Triggering OEM-specific change events (e.g., `com.miui.powercenter.SET_POWER_ON_OFF`).

## Installation & Setup
1.  **Direct Download**: Build the APK using `./gradlew assembleDebug`.
2.  **Permissions**:
    *   **Shizuku (Recommended)**: Install the Shizuku app from Play Store/GitHub and grant access to Reboxt for ADB-level privileges without Root.
    *   **Root**: Grant SU access for full system control.
    *   **Write Settings**: Grant the system permission via the app settings.

## Credits
- **Nuricon**: Application Icon (via Flaticon).
- **RikkaApps**: Shizuku API.
- **YoannDev**: Main implementation.

## Build Instructions
1. Open this project in Android Studio or VS Code with the Android extension.
2. Run `./gradlew assembleDebug` to build the APK.

## License
MIT License
