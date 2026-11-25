package com.js10bii.calculator.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.js10bii.calculator.engine.KeyAction
import com.js10bii.calculator.engine.KeyDefinition
import com.js10bii.calculator.ui.theme.*
import com.js10bii.calculator.viewmodel.ShiftState

/**
 * One key with its blue, primary and orange labels, always in that order. A shift lights up its label
 * in place and dims the other two, so the key says what it will do.
 */
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
    val activeLabel = when (shiftState) {
        ShiftState.ORANGE -> keyDef.orangeLabel
        ShiftState.BLUE   -> keyDef.blueLabel
        ShiftState.NONE   -> keyDef.primaryLabel
    }
    val face = keyDef.faceColor()

    KeySurface(
        color = when (shiftState) {
            ShiftState.ORANGE -> OrangeLabel.copy(alpha = 0.07f).compositeOver(face)
            ShiftState.BLUE   -> BlueLabel.copy(alpha = 0.07f).compositeOver(face)
            ShiftState.NONE   -> face
        },
        description = SPOKEN[activeLabel] ?: activeLabel,
        modifier = modifier,
        onClick = { onPress(action) },
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Short keys and large system fonts shrink the labels instead of cutting them off.
            val scale = (maxHeight.value / (LABELS_HEIGHT * LocalDensity.current.fontScale)).coerceAtMost(MAX_LABEL_SCALE)
            val width = constraints.maxWidth

            Column(
                modifier = Modifier.clearAndSetSemantics { },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ShiftLabel(keyDef.blueLabel, BlueLabel, shiftState, ShiftState.BLUE, scale, width)
                KeyLabel(
                    text = keyDef.primaryLabel,
                    color = PrimaryLabel.copy(alpha = if (shiftState == ShiftState.NONE) 1f else DIMMED),
                    size = keyDef.labelSize() * scale,
                    height = PRIMARY_HEIGHT * scale,
                    weight = FontWeight.Medium,
                    maxWidth = width,
                )
                ShiftLabel(keyDef.orangeLabel, OrangeLabel, shiftState, ShiftState.ORANGE, scale, width)
            }
        }
    }
}

@Composable
private fun ShiftLabel(
    text: String,
    color: Color,
    current: ShiftState,
    own: ShiftState,
    scale: Float,
    maxWidth: Int,
) {
    val lit = current == own
    KeyLabel(
        text = text,
        color = if (current == ShiftState.NONE || lit) color else color.copy(alpha = DIMMED),
        size = (if (lit) 13f else 11f) * scale,
        height = SHIFT_HEIGHT * scale,
        weight = if (lit) FontWeight.Bold else FontWeight.Medium,
        maxWidth = maxWidth,
    )
}

@Composable
private fun KeyLabel(text: String, color: Color, size: Float, height: Float, weight: FontWeight, maxWidth: Int) {
    val animated by animateColorAsState(color, tween(120), label = "label")
    val full = TextStyle(
        fontSize = size.sp,
        fontFamily = FontFamily.SansSerif,
        fontWeight = weight,
        platformStyle = PlatformTextStyle(includeFontPadding = false),
    )
    // A fixed slot: bold text and arrows from another font are taller and would push the other labels.
    Box(
        modifier = Modifier.height(with(LocalDensity.current) { height.sp.toDp() }),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = animated,
            style = fittedStyle(text, full, maxWidth),
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.wrapContentHeight(unbounded = true),
        )
    }
}

@Composable
fun ShiftKey(
    label: String,
    description: String,
    color: Color,
    bgColor: Color,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    KeySurface(
        color = if (active) color else bgColor,
        description = description,
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(
            text = label,
            color = if (active) CalcBody else color,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.SansSerif,
            modifier = Modifier.clearAndSetSemantics { },
        )
    }
}

/** The face shared by all keys: color, ripple, press animation, haptic tap and screen reader label. */
@Composable
private fun KeySurface(
    color: Color,
    description: String,
    modifier: Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, tween(80), label = "press")
    val background by animateColorAsState(color, tween(120), label = "face")
    val view = LocalView.current

    LaunchedEffect(interactions, view) {
        interactions.interactions.collect {
            if (it is PressInteraction.Press) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
        }
    }

    // The whole cell takes the tap; only the face inside it is drawn.
    BoxWithConstraints(
        modifier = modifier
            .clickable(
                interactionSource = interactions,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics { contentDescription = description },
    ) {
        // Short keys need all their height for the labels.
        val spare = (maxHeight - FACE_MIN_HEIGHT.dp * LocalDensity.current.fontScale) / 2
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = KEY_STYLE.insetX, vertical = spare.coerceIn(0.dp, KEY_STYLE.insetY))
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(RoundedCornerShape(KEY_STYLE.corner))
                .background(background)
                .indication(interactions, ripple(color = PrimaryLabel))
                .padding(horizontal = 2.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center,
            content = content,
        )
    }
}

/** How far the face is drawn inside the area that takes taps, and its corners. */
private class KeyStyle(val insetX: Dp, val insetY: Dp, val corner: Dp)

private val FULL = KeyStyle(insetX = 0.dp, insetY = 0.dp, corner = 14.dp)
private val SLIM = KeyStyle(insetX = 2.dp, insetY = 5.dp, corner = 8.dp)

/** The look of every key: FULL or SLIM. */
private val KEY_STYLE = SLIM

private fun KeyDefinition.isDigit() =
    primaryAction == KeyAction.DECIMAL || primaryLabel.singleOrNull()?.isDigit() == true

private fun KeyDefinition.isOperator() = primaryAction in OPERATORS

private fun KeyDefinition.labelSize() = when {
    isOperator() || primaryAction == KeyAction.EQUALS -> 23f
    isDigit() -> 20f
    else -> 16f
}

private fun KeyDefinition.faceColor(): Color = when {
    row == 0 -> KeyTvm
    isDigit() -> KeyDigit
    isOperator() -> KeyOperator
    primaryAction == KeyAction.EQUALS -> KeyEquals
    else -> KeyFunction
}

private val OPERATORS = setOf(KeyAction.ADD, KeyAction.SUBTRACT, KeyAction.MULTIPLY, KeyAction.DIVIDE)

/** Labels a screen reader would read badly or skip. */
private val SPOKEN = mapOf(
    "←" to "Backspace", "+/-" to "Change sign", "-" to "Minus", "÷" to "Divide", "×" to "Multiply",
    "." to "Decimal point", "→M" to "Store in M", "Σ+" to "Sigma plus", "Σ-" to "Sigma minus",
    "(" to "Open parenthesis", ")" to "Close parenthesis",
)

private const val FACE_MIN_HEIGHT = 56f
private const val DIMMED = 0.4f
private const val SHIFT_HEIGHT = 14f
private const val PRIMARY_HEIGHT = 22f
private const val LABELS_HEIGHT = PRIMARY_HEIGHT + 2 * SHIFT_HEIGHT
private const val MAX_LABEL_SCALE = 1.15f
