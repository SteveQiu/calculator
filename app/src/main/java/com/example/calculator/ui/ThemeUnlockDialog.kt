package com.example.calculator.ui

import android.app.Dialog
import android.os.Bundle
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.example.calculator.di.AppModule
import com.example.calculator.model.ThemeId
import com.example.calculator.viewmodel.ThemeViewModel

/**
 * Modal dialog shown when a user taps a locked premium theme.
 *
 * Presents two unlock options:
 *   1. "Watch Ad"  → AdRepository.showRewardedAd() → ThemeRepository.unlockTheme()
 *   2. "Buy ($0.99)" → BillingRepository.launchPurchaseFlow() → ThemeRepository.unlockTheme()
 *
 * Usage:
 *   ThemeUnlockDialog.newInstance(ThemeId.MIDNIGHT)
 *       .show(supportFragmentManager, ThemeUnlockDialog.TAG)
 *
 * TODO: Build AlertDialog with custom layout (R.layout.dialog_theme_unlock)
 * TODO: Display theme name and preview colour swatch
 * TODO: "Watch Ad" button → themeViewModel.unlockWithAd(themeId, requireActivity())
 * TODO: "Buy" button → themeViewModel.unlockWithPurchase(themeId, requireActivity())
 * TODO: Dismiss dialog on successful unlock (observe ThemeViewModel.unlockSuccess event)
 */
class ThemeUnlockDialog : DialogFragment() {

    private val themeViewModel: ThemeViewModel by activityViewModels {
        AppModule.themeViewModelFactory(requireContext().applicationContext)
    }

    private val themeId: ThemeId
        get() = ThemeId.fromKey(requireArguments().getString(ARG_THEME_ID, ThemeId.CLASSIC.themeKey))

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // TODO: Implement dialog UI with "Watch Ad" and "Buy" buttons
        return super.onCreateDialog(savedInstanceState)
    }

    companion object {
        const val TAG = "ThemeUnlockDialog"
        private const val ARG_THEME_ID = "themeId"

        fun newInstance(themeId: ThemeId): ThemeUnlockDialog =
            ThemeUnlockDialog().apply {
                arguments = Bundle().apply {
                    putString(ARG_THEME_ID, themeId.themeKey)
                }
            }
    }
}
