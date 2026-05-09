# Skill: Android BottomSheetDialogFragment Pattern

**Category:** Android UI  
**Language:** Kotlin  
**Min SDK:** 24  

## When to use

Use `BottomSheetDialogFragment` instead of a full `DialogFragment` or separate `Activity` when:
- The content fits in roughly 50–75% of screen height
- The user needs to select from a list/grid without losing context of the parent screen
- Dismiss-on-outside-tap behavior is desired
- You want the system back button to dismiss the sheet naturally

## Pattern

### 1. Fragment class

```kotlin
class MyBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "MyBottomSheet"
        fun newInstance(): MyBottomSheet = MyBottomSheet()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        inflater.inflate(R.layout.bottom_sheet_my, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Force fully expanded — skip the half-peek state
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }
    }
}
```

### 2. Layout file (`bottom_sheet_*.xml` or `dialog_*.xml`)

```xml
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:background="@color/colorBackground"
    android:paddingBottom="24dp">

    <!-- Standard drag handle -->
    <View
        android:layout_width="40dp"
        android:layout_height="4dp"
        android:layout_gravity="center_horizontal"
        android:layout_marginTop="12dp"
        android:layout_marginBottom="8dp"
        android:background="#44808080" />

    <!-- Content here -->

</LinearLayout>
```

Key: root is `wrap_content` height — the sheet wraps its content. Don't use `match_parent` unless you want a full-height sheet.

### 3. Show from Activity/Fragment

```kotlin
MyBottomSheet.newInstance().show(supportFragmentManager, MyBottomSheet.TAG)
```

### 4. Host contract via listener (fragment → activity)

When the sheet needs to call back into the host, use an interface cast in `onAttach`:

```kotlin
private lateinit var listener: MyListener

override fun onAttach(context: Context) {
    super.onAttach(context)
    listener = context as? MyListener
        ?: throw IllegalStateException("Host must implement MyListener")
}
```

This is safer than `setTargetFragment` (deprecated) and cleaner than shared ViewModel events for UI actions.

### 5. Shared ViewModel (fragment shares Activity's VM)

```kotlin
// In the fragment — gets the Activity's existing ViewModel instance:
private val viewModel: MyViewModel by activityViewModels {
    MyViewModelFactory(AppModule.provideRepo(requireContext()))
}
```

Use `activityViewModels` when the fragment needs to observe state already live in the Activity, avoiding double-initialization.

## Pitfalls

- **`wrap_content` on RecyclerView inside sheet**: works fine; sheet height = content height. For long lists, set `android:maxHeight` on the RecyclerView or use `NestedScrollView` if the list might exceed the screen.
- **State expansion on show**: without `STATE_EXPANDED + skipCollapsed = true`, the sheet defaults to a half-peek state (50% of screen). Always set expansion explicitly for grids/pickers.
- **Keyboard overlap**: For sheets with text input, use `WindowCompat.setDecorFitsSystemWindows(false)` and `ViewCompat.setOnApplyWindowInsetsListener` to push content above the keyboard.
- **Background color**: The sheet background comes from `?attr/colorSurface` by default. Override in the layout root or via a custom `BottomSheetDialog` style to match your theme.

## Real example in this project

`ThemePickerBottomSheet` — shows 6 theme cards in a 2-column grid with inline unlock CTAs for locked themes. See:
- `app/src/main/java/com/example/calculator/ui/ThemePickerBottomSheet.kt`
- `app/src/main/res/layout/dialog_theme_picker.xml`
