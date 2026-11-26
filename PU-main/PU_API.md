# PU service paths

PU keeps remote services optional. With no API base URL, both apps use their bundled signed catalogs and remain fully offline.

## Build configuration

Set these Gradle properties for either edition:

| Property | Purpose |
| --- | --- |
| `PU_API_BASE_URL` | HTTPS origin that hosts PU's service files. It is blank by default. |
| `PU_RUNTIME_CATALOG_PUBLIC_KEY` | Base64 ECDSA public key used to verify the runtime catalog. This is a public key, not a private signing key. |
| `PU_COMPATIBILITY_PUBLIC_KEY` | Base64 ECDSA public key used to verify the compatibility database. |

Both Lite and Plus read the same values. They remain separate APKs because their application IDs and edition capability flags differ.

## Paths

All paths are relative to `PU_API_BASE_URL`.

| Method | Path | Native state |
| --- | --- | --- |
| `GET` | `catalog/runtime-catalog.json` | Wired into the Runtimes and Runtime download screens. |
| `GET` | `catalog/runtime-catalog.json.sig` | Detached signature for the exact runtime-catalog bytes. |
| `GET` | `catalog/compatibility-fixes.json` | Wired into the Compatibility fixes screen. |
| `GET` | `catalog/compatibility-fixes.json.sig` | Detached signature for the exact compatibility-fixes bytes. |
| `GET` | `catalog/windows-modules.json` | Reserved for the Windows/Box64 installer slice. |
| `GET` | `catalog/translation-packs.json` | Reserved for the PU Plus translation slice. |
| `GET` | `runtimes/{runtimeId}/{revision}/package` | Reserved canonical package path; published catalogs currently carry an absolute HTTPS `packageUrl`. |
| `GET` | `games/{gameId}/save-backups` | Reserved for the PU Plus cloud-sync slice. |
| `GET` | `games/{gameId}/control-profiles` | Reserved for optional control-profile sync. |

The two signed catalog paths and compatibility paths are the only remote paths currently consumed by native code. The other paths are centralized now so later features do not invent separate URL schemes.

## Signature contract

- The `.sig` file contains the Base64 detached signature for the exact bytes served by the matching catalog file.
- Verification uses ECDSA with SHA-256 through the project's API-24-safe `PuCrypto` implementation.
- A missing key, non-HTTPS URL, invalid signature, oversized response, or non-2xx response is rejected.
- Runtime package URLs stay inside the signed runtime catalog as absolute HTTPS URLs. PU does not rewrite them through the metadata base URL.

## Build example

After adding the Android development stack in PocketDev Settings, run from the project root:

```bash
gradle -p PU-main \
  -PU_API_BASE_URL=https://services.example.invalid \
  -PU_RUNTIME_CATALOG_PUBLIC_KEY=replace-with-public-key \
  -PU_COMPATIBILITY_PUBLIC_KEY=replace-with-public-key \
  :shared:testDebugUnitTest :lite:assembleDebug :plus:assembleDebug
```

`example.invalid` is intentionally non-functional. Replace it with the real HTTPS service origin before release.
