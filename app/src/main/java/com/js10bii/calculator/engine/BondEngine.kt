package com.js10bii.calculator.engine

import kotlin.math.*

/** Bond formulas from the HP 10bII+ manual, appendix B. Prices are per 100 of face value, without accrued interest. */
object BondEngine {

    /**
     * Coupon [periods] left until redemption, counting the current one, and the fraction of the current
     * one that has [accrued] at settlement.
     */
    class Schedule(val periods: Int, val accrued: Double)

    /** [settle] and [redemption] are Julian days; the redemption date is a coupon date. */
    fun schedule(settle: Int, redemption: Int, semi: Boolean, actual: Boolean): Schedule {
        if (redemption <= settle) throw ArithmeticException("Dates?")
        val months = if (semi) 6 else 12
        var periods = 1
        while (couponDate(redemption, periods * months) > settle) periods++
        val previous = couponDate(redemption, periods * months)
        val next = couponDate(redemption, (periods - 1) * months)
        val accrued = if (actual) (settle - previous).toDouble() / (next - previous)
                      else DateEngine.days360(previous, settle).toDouble() / (30 * months)
        return Schedule(periods, accrued)
    }

    /** Bonds that mature at a month's end pay at month ends; others pay on the same day where it exists. */
    private fun couponDate(redemption: Int, monthsBefore: Int): Int {
        val (y, m, d) = DateEngine.fromJulianDay(redemption)
        val month = y * 12 + (m - 1) - monthsBefore
        val year = Math.floorDiv(month, 12)
        val monthOfYear = Math.floorMod(month, 12) + 1
        val last = DateEngine.daysInMonth(year, monthOfYear)
        val day = if (d == DateEngine.daysInMonth(y, m)) last else min(d, last)
        return DateEngine.julianDay(year, monthOfYear, day)
    }

    fun price(schedule: Schedule, ytmPct: Double, cpnPct: Double, call: Double, semi: Boolean): Double =
        price(schedule.periods.toDouble(), schedule.accrued, ytmPct / 100.0 / freq(semi), cpnPct / freq(semi), call)

    fun ytm(schedule: Schedule, pricePer100: Double, cpnPct: Double, call: Double, semi: Boolean): Double =
        periodicYield(schedule.periods.toDouble(), schedule.accrued, pricePer100, cpnPct / freq(semi), call) * freq(semi) * 100.0

    /** Settlement on a coupon date, with [n] whole periods to go. */
    fun price(n: Double, ytmPct: Double, cpnPct: Double, call: Double, semi: Boolean): Double =
        price(n, 0.0, ytmPct / 100.0 / freq(semi), cpnPct / freq(semi), call)

    fun ytm(n: Double, pricePer100: Double, cpnPct: Double, call: Double, semi: Boolean): Double =
        periodicYield(n, 0.0, pricePer100, cpnPct / freq(semi), call) * freq(semi) * 100.0

    fun accruedInterest(cpnPct: Double, periodFraction: Double, semi: Boolean): Double =
        cpnPct / freq(semi) * periodFraction

    private fun freq(semi: Boolean) = if (semi) 2.0 else 1.0

    /** [y] is the yield and [c] the coupon per period; [a] is the accrued fraction of the current period. */
    private fun price(n: Double, a: Double, y: Double, c: Double, call: Double): Double {
        // One period or less to redemption: simple interest
        if (n <= 1.0) return (call + c) / (1.0 + (1.0 - a) * y) - a * c
        // Value at the last coupon date, carried forward to settlement
        val growth = ln1p(y)
        val annuity = if (y == 0.0) n else -expm1(-growth * n) / y
        return (c * annuity + call * exp(-growth * n)) * exp(growth * a) - a * c
    }

    /** The price falls as the yield rises, so bisection always finds the yield if there is one. */
    private fun periodicYield(n: Double, a: Double, target: Double, c: Double, call: Double): Double {
        fun excess(y: Double) = price(n, a, y, c, call) - target
        if (abs(excess(0.0)) <= abs(target) * 1e-11) return 0.0
        var lo = -0.9999
        var hi = 1.0
        while (excess(hi) > 0) {
            hi *= 2.0
            if (hi > 1e6) throw ArithmeticException("No solution")
        }
        if (excess(lo) < 0) throw ArithmeticException("No solution")
        while (true) {
            val mid = (lo + hi) / 2.0
            if (mid <= lo || mid >= hi) return mid
            if (excess(mid) > 0) lo = mid else hi = mid
        }
    }
}
