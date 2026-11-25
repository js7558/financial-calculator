package com.js10bii.calculator.engine

import kotlin.math.*

object DepreciationEngine {

    // Returns (depreciation for year, accumulated depreciation, remaining book value)
    fun straightLine(cost: Double, salvage: Double, life: Int, year: Int): Triple<Double, Double, Double> {
        requireLife(life)
        if (year > life) return Triple(0.0, cost - salvage, salvage)
        val annual = (cost - salvage) / life
        val acc = annual * year
        return Triple(annual, acc, cost - acc)
    }

    fun soyd(cost: Double, salvage: Double, life: Int, year: Int): Triple<Double, Double, Double> {
        requireLife(life)
        if (year > life) return Triple(0.0, cost - salvage, salvage)
        val syd = life * (life + 1).toDouble() / 2.0
        var acc = 0.0
        for (k in 1 until year) acc += (life - k + 1).toDouble() / syd * (cost - salvage)
        val depr = (life - year + 1).toDouble() / syd * (cost - salvage)
        acc += depr
        return Triple(depr, acc, cost - acc)
    }

    /** Plain declining balance: no switch to straight line, and the last year takes what remains above salvage. */
    fun decliningBalance(cost: Double, salvage: Double, life: Int, year: Int, factor: Double = 2.0): Triple<Double, Double, Double> {
        requireLife(life)
        val floor = min(cost, salvage)
        val rate = min(factor / life, 1.0)
        fun bookValue(y: Int) = if (y >= life) floor else max(cost * (1.0 - rate).pow(y), floor)
        val before = bookValue(year - 1)
        val after = bookValue(year)
        return Triple(before - after, cost - after, after)
    }

    private fun requireLife(life: Int) {
        if (life < 1) throw ArithmeticException("N must be >0")
    }
}
