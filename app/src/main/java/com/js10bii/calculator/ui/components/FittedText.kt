package com.js10bii.calculator.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer

/** Shrinks the text until all of it fits: a number with digits cut off is a wrong number. */
@Composable
internal fun fittedStyle(text: String, full: TextStyle, maxWidth: Int, maxHeight: Int = Int.MAX_VALUE): TextStyle {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    return remember(text, full, maxWidth, maxHeight, density) {
        val size = measurer.measure(text, full, maxLines = 1, softWrap = false).size
        val scale = minOf(maxWidth.toFloat() / maxOf(size.width, 1), maxHeight.toFloat() / maxOf(size.height, 1))
        if (scale >= 1f) full
        else full.copy(fontSize = with(density) { (full.fontSize.toPx() * scale * 0.98f).toSp() })
    }
}
