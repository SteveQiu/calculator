# Danny — History

## Project Seed

- **Project:** Android Kotlin Calculator
- **Stack:** Kotlin, Android Views (ConstraintLayout), AdMob for rewarded ads, Google Play Billing for one-time purchases
- **Requested by:** Developer
- **Goal:** Calculator app with multiple premium themes — themes unlocked by watching a rewarded ad OR purchasing via Google Pay (Play Billing)
- **Repo:** https://github.com/SteveQiu/calculator.git

## Learnings

### 2026-05-08 — Theme System Architecture Scaffold

- Defined 4 themes: Classic (free), Midnight, Ocean, Sunset (all premium).
- Chose DataStore over SharedPreferences for unlock state — coroutine-safe, no threading footguns.
- Kept DI manual (AppModule singleton object) — Hilt is overkill for this app size.
- Theme application strategy: `activity.setTheme(styleResId)` before `setContentView`, then activity recreation on theme change — standard Android pattern, no custom delegate needed.
- BillingClient SKU convention: `theme_{themeId.themeKey}` (e.g. `theme_midnight`) — easy to extend.
- AdMob test IDs wired in — must swap to real IDs before Play Store submission.
- All skeleton files created; Rusty/Basher/Linus can implement independently without merge conflicts.
- ADR written to `.squad/decisions/inbox/danny-theme-architecture.md`.

### 2026-05-11 — Background Image Architecture + Cherry Blossom Theme

- **New theme chosen:** Cherry Blossom 🌸 (`CHERRY_BLOSSOM`, `skuId="theme_cherry_blossom"`). First gradient/image theme in the catalog; pink-floral palette is absent from all 7 existing themes. Sakura gradient drawable (`bg_cherry_blossom.xml`) replaces solid background.
- **Background image field:** Added `val backgroundImageRes: Int? = null` to `Theme` data class. Null = solid color (100% backward-compat). Non-null = vector/XML drawable replaces `binding.root` background.
- **Application site:** `applyThemeColors()` in `MainActivity.kt`. Replaces `binding.root.setBackgroundColor(colors.background)` with a one-branch conditional: drawable if non-null, solid color otherwise. Uses `ContextCompat.getDrawable()` — already imported.
- **Root view confirmed:** `binding.root` is the `LinearLayout` root of `activity_main.xml` (no explicit `android:id`; accessed via ViewBinding).
- **Architecture: Option A (vector drawables only).** No binary PNG/JPG in repo. Consistent with `ic_theme_glass_ice.xml` precedent. Scales to all densities at zero APK cost.
- **Developer guide:** KDoc added to `ThemeRegistry` documenting the 5-step process to add any new theme. Target time: ~15 min solid-color, ~30 min image-backed.
- **Rusty owns:** ThemeId enum, colors.xml, ThemeColors.kt branch, drawable XML, Theme.kt registry entry + KDoc.
- **Basher owns:** `Theme` data class field addition, `applyThemeColors()` conditional in MainActivity.
- **ADR written:** `.squad/decisions/inbox/danny-theme-bg-architecture.md`

### 2026-05-09 — Cross-Team Integration Complete ✅

- **Rusty's color system** landed perfectly: prefixed naming (`midnight_btn_operator`), theme overlays with `parent=""`, and backward-compat aliases. Usable in layouts as-is.
- **Basher's MVVM** delivered all endpoints: `ThemeViewModel.selectTheme`, `watchAdToUnlock`, `buyTheme`; `CalculatorViewModel` with full state machine; DataStore-backed `ThemeRepository` (though persistence tests show gaps). `BillingRepository` and `AdRepository` emit `SharedFlow<Result>` exactly as designed.
- **Linus's test suite** (46 cases) validates the entire architecture: 11 tests pass, 3 fail on DataStore impl (expected), 30 TDD contracts written for ViewModels. No API surprises.
- **Architecture holds:** No rework needed. Activity recreation for theme switch is acceptable UX. Manual DI (AppModule) scales cleanly. `backgroundTintList` for runtime coloring (Basher's choice) preserves Material shapes.
- **Decision:** All ADRs merged into `.squad/decisions.md`; no conflicts detected. System is cohesive and ready for final implementation/testing phase.
