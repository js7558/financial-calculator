package com.js10bii.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.js10bii.calculator.engine.KeyAction
import com.js10bii.calculator.engine.KeyDefinition
import com.js10bii.calculator.ui.theme.*
import com.js10bii.calculator.viewmodel.ShiftState

@Composable
fun CalcKey(
    keyDef: KeyDefinition,
    shiftState: ShiftState,
    modifier: Modifier = Modifier,
    onPress: (KeyAction) -> Unit,
) {
    val action = when (shiftState) {
        ShiftState.ORANGE -> keyDef.orangeAction
        ShiftState.BLUE   -> keyDef.blueAction
        ShiftState.NONE   -> keyDef.primaryAction
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val keyShape = RoundedCornerShape(7.dp)

    Column(
        modifier = modifier.padding(horizontal = 1.dp, vertical = 0.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        // Blue label above key
        Text(
            text = keyDef.blueLabel,
            color = BlueLabel,
            fontSize = 11.sp,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Clip,
            modifier = Modifier.fillMaxWidth(),
        )

        // Key body
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(keyShape)
                .background(
                    brush = if (isPressed) {
                        Brush.verticalGradient(listOf(KeyPressed, KeyPressed))
                    } else {
                        Brush.verticalGradient(listOf(KeyHighlight, KeyFace))
                    },
                )
                .border(1.dp, KeyShadow, keyShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = { onPress(action) },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 2.dp, vertical = 3.dp),
            ) {
                // Primary label
                Text(
                    text = keyDef.primaryLabel,
                    color = PrimaryLabel,
                    fontSize = 15.sp,
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
                // Orange label on bevel
                if (keyDef.orangeLabel.isNotEmpty()) {
                    Text(
                        text = keyDef.orangeLabel,
                        color = OrangeLabel,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                    )
                }
            }
        }
    }
}

@Composable
fun ShiftKey(
    label: String,
    color: androidx.compose.ui.graphics.Color,
    bgColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val shape = RoundedCornerShape(7.dp)

    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isPressed) KeyPressed else bgColor)
            .border(1.dp, KeyShadow, shape)
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
        )
    }
}
