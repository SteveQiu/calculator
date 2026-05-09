# Decision: ThemeUnlockListener Wiring into MainActivity

**Author:** Basher  
**Date:** 2026-05-09  
**Status:** Implemented

## Context

Rusty is replacing the cycling `btnTheme` button with a `ThemePickerDialog`. The dialog needs to
trigger ad and billing flows — but dialogs should not know about `ThemeViewModel` directly.
We need a clean callback interface so `MainActivity` owns the business logic and the dialogs stay
decoupled.

## Decision

### 1. `ThemeUnlockListener` interface (updated)

Added `isThemeUnlocked(themeId: ThemeId): Boolean` to the existing interface stub so the picker
dialog can query lock state without holding a ViewModel reference.

```kotlin
interface ThemeUnlockListener {
    fun onThemeSelected(themeId: ThemeId)
    fun onWatchAdRequested(themeId: ThemeId)
    fun onPurchaseRequested(themeId: ThemeId)
    fun isThemeUnlocked(themeId: ThemeId): Boolean
}
```

### 2. `ThemePickerDialog` (new — `BottomSheetDialogFragment`)

- Layout: `dialog_theme_picker.xml` (drag handle + title + 2-column RecyclerView + close button).
- Adapter: `PickerAdapter` (private inner class, functionally identical to `ThemeAdapter` in
  `ThemePickerActivity`; kept local to avoid polluting the package namespace with a duplicate).
- Resolves `ThemeUnlockListener` via `requireActivity() as? ThemeUnlockListener` — standard
  Android DialogFragment → host pattern; no constructor injection needed.
- On **unlocked theme tap**: calls `listener.onThemeSelected(themeId)` + `dismiss()`.
- On **locked theme tap**: shows `ThemeUnlockDialog.newInstance(themeId)` via `parentFragmentManager`
  so the full colour-preview unlock sheet appears.

### 3. `ThemeUnlockDialog` (updated)

Removed `ThemeViewModel` field and `ViewModelProvider` instantiation. Both "Watch Ad" and "Buy"
buttons now resolve the listener from `requireActivity()` and call through it. This breaks the
direct ViewModel dependency and routes all unlock signals through `MainActivity`.

### 4. `MainActivity` (updated)

- Implements `ThemeUnlockListener` (was already declared but was missing `isThemeUnlocked`).
- **`onThemeSelected`** → `themeViewModel.selectTheme(themeId)` — writes active theme to DataStore.
- **`onWatchAdRequested`** → sets `themeViewModel.pendingUnlockTheme = themeId` then calls
  `themeViewModel.watchAdToUnlock(this)`. `pendingUnlockTheme` is the disambiguation key that
  maps the generic ad reward callback to the correct theme.
- **`onPurchaseRequested`** → `themeViewModel.buyTheme(this, themeId)` — queries `ProductDetails`
  from `BillingRepository` and launches `BillingClient.launchBillingFlow`.
- **`isThemeUnlocked`** → delegates to `themeViewModel.isThemeUnlocked(themeId)` which checks
  the in-memory `unlockedThemes` StateFlow snapshot; CLASSIC always returns `true` because
  `ThemeId.CLASSIC.isPremium == false`.
- Added **`observeUiEvents()`**: collects `themeViewModel.uiEvents` SharedFlow and shows a
  Snackbar for: `ThemeUnlocked` ("X unlocked!"), `Error` (full message), `AdNotReady`
  (uses `@string/ad_not_available`). `PurchaseCancelled` is a no-op.
- `btnTheme` click now opens `ThemePickerDialog.newInstance().show(...)` instead of cycling.

## Unlock State Persistence

All persistence remains in `ThemeRepository` (DataStore):
- `setActiveTheme(themeId)` writes to `KEY_ACTIVE_THEME` (String key).
- `unlockTheme(themeId)` appends to `KEY_UNLOCKED_THEMES` (StringSet key).
Both are called by `ThemeViewModel`'s billing/ad observers on confirmed events — MainActivity
never writes to DataStore directly.

## Edge Cases

| Case | Handling |
|------|----------|
| Ad not loaded yet | `AdRepository.isAdReady()` returns false → `UiEvent.AdNotReady` → Snackbar |
| Ad load failure | `FullScreenContentCallback.onAdFailedToShowFullScreenContent` → `AdResult.Error` → Snackbar |
| Billing product not found | `queryProductDetails` returns null → `UiEvent.Error` |
| User cancels billing | `BillingResponseCode.USER_CANCELED` → `UiEvent.PurchaseCancelled` → no-op |
| Already purchased (restore) | `BillingRepository.restorePurchases()` runs on every `BillingClient` connect |
| CLASSIC theme selected | `isThemeUnlocked` always `true`; `selectTheme` writes to DataStore normally |

## SKU → ThemeId Mapping

Defined on `ThemeId` enum itself (`skuId` field). `BillingRepository.handlePurchase` matches
`purchase.products` against all `ThemeId.entries` with non-null `skuId`. No separate map needed.

| ThemeId  | SKU            |
|----------|----------------|
| MIDNIGHT | theme_midnight |
| OCEAN    | theme_ocean    |
| SUNSET   | theme_sunset   |
| RABBIT   | theme_rabbit   |
| PANDA    | theme_panda    |
