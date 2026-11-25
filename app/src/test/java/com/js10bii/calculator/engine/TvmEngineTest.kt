package com.js10bii.calculator.engine

import org.junit.Assert.assertEquals
import org.junit.Test

/** Examples drawn from the JS 10bII+ manual (c02989763.pdf). */
class TvmEngineTest {

    private fun assertApprox(expected: Double, actual: Double, tol: Double = 0.005) {
        assertEquals("expected $expected but got $actual", expected, actual, tol)
    }

    // ── Solve FV ─────────────────────────────────────────────────────────────

    /** Manual ch.2 basic TVM: N=10 annual, 5% annual, PV=-1000, PMT=-100, P/YR=1 → FV=2886.68 */
    @Test fun solveFV_annual() {
        val fv = TvmEngine.solve(n=10.0, iyr=5.0, pv=-1000.0, pmt=-100.0, fv=null, pyr=1.0, begin=false)
        assertApprox(2886.68, fv)
    }

    /** Monthly compounding: same inputs, P/YR=12 → FV=2061.42 */
    @Test fun solveFV_monthly() {
        val fv = TvmEngine.solve(n=10.0, iyr=5.0, pv=-1000.0, pmt=-100.0, fv=null, pyr=12.0, begin=false)
        assertApprox(2061.42, fv)
    }

    // ── Solve PMT ────────────────────────────────────────────────────────────

    /** Manual mortgage example: 30-yr loan $14,000 @ 10% monthly → PMT=-122.86 */
    @Test fun solvePMT_mortgage() {
        val pmt = TvmEngine.solve(n=360.0, iyr=10.0, pv=14000.0, pmt=null, fv=0.0, pyr=12.0, begin=false)
        assertApprox(-122.86, pmt)
    }

    /** Save $200/month for 5 years @ 10% annual → FV≈15,488 */
    @Test fun solveFV_savings() {
        val fv = TvmEngine.solve(n=60.0, iyr=10.0, pv=0.0, pmt=-200.0, fv=null, pyr=12.0, begin=false)
        assertApprox(15488.0, fv, tol=1.0)
    }

    // ── Solve N ──────────────────────────────────────────────────────────────

    /** Round-trip: solve FV then solve back for N → 10 */
    @Test fun solveN_roundTrip_annual() {
        val fv = TvmEngine.solve(n=10.0,  iyr=5.0, pv=-1000.0, pmt=-100.0, fv=null, pyr=1.0, begin=false)
        val n  = TvmEngine.solve(n=null,  iyr=5.0, pv=-1000.0, pmt=-100.0, fv=fv,   pyr=1.0, begin=false)
        assertApprox(10.0, n, tol=0.01)
    }

    @Test fun solveN_roundTrip_monthly() {
        val pmt = TvmEngine.solve(n=360.0, iyr=10.0, pv=14000.0, pmt=null, fv=0.0, pyr=12.0, begin=false)
        val n   = TvmEngine.solve(n=null,  iyr=10.0, pv=14000.0, pmt=pmt,  fv=0.0, pyr=12.0, begin=false)
        assertApprox(360.0, n, tol=0.5)
    }

    // ── Solve PV ─────────────────────────────────────────────────────────────

    /** Solve PV: what lump sum today equals $1000/month for 10 years @ 8%? */
    @Test fun solvePV_annuity() {
        val pv = TvmEngine.solve(n=120.0, iyr=8.0, pv=null, pmt=-1000.0, fv=0.0, pyr=12.0, begin=false)
        assertApprox(82421.48, pv)
    }

    // ── Solve I/YR ───────────────────────────────────────────────────────────

    /** Solve I/YR: mortgage $14,000, 360 payments of $122.86 → 10% */
    @Test fun solveIYR_mortgage() {
        val pmt = TvmEngine.solve(n=360.0, iyr=10.0, pv=14000.0, pmt=null, fv=0.0, pyr=12.0, begin=false)
        val iyr = TvmEngine.solve(n=360.0, iyr=null, pv=14000.0, pmt=pmt,  fv=0.0, pyr=12.0, begin=false)
        assertApprox(10.0, iyr, tol=0.01)
    }

    // ── BEGIN mode ───────────────────────────────────────────────────────────

    /** BEGIN mode PMT is slightly higher (payment at start of period) */
    @Test fun solvePMT_beginMode() {
        val pmtEnd   = TvmEngine.solve(n=360.0, iyr=10.0, pv=14000.0, pmt=null, fv=0.0, pyr=12.0, begin=false)
        val pmtBegin = TvmEngine.solve(n=360.0, iyr=10.0, pv=14000.0, pmt=null, fv=0.0, pyr=12.0, begin=true)
        // Begin-mode payment magnitude is slightly smaller (paid earlier)
        assert(Math.abs(pmtBegin) < Math.abs(pmtEnd)) {
            "BEGIN pmt=${pmtBegin} should be smaller magnitude than END pmt=${pmtEnd}"
        }
    }

    // ── Interest rate conversion ──────────────────────────────────────────────

    /** Manual example: 10% nominal, 12 P/YR → 10.47% effective */
    @Test fun nomToEff_12pyr() {
        assertApprox(10.47, TvmEngine.nomToEff(10.0, 12.0))
    }

    /** Reverse: 10.47% effective, 12 P/YR → ~10% nominal */
    @Test fun effToNom_12pyr() {
        assertApprox(10.0, TvmEngine.effToNom(10.47, 12.0), tol=0.01)
    }

    // ── Amortization ─────────────────────────────────────────────────────────

    /**
     * Mortgage $14,000 @ 10%, 360 payments.
     * Period 1: mostly interest. INT≈-116.67, PRIN≈-6.19, BAL≈13993.81
     */
    @Test fun amortize_period1() {
        val pmt = TvmEngine.solve(n=360.0, iyr=10.0, pv=14000.0, pmt=null, fv=0.0, pyr=12.0, begin=false)
        val (prin, int, bal) = TvmEngine.amortize(1, 1, 14000.0, 10.0, pmt, 12.0, false)
        assertApprox(-116.67, int,  tol=0.05)
        assert(prin < 0.0 && prin > -30.0) { "PRIN should be small negative, got $prin" }
        assertApprox(14000.0 + prin, bal, tol=0.05)   // BAL = PV + PRIN
        // PRIN + INT = PMT (round-trip identity)
        assertApprox(pmt, prin + int, tol=0.01)
    }

    /**
     * Manual Table 1-15: period 20 → PRIN=-7.25, INT=-115.61, BAL=13865.83
     */
    @Test fun amortize_period20() {
        val pmt = TvmEngine.solve(n=360.0, iyr=10.0, pv=14000.0, pmt=null, fv=0.0, pyr=12.0, begin=false)
        val (prin, int, bal) = TvmEngine.amortize(20, 20, 14000.0, 10.0, pmt, 12.0, false)
        assertApprox(-7.25,     prin, tol=0.10)
        assertApprox(-115.61,   int,  tol=0.10)
        assertApprox(13865.83,  bal,  tol=1.0)
        assertApprox(pmt, prin + int, tol=0.01)
    }
}
