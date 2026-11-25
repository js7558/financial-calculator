package com.js10bii.calculator.engine

import com.js10bii.calculator.viewmodel.AngleMode
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.*

object CalculatorEngine {

    fun applyOperator(a: Double, op: KeyAction, b: Double): Double = when (op) {
        KeyAction.ADD -> a + b
        KeyAction.SUBTRACT -> a - b
        KeyAction.MULTIPLY -> a * b
        KeyAction.DIVIDE -> {
            if (b == 0.0) throw ArithmeticException("Divide by 0")
            a / b
        }
        KeyAction.Y_POW_X -> {
            val power = a.pow(b)
            if (power.isNaN()) throw ArithmeticException("y^x domain error")
            power
        }
        KeyAction.N_PERMUT_R -> nPr(a, b)
        KeyAction.N_COMB_R -> nCr(a, b)
        else -> b
    }

    fun sqrt(x: Double): Double {
        if (x < 0) throw ArithmeticException("√ of negative")
        return kotlin.math.sqrt(x)
    }

    fun xSquared(x: Double) = x * x

    fun reciprocal(x: Double): Double {
        if (x == 0.0) throw ArithmeticException("1/0")
        return 1.0 / x
    }

    fun ePowX(x: Double) = kotlin.math.exp(x)

    fun ln(x: Double): Double {
        if (x <= 0) throw ArithmeticException("LN domain error")
        return kotlin.math.ln(x)
    }

    fun factorial(n: Double): Double {
        val ni = n.toInt()
        if (n < 0 || ni.toDouble() != n || ni > 253) throw ArithmeticException("n! range error")
        if (ni == 0 || ni == 1) return 1.0
        return (2..ni).fold(1.0) { acc, i -> acc * i }
    }

    fun nPr(n: Double, r: Double): Double {
        val ni = truncate(n); val ri = truncate(r)
        if (ri > ni || ri < 0 || ni < 0) throw ArithmeticException("nPr domain error")
        // Multiplies only the factors that do not cancel, so n! itself cannot overflow.
        var result = 1.0
        var k = 0.0
        while (k < ri) {
            result *= ni - k
            if (result.isInfinite()) throw ArithmeticException("nPr range error")
            k++
        }
        return result
    }

    fun nCr(n: Double, r: Double): Double {
        val ni = truncate(n); val ri = truncate(r)
        if (ri > ni || ri < 0 || ni < 0) throw ArithmeticException("nCr domain error")
        val terms = min(ri, ni - ri)
        var result = 1.0
        var k = 1.0
        while (k <= terms) {
            result = result * (ni - terms + k) / k
            if (result.isInfinite()) throw ArithmeticException("nCr range error")
            k++
        }
        return result
    }

    // Trig
    // Degrees are reduced before conversion (% is exact); radians of large angles are not.
    fun sin(x: Double, mode: AngleMode): Double =
        if (mode == AngleMode.DEG && x % 180.0 == 0.0) 0.0 else cleanTrig(kotlin.math.sin(toRad(x, mode)))

    fun cos(x: Double, mode: AngleMode): Double =
        if (mode == AngleMode.DEG && isOddQuarterTurn(x)) 0.0 else cleanTrig(kotlin.math.cos(toRad(x, mode)))

    fun tan(x: Double, mode: AngleMode): Double {
        if (mode == AngleMode.DEG) {
            if (isOddQuarterTurn(x)) throw ArithmeticException("TAN domain error")
            if (x % 180.0 == 0.0) return 0.0
        }
        return cleanTrig(kotlin.math.tan(toRad(x, mode)))
    }

    fun asin(x: Double, mode: AngleMode): Double {
        if (abs(x) > 1.0) throw ArithmeticException("ASIN domain error")
        return fromRad(kotlin.math.asin(x), mode)
    }

    fun acos(x: Double, mode: AngleMode): Double {
        if (abs(x) > 1.0) throw ArithmeticException("ACOS domain error")
        return fromRad(kotlin.math.acos(x), mode)
    }

    fun atan(x: Double, mode: AngleMode) = fromRad(kotlin.math.atan(x), mode)

    private fun isOddQuarterTurn(degrees: Double) = abs(degrees % 180.0) == 90.0

    private fun cleanTrig(x: Double) = when {
        abs(x) < 1e-15 -> 0.0
        abs(x - 1.0) < 1e-15 -> 1.0
        abs(x + 1.0) < 1e-15 -> -1.0
        else -> x
    }

    // Hyperbolic
    fun sinh(x: Double) = kotlin.math.sinh(x)
    fun cosh(x: Double) = kotlin.math.cosh(x)
    fun tanh(x: Double) = kotlin.math.tanh(x)
    fun asinh(x: Double) = kotlin.math.asinh(x)
    fun acosh(x: Double): Double {
        if (x < 1) throw ArithmeticException("acosh domain error")
        return ln(x + sqrt(x * x - 1))
    }
    fun atanh(x: Double): Double {
        if (x <= -1 || x >= 1) throw ArithmeticException("atanh domain error")
        return 0.5 * ln((1 + x) / (1 - x))
    }

    private fun toRad(x: Double, mode: AngleMode) =
        if (mode == AngleMode.DEG) Math.toRadians(x % 360.0) else x

    private fun fromRad(x: Double, mode: AngleMode) =
        if (mode == AngleMode.DEG) Math.toDegrees(x) else x

    /** Up to 12 digits: from 10 billion, whole digits take the place of decimals, and 1 trillion is scientific. */
    fun formatDisplay(value: Double, decimalPlaces: Int): String {
        if (value.isNaN() || value.isInfinite()) return "Error"
        val dp = decimalPlaces.coerceIn(0, 9)
        val abs = abs(value)
        return if (abs(toTwelveDigits(value)) >= 1e12 || (abs > 0.0 && abs < 0.001)) {
            formatScientific(value, dp)
        } else {
            formatFixed(value, dp)
        }
    }

    private val TWELVE_DIGITS = MathContext(12, RoundingMode.HALF_UP)

    /** The calculator works with 12-digit numbers; this also removes binary noise such as 0.1 + 0.2. */
    fun toTwelveDigits(value: Double): Double =
        if (value.isNaN() || value.isInfinite()) value else BigDecimal(value).round(TWELVE_DIGITS).toDouble()

    private fun rounded(value: Double, decimalPlaces: Int): BigDecimal =
        BigDecimal(value).round(TWELVE_DIGITS).setScale(decimalPlaces, RoundingMode.HALF_UP)

    /** Rounds half away from zero, exactly as the display does. */
    fun roundToPlaces(value: Double, decimalPlaces: Int): Double = rounded(value, decimalPlaces).toDouble()

    private fun formatFixed(value: Double, dp: Int): String {
        val number = BigDecimal(value).round(TWELVE_DIGITS)
        val wholeDigits = (number.precision() - number.scale()).coerceAtLeast(1)
        val places = minOf(dp, 12 - wholeDigits)
        val pattern = if (places == 0) "#,##0" else "#,##0.${"0".repeat(places)}"
        return DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).format(rounded(value, places))
    }

    private fun formatScientific(value: Double, dp: Int): String {
        val formatted = "%.${dp}E".format(Locale.US, value)
        // Normalize exponent: E+06 → E06, E-06 → E-06
        return formatted.replace(Regex("E\\+0*(\\d+)"), "E$1")
            .replace(Regex("E-0*(\\d+)"), "E-$1")
    }
}
