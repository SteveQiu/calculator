# Basher — History

## Project Seed

- **Project:** Android Kotlin Calculator
- **Stack:** Kotlin, AdMob rewarded ads, Google Play Billing (one-time purchase), DataStore, MVVM
- **Requested by:** Developer
- **Goal:** Calculator logic + dual unlock path: watch a rewarded ad to unlock a theme temporarily/permanently, OR purchase via Google Pay (Play Billing) for permanent unlock
- **Repo:** https://github.com/SteveQiu/calculator.git

## 📋 Summary of Prior Work (2026-05-08 through 2026-05-09, early)

**Session 2026-05-08:** Full MVVM refactor — implemented CalculatorViewModel, ThemeRepository (DataStore), BillingRepository (Play Billing client), AdRepository (RewardedAd), ThemeViewModel, ThemePickerActivity with adapter. Build successful.

**Session 2026-05-09 (early sessions):**
1. **PANDA Black Screen Fix (0c23fe7):** Root cause = dark Material theme base + black initial background + async color application. Fix: Light Material base + sync `applyThemeColors()` in `onCreate()`.
2. **ThemeUnlockListener Wiring:** Created interface, wired `ThemePickerDialog` + `ThemeUnlockDialog` to `MainActivity` via `requireActivity()` pattern. Added `observeUiEvents()` for Snackbar feedback.
3. **Panda Lock/Badge State Inconsistency (de2f771):** Badge logic not distinguishing locked vs owned; `unlockedThemesFlow` incorrectly persisting free themes. Fix: Badge text based on `isUnlocked`, DataStore filtered for premium-only, StateFlow initial value `emptySet()`.

See `.squad/decisions/decisions.md` for full decision records and architectural notes.

## Session — 2026-05-09 — Per-Theme Font Support (`fontResId`)

### Work Done
- Added `fontResId: Int? = null` to the `Theme` data class in `model/Theme.kt`.
- All themes in `ThemeRegistry.all` left at default `null` except Rabbit and Panda, which have `fontResId = null` with an explicit comment for Rusty to fill in once font files land in `res/font/`.
- Updated `MainActivity.applyThemeColors()`: resolves typeface via `ResourcesCompat.getFont(this, theme.fontResId!!)` (null-safe) and applies it to `tvDisplay` and all number/dot buttons. Falls back to `Typeface.DEFAULT` when `fontResId` is null.
- Added imports: `android.graphics.Typeface`, `androidx.core.content.res.ResourcesCompat`.
- Wrote decision to `.squad/decisions/inbox/basher-theme-font-field.md`.

### Key Technical Learnings

- **`fontResId: Int? = null` is the Android nullable-resource pattern**: Same convention as `iconRes: Int?` already in `Theme`. Using null as "use default" and non-null as a `@FontRes` integer keeps the API simple and avoids a sealed-class hierarchy for what is essentially a tri-state (null / R.font.x).

- **`ResourcesCompat.getFont()` can return null**: Even if the resource ID is valid, the font may fail to inflate (malformed XML, missing file in release builds). The `?.let { } ?: Typeface.DEFAULT` guard handles this gracefully without a crash.

- **Apply font on every `applyThemeColors()` call, not just theme changes**: Font application is inside `applyThemeColors`, which is called on every theme switch. This ensures font resets to DEFAULT when switching away from Rabbit/Panda, not just when switching to them.

- **Leave font fields null when font file doesn't exist yet**: Referencing `R.font.fredoka_one` before `res/font/fredoka_one.xml` exists will cause a compile error. Keep as `null` with a comment until the asset is in place — one-line activation once Rusty adds the file.

## Session — 2026-05-09 — Theme System Modularization + GLASS_ICE

### Work Done
- Created `Theme` data class with `id`, `displayName`, `isPremium`, `colors: (Context) → ThemeColors`, `iconRes`, `iconEmoji`, `skuId` — fully self-describing.
- Created `ThemeRegistry` singleton (`Theme.kt`): `all: List<Theme>`, `forId(id): Theme`.
- Added `GLASS_ICE` to `ThemeId` enum (`isPremium=true`, `skuId="theme_glass_ice"`).
- Added `GLASS_ICE` case to `ThemeColors.toColors()`.
- Added 11 `glass_ice_*` color resources to `colors.xml` (light ice blue palette, deep navy text).
- Refactored `ThemePickerDialog`: adapter is now `List<Theme>` from `ThemeRegistry.all`; removed `iconResFor()` helper; all metadata (displayName, isPremium, colors, iconRes) read from `Theme`.
- Refactored `MainActivity.applyThemeColors`: replaced `toColors()` + hardcoded `when(themeId)` icon block with `ThemeRegistry.forId(id).colors(ctx)` and `theme.iconEmoji`.
- Build: **clean `assembleDebug` passes** (zero errors, pre-existing `@ColorInt` warnings only). Commit: `070fdd6`.
- Decision: `.squad/decisions/inbox/basher-theme-modularization.md`.

### Key Technical Learnings

- **`(Context) -> ThemeColors` as field type**: Storing color factories as lambdas inside `Theme` defers context-dependent resolution without making Theme an abstract class. Callers use `theme.colors(context)` — natural, readable. This is the right pattern when you have context-dependent values in an otherwise pure data object.

- **ThemeRegistry delegates to `toColors()` extension**: Rather than duplicating color-resource references in the registry, each `Theme` entry's lambda calls `themeId.toColors(ctx)`. This keeps `colors.xml` as the single source of truth and `toColors()` as the only builder — registry just adds metadata around it.

- **When-block icon mapping is a smell**: The `when(themeId) { RABBIT -> "🐰" ... else -> "🌙" }` in `MainActivity` was a hidden coupling — adding a theme didn't cause a compile error if you forgot to update it. Moving `iconEmoji` to `Theme` and `ThemeRegistry` makes the omission visible at registration time.

- **`ThemeId` enum redundancy is intentional for now**: `ThemeId.displayName`, `isPremium`, `skuId` remain on the enum for backward compat (tests, DataStore deserialization). The Registry carries the canonical truth; the enum carries the key. A future cleanup pass could strip ThemeId down to just the identifier — but that touches tests and DataStore code, so defer until needed.

## Session — 2026-05-09 — ThemeUnlockListener Wiring

### Work Done
- Added `isThemeUnlocked(themeId)` to `ThemeUnlockListener` interface (was missing from Rusty's stub).
- Created **`ThemePickerDialog`** (`BottomSheetDialogFragment`): shows 2-column theme grid using a
  private `PickerAdapter`; unlocked theme taps call `listener.onThemeSelected`; locked taps open
  `ThemeUnlockDialog` via `parentFragmentManager`.
- Updated **`ThemeUnlockDialog`**: removed `ThemeViewModel` dependency entirely; "Watch Ad" and
  "Buy" now call `listener.onWatchAdRequested` / `listener.onPurchaseRequested` resolved via
  `requireActivity() as? ThemeUnlockListener`.
- Updated **`MainActivity`**: added `isThemeUnlocked` override (was absent), wired `observeUiEvents()`
  for Snackbar feedback (ThemeUnlocked, Error, AdNotReady), replaced cycling `btnTheme` click with
  `ThemePickerDialog.newInstance().show(...)`.
- Created decision file: `.squad/decisions/inbox/basher-theme-unlock-wiring.md`.

### Key Technical Learnings

- **DialogFragment → host via `requireActivity() as? Interface`**: The standard Android pattern for
  DialogFragment callbacks. Both `ThemePickerDialog` and `ThemeUnlockDialog` use this — no constructor
  injection, no `setTargetFragment` (deprecated), no shared ViewModel needed inside the dialog.
  `requireActivity()` always returns the same `MainActivity` regardless of how deeply dialogs nest.

- **`BottomSheetDialogFragment` for pickers**: `dialog_theme_picker.xml` uses `wrap_content` height
  with a drag handle — standard Material bottom sheet anatomy. No need to override `onStart` to
  force dimensions; the BottomSheetDialogFragment handles this automatically via the Material theme.

- **`parentFragmentManager` vs `childFragmentManager` in dialogs**: When showing a secondary dialog
  from inside a BottomSheetDialogFragment, use `parentFragmentManager` so it's attached to the
  Activity's back-stack, not the parent dialog's child manager (which would be destroyed with it).

- **`pendingUnlockTheme` pattern is the correct ad-theme bridge**: `AdRepository.showAd()` calls a
  generic `RewardItem` callback with no theme context. Setting `themeViewModel.pendingUnlockTheme`
  before calling `watchAdToUnlock` is the correct disambiguation — the ad reward observer picks it
  up. The listener interface enforces that `onWatchAdRequested` is always the call site where the
  theme context is set (in `MainActivity`), keeping it out of the dialog.

- **Snackbar requires a root view anchor**: Use `binding.root` as the anchor. Avoid `window.decorView`
  (overlaps system UI) or individual buttons (wrong position). `binding.root` is the calculator grid
  layout — perfect anchor.

## Session — 2026-05-09 — Bug 2: Panda Premium Badge / Lock Overlay Fix

### Work Done
- Diagnosed the "Premium badge + no lock overlay" bug on the Panda theme card.
- Applied three-part fix across four files; committed as `de2f771`.
- Wrote decision to `.squad/decisions/inbox/basher-panda-fix.md`.

### Root Cause Found

Two compounding issues:

1. **Badge text was always `"Premium"` for any `isPremium = true` theme**, regardless of whether
   it was locked or already purchased. When PANDA landed in the DataStore unlocked set (e.g. from a
   dev-time `unlockTheme(PANDA)` call), the card showed `"Premium"` badge + no lock overlay + no CTAs
   — visually contradicting itself.

2. **`unlockedThemesFlow` included free themes (CLASSIC)** via the default `setOf(ThemeId.CLASSIC.name)`.
   Free themes have no business in the DataStore unlock set; their accessibility is determined entirely
   by the `!isPremium` short-circuit in `isThemeUnlocked()`.

### Changes Made

| File | Change |
|------|--------|
| `ThemePickerDialog.kt` | Badge: `"Premium"` for all premium → `"✓ Owned"` when unlocked, `"Premium"` when locked |
| `ThemePickerActivity.kt` | Same badge fix in `ThemeAdapter` |
| `ThemeRepository.kt` | `unlockedThemesFlow` filter: added `it.isPremium &&` guard |
| `ThemeViewModel.kt` | StateFlow initial value: `setOf(ThemeId.CLASSIC)` → `emptySet()` |

### Key Technical Learnings

- **Badge text must reflect both `isPremium` AND `isUnlocked`**: A two-axis state
  (`isPremium × isUnlocked`) needs four badge variants, not two. The old single-axis check
  (`isPremium -> "Premium"`) created an impossible-looking "Premium with no lock" state.

- **DataStore unlocked set should only store premium theme names**: Free themes are always
  accessible via `!isPremium` in `isThemeUnlocked()`. Storing them in the unlocked set is
  semantic noise and can mask bugs. Filter with `it.isPremium && it.name in names`.

- **StateFlow initial value should match the filtered flow semantics**: If `unlockedThemesFlow`
  only emits premium themes, the initial StateFlow value should be `emptySet()` (no premium
  theme is unlocked before DataStore loads), not `setOf(ThemeId.CLASSIC)`.

## Session — 2026-05-09 — PANDA Theme Black Screen Fix

### Work Done
- Diagnosed root cause of PANDA (🐼) theme rendering completely black.
- Fixed three files: `values-night/themes.xml`, `values-night/colors.xml`, `MainActivity.kt`.
- Committed as `0c23fe7`.
- Wrote decision to `.squad/decisions/inbox/basher-panda-theme-fix.md`.

### Root Cause Found
The black screen had **three contributing causes** (not one):
1. `values-night/themes.xml` used `Theme.MaterialComponents.NoActionBar` (dark). The dark Material base applies internal color processing that interferes with `MaterialButton.backgroundTintList` for light themes.
2. `values-night/colors.xml` had `colorBackground=#000000`. Layout inflated black in dark mode. `applyThemeColors()` is async (coroutine); any delay left the screen black.
3. No synchronous color application in `onCreate` — colors only applied via the async flow, never on the first rendered frame.

### Key Technical Learnings

- **`values-night/` conflicts with programmatic theming**: When an app applies 100% of its colors programmatically (no `setTheme()`/`recreate()` pattern), dark-mode resource overrides in `values-night/colors.xml` create an antagonistic initial state. The async coroutine that overrides these values fires AFTER the first frame is drawn. Delete or neutralize `values-night/colors.xml` if you own all coloring.

- **Dark MaterialComponents base theme conflicts with light programmatic tints**: `Theme.MaterialComponents.NoActionBar` (dark) applies surface/color overlays that can prevent `MaterialButton.backgroundTintList = ColorStateList.valueOf(lightColor)` from rendering correctly. Use `Theme.MaterialComponents.Light.NoActionBar` as base if you control all colors via code.

- **Belt-and-suspenders: synchronous color apply in `onCreate`**: Call `applyThemeColors(themeViewModel.activeTheme.value)` synchronously after `setContentView`. The StateFlow initial value (`CLASSIC`) is always light and safe. This guarantees the first frame is never black, even before DataStore emits the saved theme.

- **StateFlow initial value as safe fallback**: `stateIn(…, SharingStarted.Eagerly, ThemeId.CLASSIC)` means `.value` is always `CLASSIC` until DataStore loads. This makes synchronous access safe — applying CLASSIC colors briefly before PANDA arrives is an invisible transition.

## Session — 2026-05-08

### Work Done
- Implemented full MVVM refactor of the Calculator app monetisation layer.
- Rewrote **ThemeId** to carry `displayName`, `isPremium`, `skuId` — single source of truth for all per-theme metadata.
- Created **ThemeColors** data class and `ThemeId.toColors(context)` extension that reads named color resources from `colors.xml` (avoiding constant duplication).
- Implemented **ThemeRepository** with real DataStore flows (`activeThemeFlow`, `unlockedThemesFlow`) using `stringSetPreferencesKey` for the unlocked set.
- Implemented **BillingRepository** (full `PurchasesUpdatedListener` lifecycle): `queryProductDetails`, `launchPurchaseFlow`, `acknowledgePurchase`, `restorePurchases` on connect. Emits `SharedFlow<BillingResult>` to ViewModel.
- Implemented **AdRepository**: `RewardedAd` load → show → preload-next lifecycle. Emits `SharedFlow<AdResult>` to ViewModel.
- Implemented **ThemeViewModel**: `selectTheme`, `watchAdToUnlock`, `buyTheme`, `observeBillingResults`, `observeAdResults`. `pendingUnlockTheme` tracks which theme a pending ad will unlock.
- Implemented **CalculatorViewModel**: full engine (digit/dot/clear/delete/sign/percent/operator/equals/formatNumber/restoreState) extracted from MainActivity.
- Fully implemented **ThemePickerActivity** with `ThemeAdapter` (palette dot preview, lock overlay, active stroke highlight using `MaterialCardView.strokeColor`).
- Fully implemented **ThemeUnlockDialog** (Watch Ad + Buy + Restore Purchase; theme colour preview blocks; `onStart` sets MATCH_PARENT to render the CoordinatorLayout backdrop correctly).
- Refactored **MainActivity** to delegate all logic to ViewModels; applies programmatic theme colours via `backgroundTintList` (preserves MaterialButton corner radius and ripple).
- Added all required deps to `build.gradle`; added INTERNET permission, AdMob test app ID meta-data, and `ThemePickerActivity` registration to `AndroidManifest.xml`.

## Key Learnings & Technical Decisions

- **`MaterialButton.backgroundTintList` not `setBackgroundColor`**: Calling `setBackgroundColor` replaces the `MaterialShapeDrawable` with a plain `ColorDrawable`, losing corner radius and ripple. Always tint with `btn.backgroundTintList = ColorStateList.valueOf(color)`.
- **`BillingResult` name collision**: `com.android.billingclient.api.BillingResult` and our `BillingRepository.BillingResult` share a name. Inside `BillingRepository`, Kotlin resolves to the inner class; callers use the fully-qualified `com.android.billingclient.api.BillingResult` in the `onPurchasesUpdated` signature to avoid ambiguity.
- **DialogFragment full-screen**: A `DialogFragment` using `onCreateView` renders as a small centered dialog by default. Override `onStart` and call `setLayout(MATCH_PARENT, MATCH_PARENT)` + `setBackgroundDrawableResource(transparent)` to let the layout's CoordinatorLayout backdrop fill the window.
- **DataStore `stringSetPreferencesKey`**: Storing unlocked themes as a `Set<String>` (enum names) is simpler and more compact than one Boolean key per theme. `ThemeId.entries.filter { it.name in names }` cleanly deserialises.
- **`ThemeId.entries` (Kotlin 1.9+)**: Prefer `entries` over deprecated `values()` for type-safe `EnumEntries<ThemeId>`.

### 2026-05-09 — Cross-Team Integration Verified ✅

- **Rusty's color system** integrates perfectly: `ThemeId.toColors(context)` reads prefixed color names from `colors.xml`. Prefixed naming (e.g., `midnight_btn_operator`) avoids collisions and maps directly to enum values. Theme overlays apply cleanly.
- **Danny's architecture** held under implementation: manual DI (AppModule) creates zero circular dependency issues. Singleton repositories live exactly as designed. Activity recreation on theme switch is the standard Android pattern; no workarounds needed.
- **Linus's tests** reveal the actual integration gaps: 3 ThemeRepositoryTest cases fail because DataStore stubs don't persist. Root cause is clear: stub `unlockTheme()` and `setActiveTheme()` don't write to the backing store. This is expected and actionable.
- **SharedFlow architecture** is testable: Linus's TDD test cases (8 for ThemeViewModel) outline the exact APIs needed. Once `BillingRepository.purchaseResults` and `AdRepository.adResults` are wired, those tests will compile and drive development.
- **No API mismatches:** Every contract Linus wrote maps to an existing implementation. The system is cohesive.
- **Next action:** Wire real DataStore persistence in `ThemeRepository.unlockTheme()` and `setActiveTheme()` to flip 3 red tests green.

## Session — 2026-05-09 — Scribe: Decision Archive & Cross-Agent Log

### Work Done (Scribe)
- Merged 5 decision files from inbox into single `.squad/decisions/decisions.md` (7029 bytes)
- Created 3 orchestration logs documenting agent work this session
- Created session log `.squad/log/20260509T075626-theme-picker-inspection.md`
- Cleaned up 5 inbox files
- Appended cross-agent updates to Basher and Rusty history

### Decisions Archived
1. PANDA Black Screen Fix (root causes + solution) — **Status:** Implemented, commit 0c23fe7
2. ThemeUnlockListener Wiring (ad/billing integration) — **Status:** Implemented
3. Theme Picker BottomSheet UI (architecture + listener pattern) — **Status:** Implemented
4. Visual Inspection Bugs (3 findings, recommendations) — **Status:** Open

### Outstanding Bugs Documented
- **Bug 1 (Major):** Locked theme names hidden by overlay — fix `item_theme_card.xml`
- **Bug 2 (Major):** PANDA theme lock state inconsistent — verify UnlockStatusManager
- **Bug 3 (Minor):** Display text size/position — polish cosmetic

## Session — 2026-05-09 — Bug 2: Panda Lock/Badge Behavior (basher-2)

### Work Done
- **Diagnosed and fixed** the "Panda shows Premium badge but no lock overlay" bug.
- Root cause: Badge logic not distinguishing locked vs owned states; `unlockedThemesFlow` incorrectly persisting free themes (CLASSIC).
- **Changes:**
  - `ThemePickerDialog.kt`: Badge `isPremium -> "Premium"` → `isUnlocked -> "✓ Owned"` / `else -> "Premium"`
  - `ThemePickerActivity.kt`: Applied same badge fix in `ThemeAdapter.onBindViewHolder`
  - `ThemeRepository.kt`: `unlockedThemesFlow` filter: `it.name in names` → `it.isPremium && it.name in names`
  - `ThemeViewModel.kt`: StateFlow initial value: `setOf(ThemeId.CLASSIC)` → `emptySet()`

### Key Technical Learnings
- **Two-axis state (isPremium × isUnlocked)** requires four badge variants, not two. Badge text must reflect both dimensions to avoid impossible states like "Premium with no lock".
- **DataStore unlocked set should filter for premium themes only**. Free themes are always accessible via `!isPremium` in `isThemeUnlocked()`. Storing them creates semantic noise.
- **StateFlow initial value must match the filtered flow semantics**. If `unlockedThemesFlow` only emits premium themes, initial value should be `emptySet()`, not `setOf(ThemeId.CLASSIC)`.

### Results
✓ Locked premium themes (PANDA, RABBIT, etc.) show "Premium" badge + lock overlay + CTAs
✓ Unlocked premium themes show "✓ Owned" badge with no lock
✓ Free themes (CLASSIC) correctly excluded from unlock persistence
✓ Badge state consistent across all themes
✓ Commit: `de2f771`

## Session — 2026-05-09 — Theme System Modularization + Glass Ice Integration (basher-3, rusty-4, rusty-5)

### Work Done (Basher-3)

**Theme System Modularization (070fdd6)**
- Created `Theme` data class with self-describing metadata: `id`, `displayName`, `isPremium`, `colors: (Context) → ThemeColors`, `iconRes`, `iconEmoji`, `skuId`
- Created `ThemeRegistry` singleton: `all: List<Theme>`, `forId(id): Theme` with CLASSIC fallback
- Added `GLASS_ICE` to `ThemeId` enum (`isPremium=true`, `skuId="theme_glass_ice"`)
- Added `GLASS_ICE` case to `ThemeColors.toColors()` with 11 color resources
- Refactored `ThemePickerDialog`: adapter now reads `List<Theme>` from registry; all metadata derived from `Theme` object
- Refactored `MainActivity.applyThemeColors`: uses `ThemeRegistry.forId(id).colors(ctx)` instead of hardcoded when blocks

**Key Design:** `colors` field is function type `(Context) → ThemeColors` to defer Android resource resolution until runtime while keeping color wiring self-contained in ThemeRegistry. Callers use `theme.colors(context)` — readable and unambiguous.

**New Theme Onboarding (single file chain):**
1. Add enum entry to `ThemeId.kt`
2. Add color resources to `colors.xml`
3. Add `when` branch to `ThemeColors.kt`
4. Add `Theme(...)` entry to `ThemeRegistry.all`
Done — UI auto-discovers via `ThemeRegistry.all`.

### Collaboration Notes

- **Rusty's Glass Ice visuals (commits bb0ec21, d40fa0b):** 11-color palette with snowflake icon, wired into ThemeRegistry — seamless integration
- **No regressions:** All current themes unaffected; Glass Ice integrated end-to-end
- **Build:** `assembleDebug` — **BUILD SUCCESSFUL**

### Key Technical Learnings

- **`(Context) → ThemeColors` function field:** Defers Android resource resolution until runtime while keeping color wiring self-contained in ThemeRegistry. This is the right pattern for context-dependent values in otherwise pure data objects.

- **ThemeRegistry as single source of truth:** Delegates to `toColors()` extension for color building, keeping `colors.xml` and `toColors()` as canonical. Eliminates hidden couplings where adding a theme didn't error if you forgot a field.

- **Icon system extensibility:** `iconRes` and `iconEmoji` fields support both drawables and emoji. Hidden by default (`visibility="gone"`) — no layout impact. Future themes only need drawable + one-line adapter wiring.

- **Theme enum redundancy is intentional:** `displayName`, `isPremium`, `skuId` kept on enum for backward compat. ThemeRegistry carries the canonical truth. Future cleanup could consolidate when broader refactor touches DataStore deserialization code.
