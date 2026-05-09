# Rusty — History

## Project Seed

- **Project:** Android Kotlin Calculator
- **Stack:** Kotlin, XML layouts, Material Design, ConstraintLayout
- **Requested by:** Developer
- **Goal:** Multiple premium-looking themes, runtime theme switching, locked theme preview with unlock UI (ad or Google Pay)
- **Repo:** https://github.com/SteveQiu/calculator.git

## Learnings

_Appended by Rusty after each session._

### 2026-05-08 — Multi-theme color system + picker/unlock UI

- **Color system:** Established a dual-layer color strategy. All theme palettes live in `res/values/colors.xml` with clear prefixes (`classic_`, `midnight_`, `ocean_`, `sunset_`). Backward-compat aliases (`colorBackground`, etc.) point at classic_ colors so existing layouts and styles compile unchanged.
- **Per-theme files:** Created `colors_midnight.xml`, `colors_ocean.xml`, `colors_sunset.xml` as self-contained palette references for style preview tools and future compile-time theme variants.
- **Theme overlays:** Added `ThemeOverlay.Calculator.{Midnight,Ocean,Sunset}` styles in `themes.xml`. These use `parent=""` so they are pure overlays — applied programmatically by the ViewModel/Activity without recreating the Activity.
- **Drawable shapes:** Layouts reference three lightweight drawables (`shape_palette_dot`, `bg_theme_preview_card`, `bg_theme_preview_dialog`) to avoid hardcoded colors in XML. The dot's fill color is overridden at runtime by the adapter.
- **Layout pattern:** `activity_theme_picker.xml` delegates to a `RecyclerView` with `GridLayoutManager` (spanCount=2). `item_theme_card.xml` uses a `FrameLayout` overlay for the lock badge so it can be shown/hidden without re-inflating. `dialog_theme_unlock.xml` is a `CoordinatorLayout` + `BottomSheetBehavior` pattern — no custom dialog needed.
- **String keys:** All user-facing copy is in `strings.xml`; no hardcoded text in layouts.

### 2026-05-09 — Cross-Team Integration Validated ✅

- **Basher's implementation** proved the color system works: `ThemeId.toColors(context)` reads `R.color.*` entries from `colors.xml` exactly as expected. Prefixed naming enables direct lookups without hardcoded mappings.
- **Danny's architecture** scaled the color system: theme overlays apply cleanly via activity recreation; no reimplementation of Material components needed.
- **Linus's tests** validate all 4 theme enum values and color resource references. ThemeIdTest passes 100%; no color naming conflicts detected.
- **Material Design integration:** `MaterialCardView` with runtime stroke coloring (via Basher's `backgroundTintList` approach) works perfectly for active theme highlight. No Material components broken by theme switching.
- **Insight:** Prefixed color naming in a single `colors.xml` beats per-theme resource directories for simplicity and maintainability. Future themes add ~15 colors + 1 overlay style + 1 adapter entry — mechanical work, zero risk.

### 2026-05-09 — Emulator Visual Inspection 🐛 CRITICAL BUG FOUND

**Inspection Results:**
- **Device:** emulator-5554 connected and responsive
- **App:** com.example.calculator launches successfully (LaunchState: COLD, TotalTime: ~5s)
- **Active theme:** Panda (🐼 emoji shown on btnTheme button)

**UI Hierarchy Verified (all elements present):**
- `rootLayout` (LinearLayout) — full screen bounds [0,118][1080,2337]
- **Top bar:** "Calculator" title + theme toggle button (🐼)
- **Display area:** `tvExpression` (empty), `tvDisplay` showing "0"
- **Button grid (5×4):**
  - Row 1: AC, +/−, %, ÷
  - Row 2: 7, 8, 9, ×
  - Row 3: 4, 5, 6, −
  - Row 4: 1, 2, 3, +
  - Row 5: 0, ., ⌫, =

**🐛 CRITICAL BUG:** Screen renders **completely black** despite all UI elements being present in the view hierarchy with correct bounds. Colors from Panda theme (#F5F5F5 background, white buttons) should display light UI, but actual rendering is solid black. This appears to be a runtime theme application failure — the theme overlay style attributes are not being propagated to the MaterialButton/TextView views after theme switch.

**Cross-reference:** Layout XML (`activity_main.xml`) matches hierarchy — no missing views. Color values in `colors.xml` are correct. Issue is in runtime theme binding, not static resources.

**Recommendation:** Investigate `ThemeManager` or `MainActivity.applyTheme()` — the overlay may be set but views are not refreshing their tints/backgroundTint from the new context.
