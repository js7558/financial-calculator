package com.js10bii.calculator.keystroke

import com.js10bii.calculator.engine.CalculatorEngine
import com.js10bii.calculator.engine.KeyAction
import com.js10bii.calculator.engine.TvmEngine
import com.js10bii.calculator.support.Calc
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

/** State that should not leak from one calculation into the next, and number entry/display. */
class EntryAndStateTest {

    private fun calc(keys: String) = Calc().press(keys)

    // ── Precision ────────────────────────────────────────────────────────────

    @Test fun storedMemoryKeepsPrecision() { calc("1 ÷ 3 = →M C RM × 3 =").expect("1.00") }

    @Test fun solvedTvmValueKeepsPrecisionInArithmetic() {
        // 14000 @10%/360: PMT = -122.860...; ×360 differs by > 0.10 if PMT was rounded to cents first.
        val exact = Math.round(TvmEngine
            .solve(360.0, 10.0, 14000.0, null, 0.0, 12.0, false) * 360 * 100) / 100.0
        calc("360 N 10 I/YR 14000 PV 0 FV PMT × 360 =").expect(exact, 0.005)
    }

    @Test fun computedRateStoredInTvmKeepsPrecision() {
        // 5 ÷ 7 = 0.714285...% must reach I/YR unrounded: 1,000,000 for one year.
        calc("1 o:P/YR 1 N -1000000 PV 0 PMT 5 ÷ 7 = I/YR FV").expect("1,007,142.86")
    }

    @Test fun reciprocalTwiceIsIdentity() { calc("7 o:1/x o:1/x").expect("7.00") }

    @Test fun sqrtThenSquare() { calc("2 o:√x o:x²").expect("2.00") }

    // ── State leaks ──────────────────────────────────────────────────────────

    @Test fun typingAfterError() { calc("5 ÷ 0 = 12").expect("12") }

    @Test fun calculatingAfterError() { calc("5 ÷ 0 = 12 + 13 =").expect("25.00") }

    /** Inferred; the manual does not say when K ends. */
    @Test fun constantDoesNotHijackNextCalculation() { calc("5 + 2 K = 10 × 4 =").expect("40.00") }

    /** Manual ch. 2: with no entry in progress, C "cancels the current calculation". */
    @Test fun clearDropsPendingOperation() { calc("5 + C 7 =").expect("7.00") }

    @Test fun divideByZeroIsError() { check(calc("5 ÷ 0 =").vm.state.value.isError) }

    /** Inferred; the manual always enters all four known values. */
    @Test fun solvingWithUnsetRegistersUsesZero() {
        // Fresh calculator: PMT was never entered. 1000 at 10% for 1 year.
        calc("1 o:P/YR 1 N 10 I/YR -1000 PV FV").expect("1,100.00")
    }

    /** Same behavior Table 6-3 relies on. */
    @Test fun pendingExpressionCompletesWhenStored() { calc("12 o:P/YR 2 × 18 N C RCL N").expect("36.00") }

    @Test fun solvedValueCanBeStoredInAnotherRegister() {
        // Solve PMT, then PV must solve again (not be overwritten by the displayed PMT).
        calc("36 N 10.5 I/YR 13000 PV 0 FV PMT PV").expect("13,000.00")
    }

    // ── More precision and pending-operation cases ───────────────────────────

    @Test fun decimalNoiseDoesNotShow() { calc(".1 + .2 - .3 =").expect("0.00") }

    @Test fun rndAgreesWithDisplay() { calc("1 ÷ 8 = o:RND o:DISP 4").expect("0.1300") }

    @Test fun functionResultIsUsedAsOperand() { calc("2 + 9 o:√x + 1 =").expect("6.00") }

    @Test fun recalledValueIsUsedAsOperand() { calc("7 →M C 10 + RM × 2 =").expect("34.00") }

    @Test fun recalledStatisticIsUsedAsOperand() { calc("4 Σ+ 6 Σ+ 10 + b:Σx × 2 =").expect("40.00") }

    @Test fun changingOperatorReplacesIt() { calc("5 + × 3 =").expect("15.00") }

    @Test fun changingDispMidCalculationKeepsPendingOperation() { calc("5 + o:DISP 4 3 =").expect("8.0000") }

    /** Manual ch. 2: with entry in progress C clears only the entry. */
    @Test fun clearDuringEntryKeepsCalculation() { calc("5 + 3 C 4 =").expect("9.00") }

    /** Manual ch. 2: C "clears the message and restores the original contents of the display". */
    @Test fun clearAfterErrorRestoresDisplay() { calc("7 + 2 = -4 o:√x C").expect("-4.00") }

    @Test fun clearCancelsConstant() { calc("5 + 2 K = C 7 =").expect("7.00") }

    @Test fun resultPlusTypedNumberStartsFresh() { calc("5 + 2 = 8 + 1 =").expect("9.00") }

    @Test fun typedNumberSurvivesDispBeforeStore() { calc("36.5 o:DISP 0 N C RCL N o:DISP 2").expect("36.50") }

    @Test fun amortizationInputDoesNotLeakIntoStatistics() {
        calc("36 N 10.5 I/YR 13000 PV 0 FV PMT 1 INPUT o:AMORT C 5 Σ+ b:Σx").expect("5.00")
    }

    @Test fun computedXValueInStatisticsPair() { calc("20 ÷ 2 = INPUT 3 Σ+ b:Σxy").expect("30.00") }

    @Test fun dividingByZeroStatePressedOn() { calc("5 ÷ 0 = C C 2 × 3 =").expect("6.00") }

    // ── Percent: ÷100, unless it follows + or - ──────────────────────────────

    @Test fun percentAlone() { calc("25 %").expect("0.25") }

    @Test fun divideByPercent() { calc("50 ÷ 25 %").expect("0.25").press("=").expect("200.00") }

    @Test fun percentOfRunningTotal() { calc("100 + 10 % + 5 % =").expect("115.50") }

    @Test fun percentAfterChain() { calc("6 + 4 × 10 % =").expect("1.00") }

    @Test fun percentKeepsPrecision() { calc("1 ÷ 3 = - 10 % = × 3 =").expect("0.90") }

    @Test fun percentResultCanBeStored() { calc("750 % I/YR C RCL I/YR").expect("7.50") }

    @Test fun editedPercentIsPlainConstant() { calc("10 + 10 % 3 K = 20 =").expect("23.00") }

    // ── Break-even with a profit target ──────────────────────────────────────

    @Test fun breakEvenSolvesEachUnknownConsistently() {
        val base = "150000 b:FC 250 b:VC 300 b:SP 10000 b:PROFIT 3200 b:UNITS"
        calc("$base b:FC").expect("150,000.00").press("b:VC").expect("250.00").press("b:SP").expect("300.00")
            .press("b:PROFIT").expect("10,000.00").press("b:UNITS").expect("3,200.00")
    }

    // ── INPUT ────────────────────────────────────────────────────────────────

    @Test fun secondNumberOfAPairCanBeAnExpression() {
        calc("5 INPUT 1 + 2 b:nPr").expect("60.00")
        calc("10 INPUT 2 × 3 Σ+ b:Σy").expect("6.00")
        calc("100 INPUT 1 + 1 CFj").press("0 I/YR o:NPV").expect("200.00")
    }

    @Test fun twoNumberResultContinues() { calc("5 INPUT 3 b:nCr + 1 =").expect("11.00") }

    @Test fun twoNumberFunctionUsesUpInput() { calc("5 INPUT 3 b:nCr C 4 Σ+ b:Σx").expect("4.00") }

    @Test fun inputDoesNotChangeAmortizationPeriods() {
        val loan = "36 N 10.5 I/YR 13000 PV 0 FV PMT"
        val first = calc("$loan o:AMORT =").display
        calc("$loan 7 INPUT 8 Σ+ o:AMORT =").expect(first)
    }

    @Test fun backwardsAmortizationRangeIsError() { check(calc("36 N 10.5 I/YR 13000 PV 0 FV PMT 12 INPUT 1 o:AMORT").vm.state.value.isError) }

    @Test fun sigmaMinusRemovesTypedPair() {
        calc("1 INPUT 2 Σ+ 3 INPUT 4 Σ+ 1 INPUT 2 o:Σ-").expect("1.00").press("b:Σx").expect("3.00").press("b:Σy").expect("4.00")
    }

    @Test fun sigmaMinusAloneStillUndoesLastPair() {
        calc("1 INPUT 2 Σ+ 3 INPUT 4 Σ+ o:Σ-").expect("1.00").press("b:Σx").expect("1.00")
    }

    // ── Inverse probability and depreciation ─────────────────────────────────

    @Test fun inverseProbabilityRejectsImpossibleInput() {
        for (p in listOf("0", "1", "1.5", "-.1")) check(calc("$p b:INV 3").vm.state.value.isError) { p }
    }

    @Test fun invLastsOneKey() { calc("o:DISP 4 .35 b:INV × C 30 b:SIN").expect("0.5000") }

    @Test fun decliningBalanceNeedsAFactor() { check(calc("5000 PV 7 N 0 FV 1 b:DB").vm.state.value.isError) }

    @Test fun depreciationEqualsCycleShowsRemainingDepreciableValue() {
        calc("4000 PV 4 N 1000 FV 3 b:SOYD").expect("600.00").press("=").expect("300.00").press("=").expect("2,700.00")
    }

    @Test fun depreciationSwapTogglesBack() { calc("4000 PV 4 N 1000 FV 3 b:SOYD o:SWAP o:SWAP").expect("600.00") }

    // ── I/YR and IRR ─────────────────────────────────────────────────────────

    /** 10,000 saved in 30 years at 7.5%: the kind of problem the old solver gave up on. */
    @Test fun rateOfLongTermSavings() { calc("360 N 7.5 I/YR 0 PV 10000 FV PMT I/YR").expect("7.50") }

    @Test fun rateOfInterestFreeLoan() { calc("12 N 1200 PV -100 PMT 0 FV I/YR").expect("0.00") }

    /** The payment is only exact to 12 digits; that must not show up as a tiny rate. */
    @Test fun rateOfInterestFreeLoan_repeatingDecimalPayment() { calc("3 N -1000 PV 1000 ÷ 3 = PMT 0 FV I/YR").expect("0.00") }

    @Test fun rateOfLosingInvestment() { calc("1 o:P/YR 10 N -1000 PV 0 PMT 800 FV I/YR").expect(-2.2067, 0.005) }

    /** Ten years of rent, then a sale: 0.2% a month. */
    @Test fun irrOfRentalProperty() { calc("-250000 CFj 1500 CFj 120 o:Nj 114756.81 CFj o:IRR/YR").expect("2.40") }

    @Test fun irrOfExactBreakEven() { calc("-250000 CFj 1500 CFj 120 o:Nj 70000 CFj o:IRR/YR").expect("0.00") }

    @Test fun impossibleRateIsError() {
        check(calc("12 N 1000 PV 100 PMT 0 FV I/YR").vm.state.value.isError) // only money received
        check(calc("12 N 0 PV 0 PMT 1000 FV I/YR").vm.state.value.isError)
    }

    // ── Locales ──────────────────────────────────────────────────────────────

    @Test fun everyLocaleGivesTheSameAnswers() {
        val before = Locale.getDefault()
        val sequences = listOf(
            "1234 + 1 =", "1234567.89 × 2 =", "10000000 × 10000000 =", ".0001 ÷ 7 =", "-9876.5 - .5 =",
            "360 N 7.5 I/YR 133006.39 PV 0 FV PMT", "12345 ← ←", "1 INPUT 12 Σ+ b:Σxy",
        )
        try {
            Locale.setDefault(Locale.US)
            val expected = sequences.map { calc(it).display }
            for (tag in listOf("de-DE", "fr-FR", "es-ES", "pt-BR", "de-CH", "en-IN", "tr-TR", "ar-EG", "ar-SA-u-nu-arab", "fa-IR", "hi-IN-u-nu-deva", "th-TH-u-nu-thai")) {
                Locale.setDefault(Locale.forLanguageTag(tag))
                sequences.zip(expected).forEach { (keys, want) -> assertEquals("$tag: $keys", want, calc(keys).display) }
            }
        } finally { Locale.setDefault(before) }
    }

    // ── Bonds ────────────────────────────────────────────────────────────────

    private val treasury = "4.282010 b:SetDate 6.042020 b:MatDate 6.75 b:CPN% 4.75 b:YTM"

    /** C MEM 7; "C MEM" contains a space, so it cannot be typed as a token. */
    private fun Calc.bondClear(): Calc = apply { vm.onKeyPress(KeyAction.CMEM_ARM); press("7") }

    @Test fun bondYieldInvertsPrice() { calc("$treasury b:PRICE b:YTM").expect("4.75") }

    @Test fun bondIgnoresLeftoverTvmPeriods() { calc("360 N $treasury b:PRICE").expect("115.89") }

    @Test fun bondDatesCanBeRecalled() {
        calc(treasury).press("b:SetDate").expect("4.282010").press("b:MatDate").expect("6.042020").press("b:PRICE").expect("115.89")
    }

    /** Stored dates are dates, not digits: changing the entry format must not change the bond. */
    @Test fun bondDatesSurviveDateFormatChange() {
        calc("$treasury b:D.MY/M.DY b:PRICE").expect("115.89").press("b:SetDate").expect("28.042010")
    }

    @Test fun bondDayMonthEntry() { calc("b:D.MY/M.DY 28.042010 b:SetDate 4.062020 b:MatDate 6.75 b:CPN% 4.75 b:YTM b:PRICE").expect("115.89") }

    @Test fun bondValuesCanBeCalculated() { calc("4.282010 b:SetDate 6.042020 b:MatDate 6 + .75 b:CPN% 9.5 ÷ 2 = b:YTM b:PRICE").expect("115.89") }

    @Test fun dateResultCanBecomeABondDate() { calc("4.272010 INPUT 1 o:DATE b:SetDate").press("b:SetDate").expect("4.282010") }

    @Test fun bondAtParPricesAtPar() { calc("4.152012 b:SetDate 10.152020 b:MatDate 5 b:CPN% 5 b:YTM b:PRICE").expect("100.00").press("b:AccInt").expect("0.00") }

    @Test fun bondCalendarAndScheduleMatter() {
        // Two months into an annual coupon of 6 on the 30/360 calendar: exactly 1.00
        calc("b:360/Act b:Semi/Ann 3.152012 b:SetDate 1.152020 b:MatDate 6 b:CPN% b:AccInt").expect("1.00")
        calc("b:Semi/Ann 3.152012 b:SetDate 1.152020 b:MatDate 6 b:CPN% b:AccInt").expect(6 * 60 / 366.0, 0.005)
    }

    @Test fun bondMissingOrBadDatesAreErrors() {
        for (keys in listOf(
            "4.282010 b:SetDate 6.75 b:CPN% 4.75 b:YTM b:PRICE",                           // no maturity
            "6.75 b:CPN% b:AccInt",                                                          // no dates
            "6.042020 b:SetDate 4.282010 b:MatDate 6.75 b:CPN% 4.75 b:YTM b:PRICE",       // matures first
            "4.282010 b:SetDate 4.282010 b:MatDate 6.75 b:CPN% 4.75 b:YTM b:PRICE",       // same day
            "2.302010 b:SetDate", "13.012010 b:MatDate",
        )) check(calc(keys).vm.state.value.isError) { "no error for $keys" }
    }

    @Test fun bondClearResetsCallAndDates() {
        calc("$treasury 104 b:CALL b:PRICE").bondClear().press("b:CALL").expect("100.00")
        check(calc(treasury).bondClear().press("6.75 b:CPN% 4.75 b:YTM b:PRICE").vm.state.value.isError)
    }

    @Test fun impossibleBondPriceIsError() { check(calc("$treasury -50 b:PRICE b:YTM").vm.state.value.isError) }

    @Test fun bondsNeedDatesEvenIfNIsSet() {
        for (keys in listOf("20 N 8 b:CPN% 9 b:YTM b:PRICE", "20 N 8 b:CPN% 93.5 b:PRICE b:YTM"))
            check(calc(keys).vm.state.value.isError) { keys }
    }

    // ── SWAP ─────────────────────────────────────────────────────────────────

    private val pairs = "1 INPUT 3 Σ+ 2 INPUT 5 Σ+ 4 INPUT 9 Σ+" // y = 2x + 1

    @Test fun swapTogglesStatisticsBothWays() {
        calc("$pairs o:x̄,ȳ").expect("2.33").press("o:SWAP").expect("5.67").press("o:SWAP").expect("2.33")
    }

    /** Going back must show the prediction for 10 again, not a prediction for the slope. */
    @Test fun swapReturnsToThePrediction() {
        calc("$pairs 10 o:ŷ,m").expect("21.00").press("o:SWAP").expect("2.00").press("o:SWAP").expect("21.00")
        calc("$pairs 21 o:x̂,r").expect("10.00").press("o:SWAP").expect("1.00").press("o:SWAP").expect("10.00")
    }

    @Test fun repeatedKeyAlsoReturnsToThePrediction() { calc("$pairs 10 o:ŷ,m o:ŷ,m o:ŷ,m").expect("21.00") }

    /** Weighted mean is undefined here (weights sum to 0), but the intercept is not. */
    @Test fun swapReachesSecondValueWhenFirstIsUndefined() {
        val c = calc("1 INPUT -1 Σ+ 2 INPUT 0 Σ+ 3 INPUT 1 Σ+ o:x̄w,b")
        check(c.vm.state.value.isError)
        c.press("o:SWAP").expect("-2.00")
    }

    @Test fun swapOfStatisticsEndsWithOtherKeys() { calc("$pairs o:x̄,ȳ + 1 = o:SWAP").expect("3.33") }

    @Test fun swapReversesPendingOperation() { calc("2 ÷ 10 o:SWAP =").expect("5.00") }

    @Test fun swapReversesInputPair() { calc("3 INPUT 5 o:SWAP b:nCr").expect("10.00") }

    @Test fun swapWithNothingToSwapKeepsDisplay() { calc("5 + 2 = o:SWAP").expect("7.00") }

    // ── Margin and markup ────────────────────────────────────────────────────

    @Test fun marginSolvesEveryUnknown() {
        calc("255 CST 300 PRC MAR").expect("15.00").press("MU").expect("17.65")
        calc("255 CST 15 MAR PRC").expect("300.00").press("MU").expect("17.65")
        calc("300 PRC 15 MAR CST").expect("255.00")
    }

    @Test fun markupSolvesEveryUnknown() {
        calc("20 CST 26.60 PRC MU").expect("33.00")
        calc("20 CST 33 MU PRC").expect("26.60")
        calc("26.60 PRC 33 MU CST").expect("20.00")
    }

    /** The rate entered last is the one that counts. */
    @Test fun lastRateEnteredWins() {
        calc("100 CST 50 MU 50 MAR PRC").expect("200.00")
        calc("100 CST 50 MAR 50 MU PRC").expect("150.00")
    }

    @Test fun rateSurvivesNewCost() { calc("255 CST 15 MAR PRC 510 CST PRC").expect("600.00") }

    @Test fun marginValuesCanBeRecalled() {
        calc("255 CST 300 PRC 15 MAR C RCL CST").expect("255.00").press("RCL PRC").expect("300.00").press("RCL MAR").expect("15.00")
        calc("60 MU C RCL MU").expect("60.00")
    }

    @Test fun marginAcceptsCalculatedValues() { calc("200 + 55 CST 30 × 10 = PRC MAR").expect("15.00") }

    @Test fun marginErrors() {
        for (keys in listOf("CST", "PRC", "MAR", "MU", "255 CST PRC", "255 CST 100 MAR PRC", "0 CST 5 PRC MU"))
            check(calc(keys).vm.state.value.isError) { "no error for $keys" }
    }

    // ── Storing calculated numbers ───────────────────────────────────────────

    @Test fun effectiveRateTypedAndConverted() {
        calc("12 o:P/YR 8.3 o:EFF%").expect("8.30").press("o:NOM%").expect("8.00").press("RCL I/YR").expect("8.00")
    }

    @Test fun ratesConvertBackAndForth() { calc("10 o:NOM% 12 o:P/YR o:EFF%").expect("10.47").press("o:NOM%").expect("10.00").press("o:EFF%").expect("10.47") }

    @Test fun calculatedRateCanBeStored() { calc("4 + 4 = o:NOM% 12 o:P/YR o:EFF%").expect("8.30") }

    @Test fun effUsesTypedIYR() { calc("12 o:P/YR 6 I/YR o:EFF%").expect("6.17") }

    @Test fun yearsCanBeAnExpression() {
        calc("12 o:P/YR 20 + 10 o:×P/YR").expect("360.00").press("6 I/YR 200000 PV 0 FV PMT").expect("-1,199.10")
    }

    @Test fun partialYearsAreNotCutOff() { calc("365 o:P/YR 2.5 o:×P/YR").expect("912.50") }

    @Test fun calculatedPaymentsPerYear() { calc("2 × 2 = o:P/YR C o:P/YR").expect("4.00") }

    /** Looking at P/YR must not turn it into a number that the next key stores. */
    @Test fun showingPaymentsPerYearDoesNotStoreThem() { calc("12 o:P/YR 360 N 6 I/YR 200000 PV 0 FV C o:P/YR PMT").expect("-1,199.10") }

    @Test fun recalledRegisterCanBeStored() {
        calc("250000 o:STO 1 C RCL 1 PV C RCL PV").expect("250,000.00")
        calc("7 o:STO .5 8 o:STO 5 C RCL .5").expect("7.00").press("RCL 5").expect("8.00")
    }

    /** Verifying a TVM register with RCL must not make the next key store it. */
    @Test fun recallingTvmThenSolving() { calc("12 o:P/YR 360 N 6 I/YR 200000 PV 0 FV RCL PV PMT").expect("-1,199.10") }

    @Test fun negatedResultCanBeStored() { calc("1 o:P/YR 10 N 5 I/YR -1000 PV 0 PMT FV +/- PV C RCL PV").expect("-1,628.89") }

    @Test fun breakEvenAcceptsCalculatedValues() {
        calc("100000 + 50000 = b:FC 250 b:VC 200 + 100 b:SP 0 b:PROFIT b:UNITS").expect("3,000.00")
            .press("100000 + 100000 = b:FC b:UNITS").expect("4,000.00")
    }

    @Test fun repeatCountCanBeAnExpression() { calc("1 o:P/YR -100 CFj 60 CFj 3 - 1 o:Nj 0 I/YR o:NPV").expect("20.00") }

    @Test fun paidOffBalanceIsZero() {
        calc("12 o:P/YR 180 N 7.905 I/YR 1477429.72 PV 0 FV PMT 170 INPUT 180 o:AMORT = = =").expect("0.00")
        calc("572971.21 PV 116368.69 FV 5 N 5 b:SL o:SWAP").expect("0.00")
    }

    // ── Parentheses and Algebraic mode (cases the manual leaves open) ────────

    @Test fun adjacentParenthesesMultiply() { calc("o:( 2 + 3 o:) o:( 4 + 5 o:) =").expect("45.00") }

    @Test fun parenthesesWithoutFirstNumberAreErrors() {
        for (keys in listOf("5 × o:( ×", "5 × o:( o:)", "5 × o:( 2 + o:)", "5 × o:( ="))
            check(calc(keys).vm.state.value.isError) { keys }
    }

    @Test fun signAfterOpenParenthesis() { calc("5 × o:( - 3 + 1 o:) =").expect("-10.00") }

    @Test fun unmatchedCloseDoesNothing() { calc("5 + 3 o:) =").expect("8.00") }

    @Test fun parenthesesAroundASingleNumber() { calc("2 × o:( 5 o:) =").expect("10.00").press("7 =").expect("7.00") }

    @Test fun clearCancelsOpenParentheses() { calc("5 × o:( 2 + C C 7 =").expect("7.00") }

    @Test fun clearInsideParenthesesClearsOnlyTheEntry() { calc("5 × o:( 2 + 9 C 3 o:) =").expect("25.00") }

    @Test fun errorCancelsOpenParentheses() { calc("5 × o:( 2 ÷ 0 o:) C C 7 + 1 =").expect("8.00") }

    @Test fun expressionInParenthesesCanBeStored() {
        calc("12 o:P/YR 360 N 6 I/YR 250000 × o:( 1 - .2 PV 0 FV PMT").expect("-1,199.10").press("RCL PV").expect("200,000.00")
        calc("250000 × o:( 1 - .2 o:) PV C RCL PV").expect("200,000.00")
        calc("b:Alg/Chain 50000 + 3 × 50000 PV C RCL PV").expect("200,000.00")
    }

    @Test fun inputPairCanUseParentheses() { calc("5 INPUT o:( 1 + 2 o:) b:nPr").expect("60.00") }

    @Test fun percentInsideParentheses() { calc("2 × o:( 50 + 10 % o:) =").expect("110.00") }

    @Test fun percentAddOnInAlgebraicMode() { calc("b:Alg/Chain 200 + 100 + 10 % =").expect("330.00") }

    @Test fun replacedOperatorUsesTheNewPriority() {
        calc("b:Alg/Chain 1 + 2 × + 3 =").expect("6.00")
        calc("b:Alg/Chain 1 + 2 - × 3 =").expect("9.00") // the sum was already complete when - was pressed
        calc("b:Alg/Chain 4 + 3 C × 5 =").expect("20.00")
    }

    @Test fun algebraicShowsIntermediateResults() { calc("b:Alg/Chain 1 + 2 × 3 ×").expect("6.00").press("4 +").expect("25.00") }

    // ── Scientific notation entry ────────────────────────────────────────────
    // The manual has the E key itself (see ManualExamplesTest); these edge cases are inferred.

    @Test fun exponentIsShownWhileTyped() {
        calc("1.5 o:E").expect("1.5E0").press("1").expect("1.5E1").press("4").expect("1.5E14").press("=").expect("1.50E14")
    }

    @Test fun signAfterEChangesTheExponent() {
        calc("5 o:E 3 +/-").expect("5E-3").press("+/-").expect("5E3")
        calc("5 o:E +/- 3").expect("5E-3").press("× 1000 =").expect("5.00")
    }

    @Test fun signBeforeEChangesTheMantissa() { calc("-5 o:E 3 =").expect("-5,000.00") }

    @Test fun exponentNumberIsAnOperand() {
        calc("2 o:E 3 + 1 =").expect("2,001.00")
        calc("6 ÷ 2 o:E 3 +/- =").expect("3,000.00")
    }

    @Test fun exponentNumberCanBeStored() {
        calc("2 o:E 5 PV C RCL PV").expect("200,000.00")
        calc("2 o:E 5 →M C RM").expect("200,000.00")
    }

    @Test fun unfinishedExponentIsZero() { calc("7 o:E N C RCL N").expect("7.00") }

    @Test fun groupedMantissaKeepsItsValue() { calc("12345 o:E 2 =").expect("1,234,500.00") }

    @Test fun eWithoutNumberStartsFromOne() {
        calc("o:E 3 =").expect("1,000.00")
        calc("5 + o:E 2 =").expect("105.00")
        calc("2 + 3 = o:E 2 =").expect("100.00")
    }

    @Test fun secondEAndDecimalInExponentAreIgnored() { calc("1 o:E 2 o:E . 3").expect("1E23") }

    @Test fun backspaceWalksBackThroughTheExponent() {
        calc("1234 o:E 14 +/- ←").expect("1234E-1").press("←").expect("1234E-0").press("←").expect("1234E0")
            .press("←").expect("1,234").press("5 =").expect("12,345.00")
    }

    @Test fun clearDuringExponentClearsTheEntry() { calc("5 + 1 o:E 14 C 4 =").expect("9.00") }

    @Test fun exponentOutOfRangeIsIgnored() {
        calc("1 o:E 400").expect("1E40")
        calc("1 o:E +/- 400").expect("1E-40")
        calc("1000 o:E 305 +/- +/-").expect("1000E305")
        calc("1000 o:E +/- 310 +/-").expect("1000E-310")
        calc("0 o:E 9999").expect("0E999")
    }

    // ── Display ──────────────────────────────────────────────────────────────

    @Test fun halfRoundsAwayFromZero() {
        calc("1 ÷ 8 =").expect("0.13")
        calc("5 ÷ 8 = o:DISP 2").expect("0.63")
    }

    @Test fun halfRoundsAwayFromZero_zeroPlaces() { calc("o:DISP 0 5 ÷ 2 =").expect("3") }

    @Test fun impossiblePowerIsError() { check(calc("-8 o:y^x .5 =").vm.state.value.isError) }

    @Test fun overflowingFactorialIsError() { check(calc("171 o:n!").vm.state.value.isError) }

    @Test fun displayWorksInCommaDecimalLocale() {
        val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals(1235.0, calc("1234 + 1 =").value(), 1e-9)
        } finally { Locale.setDefault(before) }
    }

    @Test fun scientificDisplayWorksInCommaDecimalLocale() {
        val before = Locale.getDefault()
        try {
            Locale.setDefault(Locale.GERMANY)
            assertEquals("1.00E14", CalculatorEngine.formatDisplay(1e14, 2))
        } finally { Locale.setDefault(before) }
    }

    // ── Sign and grouping while typing ───────────────────────────────────────

    @Test fun signOfAFractionSurvivesMoreDigits() {
        calc("0.5 +/- 3").expect("-0.53").press("=").expect("-0.53")
        calc("0.5 +/- 3 PV C RCL PV").expect("-0.53")
        calc("0.05 +/- ←").expect("-0.0").press("7 =").expect("-0.07")
    }

    @Test fun signChangeKeepsCommas() {
        calc("1234567 +/-").expect("-1,234,567").press(". 5").expect("-1,234,567.5").press("+/-").expect("1,234,567.5")
    }

    @Test fun largeResultsShowCents() {
        calc("312.0 - 859 × 899.9 × 177 =").expect("-87,127,418.10")
        calc("25000000 +/- + 0.25 =").expect("-24,999,999.75")
    }
}
