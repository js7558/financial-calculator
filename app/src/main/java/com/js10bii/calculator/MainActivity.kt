package com.js10bii.calculator

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import com.js10bii.calculator.ui.CalculatorScreen
import com.js10bii.calculator.ui.theme.JS10bIIPlusTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            JS10bIIPlusTheme {
                CalculatorScreen(modifier = Modifier.fillMaxSize())
            }
        }
    }
}
