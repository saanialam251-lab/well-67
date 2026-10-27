package com.aismartmath.board.board

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.floor

private val GridFaint = Color.White.copy(alpha = 0.07f)
private val AxisColor = Color.White.copy(alpha = 0.44f)
private val LabelColor = Color(0xFF9CA3AF)
private val OutlineColor = Color(0xFF0B1020)

/** Web: f(n,d) = String(+n.toFixed(d)). */
fun fmt(n: Double, d: Int = 2): String {
    var s = String.format(Locale.US, "%.${d}f", n)
    if (s.contains('.')) s = s.trimEnd('0').trimEnd('.')
    if (s == "-0" || s.isEmpty()) s = "0"
    return s
}

/** Web: T(). Bold text with a dark outline so numbers stay readable over lines. Size does not change with zoom. */
fun DrawScope.outlinedText(
    m: TextMeasurer,
    text: String,
    cx: Float,
    cy: Float,
    color: Color = Color.White,
    sizeSp: Float = 12f,
    alpha: Float = 1f,
) {
    val layout = m.measure(text, TextStyle(fontSize = sizeSp.sp, fontWeight = FontWeight.Bold))
    val tl = Offset(cx - layout.size.width / 2f, cy - layout.size.height / 2f)
    drawText(layout, OutlineColor, tl, alpha, drawStyle = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round))
    drawText(layout, color, tl, alpha)
}

/** Web: grid(). Grid and numbers re-space themselves at every zoom level; axes only when [axes]. */
fun DrawScope.drawGrid(cam: Camera, axes: Boolean, m: TextMeasurer) {
    if (cam.w <= 0.0 || cam.h <= 0.0) return
    val u = cam.u
    val st = cam.gridStep
    val x0 = -(cam.w / 2 + cam.ox) / u
    val x1 = (cam.w / 2 - cam.ox) / u
    val y1 = (cam.h / 2 + cam.oy) / u
    val y0 = -(cam.h / 2 - cam.oy) / u
    val i0 = floor(x0 / st).toInt()
    val i1 = ceil(x1 / st).toInt()
    val j0 = floor(y0 / st).toInt()
    val j1 = ceil(y1 / st).toInt()
    val w = size.width
    val h = size.height
    val lw = 1.dp.toPx()

    for (i in i0..i1) {
        val x = (cam.w / 2 + cam.ox + i * st * u).toFloat()
        drawLine(if (i == 0 && axes) AxisColor else GridFaint, Offset(x, 0f), Offset(x, h), lw)
    }
    for (j in j0..j1) {
        val y = (cam.h / 2 + cam.oy - j * st * u).toFloat()
        drawLine(if (j == 0 && axes) AxisColor else GridFaint, Offset(0f, y), Offset(w, y), lw)
    }
    if (axes) {
        val ly = (cam.h / 2 + cam.oy + 11.dp.toPx()).toFloat().coerceIn(9.dp.toPx(), h - 9.dp.toPx())
        val lx = (cam.w / 2 + cam.ox - 10.dp.toPx()).toFloat().coerceIn(12.dp.toPx(), w - 12.dp.toPx())
        for (i in i0..i1) if (i != 0) {
            outlinedText(m, fmt(i * st, 4), (cam.w / 2 + cam.ox + i * st * u).toFloat(), ly, LabelColor, 10f)
        }
        for (j in j0..j1) if (j != 0) {
            outlinedText(m, fmt(j * st, 4), lx, (cam.h / 2 + cam.oy - j * st * u).toFloat(), LabelColor, 10f)
        }
    }
}
