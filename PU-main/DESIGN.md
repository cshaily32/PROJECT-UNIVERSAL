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
- **Depth:** a hairline outline, never a shadow. Shadows cost a render layer on the low-end phones
  this app targets, and a border under a shadow reads as a ghost card.

## Tokens

The palette and typography tokens are defined in `shared/src/main/java/sgyt/pu/ui/theme/PuTheme.kt`,
the spacing and status scale in `shared/src/main/java/sgyt/pu/ui/theme/PuTokens.kt`, and the
reusable controls in `shared/src/main/java/sgyt/pu/ui/components/PuComponents.kt`.

- **Spacing:** one scale — 4, 8, 12, 16, 24, 32 dp — used for every screen margin, card padding
  and section gap. The floating navigation bar publishes its own clearance so screens clear it by
  the same amount.
- **Status:** ready, warning and danger each have a container plus the ink that reads on it, in
  both themes. Material 3 has no success/warning pair, so PU carries its own.
- **Progress:** the amber accent at a tone that reads against the surface underneath it. The bar
  sits on the primary card in the current design, so it uses the pair built for that surface.
- **Ink:** text takes its colour from the surface it sits on rather than from a fixed role, so a
  line reads in cream on the forest card and in the standard foreground on a pale card.

## Rules

- Color never carries status alone; every compatibility and device state includes a word and icon.
- Body and supporting text must reach 4.5:1 against the surface behind it, and an icon or a
  progress bar must reach 3:1.
- Unsupported runtime requirements block launch. Automatic fallback behavior remains a product decision and is not implied by the UI.
- PU Lite and PU Plus share the same layout; edition differences appear as state, badges, included features, and recommendations.
- Each service-backed screen owns an MVI ViewModel and must expose loading, success, empty, and error states.
- File and network work must not run on the main thread.
