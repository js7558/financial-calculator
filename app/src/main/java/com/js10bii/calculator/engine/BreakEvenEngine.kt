package com.js10bii.calculator.engine

import kotlin.math.*

object BreakEvenEngine {

    fun solveUnits(fc: Double, sp: Double, vc: Double, profit: Double = 0.0): Double {
        val contrib = sp - vc
        if (abs(contrib) < 1e-15) throw ArithmeticException("SP = VC: no break-even")
        return (fc + profit) / contrib
    }

    fun solveProfit(units: Double, fc: Double, sp: Double, vc: Double): Double =
        units * (sp - vc) - fc

    fun solveSP(units: Double, fc: Double, vc: Double, profit: Double = 0.0): Double {
        if (abs(units) < 1e-15) throw ArithmeticException("Units cannot be zero")
        return vc + (fc + profit) / units
    }

    fun solveVC(units: Double, fc: Double, sp: Double, profit: Double = 0.0): Double {
        if (abs(units) < 1e-15) throw ArithmeticException("Units cannot be zero")
        return sp - (fc + profit) / units
    }

    fun solveFC(units: Double, sp: Double, vc: Double, profit: Double = 0.0): Double =
        units * (sp - vc) - profit
}
