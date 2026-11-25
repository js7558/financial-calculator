package com.js10bii.calculator.ui

import com.js10bii.calculator.engine.CalculatorEngine
import com.js10bii.calculator.engine.KeyAction
import com.js10bii.calculator.viewmodel.*
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/** Text of the strip under the keypad. People copy figures from it, so amounts always show full cents. */
internal object StatusLines {

    /** Settings that change answers, always shown because C ALL keeps them, then N and I/YR. */
    fun modes(s: CalculatorState): String = listOfNotNull(
        "P/YR=${number(s.tvmPYR)}",
        if (s.paymentTiming == PaymentTiming.BEGIN) "BEGIN" else "END",
        if (s.calcMode == CalcMode.CHAIN) "CHAIN" else "ALG",
        if (s.angleMode == AngleMode.DEG) "DEG" else "RAD",
        s.tvmN?.let { "N=${number(it)}" },
        s.tvmIYR?.let { "I/YR=${number(it)}" },
    ).joinToString("  ")

    fun values(s: CalculatorState): String {
        if (s.outer.isNotEmpty()) return expression(s)
        if (s.amortPhase != AmortPhase.NONE) {
            val range = if (s.amortStart == s.amortEnd) "${s.amortStart}" else "${s.amortStart}–${s.amortEnd}"
            return "AMORT $range  PRIN=${money(s.amortPrin)}  INT=${money(s.amortInt)}  BAL=${money(s.amortBal)}"
        }
        val tvm = listOfNotNull(
            s.tvmPV?.let { "PV=${money(it)}" },
            s.tvmPMT?.let { "PMT=${money(it)}" },
            s.tvmFV?.let { "FV=${money(it)}" },
        )
        if (tvm.isNotEmpty()) return tvm.joinToString("  ")
        if (s.kOperator != null) return "K: ${symbol(s.kOperator)} ${number(s.kOperand)}${if (s.kIsPercent) "%" else ""}"
        if (s.pendingOperator != null) return expression(s)
        return listOfNotNull(
            s.cstValue?.let { "CST=${money(it)}" },
            s.prcValue?.let { "PRC=${money(it)}" },
            s.muValue?.let { "MU=${number(it)}" },
            s.marValue?.let { "MAR=${number(it)}" },
        ).joinToString("  ")
    }

    private fun expression(s: CalculatorState): String = buildString {
        s.outer.forEach { append(it.op?.let { op -> "${number(it.operand)} ${symbol(op)} " } ?: "( ") }
        s.pendingOperator?.let { append("${number(s.operandA)} ${symbol(it)} ") }
        if (s.isEditing) append(s.displayText)
    }.trimEnd()

    private fun money(v: Double) = format(v, "#,##0.00", 2)

    /** Rates, counts and operands: whole numbers stay whole, and dates keep their six places. */
    private fun number(v: Double) = format(v, "#,##0.######", 6)

    private fun format(v: Double, pattern: String, places: Int): String {
        val tooSmall = v != 0.0 && abs(v) < 1e-6 && places > 2
        if (abs(v) >= 1e12 || tooSmall) return CalculatorEngine.formatDisplay(v, places)
        val rounded = BigDecimal(v).round(MathContext(12)).setScale(places, RoundingMode.HALF_UP)
        return DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).format(rounded)
    }

    private fun symbol(op: KeyAction) = when (op) {
        KeyAction.ADD -> "+"
        KeyAction.SUBTRACT -> "−"
        KeyAction.MULTIPLY -> "×"
        KeyAction.DIVIDE -> "÷"
        KeyAction.Y_POW_X -> "y^x"
        KeyAction.N_PERMUT_R -> "nPr"
        KeyAction.N_COMB_R -> "nCr"
        KeyAction.PCT_CHG -> "%CHG"
        KeyAction.DATE_CALC -> "DATE"
        KeyAction.DATE_DELTA_DAYS -> "ΔDAYS"
        KeyAction.DF_T_TO_P -> "df,t→P"
        KeyAction.DF_P_TO_T -> "df,P→t"
        else -> "?"
    }
}
