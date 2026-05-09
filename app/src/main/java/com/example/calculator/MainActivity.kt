package com.example.calculator

import android.content.Intent
import android.content.res.ColorStateList
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.calculator.databinding.ActivityMainBinding
import com.example.calculator.di.AppModule
import com.example.calculator.model.ThemeId
import com.example.calculator.model.toColors
import com.example.calculator.ui.ThemePickerActivity
import com.example.calculator.ui.ThemeUnlockDialog
import com.example.calculator.viewmodel.CalculatorViewModel
import com.example.calculator.viewmodel.ThemeViewModel
import com.example.calculator.viewmodel.ThemeViewModelFactory
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val calcViewModel: CalculatorViewModel by viewModels()

    private val themeViewModel: ThemeViewModel by viewModels {
        ThemeViewModelFactory(
            AppModule.provideThemeRepository(this),
            AppModule.provideBillingRepository(this),
            AppModule.provideAdRepository(this)
        )
    }

    private val themePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            recreate()
        }
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
        observeTheme()
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

    private fun applyThemeColors(themeId: ThemeId) {
        val colors = themeId.toColors(this)

        binding.root.setBackgroundColor(colors.background)
        binding.tvDisplay.setTextColor(colors.textPrimary)
        binding.tvExpression.setTextColor(colors.textSecondary)

        // Update Theme Button Icon based on theme
        val themeIcon = when(themeId) {
            ThemeId.RABBIT -> "🐰"
            ThemeId.PANDA -> "🐼"
            ThemeId.MIDNIGHT -> "🌙"
            ThemeId.OCEAN -> "🌊"
            ThemeId.SUNSET -> "🌇"
            else -> "🌙"
        }
        binding.btnTheme.text = themeIcon

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

        listOf(binding.btnClear, binding.btnToggleSign, binding.btnPercent).forEach { btn ->
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
            // Simplified theme flow: Toggle to next theme or open unlock dialog for premium
            val nextTheme = when(themeViewModel.activeTheme.value) {
                ThemeId.CLASSIC -> ThemeId.MIDNIGHT
                ThemeId.MIDNIGHT -> ThemeId.OCEAN
                ThemeId.OCEAN -> ThemeId.SUNSET
                ThemeId.SUNSET -> ThemeId.RABBIT
                ThemeId.RABBIT -> ThemeId.PANDA
                ThemeId.PANDA -> ThemeId.CLASSIC
            }
            
            if (themeViewModel.isThemeUnlocked(nextTheme)) {
                themeViewModel.selectTheme(nextTheme)
            } else {
                ThemeUnlockDialog.newInstance(nextTheme).show(supportFragmentManager, "unlock")
            }
        }
    }

    private fun updateDisplay() {
        val state = calcViewModel.displayState
        binding.tvDisplay.text    = state.display
        binding.tvExpression.text = state.expression
        binding.btnClear.text     = if (state.showAC) "AC" else "C"
    }
}