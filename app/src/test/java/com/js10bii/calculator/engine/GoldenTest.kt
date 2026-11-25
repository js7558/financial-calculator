package com.js10bii.calculator.engine

import com.js10bii.calculator.support.Golden
import com.js10bii.calculator.support.Row
import com.js10bii.calculator.support.checkAll
import com.js10bii.calculator.support.near
import org.junit.Test

/** Engines against 50-digit mpmath tables; see app/src/test/golden/generate.py. */
class GoldenTest {

    private val tvm = Golden.load("tvm.csv")

    private fun solve(r: Row, unknown: String): Double {
        fun v(name: String) = if (name == unknown) null else r.num(name)
        return TvmEngine.solve(v("n"), v("iyr"), v("pv"), v("pmt"), v("fv"), r.num("pyr"), r.flag("begin"))
    }

    // Money to a tenth of a cent, rates and periods to 1e-6.
    @Test fun tvm_solvePV() = checkAll(tvm) { near(it.num("pv"), solve(it, "pv"), 0.001) }
    @Test fun tvm_solvePMT() = checkAll(tvm) { near(it.num("pmt"), solve(it, "pmt"), 0.001) }
    @Test fun tvm_solveFV() = checkAll(tvm) { near(it.num("fv"), solve(it, "fv"), 0.001) }
    @Test fun tvm_solveN() = checkAll(tvm.filter { it.flag("nSolvable") }) { near(it.num("n"), solve(it, "n"), 1e-6) }
    @Test fun tvm_solveIYR() = checkAll(tvm.filter { it.flag("iyrSolvable") && it.num("iyr") != 0.0 }) { near(it.num("iyr"), solve(it, "iyr"), 1e-6) }

    /** Separate because 0% mostly fails as display noise (e.g. 2.31E-6) rather than as a lost solution. */
    @Test fun tvm_solveIYR_zeroRate() = checkAll(tvm.filter { it.flag("iyrSolvable") && it.num("iyr") == 0.0 }) { near(0.0, solve(it, "iyr"), 1e-6) }

    /** The subset a typical user hits: monthly loans and savings, positive rate. */
    @Test fun tvm_solveIYR_everydayLoansAndSavings() = checkAll(
        tvm.filter {
            it.flag("iyrSolvable") && it.num("pyr") == 12.0 && it.num("iyr") in 1.0..25.0 && it.num("n") in 12.0..360.0 &&
                (it.num("pv") == 0.0 || it.num("fv") == 0.0)
        },
    ) { near(it.num("iyr"), solve(it, "iyr"), 1e-6) }

    private fun flows(r: Row) = r.str("flows").split(";").map {
        val (a, c) = it.split("x")
        CashFlowEntry(a.toDouble(), c.toInt())
    }

    private val cash = Golden.load("cashflow.csv")

    @Test fun cashFlow_npv() = checkAll(cash) { near(it.num("npvAt8pct"), CashFlowEngine.npv(0.08, flows(it)), 0.001) }
    @Test fun cashFlow_irr() = checkAll(cash) { near(it.num("irr"), CashFlowEngine.irr(flows(it)), 1e-8) }

    private val bonds = Golden.load("bond.csv")

    @Test fun bond_price() = checkAll(bonds) {
        near(it.num("price"), BondEngine.price(it.num("n"), it.num("ytm"), it.num("cpn"), it.num("call"), it.flag("semi")), 1e-8)
    }

    @Test fun bond_ytm() = checkAll(bonds) {
        near(it.num("ytm"), BondEngine.ytm(it.num("n"), it.num("price"), it.num("cpn"), it.num("call"), it.flag("semi")), 1e-6)
    }

    private val dated = Golden.load("bond_dates.csv")

    private fun day(iso: String) = iso.split("-").map { it.toInt() }.let { (y, m, d) -> DateEngine.julianDay(y, m, d) }

    private fun schedule(r: Row) =
        BondEngine.schedule(day(r.str("settle")), day(r.str("maturity")), r.flag("semi"), r.flag("actual"))

    @Test fun bond_datedPrice() = checkAll(dated) {
        near(it.num("price"), BondEngine.price(schedule(it), it.num("ytm"), it.num("cpn"), it.num("call"), it.flag("semi")), 1e-8)
    }

    @Test fun bond_datedAccruedInterest() = checkAll(dated) {
        near(it.num("accrued"), BondEngine.accruedInterest(it.num("cpn"), schedule(it).accrued, it.flag("semi")), 1e-9)
    }

    // A yield moves the price of a bond about to mature by almost nothing, so check those through the price.
    @Test fun bond_datedYield() = checkAll(dated) {
        val s = schedule(it)
        val y = BondEngine.ytm(s, it.num("price"), it.num("cpn"), it.num("call"), it.flag("semi"))
        val back = BondEngine.price(s, y, it.num("cpn"), it.num("call"), it.flag("semi"))
        near(it.num("price"), back, 1e-8) ?: near(it.num("ytm"), y, if (s.periods > 1) 1e-6 else 1e-3)
    }

    // Lower tail. 12-digit machine, displayed to at most 9 places; small tails are shown in scientific
    // notation, so they also need relative accuracy.
    private val tails = Golden.load("lowertail.csv")

    private fun tail(r: Row, actual: Double) =
        near(r.num("p"), actual, 5e-10) ?: near(1.0, actual / r.num("p"), 1e-8)

    @Test fun normal_lowerTail() = checkAll(tails.filter { it.str("dist") == "z" }) {
        tail(it, ProbabilityEngine.zToP(it.num("x")))
    }

    @Test fun studentT_lowerTail() = checkAll(tails.filter { it.str("dist") == "t" }) {
        tail(it, ProbabilityEngine.tToP(it.num("x"), it.num("df")))
    }

    // Beyond x = 4 the upper tail is too close to 1 for a double to pin x down.
    private val invertible = tails.filter { it.num("x") <= 4.0 }

    @Test fun normal_inverse() = checkAll(invertible.filter { it.str("dist") == "z" }) {
        near(it.num("x"), ProbabilityEngine.pToZ(it.num("p")), 1e-9)
    }

    @Test fun studentT_inverse() = checkAll(invertible.filter { it.str("dist") == "t" }) {
        near(it.num("x"), ProbabilityEngine.pToT(it.num("p"), it.num("df")), 1e-8)
    }

    /** df = 1 is the Cauchy distribution, whose quantile is -1/tan(pi p). */
    @Test fun studentT_inverse_farTail() = checkAll(listOf(1e-3, 1e-6, 1e-9)) { p ->
        near(1.0, ProbabilityEngine.pToT(p, 1.0) / (-1 / Math.tan(Math.PI * p)), 1e-8)
    }
}
