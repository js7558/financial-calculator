package com.js10bii.calculator.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.js10bii.calculator.ui.components.CalculatorDisplay
import com.js10bii.calculator.ui.components.CalculatorKeypad
import com.js10bii.calculator.ui.components.SettingsDialog
import com.js10bii.calculator.ui.theme.*
import com.js10bii.calculator.viewmodel.*

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier,
    vm: CalculatorViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CalcBody),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(start = 8.dp, top = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Title bezel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(SilverBezel, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .border(1.dp, SilverBezelDark, RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                    .padding(horizontal = 14.dp, vertical = 36.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "JS 10bII+",
                            color = CalcBody,
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Serif,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Financial Calculator",
                            color = CalcBody,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Serif,
                        )
                    }
                    // Hamburger menu button
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CalcBody.copy(alpha = 0.15f))
                            .clickable { showSettings = true },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "☰",
                            color = CalcBody,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            // Calculator body
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(CalcBody)
                    .border(2.dp, BodyBorder)
                    .padding(10.dp),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    // LCD Display
                    CalculatorDisplay(
                        state = state,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp),
                    )

                    // Keypad
                    CalculatorKeypad(
                        shiftState = state.shiftState,
                        onKeyPress = { vm.onKeyPress(it) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                    )

                    // Math expression / status strip
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(0xFF8A9E70))
                            .padding(horizontal = 10.dp, vertical = 4.dp),
                    ) {
                        StatusText(StatusLines.modes(state))
                        StatusText(StatusLines.values(state))
                    }
                }
            }

            // Bottom bezel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(12.dp)
                    .background(SilverBezel, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                    .border(1.dp, SilverBezelDark, RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)),
            )

            // Black strip — clears the Android navigation bar and mirrors the top bezel height
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(bottom = 36.dp),
            )
        }
    }

    if (showSettings) {
        SettingsDialog(
            state = state,
            onDismiss = { showSettings = false },
            onDecimalPlaces = { vm.setDecimalPlaces(it) },
            onAngleMode = { vm.setAngleMode(it) },
            onCalcMode = { vm.setCalcMode(it) },
            onPyr = { vm.setPyr(it) },
            onPaymentTiming = { vm.setPaymentTiming(it) },
        )
    }
}

/** One line that shrinks to fit: a cut-off amount could be misread. The height is fixed so the keys do not move. */
@Composable
private fun StatusText(text: String) {
    var size by remember(text) { mutableStateOf(11f) }
    Box(modifier = Modifier.fillMaxWidth().height(17.dp), contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            color = Color(0xFF1A3010),
            fontSize = size.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (it.hasVisualOverflow && size > MIN_STATUS_SP) size -= 0.5f },
        )
    }
}

private const val MIN_STATUS_SP = 6f
