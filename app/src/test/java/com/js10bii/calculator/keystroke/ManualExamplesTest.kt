package com.js10bii.calculator.keystroke

import com.js10bii.calculator.engine.KeyAction
import com.js10bii.calculator.support.Calc
import org.junit.Test

/**
 * Worked examples from the HP 10bII+ User's Guide (NW239-90001, Edition 1, c02989763.pdf),
 * typed key by key. Table numbers refer to that document; expected values are copied from it
 * unless a test says otherwise.
 */
class ManualExamplesTest {

    private fun calc(keys: String) = Calc().press(keys)

    // ── Ch. 2 Arithmetic ─────────────────────────────────────────────────────

    @Test fun table2_3_addition() { calc("24.71 + 62.47 =").expect("87.18") }

    @Test fun table2_4_and_2_5_continueAfterEquals() {
        calc("19 × 12.68 =").expect("240.92").press("+ 115.5 =").expect("356.42")
    }

    @Test fun table2_6_chainIntermediateResult() {
        calc("6.9 × 5.35 ÷").expect("36.92").press(".91 =").expect("40.57")
    }

    @Test fun table2_7_chainLeftToRight() { calc("4 + 9 ×").expect("13.00").press("3 =").expect("39.00") }

    @Test fun chainMode_1plus2times3() { calc("1 + 2 × 3 =").expect("9.00") }

    @Test fun algebraicMode_1plus2times3() { calc("b:Alg/Chain 1 + 2 × 3 =").expect("7.00") }

    @Test fun table2_8_parentheses() {
        calc("30 ÷ o:( 85 - 12 o:)").expect("73.00").press("×").expect("0.41").press("9 =").expect("3.70")
    }

    /** Ch. 2: "entering 25 ÷ (3 × (9 + 12 = is equivalent to 25 ÷ (3 × (9 + 12)) =". */
    @Test fun parentheses_mayBeLeftOpen() {
        val want = 25.0 / 63
        calc("o:DISP 6 25 ÷ o:( 3 × o:( 9 + 12 =").expect(want, 5e-7)
        calc("o:DISP 6 25 ÷ o:( 3 × o:( 9 + 12 o:) o:) =").expect(want, 5e-7)
    }

    @Test fun parentheses_overrideChainOrder() { calc("1 + o:( 2 × 3 o:) =").expect("7.00") }

    @Test fun parentheses_impliedMultiplication() { calc("53 o:( 2 + 1 o:) =").expect("159.00") }

    @Test fun parentheses_fourMayBeOpen() {
        calc("2 × o:( 2 × o:( 2 × o:( 2 × o:( 2 + 1 =").expect("48.00")
        check(calc("2 × o:( 2 × o:( 2 × o:( 2 × o:( 2 × o:(").vm.state.value.isError)
    }

    /** Ch. 2 priorities: two-number functions, y^x, × ÷, + −. */
    @Test fun algebraicMode_priorities() {
        calc("b:Alg/Chain 2 + 3 × 4 o:y^x 2 =").expect("50.00")
        calc("b:Alg/Chain 2 × 5 b:nCr 3 =").expect("20.00")
        calc("b:Alg/Chain 100 - 12 ÷ 4 - 2 =").expect("95.00")
    }

    /** Ch. 4 note: RCL R14 then =. */
    @Test fun table4_7_algebraic() {
        calc("b:Alg/Chain 475.6 o:STO .4 ÷ 39.15 o:STO 2 =").expect("12.15").press("560.1 + RCL .4 =").expect("1,035.70")
            .press("÷ RCL 2 =").expect("26.45")
    }

    @Test fun table2_20_percentChangeInline() { calc("17 o:%CHG 29 =").expect("70.59") }

    @Test fun table2_21_percentChangeInput() { calc("17 INPUT 29 o:%CHG").expect("70.59") }

    @Test fun scientificNotationResult() { calc("10000000 × 10000000 =").expect("1.00E14") }

    @Test fun scientificNotationEntry() { calc("1 o:E 14 = o:DISP 2").expect("1.00E14") }

    // ── Ch. 2 Precision: "stores and calculates using 12-digit numbers" ──────

    @Test fun precision_resultKeepsFullPrecision() { calc("1 ÷ 3 = × 3 =").expect("1.00") }

    @Test fun precision_table2_30_tenSeventhsTimesSeven() { calc("10 ÷ 7 = × 7 =").expect("10.00") }

    /** 9.87654321 shown at 2 places is still 9.87654321 underneath. */
    @Test fun precision_changingDispDoesNotRound() {
        calc("9.87654321 o:DISP 2 o:DISP 8").expect("9.87654321")
    }

    @Test fun precision_table2_31_rndDoesRound() {
        calc("9.87654321 o:DISP 2 o:RND o:DISP 8").expect("9.88000000")
    }

    // ── Ch. 2 Trigonometry. INV and HYP leave the blue shift on, so "SIN" is the unshifted ÷ key. ──

    @Test fun table2_16_trig() {
        calc("o:DISP 4 15 b:SIN").expect("0.2588")
        calc("o:DISP 4 1 + 60 b:TAN").expect("1.7321").press("=").expect("2.7321")
    }

    @Test fun table2_16_inverseTrig() {
        calc("o:DISP 4 .35 b:INV ×").expect("69.5127").press("- .62 b:INV ×").expect("51.6839").press("=").expect("17.8288")
    }

    @Test fun table2_19_hyperbolic() {
        calc("o:DISP 4 1.25 b:HYP ÷").expect("1.6019").press("+ .59 b:HYP -").expect("0.5299")
            .press("=").expect("2.1318").press("b:HYP M+ ×").expect("1.3899")
    }

    /** Pressing blue again out of habit must not lose INV. */
    @Test fun inverseTrig_withExtraBlueShift() { calc("o:DISP 4 .35 b:INV b:COS").expect("69.5127") }

    // ── Ch. 3 Business percentages ───────────────────────────────────────────

    @Test fun table3_1_findPercent() { calc("200 × 25 %").expect("0.25").press("=").expect("50.00") }

    @Test fun table3_2_subtractPercent() { calc("200 - 25 %").expect("50.00").press("=").expect("150.00") }

    @Test fun table3_3_addPercent() { calc("1250 + 7 % =").expect("1,337.50") }

    @Test fun table3_4_percentChange() { calc("291.7 o:%CHG 316.8 =").expect("8.60") }

    @Test fun table3_5_percentChangeOfExpressions() { calc("12 × 5 INPUT 65 + 18 o:%CHG").expect("38.33") }

    @Test fun table3_7_margin() { calc("255 CST 300 PRC MAR").expect("15.00") }

    @Test fun table3_8_retailPrice() { calc("19 CST 60 MU PRC").expect("30.40") }

    @Test fun table3_9_marginAndMarkup() { calc("9.60 CST 15 MU PRC").expect("11.04").press("MAR").expect("13.04") }

    /** From the definition in Table 3-6 (not a worked example): price 300 at 15% margin costs 255. */
    @Test fun margin_solvesCost() { calc("300 PRC 15 MAR CST").expect("255.00") }

    /** Likewise inferred: cost 255 at 15% margin sells for 300. */
    @Test fun margin_solvesPrice() { calc("255 CST 15 MAR PRC").expect("300.00") }

    // ── Ch. 4 Constants ──────────────────────────────────────────────────────

    @Test fun table4_1_addConstant() {
        calc("5 + 2 K").expect(2.0, 0.0).press("=").expect("7.00").press("6 =").expect("8.00").press("7 =").expect("9.00")
    }

    @Test fun table4_2_percentConstant() {
        calc("10 + 10 % K").expect("1.00").press("=").expect("11.00").press("11 =").expect("12.10").press("25 =").expect("27.50")
    }

    @Test fun table4_3_powerConstant() { calc("2 o:y^x 3 K =").expect("8.00").press("4 =").expect("64.00") }

    // ── Ch. 6 Time value of money ────────────────────────────────────────────

    private val carLoan = "12 o:P/YR 36 N 10.5 I/YR 13000 PV 0 FV"

    @Test fun table6_2_carLoanPayment() { calc("$carLoan PMT").expect("-422.53") }

    @Test fun table6_3_carLoanRate() { calc("$carLoan PMT + 50 PMT").expect("-372.53").press("I/YR").expect("2.03") }

    @Test fun table6_4_carLoanAmount() {
        calc("$carLoan -375 PMT PV").expect("11,537.59").press("+ 1500 =").expect("13,037.59")
    }

    @Test fun table6_5_homeMortgage() {
        calc("12 o:P/YR 30 o:×P/YR 0 FV 7.5 I/YR -930 PMT PV").expect("133,006.39").press("+ 12000 =").expect("145,006.39")
    }

    @Test fun table6_10_ira() {
        calc("24 o:P/YR -2000 PV -80 PMT 6.3 I/YR 15 o:×P/YR FV").expect("52,975.60")
    }

    @Test fun table6_11_annuityBegin() {
        calc("o:Beg/End 12 o:P/YR -400000 PV 7 I/YR 50 o:×P/YR 0 FV PMT").expect("2,392.80")
    }

    @Test fun table6_12_leaseBegin() {
        calc("o:Beg/End 12 o:P/YR 10 I/YR -13500 PV 7500 FV 36 N PMT").expect("253.99")
    }

    // ── Ch. 6 Amortization ───────────────────────────────────────────────────

    // The manual's own figures disagree by up to 3 cents (year 1 sums to 12 unrounded payments,
    // year 2 to 12 rounded ones), so cent-exact matches are not meaningful here.
    private val AMORT_TOL = 0.05

    private val mortgage = "12 o:P/YR 30 o:×P/YR 7.75 I/YR 180000 PV 0 FV PMT"

    @Test fun table6_18_mortgagePayment() { calc(mortgage).expect("-1,289.54") }

    @Test fun table6_19_amortizeYear1() {
        calc("$mortgage 1 INPUT 12 o:AMORT =").expect(-1579.84, AMORT_TOL)
            .press("=").expect(-13894.67, AMORT_TOL).press("=").expect(178420.16, AMORT_TOL)
    }

    @Test fun table6_20_amortizeYear2() {
        calc("$mortgage 13 INPUT 24 o:AMORT =").expect(-1706.69, AMORT_TOL)
            .press("=").expect(-13767.79, AMORT_TOL).press("=").expect(176713.49, AMORT_TOL)
    }

    private val carLease = "o:Beg/End 12 o:P/YR 5 o:×P/YR 11.5 I/YR 14250 PV 0 FV PMT"

    @Test fun table6_22_leasePayment1_hasNoInterest() {
        calc(carLease).expect("-310.42").press("1 INPUT o:AMORT =").expect(-310.42, AMORT_TOL)
            .press("=").expect(0.00, AMORT_TOL).press("=").expect(13939.58, AMORT_TOL)
    }

    @Test fun table6_22_leasePayment25() {
        calc("$carLease 25 INPUT o:AMORT =").expect(-220.21, AMORT_TOL).press("=").expect(-90.21, AMORT_TOL).press("=").expect(9193.28, AMORT_TOL)
    }

    @Test fun table6_22_leasePayment54() {
        calc("$carLease 54 INPUT o:AMORT =").expect(-290.37, AMORT_TOL).press("=").expect(-20.05, AMORT_TOL).press("=").expect(1801.57, AMORT_TOL)
    }

    /** "If you ... perform any other calculations during amortization, pressing = will no longer cycle". */
    @Test fun amortization_otherCalculationEndsCycling() { calc("$mortgage 1 INPUT o:AMORT = 2 + 3 =").expect("5.00") }

    // ── Ch. 7 Depreciation ───────────────────────────────────────────────────

    /** C MEM then 1: typed as its own step because "C MEM" contains a space. */
    private fun depreciation(keys: String): Calc = Calc().apply {
        vm.onKeyPress(KeyAction.CMEM_ARM)
        press("1 $keys")
    }

    @Test fun table7_2_straightLine_amounts() {
        depreciation("10000 PV 500 FV 5 N 1 b:SL").expect("1,900.00").press("2 b:SL").expect("1,900.00")
    }

    @Test fun table7_2_straightLine() {
        depreciation("10000 PV 500 FV 5 N 1 b:SL").expect("1,900.00").press("o:SWAP").expect("7,600.00")
            .press("2 b:SL").expect("1,900.00").press("o:SWAP").expect("5,700.00")
    }

    @Test fun table7_3_soyd_amounts() {
        depreciation("4000 PV 4 N 1000 FV 1 b:SOYD").expect("1,200.00").press("3 b:SOYD").expect("600.00")
    }

    @Test fun table7_3_soyd_remaining() {
        depreciation("4000 PV 4 N 1000 FV 3 b:SOYD o:SWAP").expect("300.00")
    }

    @Test fun table7_4_decliningBalance() {
        depreciation("5000 PV 7 N 200 I/YR 0 FV 1 b:DB").expect("1,428.57")
            .press("2 b:DB").expect("1,020.41").press("3 b:DB").expect("728.86")
    }

    @Test fun table7_4_decliningBalance_remaining() {
        depreciation("5000 PV 7 N 200 I/YR 0 FV 3 b:DB o:SWAP").expect("1,822.16")
    }

    /** Step 4 of the procedure: 125% declining balance is entered as 125 I/YR. 5000 × 1.25/7. */
    @Test fun decliningBalance_factorComesFromIYR() {
        depreciation("5000 PV 7 N 125 I/YR 0 FV 1 b:DB").expect("892.86")
    }

    // ── Ch. 8 Cash flows ─────────────────────────────────────────────────────

    @Test fun table8_3_irr() {
        calc("12 o:P/YR -11000 CFj 0 CFj 6 o:Nj 1000 CFj 3 o:Nj 10000 CFj o:IRR/YR").expect("21.22")
    }

    @Test fun table8_8_npv_usingNj() {
        calc(
            "12 o:P/YR 0 CFj 4 o:Nj 5000 CFj 0 CFj 4 o:Nj 5000 CFj 2 o:Nj 0 CFj 4 o:Nj " +
                "7500 CFj 0 CFj 9 o:Nj 10000 CFj 15 I/YR o:NPV",
        ).expect("27,199.92")
    }

    /** The same table as printed: amount INPUT count CFj. */
    @Test fun table8_7_npv_usingInput() {
        calc(
            "12 o:P/YR 0 INPUT 4 CFj 5000 INPUT 1 CFj 0 INPUT 4 CFj 5000 INPUT 2 CFj 0 INPUT 4 CFj " +
                "7500 INPUT 1 CFj 0 INPUT 9 CFj 10000 INPUT 1 CFj 15 I/YR o:NPV",
        ).expect("27,199.92")
    }

    // ── Ch. 9 Dates ──────────────────────────────────────────────────────────

    @Test fun table9_4_daysInline_dmy() { calc("b:D.MY/M.DY o:DISP 6 4.062010 o:ΔDAYS 31.102010 =").expect("149.000000") }

    @Test fun table9_5_daysInline() { calc("10.172012 o:ΔDAYS 6.042015 =").expect("960.00") }

    @Test fun table9_6_daysInput() { calc("10.172012 INPUT 6.042015 o:ΔDAYS").expect("960.00") }

    /** SetDate is the bond settlement date only; it must not get in the way of date calculations. */
    @Test fun dateFunctionsIgnoreSetDate() {
        calc("1.012000 b:SetDate C 10.172012 o:ΔDAYS 6.042015 =").expect("960.00")
        calc("1.012000 b:SetDate C 12.182011 o:DATE 100 =").expect("3.272012")
        calc("1.012000 b:SetDate C 10.172012 INPUT 6.042015 o:ΔDAYS").expect("960.00")
    }

    // ── Ch. 10 Bonds ─────────────────────────────────────────────────────────

    @Test fun table10_2_bondPriceAndAccruedInterest() {
        calc("4.282010 b:SetDate 6.042020 b:MatDate 6.75 b:CPN% 100 b:CALL 4.75 b:YTM b:PRICE").expect("115.89")
            .press("+ b:AccInt").expect("2.69").press("=").expect("118.58")
    }

    @Test fun table10_3_yieldToCall() {
        calc("5.5 b:CPN% 104 b:CALL 101 b:PRICE 10.152020 b:MatDate 4.152012 b:SetDate b:YTM").expect("5.72")
    }

    @Test fun table10_4_yieldToMaturity() {
        calc("5.5 b:CPN% 101 b:PRICE 10.152020 b:MatDate 4.152012 b:SetDate 100 b:CALL b:YTM").expect("5.35")
    }

    // ── Ch. 11 Break-even ────────────────────────────────────────────────────

    @Test fun table11_2_unitsForProfit() {
        calc("150000 b:FC 250 b:VC 300 b:SP 10000 b:PROFIT b:UNITS").expect("3,200.00")
    }

    @Test fun table11_3_maximumFixedCost() {
        calc("10000 b:UNITS 45 b:SP 23 b:VC 0 b:PROFIT b:FC").expect("220,000.00")
    }

    // ── Ch. 12 Statistics (x INPUT y Σ+) ─────────────────────────────────────

    private val sailors = "4.5 Σ+ 4 Σ+ 2 Σ+ 3.25 Σ+ 3.5 Σ+ 3.75 Σ+"

    @Test fun table12_7_oneVariable() {
        calc(sailors).expect("6.00").press("o:x̄,ȳ").expect("3.50").press("o:Sx,Sy").expect("0.85")
            .press("b:Σx²").expect("77.13").press("÷ b:n").expect("6.00").press("=").press("o:√x").expect("3.59")
    }

    private val players = "193 INPUT 90 Σ+ 182 INPUT 81 Σ+ 177 INPUT 83 Σ+ 185 INPUT 77 Σ+"

    @Test fun table12_8_meanOfX() { calc("$players o:x̄,ȳ").expect("184.25") }

    @Test fun table12_8_meanOfY_viaSwap() { calc("$players o:x̄,ȳ o:SWAP").expect("82.75") }

    @Test fun table12_8_popStdDevOfX() { calc("$players o:Ox,Oy").expect("5.80") }

    @Test fun table12_8_sumOfY() { calc("$players b:Σy").expect("331.00") }

    private val azaleas =
        "2 INPUT 1400 Σ+ 1 INPUT 920 Σ+ 3 INPUT 1100 Σ+ 5 INPUT 2265 Σ+ 5 INPUT 2890 Σ+ 4 INPUT 2200 Σ+"

    @Test fun table12_10_intercept() { calc("$azaleas o:x̄w,b o:SWAP").expect("376.25") }

    @Test fun table12_10_slope() { calc("$azaleas o:ŷ,m o:SWAP").expect("425.88") }

    @Test fun table12_10_correlation() { calc("$azaleas o:x̂,r o:SWAP").expect("0.90") }

    /** Table 12-10 values through the app's own route: pressing the key again shows the second value. */
    @Test fun table12_10_viaRepeatedKey() {
        calc("$azaleas o:ŷ,m o:ŷ,m").expect("425.88")
        calc("$azaleas o:x̄w,b o:x̄w,b").expect("376.25")
        calc("$azaleas o:x̂,r o:x̂,r").expect("0.90")
    }

    @Test fun table12_11_predictY() { calc("$azaleas 7 o:ŷ,m").expect("3,357.38").press("8 o:ŷ,m").expect("3,783.25") }

    @Test fun table12_12_predictX() { calc("$azaleas 3000 o:x̂,r").expect("6.16") }

    /** x INPUT weight: mean of 10 (weight 1) and 20 (weight 3). */
    @Test fun weightedMean_xThenWeight() { calc("10 INPUT 1 Σ+ 20 INPUT 3 Σ+ o:x̄w,b").expect("17.50") }

    // ── Ch. 12 Probability (lower tail) ──────────────────────────────────────

    @Test fun table12_15_permutationsInline() { calc("5 b:nPr 3 =").expect("60.00") }

    @Test fun table12_15_permutationsInput() { calc("5 INPUT 3 b:nPr").expect("60.00") }

    @Test fun table12_16_combinationsInline() { calc("5 b:nCr 3 =").expect("10.00") }

    @Test fun table12_16_combinationsInput() { calc("5 INPUT 3 b:nCr").expect("10.00") }

    @Test fun table12_19_normalLowerTail() { calc("o:DISP 6 -1.7 b:Z→P").expect(0.044565, 5e-7) }

    @Test fun table12_21_studentLowerTail_input() { calc("o:DISP 6 8 INPUT -1.86 b:df,t→P").expect(0.0499653, 5e-7) }

    /** Symmetry with Table 12-19, and the middle of the distribution. */
    @Test fun normalLowerTail_upperHalf() {
        calc("o:DISP 6 1.7 b:Z→P").expect(1 - 0.044565, 1e-6)
        calc("o:DISP 6 0 b:Z→P").expect(0.5, 0.0)
    }

    @Test fun studentLowerTail_upperHalf() { calc("o:DISP 6 8 INPUT 1.86 b:df,t→P").expect(1 - 0.0499653, 1e-6) }

    @Test fun table12_20_inverseNormal() { calc("o:DISP 6 .025 b:INV 3").expect(-1.959964, 5e-7) }

    @Test fun table12_22_inverseStudent() {
        calc("o:DISP 6 26 INPUT .05 b:INV 2").expect(-1.705618, 5e-7)
        calc("o:DISP 6 26 b:INV 2 .05 =").expect(-1.705618, 5e-7)
    }

    @Test fun table12_21_studentLowerTail_inline() { calc("o:DISP 7 8 b:df,t→P -1.86 =").expect("0.0499653") }

    @Test fun table12_13_weightedRent() {
        calc("500 INPUT 54 Σ+ 505 INPUT 32 Σ+ 510 INPUT 88 Σ+ 516 INPUT 92 Σ+ o:x̄w,b")
            .expect((500 * 54 + 505 * 32 + 510 * 88 + 516 * 92) / 266.0, 0.005)
    }

    // ── At a Glance and ch. 6: amortization entry ────────────────────────────

    private val smallLoan = "12 o:P/YR 30 o:×P/YR 10 I/YR 14000 PV 0 FV PMT"

    @Test fun table1_15_period20() {
        calc("$smallLoan 20 INPUT o:AMORT").expect("20").press("=").expect(-7.25, AMORT_TOL)
            .press("=").expect(-115.61, AMORT_TOL).press("=").expect(13865.83, AMORT_TOL)
    }

    @Test fun table1_16_rangeThenAutoIncrement() {
        calc("$smallLoan 1 INPUT 12 o:AMORT").expect("1 – 12").press("=").expect(-77.82, AMORT_TOL)
            .press("=").expect(-1396.50, AMORT_TOL).press("=").expect(13922.18, AMORT_TOL)
            .press("o:AMORT").expect("13 – 24").press("=").expect(-85.96, AMORT_TOL)
            .press("=").expect(-1388.36, AMORT_TOL).press("=").expect(13836.22, AMORT_TOL)
    }

    /** Ch. 6: "To amortize a single payment, enter the period number and press AMORT". */
    @Test fun amortize_periodWithoutInput() { calc("$smallLoan 20 o:AMORT").expect("20").press("=").expect(-7.25, AMORT_TOL) }

    // ── Ch. 2/4: two-number functions ────────────────────────────────────────

    @Test fun table2_22_percentChangeAfterChain() { calc("17 INPUT 2 + 33 + 58 - 64 o:%CHG").expect("70.59") }

    @Test fun table4_4_percentChangeConstant() {
        calc("55 o:%CHG 32 K =").expect("-41.82").press("50 =").expect("-36.00").press("45 =").expect("-28.89")
    }

    @Test fun table9_2_dateInline() { calc("12.182011 o:DATE 100 =").expect("3.272012") }

    @Test fun table9_3_dateInput() { calc("12.182011 INPUT 100 o:DATE").expect("3.272012") }

    @Test fun dateInThePast() { calc("3.272012 INPUT -100 o:DATE").expect("12.182011") }

    @Test fun table12_8_viaSwap() {
        calc("$players o:x̄,ȳ o:SWAP").expect("82.75").press("o:Ox,Oy").expect("5.80").press("o:SWAP").expect("4.71")
    }

    // ── Appendix B: declining balance is a plain formula; the last year takes what remains ──

    @Test fun decliningBalance_neverSwitchesToStraightLine() {
        val asset = "5000 PV 7 N 125 I/YR 0 FV"
        calc("$asset 3 b:DB").expect(5000 * 1.25 / 7 * Math.pow(1 - 1.25 / 7, 2.0), 0.005)
        calc("$asset 6 b:DB").expect(5000 * 1.25 / 7 * Math.pow(1 - 1.25 / 7, 5.0), 0.005)
        calc("$asset 7 b:DB").expect(5000 * Math.pow(1 - 1.25 / 7, 6.0), 0.005).press("o:SWAP").expect("0.00")
    }

    // ── Ch. 1, 6: interest rate conversions ──────────────────────────────────

    @Test fun table1_20_effectiveRate() { calc("10 o:NOM% 12 o:P/YR o:EFF%").expect("10.47") }

    @Test fun table6_23_24_compareBanks() {
        calc("6.7 o:NOM% 4 o:P/YR o:EFF%").expect("6.87").press("6.65 o:NOM% 12 o:P/YR o:EFF%").expect("6.86")
    }

    @Test fun table6_26_27_dailyCompoundingMonthlyDeposits() {
        calc("5 o:NOM% 365 o:P/YR o:EFF%").expect("5.13").press("12 o:P/YR o:NOM%").expect("5.01")
            .press("o:Beg/End 0 PV -25 PMT 7 o:×P/YR FV").expect("2,519.61")
    }

    // ── Ch. 8: "When you calculate NPV, the result is stored in PV" ──────────

    @Test fun npvIsStoredInPv() {
        calc("12 o:P/YR -11000 CFj 5000 CFj 6000 INPUT 2 CFj 10 I/YR o:NPV C RCL PV")
            .expect(-11000 + 5000 / (1 + .1 / 12) + 6000 / Math.pow(1 + .1 / 12, 2.0) + 6000 / Math.pow(1 + .1 / 12, 3.0), 0.005)
    }

    /** "C ALL clears all memory ... with the exception of P/YR"; resetting modes is a separate key combination. */
    @Test fun clearAllKeepsBeginMode() {
        val c = calc("o:Beg/End")
        c.vm.onKeyPress(KeyAction.CLEAR_ALL)
        c.press("12 o:P/YR 10 I/YR -13500 PV 7500 FV 36 N PMT").expect("253.99")
    }

    // ── Ch. 13 Additional examples ───────────────────────────────────────────

    @Test fun table13_6_7_continuousCompounding() {
        calc("18 % o:eˣ").expect("1.20").press("- 1 × 100 =").expect("19.72").press("o:EFF%").expect("19.72")
            .press("12 o:P/YR o:NOM%").expect("18.14")
            .press("15 o:×P/YR").expect("180.00").press("-250 PMT -4572.80 PV FV").expect("297,640.27")
    }

    @Test fun table13_8_9_10_discountedMortgage() {
        calc("12 o:P/YR 9 I/YR 20 o:×P/YR").expect("240.00").press("-100000 PV 0 FV PMT").expect("899.73")
            .press("o:RND PMT").expect("899.73").press("5 o:×P/YR FV").expect("88,706.74")
            .press("RCL N - 42 N C RCL N").expect("18.00").press("-79000 PV I/YR").expect("20.72")
    }

    @Test fun table13_11_aprWithFees() {
        calc("12 o:P/YR 8.5 I/YR 30 o:×P/YR 160000 PV 0 FV PMT").expect("-1,230.26")
            .press("RCL PV").expect("160,000.00").press("- 2 % PV I/YR").expect("8.72")
    }

    @Test fun table13_12_interestOnlyWithFee() {
        calc("12 o:P/YR 12 I/YR 10 o:×P/YR 1000000 PV +/- FV PMT").expect("-10,000.00")
            .press("RCL PV - 3 % PV I/YR").expect("12.53")
    }

    @Test fun table13_13_oddFirstPeriod() {
        calc("12 o:P/YR 15 I/YR ÷ 12 ×").expect("1.25").press("16 ÷ 30 ×").expect("0.67")
            .press("4500 o:SWAP % =").expect("30.00").press("+ 4500 PV 36 N 0 FV PMT").expect("-157.03")
    }

    @Test fun table13_14_15_autoLoan() {
        calc("12 o:P/YR 36 N 12500 PV 0 FV 3.5 I/YR PMT").expect("-366.28").press("× RCL N =").expect("-13,185.94")
            .press("11500 PV 9.5 I/YR PMT").expect("-368.38").press("× RCL N =").expect("-13,261.64")
    }

    @Test fun table13_16_canadianMortgage() {
        calc("12 o:NOM% 2 o:P/YR o:EFF%").expect("12.36").press("12 o:P/YR o:NOM%").expect("11.71")
            .press("130000 PV 0 FV 30 o:×P/YR").expect("360.00").press("PMT").expect("-1,308.30")
    }

    @Test fun table13_17_18_19_whatIf() {
        calc("12 o:P/YR 735000 PV 11.2 I/YR 30 o:×P/YR 0 FV PMT").expect("-7,110.88")
            .press("-3555 PMT 26 o:P/YR N").expect("514.82").press("RCL o:×P/YR").expect("19.80")
            .press("RCL o:P/YR").expect("26.00")
            .press("12 o:P/YR 15 o:×P/YR PMT").expect("-8,446.53").press("× RCL N +").expect("-1,520,374.70")
            .press("RCL PV =").expect("-785,374.70")
    }

    @Test fun table13_20_21_22_college() {
        calc("9 o:NOM% 12 o:P/YR o:EFF%").expect("9.38").press("I/YR o:Beg/End 1 o:P/YR 15000 PMT 4 N 0 FV PV").expect("-52,713.28")
            .press("o:Beg/End +/- FV C RCL FV").expect("52,713.28")
            .press("0 PV 12 o:P/YR 144 N 9 I/YR PMT").expect("-204.54")
    }

    @Test fun table13_23_untaxedGains() {
        calc("o:Beg/End 1 o:P/YR 35 N 8.175 I/YR 0 PV -2000 PMT FV").expect("387,640.45")
            .press("RCL PMT × RCL N =").expect("-70,000.00").press("+ RCL FV =").expect("317,640.45")
            .press("× 15 % =").expect("47,646.07").press("+/- + RCL FV =").expect("339,994.39")
            .press("FV 4 I/YR 0 PMT PV").expect("-86,159.84")
    }

    @Test fun table13_24_taxableAccount() {
        calc("o:Beg/End 1 o:P/YR 35 N 8.175 - 28 % =").expect("5.89").press("I/YR 0 PV -3000 PMT FV").expect("345,505.61")
            .press("4 I/YR 0 PMT PV").expect("-87,556.47")
    }

    @Test fun table13_25_26_wrapAround() {
        calc("12 o:P/YR 82510.22 PV 9.5 I/YR 0 FV 15 o:×P/YR PMT").expect("-861.59")
            .press("-35000 CFj RCL PMT +/- - 754 CFj 82 o:Nj RCL PMT +/- CFj 180 - 82 o:Nj o:IRR/YR").expect("10.16")
    }

    @Test fun table13_3_salesForecast() {
        calc("1 INPUT 10000 Σ+ 2 INPUT 11210 Σ+ 3 INPUT 13060 Σ+ 4 INPUT 16075 Σ+ 5 INPUT 20590 Σ+ 6 o:ŷ,m").expect("22,000.50")
            .press("7 o:ŷ,m").expect("24,605.00")
    }

    @Test fun table4_7_registersTenAndUp() {
        calc("475.6 o:STO .4 ÷ 39.15 o:STO 2 =").expect("12.15").press("560.1 + RCL .4 ÷").expect("1,035.70")
            .press("RCL 2 =").expect("26.45")
    }
}
