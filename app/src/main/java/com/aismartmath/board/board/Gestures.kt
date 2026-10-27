package com.aismartmath.board.board

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import com.aismartmath.board.model.Fig

private enum class Mode { Draw, Free, Pan, Handle, Move, Resize, Swallow }

/**
 * Touch handling, ported from the web pointer handlers:
 *  - two fingers always pinch-zoom and pan;
 *  - Point / Triangle / Quadrilateral: a ghost follows the finger, the point is placed when it lifts;
 *  - Line / Rectangle / Right triangle / Circle: drag from corner to corner;
 *  - Pen / Highlighter / Eraser / Sketch: free-hand drawing;
 *  - Select: tap (1-5 taps), drag a handle, or move / resize after 3 / 4 taps;
 *  - hold for 0.8 s: paste / copy / cut / delete menu;
 *  - everything else drags the board.
 */
fun Modifier.boardGestures(state: BoardState): Modifier = pointerInput(state) {
    awaitEachGesture {
        val cam = state.camera
        val tool = state.tool
        val down = awaitFirstDown(requireUnconsumed = false)
        val slop = 12.dp.toPx()
        val minStep = 2.dp.toPx()
        val downPos = down.position
        state.msg = ""

        var mode = Mode.Pan
        var handle = -1
        var target: Fig? = null
        val start = state.snap(downPos)

        if (state.menu != null) {
            state.hideMenu()
            mode = Mode.Swallow
        } else if (state.drawsWithFinger) {
            mode = Mode.Draw
            if (state.isTapTool) state.cursor = start
            else state.draft = Fig(tool, listOf(start, start), 0L)
        } else if (state.isFreeTool) {
            mode = Mode.Free
            state.beginFree(downPos)
        } else if (tool == "sel") {
            val mvF = state.mv
            val rsF = state.rs
            if (mvF != null && state.shapes.contains(mvF)) {
                mode = Mode.Move
                target = mvF
                state.beginMove(mvF, downPos)
            } else if (rsF != null && state.shapes.contains(rsF)) {
                mode = Mode.Resize
                target = rsF
                state.beginResize(rsF, downPos)
            } else {
                handle = state.findHandle(downPos)
                if (handle >= 0) mode = Mode.Handle
            }
        }

        if (mode == Mode.Draw || mode == Mode.Pan) state.armHold(downPos)

        var pinching = false
        var moved = false
        var prevSpan = 0f
        var prevCenter = Offset.Zero

        while (true) {
            val ev = awaitPointerEvent()
            val pressed = ev.changes.filter { it.pressed }
            if (pressed.isEmpty()) break

            if (pressed.size >= 2) {
                state.cancelHold()
                val a = pressed[0].position
                val b = pressed[1].position
                val c = (a + b) / 2f
                val span = (a - b).getDistance()
                if (!pinching) {
                    pinching = true
                    state.cursor = null
                    state.draft = null
                    state.cancelFree()
                } else if (prevSpan > 0f && span > 0f) {
                    cam.zoomAt(c.x.toDouble(), c.y.toDouble(), (span / prevSpan).toDouble())
                    cam.pan(c.x - prevCenter.x, c.y - prevCenter.y)
                }
                prevSpan = span
                prevCenter = c
                ev.changes.forEach { it.consume() }
            } else if (!pinching) {
                val ch = pressed[0]
                if (!moved && (ch.position - downPos).getDistance() > slop) {
                    moved = true
                    state.cancelHold()
                }
                if (!state.holdFired) {
                    when (mode) {
                        Mode.Draw -> {
                            val p = state.snap(ch.position)
                            if (state.isTapTool) state.cursor = p else state.draft = Fig(tool, listOf(start, p), 0L)
                        }
                        Mode.Free -> state.moveFree(ch.position, minStep)
                        Mode.Pan -> {
                            val d = ch.positionChange()
                            cam.pan(d.x, d.y)
                        }
                        Mode.Handle -> if (moved) state.dragHandle(handle, ch.position)
                        Mode.Move -> if (moved) target?.let { state.dragMove(it, ch.position) }
                        Mode.Resize -> if (moved) target?.let { state.dragResize(it, ch.position) }
                        Mode.Swallow -> {}
                    }
                }
                ch.consume()
            }
        }

        state.cancelHold()
        if (!pinching && !state.holdFired) {
            when (mode) {
                Mode.Draw -> {
                    if (state.isTapTool) {
                        state.cursor?.let { state.placeTapPoint(it) }
                    } else {
                        val d = state.draft
                        if (d != null) state.addFig(tool, d.p)
                    }
                }
                Mode.Free -> state.finishFree()
                Mode.Pan, Mode.Move, Mode.Resize -> if (!moved) {
                    if (state.pxOn && state.pxPick(downPos)) {
                        // an angle of the parallel lines was picked
                    } else if (tool == "sel") state.onSelTap(downPos)
                    else if (state.isGraphTool) state.onGraphTap(downPos)
                }
                else -> {}
            }
        }
        state.cursor = null
        state.draft = null
        state.cancelFree()
    }
}
