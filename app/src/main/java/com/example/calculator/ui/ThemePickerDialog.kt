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
 * Bottom-sheet dialog that replaces the cycling btnTheme flow.
 *
 * Shows all themes in a 2-column grid. Unlocked theme cards call [ThemeUnlockListener.onThemeSelected];
 * locked cards expose inline "Watch Ad" / "Buy" CTAs that call [ThemeUnlockListener.onWatchAdRequested]
 * and [ThemeUnlockListener.onPurchaseRequested] respectively — so MainActivity owns all business logic.
 *
 * Host Activity MUST implement [ThemeUnlockListener]; an [IllegalStateException] is thrown otherwise.
 */
class ThemePickerDialog : BottomSheetDialogFragment() {

    companion object {
        const val TAG = "ThemePickerDialog"
        fun newInstance(): ThemePickerDialog = ThemePickerDialog()
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
            ?: throw IllegalStateException(
                "${context.javaClass.simpleName} must implement ThemeUnlockListener"
            )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = inflater.inflate(R.layout.dialog_theme_picker, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Expand fully so all 6 theme cards are visible without needing to drag
        (dialog as? BottomSheetDialog)?.behavior?.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
        }

        view.findViewById<View>(R.id.btnClosePicker).setOnClickListener { dismiss() }

        val recyclerView = view.findViewById<RecyclerView>(R.id.rvThemesPicker)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        viewLifecycleOwner.lifecycleScope.launch {
            themeViewModel.unlockedThemes.collect { unlocked ->
                recyclerView.adapter = ThemeCardAdapter(
                    themes         = ThemeId.entries,
                    unlockedThemes = unlocked,
                    activeTheme    = themeViewModel.activeTheme.value
                )
            }
        }

        // Re-render when the active theme changes (e.g. user just applied one)
        viewLifecycleOwner.lifecycleScope.launch {
            themeViewModel.activeTheme.collect { active ->
                (recyclerView.adapter as? ThemeCardAdapter)?.updateActiveTheme(active)
            }
        }
    }

    // ── Adapter ──────────────────────────────────────────────────────────
    // Inner class so it can reference `listener` and `dismiss()` directly.

    inner class ThemeCardAdapter(
        private val themes: List<ThemeId>,
        private var unlockedThemes: Set<ThemeId>,
        private var activeTheme: ThemeId
    ) : RecyclerView.Adapter<ThemeCardAdapter.VH>() {

        inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val card: MaterialCardView = itemView.findViewById(R.id.cardRoot)
            val name: TextView         = itemView.findViewById(R.id.tvThemeName)
            val nameOverlay: TextView  = itemView.findViewById(R.id.tvThemeNameOverlay)
            val badge: TextView        = itemView.findViewById(R.id.tvBadge)
            val lockOverlay: View      = itemView.findViewById(R.id.lockOverlay)
            val previewBg: View        = itemView.findViewById(R.id.previewBg)
            val dotSpecial: View       = itemView.findViewById(R.id.dotSpecial)
            val dotNumber: View        = itemView.findViewById(R.id.dotNumber)
            val dotOperator: View      = itemView.findViewById(R.id.dotOperator)
            val btnWatchAd: View       = itemView.findViewById(R.id.btnCardWatchAd)
            val btnBuy: View           = itemView.findViewById(R.id.btnCardBuy)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_theme_card, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val themeId    = themes[position]
            val isUnlocked = themeId in unlockedThemes || !themeId.isPremium
            val isActive   = themeId == activeTheme
            val colors     = themeId.toColors(holder.itemView.context)

            holder.name.text = themeId.displayName
            holder.nameOverlay.text = themeId.displayName
            holder.badge.text = when {
                isActive               -> "✓ Active"
                !themeId.isPremium     -> "Free"
                isUnlocked             -> "✓ Owned"   // premium + purchased/unlocked
                else                   -> "Premium"   // premium + locked
            }

            holder.previewBg.setBackgroundColor(colors.background)
            ViewCompat.setBackgroundTintList(holder.dotSpecial,  ColorStateList.valueOf(colors.btnSpecial))
            ViewCompat.setBackgroundTintList(holder.dotNumber,   ColorStateList.valueOf(colors.btnNumber))
            ViewCompat.setBackgroundTintList(holder.dotOperator, ColorStateList.valueOf(colors.btnOperator))

            holder.card.strokeColor = if (isActive) colors.btnOperator else Color.TRANSPARENT
            holder.card.strokeWidth = if (isActive) 6 else 0

            if (isUnlocked) {
                holder.lockOverlay.visibility = View.GONE
                holder.card.setOnClickListener {
                    listener.onThemeSelected(themeId)
                    dismiss()
                }
            } else {
                holder.lockOverlay.visibility = View.VISIBLE
                // Card tap is absorbed by the overlay; CTAs handle unlock
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

