package com.aismartmath.board.model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color

data class Pt(val x: Double, val y: Double)

/** x, y, a, b, c -> value. Explicit graphs ignore y; implicit curves are f(x,y) = 0. */
typealias Fn = (Double, Double, Double, Double, Double) -> Double

/** What a graph figure plots. dv = 0 normal, 1 = f', 2 = f''. */
class GraphDef(val src: String, val fn: Fn?, val imp: Boolean, val dv: Int = 0, val base: Fig? = null)

/**
 * One figure on the board (web: a shapes[] entry). t = pt, seg, tri, rtri, quad, rect, circ, fx.
 * rect / rtri / seg / circ store 2 points, tri 3, quad 4 (same as the web version).
 * Graphs (t = fx) keep their data in [gd] and their sliders in ga / gb / gc.
 */
class Fig(t0: String, p0: List<Pt>, val born: Long, nm0: String? = null) {
    var t by mutableStateOf(t0)
    var p by mutableStateOf(p0)
    var nm by mutableStateOf(nm0)
    var col by mutableStateOf<Color?>(null)
    var sz by mutableDoubleStateOf(1.0)
    var rot by mutableDoubleStateOf(0.0)
    var base: List<Pt>? = null
    var ov by mutableStateOf<Set<String>>(emptySet())
    var dieAt by mutableLongStateOf(0L)

    var inkW = 4.0
    var hl = false

    var gd: GraphDef? = null
    var ga by mutableDoubleStateOf(1.0)
    var gb by mutableDoubleStateOf(0.0)
    var gc by mutableDoubleStateOf(0.0)
    var infoKey: String = ""
    var infoCache: Any? = null

    /** y of an explicit graph (or of its derivative) at x; NaN when undefined. */
    fun yAt(x: Double): Double {
        val g = gd ?: return Double.NaN
        return try {
            if (g.dv == 0) {
                g.fn!!(x, 0.0, ga, gb, gc)
            } else {
                val b = g.base ?: return Double.NaN
                if (g.dv == 1) {
                    val e = 1e-4
                    (b.yAt(x + e) - b.yAt(x - e)) / (2 * e)
                } else {
                    val e = 1e-3
                    (b.yAt(x + e) - 2 * b.yAt(x) + b.yAt(x - e)) / (e * e)
                }
            }
        } catch (e: Exception) {
            Double.NaN
        }
    }

    /** Value of an implicit curve f(x, y) at a point. */
    fun vAt(x: Double, y: Double): Double {
        val g = gd ?: return Double.NaN
        return try {
            g.fn!!(x, y, ga, gb, gc)
        } catch (e: Exception) {
            Double.NaN
        }
    }

    fun copyOf(newBorn: Long): Fig = Fig(t, p, newBorn, nm).also {
        it.col = col
        it.sz = sz
        it.rot = rot
        it.ov = ov
        it.inkW = inkW
        it.hl = hl
        it.gd = gd
        it.ga = ga
        it.gb = gb
        it.gc = gc
    }
}
