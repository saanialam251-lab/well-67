package com.aismartmath.board.math

import com.aismartmath.board.model.Fn
import kotlin.math.E
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.cosh
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sinh
import kotlin.math.sqrt
import kotlin.math.tan
import kotlin.math.tanh

/**
 * Port of the web parse / parseEq / toTex. Instead of JavaScript eval it uses a small safe parser.
 * Supports: + - * / ^ ** ² ³ ( ) x y a b c e pi π, implicit multiplication (2x, a(x+1), 3sin(x)) and
 * sqrt cosec csc sec cot asin acos atan sinh cosh tanh sin cos tan abs exp floor ceil sign log ln.
 */
private val TOKEN = Regex("\\d*\\.?\\d+|sqrt|cosec|csc|sec|cot|asin|acos|atan|sinh|cosh|tanh|sin|cos|tan|abs|exp|floor|ceil|sign|log|ln|pi|[xyabce]|\\*\\*|[-+*/()]")
private val FUNCS = setOf("sqrt", "cosec", "csc", "sec", "cot", "asin", "acos", "atan", "sinh", "cosh", "tanh", "sin", "cos", "tan", "abs", "exp", "floor", "ceil", "sign", "log", "ln")
private val NUMBER = Regex("^(\\d|\\.)")

private fun isF(t: String) = t in FUNCS
private fun isV(t: String) = NUMBER.containsMatchIn(t) || t == "pi" || (t.length == 1 && t[0] in "xyabce")
private fun startsAtom(t: String) = isV(t) || isF(t) || t == "("

private fun normalize(src: String, stripY: Boolean): String {
    var s = src.lowercase()
    if (stripY) s = s.replace(Regex("^\\s*y\\s*="), "")
    return s.replace(Regex("\\s"), "").replace("π", "pi").replace("^", "**").replace("²", "**2").replace("³", "**3")
}

// ---------------- evaluation tree ----------------
private sealed interface Node { fun ev(x: Double, y: Double, a: Double, b: Double, c: Double): Double }
private class Num(val v: Double) : Node { override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double) = v }
private class Var(val k: Char) : Node {
    override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double) = when (k) { 'x' -> x; 'y' -> y; 'a' -> a; 'b' -> b; else -> c }
}
private class Neg(val n: Node) : Node { override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double) = -n.ev(x, y, a, b, c) }
private class Bin(val op: Char, val l: Node, val r: Node) : Node {
    override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double): Double {
        val p = l.ev(x, y, a, b, c)
        val q = r.ev(x, y, a, b, c)
        return when (op) { '+' -> p + q; '-' -> p - q; '*' -> p * q; else -> p / q }
    }
}
private class Pow(val l: Node, val r: Node) : Node {
    override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double) = l.ev(x, y, a, b, c).pow(r.ev(x, y, a, b, c))
}
private class Call(val name: String, val arg: Node) : Node {
    override fun ev(x: Double, y: Double, a: Double, b: Double, c: Double): Double {
        val v = arg.ev(x, y, a, b, c)
        return when (name) {
            "sqrt" -> sqrt(v); "sin" -> sin(v); "cos" -> cos(v); "tan" -> tan(v); "abs" -> abs(v); "exp" -> exp(v)
            "log" -> log10(v); "ln" -> ln(v)
            "csc", "cosec" -> 1 / sin(v); "sec" -> 1 / cos(v); "cot" -> 1 / tan(v)
            "asin" -> asin(v); "acos" -> acos(v); "atan" -> atan(v)
            "sinh" -> sinh(v); "cosh" -> cosh(v); "tanh" -> tanh(v)
            "floor" -> floor(v); "ceil" -> ceil(v); "sign" -> sign(v)
            else -> Double.NaN
        }
    }
}

private class Parser(val t: List<String>) {
    var i = 0
    fun pk(): String? = t.getOrNull(i)
    fun nx(): String? = t.getOrNull(i++)

    fun expr(): Node? {
        var l = term() ?: return null
        while (pk() == "+" || pk() == "-") {
            val op = nx()!![0]
            val r = term() ?: return null
            l = Bin(op, l, r)
        }
        return l
    }

    fun term(): Node? {
        var l = unary() ?: return null
        while (true) {
            val tk = pk() ?: break
            if (tk == "*" || tk == "/") {
                nx()
                val r = unary() ?: return null
                l = Bin(tk[0], l, r)
            } else if (startsAtom(tk)) {
                val r = unary() ?: return null
                l = Bin('*', l, r)
            } else break
        }
        return l
    }

    fun unary(): Node? {
        if (pk() == "-") { nx(); val u = unary() ?: return null; return Neg(u) }
        if (pk() == "+") { nx(); return unary() }
        return power()
    }

    fun power(): Node? {
        val b = atom() ?: return null
        if (pk() == "**") { nx(); val e = expo() ?: return null; return Pow(b, e) }
        return b
    }

    fun expo(): Node? {
        if (pk() == "-") { nx(); val e = expo() ?: return null; return Neg(e) }
        if (pk() == "+") { nx(); return expo() }
        return power()
    }

    fun atom(): Node? {
        val tk = nx() ?: return null
        if (tk == "(") {
            val e = expr() ?: return null
            if (nx() != ")") return null
            return e
        }
        if (isF(tk)) {
            if (nx() != "(") return null
            val e = expr() ?: return null
            if (nx() != ")") return null
            return Call(tk, e)
        }
        if (tk == "pi") return Num(PI)
        if (tk == "e") return Num(E)
        if (tk.length == 1 && tk[0] in "xyabc") return Var(tk[0])
        return tk.toDoubleOrNull()?.let { Num(it) }
    }
}

private fun tokens(s: String): List<String>? {
    val tk = TOKEN.findAll(s).map { it.value }.toList()
    if (tk.isEmpty() || tk.joinToString("") != s) return null
    return tk
}

/** Web: parse(src). Returns a function of (x, y, a, b, c) or null if it cannot be read. */
fun parseExpr(src: String): Fn? {
    val s = normalize(src, stripY = true)
    val tk = tokens(s) ?: return null
    val p = Parser(tk)
    val n = p.expr() ?: return null
    if (p.i != tk.size) return null
    return { x, y, a, b, c -> n.ev(x, y, a, b, c) }
}

class ParsedEq(val fn: Fn, val imp: Boolean)

/** Web: parseEq - y = f(x), f(x), or an implicit equation like x^2 + y^2 = 25. */
fun parseEq(src: String): ParsedEq? {
    val s = src.lowercase().replace(Regex("\\s"), "")
    val k = s.indexOf('=')
    if (k < 0) {
        val f = parseExpr(s) ?: return null
        return ParsedEq(f, 'y' in s)
    }
    val l = s.substring(0, k)
    val r = s.substring(k + 1)
    if (l.isEmpty() || r.isEmpty() || r.contains('=')) return null
    if (l == "y" && 'y' !in r) {
        val f = parseExpr(r) ?: return null
        return ParsedEq(f, false)
    }
    val f = parseExpr(l) ?: return null
    val g = parseExpr(r) ?: return null
    return ParsedEq({ x, y, a, b, c -> f(x, y, a, b, c) - g(x, y, a, b, c) }, true)
}

private val TYPING_STAR = Regex("([\\dxyabce)])\\*(?=[xyabce](?![a-z])|\\()")
private val TYPING_SQ = Regex("\\^2(?![\\d.])")
private val TYPING_CU = Regex("\\^3(?![\\d.])")

/** Web typing shortcuts: a*x becomes ax, x^2 becomes x², x^3 becomes x³ (a*sin(bx) and 2*3 are left alone). */
fun tidyTyping(o: String): String =
    o.replace(TYPING_STAR, "\$1").replace(TYPING_SQ, "²").replace(TYPING_CU, "³")

fun usesParams(src: String): Boolean =
    Regex("[abc]").containsMatchIn(
        src.lowercase().replace(Regex("sqrt|cosec|csc|sec|cot|asin|acos|atan|sinh|cosh|tanh|sin|cos|tan|abs|exp|floor|ceil|sign|log|ln"), ""),
    )

// ---------------- maths layout (web: toTex / eqTex) ----------------
private class MathBuilder(val t: List<String>) {
    var i = 0
    fun pk(): String? = t.getOrNull(i)
    fun nx(): String? = t.getOrNull(i++)

    fun ex(): MathNode {
        val items = mutableListOf(tm())
        while (pk() == "+" || pk() == "-") {
            items.add(MathNode.Txt(if (nx() == "+") " + " else " − "))
            items.add(tm())
        }
        return if (items.size == 1) items[0] else MathNode.Seq(items)
    }

    fun tm(): MathNode {
        var o = un()
        while (true) {
            val tk = pk() ?: break
            if (tk == "*") {
                nx()
                val implicit = pk()?.let { it.isNotEmpty() && (it[0] in 'a'..'z' || it[0] == '(') } ?: false
                val r = un()
                o = if (implicit) MathNode.Seq(listOf(o, r)) else MathNode.Seq(listOf(o, MathNode.Txt(" · "), r))
            } else if (tk == "/") {
                nx()
                o = MathNode.Frac(o, un())
            } else if (tk != ")" && tk != "+" && tk != "-" && tk != "**") {
                o = MathNode.Seq(listOf(o, un()))
            } else break
        }
        return o
    }

    fun un(): MathNode {
        if (pk() == "-") { nx(); return MathNode.Seq(listOf(MathNode.Txt("−"), un())) }
        if (pk() == "+") { nx(); return un() }
        return pw()
    }

    fun pw(): MathNode {
        val b = at()
        if (pk() == "**") { nx(); return MathNode.Sup(b, un()) }
        return b
    }

    fun at(): MathNode {
        val tk = nx() ?: return MathNode.Txt("")
        if (tk == "(") {
            val o = ex()
            if (pk() == ")") nx()
            return MathNode.Paren(o)
        }
        if (isF(tk)) {
            val o: MathNode
            if (pk() == "(") {
                nx()
                o = ex()
                if (pk() == ")") nx()
            } else o = un()
            return when (tk) {
                "sqrt" -> MathNode.Sqrt(o)
                "abs" -> MathNode.Seq(listOf(MathNode.Txt("|"), o, MathNode.Txt("|")))
                "exp" -> MathNode.Sup(MathNode.Txt("e"), o)
                else -> MathNode.Seq(listOf(MathNode.Txt(funName(tk)), MathNode.Paren(o)))
            }
        }
        return MathNode.Txt(if (tk == "pi") "π" else tk)
    }
}

private fun funName(t: String) = when (t) { "asin" -> "arcsin"; "acos" -> "arccos"; "atan" -> "arctan"; else -> t }

fun toMath(src: String): MathNode {
    val s = normalize(src, stripY = true)
    val tk = TOKEN.findAll(s).map { it.value }.toList()
    return MathBuilder(tk).ex()
}

/** Web: eqTex - "y = ..." or "lhs = rhs". */
fun eqMath(src: String): MathNode {
    val s = src.lowercase().replace(Regex("\\s"), "")
    val k = s.indexOf('=')
    if (k < 0) {
        return if ('y' in s) MathNode.Seq(listOf(toMath(s), MathNode.Txt(" = 0")))
        else MathNode.Seq(listOf(MathNode.Txt("y = "), toMath(s)))
    }
    val l = s.substring(0, k)
    val r = s.substring(k + 1)
    return if (l == "y" && 'y' !in r) MathNode.Seq(listOf(MathNode.Txt("y = "), toMath(r)))
    else MathNode.Seq(listOf(toMath(l), MathNode.Txt(" = "), toMath(r)))
}
