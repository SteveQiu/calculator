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

### 2026-05-09 — ThemePickerBottomSheet implemented

- **BottomSheet over Activity:** Replaced the cycling `btnTheme` click handler with `ThemePickerBottomSheet` (a `BottomSheetDialogFragment`). This eliminates the need for `ActivityResultLauncher` and avoids a full-screen navigation break for a quick theme swap.
- **ThemeUnlockListener interface:** Defined the clean 3-method interface (`onThemeSelected`, `onWatchAdRequested`, `onPurchaseRequested`). `MainActivity` implements it and delegates to `ThemeViewModel` — keeps the fragment decoupled from billing/ad logic entirely.
- **Inline unlock CTAs in card:** Updated `item_theme_card.xml` to replace the simple lock-icon `FrameLayout` overlay with a `LinearLayout` containing 🔒 emoji + "Watch Ad" (outlined button) + "Buy for $0.99" (operator-styled button). Users see unlock options without navigating to a separate dialog.
- **`activityViewModels` in fragment:** `ThemePickerBottomSheet` uses `activityViewModels { ThemeViewModelFactory(...) }` to share the same `ThemeViewModel` instance already live in `MainActivity` — no double-initialization.
- **Expand on show:** Set `BottomSheetBehavior.STATE_EXPANDED + skipCollapsed = true` so all 6 cards are visible immediately; no half-peek state.
- **Active badge:** Changed `tvBadge` text from "Free"/"Premium" to "✓ Active" when the theme matches the current active theme — gives clear visual confirmation alongside the colored stroke ring.
- **`ThemePickerActivity` preserved:** The existing activity-based picker is still present and updated to handle the new `btnCardWatchAd`/`btnCardBuy` IDs (routes to `ThemeUnlockDialog`). Can be deprecated in a future cleanup pass.


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

### 2026-05-09 — Visual Inspection #2 (Post-Fix)

**Good news:** The black screen issue from earlier inspection is RESOLVED. Calculator now renders correctly with the Midnight theme active.

**ThemePickerDialog Inspection:**
- BottomSheetDialogFragment opens correctly from moon icon tap
- 2-column RecyclerView grid displays 6 theme cards
- Color swatches (3 dots) render properly for all themes
- Active theme (Midnight) has purple highlight border + "✓ Active" badge
- "WATCH AD" and "BUY FOR $0.99" buttons visible on locked themes

**Bugs Found:**
1. **Locked theme cards hide theme names** — The `lockOverlay` covers the entire card, obscuring the theme name. Users can't tell which premium theme they're about to unlock without recognizing the color palette.
2. **Panda theme state inconsistency** — Shows "Premium" badge but NO lock overlay or unlock buttons. Either it should be locked, or the badge should say "Free".
3. **Display text size** — The "0" on main screen appears small; should be larger for calculator aesthetic.

**Learnings:**
- Overlay layouts that cover sibling views need careful bounds management. Either exclude certain areas from the overlay, or duplicate the content inside it.
- Unlock state and badge text should be derived from the same source of truth to avoid drift.
