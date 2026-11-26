# PU — native Android applications

PU is an Android game runtime for Ren'Py and RPG Maker games. This Gradle project builds two independent applications from one shared Jetpack Compose implementation:

| Module | Application ID | Launcher name | Purpose |
| --- | --- | --- | --- |
| [`lite`](./lite) | `SGYT.PU_LITE` | PU Lite | Low-power edition with Lite capability flags |
| [`plus`](./plus) | `SGYT.PU_PLUS` | PU Plus | Full edition with Plus capability flags |
| [`shared`](./shared) | — | — | Shared Compose UI, navigation, ViewModels, services, and tests |

The modules have different application IDs, so both APKs can be installed on the same Android device at the same time.

## Native screens

The implementation covers the product flow defined by the IRD, PRD, TRD, and `design.md`:

- Device check
- Library and Add game
- Game details and launch check
- Runtime download and Runtimes
- Optimizing
- Compatibility fixes
- Touch controls and gamepad mapping
- Saves and backup
- Windows games module
- Translation overlay
- Background work
- Settings

See [`PU_SCREENS.md`](./PU_SCREENS.md) for the native route, requirement, and edition mapping.

## Build

Add the **Android development stack in PocketDev Settings** before building (JDK 17, Gradle 8.9 or newer, and Android SDK 35 for AGP 8.7.3). From the repository root:

```bash
gradle -p PU-main :lite:assembleRelease
gradle -p PU-main :plus:assembleRelease
```

Build both release APKs in one command:

```bash
gradle -p PU-main assembleEditions
```

The generated APKs are written under `PU-main/lite/build/outputs/apk/release/` and `PU-main/plus/build/outputs/apk/release/`. The shared screens are compiled into each APK, while each application module controls its launcher name, capabilities, version suffix, and application ID.

> The current PocketDev runtime does not contain Gradle, Java, the Android SDK, or aapt2. These Gradle commands are therefore not build-verified here, and no APK is claimed to exist yet.

## Native feature status

| Feature | Requirement | Native state |
| --- | --- | --- |
| Game import (folder or archive) | FR-1 | Implemented — system picker, copy into app-owned storage, on-device detection |
| Library with search and engine filters | FR-12 | Implemented — reads the persisted library from app storage |
| Engine and version detection | FR-14 | Implemented — `PuGameDetector` runs on imported files |
| Runtime download and reuse | FR-4, FR-5 | Implemented — signed catalog entries, real progress, cancel/resume, SHA-256 and signature checks, shared reuse, and removal; a published runtime URL/public key is still required |
| Launch checks and device gate | FR-6, R9 | Implemented — native device capability gate with runtime warnings and older-runtime fallback |
| Optimization pipeline | FR-8, OPT-1–12 | Implemented for images — resumable, non-destructive cache work with thermal and battery protection; video/audio encoding remains pending |
| Compatibility fixes | FR-19 | Implemented — offline fix database, engine/version matching, one-tap sandbox overlays, and optional signed API refresh |
| Touch controls and gamepad mapping | FR-15 | Implemented as a native configuration flow — persistent per-game/global profiles, touch placement, keyboard/gamepad capture, and input resolution; live engine overlay injection remains pending |
| Saves, OCR, background work | FR-16, FR-18, FR-20 | Prototype states |

The main native implementation is organized as follows:

- `shared/.../core/PuCoreGraph.kt` — composition root for storage, library, device inspection, runtime management, optimization, compatibility, controls, and launch coordination.
- `shared/.../core/library/` — local game import, detection, and persisted library records.
- `shared/.../core/runtime/` — signed catalog loading, resumable downloads, mandatory SHA-256/signature verification, installation, reuse, and removal.
- `shared/.../core/controls/` — persistent control profiles, connected-gamepad discovery, and touch/keyboard/gamepad action resolution.
- `shared/.../ui/viewmodels/` — MVI ViewModels with `State` / `Action` / `Event` contracts; file and network work runs off the main thread.
- `shared/.../ui/PuApp.kt` — shared Compose navigation and screen-to-ViewModel wiring for both editions.

The bundled catalog is intentionally empty until signed runtime packages are published. Saves, OCR, and background-work screens still contain prototype states until their native slices are implemented.

## API path configuration

The native client defines service paths in `shared/.../core/network/PuApiPaths.kt`:

- `catalog/runtime-catalog.json` and `.sig`
- `catalog/compatibility-fixes.json` and `.sig`
- `catalog/windows-modules.json`
- `catalog/translation-packs.json`
- `runtimes/{runtimeId}/{revision}/package`
- `games/{gameId}/save-backups`
- `games/{gameId}/control-profiles`

Remote services stay disabled when no API URL is supplied. To enable signed catalog refreshes, provide these Gradle properties to either edition:

```bash
gradle -p PU-main :lite:assembleRelease \
  -PU_API_BASE_URL=https://services.example.com \
  -PU_RUNTIME_CATALOG_PUBLIC_KEY=base64-public-key \
  -PU_COMPATIBILITY_PUBLIC_KEY=base64-public-key
```

These are client paths only; the corresponding backend and published signing keys are still required before remote refresh can succeed. See [`PU_API.md`](./PU_API.md) for the full contract.
