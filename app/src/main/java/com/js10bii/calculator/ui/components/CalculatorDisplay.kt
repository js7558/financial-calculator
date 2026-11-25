package com.js10bii.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.js10bii.calculator.ui.StatusLines
import com.js10bii.calculator.ui.theme.*
import com.js10bii.calculator.viewmodel.AmortPhase
import com.js10bii.calculator.viewmodel.CalculatorState

@Composable
fun CalculatorDisplay(
    state: CalculatorState,
    modifier: Modifier = Modifier,
) {
    val frame = RoundedCornerShape(14.dp)
    Column(
        modifier = modifier
            .background(Panel, frame)
            .border(1.dp, DisplayFrame, frame)
            .padding(start = 14.dp, end = 14.dp, top = 8.dp, bottom = 2.dp),
    ) {
        // Annunciators; the height is fixed so the number does not move,
        // and the system font size cannot push one off the screen.
        val density = LocalDensity.current
        CompositionLocalProvider(LocalDensity provides Density(density.density, density.fontScale.coerceAtMost(1f))) {
            Row(
                modifier = Modifier.fillMaxWidth().height(22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Annunciator("▲", state.showShiftUp, BlueLabel)
                    Annunciator("▼", state.showShiftDown, OrangeLabel)
                    Annunciator("BEGIN", state.showBegin)
                    Annunciator("INPUT", state.showInput)
                    Annunciator("PRIN", state.amortPhase == AmortPhase.PRIN)
                    Annunciator("INT", state.amortPhase == AmortPhase.INT)
                    Annunciator("BAL", state.amortPhase == AmortPhase.BAL)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    Annunciator("RAD", state.showRad)
                    Annunciator("SEMI", state.bondSemi)
                    Annunciator("D.MY", state.dateDMY)
                    Annunciator("360", !state.dateActual)
                }
            }
        }

        // What the next number will be combined with
        Text(
            text = if (state.isOff) "" else StatusLines.pending(state),
            color = SecondaryLabel,
            style = NumberStyle.copy(fontSize = 16.sp, fontWeight = FontWeight.Normal),
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth(),
        )

        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth().weight(1f),
            contentAlignment = Alignment.CenterEnd,
        ) {
            Text(
                text = state.displayText,
                color = when {
                    state.isError -> ErrorText
                    state.isOff -> PrimaryLabel.copy(alpha = 0.4f)
                    else -> PrimaryLabel
                },
                style = fittedStyle(state.displayText, NumberStyle, constraints.maxWidth, constraints.maxHeight),
                textAlign = TextAlign.End,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun Annunciator(text: String, visible: Boolean, color: Color = Accent) {
    if (!visible) return
    Text(
        text = text,
        color = color,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
        modifier = Modifier
            .background(color.copy(alpha = 0.14f), RoundedCornerShape(50))
            .padding(horizontal = 7.dp, vertical = 2.dp),
    )
}
