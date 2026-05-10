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

---

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
