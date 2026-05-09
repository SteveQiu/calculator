# Squad Decisions

## Active Decisions

### ADR: Premium Theme System Architecture

**Date:** 2026-05-08  
**Author:** Danny (Lead)  
**Status:** Accepted

#### Context
The Calculator app needs a premium theme system where users can unlock visual themes by watching a rewarded ad or making a one-time in-app purchase via Google Play Billing. The app currently has a basic light/dark toggle. We need to scale this into a full theme system without over-engineering.

#### Decision

**Theme System**

Four themes are defined:

| Theme ID | Name     | Tier    |
|----------|----------|---------|
| CLASSIC  | Classic  | Free    |
| MIDNIGHT | Midnight | Premium |
| OCEAN    | Ocean    | Premium |
| SUNSET   | Sunset   | Premium |

Each theme maps to a named `style` resource that overrides color attributes (e.g., `colorPrimary`, `colorSurface`, `buttonNumberBackground`, etc.). Themes are applied at runtime by calling `activity.setTheme(R.style.Theme_Calculator_Midnight)` before `setContentView`, then recreating the activity when the user switches themes.

Unlock state is persisted in **DataStore (Preferences)** — not SharedPreferences — for structured, coroutine-friendly key-value storage. Each theme has a boolean key `theme_unlocked_{themeId}`. The Classic theme is always considered unlocked.

**Package Structure**

```
com.example.calculator/
  ui/
    MainActivity.kt          — host activity, applies active theme, houses calculator UI
    ThemePickerActivity.kt   — gallery of 4 themes; shows lock overlay on premium locked themes
    ThemeUnlockDialog.kt     — DialogFragment: "Watch Ad" / "Buy ($0.99)" CTA
  viewmodel/
    CalculatorViewModel.kt   — calculator state machine (input, expression, result)
    ThemeViewModel.kt        — active theme, unlock state map, triggers unlock dialog
  repository/
    ThemeRepository.kt       — DataStore-backed; exposes unlock flow; writes unlock on reward/purchase
    BillingRepository.kt     — BillingClient wrapper; exposes launchPurchaseFlow; validates purchases
    AdRepository.kt          — AdMob RewardedAd wrapper; loads/shows ads; callbacks on reward
  model/
    Theme.kt                 — data class: id, name, isPremium, colors ref
    ThemeId.kt               — enum: CLASSIC, MIDNIGHT, OCEAN, SUNSET
  di/
    AppModule.kt             — manual singleton DI (no Hilt — overkill at this scale)
```

**MVVM Layer Responsibilities**

- **UI Layer**: `MainActivity` and `ThemePickerActivity` observe ViewModels for display state and theme state. `ThemeUnlockDialog` triggers unlock flows.
- **ViewModel Layer**: `CalculatorViewModel` holds input/result state; `ThemeViewModel` holds active theme and unlock state, orchestrates repositories.
- **Repository Layer**: `ThemeRepository` is the single source of truth (DataStore-backed). `BillingRepository` wraps `BillingClient`. `AdRepository` wraps `RewardedAd`.

**Unlock Flow**: User taps locked theme → `ThemeViewModel.requestUnlock()` → dialog shows → user taps "Watch Ad" or "Buy" → repository unlocks → flow updates → UI shows as unlocked.

**Dependencies Added**

```gradle
implementation 'androidx.datastore:datastore-preferences:1.0.0'
implementation 'androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0'
implementation 'androidx.lifecycle:lifecycle-runtime-ktx:2.7.0'
implementation 'com.android.billingclient:billing-ktx:6.1.0'
implementation 'com.google.android.gms:play-services-ads:22.6.0'
implementation 'androidx.activity:activity-ktx:1.8.2'
implementation 'androidx.fragment:fragment-ktx:1.6.2'
```

#### Consequences

- Skeleton files are in place; all agents can work in parallel.
- `AppModule` provides singletons; no circular dependency risk.
- Theme application requires activity recreation — this is standard Android behavior.
- BillingClient SKU IDs must be registered in Play Console before release.

---

### Decision: Billing & AdMob Architecture for Theme Unlock

**Author:** Basher  
**Date:** 2026-05-08  
**Status:** Implemented  

#### Context
The calculator app monetises premium themes (Midnight, Ocean, Sunset) via two paths:
1. Watch a rewarded AdMob ad — free, immediate unlock
2. One-time in-app purchase via Google Play Billing — permanent unlock, backed by receipt

#### Decision

**Repository pattern (no Hilt)**: At current app scale, a manual singleton `AppModule` with `provide*()` methods is simpler than Hilt. Each repository is created lazily on first access and shared across the process lifetime.

**SharedFlow for cross-layer events**: `BillingRepository` and `AdRepository` expose `SharedFlow<Result>` rather than callback lambdas. This keeps repositories coroutine-native and lets `ThemeViewModel` collect results in `viewModelScope` with structured cancellation — no leaking callbacks.

**`pendingUnlockTheme` on ThemeViewModel**: Because `AdRepository.showAd()` does not accept a theme parameter (it only calls a generic reward callback), `ThemeViewModel` stores the pending theme in `pendingUnlockTheme` before showing the ad. This is set by the dialog before calling `watchAdToUnlock`, so the reward observer knows which theme to unlock.

**`backgroundTintList` for programmatic theming**: MaterialButton shapes are `MaterialShapeDrawable`-backed. Calling `setBackgroundColor()` replaces the drawable and loses corner radius + ripple. Using `backgroundTintList = ColorStateList.valueOf(color)` preserves shape while changing colour.

**`stringSetPreferencesKey` for unlocked themes**: Storing the unlocked set as a single `Set<String>` (enum names) in DataStore is simpler than one Boolean key per theme. New themes added in future releases do not require a migration.

**Theme colours from named resources**: `ThemeId.toColors(context)` reads `R.color.*` entries from `colors.xml` (which already defines all four palettes) rather than hardcoding hex integers. This keeps colour values in a single canonical location.

#### Consequences

- Billing and ad clients live in singleton repositories scoped to the app process.
- ViewModel subscribes to two `SharedFlow`s; on process death the ViewModel is recreated and re-subscribes — no state loss risk.
- `BillingRepository.restorePurchases()` runs on every BillingClient connect, ensuring previously purchased themes are re-unlocked on reinstall or device change.
- Test ad unit ID must be swapped for a real unit ID before production release.
- Test AdMob app ID must be replaced with production app ID before release.

---

### Decision: Theme Color Palette Design

**Agent:** Rusty  
**Date:** 2026-05-08  
**Status:** Accepted  

#### Context
The Calculator app needs multiple premium themes locked behind ads or Google Pay. We need a color system that:
1. Works with Android resource compilation (no runtime resource injection)
2. Supports runtime theme switching without Activity recreation where possible
3. Looks premium enough to motivate unlock

#### Decision: Four-theme palette with prefixed color names

**Palette choices and rationale**

| Theme | Vibe | Operator accent | Why |
|-------|------|-----------------|-----|
| **Classic** | iOS-inspired light | `#FF9F0A` amber | Familiar, zero-friction default. Free tier — must feel complete, not degraded. |
| **Midnight** | Deep space, dark blue-black | `#7B61FF` electric violet | The "hero" premium theme. Dark backgrounds with cool violet pops feel modern and high-end. |
| **Ocean** | Deep sea, teal/cyan | `#00B4D8` vivid teal | High-contrast on near-black; operator text is dark to ensure readability. |
| **Sunset** | Warm embers, burnt orange/pink | `#FF6B6B` coral-red | Warm palette as a counterpoint to the cool premiums. Gives the set visual breadth. |

**Color naming convention**: All colors prefixed `{theme}_{role}`, e.g. `midnight_btn_operator`. Avoids collisions, enables grep audits, keeps Android Studio color preview working. Backward-compat aliases reference `classic_*` so existing code does not break.

**Runtime switching strategy**: Theme overlays use `parent=""` so they are safe to apply via `setTheme()` before `setContentView()`. ViewModel holds active `ThemeId`; Activity recreates only when theme changes.

**Layout architecture**: `activity_theme_picker.xml` uses RecyclerView with GridLayoutManager(2). `item_theme_card.xml` is MaterialCardView with lock overlay. `dialog_theme_unlock.xml` uses BottomSheetBehavior.

#### Consequences

- Any new theme requires: (a) prefix colors in `colors.xml`, (b) a `ThemeOverlay` style, (c) adapter data entry. About 15 min of work.
- The classic theme's `colorBtnSpecial` changed from `#A5A5A5` to `#D1D1D6` (lighter, closer to iOS spec). Deliberate design upgrade.

---

### Test Coverage Report — 2026-05-08

**Author:** Linus (Tester)  
**Date:** 2026-05-08  
**Status:** Informational  

#### What IS Covered

- **ThemeIdTest (8 tests)** ✅ All pass: premium flags, SKU IDs, display names
- **ThemeRepositoryTest (8 tests)**: 5 pass (default theme, unlock flags); 3 fail on DataStore impl
- **CalculatorViewModelTest (22 tests)** ⏳ TDD written; awaits implementation
- **ThemeViewModelTest (8 tests)** ⏳ TDD written; awaits implementation

#### What is NOT Covered (gaps)

- **CalculatorViewModel**: Bulk entry, repeated equals, large number formatting, negative operands, precision display
- **ThemeRepository**: DataStore persistence across restart, idempotency, locked theme rejection
- **BillingRepository & AdRepository**: No tests yet (implementations in progress)
- **UI / Integration**: Espresso suite deferred (future work)

#### Recommended Next Steps

1. Implement `CalculatorViewModel` public API — unlocks 22 tests
2. Wire DataStore in `ThemeRepository` — fixes 3 failing tests
3. Add `SharedFlow` results to `BillingRepository` and `AdRepository` — unlocks 8 ThemeViewModel tests
4. Add `BillingRepositoryTest` and `AdRepositoryTest` after implementations
5. Espresso suite after UI is stable

## Governance

- All meaningful changes require team consensus
- Document architectural decisions here
- Keep history focused on work, decisions focused on direction
