package com.js10bii.calculator.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class BreakEvenEngineTest {

    private fun assertClose(expected: Double, actual: Double, tol: Double = 0.01) {
        assertTrue("Expected $expected but was $actual", abs(expected - actual) <= tol)
    }

    // HP 10bII+ manual example: FC=10000, VC=5, SP=15 → break-even Units=1000
    @Test fun solveUnits_breakeven() {
        assertClose(1_000.0, BreakEvenEngine.solveUnits(10_000.0, 15.0, 5.0))
    }

    // At 1500 units: Profit = 1500*(15-5) - 10000 = 5000
    @Test fun solveProfit() {
        assertClose(5_000.0, BreakEvenEngine.solveProfit(1_500.0, 10_000.0, 15.0, 5.0))
    }

    // Given Units=1000, FC=10000, VC=5, Profit=0 → SP=15
    @Test fun solveSP() {
        assertClose(15.0, BreakEvenEngine.solveSP(1_000.0, 10_000.0, 5.0, 0.0))
    }

    // Given Units=1000, FC=10000, SP=15, Profit=0 → VC=5
    @Test fun solveVC() {
        assertClose(5.0, BreakEvenEngine.solveVC(1_000.0, 10_000.0, 15.0, 0.0))
    }

    // Given Units=1000, SP=15, VC=5, Profit=0 → FC=10000
    @Test fun solveFC() {
        assertClose(10_000.0, BreakEvenEngine.solveFC(1_000.0, 15.0, 5.0, 0.0))
    }

    // With target profit: to earn 5000 profit at FC=10000, SP=15, VC=5 → Units=1500
    @Test fun solveSP_withProfit() {
        // 1500 units, profit target 5000 → SP = VC + (FC + Profit)/Units = 5 + 15000/1500 = 15
        assertClose(15.0, BreakEvenEngine.solveSP(1_500.0, 10_000.0, 5.0, 5_000.0))
    }

    @Test(expected = ArithmeticException::class)
    fun solveUnits_spEqualsVc() {
        BreakEvenEngine.solveUnits(10_000.0, 5.0, 5.0)
    }
}
