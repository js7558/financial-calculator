package com.js10bii.calculator.engine

import kotlin.math.*

object TvmEngine {

    fun solve(
        n: Double?,
        iyr: Double?,
        pv: Double?,
        pmt: Double?,
        fv: Double?,
        pyr: Double,
        begin: Boolean,
    ): Double {
        val unknownCount = listOf(n, iyr, pv, pmt, fv).count { it == null }
        if (unknownCount != 1) throw ArithmeticException("Exactly one TVM variable must be unknown")
        return when {
            n   == null -> solveN(iyr!!, pv!!, pmt!!, fv!!, pyr, begin)
            iyr == null -> solveIYR(n, pv!!, pmt!!, fv!!, pyr, begin)
            pv  == null -> solvePV(n, iyr, pmt!!, fv!!, pyr, begin)
            pmt == null -> solvePMT(n, iyr, pv!!, fv!!, pyr, begin)
            else        -> solveFV(n, iyr, pv!!, pmt, pyr, begin)
        }
    }

    private fun periodicRate(iyr: Double, pyr: Double) = iyr / pyr / 100.0

    // TVM equation: PV*(1+i)^n + PMT*(1+i*b)*((1+i)^n - 1)/i + FV = 0
    // b = 1 for BEGIN, 0 for END

    private fun solveN(iyr: Double, pv: Double, pmt: Double, fv: Double, pyr: Double, begin: Boolean): Double {
        val i = periodicRate(iyr, pyr)
        val b = if (begin) 1.0 else 0.0
        if (abs(i) < 1e-12) {
            if (abs(pmt) < 1e-12) throw ArithmeticException("Cannot solve N: i=0 and PMT=0")
            return -(pv + fv) / pmt
        }
        val num = pmt * (1.0 + i * b) - fv * i
        val den = pv * i + pmt * (1.0 + i * b)
        if (den == 0.0 || (num / den) <= 0)
            throw ArithmeticException("No valid solution for N")
        return ln(num / den) / ln(1.0 + i)
    }

    private fun solveIYR(n: Double, pv: Double, pmt: Double, fv: Double, pyr: Double, begin: Boolean): Double {
        if (n <= 0.0) throw ArithmeticException("N must be > 0")
        val rate = RateSolver.solve(
            listOf(
                RateSolver.Level(pv, start = 0.0, count = 1.0),
                RateSolver.Level(pmt, start = if (begin) 0.0 else 1.0, count = n),
                RateSolver.Level(fv, start = n, count = 1.0),
            ),
        )
        return rate * pyr * 100.0
    }

    private fun solvePV(n: Double, iyr: Double, pmt: Double, fv: Double, pyr: Double, begin: Boolean): Double {
        val i = periodicRate(iyr, pyr)
        val b = if (begin) 1.0 else 0.0
        val factor = (1.0 + i).pow(n)
        val pmtFactor = if (abs(i) < 1e-12) n else (factor - 1.0) / i
        return -(pmt * (1.0 + i * b) * pmtFactor + fv) / factor
    }

    private fun solvePMT(n: Double, iyr: Double, pv: Double, fv: Double, pyr: Double, begin: Boolean): Double {
        val i = periodicRate(iyr, pyr)
        val b = if (begin) 1.0 else 0.0
        val factor = (1.0 + i).pow(n)
        val pmtFactor = if (abs(i) < 1e-12) n else (factor - 1.0) / i
        val denom = pmtFactor * (1.0 + i * b)
        if (abs(denom) < 1e-15) throw ArithmeticException("Cannot solve PMT")
        return -(pv * factor + fv) / denom
    }

    private fun solveFV(n: Double, iyr: Double, pv: Double, pmt: Double, pyr: Double, begin: Boolean): Double {
        val i = periodicRate(iyr, pyr)
        val b = if (begin) 1.0 else 0.0
        val factor = (1.0 + i).pow(n)
        val pmtFactor = if (abs(i) < 1e-12) n else (factor - 1.0) / i
        return -(pv * factor + pmt * (1.0 + i * b) * pmtFactor)
    }

    // Amortize range [startPeriod, endPeriod] and return (principal, interest, balance)
    fun amortize(
        startPeriod: Int,
        endPeriod: Int,
        pv: Double,
        iyr: Double,
        pmt: Double,
        pyr: Double,
        begin: Boolean,
    ): Triple<Double, Double, Double> {
        val i = periodicRate(iyr, pyr)
        var balance = pv
        var totalPrin = 0.0
        var totalInt = 0.0
        for (p in 1..endPeriod) {
            // In BEGIN mode the first payment is made before any interest has accrued
            val interest = if (begin && p == 1) 0.0 else balance * i
            val principalPaid = -(pmt + interest)
            balance -= principalPaid
            if (p >= startPeriod) {
                totalInt += interest
                totalPrin += principalPaid
            }
        }
        return Triple(-totalPrin, -totalInt, balance)
    }

    fun nomToEff(nom: Double, pyr: Double): Double {
        val i = nom / 100.0 / pyr
        return ((1.0 + i).pow(pyr) - 1.0) * 100.0
    }

    fun effToNom(eff: Double, pyr: Double): Double {
        val factor = (1.0 + eff / 100.0).pow(1.0 / pyr)
        return (factor - 1.0) * pyr * 100.0
    }
}
