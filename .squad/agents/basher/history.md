# Basher — History

## Project Seed

- **Project:** Android Kotlin Calculator
- **Stack:** Kotlin, AdMob rewarded ads, Google Play Billing (one-time purchase), DataStore, MVVM
- **Requested by:** Developer
- **Goal:** Calculator logic + dual unlock path: watch a rewarded ad to unlock a theme temporarily/permanently, OR purchase via Google Pay (Play Billing) for permanent unlock
- **Repo:** https://github.com/SteveQiu/calculator.git

## 📋 Work Summary (2026-05-08 through 2026-05-10)

### Session 2026-05-08: Full MVVM Refactor
- Implemented full MVVM stack: CalculatorViewModel, ThemeRepository (DataStore), BillingRepository (Play Billing), AdRepository (RewardedAd), ThemeViewModel, ThemePickerActivity
- All repos use proper flows and SharedFlow for async events
- Build successful; all components integrated

### Session 2026-05-09: Bug Fixes & Theme System Modularization

**Bug Fixes:**
- **PANDA Black Screen Fix (0c23fe7):** Root cause = dark Material base + black initial background + async color. Fix: Light Material base + sync `applyThemeColors()` in `onCreate()`.
- **Panda Lock/Badge State (de2f771):** Badge not distinguishing locked vs owned; `unlockedThemesFlow` incorrectly persisting free themes. Fix: Two-axis state logic + premium-only filter + `emptySet()` initial value.
- **ThemeUnlockListener Wiring:** Created interface, wired dialogs to MainActivity via `requireActivity()` pattern.

**Theme System Modularization (basher-3, 070fdd6):**
- Created `Theme` data class: `id`, `displayName`, `isPremium`, `colors: (Context) → ThemeColors`, `iconRes`, `iconEmoji`, `skuId`
- Created `ThemeRegistry` singleton: `all: List<Theme>`, `forId(id): Theme`
- Refactored UI to use ThemeRegistry — zero hardcoded theme logic in MainActivity/dialogs
- Added GLASS_ICE theme (premium, snowflake icon, 11-color palette)
- Design: `colors` field is function type to defer resource resolution; Callers use `theme.colors(context)`

**Key Technical Learnings:**
- `MaterialButton.backgroundTintList` preserves corner radius/ripple (not `setBackgroundColor`)
- DataStore `stringSetPreferencesKey` + `ThemeId.entries.filter { it.name in names }` for clean serialization
- DialogFragment full-screen: override `onStart`, set `MATCH_PARENT` + transparent background
- Two-axis state (isPremium × isUnlocked) requires four badge variants
- Nullable-resource pattern `iconRes: Int?` mirrors `fontResId`

---

## Session — 2026-05-10 — Per-Theme Font Support (`fontResId`) - Design Phase (basher-4)

### Work Done

- Added `fontResId: Int? = null` to the `Theme` data class in `model/Theme.kt`.
- All themes in `ThemeRegistry.all` default to `null` (system default) except Rabbit and Panda, which have `fontResId = null` with explicit comment for Rusty to fill in once font resources land in `res/font/`.
- Updated `MainActivity.applyThemeColors()`: Added font application after color setup. Resolves typeface via `ResourcesCompat.getFont(this, fontResId)` and applies to `tvDisplay`, `tvExpression`, and all number/dot buttons (`btn0`–`btn9`, `btnDot`). Falls back to `Typeface.DEFAULT` when `fontResId` is null or font fails to load.
- Added imports: `android.graphics.Typeface`, `androidx.core.content.res.ResourcesCompat`.
- Wrote decision to `.squad/decisions/inbox/basher-theme-font-field.md`.

### Key Technical Decisions

- **`fontResId: Int? = null` is the Android nullable-resource pattern**: Same convention as `iconRes: Int?` already in `Theme`. Using null as "use default" and non-null as a `@FontRes` integer keeps the API simple.

- **`ResourcesCompat.getFont()` graceful fallback**: Font may fail to load (malformed XML, missing file). The `?.let { } ?: Typeface.DEFAULT` guard prevents crashes and ensures readable display.

- **Apply font on every `applyThemeColors()` call**: Font application is inside `applyThemeColors`, which runs on every theme switch. This ensures font resets to DEFAULT when switching away from Rabbit/Panda, not just when switching to them.

- **Leave font fields null when asset doesn't exist yet**: Referencing `R.font.fredoka_one` before `res/font/fredoka_one.xml` exists causes compile error. Keep as `null` with comment until asset in place — one-line activation once Rusty adds the file.

### Handed Off to Rusty

1. Add `res/font/fredoka_one.xml` (downloadable Google Fonts provider XML)
2. Add `res/values/font_certs.xml` (certificate pinning for GMS fonts)
3. Update `fontResId = R.font.fredoka_one` for RABBIT and PANDA in ThemeRegistry

### Status

✓ Architecture complete and buildable (null fontResId safe)  
⏳ Waiting for Rusty to wire actual font resources
