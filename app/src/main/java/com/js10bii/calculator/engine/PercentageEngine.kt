package com.js10bii.calculator.engine

object PercentageEngine {

    // HP-style: base * pct/100 (e.g., 17.50 + 15% = 17.50 + 2.625 = 20.125)
    fun percentOf(base: Double, pct: Double) = base * pct / 100.0

    // (new - old) / old * 100
    fun percentChange(old: Double, newVal: Double): Double {
        if (old == 0.0) throw ArithmeticException("%CHG: base is 0")
        return (newVal - old) / old * 100.0
    }

    // Markup: (price - cost) / cost * 100
    fun markup(cost: Double, price: Double): Double {
        if (cost == 0.0) throw ArithmeticException("MU: cost is 0")
        return (price - cost) / cost * 100.0
    }

    fun priceFromMarkup(cost: Double, mu: Double) = cost * (1.0 + mu / 100.0)

    fun costFromMarkup(price: Double, mu: Double): Double {
        if (mu == -100.0) throw ArithmeticException("MU: markup = -100%")
        return price / (1.0 + mu / 100.0)
    }

    // Margin: (price - cost) / price * 100
    fun margin(cost: Double, price: Double): Double {
        if (price == 0.0) throw ArithmeticException("MAR: price is 0")
        return (price - cost) / price * 100.0
    }

    fun priceFromMargin(cost: Double, mar: Double): Double {
        if (mar >= 100.0) throw ArithmeticException("MAR: margin >= 100%")
        return cost / (1.0 - mar / 100.0)
    }

    fun costFromMargin(price: Double, mar: Double) = price * (1.0 - mar / 100.0)
}
