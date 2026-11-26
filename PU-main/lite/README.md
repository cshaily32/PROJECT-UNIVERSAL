# PU Lite Android application

PU Lite is a standalone Android application module. It shares the Compose UI in `../shared` but owns its own launcher activity, launcher label, version suffix, and application ID.

- Application ID: `SGYT.PU_LITE`
- Namespace: `sgyt.pu.lite`
- Launcher name: PU Lite
- Launcher icon: [`../branding/pu-lite.svg`](../branding/pu-lite.svg), packaged as `src/main/res/drawable/ic_launcher.xml`
- Minimum Android version: Android 7.0 / API 24
- Default optimization profile: Low

After adding the Android development stack in PocketDev Settings, build it from the project root:

```bash
gradle -p PU-main :lite:assembleRelease
```

The release APK is generated under `PU-main/lite/build/outputs/apk/release/`.
