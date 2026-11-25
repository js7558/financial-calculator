package com.js10bii.calculator.ui

import com.js10bii.calculator.engine.KeyAction
import com.js10bii.calculator.support.Calc
import com.js10bii.calculator.viewmodel.CalculatorState
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class StatusLinesTest {

    private fun state(keys: String) = Calc().press(keys).vm.state.value
    private fun modes(s: CalculatorState) = with(StatusLines) { listOf(pyr(s), timing(s), calcMode(s), angle(s)).joinToString("  ") }
    private fun modes(keys: String) = modes(state(keys))
    private fun registers(keys: String) = StatusLines.registers(state(keys)).mapNotNull { (name, v) -> v?.let { "$name=$it" } }.joinToString("  ")
    private fun values(keys: String) = StatusLines.values(state(keys))

    @Test fun pendingLeavesOutTheNumberBeingTyped() {
        assertEquals("", StatusLines.pending(state("8")))
        assertEquals("8 ×", StatusLines.pending(state("8 × 5")))
        assertEquals("2 + 3 ×", StatusLines.pending(state("b:Alg/Chain 2 + 3 × 4")))
        assertEquals("", StatusLines.pending(state("8 × 5 =")))
    }

    @Test fun startsWithSettingsOnly() {
        assertEquals("P/YR=12  END  CHAIN  DEG", modes(Calc().vm.state.value))
        assertEquals("", StatusLines.values(Calc().vm.state.value))
    }

    @Test fun amountsShowFullCents() {
        val loan = "26 o:P/YR 520 N 4.25 I/YR -8841.99 PMT 0 FV PV"
        assertEquals("PV=3,095,632.15  PMT=-8,841.99  FV=0.00", values(loan))
        assertEquals("P/YR=26  END  CHAIN  DEG", modes(loan))
        assertEquals("N=520  I/YR=4.25  PV=3,095,632.15  PMT=-8,841.99  FV=0.00", registers(loan))
    }

    @Test fun largeNegativeAmountsAreNotScientific() { assertEquals("PV=-25,000,000.25", values("-25000000.25 PV")) }

    @Test fun settingsStayVisibleWithRegistersSet() {
        val keys = "2 o:P/YR o:Beg/End b:Alg/Chain b:Rad/Deg 360 N 6 I/YR 200000 PV"
        assertEquals("P/YR=2  BEGIN  ALG  RAD", modes(keys))
        assertEquals("N=360  I/YR=6  PV=200,000.00", registers(keys))
        assertEquals("P/YR=2.5  END  CHAIN  DEG", modes("2.5 o:P/YR"))
    }

    @Test fun settingsKeptByClearAllAreShown() {
        val c = Calc().press("2 o:P/YR o:Beg/End 360 N")
        c.vm.onKeyPress(KeyAction.CLEAR_ALL)
        assertEquals("P/YR=2  BEGIN  CHAIN  DEG", modes(c.vm.state.value))
    }

    @Test fun solvedRateKeepsItsDecimals() {
        assertEquals("N=40  I/YR=11.12  PV=483,471.70  PMT=-30,367.93  FV=0.00", registers("2 o:P/YR 40 N 483471.70 PV -30367.93 PMT 0 FV I/YR"))
        assertEquals("I/YR=8.333333", registers("25 ÷ 3 = I/YR"))
    }

    @Test fun amortization() {
        assertEquals("AMORT 1–12  PRIN=-2,456.02  INT=-11,933.19  BAL=197,543.98", values("360 N 6 I/YR 200000 PV 0 FV PMT 1 INPUT 12 o:AMORT"))
    }

    @Test fun marginAndConstants() {
        assertEquals("CST=457,609.79  PRC=489,369.90  MAR=6.49", values("457609.79 CST 6.49 MAR PRC"))
        assertEquals("K: + 10%", values("10 + 10 % K"))
    }

    @Test fun expressions() {
        assertEquals("30 ÷ ( 85 − 12", values("30 ÷ o:( 85 - 12"))
        assertEquals("1,234,567.89 ×", values("1234567.89 ×"))
        assertEquals("12.182011 DATE 100", values("12.182011 o:DATE 100"))
        assertEquals("2 + 3 × 4", values("b:Alg/Chain 2 + 3 × 4"))
    }

    @Test fun sameInEveryLocale() {
        val before = Locale.getDefault()
        try {
            for (tag in listOf("de-DE", "fr-FR", "ar-EG", "hi-IN-u-nu-deva")) {
                Locale.setDefault(Locale.forLanguageTag(tag))
                assertEquals("PV=1,234.50", values("1234.5 PV"))
            }
        } finally { Locale.setDefault(before) }
    }
}
