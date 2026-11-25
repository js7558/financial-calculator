package com.js10bii.calculator.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    secondary = BlueLabel,
    background = CalcBody,
    surface = Panel,
    error = ErrorText,
    onPrimary = CalcBody,
    onSecondary = CalcBody,
    onSurface = PrimaryLabel,
    onBackground = PrimaryLabel,
)

@Composable
fun JS10bIIPlusTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content,
    )
}
