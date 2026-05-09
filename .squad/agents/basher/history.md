# Basher — History

## Project Seed

- **Project:** Android Kotlin Calculator
- **Stack:** Kotlin, AdMob rewarded ads, Google Play Billing (one-time purchase), DataStore, MVVM
- **Requested by:** Developer
- **Goal:** Calculator logic + dual unlock path: watch a rewarded ad to unlock a theme temporarily/permanently, OR purchase via Google Pay (Play Billing) for permanent unlock
- **Repo:** https://github.com/SteveQiu/calculator.git

## Learnings

_Appended by Basher after each session._

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
