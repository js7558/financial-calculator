package com.js10bii.calculator.keystroke

import com.js10bii.calculator.engine.CalculatorEngine
import com.js10bii.calculator.engine.KeyAction
import com.js10bii.calculator.viewmodel.AmortPhase
import com.js10bii.calculator.viewmodel.CalculatorViewModel
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Random key presses: nothing may crash, and the display must always agree with the number behind it. */
class KeyFuzzTest {

    // These loop once per period, so a huge random operand would stall the test.
    private val slow = setOf(KeyAction.NJ, KeyAction.CFJ, KeyAction.TVM_AMORT, KeyAction.DEPR_SOYD)
    private val digits = KeyAction.entries.filter { it.name.startsWith("DIGIT_") }
    private val others = KeyAction.entries.filter { it !in slow && it !in digits }

    @Test fun displayAlwaysAgreesWithOperand() {
        for (seed in 1..400) {
            val rnd = Random(seed)
            val vm = CalculatorViewModel()
            val history = ArrayList<KeyAction>()
            repeat(250) {
                val key = if (rnd.nextInt(100) < 45) digits.random(rnd) else others.random(rnd)
                history += key
                val trail = { "seed $seed: ${history.takeLast(12)}" }
                try { vm.onKeyPress(key) } catch (e: Exception) { throw AssertionError(trail(), e) }
                val s = vm.state.value
                assertTrue("non-finite x, ${trail()}", s.x.isFinite())
                if (!s.isError && !s.isOff && s.amortPhase != AmortPhase.RANGE) {
                    val shown = s.displayText.replace(",", "")
                    assertTrue(
                        "shows '${s.displayText}' but x=${s.x}, ${trail()}",
                        s.displayText == CalculatorEngine.formatDisplay(s.x, s.displayDecimalPlaces) || shown.toDoubleOrNull() == s.x,
                    )
                }
            }
        }
    }
}
