package com.js10bii.calculator.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class BondEngineTest {

    private fun assertClose(expected: Double, actual: Double, tol: Double = 0.05) {
        assertTrue("Expected $expected but was $actual", abs(expected - actual) <= tol)
    }

    // HP 10bII+ manual example (semi-annual bond):
    // N=20 periods, CPN%=8%, CALL=100, YTM=9% → PRICE ≈ 93.50
    @Test fun price_semiAnnual() {
        val p = BondEngine.price(20.0, 9.0, 8.0, 100.0, true)
        assertClose(93.50, p, 0.10)
    }

    // Reverse: given PRICE=93.50, find YTM → 9%
    @Test fun ytm_semiAnnual() {
        val y = BondEngine.ytm(20.0, 93.50, 8.0, 100.0, true)
        assertClose(9.0, y, 0.10)
    }

    // Par bond: CPN% == YTM → price == 100
    @Test fun price_parBond() {
        val p = BondEngine.price(10.0, 8.0, 8.0, 100.0, true)
        assertClose(100.0, p, 0.01)
    }

    // Annual bond: N=10, CPN%=10%, YTM=12%, CALL=100 → PRICE ≈ 88.70
    @Test fun price_annual() {
        val p = BondEngine.price(10.0, 12.0, 10.0, 100.0, false)
        assertClose(88.70, p, 0.10)
    }

    // Zero coupon: CPN%=0, N=10, YTM=8% → PRICE = 100/(1.04)^20 ≈ 45.64
    @Test fun price_zeroCoupon() {
        val p = BondEngine.price(20.0, 8.0, 0.0, 100.0, true)
        assertClose(45.64, p, 0.10)
    }

    // Accrued interest for half-period at 8% semi-annual = 8/2 * 0.5 = 2.0
    @Test fun accruedInterest() {
        assertClose(2.0, BondEngine.accruedInterest(8.0, 0.5, true))
    }
}
