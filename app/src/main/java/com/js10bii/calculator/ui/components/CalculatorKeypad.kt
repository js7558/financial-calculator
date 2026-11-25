package com.js10bii.calculator.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.js10bii.calculator.engine.KeyAction
import com.js10bii.calculator.engine.KEY_ROWS
import com.js10bii.calculator.ui.theme.*
import com.js10bii.calculator.viewmodel.ShiftState

@Composable
fun CalculatorKeypad(
    shiftState: ShiftState,
    onKeyPress: (KeyAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        // Rows 0–3: TVM, input/business, operators, memory
        for (rowIndex in 0..3) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                KEY_ROWS[rowIndex].forEach { keyDef ->
                    CalcKey(
                        keyDef = keyDef,
                        shiftState = shiftState,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onPress = onKeyPress,
                    )
                }
            }
        }

        // Blue shift + 7, 8, 9, ÷
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            // 12dp spacer matches the blue-label header height in CalcKey
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Spacer(modifier = Modifier.height(12.dp))
                ShiftKey(
                    label = "▲",
                    color = BlueLabel,
                    bgColor = ShiftKeyBlue,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    onClick = { onKeyPress(KeyAction.BLUE_SHIFT) },
                )
            }
            KEY_ROWS[5].forEach { keyDef ->
                CalcKey(
                    keyDef = keyDef,
                    shiftState = shiftState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onPress = onKeyPress,
                )
            }
        }

        // Orange shift + 4, 5, 6, ×
        Row(
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                Spacer(modifier = Modifier.height(12.dp))
                ShiftKey(
                    label = "▼",
                    color = OrangeLabel,
                    bgColor = ShiftKeyOrange,
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    onClick = { onKeyPress(KeyAction.ORANGE_SHIFT) },
                )
            }
            KEY_ROWS[6].forEach { keyDef ->
                CalcKey(
                    keyDef = keyDef,
                    shiftState = shiftState,
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    onPress = onKeyPress,
                )
            }
        }

        // C, 1, 2, 3, −  and  ON, 0, ., =, +
        for (rowIndex in 7..8) {
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f),
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                KEY_ROWS[rowIndex].forEach { keyDef ->
                    CalcKey(
                        keyDef = keyDef,
                        shiftState = shiftState,
                        modifier = Modifier.weight(1f).fillMaxHeight(),
                        onPress = onKeyPress,
                    )
                }
            }
        }
    }
}
