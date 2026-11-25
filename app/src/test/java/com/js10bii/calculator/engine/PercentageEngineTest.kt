package com.js10bii.calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Test

class PercentageEngineTest {

    private fun assertApprox(expected: Double, actual: Double, tol: Double = 0.005) {
        assertEquals("expected $expected but got $actual", expected, actual, tol)
    }

    // ── % ────────────────────────────────────────────────────────────────────

    @Test fun percentOf() = assertApprox(2.625, PercentageEngine.percentOf(17.50, 15.0))

    // ── %CHG ─────────────────────────────────────────────────────────────────

    @Test fun percentChange_increase() = assertApprox(25.0, PercentageEngine.percentChange(80.0, 100.0))
    @Test fun percentChange_decrease() = assertApprox(-20.0, PercentageEngine.percentChange(100.0, 80.0))

    // ── CST / PRC / MAR ──────────────────────────────────────────────────────

    /**
     * Manual example: CST=15, PRC=22 → MAR=31.82%
     * margin = (price - cost) / price * 100
     */
    @Test fun margin_manual() = assertApprox(31.82, PercentageEngine.margin(15.0, 22.0))

    /** Solve PRC from CST=15 and MAR=31.82 → PRC=22 */
    @Test fun priceFromMargin_manual() = assertApprox(22.0, PercentageEngine.priceFromMargin(15.0, 31.82), tol=0.01)

    // ── CST / PRC / MU ───────────────────────────────────────────────────────

    /**
     * Manual example: CST=20, MU=33 → PRC=26.60
     * price = cost * (1 + mu/100)
     */
    @Test fun priceFromMarkup_manual() = assertApprox(26.60, PercentageEngine.priceFromMarkup(20.0, 33.0))

    /** Solve MU from CST=20, PRC=26.60 → MU=33 */
    @Test fun markup_manual() = assertApprox(33.0, PercentageEngine.markup(20.0, 26.60), tol=0.01)

    /** Solve CST from PRC=26.60, MU=33 → CST=20 */
    @Test fun costFromMarkup_manual() = assertApprox(20.0, PercentageEngine.costFromMarkup(26.60, 33.0), tol=0.01)

    /** Round-trip margin: cost=15, price=22 → margin → price back */
    @Test fun margin_roundTrip() {
        val mar = PercentageEngine.margin(15.0, 22.0)
        val price = PercentageEngine.priceFromMargin(15.0, mar)
        assertApprox(22.0, price, tol=0.01)
    }
}
