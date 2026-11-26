# PU (Project UNIVERSAL) — Design Specification

> **Press play. PU handles the rest.**

PU is an Android game runtime for Ren'Py and RPG Maker games. This document turns the supplied HTML design document into an implementation-ready Markdown specification for Kotlin and Jetpack Compose.

| Field | Value |
|---|---|
| Product | PU (Project UNIVERSAL) |
| Editions | PU Lite and PU Plus, one shared core |
| Platform | Android 7.0 / API 24 and newer |
| UI stack | Kotlin and Jetpack Compose |
| Design status | Version 1, implementation-ready |
| Source date | 24 November 2026 |
| Primary visual reference | [`attachments/9bd70ec5-2c30-41ce-9667-6ad11dd47367/PU_Design_Document.html`](attachments/9bd70ec5-2c30-41ce-9667-6ad11dd47367/PU_Design_Document.html) |
| Requirement sources | IRD, PRD and TRD, revision 2 |

## 1. Product intent

PU is a player's tool. It is used on budget phones, often with one hand, while the player is trying to play a game rather than administer software.

The interface must:

1. Open quickly and stay out of the game's way.
2. Detect a game's engine and version without asking the player to choose a runtime.
3. Warn before a game starts when the phone or a runtime is unsuitable.
4. Explain a one-time optimization pass with real counts and real savings.
5. Keep the original game files untouched.
6. Remain legible in a dark room and on a small, low-power display.

The product promise is **Press play. PU handles the rest.** The interface itself uses plain labels and first-person copy only inside the Narrator.

### 1.1 Design principles

| Principle | Product rule | Interface consequence |
|---|---|---|
| Fast before beautiful | The interface must be inexpensive to draw and load | Flat surfaces, hairlines, vector icons, lazy cover thumbnails, no blur, gradients, animated backgrounds or heavy shadows |
| Warn early, in plain words | A crash must not be the first sign of a problem | Every compatibility, GPU, memory, storage and thermal check happens before launch and produces a readable result |
| One memorable thing | The Narrator is the only ornate element | Everything around it stays quiet; one Narrator box per screen |
| One app, two editions | PU Plus adds capability, not a second visual language | Both editions share every screen, token and component; Plus-only features appear as quiet rows or enabled states |
| Real state, not decoration | The UI shows what the player will actually get | Covers, engine versions, runtime requirements, estimates, cache sizes and optimization counts are live data |
| Nothing important is hidden | Meaning never depends on color alone | Every status pairs color with a word and, where useful, an icon |

### 1.2 Taste Skill application

The `design-taste-frontend` skill was loaded and applied as the design-quality framework for this specification.

| Taste Skill principle | PU adaptation |
|---|---|
| One design system | One Compose theme, one type scale, one spacing scale, one radius family and one component set for both editions |
| Plain copy with a voice of its own | Short sentences, active voice, sentence case, concrete game and runtime nouns, no marketing filler |
| Show the real product | The design documents the actual library, game sheet, optimizer, runtimes, module gate and settings states rather than generic dashboard cards |
| Every state must render | No blank cover, empty progress panel or placeholder-only screen; failures and missing assets have deliberate fallbacks |
| Quiet interaction | One ambient Narrator marker and short state transitions; no decorative motion, looping counters or attention-seeking animation |
| Accessibility is structural | Contrast, text scaling, touch size, focus, labels and non-color status encoding are release requirements |
| Honest content | No invented reviews, performance claims, pricing or compatibility results; illustrative fixture names and numbers are identified as such |

The client already supplies a deliberate palette and type direction, so the Taste Skill's palette dealer is not used. Its website-specific shader, scroll-scene and browser-gate features do not apply to this native utility interface or this Markdown deliverable.

## 2. Information architecture

### 2.1 Primary navigation

PU has three top-level destinations, shown in a persistent bottom navigation bar:

| Destination | Purpose | Primary content |
|---|---|---|
| **Library** | Browse and open installed games | Covers, engine version and compatibility estimate |
| **Runtimes** | Manage downloaded engine runtimes | Installed, available and unsupported runtimes |
| **Settings** | Configure the app and edition features | Performance profile, downloads, background work and Plus features |

The bottom navigation is 64 dp tall, sits on Ink, and uses a one-pixel top hairline. The current destination uses Mist text and its icon; inactive destinations use Dim. Every destination has both an icon and a text label.

### 2.2 Screen map

| Screen | Route or destination | Main action | Source |
|---|---|---|---|
| Library | `library` | Add game | HTML design, screen 1 |
| Game sheet | `library/game/{id}` | Play | HTML design, screen 2 |
| Optimizing | `library/game/{id}/optimizing` | Pause or resume | HTML design, screen 3 |
| Runtimes | `runtimes` | Download runtime | HTML design, screen 4 |
| Windows games module | `runtimes/windows-module` | Download module | HTML design, screen 5 |
| Settings | `settings` | Save selection | HTML design, screen 6 |

### 2.3 Global screen rules

- The screen margin is 16 dp on both sides.
- At most one filled gilt action appears on a screen.
- Secondary actions use the ghost style: Lacquer fill, Hairline border and Mist text.
- A bottom sheet slides up from the bottom edge. It never replaces the screen without an explicit choice.
- The content scrolls vertically; horizontal scrolling is limited to intentional chip rows.
- A fixed bottom navigation or action area must never cover content, labels or controls.
- Every screen has a top app region with the PU wordmark or a plain screen title.
- The edition badge appears beside the wordmark: outlined for Lite, gilt-outlined for Plus.
- Every screen supports back navigation, except the three root destinations.
- Content is state-driven. The screen must render meaningful content in loading, populated, empty, blocked and failed states.

## 3. Visual system

### 3.1 Color

The palette combines deep indigo lacquer with a single gilt accent. The signal colors are reserved for compatibility and status.

| Token | Value | Role | Compose name | Minimum contrast on stated ground |
|---|---:|---|---|---:|
| Ink | `#161A33` | App background, navigation and primary action text | `PuColors.background` | Mist 14.26:1; Dim 7.24:1; Gilt 7.60:1 |
| Lacquer | `#20254A` | Cards, inputs and Narrator surface | `PuColors.surface` | Mist 12.31:1; Dim 6.25:1; Gilt 6.56:1 |
| Lacquer high | `#2C3263` | Selected and pressed surfaces | `PuColors.surfaceHigh` | Mist 10.06:1; Dim 5.10:1; Gilt 5.36:1 |
| Hairline | `#353B73` | One-pixel dividers and outlines | `PuColors.outline` | Visual boundary, not body text |
| Mist | `#E9EAF4` | Primary text and icon color | `PuColors.onSurface` | 10.06:1 or better on all dark surfaces |
| Dim | `#A3A7C6` | Secondary text and inactive icons | `PuColors.onSurfaceVariant` | 5.10:1 or better on all dark surfaces |
| Gilt | `#CDA85A` | Primary action, focus ring and Narrator information frame | `PuColors.primary` | 5.36:1 or better on all dark surfaces |
| Celadon | `#8CCDB8` | Likely smooth and passed checks | `PuColors.success` | 6.62:1 or better on all dark surfaces |
| Madder | `#E0788C` | Warning and likely failure | `PuColors.danger` | Use for borders and large text on Lacquer high; small Madder text on Lacquer high is only 4.15:1 and is prohibited |

**Rules**

- Dark theme only at launch. A light theme is deferred until player research justifies it.
- Gilt is the only filled accent. It marks the one main action on a screen.
- Celadon and Madder never appear as decorative fills.
- Color never carries meaning alone. Every status uses a dot or icon plus a word.
- Mist is the default body color. Dim is reserved for metadata, labels and inactive controls.
- The primary action uses Ink text on Gilt. This gives a 7.60:1 contrast ratio.
- Text is never placed on a game cover without a tested, opaque reading surface.

### 3.2 Typography

PU uses two system font families so nothing downloads and no first-screen delay is introduced. The serif family gives the app its voice; the sans-serif family carries controls and operational data.

| Role | Family and weight | Size / line height | Use |
|---|---|---:|---|
| Display | Serif 500 | 32 / 38 sp | Short screen or section display line |
| Title | Serif 500 | 22 / 28 sp | Game title and sheet title |
| Subtitle | Sans 600 | 17 / 24 sp | Section and control-group subtitle |
| Body | Sans 400 | 15 / 22 sp | Explanations and operational copy |
| Narrator | Serif 400 | 15 / 23 sp | Narrator message |
| Label | Sans 500 | 12 / 16 sp | Metadata, status and runtime details |

**Rules**

- Type sizes are expressed in `sp` and follow the player's system font-size setting.
- Layouts remain usable at 200% text scaling.
- Sentence case is used for headings, buttons and labels.
- Text scales rather than truncating. A longer game title may occupy two lines; it must not become a clipped single line.
- Numerals use tabular alignment where they update in place, especially progress and cache-size rows.
- The system serif fallback order is: `Iowan Old Style`, `Palatino Linotype`, `Book Antiqua`, `Georgia`, `Noto Serif`, `serif`.
- The system sans fallback order is: `ui-sans-serif`, `system-ui`, `-apple-system`, `Segoe UI`, `Roboto`, `Helvetica Neue`, `Arial`, `sans-serif`.

### 3.3 Shape and spacing

| Token | Value | Use | Compose name |
|---|---:|---|---|
| Extra small | 4 dp | Icon-to-label micro gaps | `PuSpacing.xs` |
| Small | 8 dp | Tight internal gaps | `PuSpacing.sm` |
| Medium | 12 dp | Compact card and list gaps | `PuSpacing.md` |
| Base | 16 dp | Screen margin and default control padding | `PuSpacing.base` |
| Large | 24 dp | Section and card separation | `PuSpacing.lg` |
| Extra large | 32 dp | Major group separation | `PuSpacing.xl` |

| Shape | Radius | Use | Compose name |
|---|---:|---|---|
| Cover | 6 dp | Game cover art | `PuShapes.cover` |
| Control | 12 dp | Buttons, inputs, cards and standard panels | `PuShapes.control` |
| Sheet | 20 dp | Bottom sheets and large framed surfaces | `PuShapes.sheet` |
| Chip | Full | Filter chips, edition badge and compact status pills | `PuShapes.chip` |
| Narrator | 4 dp top-left, 14 dp elsewhere | Narrator's asymmetric signature | `PuShapes.narrator` |

**Rules**

- The screen margin is 16 dp.
- Every interactive target is at least 48 × 48 dp. A 44 dp visual field may sit inside a 48 dp touch target.
- The Narrator's asymmetric corner is the only exception to the radius scale.
- Cards and rows do not nest more than one level inside another framed surface.

### 3.4 Depth

There are no shadows.

Depth is expressed with:

1. A surface-color step from Ink to Lacquer to Lacquer high.
2. A one-pixel Hairline border where an edge must be explicit.
3. A change in fill or border for selection and focus.

This is cheaper to render than shadow layers and keeps the interface stable on low-end GPUs.

### 3.5 Icons

- Icons are 24 dp vector drawables or Compose `ImageVector` assets.
- Stroke width is 1.75 dp.
- Caps and joins are round.
- No raster icon assets are shipped.
- Icon-only actions require a content description. Bottom navigation and primary actions also keep visible labels.
- The initial icon set contains Library, Runtimes, Settings, Add, Done, Download, Back and a compatibility status dot.

### 3.6 Motion

Motion is functional and restrained. PU does not use looping counters, ambient background animation, scroll-triggered reveals, parallax or decorative shader effects.

| Moment | Behavior | Duration |
|---|---|---:|
| Narrator advance marker | A small gilt chevron nudges down and back | 1.6 s loop |
| Bottom sheet opens | Slides up from the bottom edge | 280 ms |
| Chip or segment selection | Fill changes in place | 120 ms |
| Progress update | Bar eases to its new value | 200 ms |
| Reduced motion | Marker stops and all state changes become instant | 0 ms |

All transitions use `CubicBezierEasing(0.2f, 0f, 0f, 1f)`.

**Motion rules**

- The Narrator marker is the only ambient motion in the app.
- A Narrator marker is hidden or static when its message has no detail view to open.
- Reduced-motion mode removes the marker animation and all non-essential transitions.
- No action moves because the user merely scrolled.
- State changes never depend on animation to be understood.

## 4. Voice and copy

### 4.1 Voice card

| Field | Rule |
|---|---|
| Register | Plain, exact, calm; technical only when the technical fact is useful |
| Person | Plain labels use the imperative. The Narrator alone uses first person |
| Sentence length | Usually one sentence; two at most in the Narrator |
| Tone | Never blames the player and never apologizes |
| Vocabulary | Use the exact engine, runtime, profile and module names |
| Buttons | Verb plus object, such as **Add game**, **Download runtime**, **Download module** and **Play** |
| Avoid | Hype, vague reassurance, blame, jokes, exclamation marks and invented performance claims |

### 4.2 Copy rules

- Use sentence case.
- Put the result before the mechanism: “This game might lag. I turned on the Low profile to help.”
- State what happened and what PU will do next.
- Use one wording for one fact throughout the app.
- Errors say what went wrong and what the player can do.
- Do not use “Learn more”, “Continue”, “OK”, or a bare noun as a primary button.
- Do not display a technical error code without a plain-language explanation.
- Do not present an estimated result as a measured result.
- Do not expose a sample note, template token or bracket in the interface.

### 4.3 Canonical message set

| Moment | PU says |
|---|---|
| Likely smooth | “Found Ren'Py 8.5. This game should run smoothly on your phone.” |
| Might lag | “This game might lag. I turned on the Low profile to help.” |
| Probably will not run | “This game needs more memory than this phone has, so it probably won't run.” |
| GPU fallback | “This phone lacks OpenGL ES 3.0, which Ren'Py 8.6 needs. I'll try the 8.5 runtime instead.” |
| Download action | Button: “Download runtime”. Result: “Runtime downloaded.” |
| Empty library | “No games yet. Add a folder or archive and PU will set it up.” |
| Unreadable folder | “Couldn't read this folder. Choose the folder that contains the game's files.” |

### 4.4 Button language

| Action | Label |
|---|---|
| Select a game folder or archive | **Add game** |
| Launch a prepared game | **Play** |
| Download an engine runtime | **Download runtime** |
| Download the Windows compatibility module | **Download module** |
| Remove a downloaded module | **Remove module** |
| Pause optimization | **Pause** |
| Resume optimization | **Resume** |
| Defer module installation | **Not now** |
| Clear generated cache | **Clear cache** |
| Apply a compatibility fix | **Apply fix** |

The same verb is used for the action and its confirmation. The app must not change “Play” to “Launch” in one place and “Start” in another.

## 5. The Narrator

The Narrator borrows the form of a visual-novel text window. It is the one memorable element in the interface and the only place where PU speaks in the first person.

### 5.1 Anatomy

1. **Nameplate:** always `PU`.
2. **Message:** at most two sentences and about 140 characters.
3. **Advance marker:** a small gilt chevron when tapping the message opens details or a corrective action.
4. **Surface:** Lacquer with a variant-colored one-pixel frame.
5. **Plate:** the same variant color as the frame, with Ink text.

### 5.2 Variants

| Variant | Frame and plate | Meaning | Required companion text |
|---|---|---|---|
| Information | Gilt | A result, estimate, progress note or confirmation | The message itself |
| Warning | Madder | A fallback, weak-device warning or likely failure | A word such as “Warning” in the expanded details; never color alone |
| Success | Celadon | A completed check or ready state | The message itself |

The visible message may rely on its wording for meaning. Color is reinforcement, never the sole signal.

### 5.3 Behavior

- Show at most one Narrator on a screen.
- Never blame the player for an unsupported device, malformed folder or missing storage.
- Never say “Oops”, “Sorry” or “Something went wrong”.
- A warning that has a fallback states the fallback in the same message.
- A blocked launch must not begin in the background.
- Tapping the advance marker opens exact details or the setting that can change the result.
- The expanded detail repeats the result in words and adds the actionable fact.

### 5.4 Accessibility

- The message is one semantic text unit for screen readers.
- The advance marker is not announced separately when the whole card is actionable.
- When the card is clickable, the accessible label combines the message and action, for example “PU: This phone lacks OpenGL ES 3.0. Open device details.”
- The variant is available to accessibility services as a text label, not only as a color.
- Text may wrap to more lines at 200% scaling; the card grows vertically.

## 6. Screen specifications

### 6.1 Library

**Purpose:** show the player's games and make the next action obvious.

**Content order**

1. PU wordmark and edition badge.
2. Search field with the visible label or accessible name **Search your games**.
3. Filter chips: **All**, **Ren'Py**, **RPG Maker**, **.exe**.
4. Two-column cover grid.
5. **Add game** action above the bottom navigation.
6. Bottom navigation.

**Game tile**

- Cover: 3:4 aspect ratio, 6 dp radius.
- Title: one or two lines.
- Engine and version: for example `Ren'Py 8.6` or `MV`.
- Compatibility estimate: a status dot plus a word.
- The whole tile is one control with one action: open the game sheet.

**Compatibility labels**

The stored estimate model uses three states:

| Model value | Compact library label | Color |
|---|---|---|
| Likely smooth | Smooth | Celadon |
| Might lag | Might lag | Gilt |
| Probably won't run | Won't run | Madder |

The compact label is visual only. The accessible label uses the full model value so the meaning is not shortened for screen readers.

**States**

- Empty: the canonical empty-library message and **Add game** action.
- Loading: retain the header, search and filters while the grid loads; use placeholder geometry without a fake game result.
- Populated: the cover grid.
- Filtered with no result: say that the selected filter has no games and offer **Show all games**.
- Missing cover: a deterministic, non-empty title-and-engine fallback; never an empty rectangle.
- Detection failed: keep the game visible and show a plain failure state on its sheet.

**Acceptance criteria**

- The player can reach **Add game** in one action from Library.
- Every tile states engine and compatibility before the game is opened.
- The grid uses small, lazy-loaded thumbnails.
- Status is understandable without color.
- No two actions compete for the same filled gilt treatment.

**Requirement trace:** PRD FR-1, FR-6, FR-12, FR-14 and NFR-1; TRD sections 2, 3 and 4.

### 6.2 Game sheet

**Purpose:** show the compatibility decision and let the player launch with one action.

**Content order**

1. Game artwork with a circular Back control.
2. Game title.
3. Compatibility estimate and cache information on one row.
4. Narrator warning or fallback, when needed.
5. **Performance profile** label.
6. Segmented profile control: **Ultra Low**, **Low**, **Balanced**.
7. One full-width **Play** action.

**Rules**

- **Play** is the only filled gilt action on the sheet.
- The selected profile is shown with Lacquer high fill, a Hairline border and stronger text.
- A manual profile override persists for the game.
- The cache line compares optimized cache size with original size after a cache exists.
- The sheet explains a fallback before launch; it never allows the player to enter a knowingly unsupported state without explanation.
- The artwork is the player's cover, not a stock image or a drawn substitute presented as the game.

**States**

- Ready to play.
- Runtime must be downloaded.
- Runtime unsupported on this phone.
- Cache missing or incomplete.
- Estimate unavailable pending detection.
- Play request failed: keep the sheet and retry from the same action.

**Acceptance criteria**

- The player can see why a game may lag before pressing **Play**.
- The compatibility estimate, profile and fallback explanation appear together.
- The game never starts before the device gate completes.
- At most one gilt action is visible.

**Requirement trace:** PRD FR-6, FR-9, FR-12 and NFR-3; TRD sections 2, 4 and 7.

### 6.3 Optimizing

**Purpose:** make a one-time wait legible and reassure the player that the original files are untouched.

**Content order**

1. Screen title **Optimizing**.
2. Game name.
3. Progress bar with a numeric percentage.
4. Space saved so far.
5. Ordered work steps: scan files, images, audio and video.
6. Narrator note.
7. **Pause** or **Resume** action.

**Progress-step states**

| State | Indicator | Text |
|---|---|---|
| Done | Celadon check | `Done` |
| Active | Gilt ring | Real current count, for example `1,240 of 2,010` |
| Waiting | Hairline outline | `Waiting` |
| Failed | Madder warning plus word | A plain failure reason |
| Skipped | Neutral icon plus word | `Skipped` with the reason |

**Rules**

- Show real counts and real savings, not an indefinite spinner.
- The Narrator states that the original files stay untouched and that PU will pause if the phone gets hot.
- Progress uses the actual cached value; it does not animate beyond completion.
- Pause and resume use the same location and wording.
- The optimizer never runs while a game is running.
- The process is resumable after interruption.

**Acceptance criteria**

- The screen always identifies the current phase.
- The player can pause without losing completed work.
- The original game folder is never changed.
- A failure names the phase and gives a usable next action.

**Requirement trace:** PRD FR-8 to FR-10, NFR-5, NFR-6, NFR-9 and NFR-11; TRD section 5.

### 6.4 Runtimes

**Purpose:** make shared runtime management understandable and prevent avoidable downloads.

**Content order**

1. Screen title **Runtimes**.
2. Short explanation: **Downloaded once and shared by every game.**
3. Runtime list.
4. Bottom navigation.

**Runtime row**

- Runtime name and version family.
- Requirement or usage detail.
- Right-aligned status or action.
- One Hairline divider between rows; the final row has no trailing divider.

**Example content model**

| Runtime | Detail | Status / action |
|---|---|---|
| Ren'Py 8.6 | Needs OpenGL ES 3.0 | Not supported on this phone |
| Ren'Py 8.5 | Used by 1 game | Installed |
| Ren'Py 8.4 | Sample size 52 MB | Download |
| Ren'Py 7.8 | Used by 2 games | Installed |
| RPG Maker engines | MV/MZ, XP/VX/Ace, 2000/2003 | Installed |

The titles, usage counts and sizes above are fixture content, not product facts.

**Rules**

- An unsupported runtime states the missing requirement in words.
- A download shows its size before installation starts.
- A download is resumable and integrity-checked.
- Installed runtimes are shared across games and can be removed when unused.
- A runtime update installs beside the existing version and does not replace it in place.

**States**

- Not installed.
- Downloading, with byte progress and cancel action.
- Verifying.
- Installed.
- Update available.
- Unsupported on this device.
- Download failed, with retry action.
- Corrupt package rejected, with the package discarded.

**Acceptance criteria**

- The player can tell which runtimes are installed, available and blocked before tapping them.
- Unsupported versions explain why.
- A second launch of the same version never downloads it again.

**Requirement trace:** PRD FR-3 to FR-6 and NFR-10; TRD section 3.

### 6.5 Windows games module

**Purpose:** gate the optional Wine/Box64 module honestly and let the player decide.

**Content order**

1. Screen title **.exe games**.
2. Narrator warning.
3. Device-check list.
4. Download and unpacked-size note.
5. **Download module** primary action.
6. **Not now** ghost action.
7. **Remove module** when installed.

**Narrator warning**

> Experimental on 3 GB phones. Only simple 2D games are likely to run well.

**Device checks**

| Check | Hard result | Weak-device result |
|---|---|---|
| CPU architecture | 32-bit ARM blocks the module | arm64 passes |
| Free storage | Insufficient storage blocks installation | Adequate storage passes |
| Memory | — | 3 GB warns but does not block |

**Download note**

> Download up to 100 MB. Unpacked, the module needs more free space. Wi-Fi is recommended.

**Rules**

- Hard checks block; the memory check warns and leaves the decision with the player.
- The screen shows the compressed download size and the additional unpacked-space requirement.
- The module is never downloaded silently.
- Removal is always available after installation.
- The module is included in PU Plus and opt-in in PU Lite; the visual treatment stays the same.

**Acceptance criteria**

- A 32-bit ARM phone is blocked before download.
- A 3 GB arm64 phone sees a warning, not a hard block.
- The download action states what is downloaded and how large it is.
- The module can be removed in one action.

**Requirement trace:** PRD FR-17 and NFR-8; TRD section 6.

### 6.6 Settings

**Purpose:** expose real configuration without hiding Plus-only features in a sales modal.

**Content order**

1. Screen title **Settings**.
2. **Performance profile** group.
3. Device and download toggles.
4. Edition-gated rows.
5. Bottom navigation.

**Performance profiles**

| Profile | Description | PU Lite default | PU Plus default |
|---|---|---:|---:|
| Ultra Low | About 540 to 720p, lite video, smallest cache | Offered on weaker phones | Available when manually selected |
| Low | 720p assets, capped image cache | Default | Available |
| Balanced | Near-original quality, light tuning | Available | Default |

The **Recommended** tag marks the profile selected from the device tier. The tag is a small Gilt pill with Ink text.

**Toggles**

- Pause optimizing when hot.
- Download on Wi-Fi only.

**Edition rows**

- **Sync and translation** appears as a quiet row marked **PU Plus** in Lite.
- In Plus, the same capability opens full screens inside Settings and the game sheet.
- A Lite row is not a pop-up, a fake disabled dialog or a second navigation pattern.

**Acceptance criteria**

- The selected profile is obvious without color alone.
- A manual profile override persists.
- The same Settings hierarchy works in both editions.
- Plus-only rows never alter the surrounding layout.

**Requirement trace:** PRD FR-9, FR-13, FR-16 and FR-20; TRD sections 7 and 8.

## 7. Core flows

### 7.1 Add and play

The main path has six steps. Branch behavior reuses the current screen and Narrator; it does not add a separate wizard.

| Step | Player action or system action | UI result |
|---|---|---|
| 1. Add game | Choose a folder or archive | PU begins detection |
| 2. Detect | PU identifies engine and version | Library entry or game sheet states the engine |
| 3. Runtime | PU checks for the matching runtime | Download only when missing; size shown first |
| 4. Estimate | PU checks the device and chooses a profile | Narrator states likely result and fallback |
| 5. Optimize | PU builds the cache once | Optimizing screen shows real progress and savings |
| 6. Play | Player presses Play | Game starts in its sandbox |

### 7.2 Pre-launch gate

All checks run before the game starts:

1. Confirm that the runtime exists.
2. Confirm that the runtime's Android minimum is met.
3. Confirm that the runtime's GPU requirement, such as OpenGL ES 3.0, is met.
4. Choose a profile from the device tier.
5. Build or reuse the optimized cache.
6. Launch only after every blocking check passes.

A failed gate produces a Narrator explanation and leaves the game stopped.

### 7.3 Runtime fallback

When the newest runtime cannot run on the phone but an older compatible runtime is available for that game:

1. The game sheet shows the warning before launch.
2. The Narrator names the missing feature and the fallback in one message.
3. The player chooses **Play** to accept the fallback or leaves the sheet.
4. PU records the selected runtime with the game so the choice is not repeated silently.

### 7.4 Windows module gate

1. The player opens the module screen.
2. PU checks CPU architecture and free storage.
3. A 32-bit ARM device is blocked; a 3 GB device receives a memory warning.
4. PU shows compressed size, unpacked-space need and Wi-Fi recommendation.
5. The player chooses **Download module** or **Not now**.
6. After installation, **Remove module** is always available.

### 7.5 Game misbehavior

1. The player opens the game's settings.
2. PU offers a matching fix profile from the compatibility database, or a lower optimization profile.
3. The player applies the fix in one action and relaunches.

The first version of this flow may be represented inside the game sheet's settings area; it must not introduce a separate visual language.

## 8. PU Lite and PU Plus

The editions share one design. Differences are about what is switched on.

| Area | PU Lite | PU Plus |
|---|---|---|
| Edition badge | Outlined chip beside the wordmark | Gilt-outlined chip beside the wordmark |
| Default profile | Low, with Ultra Low offered on weaker phones | Balanced |
| Windows games module | Opt-in sheet with a warning | Included and switched on |
| Sync and translation | One quiet Settings row marked PU Plus | Full screens in Settings and the game sheet |
| Background work | None while idle | Optimizes while charging |
| Image-cache target | Approximately 250–350 MB | Approximately 500–800 MB |
| Save sync | Basic local backup | Cloud or local sync |

Pricing is undecided. There are no paywall screens in this design version.

A first-launch device check recommends an edition but never blocks the other edition. Cache and save formats are identical so a player can move between editions.

## 9. Accessibility and performance

### 9.1 Accessibility floor

- Body text has at least 4.5:1 contrast; controls and meaningful graphics have at least 3:1.
- Every interactive target is at least 48 × 48 dp.
- State is never color alone: a dot or icon is paired with a word.
- Every control has a screen-reader name.
- Text scales to 200% without clipping, overlap or loss of action.
- Focus or keyboard selection is visible and uses the Gilt focus treatment.
- Reading order follows the visual order.
- Motion respects the system's reduced-motion setting.
- The Narrator's variant is exposed as text, not only as color.
- Game covers have meaningful alternative text based on the game title; decorative fallback artwork is marked decorative.

### 9.2 Interface speed budget

| Budget | Target | Source |
|---|---:|---|
| Cold start to Library | Within 2 seconds | PRD NFR-1 |
| Engine and version detection | Within 3 seconds | PRD NFR-2 |
| Optimized game to first interactive screen | Within 10 seconds | PRD NFR-3 |
| Scene-transition frame rate on 3 GB phone | Approximately 30 fps | PRD NFR-4 |
| One-hour heavy-game soak | No out-of-memory crash | PRD NFR-5 |
| Base app | Under approximately 15 MB | PRD NFR-7 |

### 9.3 Interface implementation rules

- Load no external fonts or icon packs.
- Use small downscaled cover thumbnails in a lazy grid.
- Keep native libraries memory-mapped and uncompressed where the platform permits.
- Do no heavy work on the main thread.
- Do not start background services while idle in Lite.
- Avoid expensive clipping, blur, backdrop filtering, animated gradients and shadow layers.
- Keep the app usable when the player is offline and no runtime catalog sync is available.

## 10. Jetpack Compose handoff

### 10.1 Theme

```text
PuColors.background      #161A33
PuColors.surface         #20254A
PuColors.surfaceHigh     #2C3263
PuColors.outline         #353B73
PuColors.onSurface       #E9EAF4
PuColors.onSurfaceVariant #A3A7C6
PuColors.primary         #CDA85A
PuColors.success         #8CCDB8
PuColors.danger          #E0788C
```

The theme exposes color, typography, shapes and spacing as one token set. Screen code must not introduce a second local color or spacing scale.

### 10.2 Components

Build the following component family first:

1. `PuNarratorCard`
2. `PuGameTile`
3. `PuEstimateChip`
4. `PuProfileCard`
5. `PuRuntimeRow`
6. `PuProgressStep`
7. `PuPrimaryButton`
8. `PuGhostButton`
9. `PuSegmentedControl`
10. `PuBottomNavigation`
11. `PuAddGameButton`

Each component must define its states before it is reused. The minimum state set is default, selected, disabled, loading and error where applicable.

### 10.3 Suggested component boundaries

| Component | Owns | Does not own |
|---|---|---|
| `PuNarratorCard` | Variant, message, optional advance action | Screen navigation or compatibility logic |
| `PuGameTile` | Cover, title, engine and estimate presentation | Game detection or launch |
| `PuEstimateChip` | Dot, visible word and accessible full state | Compatibility calculation |
| `PuProfileCard` | Profile label, description and selection | Device-tier calculation |
| `PuRuntimeRow` | Runtime details and row-level status | Download orchestration |
| `PuProgressStep` | One optimizer step and its state | Optimizer scheduling |
| `PuSegmentedControl` | Options and selected value | Profile persistence |

State that affects a whole screen belongs in the screen's ViewModel or state holder. Components receive values and callbacks; they do not query repositories directly.

### 10.4 Editions in Compose

Use separate `lite` and `plus` Gradle application modules over one shared Compose library. Each module has its own application ID, launcher label, version suffix, and edition-specific capability configuration. The UI consumes capabilities and must not branch on an edition name deep inside components.

## 11. Definition of done

A screen is ready for release only when all applicable items pass.

### 11.1 Design fidelity

- It uses only the shared color, type, spacing and shape tokens.
- It has one button family, one card family and one clear focus treatment.
- It contains no second visual style for PU Plus.
- It contains no blur, shadow, gradient, ambient animation or decorative effect.
- Every state renders real content; no placeholder token reaches the UI.
- The primary action is obvious and appears at most once in filled Gilt.

### 11.2 Content

- Copy uses the canonical terms and verbs.
- Every error says what happened and what to do next.
- No copy blames the player or apologizes.
- All numbers carry a unit and are labeled as measured or estimated.
- The Narrator stays within two sentences and about 140 characters.
- Sample game names, counts and sizes are replaced by live or fixture data in the implementation preview.

### 11.3 Accessibility

- Contrast and touch-size requirements pass.
- All controls have names.
- State has a non-color signal.
- 200% text scaling produces no clipping or overlap.
- Reduced motion leaves every state understandable.
- Focus order follows reading order.

### 11.4 Product behavior

- The screen never starts an unsupported game.
- A runtime is downloaded only when missing.
- Optimizer output is separate from original files.
- The original files remain byte-for-byte unchanged.
- The screen distinguishes loading, empty, populated, blocked and failed states.
- Lite and Plus render the same hierarchy.

## 12. Open decisions

| Decision | Current design | Needed to close it |
|---|---|---|
| Final app icon | Working idea: a Gilt PU monogram on Ink | Final vector artwork and Android launcher-icon set |
| Paid-tier presentation | No paywall screens | Pricing model and selected Plus features |
| Light theme | Dark only at launch | Player evidence that a light theme is needed |
| Windows module memory cap | 3 GB shows a warning | Owner decision on any cap stricter than the shared warning |
| Distribution channel | Module-delivery layer keeps the design channel-agnostic | First release channel and its runtime-delivery rules |

## 13. Requirement traceability

| Design area | PRD / NFR | TRD | IRD |
|---|---|---|---|
| Library and game discovery | FR-1, FR-14 | Sections 2, 3, 4 | R2, R4 |
| Runtime rows and downloads | FR-3 to FR-5, NFR-10 | Section 3 | R2, R3, R10 |
| Pre-launch Narrator and estimate | FR-6, FR-12 | Section 2 | R9 |
| Game sheet and profile override | FR-9, NFR-3 | Sections 2, 4, 7 | R4, R5 |
| Optimizing screen | FR-8 to FR-10, NFR-5, NFR-6, NFR-9, NFR-11 | Section 5 | R5, R7 |
| Runtimes screen | FR-4, FR-5, NFR-10 | Section 3 | R3, R10 |
| Windows games module | FR-17, NFR-8 | Section 6 | R11 |
| Settings and edition behavior | FR-9, FR-13, FR-16, FR-20 | Sections 7, 8 | R8, R13, R14 |
| Visual tokens and Compose mapping | NFR-1, NFR-7, NFR-12 | Sections 1, 4, 7 | R1, R4, R10 |
| Accessibility and performance floor | NFR-1 to NFR-14 | Sections 4, 7, 9 | R4, R5 |

## 14. Source and validation note

This specification was derived from:

- `PU_Design_Document.html` — the visual source of truth for palette, type, shape, motion, Narrator, screens, copy and Compose mapping.
- `Project_UNIVERSAL_IRD.txt` — product purpose, scope, editions, requirements, assumptions and open questions.
- `Project_UNIVERSAL_PRD.txt` — player behavior, functional requirements, performance targets, editions, flows and release scope.
- `Project_UNIVERSAL_TRD.txt` — Android architecture, runtime catalog, optimizer, device tiers, security, testing and technical constraints.

No Android application was built and no Gradle, Android SDK or `aapt2` build was run for this Markdown deliverable. Native screen review and build validation must be performed after the Android development stack is added in PocketDev Settings.
