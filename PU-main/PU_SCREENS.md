# PU Lite and PU Plus screen map

This is the implementation map for the native Kotlin and Compose modules in `PU-main/lite`, `PU-main/plus`, and `PU-main/shared`. The shared visual language uses a sage backdrop, deep forest primary cards, honey progress, native typography, and a floating pill navigation bar.

## Shared flow

| Screen | Route | Purpose | Product coverage |
| --- | --- | --- | --- |
| Device check | `device-check` | Read the device tier and recommend an edition without blocking the other edition | FR-13, NFR-13 |
| Library | `library` | Browse detected games, filter by engine, and see the pre-launch estimate | FR-12, FR-14 |
| Add game | `add-game` | Choose a local folder or archive and show detection before setup | FR-1, UC1 |
| Game details | `game` | Choose a profile, inspect fixes, controls, saves and cache, then play | FR-9, FR-11, FR-14, FR-15, FR-16 |
| Runtime download | `runtime-download` | Show package size, verification, reuse and resumable download states | FR-4, FR-5, NFR-10 |
| Launch check | `device-gate` | Block an unsupported runtime before launch and offer a compatible fallback | FR-6, R9, Flow B |
| Optimizing | `optimize` | Show resumable asset work, saved space and heat or battery protection | FR-8, FR-10, OPT-1–12 |
| Compatibility fixes | `compatibility` | Apply a signed, offline-ready one-tap fix profile | FR-19, UC7 |
| Touch controls | `controls` | Configure the touch overlay, keyboard layout, haptics and gamepad mapping | FR-15 |
| Saves and backup | `saves` | Back up game saves and choose local or cloud storage where available | FR-16 |
| Runtimes | `runtimes` | Manage shared on-demand Ren'Py and RPG Maker runtimes | FR-2–FR-5, FR-7 |
| Windows games | `windows` | Gate and install the Wine/Box64 module, or remove it | FR-17, FR-18, WIN-1–11 |
| Settings | `settings` | Set profiles, network, backups, fixes and edition-specific behavior | FR-9, FR-13, FR-16, FR-19 |

## Edition behavior

The two builds share every screen and all copy stays in the same place. Only capability and defaults change.

| Area | PU Lite | PU Plus |
| --- | --- | --- |
| Default profile | Low, with Ultra Low available for weaker phones | Balanced |
| Windows module | Optional download with the device warning and removal action | Included and ready for supported 2D games |
| Save destination | Local backup | Local or cloud sync |
| Translation | Locked, quiet PU Plus state | Full offline OCR configuration |
| Background work | No work while idle | Optimize while charging and no game is running |
| Shared formats | Saves and caches use the same format in both editions | Saves and caches use the same format in both editions |

## Screen sequence for the main flow

1. `device-check`
2. `library`
3. `add-game`
4. `runtime-download` when a runtime is missing
5. `game`
6. `optimize`
7. `game` → Play
8. `device-gate` when the phone lacks a required GPU or Android feature

## Native implementation status

| Screen | Native state |
| --- | --- |
| Library | Real — reads the persisted library from app storage, with search and engine filters |
| Add game | Real — system folder/archive picker, copy into app storage, on-device detection |
| Device check | Real — reads the phone's processor, memory, storage, Android, graphics, and WebView facts |
| Game details | Real — persistent profile override, cache size, compatibility entry point, and cache clearing |
| Runtime download | Real — signed package metadata, live progress, cancel/resume, hash/signature checks, reuse, and removal |
| Launch check | Real — device capability gate with runtime warnings and blockers before launch |
| Optimizing | Real for images — non-destructive, resumable cache generation with thermal and battery protection; video/audio encoding remains pending |
| Compatibility fixes | Real — bundled offline fix database, engine/version matching, one-tap sandbox overlays, and optional signed API refresh |
| Touch controls | Real configuration — persistent per-game/global profiles, touch placement, keyboard and gamepad capture, and an input-resolution bridge; injecting the overlay into a live engine remains pending |
| Saves and backup | UI-only state |
| Runtimes | Real — reads the signed catalog and installed runtime records, with engine and installed filters plus an optional signed API refresh |
| Windows games | UI-only state |
| Translation overlay | UI-only state |
| Background work | UI-only state |
| Settings | UI-only state |

Runtime detection, optimization, compatibility fixes, sandboxing, save transfer, and OCR are being completed one native slice at a time. The remaining UI-only areas still use presentation states until their Kotlin/Compose implementation is finished.
