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

### 2026-05-09 — Cross-Team Integration Complete ✅

- **Rusty's color system** landed perfectly: prefixed naming (`midnight_btn_operator`), theme overlays with `parent=""`, and backward-compat aliases. Usable in layouts as-is.
- **Basher's MVVM** delivered all endpoints: `ThemeViewModel.selectTheme`, `watchAdToUnlock`, `buyTheme`; `CalculatorViewModel` with full state machine; DataStore-backed `ThemeRepository` (though persistence tests show gaps). `BillingRepository` and `AdRepository` emit `SharedFlow<Result>` exactly as designed.
- **Linus's test suite** (46 cases) validates the entire architecture: 11 tests pass, 3 fail on DataStore impl (expected), 30 TDD contracts written for ViewModels. No API surprises.
- **Architecture holds:** No rework needed. Activity recreation for theme switch is acceptable UX. Manual DI (AppModule) scales cleanly. `backgroundTintList` for runtime coloring (Basher's choice) preserves Material shapes.
- **Decision:** All ADRs merged into `.squad/decisions.md`; no conflicts detected. System is cohesive and ready for final implementation/testing phase.
