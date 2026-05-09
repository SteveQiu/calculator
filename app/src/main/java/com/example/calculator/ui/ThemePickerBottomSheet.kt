package com.example.calculator.ui

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calculator.R
import com.example.calculator.di.AppModule
import com.example.calculator.model.ThemeId
import com.example.calculator.model.toColors
import com.example.calculator.viewmodel.ThemeViewModel
import com.example.calculator.viewmodel.ThemeViewModelFactory
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

/**
 * Full-screen theme picker shown as a bottom sheet.
 * Displays all themes in a 2-column grid; locked themes show inline Watch Ad / Buy CTAs.
 * The host Activity must implement [ThemeUnlockListener].
 */
class ThemePickerBottomSheet : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "ThemePickerBottomSheet"

        fun newInstance(): ThemePickerBottomSheet = ThemePickerBottomSheet()
    }

    private lateinit var listener: ThemeUnlockListener

    private val themeViewModel: ThemeViewModel by activityViewModels {
        ThemeViewModelFactory(
            AppModule.provideThemeRepository(requireContext()),
            AppModule.provideBillingRepository(requireContext()),
            AppModule.provideAdRepository(requireContext())
        )
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ThemeUnlockListener
            ?: throw IllegalStateException("${context.javaClass.simpleName} must implement ThemeUnlockListener")
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_theme_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Expand the sheet fully so all 6 theme cards are visible at once
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        view.findViewById<View>(R.id.btnClosePicker).setOnClickListener { dismiss() }

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvThemesPicker)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        viewLifecycleOwner.lifecycleScope.launch {
            // Combine active + unlocked state in a single collect loop using zip-like pattern
            themeViewModel.unlockedThemes.collect { unlocked ->
                val active = themeViewModel.activeTheme.value
                recyclerView.adapter = ThemePickerAdapter(
                    themes         = ThemeId.entries,
                    unlockedThemes = unlocked,
                    activeTheme    = active
                )
            }
        }

        // Also re-render when active theme changes (e.g. user applied a theme)
        viewLifecycleOwner.lifecycleScope.launch {
            themeViewModel.activeTheme.collect { active ->
                (recyclerView.adapter as? ThemePickerAdapter)?.updateActiveTheme(active)
            }
        }
    }

    // ────────────────────────────────────────────────────────────
    // Adapter (inner class — has access to listener + dismiss())
    // ────────────────────────────────────────────────────────────

    inner class ThemePickerAdapter(
        private val themes: List<ThemeId>,
        private var unlockedThemes: Set<ThemeId>,
        private var activeTheme: ThemeId
    ) : RecyclerView.Adapter<ThemePickerAdapter.ThemeViewHolder>() {

        inner class ThemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val card: MaterialCardView = itemView.findViewById(R.id.cardRoot)
            val themeName: TextView    = itemView.findViewById(R.id.tvThemeName)
            val badge: TextView        = itemView.findViewById(R.id.tvBadge)
            val lockOverlay: View      = itemView.findViewById(R.id.lockOverlay)
            val previewBg: View        = itemView.findViewById(R.id.previewBg)
            val dotSpecial: View       = itemView.findViewById(R.id.dotSpecial)
            val dotNumber: View        = itemView.findViewById(R.id.dotNumber)
            val dotOperator: View      = itemView.findViewById(R.id.dotOperator)
            val btnWatchAd: View       = itemView.findViewById(R.id.btnCardWatchAd)
            val btnBuy: View           = itemView.findViewById(R.id.btnCardBuy)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_theme_card, parent, false)
            return ThemeViewHolder(view)
        }

        override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
            val themeId    = themes[position]
            val isUnlocked = themeId in unlockedThemes || !themeId.isPremium
            val isActive   = themeId == activeTheme
            val colors     = themeId.toColors(holder.itemView.context)

            // Theme name and badge
            holder.themeName.text = themeId.displayName
            holder.badge.text = when {
                isActive   -> "✓ Active"
                themeId.isPremium -> "Premium"
                else       -> "Free"
            }

            // Color preview swatch
            holder.previewBg.setBackgroundColor(colors.background)
            ViewCompat.setBackgroundTintList(holder.dotSpecial,  ColorStateList.valueOf(colors.btnSpecial))
            ViewCompat.setBackgroundTintList(holder.dotNumber,   ColorStateList.valueOf(colors.btnNumber))
            ViewCompat.setBackgroundTintList(holder.dotOperator, ColorStateList.valueOf(colors.btnOperator))

            // Active state — colored stroke ring
            holder.card.strokeColor = if (isActive) colors.btnOperator else Color.TRANSPARENT
            holder.card.strokeWidth = if (isActive) 6 else 0

            // Lock overlay
            holder.lockOverlay.visibility = if (isUnlocked) View.GONE else View.VISIBLE

            if (isUnlocked) {
                // Unlocked: tap card to apply theme and close sheet
                holder.card.setOnClickListener {
                    listener.onThemeSelected(themeId)
                    dismiss()
                }
            } else {
                // Locked: card taps are absorbed by the overlay buttons
                holder.card.setOnClickListener(null)
                holder.btnWatchAd.setOnClickListener {
                    listener.onWatchAdRequested(themeId)
                    dismiss()
                }
                holder.btnBuy.setOnClickListener {
                    listener.onPurchaseRequested(themeId)
                    dismiss()
                }
            }
        }

        override fun getItemCount(): Int = themes.size

        fun updateActiveTheme(newActive: ThemeId) {
            activeTheme = newActive
            notifyDataSetChanged()
        }
    }
}
