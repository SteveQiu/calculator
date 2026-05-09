package com.example.calculator.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import com.example.calculator.R
import com.example.calculator.di.AppModule
import com.example.calculator.model.ThemeId
import com.example.calculator.model.toColors
import com.example.calculator.viewmodel.ThemeViewModel
import com.example.calculator.viewmodel.ThemeViewModelFactory

class ThemeUnlockDialog : DialogFragment() {

    private lateinit var themeViewModel: ThemeViewModel

    companion object {
        const val TAG = "ThemeUnlockDialog"
        private const val ARG_THEME_ID = "theme_id"

        fun newInstance(themeId: ThemeId): ThemeUnlockDialog =
            ThemeUnlockDialog().apply {
                arguments = Bundle().apply { putString(ARG_THEME_ID, themeId.name) }
            }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_theme_unlock, container, false)

    override fun onStart() {
        super.onStart()
        // Expand to full-screen so the CoordinatorLayout backdrop fills the window
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundDrawableResource(android.R.color.transparent)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val themeId = ThemeId.valueOf(
            arguments?.getString(ARG_THEME_ID) ?: ThemeId.CLASSIC.name
        )

        themeViewModel = ViewModelProvider(
            requireActivity(),
            ThemeViewModelFactory(
                AppModule.provideThemeRepository(requireContext()),
                AppModule.provideBillingRepository(requireContext()),
                AppModule.provideAdRepository(requireContext())
            )
        ).get(ThemeViewModel::class.java)

        view.findViewById<TextView>(R.id.tvDialogThemeName).text = themeId.displayName

        // Show theme colour preview
        val colors = themeId.toColors(requireContext())
        view.findViewById<View>(R.id.previewBlockSpecial).setBackgroundColor(colors.btnSpecial)
        view.findViewById<View>(R.id.previewBlockNumber).setBackgroundColor(colors.btnNumber)
        view.findViewById<View>(R.id.previewBlockOperator).setBackgroundColor(colors.btnOperator)

        view.findViewById<Button>(R.id.btnWatchAd).setOnClickListener {
            themeViewModel.pendingUnlockTheme = themeId
            themeViewModel.watchAdToUnlock(requireActivity())
            dismiss()
        }

        view.findViewById<Button>(R.id.btnBuy).setOnClickListener {
            themeViewModel.buyTheme(requireActivity(), themeId)
            dismiss()
        }

        view.findViewById<TextView>(R.id.tvRestorePurchase).setOnClickListener {
            // Purchases are restored automatically via BillingRepository.restorePurchases() on connect
            dismiss()
        }

        view.findViewById<View>(R.id.btnClose).setOnClickListener { dismiss() }
    }
}