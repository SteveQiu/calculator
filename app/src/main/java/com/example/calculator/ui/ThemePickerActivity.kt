package com.example.calculator.ui

import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.example.calculator.di.AppModule
import com.example.calculator.viewmodel.ThemeViewModel

/**
 * Displays all available themes in a grid/list.
 * Each theme card shows:
 *   - Theme name and colour preview
 *   - A lock overlay when isPremium && !isUnlocked
 *
 * Tapping a locked theme → ThemeViewModel.requestUnlock(themeId)
 *   which triggers ThemeUnlockDialog.
 * Tapping an unlocked theme → ThemeViewModel.setActiveTheme(themeId)
 *   which persists the selection and finishes this activity so
 *   MainActivity can recreate itself with the new theme.
 *
 * TODO: Inflate R.layout.activity_theme_picker (RecyclerView + toolbar)
 * TODO: Observe ThemeViewModel.themeList and ThemeViewModel.unlockState
 * TODO: Wire RecyclerView adapter (ThemeCardAdapter) to ViewModel actions
 * TODO: Show ThemeUnlockDialog on ThemeViewModel.unlockRequest event
 */
class ThemePickerActivity : AppCompatActivity() {

    private val themeViewModel: ThemeViewModel by viewModels {
        AppModule.themeViewModelFactory(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // TODO: setContentView(R.layout.activity_theme_picker)
        // TODO: Set up toolbar with back navigation
        // TODO: Observe themeViewModel.themeList to populate adapter
        // TODO: Observe themeViewModel.unlockRequest to show ThemeUnlockDialog
    }
}
