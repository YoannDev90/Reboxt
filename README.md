# Reboxt

> [!IMPORTANT]
> **Research in progress**: Direct access to hardware settings via `WRITE_SECURE_SETTINGS` is inconsistent across different devices. Currently, there is no guarantee that modifications will be successfully applied to your specific device.

A minimal, FOSS (Free and Open Source Software) Android application built with Jetpack Compose to schedule system reboots.

## Features
- **Minimalist UI**: Built with modern Jetpack Compose.
- **Pure FOSS**: No trackers, no bloat.
- **Native Integration**: Directly deep-links into the hardware-level "Scheduled Power On/Off" settings (MediaTek, MIUI, ColorOS, etc.), which allows the phone to fully power down and wake up automatically.

## How it works
Most Android phones (especially those with MediaTek chips) have a hidden system activity that manages hardware-timed power events. This app scans for these vendor-specific activities (common in Xiaomi, OPPO, Realme, Vivo, and generic MTK devices) to let you bypass deep nested menus.

## Build Instructions
1. Open this project in Android Studio or VS Code with the Android extension.
2. Run `./gradlew assembleDebug` to build the APK.
3. Install on a device with `adb install app/build/outputs/apk/debug/app-debug.apk`.

## License
MIT License
