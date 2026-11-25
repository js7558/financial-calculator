package com.js10bii.calculator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.js10bii.calculator.ui.theme.*
import com.js10bii.calculator.viewmodel.*

@Composable
fun SettingsDialog(
    state: CalculatorState,
    onDismiss: () -> Unit,
    onDecimalPlaces: (Int) -> Unit,
    onAngleMode: (AngleMode) -> Unit,
    onCalcMode: (CalcMode) -> Unit,
    onPyr: (Double) -> Unit,
    onPaymentTiming: (PaymentTiming) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF272727))
                .border(1.dp, Color(0xFF404040), RoundedCornerShape(12.dp)),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    "Settings",
                    color = PrimaryLabel,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )

                SettingsDivider()

                // Display
                SectionHeader("Display")
                SettingRow("Decimal places") {
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                        for (i in 0..9) {
                            SettingChip("$i", state.displayDecimalPlaces == i) { onDecimalPlaces(i) }
                        }
                    }
                }

                SettingsDivider()

                // Math
                SectionHeader("Math")
                SettingRow("Angle mode") {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        SettingChip("DEG", state.angleMode == AngleMode.DEG) { onAngleMode(AngleMode.DEG) }
                        SettingChip("RAD", state.angleMode == AngleMode.RAD) { onAngleMode(AngleMode.RAD) }
                    }
                }
                SettingRow("Evaluation") {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        SettingChip("Chain", state.calcMode == CalcMode.CHAIN) { onCalcMode(CalcMode.CHAIN) }
                        SettingChip("Algebraic", state.calcMode == CalcMode.ALGEBRAIC) { onCalcMode(CalcMode.ALGEBRAIC) }
                    }
                }

                SettingsDivider()

                // TVM
                SectionHeader("TVM")
                SettingRow("Payments / yr") {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf(1.0, 2.0, 4.0, 12.0, 52.0, 365.0).forEach { pyr ->
                            SettingChip(pyr.toInt().toString(), state.tvmPYR == pyr) { onPyr(pyr) }
                        }
                    }
                }
                SettingRow("Payment timing") {
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        SettingChip("End", state.paymentTiming == PaymentTiming.END) { onPaymentTiming(PaymentTiming.END) }
                        SettingChip("Begin", state.paymentTiming == PaymentTiming.BEGIN) { onPaymentTiming(PaymentTiming.BEGIN) }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Done", color = BlueLabel, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title.uppercase(),
        color = OrangeLabel,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.2.sp,
    )
}

@Composable
private fun SettingRow(label: String, content: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = Color(0xFFAAAAAA), fontSize = 13.sp)
        content()
    }
}

@Composable
private fun SettingsDivider() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Color(0xFF3A3A3A)),
    )
}

@Composable
private fun SettingChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(if (selected) BlueLabel else Color(0xFF363636))
            .border(
                1.dp,
                if (selected) BlueLabel else Color(0xFF505050),
                RoundedCornerShape(4.dp),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 7.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = PrimaryLabel, fontSize = 12.sp)
    }
}
