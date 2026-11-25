package com.js10bii.calculator.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class ProbabilityEngineTest {

    private fun assertClose(expected: Double, actual: Double, tol: Double = 0.001) {
        assertTrue("Expected $expected but was $actual", abs(expected - actual) <= tol)
    }

    // Lower-tail probabilities, as defined in ch. 12 of the HP 10bII+ manual. Values from standard tables.
    @Test fun zToP_z1() { assertClose(0.8413, ProbabilityEngine.zToP(1.0)) }

    @Test fun zToP_z196() { assertClose(0.9750, ProbabilityEngine.zToP(1.96)) }

    @Test fun zToP_z2576() { assertClose(0.9950, ProbabilityEngine.zToP(2.576)) }

    @Test fun zToP_z0() { assertClose(0.5, ProbabilityEngine.zToP(0.0)) }

    @Test fun zToP_symmetry() { assertClose(1.0, ProbabilityEngine.zToP(1.5) + ProbabilityEngine.zToP(-1.5), 1e-12) }

    @Test fun zToP_extremes() {
        assertClose(0.0, ProbabilityEngine.zToP(-50.0), 0.0)
        assertClose(1.0, ProbabilityEngine.zToP(50.0), 0.0)
    }

    @Test fun tToP_df19_t2093() { assertClose(0.9750, ProbabilityEngine.tToP(2.093, 19.0)) }

    @Test fun tToP_df10_t1812() { assertClose(0.9500, ProbabilityEngine.tToP(1.812, 10.0)) }

    @Test fun tToP_df30_t2042() { assertClose(0.9750, ProbabilityEngine.tToP(2.042, 30.0)) }

    @Test fun tToP_largeDf_approachesNormal() { assertClose(0.9750, ProbabilityEngine.tToP(1.96, 1000.0)) }

    @Test fun tToP_symmetry() { assertClose(1.0, ProbabilityEngine.tToP(1.5, 7.0) + ProbabilityEngine.tToP(-1.5, 7.0), 1e-12) }

    @Test(expected = ArithmeticException::class)
    fun tToP_invalidDf() {
        ProbabilityEngine.tToP(1.0, 0.0)
    }
}
