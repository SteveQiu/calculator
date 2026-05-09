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
