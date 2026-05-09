# Basher — History

## Project Seed

- **Project:** Android Kotlin Calculator
- **Stack:** Kotlin, AdMob rewarded ads, Google Play Billing (one-time purchase), DataStore, MVVM
- **Requested by:** Developer
- **Goal:** Calculator logic + dual unlock path: watch a rewarded ad to unlock a theme temporarily/permanently, OR purchase via Google Pay (Play Billing) for permanent unlock
- **Repo:** https://github.com/SteveQiu/calculator.git

## Learnings

_Appended by Basher after each session._

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
