package com.js10bii.calculator.engine

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.expm1
import kotlin.math.ln1p

/** Finds the periodic rate at which a series of level cash flows is worth nothing. */
internal object RateSolver {

    /** [count] payments of [amount], the first one [start] periods from now. */
    class Level(val amount: Double, val start: Double, val count: Double)

    private const val LOWEST = -0.9999
    private const val HIGHEST = 100.0

    /**
     * Returns the rate closest to zero (the positive one first if both signs qualify).
     * Searches outward from zero for a sign change and bisects it, so it cannot diverge.
     */
    fun solve(flows: List<Level>): Double {
        val end = flows.maxOf { it.start + it.count - 1 }
        val atZero = flows.sumOf { it.amount * it.count }
        val scale = flows.sumOf { abs(it.amount) * it.count }
        if (scale == 0.0 || flows.all { it.amount >= 0 } || flows.all { it.amount <= 0 }) throw ArithmeticException("No solution")
        // Amounts carry 12 digits, so a smaller imbalance than this is rounding
        if (abs(atZero) <= scale * 1e-11) return 0.0

        var inner = 0.0
        var outer = 1e-7
        while (inner < HIGHEST) {
            val up = minOf(outer, HIGHEST)
            if (changesSign(flows, end, inner, up)) return bisect(flows, end, inner, up)
            val down = maxOf(-outer, LOWEST)
            if (-inner > LOWEST && changesSign(flows, end, down, -inner)) return bisect(flows, end, down, -inner)
            inner = outer
            outer *= 1.25
        }
        throw ArithmeticException("No solution")
    }

    private fun changesSign(flows: List<Level>, end: Double, a: Double, b: Double): Boolean =
        (value(flows, end, a) > 0) != (value(flows, end, b) > 0)

    private fun bisect(flows: List<Level>, end: Double, from: Double, to: Double): Double {
        var lo = from
        var hi = to
        val loPositive = value(flows, end, lo) > 0
        while (true) {
            val mid = (lo + hi) / 2.0
            if (mid <= lo || mid >= hi) return mid
            if ((value(flows, end, mid) > 0) == loPositive) lo = mid else hi = mid
        }
    }

    /**
     * Worth of the flows at rate [i]. Positive rates discount to the start and negative rates to the end,
     * which has the same sign but never overflows. expm1/ln1p keep small rates accurate.
     */
    private fun value(flows: List<Level>, end: Double, i: Double): Double {
        val growth = ln1p(i)
        return flows.sumOf { f ->
            val last = f.start + f.count - 1
            when {
                i == 0.0 -> f.amount * f.count
                i > 0.0  -> f.amount * exp(-growth * (f.start - 1)) * -expm1(-growth * f.count) / i
                else     -> f.amount * exp(growth * (end - last)) * expm1(growth * f.count) / i
            }
        }
    }
}
