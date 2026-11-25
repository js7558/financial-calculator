package com.js10bii.calculator.viewmodel

import com.js10bii.calculator.engine.CashFlowEntry
import com.js10bii.calculator.engine.KeyAction

enum class ShiftState { NONE, ORANGE, BLUE }
enum class AngleMode { DEG, RAD }
enum class CalcMode { CHAIN, ALGEBRAIC }
enum class PaymentTiming { END, BEGIN }
enum class AmortPhase { NONE, RANGE, PRIN, INT, BAL }
enum class DeprPhase { NONE, DEPR, REM, ACC }
enum class AwaitingInput { NONE, STO_REGISTER, RCL_REGISTER, DISP_PLACES, CMEM_TARGET }

/** What waits behind the innermost pending operation: another `operand op`, or an open parenthesis (op is null). */
data class Outer(val operand: Double = 0.0, val op: KeyAction? = null)

data class CalculatorState(
    // Display
    // displayText is only what is shown; x is the number behind it and the operand for every key.
    val displayText: String = "0.00",
    val x: Double = 0.0,
    val isEditing: Boolean = false,
    val displayDecimalPlaces: Int = 2,

    // Shift / modifier state
    val shiftState: ShiftState = ShiftState.NONE,
    val isInvActive: Boolean = false,
    val isHypActive: Boolean = false,
    val awaitingInput: AwaitingInput = AwaitingInput.NONE,

    // Calculator modes
    val angleMode: AngleMode = AngleMode.DEG,
    val calcMode: CalcMode = CalcMode.CHAIN,

    // Arithmetic engine
    val pendingOperator: KeyAction? = null,
    val operandA: Double = 0.0,
    // Operations and open parentheses behind operandA/pendingOperator, outermost first
    val outer: List<Outer> = emptyList(),
    // True right after ")", so that "(" multiplies
    val parenClosed: Boolean = false,
    // True right after an operator or INPUT, until the second number is typed, recalled or calculated
    val awaitingOperand: Boolean = false,

    // K constant
    val kOperator: KeyAction? = null,
    val kOperand: Double = 0.0,
    val kIsPercent: Boolean = false,

    // The percentage behind x right after "+ n %" or "- n %", so that K can store it as a percentage
    val percentOperand: Double? = null,

    // Memory
    val memoryM: Double = 0.0,
    val registers: List<Double> = List(20) { 0.0 },
    val awaitingTens: Boolean = false, // STO/RCL . typed, for registers 10 to 19

    // TVM
    val tvmN: Double? = null,
    val tvmIYR: Double? = null,
    val tvmPV: Double? = null,
    val tvmPMT: Double? = null,
    val tvmFV: Double? = null,
    val tvmPYR: Double = 12.0,
    val tvmEffRate: Double? = null, // the nominal rate is tvmIYR
    val paymentTiming: PaymentTiming = PaymentTiming.END,

    // Amortization
    val amortStart: Int = 1,
    val amortEnd: Int = 1,
    val amortPhase: AmortPhase = AmortPhase.NONE,
    val amortPrin: Double = 0.0,
    val amortInt: Double = 0.0,
    val amortBal: Double = 0.0,

    // Business % scratchpad
    val cstValue: Double? = null,
    val prcValue: Double? = null,
    val muValue: Double? = null,
    val marValue: Double? = null,
    val rateIsMargin: Boolean = false, // whether MAR or MU was set last, and so sets CST and PRC

    // Statistics accumulators
    val statN: Double = 0.0,
    val statSumX: Double = 0.0,
    val statSumY: Double = 0.0,
    val statSumX2: Double = 0.0,
    val statSumY2: Double = 0.0,
    val statSumXY: Double = 0.0,
    val statLastX: Double = 0.0,
    val statLastY: Double = 0.0,
    val pendingInput: Double? = null,     // first number of a pair, set by INPUT (x of x,y; df of df,t)
    val statResultKey: KeyAction? = null, // for result cycling
    val statResultPhase: Int = 0,
    val statInput: Double = 0.0,          // the number the prediction was made for

    // Cash flows
    val cashFlows: List<CashFlowEntry> = emptyList(),

    // Break-even
    val beFC: Double? = null,
    val beSP: Double? = null,
    val beVC: Double? = null,
    val beUnits: Double? = null,
    val beProfit: Double? = null,

    // Bond
    val bondCpnPct: Double? = null,
    val bondYield: Double? = null,
    val bondPrice: Double? = null,
    val bondCall: Double = 100.0,
    val bondSemi: Boolean = true,

    // Date
    // Julian days, so that changing the date format does not change the dates
    val settleDay: Int? = null,
    val maturityDay: Int? = null,
    val dateDMY: Boolean = false,
    val dateActual: Boolean = true,

    // Depreciation
    val deprYear: Int = 1,
    val deprPhase: DeprPhase = DeprPhase.NONE,
    val deprDepr: Double = 0.0,
    val deprRem: Double = 0.0,
    val deprAcc: Double = 0.0,

    // Probability
    val probDf: Double? = null,

    // Annunciators
    val showShiftUp: Boolean = false,
    val showShiftDown: Boolean = false,
    val showRad: Boolean = false,
    val showBegin: Boolean = false,
    val showInput: Boolean = false,

    // Error
    val isError: Boolean = false,

    // Power
    val isOff: Boolean = false,

    // True after =, unary ops, or recalled values — clears on new digit entry or CLEAR
    val hasResult: Boolean = false,
)

fun CalculatorState.displayDouble(): Double = x

fun CalculatorState.clearShift(): CalculatorState = copy(
    shiftState = ShiftState.NONE,
    showShiftUp = false,
    showShiftDown = false,
)
