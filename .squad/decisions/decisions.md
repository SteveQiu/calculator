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
