# Project Decisions

## Decision: Panda (and Rabbit) Theme Lock/Badge Behaviour

**Date:** 2026-05-09  
**Author:** Basher  
**Status:** Implemented — commit `de2f771`

---

### Problem

The Panda theme card in `ThemePickerDialog` showed a **"Premium" badge with no lock
overlay, no "Watch Ad" button, and no "Buy" button** — visually contradicting itself.

### Root Cause

Two compounding issues:

1. **Badge text was always "Premium" for every `isPremium = true` theme**, regardless
   of whether the theme was locked or already unlocked (purchased). So when PANDA was
   in the DataStore unlocked set (e.g. from a dev/test call to `unlockTheme(PANDA)`),
   the card displayed: `"Premium"` badge + no lock overlay + no CTAs. Confusing UX.

2. **`unlockedThemesFlow` included free themes (CLASSIC)** via the old default
   `setOf(ThemeId.CLASSIC.name)`. Free themes don't belong in the persisted unlock set
   because their accessibility is determined entirely by `!isPremium`, not DataStore.

### Decision

**RABBIT and PANDA are premium (locked) themes**, identical to MIDNIGHT / OCEAN / SUNSET.
They require either a rewarded ad watch or an in-app purchase to unlock.

`ThemeId.kt` already had `isPremium = true` and the correct `skuId` for both — no
change was needed there.

### Changes Made

| File | Change |
|------|--------|
| `ThemePickerDialog.kt` | Badge: `isPremium -> "Premium"` → `isUnlocked -> "✓ Owned"` / `else -> "Premium"` |
| `ThemePickerActivity.kt` | Same badge fix in `ThemeAdapter.onBindViewHolder` |
| `ThemeRepository.kt` | `unlockedThemesFlow` filter: `it.name in names` → `it.isPremium && it.name in names` |
| `ThemeViewModel.kt` | StateFlow initial value: `setOf(ThemeId.CLASSIC)` → `emptySet()` |

### Resulting Behaviour

| State | Badge | Lock overlay | CTAs |
|-------|-------|-------------|------|
| Locked premium (PANDA not purchased) | "Premium" | Visible (🔒) | Watch Ad + Buy |
| Unlocked premium (PANDA purchased) | "✓ Owned" | Gone | Card selectable |
| Free (CLASSIC) | "Free" | Gone | Card selectable |
| Active (any) | "✓ Active" | Gone | Card selectable |
