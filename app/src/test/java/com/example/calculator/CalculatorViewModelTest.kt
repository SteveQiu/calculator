package com.example.calculator

import com.example.calculator.viewmodel.CalculatorViewModel
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class CalculatorViewModelTest {

    private lateinit var vm: CalculatorViewModel

    @Before fun setUp() { vm = CalculatorViewModel() }

    // ── Basic arithmetic ─────────────────────────────────────────────────────

    @Test fun `addition two integers`() {
        vm.onDigit("3"); vm.onOperator("+"); vm.onDigit("4"); vm.onEquals()
        assertEquals("7", vm.displayValue)
    }

    @Test fun `subtraction result negative`() {
        vm.onDigit("3"); vm.onOperator("−"); vm.onDigit("7"); vm.onEquals()
        assertEquals("-4", vm.displayValue)
    }

    @Test fun `multiplication`() {
        vm.onDigit("6"); vm.onOperator("×"); vm.onDigit("7"); vm.onEquals()
        assertEquals("42", vm.displayValue)
    }

    @Test fun `division exact`() {
        vm.onDigit("1"); vm.onDigit("0"); vm.onOperator("÷"); vm.onDigit("4"); vm.onEquals()
        assertEquals("2.5", vm.displayValue)
    }

    @Test fun `division by zero returns Error`() {
        vm.onDigit("5"); vm.onOperator("÷"); vm.onDigit("0"); vm.onEquals()
        assertEquals("Error", vm.displayValue)
    }

    @Test fun `chained operations`() {
        vm.onDigit("2"); vm.onOperator("+"); vm.onDigit("3"); vm.onOperator("+")
        assertEquals("5", vm.displayValue)
        vm.onDigit("4"); vm.onEquals()
        assertEquals("9", vm.displayValue)
    }

    // ── Edge cases ───────────────────────────────────────────────────────────

    @Test fun `initial state is zero`() {
        assertEquals("0", vm.displayValue)
        assertEquals("", vm.expressionText)
    }

    @Test fun `leading zero replaced by digit`() {
        vm.onDigit("0"); vm.onDigit("5")
        assertEquals("5", vm.displayValue)
    }

    @Test fun `max input length 15 chars`() {
        repeat(20) { vm.onDigit("1") }
        assertEquals(15, vm.displayValue.length)
    }

    @Test fun `decimal point adds leading zero`() {
        vm.onDot()
        assertEquals("0.", vm.displayValue)
    }

    @Test fun `double decimal ignored`() {
        vm.onDigit("3"); vm.onDot(); vm.onDot()
        assertEquals("3.", vm.displayValue)
    }

    @Test fun `percent of standalone value`() {
        vm.onDigit("5"); vm.onDigit("0"); vm.onPercent()
        assertEquals("0.5", vm.displayValue)
    }

    @Test fun `percent of addend uses base`() {
        vm.onDigit("2"); vm.onDigit("0"); vm.onDigit("0")
        vm.onOperator("+")
        vm.onDigit("1"); vm.onDigit("0"); vm.onPercent()
        // 10% of 200 = 20
        assertEquals("20", vm.displayValue)
    }

    @Test fun `toggle sign positive to negative`() {
        vm.onDigit("5"); vm.onToggleSign()
        assertEquals("-5", vm.displayValue)
    }

    @Test fun `toggle sign zero stays zero`() {
        vm.onToggleSign()
        assertEquals("0", vm.displayValue)
    }

    @Test fun `AC clears everything`() {
        vm.onDigit("5"); vm.onOperator("+"); vm.onDigit("3"); vm.onClear(); vm.onClear()
        assertEquals("0", vm.displayValue)
        assertEquals("", vm.expressionText)
    }

    @Test fun `C clears current input only`() {
        vm.onDigit("5"); vm.onOperator("+"); vm.onDigit("3"); vm.onClear()
        assertEquals("0", vm.displayValue)
        // expression still shows the pending operator context
    }

    @Test fun `delete removes last character`() {
        vm.onDigit("1"); vm.onDigit("2"); vm.onDelete()
        assertEquals("1", vm.displayValue)
    }

    @Test fun `delete on single digit resets to zero`() {
        vm.onDigit("5"); vm.onDelete()
        assertEquals("0", vm.displayValue)
    }

    @Test fun `delete after equals clears`() {
        vm.onDigit("4"); vm.onOperator("+"); vm.onDigit("4"); vm.onEquals()
        assertEquals("8", vm.displayValue)
        vm.onDelete()
        assertEquals("0", vm.displayValue)
    }

    @Test fun `expression shows operand and operator`() {
        vm.onDigit("4"); vm.onOperator("+")
        assertEquals("4 +", vm.expressionText)
    }

    @Test fun `displayState showAC true when clean`() {
        assertTrue(vm.displayState.showAC)
    }

    @Test fun `displayState showAC false after digit`() {
        vm.onDigit("5")
        assertFalse(vm.displayState.showAC)
    }
}
