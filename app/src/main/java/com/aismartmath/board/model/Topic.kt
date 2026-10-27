package com.aismartmath.board.model

import androidx.compose.ui.graphics.Color

/** One home-screen topic. Copied from the web version's MODS table (same order, icons, colours, tools). */
data class Topic(
    val id: String,
    val icon: String,
    val name: String,
    val color: Color,
    val toolIds: List<String>,
    val axes: Boolean,
)

object Topics {
    val all: List<Topic> = listOf(
        Topic("tri", "△", "Triangles", Color(0xFF6366F1), listOf("tri", "rtri", "sk", "seg", "pt", "sel", "pan"), false),
        Topic("quad", "▱", "Quadrilaterals", Color(0xFFEC4899), listOf("rect", "quad", "pent", "hex", "sk", "seg", "pt", "sel", "pan"), false),
        Topic("circ", "○", "Circles", Color(0xFF14B8A6), listOf("circ", "sk", "seg", "pt", "sel", "pan"), false),
        Topic("coord", "⌖", "Coordinate Geometry", Color(0xFFF59E0B), listOf("fx", "dv", "sk", "pt", "seg", "tri", "rect", "quad", "circ", "sel", "pan"), true),
        Topic("der", "𝑓′", "Derivatives", Color(0xFFE11D48), listOf("dv", "pt", "seg", "sel", "pan"), true),
        Topic("solve", "✍️", "Solve", Color(0xFFEAB308), listOf("pen", "hl", "er", "sk", "sel", "seg", "pan"), false),
        Topic("trig", "∠", "Plane Trigonometry", Color(0xFF8B5CF6), listOf("trig", "unit", "rtri", "sel", "pan"), true),
        Topic("line", "╱", "Lines & Points", Color(0xFF38BDF8), listOf("seg", "pt", "sel", "pan"), true),
        Topic("par", "∥", "Parallel Lines", Color(0xFFF97316), listOf("seg", "pt", "sel", "pan"), true),
        Topic("solid", "🧊", "3D Solids", Color(0xFF06B6D4), listOf("cube", "cuboid", "pyr", "prism", "cyl", "cone", "sph", "sel", "pan"), false),
        Topic("all", "✳", "All Together", Color(0xFF22C55E), listOf("sk", "pt", "seg", "tri", "rtri", "quad", "pent", "hex", "rect", "circ", "fx", "dv", "trig", "unit", "cube", "cuboid", "pyr", "prism", "cyl", "cone", "sph", "sel", "pan"), true),
    )
    val byId: Map<String, Topic> = all.associateBy { it.id }
}
