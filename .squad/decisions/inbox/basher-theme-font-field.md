# Decision: Per-Theme Font Support via `fontResId`

**Author:** Basher  
**Date:** 2026-05-09T19:45:52.463-07:00  
**Status:** Implemented

## Context

The developer wants Rabbit and Panda themes to use a cute, kid-friendly font (e.g. Fredoka One) on
the calculator display and number buttons. Other themes should use the system default. We need a
clean data-model hook so Rusty can drop a font file into `res/font/` and set the ID in one place
without touching any other code.

## Decision

Add an optional `fontResId: Int? = null` field to the `Theme` data class.

- `null` → use `Typeface.DEFAULT` (unchanged for Classic, Midnight, Ocean, Sunset, Glass Ice)
- Non-null → `ResourcesCompat.getFont(context, fontResId)` applied to `tvDisplay` and all
  number/dot buttons in `MainActivity.applyThemeColors()`

Rabbit and Panda entries in `ThemeRegistry.all` have `fontResId = null` with a comment indicating
Rusty will fill in the real `R.font.*` reference once the font file is added to `res/font/`.

## Rationale

- **No compile-time dependency on missing font files** — leaving the field null keeps the build
  clean until the font XML is actually in place.
- **One-line activation** — Rusty changes `fontResId = null` to `fontResId = R.font.fredoka_one`
  in two theme entries; nothing else needs touching.
- **Consistent with existing pattern** — `iconRes: Int?` already uses the same nullable-resource
  pattern for `ic_theme_glass_ice`; `fontResId` follows the same convention.
- **Applied to display + number buttons** — these are the digits kids interact with most; operator
  and special buttons retain the default font to keep them readable for all ages.

## Touch Points

| File | Change |
|------|--------|
| `model/Theme.kt` | Added `fontResId: Int? = null` to `Theme` data class; added comments on Rabbit/Panda registry entries |
| `MainActivity.kt` | Added `android.graphics.Typeface` + `androidx.core.content.res.ResourcesCompat` imports; applied typeface to `tvDisplay` and number buttons at end of `applyThemeColors()` |

## Consequences

- All existing themes are unaffected (default `null` preserves current behavior).
- Rusty needs to: (1) add font XML to `res/font/`, (2) set `fontResId = R.font.<name>` for Rabbit
  and Panda in `ThemeRegistry.all`.
- `ResourcesCompat.getFont()` may return `null` for malformed font files; the `?.let { } ?: Typeface.DEFAULT` guard handles that safely.
