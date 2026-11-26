# PU Plus Android application

PU Plus is a standalone Android application module. It shares the Compose UI in `../shared` but owns its own launcher activity, launcher label, version suffix, and application ID.

- Application ID: `SGYT.PU_PLUS`
- Namespace: `sgyt.pu.plus`
- Launcher name: PU Plus
- Launcher icon: [`../branding/pu-plus.svg`](../branding/pu-plus.svg), packaged as `src/main/res/drawable/ic_launcher.xml`
- Minimum Android version: Android 7.0 / API 24
- Default optimization profile: Balanced

After adding the Android development stack in PocketDev Settings, build it from the project root:

```bash
gradle -p PU-main :plus:assembleRelease
```

The release APK is generated under `PU-main/plus/build/outputs/apk/release/`.
