package com.aismartmath.board.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.ui.theme.BoardColors
import kotlin.math.roundToInt

private val EraserColor = Color(0xFFE5E7EB)

/** Eraser options: rub out just the part under the finger, or the whole stroke; and how big the eraser is. */
@Composable
fun EraserPanel(state: BoardState) {
    val shape = RoundedCornerShape(14.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, clip = false, ambientColor = EraserColor, spotColor = EraserColor)
            .clip(shape)
            .background(BoardColors.Bg.copy(alpha = 0.95f))
            .border(BorderStroke(1.dp, EraserColor.copy(alpha = 0.5f)), shape)
            .padding(10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Erase", color = Color.White, fontSize = 13.sp, modifier = Modifier.width(48.dp))
            Chip("Only the part I touch", state.erasePart, EraserColor) { state.erasePart = true }
            Chip("Whole stroke", !state.erasePart, EraserColor) { state.erasePart = false }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Size", color = Color.White, fontSize = 13.sp, modifier = Modifier.width(48.dp))
            Slider(
                value = state.eraserDp,
                onValueChange = { state.eraserDp = it.roundToInt().toFloat() },
                valueRange = 6f..40f,
                modifier = Modifier.weight(1f),
                colors = SliderDefaults.colors(
                    thumbColor = EraserColor,
                    activeTrackColor = EraserColor,
                    inactiveTrackColor = Color.White.copy(alpha = 0.15f),
                ),
            )
            Text("${state.eraserDp.roundToInt()}", color = BoardColors.Muted, fontSize = 12.sp, modifier = Modifier.width(30.dp))
        }
    }
}
