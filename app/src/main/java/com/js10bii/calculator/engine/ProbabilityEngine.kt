package com.js10bii.calculator.engine

import kotlin.math.*

object ProbabilityEngine {

    /** P(Z < z) for the standard normal distribution. */
    fun zToP(z: Double): Double {
        val a = abs(z)
        if (a > 38.0) return if (z < 0) 0.0 else 1.0
        val density = exp(-a * a / 2.0) / sqrt(2.0 * PI)
        if (a < 3.0) {
            // Phi(a) - 1/2 = density * (a + a^3/3 + a^5/(3*5) + ...)
            var term = a
            var sum = a
            var k = 1
            while (term > sum * 1e-17) {
                k += 2
                term *= a * a / k
                sum += term
            }
            return if (z < 0) 0.5 - density * sum else 0.5 + density * sum
        }
        // Upper tail = density / (a + 1/(a + 2/(a + 3/(a + ...)))), evaluated from the inside out
        var fraction = a
        for (k in 200 downTo 1) fraction = a + k / fraction
        val tail = density / fraction
        return if (z < 0) tail else 1.0 - tail
    }

    /** P(T < t) for Student's t distribution with df degrees of freedom. */
    fun tToP(t: Double, df: Double): Double {
        if (df < 1) throw ArithmeticException("df must be ≥ 1")
        // The incomplete beta I_x(df/2, 1/2) is the probability of both tails beyond |t|
        val tail = incompleteBeta(df / 2.0, 0.5, df / (df + t * t)) / 2.0
        return if (t < 0) tail else 1.0 - tail
    }

    /** Inverse of [zToP]. */
    fun pToZ(p: Double): Double = quantile(p, ::zToP)

    /** Inverse of [tToP]. */
    fun pToT(p: Double, df: Double): Double {
        if (df < 1) throw ArithmeticException("df must be ≥ 1")
        return quantile(p) { tToP(it, df) }
    }

    /** Solves cdf(x) = p by bisection on the lower half, where tail probabilities keep their precision. */
    private fun quantile(p: Double, cdf: (Double) -> Double): Double {
        if (!(p > 0.0 && p < 1.0)) throw ArithmeticException("P range")
        if (p > 0.5) return -quantile(1.0 - p, cdf)
        if (p == 0.5) return 0.0
        var lo = -1.0
        while (cdf(lo) > p) {
            lo *= 2.0
            if (lo < -1e300) throw ArithmeticException("P range")
        }
        var hi = 0.0
        while (true) {
            val mid = (lo + hi) / 2.0
            if (mid <= lo || mid >= hi) return mid
            if (cdf(mid) < p) lo = mid else hi = mid
        }
    }

    private fun incompleteBeta(a: Double, b: Double, x: Double): Double {
        if (x <= 0.0) return 0.0
        if (x >= 1.0) return 1.0
        if (x > (a + 1.0) / (a + b + 2.0)) return 1.0 - incompleteBeta(b, a, 1.0 - x)
        val lbeta = lgamma(a) + lgamma(b) - lgamma(a + b)
        val prefix = exp(a * ln(x) + b * ln(1.0 - x) - lbeta) / a
        val FPMIN = 1e-30
        var c = 1.0
        var d = 1.0 - (a + b) * x / (a + 1.0)
        if (abs(d) < FPMIN) d = FPMIN
        d = 1.0 / d
        var h = d
        for (m in 1..200) {
            val m2 = 2.0 * m
            var aa = m * (b - m) * x / ((a + m2 - 1.0) * (a + m2))
            d = 1.0 + aa * d; if (abs(d) < FPMIN) d = FPMIN
            c = 1.0 + aa / c; if (abs(c) < FPMIN) c = FPMIN
            d = 1.0 / d; h *= d * c
            aa = -(a + m) * (a + b + m) * x / ((a + m2) * (a + m2 + 1.0))
            d = 1.0 + aa * d; if (abs(d) < FPMIN) d = FPMIN
            c = 1.0 + aa / c; if (abs(c) < FPMIN) c = FPMIN
            d = 1.0 / d; val del = d * c; h *= del
            if (abs(del - 1.0) < 1e-10) break
        }
        return prefix * h
    }

    private fun lgamma(x: Double): Double {
        if (x < 0.5) return ln(PI / sin(PI * x)) - lgamma(1.0 - x)
        val z = x - 1.0
        val p = doubleArrayOf(
            0.99999999999980993, 676.5203681218851, -1259.1392167224028,
            771.32342877765313, -176.61502916214059, 12.507343278686905,
            -0.13857109526572012, 9.9843695780195716e-6, 1.5056327351493116e-7
        )
        var sum = p[0]
        for (i in 1 until p.size) sum += p[i] / (z + i)
        val t = z + 7.5
        return 0.5 * ln(2.0 * PI) + (z + 0.5) * ln(t) - t + ln(sum)
    }
}
