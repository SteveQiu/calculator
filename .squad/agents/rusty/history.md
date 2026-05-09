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

