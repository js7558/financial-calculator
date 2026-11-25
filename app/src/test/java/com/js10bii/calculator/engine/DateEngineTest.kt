package com.js10bii.calculator.engine

import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class DateEngineTest {

    private fun assertClose(expected: Double, actual: Double, tol: Double = 0.5) {
        assertTrue("Expected $expected but was $actual", abs(expected - actual) <= tol)
    }

    // HP 10bII+ manual: date 9.062004 in M/D/Y mode = September 6, 2004
    @Test fun parseDate_MDY() {
        val (y, m, d) = DateEngine.parseDate(9.062004, false)
        assertEquals(2004, y)
        assertEquals(9, m)
        assertEquals(6, d)
    }

    // D/M/Y mode: 6.092004 = 6 September 2004
    @Test fun parseDate_DMY() {
        val (y, m, d) = DateEngine.parseDate(6.092004, true)
        assertEquals(2004, y)
        assertEquals(9, m)
        assertEquals(6, d)
    }

    // Days between Jan 1 2000 and Jan 1 2001 (actual) = 366 (2000 is leap year)
    @Test fun daysBetweenActual_leapYear() {
        val d1 = 1.012000  // Jan 1, 2000
        val d2 = 1.012001  // Jan 1, 2001
        assertClose(366.0, DateEngine.daysBetweenActual(d1, d2, false))
    }

    // 30/360: Jan 1 to Jul 1 = 6*30 = 180 days
    @Test fun daysBetween360() {
        val d1 = 1.012000   // Jan 1, 2000
        val d2 = 7.012000   // Jul 1, 2000
        assertClose(180.0, DateEngine.daysBetween360(d1, d2, false))
    }

    // HP manual example: Jan 1 2000 + 100 days = April 10, 2000
    @Test fun addDays_crossMonth() {
        val d1 = 1.012000  // Jan 1, 2000
        val result = DateEngine.addDays(d1, 100.0, false)
        val (y, m, d) = DateEngine.parseDate(result, false)
        assertEquals(2000, y)
        assertEquals(4, m)
        assertEquals(10, d)
    }

    // Julian day round-trip
    @Test fun julianRoundTrip() {
        val jdn = DateEngine.julianDay(2024, 6, 15)
        val (y, m, d) = DateEngine.fromJulianDay(jdn)
        assertEquals(2024, y); assertEquals(6, m); assertEquals(15, d)
    }
}
