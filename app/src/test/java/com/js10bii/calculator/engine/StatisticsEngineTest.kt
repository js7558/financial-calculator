package com.js10bii.calculator.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class StatisticsEngineTest {

    private fun assertClose(expected: Double, actual: Double, tol: Double = 0.005) {
        assertTrue("Expected $expected but was $actual", abs(expected - actual) <= tol)
    }

    // HP 10bII+ manual example: x-data = {6,3,8}, y-data = {4,2,5}
    private val n = 3.0
    private val sumX = 17.0   // 6+3+8
    private val sumY = 11.0   // 4+2+5
    private val sumX2 = 109.0 // 36+9+64
    private val sumY2 = 45.0  // 16+4+25
    private val sumXY = 70.0  // 24+6+40

    @Test fun meanX() = assertClose(17.0 / 3.0, StatisticsEngine.mean(n, sumX))
    @Test fun meanY() = assertClose(11.0 / 3.0, StatisticsEngine.mean(n, sumY))

    @Test fun sampleStdDevX() {
        // Sx = sqrt((109 - 17^2/3) / 2) = sqrt((109 - 96.333...) / 2) = sqrt(6.333..) = 2.517
        assertClose(2.517, StatisticsEngine.sampleStdDev(n, sumX, sumX2), 0.01)
    }

    @Test fun sampleStdDevY() {
        // Sy = sqrt((45 - 11^2/3) / 2) = sqrt((45 - 40.333) / 2) = sqrt(2.333) = 1.528
        assertClose(1.528, StatisticsEngine.sampleStdDev(n, sumY, sumY2), 0.01)
    }

    @Test fun popStdDevX() {
        // σx = sqrt((109 - 17^2/3) / 3) = sqrt(12.667/3) = sqrt(4.222) = 2.055
        assertClose(2.055, StatisticsEngine.popStdDev(n, sumX, sumX2), 0.01)
    }

    @Test fun correlation() {
        // r = (3*70 - 17*11) / sqrt((3*109-17^2)*(3*45-11^2))
        //   = (210 - 187) / sqrt((327-289)*(135-121)) = 23 / sqrt(38*14) = 23/23.07 ≈ 0.9970
        assertClose(0.997, StatisticsEngine.correlation(n, sumX, sumY, sumX2, sumY2, sumXY), 0.01)
    }

    @Test fun slope() {
        // m = (3*70 - 17*11) / (3*109 - 17^2) = 23/38 ≈ 0.6053
        assertClose(0.6053, StatisticsEngine.slope(n, sumX, sumY, sumX2, sumXY), 0.01)
    }

    @Test fun intercept() {
        // b = (11 - m*17) / 3, m≈0.6053 → b = (11-10.29)/3 ≈ 0.237
        assertClose(0.237, StatisticsEngine.intercept(n, sumX, sumY, sumX2, sumXY), 0.01)
    }

    @Test fun predictY() {
        // ŷ for x=5: 0.6053*5 + 0.237 = 3.264
        assertClose(3.264, StatisticsEngine.predictY(5.0, n, sumX, sumY, sumX2, sumXY), 0.01)
    }

    @Test fun predictX() {
        // x̂ for y=3.264: (3.264 - 0.237) / 0.6053 = 5.0
        assertClose(5.0, StatisticsEngine.predictX(3.264, n, sumX, sumY, sumX2, sumXY), 0.05)
    }

    @Test fun weightedMean() {
        // Σxy=70, Σy=11 → weighted mean = 70/11 ≈ 6.364
        assertClose(70.0 / 11.0, StatisticsEngine.weightedMean(sumXY, sumY), 0.01)
    }
}
