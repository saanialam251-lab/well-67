package com.aismartmath.board.board

import com.aismartmath.board.math.MathNode
import com.aismartmath.board.math.angDeg
import com.aismartmath.board.math.circumcenter
import com.aismartmath.board.math.dist
import com.aismartmath.board.math.incenter
import com.aismartmath.board.math.isCyclic
import com.aismartmath.board.math.plain
import com.aismartmath.board.math.polyArea
import com.aismartmath.board.math.pts
import com.aismartmath.board.math.segAngle
import com.aismartmath.board.model.Fig
import com.aismartmath.board.model.Pt
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot

/** One piece of a "Details" line: plain text, a stacked fraction, or a maths layout. */
sealed interface Part {
    class T(val s: String) : Part
    class F(val n: String, val d: String) : Part
    class M(val node: MathNode) : Part
}

/** Stable text of a list of parts (used as an animation key). */
fun partsKey(parts: List<Part>): String = parts.joinToString("") {
    when (it) {
        is Part.T -> it.s
        is Part.F -> "[" + it.n + "/" + it.d + "]"
        is Part.M -> it.node.plain()
    }
}

class Field(val key: String, val label: String, val value: Double, val ro: Boolean = false)

val FigNames = mapOf(
    "pt" to "Point", "seg" to "Line", "circ" to "Circle", "rect" to "Rectangle",
    "rtri" to "Right triangle", "tri" to "Triangle", "quad" to "Quadrilateral",
    "pent" to "Pentagon", "hex" to "Hexagon", "fx" to "Graph", "ink" to "Drawing",
)

fun figName(f: Fig): String = FigNames[f.t] ?: f.t

/** Web: fields() - the number boxes in the edit panel. */
fun fieldsOf(f: Fig): List<Field> {
    if (f.t == "fx" || f.t == "ink") return emptyList()
    val p = f.p
    fun l(i: Int, j: Int) = dist(p[i], p[j])
    val q = p.getOrElse(1) { p[0] }
    val w = abs(q.x - p[0].x)
    val h = abs(q.y - p[0].y)
    val names = f.nm ?: "ABCDEF"
    return when (f.t) {
        "pt" -> listOf(Field("x", "x", p[0].x), Field("y", "y", p[0].y))
        "seg" -> listOf(Field("len", "Length", l(0, 1)), Field("ang", "Angle °", segAngle(p[0], p[1])))
        "circ" -> listOf(Field("r", "Radius", l(0, 1)))
        "rect" -> listOf(Field("w", "Width", w), Field("h", "Height", h))
        "rtri" -> listOf(Field("w", "Base", w), Field("h", "Height", h))
        "tri" -> listOf(Field("ab", "AB", l(0, 1)), Field("bc", "BC", l(1, 2)), Field("ca", "CA", l(2, 0)))
        "quad" -> listOf(
            Field("ab", "Side AB", l(0, 1)),
            Field("bc", "Side BC", l(1, 2)),
            Field("cd", "Side CD", l(2, 3)),
            Field("da", "Side DA", l(3, 0)),
            Field("ac", "Diagonal AC", l(0, 2)),
            Field("bd", "Diagonal BD", l(1, 3), true),
        )
        "pent", "hex" -> p.indices.map { i ->
            val a = if (i < names.length) names[i].toString() else "?"
            val b = if ((i + 1) % p.size < names.length) names[(i + 1) % p.size].toString() else "?"
            Field("s$i", "Side $a$b", l(i, (i + 1) % p.size), true)
        }
        else -> emptyList()
    }
}

private fun line(s: String): List<Part> = listOf(Part.T(s))

private fun classifyQuad(P: List<Pt>): String {
    val s = List(4) { dist(P[it], P[(it + 1) % 4]) }
    val eps = 1e-6 * maxOf(s[0], 1e-9)
    fun par(i: Int, j: Int): Boolean {
        val ux = P[(i + 1) % 4].x - P[i].x
        val uy = P[(i + 1) % 4].y - P[i].y
        val vx = P[(j + 1) % 4].x - P[j].x
        val vy = P[(j + 1) % 4].y - P[j].y
        return abs(ux * vy - uy * vx) < 1e-6 * hypot(ux, uy) * hypot(vx, vy)
    }
    val p1 = par(0, 2)
    val p2 = par(1, 3)
    val eq = s.all { abs(it - s[0]) < eps }
    val right = abs(angDeg(P[0], P[3], P[1]) - 90) < 1e-4
    return when {
        p1 && p2 && eq && right -> "square"
        p1 && p2 && right -> "rectangle"
        p1 && p2 && eq -> "rhombus"
        p1 && p2 -> "parallelogram"
        p1 || p2 -> "trapezium"
        (abs(s[0] - s[1]) < eps && abs(s[2] - s[3]) < eps) || (abs(s[1] - s[2]) < eps && abs(s[3] - s[0]) < eps) -> "kite"
        else -> "general quadrilateral"
    }
}

private fun triDetails(P: List<Pt>, nm: String): List<List<Part>> {
    val a = dist(P[1], P[2])
    val b = dist(P[2], P[0])
    val c = dist(P[0], P[1])
    val ar = polyArea(P)
    val per = a + b + c
    val an = List(3) { angDeg(P[it], P[(it + 2) % 3], P[(it + 1) % 3]) }
    val eps = 1e-6 * maxOf(a, b, c, 1e-9)
    val sideKind = if (abs(a - b) < eps && abs(b - c) < eps) "equilateral"
    else if (abs(a - b) < eps || abs(b - c) < eps || abs(a - c) < eps) "isosceles"
    else "scalene"
    val mx = an.max()
    val angKind = if (abs(mx - 90) < 1e-4) "right-angled" else if (mx > 90) "obtuse-angled" else "acute-angled"
    val out = mutableListOf<List<Part>>()
    out.add(line("Type: $sideKind, $angKind"))
    out.add(line("Area A = ${fmt(ar)}"))
    out.add(line("Perimeter P = ${fmt(per)}"))
    out.add(listOf(Part.T("Inradius r = "), Part.F("A", "s"), Part.T(" = ${fmt(ar / (per / 2))}")))
    out.add(listOf(Part.T("Circumradius R = "), Part.F("abc", "4A"), Part.T(" = ${fmt(a * b * c / (4 * ar))}")))
    out.add(line("Centroid G (${fmt(P.sumOf { it.x } / 3)}, ${fmt(P.sumOf { it.y } / 3)})"))
    val ic = incenter(P[0], P[1], P[2])
    out.add(line("Incentre I (${fmt(ic.x)}, ${fmt(ic.y)})"))
    val oc = circumcenter(P[0], P[1], P[2])
    if (oc != null) {
        out.add(line("Circumcentre O (${fmt(oc.x)}, ${fmt(oc.y)})"))
        out.add(line("Orthocentre H (${fmt(P[0].x + P[1].x + P[2].x - 2 * oc.x)}, ${fmt(P[0].y + P[1].y + P[2].y - 2 * oc.y)})"))
    }
    out.add(line("Angles " + (0 until 3).joinToString(", ") { "∠${nm[it]} = ${fmt(an[it], 1)}°" } + " (sum 180°)"))
    for (i in 0 until 3) {
        out.add(
            line(
                "Side ${nm[(i + 1) % 3]}${nm[(i + 2) % 3]} subtends ∠${nm[i]} = ${fmt(an[i], 1)}° at the circumference " +
                    "and ${fmt(2 * an[i], 1)}° at the centre O",
            ),
        )
    }
    return out
}

/** Web: props()/desc() - the full description shown by Details, with fractions. [par] adds parallel-line facts. */
fun propsOf(f: Fig, par: ParGeo? = null): List<List<Part>> {
    if (f.t == "fx") return listOf(graphParts(f))
    if (f.t == "ink") return listOf(line("A pen or highlighter stroke"))
    fun t(s: String): Part = Part.T(s)
    fun fr(n: String, d: String): Part = Part.F(n, d)
    val P = pts(f)
    val n = P.size
    val a = f.p[0]
    val b = f.p.getOrElse(1) { a }
    val names = f.nm ?: "ABCDEF"
    when (f.t) {
        "pt" -> return listOf(listOf(t("Point (${fmt(a.x)}, ${fmt(a.y)})")))
        "seg" -> {
            val dx = b.x - a.x
            val dy = b.y - a.y
            val out = mutableListOf(
                listOf(t("Length ${fmt(dist(a, b))}")),
                listOf(t("Angle ${fmt(segAngle(a, b), 1)}°")),
                listOf(t("Midpoint (${fmt((a.x + b.x) / 2)}, ${fmt((a.y + b.y) / 2)})")),
                if (dx == 0.0) listOf(t("Slope undefined"))
                else listOf(t("Slope m = "), fr(fmt(dy), fmt(dx)), t(" = ${fmt(dy / dx)}")),
            )
            if (par != null) out.addAll(parDetails(f, par))
            return out
        }
        "circ" -> {
            val r = dist(a, b)
            return listOf(
                listOf(t("Radius ${fmt(r)}")),
                listOf(t("Diameter d = 2r = ${fmt(2 * r)}")),
                listOf(t("Circumference C = 2πr = ${fmt(2 * PI * r)}")),
                listOf(t("Area A = πr² = ${fmt(PI * r * r)}")),
                listOf(t("Quarter arc = "), fr("πr", "2"), t(" = ${fmt(PI * r / 2)}")),
            )
        }
        "rect" -> {
            val w = abs(b.x - a.x)
            val h = abs(b.y - a.y)
            return listOf(
                listOf(t("${if (w == h) "Square" else "Rectangle"} ${fmt(w)} × ${fmt(h)}")),
                listOf(t("Area A = w × h = ${fmt(w * h)}")),
                listOf(t("Perimeter P = ${fmt(2 * (w + h))}")),
                listOf(t("Diagonal ${fmt(hypot(w, h))}")),
                listOf(t("Cyclic: its diagonals are diameters of the circumcircle")),
            )
        }
        "rtri" -> {
            val j = dist(P[0], P[1])
            val o = dist(P[1], P[2])
            val h = dist(P[0], P[2])
            val k = names[0].toString()
            val out = mutableListOf(
                listOf(t("Right angle at ${names[1]}")),
                listOf(t("Legs ${fmt(j)} and ${fmt(o)}, hypotenuse ${fmt(h)}")),
                listOf(t("∠$k = ${fmt(atan2(o, j) * 180 / PI, 1)}°")),
                listOf(t("sin $k = "), fr(fmt(o), fmt(h)), t(" = ${fmt(o / h)}")),
                listOf(t("cos $k = "), fr(fmt(j), fmt(h)), t(" = ${fmt(j / h)}")),
                listOf(t("tan $k = "), fr(fmt(o), fmt(j)), t(" = ${fmt(o / j)}")),
            )
            out.addAll(triDetails(P, names))
            return out
        }
        "tri" -> return triDetails(P, names)
    }
    // quadrilateral, pentagon, hexagon
    val out = mutableListOf<List<Part>>()
    val per = P.indices.sumOf { dist(P[it], P[(it + 1) % n]) }
    val an = List(n) { angDeg(P[it], P[(it + n - 1) % n], P[(it + 1) % n]) }
    val regular = P.indices.all { abs(dist(P[it], P[(it + 1) % n]) - dist(P[0], P[1])) < 1e-6 * dist(P[0], P[1]) } &&
        an.all { abs(it - an[0]) < 1e-4 }
    if (n == 4) {
        out.add(line("Type: " + classifyQuad(P)))
    } else {
        out.add(line((if (regular) "Regular " else "Irregular ") + FigNames[f.t]?.lowercase()))
    }
    out.add(line("Area A = ${fmt(polyArea(P))}"))
    out.add(line("Perimeter P = ${fmt(per)}"))
    if (n == 4) {
        out.add(line("Diagonals d₁ = ${fmt(dist(P[0], P[2]))}, d₂ = ${fmt(dist(P[1], P[3]))}"))
    } else {
        out.add(line("${n * (n - 3) / 2} diagonals"))
    }
    out.add(line("Angles " + (0 until n).joinToString(", ") { "∠${names[it]} = ${fmt(an[it], 1)}°" }))
    out.add(line("Angle sum ${(n - 2) * 180}° = ${n - 2} × 180°"))
    if (isCyclic(P)) {
        val oc = circumcenter(P[0], P[1], P[2])
        if (oc != null) out.add(line("Cyclic: all corners lie on a circle, radius ${fmt(dist(oc, P[0]))}"))
        if (n == 4) out.add(line("Opposite angles add up to 180°"))
    }
    return out
}
