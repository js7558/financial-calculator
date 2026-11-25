package com.js10bii.calculator.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.js10bii.calculator.ui.StatusLines
import com.js10bii.calculator.ui.theme.*
import com.js10bii.calculator.viewmodel.CalculatorState

/** The stored TVM values, each above its key: the columns must match the first row of the keypad. */
@Composable
fun TvmRow(state: CalculatorState, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
        StatusLines.registers(state).forEach { (name, value) ->
            BoxWithConstraints(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clearAndSetSemantics { contentDescription = "$name ${value ?: "not set"}" },
                contentAlignment = Alignment.Center,
            ) {
                val text = value ?: "–"
                val full = NumberStyle.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = text,
                    color = if (value == null) SecondaryLabel.copy(alpha = 0.4f) else Accent,
                    style = fittedStyle(text, full, constraints.maxWidth, constraints.maxHeight),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
    }
}
