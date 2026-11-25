package com.js10bii.calculator.ui

import android.graphics.Rect
import android.os.Build
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.js10bii.calculator.ui.components.CalculatorDisplay
import com.js10bii.calculator.ui.components.CalculatorKeypad
import com.js10bii.calculator.ui.components.KeyGap
import com.js10bii.calculator.ui.components.SettingsDialog
import com.js10bii.calculator.ui.components.TvmRow
import com.js10bii.calculator.ui.components.fittedStyle
import com.js10bii.calculator.ui.theme.*
import com.js10bii.calculator.viewmodel.*

@Composable
fun CalculatorScreen(
    modifier: Modifier = Modifier,
    vm: CalculatorViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    var showSettings by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(CalcBody)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(horizontal = 10.dp)
            .padding(bottom = 8.dp),
    ) {
        Header(onSettings = { showSettings = true })

        CalculatorDisplay(
            state = state,
            modifier = Modifier.fillMaxWidth().weight(1.8f),
        )

        TvmRow(state, modifier = Modifier.fillMaxWidth().height(26.dp))

        CalculatorKeypad(
            shiftState = state.shiftState,
            onKeyPress = { vm.onKeyPress(it) },
            modifier = Modifier.fillMaxWidth().weight(8f),
        )

        Spacer(Modifier.height(8.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(KeyGap)) {
            ModePill(StatusLines.pyr(state), "Change payments per year") { showSettings = true }
            ModePill(StatusLines.timing(state), "Switch payment timing") {
                vm.setPaymentTiming(if (state.paymentTiming == PaymentTiming.END) PaymentTiming.BEGIN else PaymentTiming.END)
            }
            ModePill(StatusLines.calcMode(state), "Switch between chain and algebraic") {
                vm.setCalcMode(if (state.calcMode == CalcMode.CHAIN) CalcMode.ALGEBRAIC else CalcMode.CHAIN)
            }
            ModePill(StatusLines.angle(state), "Switch angle mode") {
                vm.setAngleMode(if (state.angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG)
            }
        }
        StatusText(StatusLines.values(state), PrimaryLabel, Modifier.padding(horizontal = 12.dp))
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

/** The title row. It moves up beside the camera when nothing there would be covered. */
@Composable
private fun Header(onSettings: () -> Unit) {
    val density = LocalDensity.current
    val view = LocalView.current
    val safeTop = WindowInsets.safeDrawing.getTop(density)
    val cutoutTop = WindowInsets.displayCutout.getTop(density)
    val barTop = WindowInsets.statusBars.getTop(density)
    val besideCamera = remember(cutoutTop, barTop, density, view) {
        val clear = with(density) { HEADER_SIDE.toPx() }
        cutoutTop > 0 && barTop == 0 && cameraBounds(view).let { rects ->
            rects.isNotEmpty() && rects.all { it.left > clear && it.right < view.width - clear }
        }
    }
    val inset = with(density) { safeTop.toDp() }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, top = if (besideCamera) 0.dp else inset)
            .height(if (besideCamera) maxOf(inset, 48.dp) else 48.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "JS 10bII+",
            color = SecondaryLabel,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = HEADER_SIDE - 24.dp),
        )
        SettingsButton(onClick = onSettings)
    }
}

private fun cameraBounds(view: View): List<Rect> =
    if (Build.VERSION.SDK_INT >= 28) view.rootWindowInsets?.displayCutout?.boundingRects.orEmpty() else emptyList()

/** Width at each end of the title row that the camera must leave free. */
private val HEADER_SIDE = 150.dp

/** A setting that changes answers; tapping it changes the setting. */
@Composable
private fun RowScope.ModePill(text: String, action: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(50)
    BoxWithConstraints(
        modifier = Modifier
            .weight(1f)
            .height(32.dp)
            .clip(shape)
            .background(Panel)
            .clickable(onClickLabel = action, role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = SecondaryLabel,
            style = fittedStyle(text, NumberStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), constraints.maxWidth, constraints.maxHeight),
            maxLines = 1,
            softWrap = false,
        )
    }
}

/** A sliders icon, drawn because the icon library is not a dependency. */
@Composable
private fun SettingsButton(onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier = Modifier.semantics { contentDescription = "Settings" }) {
        Canvas(Modifier.size(20.dp)) {
            val stroke = 2.dp.toPx()
            listOf(0.2f to 0.7f, 0.5f to 0.3f, 0.8f to 0.6f).forEach { (y, knob) ->
                drawLine(SecondaryLabel, Offset(0f, size.height * y), Offset(size.width, size.height * y), stroke, StrokeCap.Round)
                drawCircle(CalcBody, 4.5.dp.toPx(), Offset(size.width * knob, size.height * y))
                drawCircle(SecondaryLabel, 2.5.dp.toPx(), Offset(size.width * knob, size.height * y))
            }
        }
    }
}

/** One line that shrinks to fit: a cut-off amount could be misread. The height is fixed so the keys do not move. */
@Composable
private fun StatusText(text: String, color: Color, modifier: Modifier = Modifier) {
    var size by remember(text) { mutableStateOf(12f) }
    Box(modifier = modifier.fillMaxWidth().padding(top = 4.dp).height(17.dp), contentAlignment = Alignment.CenterStart) {
        Text(
            text = text,
            color = color,
            style = NumberStyle.copy(fontSize = size.sp, fontWeight = FontWeight.Medium),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (it.hasVisualOverflow && size > MIN_STATUS_SP) size -= 0.5f },
        )
    }
}

private const val MIN_STATUS_SP = 6f
