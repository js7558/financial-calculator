package com.js10bii.calculator.engine

import com.js10bii.calculator.viewmodel.AngleMode
import org.junit.Assert.assertEquals
import org.junit.Test

class CalculatorEngineTest {

    private fun assertApprox(expected: Double, actual: Double, tol: Double = 0.005) {
        assertEquals("expected $expected but got $actual", expected, actual, tol)
    }

    // ── Basic arithmetic ─────────────────────────────────────────────────────

    @Test fun add() = assertApprox(87.18, CalculatorEngine.applyOperator(24.71, KeyAction.ADD, 62.47))
    @Test fun subtract() = assertApprox(38.0, CalculatorEngine.applyOperator(100.0, KeyAction.SUBTRACT, 62.0))
    @Test fun multiply() = assertApprox(119.0, CalculatorEngine.applyOperator(17.0, KeyAction.MULTIPLY, 7.0))
    @Test fun divide() = assertApprox(4.0, CalculatorEngine.applyOperator(12.0, KeyAction.DIVIDE, 3.0))
    @Test fun yPowX() = assertApprox(8.0, CalculatorEngine.applyOperator(2.0, KeyAction.Y_POW_X, 3.0))

    // ── One-operand math ─────────────────────────────────────────────────────

    @Test fun sqrt_144() = assertApprox(12.0, CalculatorEngine.sqrt(144.0))
    @Test fun xSquared() = assertApprox(25.0, CalculatorEngine.xSquared(5.0))
    @Test fun reciprocal() = assertApprox(0.25, CalculatorEngine.reciprocal(4.0))
    @Test fun ePowX() = assertApprox(7.389, CalculatorEngine.ePowX(2.0))
    @Test fun ln_e() = assertApprox(1.0, CalculatorEngine.ln(Math.E))
    @Test fun factorial_5() = assertApprox(120.0, CalculatorEngine.factorial(5.0))
    @Test fun factorial_0() = assertApprox(1.0, CalculatorEngine.factorial(0.0))

    @Test fun nPr_5_2() = assertApprox(20.0, CalculatorEngine.nPr(5.0, 2.0))
    @Test fun nCr_5_2() = assertApprox(10.0, CalculatorEngine.nCr(5.0, 2.0))

    // ── Trig – degrees ───────────────────────────────────────────────────────

    @Test fun sin_30deg() = assertApprox(0.5, CalculatorEngine.sin(30.0, AngleMode.DEG))
    @Test fun cos_60deg() = assertApprox(0.5, CalculatorEngine.cos(60.0, AngleMode.DEG))
    @Test fun tan_45deg() = assertApprox(1.0, CalculatorEngine.tan(45.0, AngleMode.DEG))

    /** Manual example: INV SIN 0.62 → 38.32° */
    @Test fun asin_deg() = assertApprox(38.32, CalculatorEngine.asin(0.62, AngleMode.DEG))

    @Test fun acos_deg() = assertApprox(60.0, CalculatorEngine.acos(0.5, AngleMode.DEG))
    @Test fun atan_deg() = assertApprox(45.0, CalculatorEngine.atan(1.0, AngleMode.DEG))

    // ── Trig – radians ───────────────────────────────────────────────────────

    @Test fun sin_rad() = assertApprox(1.0, CalculatorEngine.sin(Math.PI / 2, AngleMode.RAD))
    @Test fun cos_rad() = assertApprox(-1.0, CalculatorEngine.cos(Math.PI, AngleMode.RAD))

    // ── Hyperbolic ───────────────────────────────────────────────────────────

    @Test fun sinh_1() = assertApprox(1.1752, CalculatorEngine.sinh(1.0))
    @Test fun cosh_1() = assertApprox(1.5431, CalculatorEngine.cosh(1.0))
    @Test fun tanh_1() = assertApprox(0.7616, CalculatorEngine.tanh(1.0))

    // ── Display formatting ───────────────────────────────────────────────────

    @Test fun format_2dp() = assertEquals("1.23", CalculatorEngine.formatDisplay(1.234567, 2))
    @Test fun format_0dp() = assertEquals("1,235", CalculatorEngine.formatDisplay(1234.5678, 0))
    @Test fun format_thousands() = assertEquals("1,234.57", CalculatorEngine.formatDisplay(1234.567, 2))
    @Test fun format_negative() = assertEquals("-3.14", CalculatorEngine.formatDisplay(-3.14159, 2))
    @Test fun format_zero() = assertEquals("0.00", CalculatorEngine.formatDisplay(0.0, 2))
    @Test fun format_scientific_large() {
        val s = CalculatorEngine.formatDisplay(1.23e12, 2)
        assert(s.contains("E")) { "expected scientific notation but got $s" }
    }

    @Test fun format_largeAmountsShowCents() {
        assertEquals("-10,000,000.00", CalculatorEngine.formatDisplay(-10_000_000.0, 2))
        assertEquals("100,000,000.00", CalculatorEngine.formatDisplay(100_000_000.0, 2))
        assertEquals("-230,845,592.79", CalculatorEngine.formatDisplay(-230845592.790985, 2))
        assertEquals("-9,999,999,999.99", CalculatorEngine.formatDisplay(-9_999_999_999.99, 2))
    }

    /** Numbers have 12 digits, so whole digits take the place of decimals. */
    @Test fun format_keepsTwelveDigits() {
        assertEquals("12,345,678,901.2", CalculatorEngine.formatDisplay(12_345_678_901.2, 2))
        assertEquals("-123,456,789,012", CalculatorEngine.formatDisplay(-123_456_789_012.0, 2))
        assertEquals("1,234.56789012", CalculatorEngine.formatDisplay(1234.56789012, 9))
        assertEquals("0.123456789", CalculatorEngine.formatDisplay(0.123456789012, 9))
    }

    @Test fun format_roundingAcrossTheLimits() {
        assertEquals("10,000,000,000.0", CalculatorEngine.formatDisplay(9_999_999_999.996, 2))
        assertEquals("999,999,999,999", CalculatorEngine.formatDisplay(999_999_999_999.4, 2))
        assertEquals("1.00E12", CalculatorEngine.formatDisplay(999_999_999_999.6, 2))
        assertEquals("-1.00E12", CalculatorEngine.formatDisplay(-1e12, 2))
    }
}
