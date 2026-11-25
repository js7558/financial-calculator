package com.js10bii.calculator.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class DepreciationEngineTest {

    private fun assertClose(expected: Double, actual: Double, tol: Double = 0.01) {
        assertTrue("Expected $expected but was $actual", abs(expected - actual) <= tol)
    }

    // HP 10bII+ manual: Cost=10000, Salvage=500, Life=5 years

    // Straight-line: annual depr = (10000-500)/5 = 1900
    @Test fun straightLine_year1() {
        val (depr, acc, rem) = DepreciationEngine.straightLine(10_000.0, 500.0, 5, 1)
        assertClose(1_900.0, depr)
        assertClose(1_900.0, acc)
        assertClose(8_100.0, rem)
    }

    @Test fun straightLine_year3() {
        val (depr, acc, rem) = DepreciationEngine.straightLine(10_000.0, 500.0, 5, 3)
        assertClose(1_900.0, depr)
        assertClose(5_700.0, acc)
        assertClose(4_300.0, rem)
    }

    // SOYD: SYD = 5*6/2 = 15
    // Year 1: 5/15 * 9500 = 3166.67
    // Year 2: 4/15 * 9500 = 2533.33
    @Test fun soyd_year1() {
        val (depr, acc, rem) = DepreciationEngine.soyd(10_000.0, 500.0, 5, 1)
        assertClose(3_166.67, depr)
    }

    @Test fun soyd_year2() {
        val (depr, acc, rem) = DepreciationEngine.soyd(10_000.0, 500.0, 5, 2)
        assertClose(2_533.33, depr)
        assertClose(5_700.0, acc, 1.0)
    }

    // Declining Balance (200%, i.e. factor=2): rate = 2/5 = 0.4
    // Years 1-4: 4000, 2400, 1440, 864. The last year takes what remains above salvage: 1296 - 500 = 796
    @Test fun decliningBalance_year1() {
        val (depr, _, _) = DepreciationEngine.decliningBalance(10_000.0, 500.0, 5, 1, 2.0)
        assertClose(4_000.0, depr)
    }

    @Test fun decliningBalance_lastYearTakesTheRest() {
        val (depr, _, rem) = DepreciationEngine.decliningBalance(10_000.0, 500.0, 5, 5, 2.0)
        assertClose(796.0, depr, 5.0)
        assertClose(500.0, rem, 5.0)
    }

    @Test fun straightLine_totalEqualsDepreciableBase() {
        var totalDepr = 0.0
        for (y in 1..5) {
            val (depr, _, _) = DepreciationEngine.straightLine(10_000.0, 500.0, 5, y)
            totalDepr += depr
        }
        assertClose(9_500.0, totalDepr)
    }
}
