package com.example.calculator.ui

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.calculator.R
import com.example.calculator.di.AppModule
import com.example.calculator.model.ThemeId
import com.example.calculator.model.toColors
import com.example.calculator.viewmodel.ThemeViewModel
import com.example.calculator.viewmodel.ThemeViewModelFactory
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class ThemePickerActivity : AppCompatActivity() {

    private lateinit var themeViewModel: ThemeViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_theme_picker)

        themeViewModel = ViewModelProvider(
            this,
            ThemeViewModelFactory(
                AppModule.provideThemeRepository(this),
                AppModule.provideBillingRepository(this),
                AppModule.provideAdRepository(this)
            )
        ).get(ThemeViewModel::class.java)

        val recyclerView = findViewById<RecyclerView>(R.id.rvThemes)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        lifecycleScope.launch {
            themeViewModel.unlockedThemes.collect { unlocked ->
                val activeTheme = themeViewModel.activeTheme.value
                recyclerView.adapter = ThemeAdapter(
                    themes         = ThemeId.entries,
                    unlockedThemes = unlocked,
                    activeTheme    = activeTheme,
                    onThemeClick   = { themeId -> onThemeClicked(themeId) }
                )
            }
        }

        lifecycleScope.launch {
            themeViewModel.uiEvents.collect { event ->
                when (event) {
                    is ThemeViewModel.UiEvent.ThemeUnlocked -> {
                        Toast.makeText(this@ThemePickerActivity, "Theme unlocked!", Toast.LENGTH_SHORT).show()
                        setResult(RESULT_OK)
                    }
                    is ThemeViewModel.UiEvent.Error -> {
                        Toast.makeText(this@ThemePickerActivity, event.message, Toast.LENGTH_LONG).show()
                    }
                    ThemeViewModel.UiEvent.AdNotReady -> {
                        Toast.makeText(this@ThemePickerActivity, "Ad loading… try again in a moment.", Toast.LENGTH_SHORT).show()
                    }
                    ThemeViewModel.UiEvent.PurchaseCancelled -> { /* no-op */ }
                }
            }
        }

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
    }

    private fun onThemeClicked(themeId: ThemeId) {
        if (themeViewModel.isThemeUnlocked(themeId)) {
            themeViewModel.selectTheme(themeId)
            setResult(RESULT_OK)
            finish()
        } else {
            ThemeUnlockDialog.newInstance(themeId).show(supportFragmentManager, ThemeUnlockDialog.TAG)
        }
    }
}

class ThemeAdapter(
    private val themes: List<ThemeId>,
    private val unlockedThemes: Set<ThemeId>,
    private val activeTheme: ThemeId,
    private val onThemeClick: (ThemeId) -> Unit
) : RecyclerView.Adapter<ThemeAdapter.ThemeViewHolder>() {

    inner class ThemeViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val card: MaterialCardView  = itemView.findViewById(R.id.cardRoot)
        val themeName: TextView     = itemView.findViewById(R.id.tvThemeName)
        val badge: TextView         = itemView.findViewById(R.id.tvBadge)
        val lockOverlay: View       = itemView.findViewById(R.id.lockOverlay)
        val previewBg: View         = itemView.findViewById(R.id.previewBg)
        val dotSpecial: View        = itemView.findViewById(R.id.dotSpecial)
        val dotNumber: View         = itemView.findViewById(R.id.dotNumber)
        val dotOperator: View       = itemView.findViewById(R.id.dotOperator)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ThemeViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_theme_card, parent, false)
        return ThemeViewHolder(view)
    }

    override fun onBindViewHolder(holder: ThemeViewHolder, position: Int) {
        val themeId    = themes[position]
        val isUnlocked = themeId in unlockedThemes || !themeId.isPremium
        val isActive   = themeId == activeTheme
        val colors     = themeId.toColors(holder.itemView.context)

        holder.themeName.text = themeId.displayName
        holder.badge.text     = if (themeId.isPremium) "Premium" else "Free"
        holder.lockOverlay.visibility = if (isUnlocked) View.GONE else View.VISIBLE

        holder.previewBg.setBackgroundColor(colors.background)
        ViewCompat.setBackgroundTintList(holder.dotSpecial,  ColorStateList.valueOf(colors.btnSpecial))
        ViewCompat.setBackgroundTintList(holder.dotNumber,   ColorStateList.valueOf(colors.btnNumber))
        ViewCompat.setBackgroundTintList(holder.dotOperator, ColorStateList.valueOf(colors.btnOperator))

        holder.card.strokeColor = if (isActive) colors.btnOperator else Color.TRANSPARENT
        holder.card.strokeWidth = if (isActive) 6 else 0

        holder.card.setOnClickListener { onThemeClick(themeId) }
    }

    override fun getItemCount(): Int = themes.size
}