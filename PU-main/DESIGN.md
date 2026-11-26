# PU native UI system

PU Lite and PU Plus use the same native Compose design system. Product content and behavior come from `design.md` and the PU IRD, PRD, and TRD.

## Visual foundations

- **Backdrop:** pale sage background with the shared Material 3 color scheme.
- **Surfaces:** light cards with restrained forest outlines and elevation.
- **Primary surfaces:** deep forest cards for the key decision or current status.
- **Accent:** mint for ready states and honey for one active progress or warning state.
- **Type:** Material 3 typography with a native sans-serif family.
- **Shape:** rounded cards, full-pill segments and filters, and consistent touch targets.
- **Navigation:** floating bottom pill; the active destination is visually selected.
- **Icons and controls:** Material Icons and Compose controls shared by both editions.

The palette and typography tokens are defined in `shared/src/main/java/sgyt/pu/ui/theme/PuTheme.kt`. Reusable controls are defined in `shared/src/main/java/sgyt/pu/ui/components/PuComponents.kt`.

## Rules

- Color never carries status alone; every compatibility and device state includes a word and icon.
- Unsupported runtime requirements block launch. Automatic fallback behavior remains a product decision and is not implied by the UI.
- PU Lite and PU Plus share the same layout; edition differences appear as state, badges, included features, and recommendations.
- Each service-backed screen owns an MVI ViewModel and must expose loading, success, empty, and error states.
- File and network work must not run on the main thread.
