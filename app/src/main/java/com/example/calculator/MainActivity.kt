package com.example.calculator

import android.content.res.ColorStateList
import android.graphics.Typeface
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.example.calculator.databinding.ActivityMainBinding
import com.example.calculator.di.AppModule
import com.example.calculator.model.ThemeId
import com.example.calculator.model.ThemeRegistry
import com.example.calculator.ui.ThemePickerDialog
import com.example.calculator.ui.ThemeUnlockListener
import com.example.calculator.viewmodel.CalculatorViewModel
import com.example.calculator.viewmodel.ThemeViewModel
import com.example.calculator.viewmodel.ThemeViewModelFactory
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity(), ThemeUnlockListener {

    private lateinit var binding: ActivityMainBinding

    private val calcViewModel: CalculatorViewModel by viewModels()

    private val themeViewModel: ThemeViewModel by viewModels {
        ThemeViewModelFactory(
            AppModule.provideThemeRepository(this),
            AppModule.provideBillingRepository(this),
            AppModule.provideAdRepository(this)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState?.let {
            calcViewModel.restoreState(
                display   = it.getString("displayValue", "0")!!,
                expr      = it.getString("expressionText", "")!!,
                first     = if (it.containsKey("firstOperand")) it.getDouble("firstOperand") else null,
                op        = it.getString("pendingOperator", "")!!,
                newInput  = it.getBoolean("isNewInput", true),
                wasEquals = it.getBoolean("lastWasEquals", false)
            )
        }

        setupButtons()
        updateDisplay()
        // Apply the current theme synchronously before the async flow fires.
        // This prevents any black-screen flash caused by the dark-mode colorBackground
        // override in values-night/colors.xml showing through before the coroutine runs.
        applyThemeColors(themeViewModel.activeTheme.value)
        observeTheme()
        observeUiEvents()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("displayValue", calcViewModel.displayValue)
        outState.putString("expressionText", calcViewModel.expressionText)
        // Operator state lives in ViewModel; survives config changes automatically.
        // On process death the display resets to "0" — acceptable for a calculator.
    }

    private fun observeTheme() {
        lifecycleScope.launch {
            themeViewModel.activeTheme.collect { themeId ->
                applyThemeColors(themeId)
            }
        }
    }

    /** Observes ThemeViewModel events and surfaces them as Snackbars so the user gets feedback
     *  whether the ad or billing flow succeeded or failed. */
    private fun observeUiEvents() {
        lifecycleScope.launch {
            themeViewModel.uiEvents.collect { event ->
                when (event) {
                    is ThemeViewModel.UiEvent.ThemeUnlocked -> {
                        Snackbar.make(
                            binding.root,
                            "${event.themeId.displayName} unlocked!",
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    is ThemeViewModel.UiEvent.Error -> {
                        Snackbar.make(binding.root, event.message, Snackbar.LENGTH_LONG).show()
                    }
                    ThemeViewModel.UiEvent.AdNotReady -> {
                        Snackbar.make(
                            binding.root,
                            getString(R.string.ad_not_available),
                            Snackbar.LENGTH_SHORT
                        ).show()
                    }
                    ThemeViewModel.UiEvent.PurchaseCancelled -> { /* no-op */ }
                }
            }
        }
    }

    private fun applyThemeColors(themeId: ThemeId) {
        val theme  = ThemeRegistry.forId(themeId)
        val colors = theme.colors(this)

        // Apply background: image takes precedence over solid color
        if (theme.backgroundImageRes != null) {
            binding.root.background = ContextCompat.getDrawable(this, theme.backgroundImageRes)
        } else {
            binding.root.setBackgroundColor(colors.background)
        }
        binding.tvDisplay.setTextColor(colors.textPrimary)
        binding.tvExpression.setTextColor(colors.textSecondary)

        binding.btnTheme.text = theme.iconEmoji ?: "🎨"

        val numberTint   = ColorStateList.valueOf(colors.btnNumber)
        val specialTint  = ColorStateList.valueOf(colors.btnSpecial)
        val operatorTint = ColorStateList.valueOf(colors.btnOperator)

        listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9,
            binding.btnDot
        ).forEach { btn ->
            btn.backgroundTintList = numberTint
            btn.setTextColor(colors.textOnNumber)
        }

        listOf(binding.btnClear, binding.btnToggleSign, binding.btnPercent, binding.btnDeleteRow).forEach { btn ->
            btn.backgroundTintList = specialTint
            btn.setTextColor(colors.textOnSpecial)
        }

        listOf(
            binding.btnDivide, binding.btnMultiply, binding.btnSubtract,
            binding.btnAdd, binding.btnEquals
        ).forEach { btn ->
            btn.backgroundTintList = operatorTint
            btn.setTextColor(colors.textOnOperator)
        }

        // Apply theme-specific font to the display and number buttons.
        val typeface = theme.fontResId?.let { ResourcesCompat.getFont(this, it) }
            ?: Typeface.DEFAULT
        binding.tvDisplay.typeface = typeface
        binding.tvExpression.typeface = typeface
        listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9,
            binding.btnDot
        ).forEach { btn -> btn.typeface = typeface }
    }

    private fun setupButtons() {
        listOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9"
        ).forEach { (btn, digit) ->
            btn.setOnClickListener { calcViewModel.onDigit(digit); updateDisplay() }
        }

        binding.btnDot.setOnClickListener      { calcViewModel.onDot();        updateDisplay() }
        binding.btnClear.setOnClickListener    { calcViewModel.onClear();       updateDisplay() }
        binding.btnDeleteRow.setOnClickListener { calcViewModel.onDelete();      updateDisplay() }
        binding.btnToggleSign.setOnClickListener { calcViewModel.onToggleSign(); updateDisplay() }
        binding.btnPercent.setOnClickListener  { calcViewModel.onPercent();     updateDisplay() }
        binding.btnAdd.setOnClickListener      { calcViewModel.onOperator("+"); updateDisplay() }
        binding.btnSubtract.setOnClickListener { calcViewModel.onOperator("−"); updateDisplay() }
        binding.btnMultiply.setOnClickListener { calcViewModel.onOperator("×"); updateDisplay() }
        binding.btnDivide.setOnClickListener   { calcViewModel.onOperator("÷"); updateDisplay() }
        binding.btnEquals.setOnClickListener   { calcViewModel.onEquals();      updateDisplay() }

        binding.btnTheme.setOnClickListener {
            ThemePickerDialog.newInstance()
                .show(supportFragmentManager, ThemePickerDialog.TAG)
        }
    }

    // ── ThemeUnlockListener ──────────────────────────────────────────────
    override fun onThemeSelected(themeId: ThemeId) {
        themeViewModel.selectTheme(themeId)
    }

    override fun onWatchAdRequested(themeId: ThemeId) {
        themeViewModel.pendingUnlockTheme = themeId
        themeViewModel.watchAdToUnlock(this)
    }

    override fun onPurchaseRequested(themeId: ThemeId) {
        themeViewModel.buyTheme(this, themeId)
    }

    override fun isThemeUnlocked(themeId: ThemeId): Boolean =
        themeViewModel.isThemeUnlocked(themeId)

    private fun updateDisplay() {
        val state = calcViewModel.displayState
        binding.tvDisplay.text    = state.display
        binding.tvExpression.text = state.expression
        binding.btnClear.text     = if (state.showAC) "AC" else "C"
    }
}