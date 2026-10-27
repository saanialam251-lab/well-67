package com.aismartmath.board.board

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.math.eqMath
import com.aismartmath.board.math.tidyTyping
import com.aismartmath.board.ui.components.MathView
import com.aismartmath.board.ui.theme.BoardColors
import kotlin.math.roundToInt

private val Gold = Color(0xFFFBBF24)

/** Web: the equation panel - typed equation with live maths, Plot, presets, a / b / c sliders, Animate and f' / f''. */
@Composable
fun GraphPanel(state: BoardState) {
    val shape = RoundedCornerShape(14.dp)
    var tf by remember { mutableStateOf(TextFieldValue(state.eq)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(state.eq) { if (tf.text != state.eq) tf = TextFieldValue(state.eq, TextRange(state.eq.length)) }
    val fieldBorder by animateColorAsState(if (focused) Gold else Color.White.copy(alpha = 0.2f), tween(180), label = "eqBorder")

    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, clip = false, ambientColor = Gold, spotColor = Gold)
            .clip(shape)
            .background(BoardColors.Bg.copy(alpha = 0.95f))
            .border(BorderStroke(1.dp, Gold.copy(alpha = 0.55f)), shape)
            .heightIn(max = 300.dp)
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(Modifier.horizontalScroll(rememberScrollState())) { MathView(eqMath(state.eq), 18.sp) }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            BasicTextField(
                value = tf,
                onValueChange = { nv ->
                    val o = nv.text
                    val n = tidyTyping(o)
                    tf = if (n != o) {
                        val q = (nv.selection.start - (o.length - n.length)).coerceIn(0, n.length)
                        TextFieldValue(n, TextRange(q))
                    } else nv
                    state.eq = n
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { state.plot() }),
                textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                cursorBrush = SolidColor(Gold),
                modifier = Modifier
                    .weight(1f)
                    .onFocusChanged { focused = it.isFocused }
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.09f))
                    .border(BorderStroke(1.5.dp, fieldBorder), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            )
            PanelButton("Plot", true) { state.plot() }
        }

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PRESETS.forEachIndexed { i, p -> PanelButton(p.name, false) { state.preset(i) } }
        }

        listOf("a" to state.gpA, "b" to state.gpB, "c" to state.gpC).forEach { (k, v) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(k, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(22.dp))
                Slider(
                    value = v.toFloat().coerceIn(-10f, 10f),
                    onValueChange = { state.setParam(k, (it * 10).roundToInt() / 10.0) },
                    valueRange = -10f..10f,
                    modifier = Modifier.weight(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = Gold,
                        activeTrackColor = Gold,
                        inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                    ),
                )
                Text(fmt(v, 1), color = BoardColors.Muted, fontSize = 12.sp, modifier = Modifier.width(40.dp))
            }
        }

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelButton(if (state.animA) "⏸ Animate a" else "▶ Animate a", state.animA) { state.animA = !state.animA }
            if (state.tool == "dv") {
                PanelButton("ƒ′ Derivative", false) { state.addDv(1) }
                PanelButton("ƒ″ Second", false) { state.addDv(2) }
            }
        }
    }
}

@Composable
private fun PanelButton(label: String, on: Boolean, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "pbPress",
    )
    val bg by animateColorAsState(if (on) Gold.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.07f), tween(200), label = "pbBg")
    val bd by animateColorAsState(if (on || pressed) Gold else Color.White.copy(alpha = 0.18f), tween(150), label = "pbBd")
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.5.dp, bd), shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) { Text(label, color = Color.White, fontSize = 13.sp) }
}
