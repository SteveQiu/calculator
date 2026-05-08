package com.example.calculator

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.calculator.databinding.ActivityMainBinding
import java.math.BigDecimal
import java.math.MathContext

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    // Calculator state
    private var displayValue = "0"
    private var expressionText = ""
    private var firstOperand: Double? = null
    private var pendingOperator = ""
    private var isNewInput = true
    private var lastWasEquals = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        savedInstanceState?.let { state ->
            displayValue = state.getString("displayValue", "0")!!
            expressionText = state.getString("expressionText", "")!!
            firstOperand = if (state.containsKey("firstOperand")) state.getDouble("firstOperand") else null
            pendingOperator = state.getString("pendingOperator", "")!!
            isNewInput = state.getBoolean("isNewInput", true)
            lastWasEquals = state.getBoolean("lastWasEquals", false)
        }

        updateDisplay()
        setupButtons()
        updateThemeButton()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("displayValue", displayValue)
        outState.putString("expressionText", expressionText)
        firstOperand?.let { outState.putDouble("firstOperand", it) }
        outState.putString("pendingOperator", pendingOperator)
        outState.putBoolean("isNewInput", isNewInput)
        outState.putBoolean("lastWasEquals", lastWasEquals)
    }

    private fun setupButtons() {
        listOf(
            binding.btn0 to "0", binding.btn1 to "1", binding.btn2 to "2",
            binding.btn3 to "3", binding.btn4 to "4", binding.btn5 to "5",
            binding.btn6 to "6", binding.btn7 to "7", binding.btn8 to "8",
            binding.btn9 to "9"
        ).forEach { (btn, digit) -> btn.setOnClickListener { onDigit(digit) } }

        binding.btnDot.setOnClickListener { onDot() }
        binding.btnClear.setOnClickListener { onClear() }
        binding.btnDelete.setOnClickListener { onDelete() }
        binding.btnToggleSign.setOnClickListener { onToggleSign() }
        binding.btnPercent.setOnClickListener { onPercent() }
        binding.btnAdd.setOnClickListener { onOperator("+") }
        binding.btnSubtract.setOnClickListener { onOperator("−") }
        binding.btnMultiply.setOnClickListener { onOperator("×") }
        binding.btnDivide.setOnClickListener { onOperator("÷") }
        binding.btnEquals.setOnClickListener { onEquals() }
        binding.btnTheme.setOnClickListener { toggleTheme() }
    }

    // ── Input handlers ──────────────────────────────────────────────────────

    private fun onDigit(digit: String) {
        if (isNewInput) {
            displayValue = digit
            isNewInput = false
        } else {
            if (displayValue == "0") displayValue = digit
            else if (displayValue.length < 15) displayValue += digit
        }
        lastWasEquals = false
        updateDisplay()
    }

    private fun onDot() {
        if (isNewInput) {
            displayValue = "0."
            isNewInput = false
        } else if (!displayValue.contains(".")) {
            displayValue += "."
        }
        lastWasEquals = false
        updateDisplay()
    }

    private fun onClear() {
        val isFullClear = displayValue == "0" && pendingOperator.isEmpty() && firstOperand == null
        if (isFullClear) {
            expressionText = ""
            firstOperand = null
            pendingOperator = ""
            lastWasEquals = false
        }
        displayValue = "0"
        isNewInput = pendingOperator.isNotEmpty()
        updateDisplay()
    }

    private fun onDelete() {
        if (lastWasEquals) {
            onClear()
            return
        }
        // Cancel pending operator if we haven't typed new input yet
        if (isNewInput && pendingOperator.isNotEmpty()) {
            pendingOperator = ""
            firstOperand?.let { displayValue = formatNumber(it) }
            firstOperand = null
            expressionText = ""
            isNewInput = false
            updateDisplay()
            return
        }
        if (!isNewInput) {
            displayValue = when {
                displayValue.length <= 1 -> "0"
                displayValue == "-0" -> "0"
                else -> displayValue.dropLast(1).let { if (it == "-") "0" else it }
            }
            updateDisplay()
        }
    }

    private fun onToggleSign() {
        val value = displayValue.toDoubleOrNull() ?: return
        if (value == 0.0) return
        displayValue = formatNumber(-value)
        isNewInput = false
        updateDisplay()
    }

    private fun onPercent() {
        val value = displayValue.toDoubleOrNull() ?: return
        val result = if (firstOperand != null && (pendingOperator == "+" || pendingOperator == "−")) {
            firstOperand!! * (value / 100.0)
        } else {
            value / 100.0
        }
        displayValue = formatNumber(result)
        isNewInput = false
        updateDisplay()
    }

    private fun onOperator(op: String) {
        val currentValue = displayValue.toDoubleOrNull() ?: return
        if (firstOperand != null && !isNewInput) {
            val result = compute(firstOperand!!, currentValue, pendingOperator)
            if (result == null) { showError(); return }
            firstOperand = result
            displayValue = formatNumber(result)
        } else {
            firstOperand = currentValue
        }
        pendingOperator = op
        expressionText = "${formatNumber(firstOperand!!)} $op"
        isNewInput = true
        lastWasEquals = false
        updateDisplay()
    }

    private fun onEquals() {
        if (pendingOperator.isEmpty() || firstOperand == null) return
        val secondOperand = displayValue.toDoubleOrNull() ?: return
        val result = compute(firstOperand!!, secondOperand, pendingOperator)
        if (result == null) { showError(); return }
        expressionText = "${formatNumber(firstOperand!!)} $pendingOperator ${formatNumber(secondOperand)} ="
        displayValue = formatNumber(result)
        firstOperand = null
        pendingOperator = ""
        isNewInput = true
        lastWasEquals = true
        updateDisplay()
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private fun compute(a: Double, b: Double, op: String): Double? = when (op) {
        "+" -> a + b
        "−" -> a - b
        "×" -> a * b
        "÷" -> if (b != 0.0) a / b else null
        else -> null
    }

    private fun formatNumber(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return "Error"
        if (value == 0.0) return "0"
        val formatted = "%.10g".format(value)
        return when {
            formatted.contains('e', ignoreCase = true) -> {
                BigDecimal(value).round(MathContext(10)).toEngineeringString()
            }
            formatted.contains('.') -> formatted.trimEnd('0').trimEnd('.')
            else -> formatted
        }
    }

    private fun showError() {
        displayValue = "Error"
        expressionText = ""
        firstOperand = null
        pendingOperator = ""
        isNewInput = true
        updateDisplay()
    }

    private fun updateDisplay() {
        binding.tvDisplay.text = displayValue
        binding.tvExpression.text = expressionText
        val isFullClear = displayValue == "0" && pendingOperator.isEmpty() && firstOperand == null
        binding.btnClear.text = if (isFullClear) "AC" else "C"
    }

    // ── Theme ────────────────────────────────────────────────────────────────

    private fun toggleTheme() {
        val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        val newMode = if (isNight) AppCompatDelegate.MODE_NIGHT_NO else AppCompatDelegate.MODE_NIGHT_YES
        getSharedPreferences("calc_prefs", MODE_PRIVATE).edit()
            .putBoolean("dark_mode", !isNight).apply()
        AppCompatDelegate.setDefaultNightMode(newMode)
    }

    private fun updateThemeButton() {
        val isNight = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        binding.btnTheme.text = if (isNight) "☀" else "🌙"
    }
}
