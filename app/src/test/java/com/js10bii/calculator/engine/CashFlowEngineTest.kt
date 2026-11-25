package com.js10bii.calculator.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class CashFlowEngineTest {

    private fun assertClose(expected: Double, actual: Double, tol: Double = 0.01) {
        assertTrue("Expected $expected but was $actual", abs(expected - actual) <= tol)
    }

    // CF0=-1000, CF1=200, CF2=300, CF3=400, CF4=500, rate=10%/yr
    // NPV = -1000 + 200/1.1 + 300/1.21 + 400/1.331 + 500/1.4641 = 71.78
    @Test fun npv_basicExample() {
        val flows = listOf(
            CashFlowEntry(-1000.0),
            CashFlowEntry(200.0),
            CashFlowEntry(300.0),
            CashFlowEntry(400.0),
            CashFlowEntry(500.0),
        )
        assertClose(71.78, CashFlowEngine.npv(0.10, flows), 0.05)
    }

    // CF0=-1000, CF1=500 (count=3), rate=12%
    // NPV = -1000 + 500/1.12 + 500/1.2544 + 500/1.404928 = 200.92
    @Test fun npv_withRepeatedCF() {
        val flows = listOf(
            CashFlowEntry(-1000.0),
            CashFlowEntry(500.0, 3),
        )
        assertClose(200.92, CashFlowEngine.npv(0.12, flows), 0.05)
    }

    // IRR of CF0=-1000, CF1=200, CF2=300, CF3=400, CF4=500 ≈ 12.82%
    // Verify by checking NPV at the returned rate ≈ 0
    @Test fun irr_basicExample() {
        val flows = listOf(
            CashFlowEntry(-1000.0),
            CashFlowEntry(200.0),
            CashFlowEntry(300.0),
            CashFlowEntry(400.0),
            CashFlowEntry(500.0),
        )
        val irr = CashFlowEngine.irr(flows)
        val npvAtIrr = CashFlowEngine.npv(irr, flows)
        assertTrue("NPV at IRR should be ~0, was $npvAtIrr", abs(npvAtIrr) < 0.01)
        assertClose(0.1282, irr, 0.001)
    }

    // Simple: invest -100, get back 110 → IRR = 10%
    @Test fun irr_simple() {
        val flows = listOf(CashFlowEntry(-100.0), CashFlowEntry(110.0))
        assertClose(0.10, CashFlowEngine.irr(flows), 0.0001)
    }

    @Test(expected = ArithmeticException::class)
    fun irr_noSignChange() {
        CashFlowEngine.irr(listOf(CashFlowEntry(100.0), CashFlowEntry(200.0)))
    }
}
