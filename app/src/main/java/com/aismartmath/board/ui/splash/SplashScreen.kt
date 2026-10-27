package com.aismartmath.board.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.ui.theme.BoardColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Same splash as the web version: triangle + circle draw themselves, title and tagline rise, loading bar fills. */
@Composable
fun SplashScreen(onFinished: () -> Unit) {
    val draw = remember { Animatable(0f) }
    val title = remember { Animatable(0f) }
    val tag = remember { Animatable(0f) }
    val bar = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        launch { draw.animateTo(1f, tween(1600, easing = FastOutSlowInEasing)) }
        launch { delay(300); title.animateTo(1f, tween(800)) }
        launch { delay(600); tag.animateTo(1f, tween(800)) }
        launch { bar.animateTo(1f, tween(2400, easing = LinearEasing)) }
        delay(2700)
        onFinished()
    }

    val pulseT = rememberInfiniteTransition(label = "pulse")
    val pulse by pulseT.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1200, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulseScale",
    )

    Box(
        Modifier
            .fillMaxSize()
            .drawBehind {
                drawRect(
                    Brush.radialGradient(
                        colors = listOf(BoardColors.Glow, BoardColors.Bg),
                        center = Offset(size.width * 0.5f, size.height * 0.35f),
                        radius = maxOf(size.width, size.height) * 0.7f,
                    ),
                )
            },
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Canvas(
                Modifier
                    .size(130.dp)
                    .graphicsLayer { scaleX = pulse; scaleY = pulse },
            ) {
                val s = size.width / 120f
                val len = 330f * s
                val dash = PathEffect.dashPathEffect(floatArrayOf(len, len), len * (1f - draw.value))
                val tri = Path().apply {
                    moveTo(20f * s, 100f * s)
                    lineTo(60f * s, 20f * s)
                    lineTo(100f * s, 100f * s)
                    close()
                }
                val c = Offset(60f * s, 72f * s)
                val r = 26f * s
                fun stroke(w: Float) = Stroke(width = w * s, cap = StrokeCap.Round, join = StrokeJoin.Round, pathEffect = dash)
                // soft glow underneath, then the crisp lines
                drawPath(tri, BoardColors.Indigo.copy(alpha = 0.25f), style = stroke(11f))
                drawCircle(BoardColors.Teal.copy(alpha = 0.25f), r, c, style = stroke(11f))
                drawPath(tri, BoardColors.Indigo, style = stroke(4f))
                drawCircle(BoardColors.Teal, r, c, style = stroke(4f))
            }
            Spacer(Modifier.height(14.dp))
            Text(
                "Smart Math Board",
                color = Color.White,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer {
                    alpha = title.value
                    translationY = (1f - title.value) * 14.dp.toPx()
                },
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Draw · Measure · Learn",
                color = Color.White,
                modifier = Modifier.graphicsLayer {
                    alpha = 0.6f * tag.value
                    translationY = (1f - tag.value) * 14.dp.toPx()
                },
            )
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .width(160.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color.White.copy(alpha = 0.13f)),
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(bar.value)
                        .background(BoardColors.Indigo),
                )
            }
        }
    }
}
