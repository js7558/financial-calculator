# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

JS 10bII+ is a single-module Android app (Kotlin, Jetpack Compose) that emulates the keystroke behavior of an HP 10bII+ financial calculator.

## Commands

Requires JDK 17 and an Android SDK path in `local.properties`.

```bash
./gradlew assembleDebug                 # APK -> app/build/outputs/apk/debug/
./gradlew installDebug                  # install on a connected device/emulator
./gradlew testDebugUnitTest             # unit tests
./gradlew lintDebug                     # Android lint

# Single test class / single method
./gradlew :app:testDebugUnitTest --tests "com.js10bii.calculator.engine.TvmEngineTest"
./gradlew :app:testDebugUnitTest --tests "com.js10bii.calculator.engine.TvmEngineTest.solveFV_annual"
```

## Architecture

Source is under `app/src/main/java/com/js10bii/calculator/`. Every key press follows one path:

1. `engine/KeyLayout.kt` (`KEY_ROWS`) gives each key a primary, orange-shift and blue-shift label and `KeyAction`.
2. `CalcKey` picks the action for the current `ShiftState` and the keypad passes it to `CalculatorViewModel.onKeyPress`.
3. `process(action, state)` is a pure reducer: a large `when` returning a new immutable `CalculatorState`. Before that `when`, it handles in order: power-off wake, `awaitingInput` (multi-key sequences: STO/RCL, DISP, C MEM), then INV/HYP-modified trig.
4. Handlers call stateless `object`s in `engine/`, which signal invalid input by throwing `ArithmeticException`; handlers catch it and return `errorState`.

Adding a key function means touching `KeyAction`, `KEY_ROWS` and the `when` in `process`. The `when` has an `else` branch that ignores unhandled actions, so a missing case compiles and silently does nothing.

`CalculatorKeypad` renders `KEY_ROWS` by hard-coded index and draws the shift keys itself, so row 4 of `KEY_ROWS` is not rendered.

The strip under the keypad is `ui/StatusLines.kt` (plain text, tested in `ui/StatusLinesTest`): full cents, and always P/YR and the modes, which C ALL keeps.

`CalculatorEngine` imports `AngleMode` from the `viewmodel` package, so `engine` is not fully independent of it.

### State conventions

- **`x` is the current operand; `displayText` is only its view.** Display every calculated or recalled number with `show()` / `showResult()`, never by setting `displayText`: they keep 12 digits, reject NaN/infinity and reset entry flags. While `isEditing`, `onKeyPress` derives `x` from the typed text.
- **Store vs. solve depends on `isEditing`.** TVM, break-even, bond, margin, P/YR, NOM% and EFF% keys store when the user is typing or has a result (`hasResult`), after completing pending arithmetic (`completePending`), and otherwise compute. `INPUT` works the same way. Solved values and TVM recalls use `show()`, so the next TVM key solves; `+/-`, RM and numbered-register recalls use `showResult()`, so it stores. Store handlers must set `isEditing = false`.
- **Numbers display with up to 12 digits** (`formatDisplay`), trading decimals for whole digits; 1 trillion and up is scientific. `CalculatorDisplay` shrinks the text to fit instead of clipping it. Typed text is grouped by `formatRaw`, which keeps the sign as text.
- **An exponent being typed is always parseable:** `E` shows `1E0` and digits replace the 0. Digit, `.`, `+/-` and ← handlers must check for `E` in the text.
- **`awaitingOperand`** is true between an operator key or `INPUT` and the second number; pressing another operator then replaces the first instead of evaluating.
- **`operandA`/`pendingOperator` is the innermost operation; `outer` holds the ones waiting behind it** and open parentheses (`op == null`). `priority` is the same for all operators in Chain mode. `completePending` evaluates everything; code that drops a calculation must clear `outer` too.
- **TVM registers start as `null`**, which solving treats as 0. AMORT, NPV and depreciation instead report a missing register (except salvage, which defaults to 0). C MEM + 1 sets them to `0.0`, while C ALL resets them to `null`.
- **Registers are shared across features:** depreciation uses PV = cost, FV = salvage (entered positive), N = life, I/YR = declining-balance factor in %; NPV uses I/YR and is stored in PV; IRR and NOM% write I/YR.
- **`INPUT` holds the first number of a pair in `pendingInput`.** Amortization ranges, CFj counts, Σ+/Σ− and two-number functions (`handleTwoNumber`: %CHG, nPr, nCr, ΔDAYS, DATE, df,t→P) consume it. Without it, two-number functions stay pending like an operator; `showOperation` evaluates both forms.
- **Bond dates are stored as Julian days** (`settleDay`, `maturityDay`), so toggling D.MY does not change them. `BondEngine.schedule` derives periods and the accrued fraction from them.
- **SWAP** shows the second value of a depreciation or statistics result (repeating a statistics key does the same), else exchanges the operands of a pending operation or `INPUT` pair.
- **Margin keys** (CST, PRC, MAR, MU) store or solve; `rateIsMargin` records whether MAR or MU was set last, and that one is used to solve CST and PRC.
- **INV and HYP keep the blue shift on**, because the functions they modify are blue.
- **`=` is overloaded:** in order, it cycles amortization results, cycles depreciation results, evaluates the pending operator, then applies the K constant. Any key outside `KEEPS_AMORT_CYCLE` / `KEEPS_DEPR_CYCLE` ends the first two. C clears an entry in progress; otherwise it cancels the calculation, including K and `pendingInput`.
- Handlers begin with `s.clearShift().clearModifiers()` so shift and INV/HYP state lasts one key.
- Any key dismisses an error and restores `x`; C and ← do only that. `errorState` drops the pending operator and replaces messages over 12 characters with "Error" to fit the display.
- Number text is always US-style because it is parsed back; pass `Locale.US` when formatting numbers.
- `clearAll` keeps a specific list of settings; add new persistent settings there.

## Tests

Plain JUnit 4 on the JVM, under `app/src/test/`. Failures in the manual, entry/state, golden and invariant tests are known app bugs unless you just changed that code; do not loosen tolerances to make them pass.

- `keystroke/ManualExamplesTest`: worked examples from the HP 10bII+ User's Guide (c02989763.pdf, not in this repo), typed through the view model with the `Calc` helper (`"30 o:×P/YR"`, `"b:YTM"`). The manual is the spec for behavior; tests not taken from it say so.
- `keystroke/EntryAndStateTest`: precision, state leaking between calculations, entry and display.
- `keystroke/KeyFuzzTest`: random keys must not crash, and the display must match `x`. This one should always pass.
- `engine/GoldenTest`, `engine/InvariantTest`: engines vs. mpmath tables and mathematical identities. Regenerate tables with `python3 app/src/test/golden/generate.py`; never derive them from app code.
- Other `engine/*Test`: the original tests, with loose tolerances. Some of their "HP manual" citations are not in the manual.

## Known gaps

- NFV (SWAP after NPV), non-linear regression models and the cash-flow and statistics editors are in the manual but not implemented.
- `KeyAction.NPR`/`NCR` are unused; the nPr/nCr keys use `N_PERMUT_R`/`N_COMB_R`.
- "(deferred)" comments in `KeyAction.kt` are stale; those groups are wired up.
