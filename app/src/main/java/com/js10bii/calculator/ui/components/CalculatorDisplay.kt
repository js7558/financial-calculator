package com.js10bii.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.js10bii.calculator.ui.theme.*
import com.js10bii.calculator.viewmodel.AmortPhase
import com.js10bii.calculator.viewmodel.CalculatorState

@Composable
fun CalculatorDisplay(
    state: CalculatorState,
    modifier: Modifier = Modifier,
) {
    val displayShape = RoundedCornerShape(4.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(displayShape)
            .background(DisplayBg)
            .border(2.dp, DisplayBorder, displayShape)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Annunciator row — pinned to the top
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Annunciator(text = "▲", visible = state.showShiftUp, color = BlueLabel)
                    Annunciator(text = "▼", visible = state.showShiftDown, color = OrangeLabel)
                    Annunciator(text = "BEGIN", visible = state.showBegin)
                    Annunciator(text = "INPUT", visible = state.showInput)
                    Annunciator(text = "PRIN",  visible = state.amortPhase == AmortPhase.PRIN)
                    Annunciator(text = "INT",   visible = state.amortPhase == AmortPhase.INT)
                    Annunciator(text = "BAL",   visible = state.amortPhase == AmortPhase.BAL)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Annunciator(text = "RAD",  visible = state.showRad)
                    Annunciator(text = "SEMI", visible = state.bondSemi)
                    Annunciator(text = "D.MY", visible = state.dateDMY)
                    Annunciator(text = "360",  visible = !state.dateActual)
                }
            }

            // Main numeric display — vertically centered in remaining space
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = state.displayText,
                    color = if (state.isError) Color(0xFFCC2200) else if (state.isOff) DisplayText.copy(alpha = 0.4f) else DisplayText,
                    style = fittedStyle(state.displayText, constraints.maxWidth),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** Shrinks the text until all of it fits: a number with digits cut off is a wrong number. */
@Composable
private fun fittedStyle(text: String, maxWidth: Int): TextStyle {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, maxWidth, density) {
        val full = TextStyle(fontSize = 42.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
        val width = measurer.measure(text, full, maxLines = 1, softWrap = false).size.width
        if (width <= maxWidth) full
        else full.copy(fontSize = with(density) { (full.fontSize.toPx() * maxWidth / width * 0.98f).toSp() })
    }
}

@Composable
private fun Annunciator(text: String, visible: Boolean, color: Color = DisplayText) {
    Text(
        text = if (visible) text else "",
        color = color,
        fontSize = 8.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.widthIn(min = if (text.length > 3) 32.dp else 20.dp),
    )
}
