# Decisions Archive

## 2026-05-09

### [RESOLVED] UI Black Screen Issue — PANDA Theme (Basher)

**Date:** 2026-05-09  
**Status:** Implemented  
**Commit:** 0c23fe7  
**Summary:** PANDA theme caused completely black screen due to dark Material theme base, black background override, and async color application. Fixed by switching to Light base theme, replacing dark color overrides, and adding synchronous `applyThemeColors()` in `onCreate`.

**Root Causes:**
1. Dark Material theme base interfered with programmatic `backgroundTintList` overrides
2. Black initial background in dark mode with async color application window
3. No synchronous color application on first frame

**Solution:**
- `values-night/themes.xml`: Switch from `Theme.MaterialComponents.NoActionBar` to `Theme.MaterialComponents.Light.NoActionBar`
- `values-night/colors.xml`: Replace black overrides with light color aliases
- `MainActivity.onCreate`: Add synchronous `applyThemeColors(themeViewModel.activeTheme.value)` before `observeTheme()`

**Consequences:**
- PANDA, RABBIT, CLASSIC themes render correctly in both light and dark system modes
- System status/nav bars remain light-colored in dark mode (can be updated programmatically later)
- `applyThemeColors()` called twice on startup (harmless, second call shows correct saved theme)

---

### Theme Picker — BottomSheet UI Replacement (Rusty)

**Date:** 2026-05-09  
**Status:** Implemented  
**Summary:** Replaced theme cycling button with `ThemePickerBottomSheet` — a `BottomSheetDialogFragment` showing all 6 themes in a 2-column grid.

**Architecture:**
- **UI:** `ThemePickerBottomSheet` (BottomSheetDialogFragment) + `ThemePickerAdapter` (inner)
- **Interface:** `ThemeUnlockListener` (decouples fragment from billing/ad logic)
- **Host:** `MainActivity` implements `ThemeUnlockListener`, delegates to `ThemeViewModel`
- **Layouts:** `dialog_theme_picker.xml` (bottom sheet) + `item_theme_card.xml` (cards with inline unlock buttons)

**Card Visual States:**
| State | Visual |
|---|---|
| Unlocked, not active | Normal card with theme swatches |
| Unlocked, active | Colored stroke ring (6dp) + "✓ Active" badge |
| Locked | Semi-transparent overlay (#CC000000) with 🔒, "Watch Ad", "Buy $0.99" |

**Listener Callbacks:**
- `onThemeSelected(themeId)` → write to DataStore, dismiss
- `onWatchAdRequested(themeId)` → route to `ThemeViewModel.watchAdToUnlock()`
- `onPurchaseRequested(themeId)` → route to `ThemeViewModel.buyTheme()`
- `isThemeUnlocked(themeId)` → check unlock state without ViewModel reference

**Consequences:**
- `ThemePickerActivity` now secondary (can be deleted later)
- `themePickerLauncher` removed from MainActivity
- No activity recreation needed (faster than old result launcher pattern)

---

### ThemeUnlockListener Wiring — Ad & Billing Integration (Basher)

**Date:** 2026-05-09  
**Status:** Implemented  
**Summary:** Wired `ThemeUnlockListener` interface into `MainActivity` to decouple dialogs from `ThemeViewModel` and centralize unlock business logic.

**Key Changes:**

1. **`ThemeUnlockListener` Interface (enhanced):**
   ```kotlin
   interface ThemeUnlockListener {
       fun onThemeSelected(themeId: ThemeId)
       fun onWatchAdRequested(themeId: ThemeId)
       fun onPurchaseRequested(themeId: ThemeId)
       fun isThemeUnlocked(themeId: ThemeId): Boolean
   }
   ```

2. **`ThemePickerDialog` (new):**
   - Resolves listener via `requireActivity() as? ThemeUnlockListener` (standard Android pattern)
   - On unlocked tap: `listener.onThemeSelected(themeId)` + dismiss
   - On locked tap: shows `ThemeUnlockDialog.newInstance(themeId)`

3. **`ThemeUnlockDialog` (updated):**
   - Removed `ThemeViewModel` field
   - Both ad/buy buttons resolve listener from `requireActivity()` and call through it

4. **`MainActivity` (updated):**
   - Implements `ThemeUnlockListener`
   - **`onThemeSelected`** → `themeViewModel.selectTheme(themeId)`
   - **`onWatchAdRequested`** → set `pendingUnlockTheme`, call `themeViewModel.watchAdToUnlock(this)`
   - **`onPurchaseRequested`** → `themeViewModel.buyTheme(this, themeId)`
   - **`isThemeUnlocked`** → delegate to `themeViewModel.isThemeUnlocked(themeId)` (CLASSIC always unlocked)
   - **`observeUiEvents()`** → collect `themeViewModel.uiEvents` and show Snackbar for:
     - `ThemeUnlocked` ("X unlocked!")
     - `Error` (full message)
     - `AdNotReady` (uses `@string/ad_not_available`)
     - `PurchaseCancelled` (no-op)
   - `btnTheme` now opens `ThemePickerDialog` instead of cycling

**Persistence:**
- All unlock state remains in `ThemeRepository` (DataStore)
- `setActiveTheme(themeId)` writes to `KEY_ACTIVE_THEME`
- `unlockTheme(themeId)` appends to `KEY_UNLOCKED_THEMES`

**SKU → ThemeId Mapping:**
| ThemeId  | SKU            | Premium |
|----------|----------------|---------|
| MIDNIGHT | theme_midnight | Yes     |
| OCEAN    | theme_ocean    | Yes     |
| SUNSET   | theme_sunset   | Yes     |
| RABBIT   | theme_rabbit   | Yes     |
| PANDA    | theme_panda    | Yes     |
| CLASSIC  | (none)         | No      |

---

## Bugs Found — Visual Inspection (Rusty, 2026-05-09)

**Inspector:** Rusty (Android UI Dev)  
**Build:** Debug APK  
**Device:** emulator-5554 (sdk_gphone16k_x86_64)

### ✅ Overall Status

✅ **Black screen issue RESOLVED** — app renders correctly  
✅ **ThemePickerDialog** opens successfully via bottom sheet  
✅ **2-column grid layout** working  

### 🔴 Bug 1: Locked Theme Cards Missing Theme Names

**Severity:** Major — UX blocker  
**Root Cause:** `lockOverlay` LinearLayout covers entire card including `tvThemeName`  
**Fix:** Modify `item_theme_card.xml` to exclude name area from overlay, or include name inside overlay  
**Impact:** Users cannot see which premium theme they're about to unlock

### 🔴 Bug 2: PANDA Theme Lock State Inconsistent

**Severity:** Major — confusing UX  
**Observed:** Panda card shows "Premium" badge but NO lock overlay, no unlock buttons  
**Root Cause:** Either PANDA unlock check returning `true` incorrectly, or badge text is wrong  
**Fix:** Either add PANDA to locked themes list, or change badge from "Premium" to "Free"  
**Impact:** Badge-unlock state mismatch

### 🟡 Bug 3: Display Text Positioning/Size (Minor)

**Severity:** Minor — cosmetic  
**Observed:** "0" appears small and bottom-right in display area  
**Root Cause:** Incorrect `gravity`, `textSize`, or layout weight  
**Fix:** Review `activity_main.xml` — ensure large text (56sp+) right-aligned at bottom  
**Impact:** Not visually polished but functional

### ✅ Positive Findings

- Color swatches render correctly for all themes
- Classic & Midnight (unlocked) display properly
- "✓ Active" badge shows correctly
- Active theme has purple highlight border
- Bottom sheet drag handle visible, close button functional
- All calculator buttons respond to taps
- Theme switching works

---

### Theme System Modularization + ThemeRegistry (Basher)

**Date:** 2026-05-09  
**Status:** Implemented  
**Commit:** 070fdd6  

**Context:** Original theme system scattered metadata across three files (`ThemeId.kt`, `ThemeColors.kt`, `ThemePickerDialog.kt`, `MainActivity.kt`). Adding a new theme required editing 4 files with no compile-time reminder if one was missed.

**Solution:** Created self-describing `Theme` data class and `ThemeRegistry` singleton:

- **`model/Theme.kt`** — `Theme` data class with color function type `(Context) -> ThemeColors`
- **`model/ThemeRegistry.kt`** — singleton holding all themes in `all: List<Theme>` with `forId()` lookup
- **New theme onboarding:** Add enum entry in `ThemeId.kt` → add color resources in `colors.xml` → add `when` branch in `ThemeColors.kt` → add `Theme(...)` entry in `ThemeRegistry.all`. Done.

**Key Design:**
- `colors` is a function `(Context) -> ThemeColors` to defer color resource resolution until runtime
- `ThemeRegistry.forId()` falls back to CLASSIC for unknown theme IDs
- `toColors()` extension preserved as canonical color builder; ThemeRegistry delegates to it
- `ThemeId` enum still carries `displayName`/`isPremium`/`skuId` for sync; kept both for backward compatibility

**GLASS_ICE Palette:**
- Background: `#E8F4FD`
- Buttons: white (number), `#D0EAF8` (special), `#B3D9F5` (operator)
- Text: `#1A3A5C` primary, `#4A7A9B` secondary, `#1A3A5C` on all buttons
- Display bg: `#F0F8FF`

---

### Glass Ice Theme — Visuals (Rusty)

**Date:** 2026-05-09  
**Status:** Implemented  
**Commit:** bb0ec21  

**Deliverables:**
1. **Color palette** (`colors.xml`): 11 `glass_ice_*` resources matching Basher's convention
   - Operator button text = `#0D2137` (dark navy) for contrast on light ice blue buttons (~8:1 contrast)
   - Added `glass_ice_accent = #5BB8D4` for snowflake icon and highlights
2. **Snowflake icon** (`ic_theme_glass_ice.xml`): 24dp VectorDrawable
   - Central circle + 8 arms (4 cardinal, 4 diagonal)
   - Rounded proportions with side branches for snowflake realism
   - Circular tips (r≈0.75dp) for "cute dot" effect
   - Fill: `#5BB8D4` (glass_ice_accent), single flat color
3. **Card layout** (`item_theme_card.xml`): Added `ivThemeIcon` (20dp ImageView) in horizontal layout beside `tvThemeName`
   - Icon visibility controlled per theme
   - Hidden by default with `visibility="gone"` (no layout impact)

**Adapter integration:** `ThemePickerDialog.ThemeCardAdapter` has `iconResFor(ThemeId): Int?` helper. Wire Glass Ice by adding one line in implementation.

---

### Glass Ice Wiring — Registry Integration (Rusty)

**Date:** 2026-05-09  
**Status:** Implemented  
**Commit:** d40fa0b  

**Change:** Wired `R.drawable.ic_theme_glass_ice` into `ThemeRegistry` GLASS_ICE entry via `iconRes` field.

**Result:** Glass Ice icon now displays in theme picker card alongside theme name.
