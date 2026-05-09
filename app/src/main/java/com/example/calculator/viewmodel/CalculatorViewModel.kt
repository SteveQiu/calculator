package com.example.calculator.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns all calculator state: the current input expression and the evaluated result.
 *
 * Logic extracted from MainActivity so the UI layer holds zero business logic.
 *
 * TODO: Extract CalculatorEngine (pure functions, no Android deps) for expression parsing
 * TODO: Implement onButtonPressed(symbol: String) to build inputExpression
 * TODO: Implement evaluate() using CalculatorEngine to produce result
 * TODO: Implement onClear(), onBackspace(), onToggleSign(), onPercent()
 * TODO: Handle error states (division by zero, malformed expression)
 */
class CalculatorViewModel : ViewModel() {

    /** The expression currently being built by the user, e.g. "123 + 45". */
    private val _inputExpression = MutableStateFlow("")
    val inputExpression: StateFlow<String> = _inputExpression

    /** The computed result shown in the secondary display. Empty until "=" is pressed. */
    private val _result = MutableStateFlow("")
    val result: StateFlow<String> = _result

    // TODO: fun onButtonPressed(symbol: String)
    // TODO: fun evaluate()
    // TODO: fun onClear()
    // TODO: fun onBackspace()
}
