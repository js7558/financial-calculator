package com.js10bii.calculator.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.js10bii.calculator.engine.KeyAction
import com.js10bii.calculator.engine.KEY_ROWS
import com.js10bii.calculator.ui.theme.*
import com.js10bii.calculator.viewmodel.ShiftState

/** Space between keys, shared with the row of TVM values above them. */
internal val KeyGap = 6.dp

@Composable
fun CalculatorKeypad(
    shiftState: ShiftState,
    onKeyPress: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(KeyGap),
    ) {
        // Rows 0–3: TVM, input/business, operators, memory
        for (rowIndex in 0..3) KeyRow(rowIndex, shiftState, onKeyPress)

        // Blue shift + 7, 8, 9, ÷
        KeyRow(5, shiftState, onKeyPress) { keyModifier ->
            ShiftKey(
                label = "▲",
                description = "Blue shift",
                color = BlueLabel,
                bgColor = ShiftKeyBlue,
                active = shiftState == ShiftState.BLUE,
                modifier = keyModifier,
                onClick = { onKeyPress(KeyAction.BLUE_SHIFT) },
            )
        }

        // Orange shift + 4, 5, 6, ×
        KeyRow(6, shiftState, onKeyPress) { keyModifier ->
            ShiftKey(
                label = "▼",
                description = "Orange shift",
                color = OrangeLabel,
                bgColor = ShiftKeyOrange,
                active = shiftState == ShiftState.ORANGE,
                modifier = keyModifier,
                onClick = { onKeyPress(KeyAction.ORANGE_SHIFT) },
            )
        }

        // C, 1, 2, 3, −  and  ON, 0, ., =, +
        for (rowIndex in 7..8) KeyRow(rowIndex, shiftState, onKeyPress)
    }
}

@Composable
private fun ColumnScope.KeyRow(
    rowIndex: Int,
    shiftState: ShiftState,
    onKeyPress: (KeyAction) -> Unit,
    leading: @Composable (Modifier) -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth().weight(1f),
        horizontalArrangement = Arrangement.spacedBy(KeyGap),
    ) {
        val keyModifier = Modifier.weight(1f).fillMaxHeight()
        leading(keyModifier)
        KEY_ROWS[rowIndex].forEach { keyDef ->
            CalcKey(keyDef = keyDef, shiftState = shiftState, modifier = keyModifier, onPress = onKeyPress)
        }
    }
}
