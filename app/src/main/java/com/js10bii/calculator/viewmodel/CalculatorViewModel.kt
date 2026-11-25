package com.js10bii.calculator.viewmodel

import androidx.lifecycle.ViewModel
import com.js10bii.calculator.engine.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import kotlin.math.abs

class CalculatorViewModel : ViewModel() {

    private val _state = MutableStateFlow(CalculatorState())
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    fun onKeyPress(action: KeyAction) {
        val next = process(action, _state.value)
        // While digits are being typed the text is the source of the number; otherwise show() set it.
        _state.value = if (next.isEditing) next.copy(
            x = parseTyped(next.displayText), awaitingOperand = false, percentOperand = null,
        ) else next
    }

    private fun process(action: KeyAction, current: CalculatorState): CalculatorState {
        var s = current
        // Any key wakes the calculator from sleep
        if (s.isOff) return s.copy(isOff = false).show(0.0)

        // Any key dismisses an error and brings back the number behind it; C and <- do only that.
        if (s.isError) {
            s = s.copy(isError = false).show(s.x)
            if (action == KeyAction.CLEAR || action == KeyAction.BACKSPACE) return s.clearShift().clearModifiers()
        }

        if (s.awaitingInput != AwaitingInput.NONE) return handleAwaiting(action, s)

        // Repeated keys page through multi-value results only until some other key is used.
        if (action !in KEEPS_AMORT_CYCLE) s = s.copy(amortPhase = AmortPhase.NONE)
        if (action !in KEEPS_DEPR_CYCLE) s = s.copy(deprPhase = DeprPhase.NONE)
        if (action !in SHIFT_KEYS && action != KeyAction.PAREN_OPEN) s = s.copy(parenClosed = false)
        if (action !in KEEPS_STAT_CYCLE) s = s.copy(statResultKey = null, statResultPhase = 0)

        if (s.isInvActive || s.isHypActive) {
            val result = handleModified(action, s)
            if (result != null) return result
        }

        return when (action) {
            KeyAction.BLUE_SHIFT   -> toggleShift(s, ShiftState.BLUE)
            KeyAction.ORANGE_SHIFT -> toggleShift(s, ShiftState.ORANGE)

            KeyAction.DIGIT_0 -> appendDigit(s, '0')
            KeyAction.DIGIT_1 -> appendDigit(s, '1')
            KeyAction.DIGIT_2 -> appendDigit(s, '2')
            KeyAction.DIGIT_3 -> appendDigit(s, '3')
            KeyAction.DIGIT_4 -> appendDigit(s, '4')
            KeyAction.DIGIT_5 -> appendDigit(s, '5')
            KeyAction.DIGIT_6 -> appendDigit(s, '6')
            KeyAction.DIGIT_7 -> appendDigit(s, '7')
            KeyAction.DIGIT_8 -> appendDigit(s, '8')
            KeyAction.DIGIT_9 -> appendDigit(s, '9')

            KeyAction.DECIMAL     -> appendDecimal(s)
            KeyAction.SIGN_CHANGE -> changeSign(s)
            KeyAction.SCI_NOTATION -> startExponent(s)
            KeyAction.BACKSPACE   -> backspace(s)

            KeyAction.ADD, KeyAction.SUBTRACT,
            KeyAction.MULTIPLY, KeyAction.DIVIDE -> handleOperator(s, action)
            KeyAction.EQUALS -> handleEquals(s)

            KeyAction.CLEAR -> handleClear(s)
            KeyAction.CLEAR_ALL -> clearAll(s)

            // TVM
            KeyAction.TVM_N   -> handleTvmKey(s, "N")
            KeyAction.TVM_IYR -> handleTvmKey(s, "IYR")
            KeyAction.TVM_PV  -> handleTvmKey(s, "PV")
            KeyAction.TVM_PMT -> handleTvmKey(s, "PMT")
            KeyAction.TVM_FV  -> handleTvmKey(s, "FV")
            KeyAction.TVM_XPYR    -> handleXPYR(s)
            KeyAction.TVM_PYR     -> handlePYR(s)
            KeyAction.TVM_NOM_PCT -> handleNomPct(s)
            KeyAction.TVM_EFF_PCT -> handleEffPct(s)
            KeyAction.TVM_AMORT   -> handleAmort(s)
            KeyAction.TVM_BEG_END -> toggleBegEnd(s)

            // Business %
            KeyAction.INPUT  -> handleInput(s)
            KeyAction.CST, KeyAction.PRC, KeyAction.MAR, KeyAction.MU -> handleMarginKey(s, action)
            KeyAction.PCT    -> handlePercent(s)
            KeyAction.PCT_CHG -> handleTwoNumber(s, action)

            // Memory
            KeyAction.STORE_M -> s.copy(memoryM = s.displayDouble(), isEditing = false).clearShift().clearModifiers()
            KeyAction.RECALL_M -> showValue(s, s.memoryM)
            KeyAction.M_PLUS  -> s.copy(memoryM = s.memoryM + s.displayDouble(), isEditing = false).clearShift().clearModifiers()
            KeyAction.STO     -> s.copy(awaitingInput = AwaitingInput.STO_REGISTER).clearShift()
            KeyAction.RCL     -> s.copy(awaitingInput = AwaitingInput.RCL_REGISTER).clearShift()
            KeyAction.K_CONST -> handleKConst(s)
            KeyAction.SWAP    -> handleSwap(s)

            // Math
            KeyAction.SQRT      -> applyUnary(s) { CalculatorEngine.sqrt(it) }
            KeyAction.X_SQUARED -> applyUnary(s) { CalculatorEngine.xSquared(it) }
            KeyAction.RECIP     -> applyUnary(s) { CalculatorEngine.reciprocal(it) }
            KeyAction.E_POW_X   -> applyUnary(s) { CalculatorEngine.ePowX(it) }
            KeyAction.LN        -> applyUnary(s) { CalculatorEngine.ln(it) }
            KeyAction.FACTORIAL -> applyUnary(s) { CalculatorEngine.factorial(it) }
            KeyAction.PI        -> showValue(s, Math.PI)
            KeyAction.Y_POW_X -> handleOperator(s, action)
            KeyAction.N_PERMUT_R, KeyAction.N_COMB_R -> handleTwoNumber(s, action)

            // Trig
            KeyAction.SIN -> applyUnary(s) { CalculatorEngine.sin(it, s.angleMode) }
            KeyAction.COS -> applyUnary(s) { CalculatorEngine.cos(it, s.angleMode) }
            KeyAction.TAN -> applyUnary(s) { CalculatorEngine.tan(it, s.angleMode) }
            KeyAction.INV_MODIFIER -> s.copy(isInvActive = true).keepBlueShift()
            KeyAction.HYP_MODIFIER -> s.copy(isHypActive = true).keepBlueShift()

            // Display
            KeyAction.DISP -> s.copy(awaitingInput = AwaitingInput.DISP_PLACES).clearShift()
            KeyAction.RAD_DEG_TOGGLE -> {
                val m = if (s.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
                s.copy(angleMode = m, showRad = m == AngleMode.RAD).clearShift().clearModifiers()
            }
            KeyAction.ALG_CHAIN_TOGGLE -> {
                val m = if (s.calcMode == CalcMode.CHAIN) CalcMode.ALGEBRAIC else CalcMode.CHAIN
                s.copy(calcMode = m).clearShift().clearModifiers()
            }

            KeyAction.CMEM_ARM -> s.copy(awaitingInput = AwaitingInput.CMEM_TARGET).clearShift()

            // Probability
            KeyAction.Z_TO_P   -> handleZToP(s)
            KeyAction.DF_T_TO_P -> handleTwoNumber(s, action)
            KeyAction.RAND     -> showValue(s, Math.random())

            // Statistics
            KeyAction.STAT_PLUS  -> handleStatPlus(s)
            KeyAction.STAT_MINUS -> handleStatMinus(s)
            KeyAction.STAT_MEAN_XY -> handleStatResult(s, KeyAction.STAT_MEAN_XY)
            KeyAction.STAT_SX_SY   -> handleStatResult(s, KeyAction.STAT_SX_SY)
            KeyAction.STAT_OX_OY   -> handleStatResult(s, KeyAction.STAT_OX_OY)
            KeyAction.STAT_XW_B    -> handleStatResult(s, KeyAction.STAT_XW_B)
            KeyAction.STAT_XR      -> handleStatResult(s, KeyAction.STAT_XR)
            KeyAction.STAT_YM      -> handleStatResult(s, KeyAction.STAT_YM)
            KeyAction.STAT_N       -> s.clearShift().clearModifiers().show(s.statN)
            KeyAction.STAT_SUM_X  -> s.clearShift().clearModifiers().show(s.statSumX)
            KeyAction.STAT_SUM_Y  -> s.clearShift().clearModifiers().show(s.statSumY)
            KeyAction.STAT_SUM_X2 -> s.clearShift().clearModifiers().show(s.statSumX2)
            KeyAction.STAT_SUM_Y2 -> s.clearShift().clearModifiers().show(s.statSumY2)
            KeyAction.STAT_SUM_XY -> s.clearShift().clearModifiers().show(s.statSumXY)
            KeyAction.REGR        -> handleRegr(s)
            KeyAction.CLEAR_STAT  -> s.copy(
                statN = 0.0, statSumX = 0.0, statSumY = 0.0,
                statSumX2 = 0.0, statSumY2 = 0.0, statSumXY = 0.0,
                statLastX = 0.0, statLastY = 0.0, pendingInput = null, showInput = false,
                statResultKey = null, statResultPhase = 0,
            ).clearShift().clearModifiers()

            // Cash flows
            KeyAction.CFJ     -> handleCfj(s)
            KeyAction.NJ      -> handleNj(s)
            KeyAction.NPV_CALC -> handleNpvCalc(s)
            KeyAction.IRR_YR  -> handleIrrYr(s)

            // Break-even
            KeyAction.BREAKEVEN_UNITS  -> handleBreakeven(s, "UNITS")
            KeyAction.BREAKEVEN_SP     -> handleBreakeven(s, "SP")
            KeyAction.BREAKEVEN_VC     -> handleBreakeven(s, "VC")
            KeyAction.BREAKEVEN_FC     -> handleBreakeven(s, "FC")
            KeyAction.BREAKEVEN_PROFIT -> handleBreakeven(s, "PROFIT")

            // Bond
            KeyAction.BOND_CPN   -> handleBond(s, "CPN")
            KeyAction.BOND_CALL  -> handleBond(s, "CALL")
            KeyAction.BOND_YTM   -> handleBond(s, "YTM")
            KeyAction.BOND_PRICE -> handleBond(s, "PRICE")
            KeyAction.BOND_ACCINT -> handleBondAccInt(s)

            // Date
            KeyAction.DATE_CALC, KeyAction.DATE_DELTA_DAYS -> handleTwoNumber(s, action)
            KeyAction.DATE_DMY         -> s.copy(dateDMY = !s.dateDMY).clearShift().clearModifiers()
            KeyAction.DATE_360_ACT     -> s.copy(dateActual = !s.dateActual).clearShift().clearModifiers()
            KeyAction.DATE_SEMI_ANN    -> s.copy(bondSemi = !s.bondSemi).clearShift().clearModifiers()
            KeyAction.DATE_SET         -> handleBondDate(s, s.settleDay, "SetDate?") { copy(settleDay = it) }
            KeyAction.DATE_MAT         -> handleBondDate(s, s.maturityDay, "MatDate?") { copy(maturityDay = it) }

            // Depreciation
            KeyAction.DEPR_SL   -> handleDepr(s, "SL")
            KeyAction.DEPR_SOYD -> handleDepr(s, "SOYD")
            KeyAction.DEPR_DB   -> handleDepr(s, "DB")

            KeyAction.RND -> applyUnary(s) { CalculatorEngine.roundToPlaces(it, s.displayDecimalPlaces) }
            KeyAction.PAREN_OPEN  -> openParen(s)
            KeyAction.PAREN_CLOSE -> closeParen(s)
            KeyAction.ON_OFF -> s.copy(isOff = true, displayText = "asleep", isEditing = false).clearShift().clearModifiers()
            KeyAction.NOOP -> s.clearShift().clearModifiers()

            else -> s.clearShift().clearModifiers()
        }
    }

    // ---- Shift ----

    private fun toggleShift(s: CalculatorState, target: ShiftState): CalculatorState {
        // INV and HYP apply to blue functions and leave blue on, so pressing blue again must not undo them.
        if (target == ShiftState.BLUE && (s.isInvActive || s.isHypActive)) return s.keepBlueShift()
        return s.withShift(if (s.shiftState == target) ShiftState.NONE else target).clearModifiers()
    }

    private fun CalculatorState.keepBlueShift() = withShift(ShiftState.BLUE)

    private fun CalculatorState.withShift(shift: ShiftState) = copy(
        shiftState = shift,
        showShiftUp = shift == ShiftState.BLUE,
        showShiftDown = shift == ShiftState.ORANGE,
    )

    // ---- Digit entry ----

    private fun appendDigit(s: CalculatorState, digit: Char): CalculatorState {
        val base = s.clearShift().clearModifiers().copy(hasResult = false)
        val current = if (!s.isEditing) "" else s.displayText.replace(",", "")
        if ('E' in current) {
            val (mantissa, exponent) = current.split('E')
            val sign = if (exponent.startsWith("-")) "-" else ""
            val digits = (exponent.removePrefix("-") + digit).trimStart('0').ifEmpty { "0" }
            return base.withExponent(mantissa, sign + digits)
        }
        val raw = current + digit
        raw.toDoubleOrNull() ?: return base
        return base.copy(displayText = formatRaw(raw, s.displayDecimalPlaces), isEditing = true)
    }

    private fun appendDecimal(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (!s.isEditing) return base.copy(displayText = "0.", isEditing = true)
        val raw = s.displayText.replace(",", "")
        if ('.' in raw || 'E' in raw) return base
        return base.copy(displayText = "$raw.", isEditing = true)
    }

    /** The exponent starts as 0, which the first digit replaces, so the text can be parsed at every step. */
    private fun startExponent(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers().copy(hasResult = false)
        val raw = if (s.isEditing) s.displayText.replace(",", "") else "1"
        if ('E' in raw) return base
        return base.copy(displayText = "${raw}E0", isEditing = true)
    }

    /** Ignores an exponent that does not fit: the number must not become infinite or collapse to 0. */
    private fun CalculatorState.withExponent(mantissa: String, exponent: String): CalculatorState {
        val text = "${mantissa}E$exponent"
        val size = abs(text.toDoubleOrNull() ?: return this)
        val fits = if (mantissa.toDouble() == 0.0) exponent.removePrefix("-").length <= 3
                   else size >= java.lang.Double.MIN_NORMAL && size <= Double.MAX_VALUE
        return if (fits) copy(displayText = text) else this
    }

    private fun changeSign(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        val raw = s.displayText.replace(",", "")
        if (s.isEditing && 'E' in raw) {
            val (mantissa, exponent) = raw.split('E')
            return base.withExponent(mantissa, if (exponent.startsWith("-")) exponent.drop(1) else "-$exponent")
        }
        val v = s.displayDouble()
        if (v == 0.0) return base
        return if (s.isEditing) {
            val toggled = if (raw.startsWith("-")) raw.drop(1) else "-$raw"
            base.copy(displayText = formatRaw(toggled, s.displayDecimalPlaces))
        } else {
            base.showResult(-v) // counts as a new number, so the next TVM key stores it
        }
    }

    private fun backspace(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (!s.isEditing) return handleClear(s)
        val raw = s.displayText.replace(",", "").trimEnd()
        if ('E' in raw) {
            val (mantissa, exponent) = raw.split('E')
            val shorter = when {
                exponent == "0" -> return base.copy(displayText = formatRaw(mantissa, s.displayDecimalPlaces))
                exponent == "-0" -> "0"
                exponent.removePrefix("-").length == 1 -> exponent.dropLast(1) + "0"
                else -> exponent.dropLast(1)
            }
            return base.withExponent(mantissa, shorter)
        }
        val newRaw = if (raw.length <= 1 || (raw.length == 2 && raw.startsWith("-"))) "0"
                     else raw.dropLast(1)
        return base.copy(displayText = formatRaw(newRaw, s.displayDecimalPlaces), isEditing = newRaw != "0", x = 0.0)
    }

    // ---- Arithmetic ----

    private fun handleOperator(s: CalculatorState, op: KeyAction): CalculatorState {
        var done = s.clearShift().clearModifiers()
        if (done.awaitingOperand && done.pendingOperator != null) {
            // A second operator replaces the first
            done = (if (done.x == done.operandA) done else done.show(done.operandA)).popOperation().copy(awaitingOperand = false)
        } else if (done.awaitingOperand && done.outer.isNotEmpty()) {
            if (op != KeyAction.ADD && op != KeyAction.SUBTRACT) return errorState(done, "Error") // "( ×" has no first number
            done = done.show(0.0) // "( - 3" is a negative number
        }
        while (done.pendingOperator?.let { done.priority(it) >= done.priority(op) } == true) done = done.evaluateOperation()
        if (done.isError) return done
        val behind = done.pendingOperator?.let { done.outer + Outer(done.operandA, it) } ?: done.outer
        if (behind.count { it.op != null } >= MAX_PENDING) return errorState(done, "Error")
        return done.copy(
            outer = behind,
            operandA = done.x, pendingOperator = op, awaitingOperand = true, isEditing = false, hasResult = false,
        )
    }

    /** In Chain mode everything has the same priority, so it is evaluated from left to right. */
    private fun CalculatorState.priority(op: KeyAction): Int = when {
        calcMode == CalcMode.CHAIN -> 1
        op == KeyAction.ADD || op == KeyAction.SUBTRACT -> 1
        op == KeyAction.MULTIPLY || op == KeyAction.DIVIDE -> 2
        op == KeyAction.Y_POW_X -> 3
        else -> 4 // two-number functions
    }

    /** Drops the innermost operation; one waiting behind it, up to an open parenthesis, takes its place. */
    private fun CalculatorState.popOperation(): CalculatorState {
        val next = outer.lastOrNull()
        return if (next?.op == null) copy(pendingOperator = null, operandA = 0.0)
               else copy(pendingOperator = next.op, operandA = next.operand, outer = outer.dropLast(1))
    }

    /** Evaluates `operandA pendingOperator x`. */
    private fun CalculatorState.evaluateOperation(): CalculatorState {
        val op = pendingOperator ?: return this
        return popOperation().showOperation(operandA, op, x)
    }

    private fun CalculatorState.evaluateToParen(): CalculatorState {
        var s = this
        while (s.pendingOperator != null) s = s.evaluateOperation() // an error also ends it: errorState drops the operations
        return s
    }

    /** Evaluates everything, closing open parentheses. */
    private fun CalculatorState.evaluateAll(): CalculatorState {
        var s = evaluateToParen()
        while (s.outer.isNotEmpty()) s = s.copy(outer = s.outer.dropLast(1)).popOperation().evaluateToParen()
        return s
    }

    /** Completes pending arithmetic if its last operand is present. */
    private fun completePending(s: CalculatorState): CalculatorState = if (s.awaitingOperand) s else s.evaluateAll()

    private fun openParen(s: CalculatorState): CalculatorState {
        // A number before "(" is multiplied by what is in the parentheses
        val hasNumber = s.isEditing || s.parenClosed || ((s.pendingOperator != null || s.outer.isNotEmpty()) && !s.awaitingOperand)
        val base = if (hasNumber) handleOperator(s, KeyAction.MULTIPLY) else s.clearShift().clearModifiers()
        if (base.isError) return base
        if (base.outer.count { it.op == null } >= MAX_PARENS) return errorState(base, "Error")
        val behind = base.pendingOperator?.let { base.outer + Outer(base.operandA, it) } ?: base.outer
        return base.copy(
            outer = behind + Outer(), pendingOperator = null, operandA = 0.0, awaitingOperand = true,
            isEditing = false, hasResult = false, parenClosed = false,
        )
    }

    private fun closeParen(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (base.outer.none { it.op == null }) return base
        if (base.awaitingOperand) return errorState(base, "Error") // "()" or "(5 +)"
        val done = base.evaluateToParen()
        if (done.isError) return done
        return done.copy(outer = done.outer.dropLast(1)).popOperation().showResult(done.x)
            .let { if (it.isError) it else it.copy(parenClosed = true) }
    }

    /** `a INPUT b FN` calculates at once; `a FN b =` leaves FN pending like an operator. */
    private fun handleTwoNumber(s: CalculatorState, op: KeyAction): CalculatorState {
        val base = s.clearShift().clearModifiers()
        val a = base.pendingInput
        if (a == null || base.awaitingOperand) return handleOperator(base.copy(pendingInput = null, showInput = false), op)
        val done = completePending(base)
        return if (done.isError) done else done.copy(pendingInput = null, showInput = false).showOperation(a, op, done.x)
    }

    private fun CalculatorState.showOperation(a: Double, op: KeyAction, b: Double): CalculatorState = try {
        when (op) {
            KeyAction.PCT_CHG    -> showResult(PercentageEngine.percentChange(a, b))
            KeyAction.DF_T_TO_P  -> showResult(ProbabilityEngine.tToP(b, a))
            KeyAction.DF_P_TO_T  -> showResult(ProbabilityEngine.pToT(b, a))
            KeyAction.DATE_CALC  -> showDate(DateEngine.addDays(a, b, dateDMY))
            KeyAction.DATE_DELTA_DAYS -> showResult(
                if (dateActual) DateEngine.daysBetweenActual(a, b, dateDMY) else DateEngine.daysBetween360(a, b, dateDMY),
            )
            else -> showResult(CalculatorEngine.applyOperator(a, op, b))
        }
    } catch (e: ArithmeticException) { errorState(this, e.message ?: "Error") }

    /** Dates need all six places whatever the display setting. */
    private fun CalculatorState.showDate(date: Double): CalculatorState =
        showResult(date).let { if (it.isError) it else it.copy(displayText = CalculatorEngine.formatDisplay(it.x, 6)) }

    /** C clears an entry in progress; otherwise it cancels the whole calculation. */
    private fun handleClear(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (s.isEditing) return base.show(0.0).copy(
            awaitingOperand = s.pendingOperator != null || s.outer.isNotEmpty() || s.pendingInput != null,
        )
        return base.copy(
            pendingOperator = null, operandA = 0.0, outer = emptyList(), awaitingOperand = false,
            kOperator = null, kOperand = 0.0, kIsPercent = false, pendingInput = null, showInput = false,
        ).show(0.0)
    }

    private fun handleEquals(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (s.amortPhase != AmortPhase.NONE) return cycleAmort(s)
        if (s.deprPhase != DeprPhase.NONE) return cycleDepr(s)
        return when {
            s.pendingOperator == null && s.outer.isNotEmpty() && s.awaitingOperand -> errorState(base, "Error") // "( ="
            s.pendingOperator != null || s.outer.isNotEmpty() -> base.evaluateAll().let { if (it.isEditing) it.showResult(it.x) else it }
            s.kOperator != null ->
                base.showOperation(s.x, s.kOperator, if (s.kIsPercent) PercentageEngine.percentOf(s.x, s.kOperand) else s.kOperand)
            s.isEditing -> base.showResult(s.x)
            else -> base
        }
    }

    // ---- TVM ----

    private fun handleTvmKey(s: CalculatorState, register: String): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (!s.isEditing && !s.hasResult) return solveTvm(base, register)
        val done = completePending(base)
        if (done.isError) return done
        return storeTvm(done, register, done.x).copy(isEditing = false, hasResult = false)
    }

    private fun storeTvm(s: CalculatorState, register: String, v: Double) = when (register) {
        "N"   -> s.copy(tvmN   = v)
        "IYR" -> s.copy(tvmIYR = v)
        "PV"  -> s.copy(tvmPV  = v)
        "PMT" -> s.copy(tvmPMT = v)
        "FV"  -> s.copy(tvmFV  = v)
        else  -> s
    }

    private fun solveTvm(s: CalculatorState, register: String): CalculatorState {
        return try {
            val begin = s.paymentTiming == PaymentTiming.BEGIN
            val result = TvmEngine.solve(
                n   = if (register == "N"  ) null else s.tvmN ?: 0.0,
                iyr = if (register == "IYR") null else s.tvmIYR ?: 0.0,
                pv  = if (register == "PV" ) null else s.tvmPV ?: 0.0,
                pmt = if (register == "PMT") null else s.tvmPMT ?: 0.0,
                fv  = if (register == "FV" ) null else s.tvmFV ?: 0.0,
                pyr = s.tvmPYR, begin = begin,
            )
            if (result.isNaN() || result.isInfinite()) throw ArithmeticException("No solution")
            storeTvm(s, register, result).show(result)
        } catch (e: ArithmeticException) { errorState(s, e.message ?: "Error") }
    }

    private fun handleXPYR(s: CalculatorState): CalculatorState {
        val done = completePending(s.clearShift().clearModifiers())
        if (done.isError) return done
        return done.show(done.x * s.tvmPYR).let { if (it.isError) it else it.copy(tvmN = it.x) }
    }

    private fun handlePYR(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (!s.isEditing && !s.hasResult) return base.show(s.tvmPYR)
        val done = completePending(base)
        return if (done.isError) done else done.copy(tvmPYR = done.x, isEditing = false, hasResult = false)
    }

    private fun handleNomPct(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        // Right after EFF% the display holds that rate itself, so it is converted, not stored.
        if (s.isEditing || (s.hasResult && s.x != s.tvmEffRate)) {
            val done = completePending(base)
            return if (done.isError) done else done.copy(tvmIYR = done.x).show(done.x)
        }
        return base.show(TvmEngine.effToNom(s.tvmEffRate ?: 0.0, s.tvmPYR)).let { if (it.isError) it else it.copy(tvmIYR = it.x) }
    }

    private fun handleEffPct(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (s.isEditing || (s.hasResult && s.x != s.tvmEffRate)) {
            val done = completePending(base)
            return if (done.isError) done else done.copy(tvmEffRate = done.x).show(done.x)
        }
        // A result, so that I/YR can store it (manual table 13-20)
        return base.showResult(TvmEngine.nomToEff(s.tvmIYR ?: 0.0, s.tvmPYR)).let { if (it.isError) it else it.copy(tvmEffRate = it.x) }
    }

    private fun toggleBegEnd(s: CalculatorState): CalculatorState {
        val t = if (s.paymentTiming == PaymentTiming.END) PaymentTiming.BEGIN else PaymentTiming.END
        return s.copy(paymentTiming = t, showBegin = t == PaymentTiming.BEGIN).clearShift().clearModifiers()
    }

    private fun handleAmort(s: CalculatorState): CalculatorState {
        val base = completePending(s.clearShift().clearModifiers())
        if (base.isError) return base
        val pv  = s.tvmPV  ?: return errorState(base, "PV?")
        val iyr = s.tvmIYR ?: return errorState(base, "I/YR?")
        val pmt = s.tvmPMT ?: return errorState(base, "PMT?")
        val first = base.pendingInput
        val (start, end) = when {
            first != null && !base.awaitingOperand -> first.toInt() to base.x.toInt()   // first INPUT last AMORT
            first != null                          -> first.toInt() to first.toInt()    // period INPUT AMORT
            base.isEditing || base.hasResult       -> base.x.toInt() to base.x.toInt()  // period AMORT
            s.amortPhase != AmortPhase.NONE        -> s.amortEnd + 1 to 2 * s.amortEnd - s.amortStart + 1 // next set
            else                                   -> s.amortStart to s.amortEnd
        }
        if (start < 1 || end < start) return errorState(base.copy(pendingInput = null, showInput = false), "Periods?")
        val (prin, int, bal) = TvmEngine.amortize(start, end, pv, iyr, pmt, s.tvmPYR, s.paymentTiming == PaymentTiming.BEGIN)
        return base.copy(
            displayText = if (start == end) "$start" else "$start – $end",
            amortStart = start, amortEnd = end,
            amortPhase = AmortPhase.RANGE, amortPrin = prin, amortInt = int, amortBal = zeroIfNoise(bal, pv),
            isEditing = false, hasResult = false, pendingInput = null, showInput = false, awaitingOperand = false,
        )
    }

    private fun cycleAmort(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        return when (s.amortPhase) {
            AmortPhase.RANGE -> base.copy(amortPhase = AmortPhase.PRIN).show(s.amortPrin)
            AmortPhase.PRIN  -> base.copy(amortPhase = AmortPhase.INT).show(s.amortInt)
            AmortPhase.INT   -> base.copy(amortPhase = AmortPhase.BAL).show(s.amortBal)
            AmortPhase.BAL   -> base.copy(amortPhase = AmortPhase.PRIN).show(s.amortPrin)
            AmortPhase.NONE  -> base
        }
    }

    // ---- Input key ----

    private fun handleInput(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (!s.isEditing && !s.hasResult) return base
        val done = completePending(base)
        if (done.isError) return done
        return done.copy(
            pendingInput = done.x, showInput = true, awaitingOperand = true, isEditing = false, hasResult = false,
        )
    }

    // ---- Business % ----

    /** n% is n/100, except after + or - where it is n% of the left operand. The operation stays pending. */
    private fun handlePercent(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        return if (s.pendingOperator == KeyAction.ADD || s.pendingOperator == KeyAction.SUBTRACT)
            base.showResult(PercentageEngine.percentOf(s.operandA, s.x)).copy(percentOperand = s.x)
        else base.showResult(s.x / 100.0)
    }

    private fun handleMarginKey(s: CalculatorState, key: KeyAction): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (s.isEditing || s.hasResult) {
            val done = completePending(base)
            return if (done.isError) done else done.storeMargin(key, done.x).copy(isEditing = false, hasResult = false)
        }
        return try {
            val value = when (key) {
                KeyAction.CST -> if (s.rateIsMargin) PercentageEngine.costFromMargin(s.need(KeyAction.PRC), s.need(KeyAction.MAR))
                                 else PercentageEngine.costFromMarkup(s.need(KeyAction.PRC), s.need(KeyAction.MU))
                KeyAction.PRC -> if (s.rateIsMargin) PercentageEngine.priceFromMargin(s.need(KeyAction.CST), s.need(KeyAction.MAR))
                                 else PercentageEngine.priceFromMarkup(s.need(KeyAction.CST), s.need(KeyAction.MU))
                KeyAction.MAR -> PercentageEngine.margin(s.need(KeyAction.CST), s.need(KeyAction.PRC))
                else          -> PercentageEngine.markup(s.need(KeyAction.CST), s.need(KeyAction.PRC))
            }
            base.storeMargin(key, value).show(value)
        } catch (e: ArithmeticException) { errorState(base, e.message ?: "Error") }
    }

    private fun CalculatorState.marginValue(key: KeyAction): Double? = when (key) {
        KeyAction.CST -> cstValue
        KeyAction.PRC -> prcValue
        KeyAction.MAR -> marValue
        else          -> muValue
    }

    private fun CalculatorState.need(key: KeyAction): Double =
        marginValue(key) ?: throw ArithmeticException("Enter ${key.name}")

    private fun CalculatorState.storeMargin(key: KeyAction, value: Double) = when (key) {
        KeyAction.CST -> copy(cstValue = value)
        KeyAction.PRC -> copy(prcValue = value)
        KeyAction.MAR -> copy(marValue = value, rateIsMargin = true)
        else          -> copy(muValue = value, rateIsMargin = false)
    }

    // ---- Memory ----

    private fun handleKConst(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        val op = s.pendingOperator ?: return base
        // The operation stays pending so that the first = completes it; later = presses reuse the constant.
        return base.copy(
            kOperator = op, kOperand = s.percentOperand ?: s.x, kIsPercent = s.percentOperand != null,
        ).showResult(s.x)
    }

    /** Second value of a two-valued result, else the two operands of a pending operation or INPUT pair. */
    private fun handleSwap(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        return when {
            s.deprPhase == DeprPhase.DEPR -> base.copy(deprPhase = DeprPhase.REM).show(s.deprRem)
            s.deprPhase != DeprPhase.NONE -> base.copy(deprPhase = DeprPhase.DEPR).show(s.deprDepr)
            s.statResultKey != null       -> showOtherStat(base)
            s.awaitingOperand             -> base
            s.pendingOperator != null     -> base.copy(operandA = s.x).show(s.operandA)
            s.pendingInput != null        -> base.copy(pendingInput = s.x).show(s.pendingInput)
            else                          -> base
        }
    }

    // ---- Statistics ----

    private fun handleStatPlus(s: CalculatorState): CalculatorState {
        val done = completePending(s.clearShift().clearModifiers())
        if (done.isError) return done
        // x INPUT y: the number held by INPUT is x, the displayed one is y.
        val x = done.pendingInput ?: done.x
        val y = if (done.pendingInput != null) done.x else 1.0
        return done.addStat(x, y, 1).copy(statLastX = x, statLastY = y)
    }

    private fun handleStatMinus(s: CalculatorState): CalculatorState {
        val done = completePending(s.clearShift().clearModifiers())
        if (done.isError) return done
        if (s.statN < 1) return errorState(done, "No data")
        // Removes the typed value or pair, or the last one entered if nothing was typed.
        return when {
            done.pendingInput != null           -> done.addStat(done.pendingInput, done.x, -1)
            done.isEditing || done.hasResult    -> done.addStat(done.x, 1.0, -1)
            else                                -> done.addStat(s.statLastX, s.statLastY, -1)
        }
    }

    private fun CalculatorState.addStat(x: Double, y: Double, sign: Int): CalculatorState = copy(
        statN = statN + sign, statSumX = statSumX + sign * x, statSumY = statSumY + sign * y,
        statSumX2 = statSumX2 + sign * x * x, statSumY2 = statSumY2 + sign * y * y, statSumXY = statSumXY + sign * x * y,
        pendingInput = null, showInput = false,
    ).show(statN + sign)

    /** Pressing the key again, or SWAP, pages between its two values. */
    private fun handleStatResult(s: CalculatorState, key: KeyAction): CalculatorState {
        val base = s.clearShift().clearModifiers()
        return if (s.statResultKey == key) showOtherStat(base)
               else showStat(base.copy(statResultKey = key, statResultPhase = 0, statInput = s.x), key)
    }

    private fun showOtherStat(s: CalculatorState): CalculatorState =
        s.statResultKey?.let { showStat(s.copy(statResultPhase = 1 - s.statResultPhase), it) } ?: s

    private fun showStat(s: CalculatorState, key: KeyAction): CalculatorState = try {
        val first = s.statResultPhase == 0
        s.show(
            when (key) {
                KeyAction.STAT_MEAN_XY -> if (first) StatisticsEngine.mean(s.statN, s.statSumX) else StatisticsEngine.mean(s.statN, s.statSumY)
                KeyAction.STAT_SX_SY   -> if (first) StatisticsEngine.sampleStdDev(s.statN, s.statSumX, s.statSumX2) else StatisticsEngine.sampleStdDev(s.statN, s.statSumY, s.statSumY2)
                KeyAction.STAT_OX_OY   -> if (first) StatisticsEngine.popStdDev(s.statN, s.statSumX, s.statSumX2) else StatisticsEngine.popStdDev(s.statN, s.statSumY, s.statSumY2)
                KeyAction.STAT_XW_B    -> if (first) StatisticsEngine.weightedMean(s.statSumXY, s.statSumY) else StatisticsEngine.intercept(s.statN, s.statSumX, s.statSumY, s.statSumX2, s.statSumXY)
                KeyAction.STAT_XR      -> if (first) StatisticsEngine.predictX(s.statInput, s.statN, s.statSumX, s.statSumY, s.statSumX2, s.statSumXY) else StatisticsEngine.correlation(s.statN, s.statSumX, s.statSumY, s.statSumX2, s.statSumY2, s.statSumXY)
                else                   -> if (first) StatisticsEngine.predictY(s.statInput, s.statN, s.statSumX, s.statSumY, s.statSumX2, s.statSumXY) else StatisticsEngine.slope(s.statN, s.statSumX, s.statSumY, s.statSumX2, s.statSumXY)
            },
        )
    } catch (e: ArithmeticException) { errorState(s, e.message ?: "Error") }

    private fun handleRegr(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        return try {
            val r = StatisticsEngine.correlation(s.statN, s.statSumX, s.statSumY, s.statSumX2, s.statSumY2, s.statSumXY)
            base.show(r)
        } catch (e: ArithmeticException) { errorState(base, e.message ?: "Error") }
    }

    // ---- Cash flows ----

    private fun handleCfj(s: CalculatorState): CalculatorState {
        val done = completePending(s.clearShift().clearModifiers())
        if (done.isError) return done
        val amount = done.pendingInput
        val entry = if (amount != null && !done.awaitingOperand) CashFlowEntry(amount, done.x.toInt().coerceAtLeast(1))
                    else CashFlowEntry(done.x, 1)
        return done.copy(
            cashFlows = s.cashFlows + entry, isEditing = false, hasResult = false,
            pendingInput = null, showInput = false, awaitingOperand = false,
        )
    }

    private fun handleNj(s: CalculatorState): CalculatorState {
        val base = completePending(s.clearShift().clearModifiers())
        if (base.isError) return base
        if (s.cashFlows.isEmpty()) return errorState(base, "No CF")
        val count = base.x.toInt().coerceAtLeast(1)
        val updated = s.cashFlows.toMutableList().apply {
            val last = last()
            set(size - 1, last.copy(count = count))
        }
        return base.copy(cashFlows = updated, isEditing = false, hasResult = false)
    }

    private fun handleNpvCalc(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (s.cashFlows.isEmpty()) return errorState(base, "No CF")
        val iyr = s.tvmIYR ?: return errorState(base, "I/YR?")
        return try {
            val rate = iyr / s.tvmPYR / 100.0
            val result = CashFlowEngine.npv(rate, s.cashFlows)
            base.show(result).let { if (it.isError) it else it.copy(tvmPV = it.x) }
        } catch (e: ArithmeticException) { errorState(base, e.message ?: "Error") }
    }

    private fun handleIrrYr(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (s.cashFlows.isEmpty()) return errorState(base, "No CF")
        return try {
            val periodicIrr = CashFlowEngine.irr(s.cashFlows)
            val annualIrr = periodicIrr * s.tvmPYR * 100.0
            if (annualIrr.isNaN() || annualIrr.isInfinite()) throw ArithmeticException("No solution")
            base.copy(tvmIYR = annualIrr).show(annualIrr)
        } catch (e: ArithmeticException) { errorState(base, e.message ?: "Error") }
    }

    // ---- Break-even ----

    private fun handleBreakeven(s: CalculatorState, field: String): CalculatorState {
        val base = s.clearShift().clearModifiers()
        return if (s.isEditing || s.hasResult) {
            val done = completePending(base)
            if (done.isError) return done
            val stored = done.copy(isEditing = false, hasResult = false)
            when (field) {
                "UNITS"  -> stored.copy(beUnits  = done.x)
                "SP"     -> stored.copy(beSP     = done.x)
                "VC"     -> stored.copy(beVC     = done.x)
                "FC"     -> stored.copy(beFC     = done.x)
                "PROFIT" -> stored.copy(beProfit = done.x)
                else     -> base
            }
        } else {
            try {
                when (field) {
                    "UNITS"  -> {
                        val r = BreakEvenEngine.solveUnits(s.beFC ?: error("FC?"), s.beSP ?: error("SP?"), s.beVC ?: error("VC?"), s.beProfit ?: 0.0)
                        base.copy(beUnits = r).show(r)
                    }
                    "PROFIT" -> {
                        val r = BreakEvenEngine.solveProfit(s.beUnits ?: error("Units?"), s.beFC ?: error("FC?"), s.beSP ?: error("SP?"), s.beVC ?: error("VC?"))
                        base.copy(beProfit = r).show(r)
                    }
                    "SP"     -> {
                        val r = BreakEvenEngine.solveSP(s.beUnits ?: error("Units?"), s.beFC ?: error("FC?"), s.beVC ?: error("VC?"), s.beProfit ?: 0.0)
                        base.copy(beSP = r).show(r)
                    }
                    "VC"     -> {
                        val r = BreakEvenEngine.solveVC(s.beUnits ?: error("Units?"), s.beFC ?: error("FC?"), s.beSP ?: error("SP?"), s.beProfit ?: 0.0)
                        base.copy(beVC = r).show(r)
                    }
                    "FC"     -> {
                        val r = BreakEvenEngine.solveFC(s.beUnits ?: error("Units?"), s.beSP ?: error("SP?"), s.beVC ?: error("VC?"), s.beProfit ?: 0.0)
                        base.copy(beFC = r).show(r)
                    }
                    else -> base
                }
            } catch (e: Exception) { errorState(base, e.message?.take(12) ?: "Error") }
        }
    }

    // ---- Bond ----

    private fun handleBond(s: CalculatorState, field: String): CalculatorState {
        val base = s.clearShift().clearModifiers()
        if (s.isEditing || s.hasResult) {
            val done = completePending(base)
            if (done.isError) return done
            val stored = done.copy(isEditing = false, hasResult = false)
            return when (field) {
                "CPN"   -> stored.copy(bondCpnPct = done.x)
                "CALL"  -> stored.copy(bondCall = done.x)
                "YTM"   -> stored.copy(bondYield = done.x)
                "PRICE" -> stored.copy(bondPrice = done.x)
                else    -> base
            }
        }
        return try {
            when (field) {
                "PRICE" -> {
                    val ytm = s.bondYield ?: throw ArithmeticException("YTM?")
                    val cpn = s.bondCpnPct ?: throw ArithmeticException("CPN%?")
                    val p = BondEngine.price(bondSchedule(s), ytm, cpn, s.bondCall, s.bondSemi)
                    base.copy(bondPrice = p).show(p)
                }
                "YTM"   -> {
                    val price = s.bondPrice ?: throw ArithmeticException("PRICE?")
                    val cpn = s.bondCpnPct ?: throw ArithmeticException("CPN%?")
                    val y = BondEngine.ytm(bondSchedule(s), price, cpn, s.bondCall, s.bondSemi)
                    base.copy(bondYield = y).show(y)
                }
                "CPN"   -> base.show(s.bondCpnPct ?: 0.0)
                "CALL"  -> base.show(s.bondCall)
                else    -> base
            }
        } catch (e: ArithmeticException) { errorState(base, e.message ?: "Error") }
    }

    private fun bondSchedule(s: CalculatorState): BondEngine.Schedule = BondEngine.schedule(
        s.settleDay ?: throw ArithmeticException("SetDate?"),
        s.maturityDay ?: throw ArithmeticException("MatDate?"),
        s.bondSemi, s.dateActual,
    )

    private fun handleBondAccInt(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        return try {
            val cpn = s.bondCpnPct ?: throw ArithmeticException("CPN%?")
            base.show(BondEngine.accruedInterest(cpn, bondSchedule(s).accrued, s.bondSemi))
        } catch (e: ArithmeticException) { errorState(base, e.message ?: "Error") }
    }

    // ---- Date ----

    /** Stores a typed or calculated date, or shows the stored one. */
    private fun handleBondDate(
        s: CalculatorState, stored: Int?, missing: String, store: CalculatorState.(Int) -> CalculatorState,
    ): CalculatorState {
        val done = completePending(s.clearShift().clearModifiers())
        if (done.isError) return done
        return try {
            val day = if (done.isEditing || done.hasResult) DateEngine.toJulianDay(done.x, s.dateDMY)
                      else stored ?: return errorState(done, missing)
            done.store(day).showDate(DateEngine.fromJulianDayEncoded(day, s.dateDMY)).copy(hasResult = false)
        } catch (e: ArithmeticException) { errorState(done, e.message ?: "Error") }
    }

    // ---- Depreciation ----

    private fun handleDepr(s: CalculatorState, method: String): CalculatorState {
        val base = s.clearShift().clearModifiers()
        val cost = s.tvmPV ?: return errorState(base, "PV=cost")
        val salvage = s.tvmFV ?: 0.0
        val life = (s.tvmN ?: return errorState(base, "N=life")).toInt()
        val year = if (s.isEditing) s.displayDouble().toInt().coerceAtLeast(1) else s.deprYear

        return try {
            val (depr, acc, bookValue) = when (method) {
                "SL"   -> DepreciationEngine.straightLine(cost, salvage, life, year)
                "SOYD" -> DepreciationEngine.soyd(cost, salvage, life, year)
                "DB"   -> {
                    val factor = (s.tvmIYR ?: 0.0) / 100.0
                    if (factor <= 0.0) return errorState(base, "I/YR=DB%")
                    DepreciationEngine.decliningBalance(cost, salvage, life, year, factor)
                }
                else   -> return base
            }
            base.copy(deprYear = year, deprDepr = depr, deprRem = zeroIfNoise(bookValue - salvage, cost), deprAcc = acc, deprPhase = DeprPhase.DEPR).show(depr)
        } catch (e: Exception) { errorState(base, e.message?.take(12) ?: "Error") }
    }

    private fun cycleDepr(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        return when (s.deprPhase) {
            DeprPhase.DEPR -> base.copy(deprPhase = DeprPhase.REM).show(s.deprRem)
            DeprPhase.REM  -> base.copy(deprPhase = DeprPhase.ACC).show(s.deprAcc)
            DeprPhase.ACC  -> base.copy(deprPhase = DeprPhase.DEPR).show(s.deprDepr)
            DeprPhase.NONE -> base
        }
    }

    // ---- Probability ----

    private fun handleZToP(s: CalculatorState): CalculatorState {
        val base = s.clearShift().clearModifiers()
        return try {
            val result = ProbabilityEngine.zToP(s.displayDouble())
            base.show(result)
        } catch (e: ArithmeticException) { errorState(base, e.message ?: "Error") }
    }

    // ---- Awaiting multi-key ----

    private fun handleAwaiting(action: KeyAction, s: CalculatorState): CalculatorState {
        return when (s.awaitingInput) {
            AwaitingInput.STO_REGISTER -> handleRegisterAction(action, s, isSto = true)
            AwaitingInput.RCL_REGISTER -> handleRegisterAction(action, s, isSto = false)
            AwaitingInput.DISP_PLACES  -> handleDispDigit(action, s)
            AwaitingInput.CMEM_TARGET  -> handleCmemTarget(action, s)
            AwaitingInput.NONE -> s
        }
    }

    private fun handleRegisterAction(action: KeyAction, s: CalculatorState, isSto: Boolean): CalculatorState {
        val base = s.copy(awaitingInput = AwaitingInput.NONE, awaitingTens = false).clearShift()
        // RCL xP/YR and RCL P/YR are shifted keys
        if (action == KeyAction.ORANGE_SHIFT || action == KeyAction.BLUE_SHIFT)
            return toggleShift(s, if (action == KeyAction.BLUE_SHIFT) ShiftState.BLUE else ShiftState.ORANGE)
        if (!isSto && action == KeyAction.TVM_PYR) return base.show(s.tvmPYR)
        if (!isSto && action == KeyAction.TVM_XPYR) return base.show((s.tvmN ?: 0.0) / s.tvmPYR)
        val tvmReg = when (action) {
            KeyAction.TVM_N   -> "N";  KeyAction.TVM_IYR -> "IYR"
            KeyAction.TVM_PV  -> "PV"; KeyAction.TVM_PMT -> "PMT"
            KeyAction.TVM_FV  -> "FV"; else -> null
        }
        if (tvmReg != null) {
            return if (isSto) storeTvm(base, tvmReg, s.displayDouble()).copy(isEditing = false)
            else {
                val v = when (tvmReg) {
                    "N" -> s.tvmN; "IYR" -> s.tvmIYR; "PV" -> s.tvmPV
                    "PMT" -> s.tvmPMT; "FV" -> s.tvmFV; else -> null
                } ?: 0.0
                base.show(v)
            }
        }
        if (!isSto && action in MARGIN_KEYS) return base.show(s.marginValue(action) ?: 0.0)
        // Registers 10 to 19 are typed as . and a digit
        if (action == KeyAction.DECIMAL && !s.awaitingTens) return s.copy(awaitingTens = true)
        val digit = digitToInt(action) ?: return base
        val reg = if (s.awaitingTens) 10 + digit else digit
        return if (isSto) {
            val updated = s.registers.toMutableList().also { it[reg] = s.displayDouble() }
            base.copy(registers = updated, isEditing = false)
        } else {
            base.showResult(s.registers[reg])
        }
    }

    private fun handleDispDigit(action: KeyAction, s: CalculatorState): CalculatorState {
        val base = s.copy(awaitingInput = AwaitingInput.NONE)
        return digitToInt(action)?.let { withDecimalPlaces(base, it) } ?: base
    }

    /** Only reformats: a number being typed, a message or an amortization range is left as it is. */
    private fun withDecimalPlaces(s: CalculatorState, places: Int): CalculatorState {
        val next = s.copy(displayDecimalPlaces = places.coerceIn(0, 9))
        val showsNumber = !s.isEditing && !s.isError && !s.isOff && s.amortPhase != AmortPhase.RANGE
        return if (showsNumber) next.copy(displayText = fmt(next.x, next)) else next
    }

    private fun handleCmemTarget(action: KeyAction, s: CalculatorState): CalculatorState {
        val base = s.copy(awaitingInput = AwaitingInput.NONE).show(0.0)
        return when (action) {
            // TVM CLR: reset to 0 (not null) so the solver still has 4 known values
            KeyAction.DIGIT_1 -> base.copy(
                tvmN = 0.0, tvmIYR = 0.0, tvmPV = 0.0, tvmPMT = 0.0, tvmFV = 0.0,
                tvmEffRate = null, amortPhase = AmortPhase.NONE,
            )
            // BR EV CLR
            KeyAction.DIGIT_4 -> base.copy(beFC = null, beSP = null, beVC = null, beUnits = null, beProfit = null)
            // BOND CLR
            KeyAction.DIGIT_7 -> base.copy(
                bondCpnPct = null, bondYield = null, bondPrice = null, bondCall = 100.0, settleDay = null, maturityDay = null,
            )
            // CFLO CLR
            KeyAction.DIGIT_0 -> base.copy(cashFlows = emptyList())
            else -> base
        }
    }

    // ---- Modified trig ----

    private fun handleModified(action: KeyAction, s: CalculatorState): CalculatorState? {
        if (s.isInvActive && action == KeyAction.DF_T_TO_P) return handleTwoNumber(s, KeyAction.DF_P_TO_T)
        if (s.isInvActive && action == KeyAction.Z_TO_P) return applyUnary(s) { ProbabilityEngine.pToZ(it) }
        if (action !in setOf(KeyAction.SIN, KeyAction.COS, KeyAction.TAN)) return null
        val base = s.clearShift().clearModifiers()
        val x = s.displayDouble()
        return try {
            val result = when {
                s.isInvActive && s.isHypActive -> when (action) {
                    KeyAction.SIN -> CalculatorEngine.asinh(x)
                    KeyAction.COS -> CalculatorEngine.acosh(x)
                    else          -> CalculatorEngine.atanh(x)
                }
                s.isHypActive -> when (action) {
                    KeyAction.SIN -> CalculatorEngine.sinh(x)
                    KeyAction.COS -> CalculatorEngine.cosh(x)
                    else          -> CalculatorEngine.tanh(x)
                }
                s.isInvActive -> when (action) {
                    KeyAction.SIN -> CalculatorEngine.asin(x, s.angleMode)
                    KeyAction.COS -> CalculatorEngine.acos(x, s.angleMode)
                    else          -> CalculatorEngine.atan(x, s.angleMode)
                }
                else -> null
            } ?: return null
            base.showResult(result)
        } catch (e: ArithmeticException) { errorState(base, e.message ?: "Error") }
    }

    // ---- Helpers ----

    private fun applyUnary(s: CalculatorState, fn: (Double) -> Double): CalculatorState {
        val base = s.clearShift().clearModifiers()
        return try {
            base.showResult(fn(s.displayDouble()))
        } catch (e: ArithmeticException) { errorState(base, e.message ?: "Error") }
    }

    /** Shows a number as the current operand. Every calculated or recalled value must go through here. */
    private fun CalculatorState.show(value: Double): CalculatorState {
        if (value.isNaN() || value.isInfinite()) return errorState(this, "Error")
        val v = CalculatorEngine.toTwelveDigits(value)
        return copy(x = v, displayText = fmt(v, this), isEditing = false, hasResult = false, awaitingOperand = false, percentOperand = null)
    }

    /** Like [show], for values that the next TVM key or INPUT should store rather than solve over. */
    private fun CalculatorState.showResult(value: Double): CalculatorState =
        show(value).let { if (it.isError) it else it.copy(hasResult = true) }

    /** What is left of [whole] after many payments: a remainder this small is rounding, not money. */
    private fun zeroIfNoise(rest: Double, whole: Double): Double = if (abs(rest) <= abs(whole) * 1e-11) 0.0 else rest

    private fun parseTyped(text: String): Double = text.replace(",", "").toDoubleOrNull() ?: 0.0

    private fun showValue(s: CalculatorState, value: Double): CalculatorState =
        s.clearShift().clearModifiers().showResult(value)

    private fun clearAll(s: CalculatorState) = CalculatorState(
        displayDecimalPlaces = s.displayDecimalPlaces,
        angleMode = s.angleMode,
        calcMode = s.calcMode,
        showRad = s.showRad,
        dateDMY = s.dateDMY,
        dateActual = s.dateActual,
        bondSemi = s.bondSemi,
        tvmPYR = s.tvmPYR,
        paymentTiming = s.paymentTiming,
        showBegin = s.showBegin,
    )

    private fun errorState(s: CalculatorState, msg: String) =
        s.clearShift().clearModifiers().copy(
            displayText = if (msg.length > 12) "Error" else msg,
            isError = true, isEditing = false, hasResult = false,
            pendingOperator = null, operandA = 0.0, outer = emptyList(), parenClosed = false, awaitingOperand = false,
        )

    private fun fmt(value: Double, s: CalculatorState) =
        CalculatorEngine.formatDisplay(value, s.displayDecimalPlaces)

    private fun formatRaw(raw: String, dp: Int): String {
        val sign = if (raw.startsWith("-")) "-" else "" // kept as text, because -0 as an integer is 0
        val whole = raw.removePrefix(sign).substringBefore('.')
        val grouped = whole.toBigIntegerOrNull()?.let { "%,d".format(Locale.US, it) } ?: whole
        return sign + grouped + raw.drop(sign.length + whole.length)
    }

    private fun digitToInt(action: KeyAction): Int? = when (action) {
        KeyAction.DIGIT_0 -> 0; KeyAction.DIGIT_1 -> 1; KeyAction.DIGIT_2 -> 2
        KeyAction.DIGIT_3 -> 3; KeyAction.DIGIT_4 -> 4; KeyAction.DIGIT_5 -> 5
        KeyAction.DIGIT_6 -> 6; KeyAction.DIGIT_7 -> 7; KeyAction.DIGIT_8 -> 8
        KeyAction.DIGIT_9 -> 9; else -> null
    }

    private fun CalculatorState.clearModifiers() = copy(isInvActive = false, isHypActive = false)

    // ---- Settings ----

    fun setDecimalPlaces(dp: Int) {
        _state.value = withDecimalPlaces(_state.value, dp)
    }

    fun setAngleMode(mode: AngleMode) {
        _state.value = _state.value.copy(angleMode = mode, showRad = mode == AngleMode.RAD)
    }

    fun setCalcMode(mode: CalcMode) {
        _state.value = _state.value.copy(calcMode = mode)
    }

    fun setPyr(pyr: Double) {
        _state.value = _state.value.copy(tvmPYR = pyr)
    }

    fun setPaymentTiming(timing: PaymentTiming) {
        _state.value = _state.value.copy(paymentTiming = timing, showBegin = timing == PaymentTiming.BEGIN)
    }

    private companion object {
        const val MAX_PARENS = 4
        const val MAX_PENDING = 12
        val SHIFT_KEYS = setOf(KeyAction.BLUE_SHIFT, KeyAction.ORANGE_SHIFT)
        val KEEPS_AMORT_CYCLE = SHIFT_KEYS + KeyAction.EQUALS + KeyAction.DISP + KeyAction.TVM_AMORT
        val KEEPS_DEPR_CYCLE = SHIFT_KEYS + KeyAction.EQUALS + KeyAction.DISP + KeyAction.SWAP
        val MARGIN_KEYS = setOf(KeyAction.CST, KeyAction.PRC, KeyAction.MAR, KeyAction.MU)
        val KEEPS_STAT_CYCLE = SHIFT_KEYS + KeyAction.DISP + KeyAction.SWAP + setOf(
            KeyAction.STAT_MEAN_XY, KeyAction.STAT_SX_SY, KeyAction.STAT_OX_OY,
            KeyAction.STAT_XW_B, KeyAction.STAT_XR, KeyAction.STAT_YM,
        )
    }
}
