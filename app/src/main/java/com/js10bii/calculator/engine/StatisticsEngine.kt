package com.js10bii.calculator.engine

import kotlin.math.*

object StatisticsEngine {

    fun mean(n: Double, sum: Double): Double {
        if (n < 1) throw ArithmeticException("No data")
        return sum / n
    }

    fun sampleStdDev(n: Double, sumX: Double, sumX2: Double): Double {
        if (n < 2) throw ArithmeticException("Need ≥ 2 data points")
        return sqrt(squaredDeviations(n, sumX, sumX2) / (n - 1))
    }

    fun popStdDev(n: Double, sumX: Double, sumX2: Double): Double {
        if (n < 1) throw ArithmeticException("No data")
        return sqrt(squaredDeviations(n, sumX, sumX2) / n)
    }

    /** Sum of squared deviations; a difference no larger than the sums' own rounding error is zero. */
    private fun squaredDeviations(n: Double, sumX: Double, sumX2: Double): Double {
        val difference = sumX2 - sumX * sumX / n
        return if (difference <= sumX2 * n * Math.ulp(1.0)) 0.0 else difference
    }

    fun slope(n: Double, sumX: Double, sumY: Double, sumX2: Double, sumXY: Double): Double {
        val denom = n * sumX2 - sumX * sumX
        if (abs(denom) < 1e-15) throw ArithmeticException("Cannot compute slope")
        return (n * sumXY - sumX * sumY) / denom
    }

    fun intercept(n: Double, sumX: Double, sumY: Double, sumX2: Double, sumXY: Double): Double {
        val m = slope(n, sumX, sumY, sumX2, sumXY)
        return (sumY - m * sumX) / n
    }

    fun correlation(n: Double, sumX: Double, sumY: Double, sumX2: Double, sumY2: Double, sumXY: Double): Double {
        val num = n * sumXY - sumX * sumY
        val den = sqrt((n * sumX2 - sumX * sumX) * (n * sumY2 - sumY * sumY))
        if (abs(den) < 1e-15) throw ArithmeticException("Cannot compute correlation")
        return num / den
    }

    fun predictY(x: Double, n: Double, sumX: Double, sumY: Double, sumX2: Double, sumXY: Double): Double {
        val m = slope(n, sumX, sumY, sumX2, sumXY)
        val b = intercept(n, sumX, sumY, sumX2, sumXY)
        return m * x + b
    }

    fun predictX(y: Double, n: Double, sumX: Double, sumY: Double, sumX2: Double, sumXY: Double): Double {
        val m = slope(n, sumX, sumY, sumX2, sumXY)
        val b = intercept(n, sumX, sumY, sumX2, sumXY)
        if (abs(m) < 1e-15) throw ArithmeticException("Slope is zero")
        return (y - b) / m
    }

    fun weightedMean(sumXY: Double, sumY: Double): Double {
        if (abs(sumY) < 1e-15) throw ArithmeticException("Sum of weights is zero")
        return sumXY / sumY
    }
}
