package com.aismartmath.board.board

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.aismartmath.board.ui.components.Chip
import com.aismartmath.board.ui.theme.BoardColors

private val ParBlue = Color(0xFF60A5FA)

/** Web: pxPanel - choose which family of angles to highlight on the parallel lines. */
@Composable
fun ParPanel(state: BoardState) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, shape, clip = false, ambientColor = ParBlue, spotColor = ParBlue)
            .clip(shape)
            .background(BoardColors.Bg.copy(alpha = 0.95f))
            .border(BorderStroke(1.dp, ParBlue.copy(alpha = 0.6f)), shape)
            .horizontalScroll(rememberScrollState())
            .padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        listOf(
            "all" to "All",
            "cor" to "Corresponding",
            "alt" to "Alt. interior",
            "ext" to "Alt. exterior",
            "co" to "Co-interior",
            "vert" to "Vertical",
        ).forEach { (key, label) ->
            Chip(label, state.pxMode == key && state.pxHl == 0, ParBlue) {
                state.pxMode = key
                state.pxHl = 0
            }
        }
        Chip("∠ Bisectors", state.pxBis, ParBlue) { state.pxBis = !state.pxBis }
        Chip("✕", false, ParBlue) { state.pxOn = false }
    }
}
