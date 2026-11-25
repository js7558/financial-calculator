package com.js10bii.calculator.engine

import kotlin.math.*

object DateEngine {

    // Parse M.DDYYYY (MDY) or D.MMYYYY (DMY) encoded date
    // Returns Triple(year, month, day)
    fun parseDate(encoded: Double, dmy: Boolean): Triple<Int, Int, Int> {
        val intPart = encoded.toInt()
        val fracPart = ((encoded - intPart) * 1_000_000).roundToLong().toInt()
        val twoDigit = fracPart / 10_000
        val year = fracPart % 10_000
        return if (dmy) {
            Triple(year, twoDigit, intPart) // D.MMYYYY → year, month, day
        } else {
            Triple(year, intPart, twoDigit) // M.DDYYYY → year, month, day
        }
    }

    fun encodeDate(year: Int, month: Int, day: Int, dmy: Boolean): Double {
        val frac = day * 10_000 + year  // twoDigit=day, year
        return if (dmy) {
            day + (month * 10_000 + year) / 1_000_000.0
        } else {
            month + (day * 10_000 + year) / 1_000_000.0
        }
    }

    // Julian Day Number for calendar arithmetic
    fun julianDay(year: Int, month: Int, day: Int): Int {
        val a = (14 - month) / 12
        val y = year + 4800 - a
        val m = month + 12 * a - 3
        return day + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
    }

    fun fromJulianDay(jdn: Int): Triple<Int, Int, Int> {
        val a = jdn + 32044
        val b = (4 * a + 3) / 146097
        val c = a - (146097 * b) / 4
        val d = (4 * c + 3) / 1461
        val e = c - (1461 * d) / 4
        val m = (5 * e + 2) / 153
        val day = e - (153 * m + 2) / 5 + 1
        val month = m + 3 - 12 * (m / 10)
        val year = 100 * b + d - 4800 + m / 10
        return Triple(year, month, day)
    }

    private val FIRST_DAY = julianDay(1582, 10, 15)
    private val LAST_DAY = julianDay(9999, 12, 31)

    /** Parses a date that must exist and lie in the calculator's range, October 15, 1582 to December 31, 9999. */
    private fun validDate(encoded: Double, dmy: Boolean, error: String): Triple<Int, Int, Int> {
        val date = parseDate(encoded, dmy)
        val (y, m, d) = date
        if (encoded < 0 || y !in 1582..9999 || m !in 1..12 || d !in 1..31) throw ArithmeticException(error)
        val jdn = julianDay(y, m, d)
        if (jdn !in FIRST_DAY..LAST_DAY || fromJulianDay(jdn) != date) throw ArithmeticException(error)
        return date
    }

    fun daysBetweenActual(date1: Double, date2: Double, dmy: Boolean): Double {
        val (y1, m1, d1) = validDate(date1, dmy, "Error ddays")
        val (y2, m2, d2) = validDate(date2, dmy, "Error ddays")
        return (julianDay(y2, m2, d2) - julianDay(y1, m1, d1)).toDouble()
    }

    fun daysBetween360(date1: Double, date2: Double, dmy: Boolean): Double =
        days360(toJulianDay(date1, dmy, "Error ddays"), toJulianDay(date2, dmy, "Error ddays")).toDouble()

    /** Days between two Julian days on the 30/360 calendar. */
    fun days360(from: Int, to: Int): Int {
        val (y1, m1, d1raw) = fromJulianDay(from)
        val (y2, m2, d2raw) = fromJulianDay(to)
        val d1 = minOf(d1raw, 30)
        val d2 = if (d1 == 30 && d2raw >= 30) 30 else d2raw
        return (y2 - y1) * 360 + (m2 - m1) * 30 + (d2 - d1)
    }

    fun daysInMonth(year: Int, month: Int): Int =
        (if (month == 12) julianDay(year + 1, 1, 1) else julianDay(year, month + 1, 1)) - julianDay(year, month, 1)

    /** Julian day of an encoded date, which must exist and be in the calculator's range. */
    fun toJulianDay(encoded: Double, dmy: Boolean, error: String = "Error days"): Int =
        validDate(encoded, dmy, error).let { (y, m, d) -> julianDay(y, m, d) }

    fun fromJulianDayEncoded(jdn: Int, dmy: Boolean): Double =
        fromJulianDay(jdn).let { (y, m, d) -> encodeDate(y, m, d, dmy) }

    fun addDays(date: Double, days: Double, dmy: Boolean): Double {
        val (y, m, d) = validDate(date, dmy, "Error days")
        val jdn = julianDay(y, m, d) + days.toLong()
        if (jdn !in FIRST_DAY..LAST_DAY) throw ArithmeticException("Error days")
        val (y2, m2, d2) = fromJulianDay(jdn.toInt())
        return encodeDate(y2, m2, d2, dmy)
    }

    private fun Double.roundToLong(): Long = if (this >= 0) (this + 0.5).toLong() else (this - 0.5).toLong()
}
