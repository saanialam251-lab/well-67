package com.aismartmath.board.board

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.ui.components.RichLine
import com.aismartmath.board.ui.theme.BoardColors
import kotlin.math.roundToInt
import com.aismartmath.board.model.Fig

/** Web: editHTML - actions, dimensions, size / rotate, extras, colours and Details. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditPanel(state: BoardState, f: Fig) {
    val accent = f.col ?: figColor(f.t)
    val border by animateColorAsState(accent, tween(300), label = "panelBorder")
    val inf = rememberInfiniteTransition(label = "panelGlow")
    val glowF by inf.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "glow",
    )
    val glow = glowF.dp
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(glow, shape, clip = false, ambientColor = border, spotColor = border)
            .clip(shape)
            .background(BoardColors.Bg.copy(alpha = 0.96f))
            .border(BorderStroke(1.5.dp, border), shape)
            .heightIn(max = 320.dp)
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("✏️ ${FigNames[f.t] ?: f.t}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            ActionBtn("✕", "", accent) { state.closeEdit() }
        }

        if (f.t == "fx") RichLine(graphParts(f).take(1))

        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionBtn("📋", "Copy", accent) { state.copySel(f, false) }
            ActionBtn("✂️", "Cut", accent) { state.copySel(f, true) }
            ActionBtn("🗑️", "Delete", accent) { state.delete(f); state.showToast("Deleted 🗑️ — undo (↶) brings it back") }
            if (f.t != "fx") ActionBtn("✥", "Move", accent) {
                state.mv = f; state.rs = null; state.closeEdit(clearSel = false); state.sel = f
                state.showToast("Move mode ✥ — drag the figure, tap empty space when done")
            }
            if (f.t != "pt" && f.t != "fx") ActionBtn("⤢", "Resize", accent) {
                state.rs = f; state.mv = null; state.closeEdit(clearSel = false); state.sel = f
                state.showToast("Resize ⤢ — drag outward or inward, tap empty space when done")
            }
            if (f.t in setOf("tri", "quad", "pent", "hex")) {
                ActionBtn("✦", "Regular", accent) { state.makeRegular(f) }
            }
            if (f.t in setOf("tri", "quad", "rect", "rtri", "pent", "hex")) {
                ActionBtn("🧱", "3D prism", accent) { state.showToast("3D from your own shape arrives in Phase 7") }
                ActionBtn("🔺", "Pyramid", accent) { state.showToast("3D from your own shape arrives in Phase 7") }
            }
            ActionBtn("ℹ️", "Details", accent) { state.detailsOn = !state.detailsOn }
        }

        fieldsOf(f).forEach { fld ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(fld.label, color = Color.White, fontSize = 13.sp, modifier = Modifier.weight(1f))
                NumField(fld.value, fld.ro, accent) { state.editField(f, fld.key, it) }
            }
        }

        if (f.t == "fx" && f.gd?.dv == 0) {
            listOf("a" to f.ga, "b" to f.gb, "c" to f.gc).forEach { (k, v) ->
                SliderRow(k, v.toFloat().coerceIn(-10f, 10f), -10f..10f, fmt(v, 1), accent) {
                    state.editField(f, k, (it * 10).roundToInt() / 10.0)
                }
            }
        }

        if (f.t != "pt" && f.t != "fx") {
            SliderRow("Size", (f.sz * 100).toFloat().coerceIn(20f, 300f), 20f..300f, "${(f.sz * 100).roundToInt()}%", accent) {
                state.editField(f, "size", it.roundToInt().toDouble())
            }
        }
        if (f.t != "pt" && f.t != "circ" && f.t != "fx") {
            SliderRow("Rotate", f.rot.toFloat().coerceIn(-180f, 180f), -180f..180f, "${f.rot.roundToInt()}°", accent) {
                state.editField(f, "rot", it.roundToInt().toDouble())
            }
        }

        val toggles = when (f.t) {
            "tri", "rtri" -> listOf(
                "med" to "Medians", "alt" to "Altitudes", "bis" to "Bisectors", "cen" to "Centroid G",
                "orth" to "Orthocentre H", "inc" to "Incircle", "circ" to "Circumcircle",
                "subt" to "Angle at centre", "mid" to "Midpoints",
            )
            "quad", "rect", "pent", "hex" -> listOf(
                "diag" to "Diagonals", "mid" to "Midpoints", "cen" to "Centroid G", "cir" to "Circumcircle",
            )
            "circ" -> listOf("dia" to "Diameter")
            else -> emptyList()
        }
        if (toggles.isNotEmpty()) {
            FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                toggles.forEach { (k, label) ->
                    ToggleChip(label, k in f.ov, accent) {
                        state.edit(f, "Toggled " + label) { f.ov = if (k in f.ov) f.ov - k else f.ov + k }
                    }
                }
            }
        }

        // colour swatches
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Swatches.forEach { c ->
                Swatch(c, (f.col ?: figColor(f.t)) == c) { state.edit(f, "Colour") { f.col = c } }
            }
        }

        if (state.detailsOn) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color.White.copy(alpha = 0.06f))
                    .border(BorderStroke(1.dp, border.copy(alpha = 0.5f)), RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                propsOf(f, state.parGeo()).forEach { RichLine(it) }
            }
        }
        if (f.t == "quad") {
            Text("Change any side or the diagonal AC — the shape redraws to match", color = BoardColors.Muted, fontSize = 11.sp)
        } else if (f.t == "pent" || f.t == "hex") {
            Text("Drag the ● handles to reshape, or tap ✦ Regular to even it out", color = BoardColors.Muted, fontSize = 11.sp)
        } else if (f.t != "fx") {
            Text("Drag the ● handles to reshape", color = BoardColors.Muted, fontSize = 11.sp)
        }
    }
}

@Composable
private fun ActionBtn(icon: String, label: String, accent: Color, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) 0.9f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "actPress",
    )
    val bd by animateColorAsState(if (pressed) accent else Color.White.copy(alpha = 0.18f), tween(120), label = "actBorder")
    Column(
        Modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(RoundedCornerShape(10.dp))
            .background(Color.White.copy(alpha = 0.07f))
            .border(BorderStroke(1.dp, bd), RoundedCornerShape(10.dp))
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 16.sp, color = Color.White)
        if (label.isNotEmpty()) Text(label, fontSize = 10.sp, color = Color.White)
    }
}

@Composable
private fun ToggleChip(label: String, on: Boolean, accent: Color, onClick: () -> Unit) {
    val bg by animateColorAsState(if (on) accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.07f), tween(200), label = "chipBg")
    val bd by animateColorAsState(if (on) accent else Color.White.copy(alpha = 0.18f), tween(200), label = "chipBd")
    Box(
        Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .border(BorderStroke(1.5.dp, bd), RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 5.dp),
    ) { Text(label, color = Color.White, fontSize = 12.sp) }
}

@Composable
private fun Swatch(c: Color, on: Boolean, onClick: () -> Unit) {
    val ring by animateFloatAsState(if (on) 1f else 0f, tween(200), label = "ring")
    val lift by animateFloatAsState(if (on) 1.15f else 1f, spring(dampingRatio = 0.5f), label = "swLift")
    Box(
        Modifier
            .graphicsLayer { scaleX = lift; scaleY = lift }
            .size(26.dp)
            .clip(CircleShape)
            .background(c)
            .border(BorderStroke(3.dp, Color.White.copy(alpha = ring)), CircleShape)
            .clickable(onClick = onClick),
    )
}

@Composable
private fun SliderRow(label: String, value: Float, range: ClosedFloatingPointRange<Float>, shown: String, accent: Color, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Color.White, fontSize = 13.sp, modifier = Modifier.width(54.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Color.White.copy(alpha = 0.15f),
            ),
        )
        Text(shown, color = BoardColors.Muted, fontSize = 12.sp, modifier = Modifier.width(48.dp))
    }
}

@Composable
private fun NumField(value: Double, readOnly: Boolean, accent: Color, onValue: (Double) -> Unit) {
    var text by remember { mutableStateOf(fmt(value)) }
    var focused by remember { mutableStateOf(false) }
    LaunchedEffect(value) { if (!focused) text = fmt(value) }
    val bd by animateColorAsState(if (focused) accent else Color.White.copy(alpha = 0.2f), tween(180), label = "fieldBd")
    BasicTextField(
        value = text,
        onValueChange = {
            text = it
            it.toDoubleOrNull()?.let(onValue)
        },
        readOnly = readOnly,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        textStyle = TextStyle(color = if (readOnly) BoardColors.Muted else Color.White, fontSize = 14.sp),
        cursorBrush = SolidColor(accent),
        modifier = Modifier
            .width(96.dp)
            .onFocusChanged { focused = it.isFocused }
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.09f))
            .border(BorderStroke(1.5.dp, bd), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 7.dp),
    )
}
