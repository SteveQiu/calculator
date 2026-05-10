# Decision: Kid-Friendly Font for Rabbit & Panda Themes

**Agent:** Rusty  
**Date:** 2026-05-09T19:45:52.463-07:00  
**Status:** Implemented

## Context

The Rabbit 🐰🥕 and Panda 🐼 themes target young/family audiences. A standard system font feels too utilitarian for these themes. The developer requested a cute, kid-friendly font for the number display and digit buttons on these two themes.

## Decision

**Font chosen: Fredoka One**

Fredoka One was selected over Nunito for the following reasons:
- Rounder, chunkier letterforms — more "chunky toy" energy that kids love
- Single weight only (One) keeps things simple; no weight selection needed
- Immediately recognisable as "fun" without being illegible at large sizes (64sp display)
- Excellent numeral design — digits are clear and friendly, not ambiguous

## Implementation

### Font resource (`app/src/main/res/font/fredoka_one.xml`)
Downloadable font XML using the Google Fonts provider (GMS). No binary TTF committed to the repo — downloads at runtime via Play Services. Requires `com_google_android_gms_fonts_certs` array for certificate pinning.

### Certificate array (`app/src/main/res/values/font_certs.xml`)
Standard Google production signing cert array. Created new file since project had none. This is required for all downloadable fonts via the GMS provider.

### ThemeRegistry (`Theme.kt`)
Set `fontResId = R.font.fredoka_one` for both `RABBIT` and `PANDA` Theme entries. All other themes retain `fontResId = null` → system default typeface.

The `fontResId` field was already present in the `Theme` data class (added by Basher) with placeholder `null` values awaiting this PR.

### MainActivity (`applyThemeColors()`)
Font application added at the end of `applyThemeColors()`:
- `tvDisplay.typeface` — the big number display
- `tvExpression.typeface` — the secondary expression line  
- All 11 number/dot buttons (`btn0`–`btn9`, `btnDot`) — digits feel tactile and cute

When `fontResId` is null (all other themes), `Typeface.DEFAULT` is used — no regression.

## Consequences

- Rabbit and Panda themes now display Fredoka One on all digit surfaces
- Font loads asynchronously on first use; fallback to system default until downloaded
- Zero impact on Classic, Midnight, Ocean, Sunset, Glass Ice themes
- Future themes can opt in by setting `fontResId` in their ThemeRegistry entry
