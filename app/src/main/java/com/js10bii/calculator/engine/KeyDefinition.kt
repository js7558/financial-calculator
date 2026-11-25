package com.js10bii.calculator.engine

data class KeyDefinition(
    val row: Int,
    val col: Int,
    val primaryLabel: String,
    val orangeLabel: String,
    val blueLabel: String,
    val primaryAction: KeyAction,
    val orangeAction: KeyAction,
    val blueAction: KeyAction,
)
