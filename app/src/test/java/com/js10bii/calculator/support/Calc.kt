package com.js10bii.calculator.support

import com.js10bii.calculator.engine.KEY_ROWS
import com.js10bii.calculator.engine.KeyAction
import com.js10bii.calculator.engine.KeyDefinition
import com.js10bii.calculator.viewmodel.CalculatorViewModel
import com.js10bii.calculator.viewmodel.ShiftState
import org.junit.Assert.assertEquals

/**
 * Drives the real view model by key label, through the real KEY_ROWS layout.
 *
 * Tokens are space separated: a number ("10.5", "-375" = digits then +/-) is typed digit by digit,
 * "o:AMORT" / "b:YTM" press the orange / blue shift first, anything else is a primary key label.
 */
class Calc {
    val vm = CalculatorViewModel()
    val display: String get() = vm.state.value.displayText

    private val keys = KEY_ROWS.flatten()
    private val number = Regex("-?\\d*\\.?\\d+")

    fun press(sequence: String): Calc {
        sequence.trim().split(Regex("\\s+")).forEach(::token)
        return this
    }

    fun expect(expected: String): Calc {
        assertEquals(expected, display)
        return this
    }

    fun expect(expected: Double, tol: Double): Calc {
        assertEquals("display was \"$display\"", expected, value(), tol)
        return this
    }

    fun value(): Double = display.replace(",", "").toDoubleOrNull() ?: Double.NaN

    private fun token(t: String) {
        when {
            t.startsWith("o:") -> shifted(t.drop(2), KeyAction.ORANGE_SHIFT, { it.orangeLabel }, { it.orangeAction })
            t.startsWith("b:") -> shifted(t.drop(2), KeyAction.BLUE_SHIFT, { it.blueLabel }, { it.blueAction })
            number.matches(t) -> {
                t.removePrefix("-").forEach { vm.onKeyPress(actionOn(find(it.toString()) { k -> k.primaryLabel })) }
                if (t.startsWith("-")) vm.onKeyPress(KeyAction.SIGN_CHANGE)
            }
            else -> vm.onKeyPress(actionOn(find(t) { it.primaryLabel }))
        }
    }

    /** Same rule as CalcKey: the action depends on the shift state when the key is pressed. */
    private fun actionOn(key: KeyDefinition) = when (vm.state.value.shiftState) {
        ShiftState.ORANGE -> key.orangeAction
        ShiftState.BLUE -> key.blueAction
        ShiftState.NONE -> key.primaryAction
    }

    private fun shifted(label: String, shift: KeyAction, name: (KeyDefinition) -> String, act: (KeyDefinition) -> KeyAction) {
        vm.onKeyPress(shift)
        val key = find(label, name)
        check(actionOn(key) == act(key)) { "'$label' is not reachable: shift is ${vm.state.value.shiftState}" }
        vm.onKeyPress(act(key))
    }

    private fun find(label: String, name: (KeyDefinition) -> String) =
        keys.firstOrNull { name(it) == label } ?: error("No key labelled '$label'")
}
