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

### Decision: Team SOPs and Linus Done Gate

**Author:** Danny (Lead)  
**Date:** 2026-05-12  
**Status:** Accepted

#### Context

The team had no shared, written process rules. Work was being declared "done" without a consistent definition, and feature validation (Linus) was treated as optional. This created risk of regressions shipping undetected and inconsistent quality across sessions.

#### Decision

Three documents were created or updated to establish hard process rules:

**1. `.squad/sop.md` (new)**

Full team playbook defining:
- **Definition of Done** — 5 conditions, all must be true simultaneously
- **Work Process** — 7-step sequence; step 4 (Linus validation) is a hard gate
- **How to trigger Linus validation** — standard prompt template for coordinators
- **Regression areas** — 7 core feature areas that must never break
- **Agent responsibilities** — quick reference, noting Linus can NEVER be skipped

**2. `.squad/ceremonies.md` (updated)**

Added **Pre-Done Validation** ceremony:
- Trigger: auto, after any feature work or bug fix being declared done
- Facilitator: Linus
- Participants: Linus + Livingston
- Livingston verifies build; Linus runs checklist; FAIL blocks done status

**3. `.squad/routing.md` (updated)**

Added **Done Gate** section at bottom:
- Mandatory routing of Livingston (build) and Linus (validation) before any task is declared complete
- No exceptions clause enforced by coordinator

#### Rationale

- Linus is the only agent with a validation skill and checklist. No other agent substitutes.
- Build verification without feature validation catches compile errors but not behavioral regressions.
- Written SOPs eliminate per-session ambiguity about what "done" means.
- Hard gates (not soft reminders) are the only reliable process enforcement mechanism.

#### Consequences

- Every code-producing session must route Livingston + Linus before calling done
- Coordinators must include the Linus prompt template from sop.md Section 3
- Validation failures produce a file in `.squad/decisions/inbox/linus-validation-fail-{slug}.md`
- Agents who skip this gate are in violation of team SOP

---

### ADR: Background Image Architecture + Cherry Blossom Theme

**Date:** 2026-05-11T23:14:40.125-07:00  
**Author:** Danny (Lead)  
**Status:** Accepted — Ready for Implementation

#### Context

The team asked to (a) make adding themes easier and (b) propose one new theme, preferring themes that change the background image. The current system applies a solid `colors.background` to `binding.root`. We need to extend `Theme` to optionally carry a drawable background while keeping backward compatibility with the seven existing solid-color themes.

#### Decision 1 — New Theme: Cherry Blossom 🌸

**Rationale:** Cherry Blossom is the most visually distinct addition to our current catalog. Every existing theme is a solid-color theme; Cherry Blossom is our first gradient/image theme. The pink-floral palette is completely absent from our lineup (Midnight = dark violet, Ocean = teal, Sunset = orange/coral, Rabbit/Panda = cute neutrals, Glass Ice = frosted). A sakura gradient is also the most universally appealing across demographics — it will convert well in the theme picker.

| Property          | Value                                         |
|-------------------|-----------------------------------------------|
| ThemeId enum name | `CHERRY_BLOSSOM`                              |
| displayName       | `"Cherry Blossom"`                            |
| emoji / icon      | 🌸                                            |
| isPremium         | `true`                                        |
| skuId             | `"theme_cherry_blossom"`                      |
| backgroundImageRes| `R.drawable.bg_cherry_blossom`                |
| fontResId         | `null` (system default — clean, not cute)     |

**Color Palette**

| Role              | Color      | Hex       |
|-------------------|------------|-----------|
| background (fallback) | Blush white | `#FFF0F5` |
| btnNumber         | Pale petal  | `#FFE4EE` |
| btnSpecial        | Soft rose   | `#FADADD` |
| btnOperator       | Deep rose   | `#C2185B` |
| textPrimary       | Dark plum   | `#2D1B24` |
| textSecondary     | Muted rose  | `#8B5067` |
| textOnNumber      | Dark plum   | `#2D1B24` |
| textOnSpecial     | Dark plum   | `#2D1B24` |
| textOnOperator    | White       | `#FFFFFF` |

**Background Drawable Concept**

File: `app/src/main/res/drawable/bg_cherry_blossom.xml`

A `<layer-list>` with two layers:
1. A vertical `<shape>` gradient from `#FFB7C5` (sakura pink, top) to `#FFF0F5` (blush white, bottom)
2. An optional subtle semi-transparent petal pattern overlay (can be a simple oval cluster shape at low alpha)

This is a pure vector/XML drawable — no binary files, no APK bloat.

```xml
<!-- bg_cherry_blossom.xml skeleton for Rusty -->
<layer-list xmlns:android="http://schemas.android.com/apk/res/android">
    <item>
        <shape android:shape="rectangle">
            <gradient
                android:type="linear"
                android:angle="270"
                android:startColor="#FFB7C5"
                android:centerColor="#FFCDD9"
                android:endColor="#FFF0F5" />
        </shape>
    </item>
    <!-- Optional: petal accent shapes at corners -->
</layer-list>
```

#### Decision 2 — Background Image Architecture

**Q1: How is the background stored?**

**Option A selected**: `@DrawableRes backgroundImageRes: Int? = null` on the `Theme` data class.

Vector/XML drawables in `res/drawable/`. No binary PNG/JPG files in the repo. This is consistent with the existing `ic_theme_glass_ice.xml` icon approach and scales perfectly on all screen densities.

**Change to `Theme.kt` — data class field**

Add one nullable field to the `Theme` data class:

```kotlin
data class Theme(
    val id: ThemeId,
    val displayName: String,
    val isPremium: Boolean,
    val colors: (Context) -> ThemeColors,
    val iconRes: Int? = null,
    val iconEmoji: String? = null,
    val skuId: String? = null,
    val fontResId: Int? = null,
    val backgroundImageRes: Int? = null   // null = use solid ThemeColors.background
)
```

All existing `Theme(...)` entries in `ThemeRegistry.all` are unaffected — Kotlin default parameter = no migration.

**Q2: How is it applied in MainActivity?**

**Method:** `applyThemeColors()` in `MainActivity.kt`

**Root view:** `binding.root` (the `LinearLayout` that is the root of `activity_main.xml` — no explicit ID, accessed via ViewBinding's generated `.root` property)

**Current code (line 111):**
```kotlin
binding.root.setBackgroundColor(colors.background)
```

**Replace with:**
```kotlin
val bgDrawable = theme.backgroundImageRes?.let { ContextCompat.getDrawable(this, it) }
if (bgDrawable != null) {
    binding.root.background = bgDrawable
} else {
    binding.root.setBackgroundColor(colors.background)
}
```

Import needed: `androidx.core.content.ContextCompat` (already imported).

This change is surgical — one replaced statement. All other button/text coloring logic is untouched.

**Q3: How does the background compose with ThemeColors?**

- Solid-color themes (`backgroundImageRes == null`): `ThemeColors.background` applied as before. Zero regression.
- Image-based themes (`backgroundImageRes != null`): The drawable replaces the solid color entirely. Button backgrounds (via `backgroundTintList`) and text colors from `ThemeColors` still overlay on top — the drawable is only the root view background.
- For Cherry Blossom: the gradient provides the visual background; semi-transparent button tints (`#FFE4EE` at appropriate alpha) let the gradient subtly show through.

**Q4: How easy is it to add a new theme after this change?**

Adding a theme requires exactly 5 steps (Rusty does 1-4, Basher wires ViewModel if needed):

1. **`ThemeId.kt`** — add enum entry: `MY_THEME("My Theme", true, "theme_my_theme")`
2. **`colors.xml`** — add `my_theme_*` color resources (9 values: background, btn_number, btn_special, btn_operator, text_primary, text_secondary, text_on_number, text_on_special, text_on_operator)
3. **`ThemeColors.kt`** — add `ThemeId.MY_THEME -> ThemeColors(...)` branch to `ThemeId.toColors()`
4. *(Optional)* **`res/drawable/bg_my_theme.xml`** — gradient/vector drawable if using a background image
5. **`Theme.kt`** — add `Theme(id=MY_THEME, displayName="My Theme", isPremium=true, colors={ it.toColors() }, iconEmoji="✨", skuId="theme_my_theme", backgroundImageRes=R.drawable.bg_my_theme)` to `ThemeRegistry.all`

Typical time: ~15 minutes for a solid-color theme; ~30 minutes with a custom background drawable.

**KDoc Comment for ThemeRegistry**

Add this KDoc immediately before `object ThemeRegistry` in `Theme.kt`:

```kotlin
/**
 * Registry of all available themes in the application.
 *
 * ## How to add a new theme
 * 1. **ThemeId.kt** — add a new enum entry:
 *    `MY_THEME("Display Name", isPremium = true, skuId = "theme_my_theme")`
 * 2. **colors.xml** — add `my_theme_*` color resources for all nine roles:
 *    `background`, `btn_number`, `btn_special`, `btn_operator`, `text_primary`,
 *    `text_secondary`, `text_on_number`, `text_on_special`, `text_on_operator`.
 * 3. **ThemeColors.kt** — add a `ThemeId.MY_THEME -> ThemeColors(...)` branch
 *    to the `ThemeId.toColors()` function.
 * 4. *(Optional)* **res/drawable/bg_my_theme.xml** — create a vector or gradient
 *    drawable for a custom background image. Prefer `<layer-list>` or `<gradient>`
 *    XML over binary PNG/JPG to keep the APK lean.
 * 5. **Theme.kt** — add a `Theme(...)` entry to [ThemeRegistry.all] below, setting
 *    `backgroundImageRes = R.drawable.bg_my_theme` if you created a drawable in step 4.
 *
 * For solid-color themes, omit `backgroundImageRes` (defaults to `null`; the solid
 * color from `ThemeColors.background` is applied to the root view).
 * For image-based themes, provide a vector drawable; it replaces the solid background
 * while button and text colors from `ThemeColors` still apply on top.
 */
```

#### Consequences

- Every code-producing session must route Livingston + Linus before calling done
- New themes can be added in 15–30 minutes depending on complexity
- The change is fully backward-compatible; existing solid-color themes work unchanged

---

## Governance

- All meaningful changes require team consensus
- Document architectural decisions here
- Keep history focused on work, decisions focused on direction
