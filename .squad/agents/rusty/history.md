# Rusty — History

## Project Seed

- **Project:** Android Kotlin Calculator
- **Stack:** Kotlin, XML layouts, Material Design, ConstraintLayout
- **Requested by:** Developer
- **Goal:** Multiple premium-looking themes, runtime theme switching, locked theme preview with unlock UI (ad or Google Pay)
- **Repo:** https://github.com/SteveQiu/calculator.git

## 📋 Work Summary (2026-05-08 through 2026-05-10)

### Session 2026-05-08: Dual-Layer Color System
- Established prefixed color naming in `colors.xml` (`classic_`, `midnight_`, `ocean_`, `sunset_`)
- Created per-theme color files for style preview tools
- Implemented ThemePickerActivity with RecyclerView 2-column grid
- Added lock badge overlay and BottomSheetBehavior pattern

### Session 2026-05-09: Bug Fixes & Theme System Integration

**Bug Triage & Fixes:**
- **Bug 1 (rusty-3, ddf3ca3):** Locked theme names hidden by overlay — added `tvThemeNameOverlay` inside lock area
- **Bug 3 (rusty-3):** Display text size — removed auto-size, set explicit 64sp with bottom|end gravity
- **Visual Inspection:** Confirmed color swatches, active badge, and theme switching work correctly

**Theme System Modularization (rusty-4, rusty-5):**
- Worked with Basher to finalize Theme data class + ThemeRegistry design
- Added Glass Ice theme (premium): 11-color ice blue palette + snowflake icon
- Wired icon system: `iconRes` and `iconEmoji` fields enable both drawables and emoji
- Color naming beats per-theme directories for simplicity — future themes add ~15 colors + 1 style + 1 registry entry

**Key Technical Learnings:**
- Overlay layouts need careful bounds — either exclude areas or duplicate content
- Prefixed color naming in single `colors.xml` scales better than per-theme resource dirs
- `ivThemeIcon` (20dp, `visibility="gone"`) integrates cleanly without layout impact
- Material Design components work perfectly with programmatic theming

### 2026-05-09 — Glass Ice Theme Visuals + Wiring (rusty-4, rusty-5)

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

## Session — 2026-05-09 — Scribe: Cross-Agent Documentation & Archive

### Work Done (Scribe)
- Compiled all agent outputs (Rusty-1: ThemePickerDialog, Rusty-2: Visual Inspection, Basher-1: Unlock Wiring)
- Merged 5 decision files from inbox into unified `.squad/decisions/decisions.md`
- Created 3 orchestration logs documenting each agent's session work
- Created comprehensive session log with architecture validation and risk assessment
- Updated cross-agent history in Basher and Rusty entries
- Cleaned up 5 inbox decision files after archival

### Inspection Results Summary
- **Black screen issue:** RESOLVED (Basher's fix in commit 0c23fe7)
- **ThemePickerDialog:** Working correctly with 2-column grid
- **Theme switching:** Instant, no activity recreation needed
- **Bug triage:** 3 issues documented (1 layout, 1 state, 1 cosmetic)

### Outstanding Bugs for Team
- **Bug 1 (Major):** Locked theme cards hiding theme names — UX blocker
- **Bug 2 (Major):** PANDA theme inconsistent state (Premium badge but no lock) — confusing
- **Bug 3 (Minor):** Display text size/positioning — polish only

### Positive Findings
✅ Color swatches render correctly  
✅ Active badge functional with purple highlight  
✅ All buttons respond to taps  
✅ Theme switching works instantly  
✅ Bottom sheet drag handle and close functional

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

### 2026-05-09 — Glass Ice Theme Visuals

- **Color dedup:** Basher had already committed a `glass_ice_*` palette using the standard `{theme}_{role}` naming convention. Avoided duplicates by removing my parallel block and instead adding only the missing `glass_ice_accent` (#5BB8D4) and correcting `glass_ice_text_on_operator` to #0D2137 (dark navy, not ice blue, for contrast on operator buttons). Always check existing color entries before adding a new theme palette.
- **Snowflake drawable:** `ic_theme_glass_ice.xml` — 24dp VectorDrawable with 8-arm snowflake (4 cardinal + 4 diagonal arms), circular tips on each arm, side-branch nubs on the vertical arms, and a central circle body. All paths use `#5BB8D4` fill. No stroke needed at icon scale.
- **Card icon integration point:** Added `ivThemeIcon` (20dp ImageView, `visibility="gone"`) inside a horizontal row alongside `tvThemeName` in `item_theme_card.xml`. Adapter exposes `iconResFor(ThemeId): Int?` — returns null for all current themes, ready for Basher to add `ThemeId.GLASS_ICE -> R.drawable.ic_theme_glass_ice` when the enum entry lands.
- **Build:** `assembleDebug` — **BUILD SUCCESSFUL** (commit `bb0ec21`).



**Bug 1 fixed (item_theme_card.xml + adapters):**
- Added `tvThemeNameOverlay` TextView inside `lockOverlay` with `textColor="@android:color/white"` so the theme name is visible over the dark overlay.
- Both `ThemePickerDialog.ThemeCardAdapter` and `ThemePickerActivity.ThemeAdapter` now bind `nameOverlay.text` / `themeNameOverlay.text` alongside `name.text` in `onBindViewHolder`.
- Users can now read the theme name even when the lock overlay is fully opaque.

**Bug 3 fixed (activity_main.xml):**
- Removed `app:autoSizeTextType="uniform"` and related attributes from `tvDisplay` — auto-size fought `wrap_content` height, keeping text at ~32sp minimum.
- Set explicit `android:textSize="64sp"` for a prominent calculator display number.
- Changed `gravity` from `end` to `bottom|end`.
- Added `paddingBottom="8dp"` and `paddingEnd="16dp"` for breathing room.

**Build:** `assembleDebug` — **BUILD SUCCESSFUL** (commit `ddf3ca3`).

## Session — 2026-05-09 — Bug Fixes: Locked Card Names + Display Text Size (rusty-3)

### Work Done
- **Bug 1 (Locked theme cards hide theme names):** Added `tvThemeNameOverlay` TextView inside `lockOverlay` in `item_theme_card.xml` with `textColor="@android:color/white"`. Updated both `ThemePickerDialog.ThemeCardAdapter` and `ThemePickerActivity.ThemeAdapter` to bind the overlay text alongside the base card text.
- **Bug 3 (Display text size):** Removed `app:autoSizeTextType="uniform"` from `tvDisplay` in `activity_main.xml`. Set explicit `android:textSize="64sp"`, changed `gravity` to `bottom|end`, added `paddingBottom="8dp"` and `paddingEnd="16dp"`.

### Results
✓ Theme names now visible on locked premium cards
✓ Display number renders at 64sp with proper bottom-right alignment
✓ Build: `assembleDebug` — **BUILD SUCCESSFUL** (commit `ddf3ca3`)

## Session — 2026-05-09 — Theme System Modularization + Glass Ice Integration

### Work Done (Basher-3, Rusty-4, Rusty-5)

**Basher-3: Theme System Modularization (070fdd6)**
- Created `Theme` data class with self-describing metadata: `id`, `displayName`, `isPremium`, `colors: (Context) → ThemeColors`, `iconRes`, `iconEmoji`, `skuId`
- Created `ThemeRegistry` singleton: `all: List<Theme>`, `forId(id): Theme` with CLASSIC fallback
- Added `GLASS_ICE` to `ThemeId` enum (`isPremium=true`, `skuId="theme_glass_ice"`)
- Added `GLASS_ICE` case to `ThemeColors.toColors()` with 11 color resources
- Refactored `ThemePickerDialog`: adapter now reads `List<Theme>` from registry; all metadata (displayName, isPremium, colors, iconRes) derived from `Theme` object
- Refactored `MainActivity.applyThemeColors`: uses `ThemeRegistry.forId(id).colors(ctx)` and `theme.iconEmoji` instead of hardcoded when blocks

**Key Design:** `colors` field is function type `(Context) → ThemeColors` to defer Android resource resolution until runtime while keeping color wiring self-contained in ThemeRegistry. Callers use `theme.colors(context)` — readable and unambiguous.

**New Theme Onboarding (single file chain):**
1. Add enum entry to `ThemeId.kt`
2. Add color resources to `colors.xml`
3. Add `when` branch to `ThemeColors.kt`
4. Add `Theme(...)` entry to `ThemeRegistry.all`
Done — UI auto-discovers via `ThemeRegistry.all`.

**Rusty-4: Glass Ice Theme Visuals (bb0ec21)**
- 11-color palette: ice blue background (#E8F4FD), white/frost/sky blue buttons, navy primary text (#1A3A5C), slate secondary (#4A7A9B), dark navy operator text (#0D2137 for ~8:1 contrast)
- Snowflake icon (`ic_theme_glass_ice.xml`): 24dp VectorDrawable with central circle, 8 arms (cardinal + diagonal), side branches, circular tips; fill color #5BB8D4
- Card layout: Added `ivThemeIcon` (20dp ImageView) in horizontal LinearLayout alongside `tvThemeName`; visibility controlled per theme

**Rationale for operator text:** Operator buttons use light ice blue background. Dark navy (#0D2137) provides ~8:1 contrast; lighter (#1A3A5C) would only give ~5:1.

**Rusty-5: Glass Ice Wiring (d40fa0b)**
- Wired `R.drawable.ic_theme_glass_ice` into ThemeRegistry GLASS_ICE entry via `iconRes` field
- Snowflake icon now displays in theme picker card alongside theme name

### Key Technical Learnings

- **`(Context) → ThemeColors` as a field type:** Storing color factories as lambdas inside `Theme` defers context-dependent resolution without making Theme abstract. Callers use `theme.colors(context)` — natural and readable. This is the right pattern for context-dependent values in an otherwise pure data object.

- **ThemeRegistry delegates to `toColors()` extension:** Rather than duplicating color resource references, each `Theme` entry's lambda calls `themeId.toColors(ctx)`. This keeps `colors.xml` as the single source of truth and `toColors()` as the canonical builder.

- **Icon system is extensible:** `iconRes` and `iconEmoji` fields enable both Vector drawables and emoji. Hidden by default (`visibility="gone"`) — no impact for themes without icons. Future themes only need drawable + one-line adapter update.

- **When-block icon mapping is a code smell:** The hidden coupling (adding a theme didn't error if you forgot icon) is eliminated by moving all metadata to `Theme` and requiring explicit registration in ThemeRegistry.

### Architectural Integrity

- **Theme + ThemeRegistry design:** Self-describing themes with compile-time registry enforce that all metadata is centralized. Forgetting a field is a build error. UI components (`ThemePickerDialog`, `MainActivity`) automatically discover and use themes via the registry.
- **Color function type:** Cleanest solution for context-dependent color resolution in an immutable data class.
- **No regressions:** All current themes unaffected; Glass Ice integrated end-to-end (Kotlin → visuals → UI).

### Build Status
✓ `assembleDebug` — **BUILD SUCCESSFUL** (commits 070fdd6, bb0ec21, d40fa0b)

## Learnings

### 2026-05-09 — Fredoka One font for Rabbit & Panda themes

- **Fredoka One beats Nunito for kid themes:** Chunkier, rounder letterforms read as "playful toy" at large display sizes. Nunito is more neutral/school-appropriate; Fredoka One is pure joy.
- **Downloadable fonts require cert pinning:** `res/font/<name>.xml` with `app:fontProviderCerts="@array/com_google_android_gms_fonts_certs"` needs a corresponding `values/font_certs.xml` defining the Google production cert. This is a one-time project setup — all future downloadable fonts reuse the same cert array.
- **Apply typeface after color in `applyThemeColors()`:** Both `tvDisplay` and `tvExpression` need typeface reset on every theme switch; otherwise a stale Fredoka One typeface stays on those views when switching away from Rabbit/Panda. `Typeface.DEFAULT` as the fallback handles this cleanly.
- **`fontResId` field in Theme is extensible:** Any future theme wanting a custom font (e.g., a gothic dark theme) just sets `fontResId` in its ThemeRegistry entry — zero MainActivity changes needed.
- **Check for pre-existing placeholder fields:** Basher had already added `fontResId: Int? = null` to the `Theme` data class with `null` placeholders in ThemeRegistry awaiting this work. Always read the full current state of files before assuming a field needs to be added.

## Session — 2026-05-10 — Per-Theme Font Support (`fontResId`) - Implementation Phase (rusty-6)

### Work Done

- Added Fredoka One downloadable font via Google Fonts provider XML (`res/font/fredoka_one.xml`)
- Created certificate pinning array (`res/values/font_certs.xml`) for GMS font provider security
- Integrated font into ThemeRegistry:
  - Set `fontResId = R.font.fredoka_one` for RABBIT Theme entry
  - Set `fontResId = R.font.fredoka_one` for PANDA Theme entry
  - All other themes (CLASSIC, MIDNIGHT, OCEAN, SUNSET, GLASS_ICE) retain `fontResId = null` (system default)
- Font application already wired by Basher in `MainActivity.applyThemeColors()`—`tvDisplay`, `tvExpression`, and number/dot buttons now render Fredoka One for Rabbit & Panda
- Committed as commit `8f09f83`

### Key Technical Implementation Details

- **Fredoka One Font Choice:** Selected over Nunito for these reasons:
  - Rounder, chunkier letterforms — more "chunky toy" energy kids love
  - Single weight only (One) keeps things simple; no weight selection needed
  - Immediately recognizable as "fun" without illegibility at large sizes (64sp display)
  - Excellent numeral design — digits are clear and friendly, not ambiguous

- **Downloadable Fonts via Google Fonts Provider:** No binary TTF committed to repo. Font downloads at runtime via Play Services when app first needs it. Requires `com_google_android_gms_fonts_certs` array for certificate pinning (security requirement).

- **Certificate Array is one-time project setup:** New file since project had no cert array. All future downloadable fonts reuse the same `com_google_android_gms_fonts_certs` array — no additional setup needed.

- **Font loads asynchronously:** First use may show system default while font downloads. Fallback to `Typeface.DEFAULT` until GMS font loads. No user-facing errors.

- **Zero impact on other themes:** Classic, Midnight, Ocean, Sunset, Glass Ice continue using system default typeface. Future themes can opt in by setting `fontResId` in ThemeRegistry.

### Results

✓ Rabbit and Panda themes display Fredoka One on all digit surfaces (display + number buttons)  
✓ Font loads asynchronously; graceful fallback to system default  
✓ Zero regression on other 5 themes  
✓ Architecture scales for future per-theme customization  
✓ Commit: `8f09f83`

