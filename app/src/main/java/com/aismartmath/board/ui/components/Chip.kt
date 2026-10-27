package com.aismartmath.board.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** A pill button whose border and fill animate to the accent colour when it is on. */
@Composable
fun Chip(label: String, on: Boolean, accent: Color, onClick: () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val sc by animateFloatAsState(
        if (pressed) 0.92f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "chipPress",
    )
    val bg by animateColorAsState(if (on) accent.copy(alpha = 0.28f) else Color.White.copy(alpha = 0.07f), tween(200), label = "chipBg")
    val bd by animateColorAsState(if (on || pressed) accent else Color.White.copy(alpha = 0.18f), tween(160), label = "chipBd")
    val shape = RoundedCornerShape(16.dp)
    Box(
        Modifier
            .graphicsLayer { scaleX = sc; scaleY = sc }
            .clip(shape)
            .background(bg)
            .border(BorderStroke(1.5.dp, bd), shape)
            .clickable(interactionSource = src, indication = null, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    ) { Text(label, color = Color.White, fontSize = 12.sp) }
}
