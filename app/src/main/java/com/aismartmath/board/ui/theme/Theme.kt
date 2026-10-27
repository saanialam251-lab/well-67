package com.aismartmath.board.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object BoardColors {
    val Bg = Color(0xFF0B1020)
    val Glow = Color(0xFF1E2A5A)
    val Indigo = Color(0xFF818CF8)
    val Teal = Color(0xFF2DD4BF)
    val Muted = Color(0xFF9CA3AF)
}

@Composable
fun SmartMathTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = BoardColors.Indigo,
            background = BoardColors.Bg,
            surface = BoardColors.Bg,
            onBackground = Color.White,
            onSurface = Color.White,
        ),
        content = content,
    )
}
