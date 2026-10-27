package com.aismartmath.board.board

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import com.aismartmath.board.math.centerOf
import com.aismartmath.board.math.dist
import com.aismartmath.board.math.dseg
import com.aismartmath.board.math.figOk
import com.aismartmath.board.math.inPoly
import com.aismartmath.board.math.parseEq
import com.aismartmath.board.math.polyArea
import com.aismartmath.board.math.pts
import com.aismartmath.board.math.rectOf
import com.aismartmath.board.math.regularPolygon
import com.aismartmath.board.math.solveQuad
import com.aismartmath.board.math.solveTri
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.GraphDef
import com.aismartmath.board.model.Pt
import com.aismartmath.board.model.Topic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.sin

/** Web: LB - a different letter set for every new shape (six letters so pentagons and hexagons fit). */
val LB = listOf("ABCDEF", "PQRSTU", "XYZWVU", "LMNOPQ", "EFGHIJ", "STUVWX", "JKLMNO")
private val POLY = setOf("tri", "quad", "rect", "rtri", "pent", "hex")
private val NEED = mapOf("pt" to 1, "tri" to 3, "quad" to 4, "pent" to 5, "hex" to 6)

/** The point on a graph whose tangent is being shown. */
class Trace(val f: Fig, val x: Double)

class Preset(val name: String, val eq: String, val a: Double, val b: Double, val c: Double)

/** Web: PRE */
val PRESETS = listOf(
    Preset("Line", "a*x+b", 1.0, 0.0, 0.0),
    Preset("Parabola", "a*x^2+b*x+c", 1.0, 0.0, 0.0),
    Preset("Cubic", "a*x^3+b*x^2+c*x", 1.0, 0.0, 0.0),
    Preset("Sine", "a*sin(b*x+c)", 1.0, 1.0, 0.0),
    Preset("1/x", "a/x", 1.0, 0.0, 0.0),
    Preset("√x", "a*sqrt(x)+b", 1.0, 0.0, 0.0),
    Preset("Circle", "x^2+y^2=a", 9.0, 0.0, 0.0),
    Preset("Ellipse", "x^2/a+y^2/b=1", 9.0, 4.0, 0.0),
    Preset("Hyperbola", "x^2/a-y^2/b=1", 4.0, 4.0, 0.0),
    Preset("y²=ax", "y^2=a*x", 4.0, 0.0, 0.0),
)

data class MenuItem(val key: String, val label: String, val icon: String)
data class MenuState(val x: Float, val y: Float, val items: List<MenuItem>, val at: Pt)

/** Which phase brings each tool to life. */
fun toolPhase(id: String): Int = when (id) {
    "pt", "seg", "tri", "rtri", "quad", "pent", "hex", "rect", "circ", "sel", "pan" -> 3
    "fx", "dv" -> 4
    "sk", "pen", "hl", "er" -> 5
    "trig", "unit" -> 6
    else -> 7
}

fun hcount(f: Fig): Int = when (f.t) {
    "pt" -> 1
    "seg", "circ", "rect", "rtri" -> 2
    "tri", "quad", "pent", "hex" -> f.p.size
    else -> 0
}

class BoardState(val topic: Topic, private val scope: CoroutineScope) {
    val camera = Camera()
    var tool by mutableStateOf(topic.toolIds.first())
    val shapes = mutableStateListOf<Fig>()
    val pend = mutableStateListOf<Pt>()

    var cursor by mutableStateOf<Pt?>(null)
    var draft by mutableStateOf<Fig?>(null)
    var msg by mutableStateOf("")

    var clock by mutableLongStateOf(0L)
    var animTick by mutableIntStateOf(0)

    var sel by mutableStateOf<Fig?>(null)
    var editing by mutableStateOf<Fig?>(null)
    var mv by mutableStateOf<Fig?>(null)
    var rs by mutableStateOf<Fig?>(null)
    var detailsOn by mutableStateOf(false)
    var clip by mutableStateOf<Fig?>(null)
    var menu by mutableStateOf<MenuState?>(null)
    var toastMsg by mutableStateOf<String?>(null)

    // ---------- history ----------
    val undoStack = mutableStateListOf<Act>()
    val redoStack = mutableStateListOf<Act>()
    val canUndo: Boolean get() = pend.isNotEmpty() || undoStack.isNotEmpty()
    val canRedo: Boolean get() = redoStack.isNotEmpty()

    // ---------- lines that close up into a shape ----------
    var autoJoin by mutableStateOf(true)

    // ---------- parallel lines ----------
    var pxOn by mutableStateOf(false)
    var pxMode by mutableStateOf("all")
    var pxHl by mutableIntStateOf(0)
    var pxBis by mutableStateOf(false)
    private var pxSeen = false
    private var parKey = ""
    private var parVal: ParGeo? = null

    // ---------- graphs ----------
    var eq by mutableStateOf("a*x+b")
    var gpA by mutableDoubleStateOf(1.0)
    var gpB by mutableDoubleStateOf(0.0)
    var gpC by mutableDoubleStateOf(0.0)
    var animA by mutableStateOf(false)
    var trace by mutableStateOf<Trace?>(null)

    // ---------- Solve: pen, highlighter, eraser, sketch ----------
    val skP = mutableStateListOf<Offset>()
    var penAt by mutableStateOf<Offset?>(null)
    var penCol by mutableStateOf(Color(0xFFFBBF24))
    var penW by mutableStateOf(4f)
    var eraserDp by mutableStateOf(14f)
    var erasePart by mutableStateOf(true)
    var ghost by mutableStateOf<List<Pt>>(emptyList())
    var ghostAt by mutableLongStateOf(0L)

    var holdFired = false
    private var lbN = 0
    private var hintShown = false
    private var toastJob: Job? = null
    private var holdJob: Job? = null
    private var tapJob: Job? = null
    private var tapCount = 0
    private var tapFig: Fig? = null
    private var tapAt = Offset.Zero
    private var tapWorld = Pt(0.0, 0.0)
    private var mvO: List<Pt> = emptyList()
    private var mvS = Pt(0.0, 0.0)
    private var rsO: List<Pt> = emptyList()
    private var rsC = Pt(0.0, 0.0)
    private var rsD = 1.0
    private var eraseSnap: List<Fig>? = null
    private val eraseOrig = HashMap<Fig, Int>()

    // ---------- tool kinds ----------
    val isTapTool: Boolean get() = tool in NEED
    val isDragTool: Boolean get() = tool == "seg" || tool == "rect" || tool == "rtri" || tool == "circ"
    val drawsWithFinger: Boolean get() = isTapTool || isDragTool
    val need: Int get() = NEED[tool] ?: 0
    val isGraphTool: Boolean get() = tool == "fx" || tool == "dv"
    val isFreeTool: Boolean get() = tool == "sk" || tool == "pen" || tool == "hl" || tool == "er"
    val isPenTool: Boolean get() = tool == "pen" || tool == "hl"
    val isEraserTool: Boolean get() = tool == "er"

    /** Web: opening Graph / Derivative with no graph yet plots the Line preset. */
    fun start() {
        if (isGraphTool && lastFx() == null) preset(0)
    }

    fun selectTool(id: String) {
        tool = id
        trace = null
        animA = false
        cancelFree()
        pend.clear()
        cursor = null
        draft = null
        msg = ""
        mv = null
        rs = null
        closeEdit()
        hideMenu()
        start()
    }

    fun initialOy(): Double =
        if (tool in setOf("fx", "dv", "trig", "unit")) -70.0 * camera.density else 0.0

    // ---------- coordinates ----------
    fun rawWorld(p: Offset): Pt = Pt(camera.wx(p.x), camera.wy(p.y))
    fun snapPt(w: Pt): Pt {
        val q = camera.snapStep
        return Pt(floor(w.x / q + 0.5) * q, floor(w.y / q + 0.5) * q)
    }

    /** Snap to an existing corner or point when one is close (so lines meet exactly), otherwise to the grid. */
    fun snap(p: Offset): Pt {
        val raw = rawWorld(p)
        val tol = 14.0 * camera.density / camera.u
        var best: Pt? = null
        var bd = tol
        for (s in shapes) {
            if (s.dieAt != 0L) continue
            val cand: List<Pt> = when (s.t) {
                "pt", "seg", "tri", "quad", "pent", "hex" -> s.p
                "rect", "rtri" -> pts(s)
                else -> emptyList()
            }
            for (c in cand) {
                val d = dist(c, raw)
                if (d < bd) {
                    bd = d
                    best = c
                }
            }
        }
        return best ?: snapPt(raw)
    }

    // ---------- animation clock, toasts ----------
    fun bump(t: Long = System.nanoTime()) {
        clock = t
        animTick++
        refreshPar()
    }

    fun showToast(m: String) {
        toastMsg = m
        toastJob?.cancel()
        toastJob = scope.launch {
            delay(2400)
            toastMsg = null
        }
    }

    // ---------- history ----------
    private fun record(a: Act) {
        undoStack.add(a)
        if (undoStack.size > 200) undoStack.removeAt(0)
        redoStack.clear()
    }

    /** Change a figure and remember it for undo. Quick successive changes of one kind become one step. */
    fun edit(f: Fig, label: String, change: () -> Unit) {
        val before = f.snapshot()
        change()
        val after = f.snapshot()
        if (before == after) return
        val now = System.nanoTime()
        val last = undoStack.lastOrNull()
        if (last is EditAct && last.f === f && last.label == label && now - last.time < 900_000_000L) {
            last.after = after
            last.time = now
            redoStack.clear()
        } else {
            record(EditAct(f, before, after, now, label))
        }
        refreshPar()
    }

    fun undo() {
        if (pend.isNotEmpty()) {
            pend.removeAt(pend.lastIndex)
            showToast("↶ Removed the last point")
            return
        }
        if (undoStack.isEmpty()) {
            showToast("Nothing to undo")
            return
        }
        val a = undoStack.removeAt(undoStack.lastIndex)
        applyAct(a, false)
        redoStack.add(a)
        showToast("↶ Undid: " + a.label)
    }

    fun redo() {
        if (redoStack.isEmpty()) {
            showToast("Nothing to redo")
            return
        }
        val a = redoStack.removeAt(redoStack.lastIndex)
        applyAct(a, true)
        undoStack.add(a)
        showToast("↷ Redid: " + a.label)
    }

    private fun applyAct(a: Act, forward: Boolean) {
        when (a) {
            is AddAct -> {
                if (forward) a.fs.forEach { restoreFig(it, Int.MAX_VALUE) } else a.fs.forEach { softRemove(it) }
                bump()
            }
            is RemoveAct -> {
                if (forward) a.fs.forEach { softRemove(it) } else a.fs.forEachIndexed { k, f -> restoreFig(f, a.idx[k]) }
                bump()
            }
            is EditAct -> {
                a.f.restore(if (forward) a.after else a.before)
                bump()
            }
            is ReplaceAct -> {
                val outList = if (forward) a.removed else a.added
                val inList = if (forward) a.added else a.removed
                val inIdx = if (forward) a.addedIdx else a.removedIdx
                outList.forEach { dropFig(it, a.instant) }
                inList.forEachIndexed { k, f -> restoreFig(f, inIdx[k]) }
                bump()
            }
        }
    }

    private fun restoreFig(f: Fig, idx: Int) {
        f.dieAt = 0L
        if (!shapes.contains(f)) shapes.add(idx.coerceIn(0, shapes.size), f)
    }

    private fun clearRefs(f: Fig) {
        if (editing === f) closeEdit()
        if (sel === f) sel = null
        if (mv === f) mv = null
        if (rs === f) rs = null
        if (trace?.f === f) trace = null
    }

    private fun dropFig(f: Fig, instant: Boolean) {
        if (instant) {
            shapes.remove(f)
            clearRefs(f)
        } else {
            softRemove(f)
        }
    }

    /** Fade a figure out and take it off the board (no history entry). */
    fun softRemove(f: Fig) {
        if (f.dieAt != 0L) return
        val t = System.nanoTime()
        f.dieAt = t
        clearRefs(f)
        bump(t)
        scope.launch {
            delay(260)
            if (f.dieAt != 0L) shapes.remove(f)
        }
    }

    /** Delete a figure the user chose, with an undo step. */
    fun delete(f: Fig) {
        if (f.dieAt != 0L) return
        record(RemoveAct(listOf(f), listOf(shapes.indexOf(f)), "Deleted " + figName(f)))
        softRemove(f)
    }

    fun clear() {
        pend.clear()
        closeEdit()
        sel = null
        mv = null
        rs = null
        hideMenu()
        val alive = shapes.filter { it.dieAt == 0L }
        if (alive.isEmpty()) return
        record(RemoveAct(alive, alive.map { shapes.indexOf(it) }, "Cleared the board"))
        alive.forEach { softRemove(it) }
    }

    // ---------- adding ----------
    fun addFig(t: String, p: List<Pt>) {
        if (!figOk(t, p)) {
            msg = "That did not make a shape — try again"
            return
        }
        msg = ""
        val born = System.nanoTime()
        val nm = if (t in POLY) LB[lbN++ % LB.size] else null
        val f = Fig(t, p, born, nm)
        shapes.add(f)
        bump(born)
        if (t == "seg" && autoJoin && tryJoin(f)) return
        record(AddAct(listOf(f), "Added " + figName(f)))
        if (!hintShown && t != "pt") {
            hintShown = true
            showToast("Tip: use 👆 Select to edit, copy or move figures")
        }
    }

    /** Called when a finger lifts after pointing with a tap tool (Point / Triangle / Quadrilateral / Pentagon / Hexagon). */
    fun placeTapPoint(p: Pt) {
        if (tool == "pt") {
            addFig("pt", listOf(p))
        } else {
            pend.add(p)
            if (pend.size == need) {
                addFig(tool, pend.toList())
                pend.clear()
            }
        }
    }

    // ---------- lines that close up into a shape ----------
    private fun sameP(a: Pt, b: Pt) = abs(a.x - b.x) < 1e-9 && abs(a.y - b.y) < 1e-9

    /** Look for a closed loop of 3 to 6 lines through the line just drawn. */
    private fun findLoop(newSeg: Fig): Pair<List<Pt>, List<Fig>>? {
        val segs = shapes.filter { it.t == "seg" && it.dieAt == 0L }
        for (total in 3..6) {
            val verts = mutableListOf(newSeg.p[0], newSeg.p[1])
            val used = mutableListOf(newSeg)
            fun dfs(): Boolean {
                val cur = verts.last()
                if (used.size == total - 1) {
                    for (s in segs) {
                        if (s in used) continue
                        val closes = (sameP(s.p[0], cur) && sameP(s.p[1], verts[0])) ||
                            (sameP(s.p[1], cur) && sameP(s.p[0], verts[0]))
                        if (closes) {
                            used.add(s)
                            return true
                        }
                    }
                    return false
                }
                for (s in segs) {
                    if (s in used) continue
                    val nxt = if (sameP(s.p[0], cur)) s.p[1] else if (sameP(s.p[1], cur)) s.p[0] else continue
                    if (verts.any { sameP(it, nxt) }) continue
                    verts.add(nxt)
                    used.add(s)
                    if (dfs()) return true
                    verts.removeAt(verts.lastIndex)
                    used.removeAt(used.lastIndex)
                }
                return false
            }
            if (dfs()) return verts.toList() to used.toList()
        }
        return null
    }

    private fun polygonKind(v: List<Pt>): Pair<String, List<Pt>> = when (v.size) {
        3 -> "tri" to v
        4 -> {
            val r = rectOf(v)
            if (r != null) "rect" to r else "quad" to v
        }
        5 -> "pent" to v
        else -> "hex" to v
    }

    /** Three lines that meet become a triangle, four a quadrilateral, and so on. */
    private fun tryJoin(f: Fig): Boolean {
        val loop = findLoop(f) ?: return false
        val verts = loop.first
        if (polyArea(verts) <= 1e-9) return false
        val kind = polygonKind(verts)
        val pointsHere = shapes.filter { it.t == "pt" && it.dieAt == 0L && verts.any { v -> sameP(v, it.p[0]) } }
        val removed = (loop.second + pointsHere).sortedBy { shapes.indexOf(it) }
        val removedIdx = removed.map { shapes.indexOf(it) }
        val born = System.nanoTime()
        val poly = Fig(kind.first, kind.second, born, LB[lbN++ % LB.size])
        removed.forEach { softRemove(it) }
        shapes.add(poly)
        val addedIdx = listOf(shapes.size - 1)
        bump(born)
        record(ReplaceAct(removed, removedIdx, listOf(poly), addedIdx, false, "Joined lines into a " + figName(poly)))
        showToast("Joined into a ${figName(poly)} — undo (↶) gives the lines back")
        return true
    }

    // ---------- parallel lines ----------
    fun parGeo(): ParGeo? {
        val segs = shapes.filter { it.t == "seg" && it.dieAt == 0L }
        val key = segs.joinToString("|") {
            "${System.identityHashCode(it)}:${it.p[0].x},${it.p[0].y},${it.p[1].x},${it.p[1].y}"
        }
        if (key != parKey) {
            parKey = key
            parVal = findPar(segs)
        }
        return parVal
    }

    /** Switch the angle tool on by itself when two parallel lines and a transversal appear. */
    fun refreshPar() {
        val g = parGeo()
        if (g != null && !pxSeen) {
            pxSeen = true
            pxOn = true
            showToast("∥ Parallel lines + transversal found — angles ∠1 to ∠8 are shown. Tap one to see what it equals")
        }
        if (g == null) {
            pxSeen = false
            pxOn = false
            pxHl = 0
        }
    }

    /** Web: pxPick - tap near a crossing to pick the angle there. */
    fun pxPick(px: Offset): Boolean {
        val g = parGeo() ?: return false
        val raw = rawWorld(px)
        for (i in 0..1) {
            val x = g.xs[i]
            val vx = raw.x - x.x
            val vy = raw.y - x.y
            val ds = hypot(vx, vy) * camera.u / camera.density
            if (ds < 55 && ds > 3) {
                val det = g.t.x * g.d.y - g.t.y * g.d.x
                val aa = (vx * g.d.y - vy * g.d.x) / det
                val bb = (g.t.x * vy - g.t.y * vx) / det
                val k = (if (i == 1) 5 else 1) + (if (aa > 0) 0 else 2) + (if (bb > 0) 1 else 0)
                pxHl = if (pxHl == k) 0 else k
                return true
            }
        }
        return false
    }

    // ---------- graphs ----------
    fun lastFx(): Fig? = shapes.lastOrNull { it.t == "fx" && it.dieAt == 0L && it.gd?.dv == 0 }

    fun plot() {
        val src = eq.trim()
        val r = parseEq(src)
        if (r == null) {
            msg = "Could not read that. Try: 2x^2 - 3x + 1 , x^2+y^2=25 , a*sin(b*x)"
            return
        }
        msg = ""
        val born = System.nanoTime()
        val f = Fig("fx", emptyList(), born)
        f.gd = GraphDef(src, r.fn, r.imp)
        f.ga = gpA
        f.gb = gpB
        f.gc = gpC
        f.col = GraphColors[shapes.count { it.t == "fx" } % GraphColors.size]
        shapes.add(f)
        bump(born)
        record(AddAct(listOf(f), "Plotted a graph"))
    }

    fun preset(i: Int) {
        val p = PRESETS[i]
        eq = p.eq
        gpA = p.a
        gpB = p.b
        gpC = p.c
        plot()
    }

    /** Web: addDv - add the f' or f'' curve of the last graph. */
    fun addDv(n: Int) {
        val b = lastFx() ?: return
        val bg = b.gd ?: return
        if (bg.imp) {
            showToast("Derivative needs a graph like y = f(x)")
            return
        }
        val born = System.nanoTime()
        val f = Fig("fx", emptyList(), born)
        f.gd = GraphDef(bg.src, null, false, n, b)
        f.col = if (n == 1) Color(0xFFF472B6) else Color(0xFF34D399)
        shapes.add(f)
        bump(born)
        record(AddAct(listOf(f), if (n == 1) "Added f′ curve" else "Added f″ curve"))
    }

    /** The a / b / c sliders of the graph panel. */
    fun setParam(k: String, v: Double) {
        when (k) {
            "a" -> gpA = v
            "b" -> gpB = v
            else -> gpC = v
        }
        if (isGraphTool) lastFx()?.let {
            when (k) {
                "a" -> it.ga = v
                "b" -> it.gb = v
                else -> it.gc = v
            }
        }
    }

    /** Graph / Derivative tool: tap to put the tangent on the last graph. */
    fun onGraphTap(px: Offset) {
        val g = lastFx() ?: return
        if (g.gd?.imp == true) return
        trace = Trace(g, rawWorld(px).x)
    }

    /** Web: act() - the figure whose description the info bar shows. */
    fun activeFig(): Fig? =
        editing ?: mv ?: if (isGraphTool) lastFx() else shapes.lastOrNull { it.dieAt == 0L && it.t != "ink" }

    // ---------- free-hand: pen, highlighter, eraser, sketch ----------
    fun beginFree(pos: Offset) {
        skP.clear()
        skP.add(pos)
        penAt = pos
        if (tool == "er") {
            eraseSnap = shapes.filter { it.t == "ink" && it.dieAt == 0L }
            eraseOrig.clear()
            eraseSnap?.forEach { eraseOrig[it] = shapes.indexOf(it) }
            erase(rawWorld(pos))
        }
    }

    fun moveFree(pos: Offset, minStep: Float) {
        val last = skP.lastOrNull()
        if (last == null || (pos - last).getDistance() >= minStep) skP.add(pos)
        penAt = pos
        if (tool == "er") erase(rawWorld(pos))
    }

    fun cancelFree() {
        skP.clear()
        penAt = null
        endErase()
    }

    /** The eraser lifted: record everything it rubbed out as one undo step. */
    private fun endErase() {
        val before = eraseSnap ?: return
        eraseSnap = null
        val now = shapes.filter { it.t == "ink" && it.dieAt == 0L }
        val removed = before.filter { it !in now }
        val added = now.filter { it !in before }
        if (removed.isEmpty() && added.isEmpty()) return
        val rem = removed.sortedBy { eraseOrig[it] ?: 0 }
        val remIdx = rem.map { eraseOrig[it] ?: 0 }
        val add = added.sortedBy { shapes.indexOf(it) }
        val addIdx = add.map { shapes.indexOf(it) }
        record(ReplaceAct(rem, remIdx, add, addIdx, true, "Erased part of the drawing"))
        eraseOrig.clear()
    }

    /** Rub out the part of every stroke under the eraser (or the whole stroke, in "Whole stroke" mode). */
    fun erase(c: Pt) {
        val r = eraserDp.toDouble() * camera.density / camera.u
        for (s in shapes.toList()) {
            if (s.t != "ink" || s.dieAt != 0L) continue
            val q = s.p
            val touched = q.indices.any { k -> dseg(c, q[k], q.getOrElse(k + 1) { q[k] }) <= r }
            if (!touched) continue
            val idx = shapes.indexOf(s)
            shapes.removeAt(idx)
            if (!erasePart) continue
            val pieces = cutStroke(q, c, r)
            pieces.forEachIndexed { k, pp ->
                val n = s.copyOf(System.nanoTime())
                n.p = pp
                shapes.add(idx + k, n)
            }
        }
    }

    private fun cutStroke(p: List<Pt>, c: Pt, r: Double): List<List<Pt>> {
        val dense = ArrayList<Pt>()
        for (i in p.indices) {
            dense.add(p[i])
            if (i + 1 < p.size) {
                val a = p[i]
                val b = p[i + 1]
                val n = minOf(200, ceil(dist(a, b) / (r * 0.4)).toInt())
                for (k in 1 until n) {
                    val t = k.toDouble() / n
                    dense.add(Pt(a.x + (b.x - a.x) * t, a.y + (b.y - a.y) * t))
                }
            }
        }
        val out = ArrayList<List<Pt>>()
        var cur = ArrayList<Pt>()
        for (q in dense) {
            if (dist(q, c) > r) {
                cur.add(q)
            } else {
                if (cur.size >= 2) out.add(cur)
                cur = ArrayList()
            }
        }
        if (cur.size >= 2) out.add(cur)
        return out
    }

    /** The finger lifted after a free-hand stroke: save ink, or recognise a sketch. */
    fun finishFree() {
        val sp = skP.toList()
        cancelFree()
        if (sp.isEmpty()) return
        if (tool == "pen" || tool == "hl") {
            val born = System.nanoTime()
            val raw = if (sp.size < 2) listOf(sp[0], sp[0]) else sp
            val f = Fig("ink", raw.map { rawWorld(it) }, born)
            f.col = penCol
            f.inkW = penW.toDouble()
            f.hl = tool == "hl"
            shapes.add(f)
            bump(born)
            record(AddAct(listOf(f), if (tool == "hl") "Highlighter stroke" else "Pen stroke"))
            return
        }
        if (tool == "er") return
        val res = recognize(sp, this)
        if (res == null) {
            showToast("Couldn't recognise that — try a clearer shape")
            return
        }
        msg = ""
        ghost = sp.map { rawWorld(it) }
        ghostAt = System.nanoTime()
        addFig(res.first, res.second)
        if (msg.isEmpty()) {
            showToast("Looks like: ${FigNames[res.first] ?: res.first} — adjust it below")
            val ns = shapes.lastOrNull { it.dieAt == 0L }
            selectTool("sel")
            if (ns != null) openEdit(ns)
        }
    }

    // ---------- hit testing ----------
    fun hit(w: Pt): Fig? {
        val tol = 16.0 * camera.density / camera.u
        for (i in shapes.indices.reversed()) {
            val s = shapes[i]
            if (s.dieAt != 0L || s.t == "ink") continue
            val q = pts(s)
            val h = when (s.t) {
                "pt" -> dist(w, q[0]) < tol * 1.4
                "seg" -> dseg(w, q[0], q[1]) < tol
                "circ" -> dist(q[0], w) <= dist(q[0], q[1]) + tol
                "fx" -> hitGraph(s, w)
                else -> inPoly(w, q) || q.indices.any { k -> dseg(w, q[k], q[(k + 1) % q.size]) < tol }
            }
            if (h) return s
        }
        return null
    }

    private fun hitGraph(s: Fig, p: Pt): Boolean {
        val g = s.gd ?: return false
        val tol = 18.0 * camera.density
        return if (g.imp) {
            val e = 1e-4
            val v = s.vAt(p.x, p.y)
            val gx = (s.vAt(p.x + e, p.y) - s.vAt(p.x - e, p.y)) / 2 / e
            val gy = (s.vAt(p.x, p.y + e) - s.vAt(p.x, p.y - e)) / 2 / e
            val gl = hypot(gx, gy).let { if (it == 0.0) 1.0 else it }
            v.isFinite() && abs(v) / gl * camera.u < tol
        } else {
            val y = s.yAt(p.x)
            y.isFinite() && abs(y - p.y) * camera.u < tol
        }
    }

    fun findHandle(px: Offset): Int {
        val e = editing ?: return -1
        if (!shapes.contains(e)) return -1
        var best = 28f * camera.density.toFloat()
        var idx = -1
        for (i in 0 until hcount(e)) {
            val q = e.p[i]
            val d = hypot(camera.sx(q.x) - px.x, camera.sy(q.y) - px.y)
            if (d < best) {
                best = d
                idx = i
            }
        }
        return idx
    }

    // ---------- editing ----------
    fun openEdit(f: Fig) {
        editing = f
        sel = f
        f.base = null
        f.sz = 1.0
        f.rot = 0.0
    }

    fun closeEdit(clearSel: Boolean = true) {
        editing = null
        detailsOn = false
        if (clearSel) {
            sel = null
            trace = null
        }
    }

    fun dragHandle(i: Int, px: Offset) {
        val f = editing ?: return
        val np = f.p.toMutableList()
        np[i] = snapPt(rawWorld(px))
        if (!figOk(f.t, np)) return
        edit(f, "Reshaped " + figName(f)) {
            f.p = np
            f.base = null
            f.sz = 1.0
            f.rot = 0.0
        }
    }

    fun beginMove(f: Fig, px: Offset) {
        mvO = f.p
        mvS = rawWorld(px)
    }

    fun dragMove(f: Fig, px: Offset) {
        val q = camera.snapStep
        val w = rawWorld(px)
        val dx = floor((w.x - mvS.x) / q + 0.5) * q
        val dy = floor((w.y - mvS.y) / q + 0.5) * q
        edit(f, "Moved " + figName(f)) {
            f.p = mvO.map { Pt(it.x + dx, it.y + dy) }
            f.base = null
        }
    }

    fun beginResize(f: Fig, px: Offset) {
        rsO = f.p
        rsC = centerOf(f.t, f.p)
        rsD = maxOf(dist(rawWorld(px), rsC), 1e-9)
    }

    fun dragResize(f: Fig, px: Offset) {
        val k = dist(rawWorld(px), rsC) / rsD
        if (k < 0.05 || k > 50) return
        val np = rsO.map { Pt(rsC.x + (it.x - rsC.x) * k, rsC.y + (it.y - rsC.y) * k) }
        if (figOk(f.t, np)) {
            edit(f, "Resized " + figName(f)) {
                f.p = np
                f.base = null
            }
        }
    }

    /** Web: editField - a number box or slider in the edit panel changed. */
    fun editField(f: Fig, k: String, v: Double) {
        edit(f, "Changed " + figName(f)) { applyField(f, k, v) }
    }

    /** Make a polygon regular (equal sides and angles). */
    fun makeRegular(f: Fig) {
        edit(f, "Made " + figName(f) + " regular") {
            f.p = regularPolygon(f.p)
            f.base = null
            f.sz = 1.0
            f.rot = 0.0
        }
    }

    private fun applyField(f: Fig, k: String, v: Double) {
        if (!v.isFinite()) return
        if (f.t == "fx") {
            when (k) {
                "a" -> { f.ga = v; gpA = v }
                "b" -> { f.gb = v; gpB = v }
                "c" -> { f.gc = v; gpC = v }
            }
            return
        }
        if (k == "size" || k == "rot") {
            if (k == "rot" && (f.t == "rect" || f.t == "rtri")) {
                f.p = pts(f)
                f.t = if (f.t == "rect") "quad" else "tri"
                f.base = null
                showToast("Now a free shape, so it can rotate")
            }
            val o = f.base ?: f.p.also { f.base = it }
            if (k == "size") f.sz = v / 100 else f.rot = v
            val c = centerOf(f.t, o)
            val r = f.sz
            val th = f.rot * PI / 180
            val co = cos(th)
            val si = sin(th)
            f.p = o.map { q ->
                val dx = (q.x - c.x) * r
                val dy = (q.y - c.y) * r
                Pt(c.x + dx * co - dy * si, c.y + dx * si + dy * co)
            }
            return
        }
        if (v <= 0 && k != "x" && k != "y" && k != "ang") return
        val np = f.p.toMutableList()
        when (f.t) {
            "pt" -> np[0] = if (k == "x") Pt(v, np[0].y) else Pt(np[0].x, v)
            "seg" -> {
                val a = np[0]
                val b = np[1]
                var l = dist(a, b)
                var t = atan2(b.y - a.y, b.x - a.x)
                if (k == "len") l = v else t = v * PI / 180
                np[1] = Pt(a.x + l * cos(t), a.y + l * sin(t))
            }
            "circ" -> {
                val a = np[0]
                val b = np[1]
                val t = atan2(b.y - a.y, b.x - a.x)
                np[1] = Pt(a.x + v * cos(t), a.y + v * sin(t))
            }
            "rect", "rtri" -> {
                val a = np[0]
                val b = np[1]
                val gx = if (b.x - a.x < 0) -1.0 else 1.0
                val gy = if (b.y - a.y < 0) -1.0 else 1.0
                np[1] = if (k == "w") Pt(a.x + gx * v, b.y) else Pt(b.x, a.y + gy * v)
            }
            "tri" -> {
                val r = solveTri(np, k, v)
                if (r == null) {
                    showToast("Those sides cannot make a triangle")
                    return
                }
                r.forEachIndexed { i, q -> np[i] = q }
            }
            "quad" -> {
                val r = solveQuad(np, k, v)
                if (r == null) {
                    showToast("Those lengths cannot make a quadrilateral")
                    return
                }
                r.forEachIndexed { i, q -> np[i] = q }
            }
            else -> return
        }
        if (figOk(f.t, np)) {
            f.p = np
            f.base = null
            f.sz = 1.0
            f.rot = 0.0
        }
    }

    // ---------- select tool: multi-tap ----------
    fun onSelTap(px: Offset) {
        val w = rawWorld(px)
        val h = hit(w)
        if (h == null) {
            mv = null
            rs = null
            closeEdit()
            sel = null
            tapCount = 0
            tapFig = null
            return
        }
        if (mv != null || rs != null) return
        if (h === tapFig && tapCount > 0) tapCount++ else {
            tapCount = 1
            tapFig = h
        }
        tapAt = px
        tapWorld = w
        tapJob?.cancel()
        tapJob = scope.launch {
            delay(350)
            resolveTap()
        }
    }

    private fun resolveTap() {
        val h = tapFig
        val n = tapCount
        tapCount = 0
        if (n == 0 || h == null || !shapes.contains(h) || h.dieAt != 0L) return
        if (n == 1) {
            openEdit(h)
            if (h.t == "fx" && h.gd?.imp == false) trace = Trace(h, tapWorld.x)
            return
        }
        closeEdit(clearSel = false)
        sel = h
        when (n) {
            2 -> menu = MenuState(tapAt.x, tapAt.y, cmItems(), tapWorld)
            3 -> {
                mv = h
                showToast("Move mode ✥ — drag the figure, tap empty space when done")
            }
            4 -> if (h.t == "pt") showToast("Resize works on figures") else {
                rs = h
                showToast("Resize ⤢ — drag outward or inward, tap empty space when done")
            }
            else -> showToast("Bisectors: switch on ∠ Bisectors in the parallel-lines panel")
        }
    }

    // ---------- hold menu / double-tap menu ----------
    fun armHold(px: Offset) {
        holdFired = false
        holdJob?.cancel()
        holdJob = scope.launch {
            delay(800)
            holdFired = true
            cursor = null
            draft = null
            holdAct(px)
        }
    }

    fun cancelHold() {
        holdJob?.cancel()
    }

    private fun holdAct(px: Offset) {
        val w = rawWorld(px)
        val h = hit(w)
        sel = h
        val items = buildList {
            if (h != null) {
                add(MenuItem("copy", "Copy", "📋"))
                add(MenuItem("cut", "Cut", "✂️"))
                add(MenuItem("del", "Delete", "🗑️"))
            }
            if (clip != null) add(MenuItem("paste", "Paste", "📥"))
        }
        if (items.isNotEmpty()) menu = MenuState(px.x, px.y, items, w)
        else showToast("Nothing to paste yet — double-tap a figure to copy it")
    }

    private fun cmItems(): List<MenuItem> = buildList {
        add(MenuItem("copy", "Copy", "📋"))
        add(MenuItem("cut", "Cut", "✂️"))
        add(MenuItem("del", "Delete", "🗑️"))
        add(MenuItem("det", "Details", "⋯"))
        if (clip != null) add(MenuItem("paste", "Paste", "📥"))
    }

    fun hideMenu() {
        menu = null
        sel = null
    }

    fun menuAction(key: String) {
        val m = menu
        val s = sel
        if (key == "det") {
            menu = null
            sel = null
            if (s != null && shapes.contains(s)) {
                detailsOn = true
                openEdit(s)
            }
            return
        }
        when (key) {
            "copy", "cut" -> if (s != null) {
                clip = s.copyOf(System.nanoTime())
                if (key == "cut") delete(s)
                showToast(if (key == "cut") "Cut ✂️ — hold on the board to paste" else "Copied 📋 — hold on the board to paste")
            }
            "del" -> if (s != null) {
                delete(s)
                showToast("Deleted 🗑️ — undo (↶) brings it back")
            }
            "paste" -> if (m != null) paste(m.at)
        }
        hideMenu()
    }

    fun copySel(f: Fig, cut: Boolean) {
        clip = f.copyOf(System.nanoTime())
        if (cut) delete(f)
        showToast(if (cut) "Cut ✂️ — hold on the board to paste" else "Copied 📋 — hold on the board to paste")
    }

    fun paste(at: Pt) {
        val c = clip ?: return
        val q = camera.snapStep
        val ce = centerOf(c.t, c.p)
        val dx = floor((at.x - ce.x) / q + 0.5) * q
        val dy = floor((at.y - ce.y) / q + 0.5) * q
        val born = System.nanoTime()
        val n = c.copyOf(born)
        n.p = c.p.map { Pt(it.x + dx, it.y + dy) }
        if (c.nm != null) n.nm = LB[lbN++ % LB.size]
        shapes.add(n)
        bump(born)
        record(AddAct(listOf(n), "Pasted " + figName(n)))
        showToast("Pasted ✅")
    }
}
