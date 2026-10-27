package com.aismartmath.board.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.math.MathNode

/** Draws a [MathNode] natively: fractions stacked over a bar, raised powers, square roots. */
@Composable
fun MathView(node: MathNode, size: TextUnit = 14.sp, color: Color = Color.White, modifier: Modifier = Modifier) {
    when (node) {
        is MathNode.Txt -> Text(
            node.s,
            modifier = modifier,
            color = color,
            fontSize = size,
            fontStyle = if (node.s.length == 1 && node.s[0].isLetter()) FontStyle.Italic else FontStyle.Normal,
        )
        is MathNode.Seq -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            node.items.forEach { MathView(it, size, color) }
        }
        is MathNode.Paren -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Text("(", color = color, fontSize = size)
            MathView(node.n, size, color)
            Text(")", color = color, fontSize = size)
        }
        is MathNode.Sup -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            MathView(node.base, size, color)
            Box(Modifier.offset(y = (-(size.value * 0.45f)).dp)) { MathView(node.exp, size * 0.7f, color) }
        }
        is MathNode.Frac -> Column(
            modifier.width(IntrinsicSize.Max).padding(horizontal = 3.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            MathView(node.n, size * 0.9f, color)
            Box(Modifier.fillMaxWidth().height(1.dp).background(color.copy(alpha = 0.9f)))
            MathView(node.d, size * 0.9f, color)
        }
        is MathNode.Sqrt -> Row(modifier, verticalAlignment = Alignment.CenterVertically) {
            Text("√", color = color, fontSize = size)
            Column(Modifier.width(IntrinsicSize.Max)) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(color.copy(alpha = 0.9f)))
                MathView(node.n, size, color)
            }
        }
    }
}
