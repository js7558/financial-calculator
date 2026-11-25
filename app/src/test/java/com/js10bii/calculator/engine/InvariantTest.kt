package com.js10bii.calculator.engine

import com.js10bii.calculator.support.checkAll
import com.js10bii.calculator.support.near
import com.js10bii.calculator.viewmodel.AngleMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.random.Random

/** Identities that must hold for any input, plus domain edges. */
class InvariantTest {

    private fun assertDomainError(f: () -> Double) {
        assertThrows(ArithmeticException::class.java) { f() }
    }

    // ── Amortization ─────────────────────────────────────────────────────────

    private data class Loan(val n: Int, val rate: Double, val pv: Double, val begin: Boolean)

    private val loans = Random(7).let { r ->
        List(200) { Loan(r.nextInt(2, 361), r.nextDouble(0.5, 25.0), r.nextDouble(500.0, 500_000.0), r.nextBoolean()) }
    }

    @Test fun amortization_principalPlusInterestIsPayments_andLoanReachesZero() = checkAll(loans) { l ->
        val pmt = TvmEngine.solve(l.n.toDouble(), l.rate, l.pv, null, 0.0, 12.0, l.begin)
        val (prin, int, bal) = TvmEngine.amortize(1, l.n, l.pv, l.rate, pmt, 12.0, l.begin)
        near(pmt * l.n, prin + int, 0.001) ?: near(0.0, bal, 0.001) ?: near(-l.pv, prin, 0.001)
    }

    /** Closed form for the balance after any payment, and for the split over any range. */
    @Test fun amortization_matchesClosedForm() = checkAll(loans) { l ->
        val i = l.rate / 1200
        val pmt = TvmEngine.solve(l.n.toDouble(), l.rate, l.pv, null, 0.0, 12.0, l.begin)
        fun balance(k: Int): Double {
            if (k == 0) return l.pv
            val growth = Math.pow(1 + i, k.toDouble())
            // In BEGIN mode payment k falls one period earlier, so the loan has grown for k-1 periods.
            val grown = if (l.begin) l.pv * growth / (1 + i) else l.pv * growth
            return grown + pmt * (growth - 1) / i
        }
        listOf(1 to 1, 1 to l.n, 2 to l.n, l.n to l.n, (l.n + 1) / 2 to l.n).filter { it.first <= it.second }.firstNotNullOfOrNull { (a, b) ->
            val (prin, int, bal) = TvmEngine.amortize(a, b, l.pv, l.rate, pmt, 12.0, l.begin)
            val expectedPrin = balance(b) - balance(a - 1)
            near(balance(b), bal, 0.001) ?: near(expectedPrin, prin, 0.001) ?: near(pmt * (b - a + 1) - expectedPrin, int, 0.001)
        }
    }

    // ── Rate solvers ─────────────────────────────────────────────────────────

    private data class Deal(val n: Int, val rate: Double, val pyr: Double, val pv: Double, val fv: Double, val begin: Boolean)

    /** Random loans, savings plans and leases: solve PMT, then get the rate back. */
    @Test fun solveIYR_recoversRate() = checkAll(
        Random(23).let { r ->
            List(3000) { k ->
                val pv = r.nextDouble(500.0, 1e6)
                val (a, b) = when (k % 3) { 0 -> pv to 0.0; 1 -> 0.0 to pv; else -> -pv to pv * r.nextDouble(0.05, 0.6) }
                Deal(r.nextInt(2, 481), r.nextDouble(-1.0, 30.0), listOf(1.0, 2.0, 4.0, 12.0, 26.0, 52.0).random(r), a, b, r.nextBoolean())
            }.filter { Math.pow(1 + it.rate / 100 / it.pyr, it.n.toDouble()) < 1e6 }
        },
    ) { d ->
        val pmt = TvmEngine.solve(d.n.toDouble(), d.rate, d.pv, null, d.fv, d.pyr, d.begin)
        near(d.rate, TvmEngine.solve(d.n.toDouble(), null, d.pv, pmt, d.fv, d.pyr, d.begin), 1e-6)
    }

    @Test fun solveIYR_ignoresScaleAndSign() = checkAll(listOf(1e-3, 1.0, 1e6, -1.0)) { k ->
        near(9.25, TvmEngine.solve(60.0, null, 10_000.0 * k, TvmEngine.solve(60.0, 9.25, 10_000.0 * k, null, 0.0, 12.0, false), 0.0, 12.0, false), 1e-8)
    }

    @Test fun solveIYR_withoutASolutionIsError() {
        assertDomainError { TvmEngine.solve(12.0, null, 1000.0, 100.0, 0.0, 12.0, false) }
        assertDomainError { TvmEngine.solve(12.0, null, 0.0, 0.0, 1000.0, 12.0, false) }
        assertDomainError { TvmEngine.solve(12.0, null, 0.0, 0.0, 0.0, 12.0, false) }
        assertDomainError { TvmEngine.solve(0.0, null, 1000.0, -100.0, 0.0, 12.0, false) }
    }

    /** With two valid rates, 10% and 20%, take the one closer to zero, as the old solver did. */
    @Test fun irr_twoSolutions() {
        val flows = listOf(CashFlowEntry(-1000.0), CashFlowEntry(2300.0), CashFlowEntry(-1320.0))
        assertEquals(0.10, CashFlowEngine.irr(flows), 1e-9)
    }

    /** Flows of cents used to pass the old "NPV < 1" convergence check with any rate. */
    @Test fun irr_tinyAmounts() = assertEquals(0.2, CashFlowEngine.irr(listOf(CashFlowEntry(-0.05), CashFlowEntry(0.06))), 1e-9)

    @Test fun irr_veryLongStream() {
        val monthly = 0.004
        val pmt = 1000 * monthly / (1 - Math.pow(1 + monthly, -1200.0))
        assertEquals(monthly, CashFlowEngine.irr(listOf(CashFlowEntry(-1000.0), CashFlowEntry(pmt, 1200))), 1e-9)
    }

    // ── Dates, against java.time ─────────────────────────────────────────────

    private val dates = Random(11).let { r ->
        val lo = LocalDate.of(1583, 1, 1).toEpochDay()
        val hi = LocalDate.of(9999, 12, 31).toEpochDay()
        List(5000) { LocalDate.ofEpochDay(r.nextLong(lo, hi)) }
    }

    private fun enc(d: LocalDate, dmy: Boolean) = DateEngine.encodeDate(d.year, d.monthValue, d.dayOfMonth, dmy)

    @Test fun date_encodeParseRoundTrip() = checkAll(dates) { d ->
        listOf(true, false).firstNotNullOfOrNull { dmy ->
            val p = DateEngine.parseDate(enc(d, dmy), dmy)
            if (p == Triple(d.year, d.monthValue, d.dayOfMonth)) null else "dmy=$dmy parsed $p"
        }
    }

    @Test fun date_daysBetween() = checkAll(dates.zipWithNext()) { (a, b) ->
        near(ChronoUnit.DAYS.between(a, b).toDouble(), DateEngine.daysBetweenActual(enc(a, false), enc(b, false), false), 0.0)
    }

    @Test fun date_addDays() = checkAll(dates.filter { it.year in 1700..9800 }) { d ->
        listOf(1L, -1L, 100L, -400L, 36525L).firstNotNullOfOrNull { k ->
            val got = DateEngine.parseDate(DateEngine.addDays(enc(d, false), k.toDouble(), false), false)
            val want = d.plusDays(k).let { Triple(it.year, it.monthValue, it.dayOfMonth) }
            if (got == want) null else "+$k expected $want got $got"
        }
    }

    @Test fun date_leapDaysAndRangeEndsAreAccepted() {
        assertEquals(1.0, DateEngine.daysBetweenActual(2.292012, 3.012012, false), 0.0)
        assertEquals(1.0, DateEngine.daysBetweenActual(10.151582, 10.161582, false), 0.0)
        assertEquals(12.319999, DateEngine.addDays(12.309999, 1.0, false), 1e-9)
    }

    @Test fun date_impossibleDateIsRejected() {
        assertDomainError { DateEngine.daysBetweenActual(2.302011, 3.012011, false) } // Feb 30
        assertDomainError { DateEngine.daysBetweenActual(13.012011, 3.012011, false) } // month 13
        assertDomainError { DateEngine.addDays(2.292011, 1.0, false) } // not a leap year
        assertDomainError { DateEngine.addDays(12.319999, 1.0, false) } // past the last date
        assertDomainError { DateEngine.addDays(1.012000, 1e12, false) }
    }

    // ── Depreciation ─────────────────────────────────────────────────────────

    private data class Asset(val cost: Double, val salvage: Double, val life: Int)

    private val assets = Random(3).let { r ->
        List(200) { val c = r.nextDouble(1000.0, 1e6); Asset(c, c * r.nextDouble(0.0, 0.5), r.nextInt(1, 41)) }
    }

    private fun checkSchedule(f: (Asset, Int) -> Triple<Double, Double, Double>) = checkAll(assets) { a ->
        var total = 0.0
        (1..a.life).firstNotNullOfOrNull { y ->
            val (d, acc, rem) = f(a, y)
            total += d
            near(total, acc, 1e-4) ?: near(a.cost - total, rem, 1e-4) ?: if (rem < a.salvage - 1e-4) "below salvage in year $y" else null
        }
    }

    @Test fun straightLine_schedule() = checkSchedule { a, y -> DepreciationEngine.straightLine(a.cost, a.salvage, a.life, y) }
    @Test fun soyd_schedule() = checkSchedule { a, y -> DepreciationEngine.soyd(a.cost, a.salvage, a.life, y) }
    @Test fun decliningBalance_schedule() = checkSchedule { a, y -> DepreciationEngine.decliningBalance(a.cost, a.salvage, a.life, y) }

    /** Appendix B formula, as long as it stays above salvage. */
    @Test fun decliningBalance_matchesFormula() = checkAll(assets) { a ->
        listOf(1.0, 1.25, 1.5, 2.0).firstNotNullOfOrNull { f ->
            (1 until a.life).firstNotNullOfOrNull { y ->
                val rate = f / a.life
                val bookAfter = a.cost * Math.pow(1 - rate, y.toDouble())
                if (bookAfter < a.salvage) null
                else near(a.cost * rate * Math.pow(1 - rate, y - 1.0), DepreciationEngine.decliningBalance(a.cost, a.salvage, a.life, y, f).first, 1e-6)
            }
        }
    }

    @Test fun depreciation_yearBeyondLifeIsZero() {
        assertEquals(0.0, DepreciationEngine.straightLine(10_000.0, 500.0, 5, 6).first, 1e-9)
        assertEquals(0.0, DepreciationEngine.soyd(10_000.0, 500.0, 5, 7).first, 1e-9)
    }

    @Test fun depreciation_zeroLifeIsError() { assertDomainError { DepreciationEngine.straightLine(10_000.0, 0.0, 0, 1).first } }

    // ── Statistics ───────────────────────────────────────────────────────────

    @Test fun stdDevOfIdenticalValuesIsZero() = checkAll(List(500) { (it + 1) / 10.0 }) { x ->
        listOf(3, 7, 10).firstNotNullOfOrNull { n ->
            var sx = 0.0; var sxx = 0.0
            repeat(n) { sx += x; sxx += x * x }
            near(0.0, StatisticsEngine.sampleStdDev(n.toDouble(), sx, sxx), 1e-5)
        }
    }

    @Test fun regressionRecoversExactLine() = checkAll(List(100) { Random(it) }) { r ->
        val m = r.nextDouble(-50.0, 50.0); val b = r.nextDouble(-1000.0, 1000.0)
        val pts = List(8) { r.nextDouble(-100.0, 100.0) }.map { it to m * it + b }
        val n = 8.0; val sx = pts.sumOf { it.first }; val sy = pts.sumOf { it.second }
        val sxx = pts.sumOf { it.first * it.first }; val sxy = pts.sumOf { it.first * it.second }
        near(m, StatisticsEngine.slope(n, sx, sy, sxx, sxy), 1e-8) ?: near(b, StatisticsEngine.intercept(n, sx, sy, sxx, sxy), 1e-7)
    }

    // ── Rate conversion, percentages, break-even ─────────────────────────────

    @Test fun nominalEffectiveRoundTrip() = checkAll(listOf(1.0, 2.0, 4.0, 12.0, 52.0, 365.0)) { p ->
        listOf(0.01, 1.0, 10.0, 36.0).firstNotNullOfOrNull { near(it, TvmEngine.effToNom(TvmEngine.nomToEff(it, p), p), 1e-8) }
    }

    @Test fun marginMarkupRoundTrips() = checkAll(List(100) { Random(it) }) { r ->
        val cost = r.nextDouble(1.0, 1000.0); val price = cost * r.nextDouble(0.5, 5.0)
        near(price, PercentageEngine.priceFromMarkup(cost, PercentageEngine.markup(cost, price)), 1e-8)
            ?: near(cost, PercentageEngine.costFromMargin(price, PercentageEngine.margin(cost, price)), 1e-8)
    }

    // ── Scientific functions ─────────────────────────────────────────────────

    @Test fun sineOfWholeTurnsIsZero() = checkAll((0..40).map { it * 180.0 }) { near(0.0, CalculatorEngine.sin(it, AngleMode.DEG), 0.0) }

    @Test fun cosineOfOddQuarterTurnsIsZero() = checkAll((0..40).map { 90.0 + it * 180.0 }) { near(0.0, CalculatorEngine.cos(it, AngleMode.DEG), 0.0) }

    @Test fun tangentOf90IsError() = assertDomainError { CalculatorEngine.tan(90.0, AngleMode.DEG) }

    @Test fun asinhIsOddAndInvertsSinh() = checkAll(listOf(1e-9, 0.5, 5.0, 1e3, 1e8, 1e10)) { x ->
        near(0.0, (CalculatorEngine.asinh(-x) + CalculatorEngine.asinh(x)), 1e-9)
            ?: near(1.0, kotlin.math.sinh(CalculatorEngine.asinh(x)) / x, 1e-6)
    }

    @Test fun inverseTrigDomainErrors() {
        assertDomainError { CalculatorEngine.asin(2.0, AngleMode.DEG) }
        assertDomainError { CalculatorEngine.acos(-2.0, AngleMode.DEG) }
    }

    @Test fun fractionalPowerOfNegativeIsError() = assertDomainError { CalculatorEngine.applyOperator(-8.0, KeyAction.Y_POW_X, 0.5) }

    @Test fun combinationsWorkForLargeN() {
        assertEquals(1.0, CalculatorEngine.nCr(200.0, 100.0) / 9.054851465610328e58, 1e-9)
        assertEquals(19900.0, CalculatorEngine.nCr(200.0, 2.0), 1e-6)
        assertEquals(39800.0, CalculatorEngine.nPr(200.0, 2.0), 1e-6)
    }

    @Test fun displayNeverShowsNegativeZero() {
        assertEquals("0.00", CalculatorEngine.formatDisplay(-0.0, 2))
    }
}
