package com.js10bii.calculator.engine

import kotlin.math.*

data class CashFlowEntry(val amount: Double, val count: Int = 1)

object CashFlowEngine {

    fun npv(periodicRate: Double, flows: List<CashFlowEntry>): Double {
        var result = 0.0
        var period = 0
        for (entry in flows) {
            repeat(entry.count) {
                val discount = if (period == 0) 1.0 else (1.0 + periodicRate).pow(period.toDouble())
                result += entry.amount / discount
                period++
            }
        }
        return result
    }

    fun irr(flows: List<CashFlowEntry>): Double {
        if (flows.isEmpty()) throw ArithmeticException("No cash flows")
        var period = 0.0
        val levels = flows.map { f ->
            RateSolver.Level(f.amount, period, f.count.toDouble()).also { period += f.count }
        }
        return RateSolver.solve(levels)
    }
}
