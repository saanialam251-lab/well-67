package com.aismartmath.board.math

/** A tiny maths layout tree (the web version used KaTeX for this). Drawn by ui/components/MathView. */
sealed interface MathNode {
    class Seq(val items: List<MathNode>) : MathNode
    class Txt(val s: String) : MathNode
    class Sup(val base: MathNode, val exp: MathNode) : MathNode
    class Frac(val n: MathNode, val d: MathNode) : MathNode
    class Sqrt(val n: MathNode) : MathNode
    class Paren(val n: MathNode) : MathNode
}

/** Plain text of a node - used to build stable keys for animations. */
fun MathNode.plain(): String = when (this) {
    is MathNode.Seq -> items.joinToString("") { it.plain() }
    is MathNode.Txt -> s
    is MathNode.Sup -> base.plain() + "^(" + exp.plain() + ")"
    is MathNode.Frac -> "(" + n.plain() + ")/(" + d.plain() + ")"
    is MathNode.Sqrt -> "sqrt(" + n.plain() + ")"
    is MathNode.Paren -> "(" + n.plain() + ")"
}
