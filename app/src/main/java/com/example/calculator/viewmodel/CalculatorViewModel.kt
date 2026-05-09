package com.example.calculator.viewmodel

import androidx.lifecycle.ViewModel
import java.math.BigDecimal
import java.math.MathContext

class CalculatorViewModel : ViewModel() {

    var displayValue = "0"
        private set
    var expressionText = ""
        private set
    private var firstOperand: Double? = null
    private var pendingOperator = ""
    private var isNewInput = true
    private var lastWasEquals = false

    data class DisplayState(val display: String, val expression: String, val showAC: Boolean)

    val displayState: DisplayState
        get() = DisplayState(
            display    = displayValue,
            expression = expressionText,
            showAC     = displayValue == "0" && pendingOperator.isEmpty() && firstOperand == null
        )

    fun onDigit(digit: String) {
        if (isNewInput) {
            displayValue = digit
            isNewInput = false
        } else {
            if (displayValue == "0") displayValue = digit
            else if (displayValue.length < 15) displayValue += digit
        }
        lastWasEquals = false
    }

    fun onDot() {
        if (isNewInput) {
            displayValue = "0."
            isNewInput = false
        } else if (!displayValue.contains(".")) {
            displayValue += "."
        }
        lastWasEquals = false
    }

    fun onClear() {
        val isFullClear = displayValue == "0" && pendingOperator.isEmpty() && firstOperand == null
        if (isFullClear) {
            expressionText = ""
            firstOperand = null
            pendingOperator = ""
            lastWasEquals = false
        }
        displayValue = "0"
        isNewInput = pendingOperator.isNotEmpty()
    }

    fun onDelete() {
        if (lastWasEquals) { onClear(); return }
        if (isNewInput && pendingOperator.isNotEmpty()) {
            pendingOperator = ""
            firstOperand?.let { displayValue = formatNumber(it) }
            firstOperand = null
            expressionText = ""
            isNewInput = false
            return
        }
        if (!isNewInput) {
            displayValue = when {
                displayValue.length <= 1  -> "0"
                displayValue == "-0"      -> "0"
                else -> displayValue.dropLast(1).let { if (it == "-") "0" else it }
            }
        }
    }

    fun onToggleSign() {
        val value = displayValue.toDoubleOrNull() ?: return
        if (value == 0.0) return
        displayValue = formatNumber(-value)
        isNewInput = false
    }

    fun onPercent() {
        val value = displayValue.toDoubleOrNull() ?: return
        val result = if (firstOperand != null && (pendingOperator == "+" || pendingOperator == "−"))
            firstOperand!! * (value / 100.0)
        else
            value / 100.0
        displayValue = formatNumber(result)
        isNewInput = false
    }

    fun onOperator(op: String) {
        val current = displayValue.toDoubleOrNull() ?: return
        if (firstOperand != null && !isNewInput) {
            val result = compute(firstOperand!!, current, pendingOperator) ?: run { showError(); return }
            firstOperand = result
            displayValue = formatNumber(result)
        } else {
            firstOperand = current
        }
        pendingOperator = op
        expressionText = "${formatNumber(firstOperand!!)} $op"
        isNewInput = true
        lastWasEquals = false
    }

    fun onEquals() {
        if (pendingOperator.isEmpty() || firstOperand == null) return
        val second = displayValue.toDoubleOrNull() ?: return
        val result = compute(firstOperand!!, second, pendingOperator) ?: run { showError(); return }
        expressionText = "${formatNumber(firstOperand!!)} $pendingOperator ${formatNumber(second)} ="
        displayValue = formatNumber(result)
        firstOperand = null
        pendingOperator = ""
        isNewInput = true
        lastWasEquals = true
    }

    private fun compute(a: Double, b: Double, op: String): Double? = when (op) {
        "+"  -> a + b
        "−"  -> a - b
        "×"  -> a * b
        "÷"  -> if (b != 0.0) a / b else null
        else -> null
    }

    private fun showError() {
        displayValue = "Error"
        expressionText = ""
        firstOperand = null
        pendingOperator = ""
        isNewInput = true
    }

    fun formatNumber(value: Double): String {
        if (value.isInfinite() || value.isNaN()) return "Error"
        if (value == 0.0) return "0"
        val formatted = "%.10g".format(value)
        return when {
            formatted.contains('e', ignoreCase = true) ->
                BigDecimal(value).round(MathContext(10)).toEngineeringString()
            formatted.contains('.') -> formatted.trimEnd('0').trimEnd('.')
            else -> formatted
        }
    }

    fun restoreState(
        display: String,
        expr: String,
        first: Double?,
        op: String,
        newInput: Boolean,
        wasEquals: Boolean
    ) {
        displayValue   = display
        expressionText = expr
        firstOperand   = first
        pendingOperator = op
        isNewInput     = newInput
        lastWasEquals  = wasEquals
    }
}