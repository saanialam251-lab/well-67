package com.aismartmath.board.board

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aismartmath.board.math.dist
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.Topic
import com.aismartmath.board.model.Tools
import com.aismartmath.board.ui.components.IconBtn
import com.aismartmath.board.ui.components.RichLine
import com.aismartmath.board.ui.theme.BoardColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun BoardScreen(topic: Topic, onBack: () -> Unit) {
    val density = LocalDensity.current.density
    val scope = rememberCoroutineScope()
    val state = remember(topic.id) {
        BoardState(topic, scope).also {
            it.camera.density = density.toDouble()
            it.camera.oy = it.initialOy()
            it.start()
        }
    }
    val cam = state.camera
    var full by rememberSaveable { mutableStateOf(false) }
    var minimised by rememberSaveable { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        AnimatedVisibility(
            visible = !full,
            enter = expandVertically(tween(250)) + fadeIn(tween(250)),
            exit = shrinkVertically(tween(250)) + fadeOut(tween(200)),
        ) {
            Column {
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    IconBtn("←", onClick = onBack)
                    Text(
                        "${topic.icon}  ${topic.name}",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    IconBtn("↶", enabled = state.canUndo) { state.undo() }
                    IconBtn("↷", enabled = state.canRedo) { state.redo() }
                    IconBtn("🔗", dim = !state.autoJoin) {
                        state.autoJoin = !state.autoJoin
                        state.showToast(if (state.autoJoin) "Auto-join ON — lines that close up become a shape" else "Auto-join OFF")
                    }
                    IconBtn("🗑") { state.clear() }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    itemsIndexed(topic.toolIds) { i, id ->
                        val tool = Tools.byId.getValue(id)
                        ToolChip(tool.icon, tool.name, state.tool == id, i) { state.selectTool(id) }
                    }
                }
            }
        }

        Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
            BoardCanvas(state, topic.axes)

            Column(
                Modifier.align(Alignment.TopEnd).padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.End,
            ) {
                IconBtn("＋", floating = true) { scope.launch { cam.animateZoomAt(cam.w / 2, cam.h / 2, 1.3) } }
                IconBtn("－", floating = true) { scope.launch { cam.animateZoomAt(cam.w / 2, cam.h / 2, 0.77) } }
                IconBtn("⌂", floating = true) { scope.launch { cam.animateReset(state.initialOy()) } }
                if (state.parGeo() != null) {
                    IconBtn("∥", floating = true, dim = !state.pxOn) { state.pxOn = !state.pxOn }
                }
                IconBtn(if (full) "✕" else "⛶", floating = true) { full = !full }
                ZoomBadge(cam.z)
            }

            // bottom panel (tool hint or edit panel) with its minimise button at the top-left
            androidx.compose.animation.AnimatedVisibility(
                visible = !full,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(tween(250)) { it } + fadeIn(tween(250)),
                exit = slideOutVertically(tween(200)) { it } + fadeOut(tween(150)),
            ) {
                Column(
                    Modifier.fillMaxWidth().widthIn(max = 700.dp).padding(8.dp),
                    horizontalAlignment = Alignment.Start,
                ) {
                    MinButton(minimised) { minimised = !minimised }
                    AnimatedVisibility(
                        visible = !minimised,
                        enter = expandVertically(spring(stiffness = Spring.StiffnessMediumLow)) + fadeIn(),
                        exit = shrinkVertically(tween(180)) + fadeOut(tween(120)),
                    ) {
                        val ed = state.editing?.takeIf { state.shapes.contains(it) }
                        AnimatedContent(
                            targetState = ed,
                            transitionSpec = {
                                (fadeIn(tween(220)) togetherWith fadeOut(tween(150))).using(SizeTransform(clip = false))
                            },
                            label = "dock",
                        ) { e ->
                            if (e != null) {
                                EditPanel(state, e)
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    AnimatedVisibility(
                                        visible = state.isGraphTool,
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                    ) { GraphPanel(state) }
                                    AnimatedVisibility(
                                        visible = state.isPenTool,
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                    ) { PenPanel(state) }
                                    AnimatedVisibility(
                                        visible = state.isEraserTool,
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                    ) { EraserPanel(state) }
                                    AnimatedVisibility(
                                        visible = state.pxOn && state.parGeo() != null,
                                        enter = expandVertically(tween(250)) + fadeIn(tween(250)),
                                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                                    ) { ParPanel(state) }
                                    InfoCard(
                                        infoOf(state),
                                        if (state.isGraphTool) Color(0xFFFBBF24)
                                        else if (state.isPenTool) state.penCol
                                        else state.sel?.let { it.col ?: figColor(it.t) } ?: Color(0xFF818CF8),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            ToastPill(state.toastMsg, Modifier.align(Alignment.TopCenter).padding(top = 10.dp))
            MenuOverlay(state)
        }
    }
}

/** Info bar content. Equality is by [key] so the bar only animates when the words really change. */
private class Info(val key: String, val parts: List<Part>) {
    override fun equals(other: Any?) = other is Info && other.key == key
    override fun hashCode() = key.hashCode()
}

private fun infoOf(state: BoardState): Info {
    fun txt(s: String) = Info(s, listOf(Part.T(s)))
    state.msg.takeIf { it.isNotEmpty() }?.let { return txt(it) }
    if (state.pxOn) {
        val g = state.parGeo()
        if (g != null) {
            val parts = pxText(g, state.pxMode, state.pxHl)
            return Info("px:" + partsKey(parts), parts)
        }
    }
    state.mv?.let { return txt("Move mode ✥ — drag the figure, tap empty space when done") }
    state.rs?.let { return txt("Resize ⤢ — drag outward or inward, tap empty space when done") }
    state.draft?.let { d ->
        val a = d.p[0]
        val b = d.p[1]
        return txt(
            when (d.t) {
                "seg" -> "Length ${fmt(dist(a, b))}"
                "circ" -> "Radius ${fmt(dist(a, b))}"
                else -> "${fmt(abs(b.x - a.x))} × ${fmt(abs(b.y - a.y))}"
            },
        )
    }
    state.cursor?.let {
        val k = state.pend.size + 1
        return txt(
            if (state.need > 1) "Point $k of ${state.need} · (${fmt(it.x)}, ${fmt(it.y)})"
            else "Point (${fmt(it.x)}, ${fmt(it.y)})",
        )
    }
    if (state.pend.isNotEmpty()) return txt("Tap point ${state.pend.size + 1} of ${state.need}")

    // web: the info bar describes the active figure (a graph while the Graph / Derivative tools are open)
    val act = state.activeFig()
    if (act != null) {
        val parts: List<Part> = if (act.t == "fx") {
            graphParts(act) + (state.trace?.takeIf { it.f === act }?.let { traceParts(act, it.x) } ?: emptyList())
        } else {
            val out = mutableListOf<Part>()
            propsOf(act, state.parGeo()).forEachIndexed { i, line ->
                if (i > 0) out.add(Part.T(" · "))
                out.addAll(line)
            }
            out
        }
        return Info(partsKey(parts), parts)
    }
    val tool = Tools.byId.getValue(state.tool)
    val phase = toolPhase(tool.id)
    return txt(
        if (phase <= 5) "${tool.icon}  ${tool.name} — ${tool.hint}"
        else "${tool.icon}  ${tool.name} — ${tool.hint}  ·  arrives in Phase $phase",
    )
}

@Composable
private fun BoardCanvas(state: BoardState, axes: Boolean) {
    val cam = state.camera
    val measurer = rememberTextMeasurer(cacheSize = 256)

    // tick the clock while anything is popping in or fading out
    LaunchedEffect(state.animTick) {
        val start = System.nanoTime()
        while (true) {
            val t = withFrameNanos { it }
            state.clock = t
            if (t - start > 700_000_000L) break
        }
    }

    // web: "Animate a" - the a slider swings back and forth
    LaunchedEffect(state.animA) {
        if (state.animA) {
            while (true) {
                val t = withFrameNanos { it }
                val v = Math.round(5 * sin(t / 1e6 / 900.0) * 100) / 100.0
                state.lastFx()?.let { it.ga = v }
                state.gpA = v
            }
        }
    }

    val inf = rememberInfiniteTransition(label = "sel")
    val dashPhase by inf.animateFloat(
        initialValue = 0f,
        targetValue = 14f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing)),
        label = "dash",
    )
    val pulse by inf.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pulse",
    )

    Canvas(
        Modifier
            .fillMaxSize()
            .onSizeChanged {
                cam.w = it.width.toDouble()
                cam.h = it.height.toDouble()
            }
            .boardGestures(state),
    ) {
        drawGrid(cam, axes, measurer)
        val clock = state.clock
        val shapes = state.shapes.toList()
        val quiet = quietSet(shapes, state.editing ?: state.sel)

        for (s in shapes) {
            if (s.t == "ink") {
                val dk = if (s.dieAt != 0L) ((clock - s.dieAt) / 240_000_000.0).toFloat().coerceIn(0f, 1f) else 0f
                drawInk(cam, s, 1f - dk)
                continue
            }
            if (s.t == "fx") {
                if (s.dieAt != 0L) {
                    val dk = ((clock - s.dieAt) / 240_000_000.0).toFloat().coerceIn(0f, 1f)
                    drawGraph(cam, s, 1f, 1f - dk)
                } else {
                    val k = ((clock - s.born) / 500_000_000.0).toFloat().coerceIn(0f, 1f)
                    drawGraph(cam, s, k, 1f)
                }
                continue
            }
            if (s.dieAt != 0L) {
                val dk = ((clock - s.dieAt) / 240_000_000.0).toFloat().coerceIn(0f, 1f)
                drawFig(cam, measurer, s, 1f - 0.3f * dk, 1f - dk, s in quiet, axes)
            } else {
                val k = ((clock - s.born) / 500_000_000.0).toFloat().coerceIn(0f, 1f)
                drawFig(cam, measurer, s, easeOutBack(k), minOf(1f, k * 2.5f), s in quiet, axes)
            }
        }
        drawCrossings(cam, shapes, quiet)

        val sel = state.sel
        if (sel != null && sel.t != "fx" && sel.dieAt == 0L && shapes.contains(sel)) {
            drawSelection(cam, sel, dashPhase, pulse, state.mv === sel || state.rs === sel)
        }
        val ed = state.editing
        if (ed != null && ed.t != "fx" && ed.dieAt == 0L && shapes.contains(ed)) drawHandles(cam, ed, pulse)
        if (state.pxOn) state.parGeo()?.let { drawParallel(cam, measurer, it, state.pxMode, state.pxHl, state.pxBis) }
        state.trace?.let { tr ->
            if (tr.f.dieAt == 0L && shapes.contains(tr.f)) drawTrace(cam, measurer, tr.f, tr.x, pulse)
        }

        if (state.pend.isNotEmpty() || state.cursor != null) {
            if (state.isTapTool && state.tool != "pt") drawPending(cam, measurer, state.pend.toList(), state.cursor, state.tool)
            else state.cursor?.let { drawPending(cam, measurer, emptyList(), it, state.tool) }
        }
        state.draft?.let { drawFig(cam, measurer, it, 1f, 0.7f, false, axes) }

        // free-hand: the stroke in progress, the eraser ring and the fading sketch
        if (state.skP.size > 1 && state.tool != "er") {
            drawLiveStroke(state.skP.toList(), state.tool, state.penCol, state.penW)
        }
        if (state.tool == "er") state.penAt?.let { drawEraserRing(it, state.eraserDp, pulse) }
        if (state.ghost.isNotEmpty()) {
            val fade = 1f - ((clock - state.ghostAt) / 450_000_000.0).toFloat().coerceIn(0f, 1f)
            drawGhost(cam, state.ghost, fade)
        }
    }
}

@Composable
private fun ToastPill(text: String?, modifier: Modifier) {
    val last = remember { arrayOf("") }
    if (text != null) last[0] = text
    val shape = RoundedCornerShape(20.dp)
    AnimatedVisibility(
        visible = text != null,
        modifier = modifier,
        enter = slideInVertically(spring(dampingRatio = 0.6f, stiffness = 300f)) { -it * 2 } + fadeIn(tween(150)),
        exit = slideOutVertically(tween(200)) { -it * 2 } + fadeOut(tween(200)),
    ) {
        Box(
            Modifier
                .padding(horizontal = 16.dp)
                .shadow(10.dp, shape, clip = false, ambientColor = BoardColors.Indigo, spotColor = BoardColors.Indigo)
                .clip(shape)
                .background(BoardColors.Bg.copy(alpha = 0.96f))
                .border(BorderStroke(1.5.dp, BoardColors.Indigo), shape)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            Text(last[0], color = Color.White, fontSize = 13.sp)
        }
    }
}

@Composable
private fun MenuOverlay(state: BoardState) {
    val menu = state.menu
    val last = remember { arrayOf<MenuState?>(null) }
    if (menu != null) last[0] = menu
    val d = LocalDensity.current.density
    val accent = state.sel?.let { it.col ?: figColor(it.t) } ?: BoardColors.Indigo
    val border by animateColorAsState(accent, tween(250), label = "menuBorder")
    AnimatedVisibility(
        visible = menu != null,
        enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = 350f), initialScale = 0.5f) + fadeIn(tween(120)),
        exit = scaleOut(tween(120), targetScale = 0.8f) + fadeOut(tween(120)),
    ) {
        val m = last[0]
        if (m != null) {
            val wPx = (m.items.size * 66 + 12) * d
            val left = (m.x - wPx / 2).coerceIn(6f * d, (state.camera.w.toFloat() - wPx - 6f * d).coerceAtLeast(6f * d))
            val top = (m.y - 84f * d).coerceIn(6f * d, (state.camera.h.toFloat() - 90f * d).coerceAtLeast(6f * d))
            val shape = RoundedCornerShape(14.dp)
            Row(
                Modifier
                    .offset { IntOffset(left.roundToInt(), top.roundToInt()) }
                    .shadow(16.dp, shape, clip = false, ambientColor = border, spotColor = border)
                    .clip(shape)
                    .background(BoardColors.Bg.copy(alpha = 0.97f))
                    .border(BorderStroke(1.5.dp, border), shape)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                m.items.forEach { it0 ->
                    Column(
                        Modifier
                            .width(66.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { state.menuAction(it0.key) }
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(it0.icon, fontSize = 20.sp, color = Color.White)
                        Text(it0.label, fontSize = 11.sp, color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
private fun ZoomBadge(z: Double) {
    val pct = z * 100
    val text = if (pct >= 100) "${pct.toLong()}%" else "${fmt(pct, 1)}%"
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xCC0B1020))
            .padding(horizontal = 6.dp, vertical = 3.dp),
    ) {
        Text(text, color = BoardColors.Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun MinButton(minimised: Boolean, onClick: () -> Unit) {
    val rot by animateFloatAsState(if (minimised) 180f else 0f, tween(250), label = "minRot")
    Row(
        Modifier
            .padding(bottom = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.13f))
            .clickable(onClick = onClick)
            .height(26.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("▾", color = Color.White, fontSize = 13.sp, modifier = Modifier.graphicsLayer { rotationZ = rot })
        Text(if (minimised) "Show" else "Hide", color = Color.White, fontSize = 12.sp)
    }
}

@Composable
private fun InfoCard(info: Info, accent: Color) {
    val border by animateColorAsState(accent.copy(alpha = 0.55f), tween(300), label = "infoBorder")
    val shape = RoundedCornerShape(12.dp)
    AnimatedContent(
        targetState = info,
        transitionSpec = {
            (slideInVertically(tween(220)) { it / 2 } + fadeIn(tween(220))) togetherWith
                (slideOutVertically(tween(160)) { -it / 2 } + fadeOut(tween(120)))
        },
        label = "info",
    ) { t ->
        Box(
            Modifier
                .fillMaxWidth()
                .shadow(8.dp, shape, clip = false, ambientColor = accent, spotColor = accent)
                .clip(shape)
                .background(BoardColors.Bg.copy(alpha = 0.94f))
                .border(BorderStroke(1.dp, border), shape)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            RichLine(t.parts)
        }
    }
}

@Composable
private fun ToolChip(icon: String, name: String, on: Boolean, index: Int, onClick: () -> Unit) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(80L + index * 50L)
        enter.animateTo(1f, spring(dampingRatio = 0.6f, stiffness = 200f))
    }
    val border by animateColorAsState(if (on) BoardColors.Indigo else Color.Transparent, tween(200), label = "border")
    val bg by animateColorAsState(
        if (on) BoardColors.Indigo.copy(alpha = 0.2f) else Color.White.copy(alpha = 0.07f),
        tween(200),
        label = "bg",
    )
    val lift by animateFloatAsState(
        if (on) 1.08f else 1f,
        spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessMedium),
        label = "lift",
    )
    val glow by animateDpAsState(if (on) 12.dp else 0.dp, tween(250), label = "chipGlow")
    val shape = RoundedCornerShape(12.dp)
    Column(
        Modifier
            .graphicsLayer {
                val e = enter.value
                alpha = e.coerceIn(0f, 1f)
                scaleX = (0.5f + 0.5f * e) * lift
                scaleY = (0.5f + 0.5f * e) * lift
            }
            .shadow(glow, shape, clip = false, ambientColor = BoardColors.Indigo, spotColor = BoardColors.Indigo)
            .clip(shape)
            .background(bg)
            .border(BorderStroke(2.dp, border), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(icon, fontSize = 20.sp, color = Color.White)
        Text(name, fontSize = 12.sp, color = Color.White)
    }
}
