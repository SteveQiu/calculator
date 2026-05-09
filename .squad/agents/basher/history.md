# Basher — History

## Project Seed

- **Project:** Android Kotlin Calculator
- **Stack:** Kotlin, AdMob rewarded ads, Google Play Billing (one-time purchase), DataStore, MVVM
- **Requested by:** Developer
- **Goal:** Calculator logic + dual unlock path: watch a rewarded ad to unlock a theme temporarily/permanently, OR purchase via Google Pay (Play Billing) for permanent unlock
- **Repo:** https://github.com/SteveQiu/calculator.git

## Learnings

_Appended by Basher after each session._

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

## Learnings

- **`MaterialButton.backgroundTintList` not `setBackgroundColor`**: Calling `setBackgroundColor` replaces the `MaterialShapeDrawable` with a plain `ColorDrawable`, losing corner radius and ripple. Always tint with `btn.backgroundTintList = ColorStateList.valueOf(color)`.
- **`BillingResult` name collision**: `com.android.billingclient.api.BillingResult` and our `BillingRepository.BillingResult` share a name. Inside `BillingRepository`, Kotlin resolves to the inner class; callers use the fully-qualified `com.android.billingclient.api.BillingResult` in the `onPurchasesUpdated` signature to avoid ambiguity.
- **DialogFragment full-screen**: A `DialogFragment` using `onCreateView` renders as a small centered dialog by default. Override `onStart` and call `setLayout(MATCH_PARENT, MATCH_PARENT)` + `setBackgroundDrawableResource(transparent)` to let the layout's CoordinatorLayout backdrop fill the window.
- **DataStore `stringSetPreferencesKey`**: Storing unlocked themes as a `Set<String>` (enum names) is simpler and more compact than one Boolean key per theme. `ThemeId.entries.filter { it.name in names }` cleanly deserialises.
- **`ThemeId.entries` (Kotlin 1.9+)**: Prefer `entries` over deprecated `values()` for type-safe `EnumEntries<ThemeId>`.
