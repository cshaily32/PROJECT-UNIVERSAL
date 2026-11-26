# Project UNIVERSAL

PU is a native Android game runtime for Ren'Py and RPG Maker games. This repository contains the **PU Lite** and **PU Plus** Android applications and their shared Kotlin/Jetpack Compose implementation. The former React prototype, its dependencies, and its generated web build have been removed.

## Repository layout

| Path | Purpose |
| --- | --- |
| `PU-main/lite/` | Standalone PU Lite application (`SGYT.PU_LITE`) |
| `PU-main/plus/` | Standalone PU Plus application (`SGYT.PU_PLUS`) |
| `PU-main/shared/` | Shared Compose screens, navigation, ViewModels, services, and tests |
| `PU-main/branding/` | Supplied launcher-icon masters and Android resource mapping |
| `PU-main/PU_SCREENS.md` | Screen, feature, requirement, and edition map |
| `PU-main/PU_API.md` | Native service paths and build configuration |
| `design.md` | Product UI and feature specification used by the native implementation |

Lite and Plus are separate Android application modules with different application IDs, so both APKs can be installed on the same device.

## Development requirements

Add the **Android development stack in PocketDev Settings** before building. The project expects:

- JDK 17
- Gradle 8.9 or newer
- Android SDK 35 and build tools

This PocketDev runtime does not include Gradle, Java, the Android SDK, or aapt2. The Android project therefore has not been compiled here, and the commands below are not build-verified in this environment.

## Build from this repository root

```bash
gradle -p PU-main :lite:assembleRelease
gradle -p PU-main :plus:assembleRelease
```

Build both release APKs in one command:

```bash
gradle -p PU-main assembleEditions
```

APK output is written under `PU-main/lite/build/outputs/apk/release/` and `PU-main/plus/build/outputs/apk/release/` after a successful Android build.
