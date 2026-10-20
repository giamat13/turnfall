package io.github.giamat13.turnfall.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/**
 * The page's line icons. Each one is drawn on a 24 unit grid with round caps and joins, as the page's SVG is.
 * The colour is set by the Icon that shows it.
 */
private fun lineIcon(name: String, strokeWidth: Float = 2.2f, paths: List<String>): ImageVector {
    val builder = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )
    for (data in paths) {
        builder.addPath(
            pathData = addPathNodes(data),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
        )
    }
    return builder.build()
}

/** A circle as two half arcs, the way an SVG path draws it. */
private fun circle(cx: Float, cy: Float, r: Float): String =
    "M${cx - r} ${cy}a$r $r 0 1 0 ${2 * r} 0a$r $r 0 1 0 ${-2 * r} 0"

val IconCalendar = lineIcon(
    "calendar",
    paths = listOf("M6 5h12a3 3 0 0 1 3 3v10a3 3 0 0 1-3 3H6a3 3 0 0 1-3-3V8a3 3 0 0 1 3-3z", "M3 10h18M8 3v4M16 3v4"),
)

val IconLevels = lineIcon(
    "levels",
    paths = listOf(
        circle(6f, 6f, 2.5f),
        circle(18f, 12f, 2.5f),
        circle(6f, 18f, 2.5f),
        "M8.5 6.5h4a3 3 0 0 1 3 3v0a3 3 0 0 0 3 3M6 8.5v7",
    ),
)

val IconSettings = lineIcon(
    "settings",
    paths = listOf(
        circle(12f, 12f, 3f),
        "M12 2v3M12 19v3M2 12h3M19 12h3M4.9 4.9l2.1 2.1M17 16.9l2.1 2.1M4.9 19.1 7 17M17 7l2.1-2.1",
    ),
)

val IconClose = lineIcon("close", paths = listOf("M18 6 6 18", "m6 6 12 12"))

val IconUndo = lineIcon(
    "undo",
    paths = listOf("M9 14 4 9l5-5", "M4 9h10.5a5.5 5.5 0 0 1 5.5 5.5v0a5.5 5.5 0 0 1-5.5 5.5H11"),
)

val IconHint = lineIcon(
    "hint",
    paths = listOf(
        "M9 18h6M10 21h4",
        "M12 3a6 6 0 0 0-3.5 10.9c.6.5 1 1.2 1 2.1h5c0-.9.4-1.6 1-2.1A6 6 0 0 0 12 3z",
    ),
)

val IconRestart = lineIcon(
    "restart",
    paths = listOf(
        "M3 12a9 9 0 0 1 9-9 9.75 9.75 0 0 1 6.74 2.74L21 8",
        "M21 3v5h-5",
        "M21 12a9 9 0 0 1-9 9 9.75 9.75 0 0 1-6.74-2.74L3 16",
        "M8 16H3v5",
    ),
)

val IconMap = lineIcon(
    "map",
    paths = listOf("M9 4 3 6v14l6-2 6 2 6-2V4l-6 2-6-2z", "M9 4v14M15 6v14"),
)

val IconCopy = lineIcon(
    "copy",
    paths = listOf("M11 8h6a3 3 0 0 1 3 3v6a3 3 0 0 1-3 3h-6a3 3 0 0 1-3-3v-6a3 3 0 0 1 3-3z", "M16 8V6a2 2 0 0 0-2-2H6a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h2"),
)

val IconChevronLeft = lineIcon("chevron-left", paths = listOf("m15 6-6 6 6 6"))

val IconChevronRight = lineIcon("chevron-right", paths = listOf("m9 6 6 6-6 6"))

/** The handle's two arrows: counter-clockwise on the left, clockwise on the right. */
val IconJoyLeft = lineIcon(
    "joy-left",
    strokeWidth = 2.4f,
    paths = listOf("M3 12a9 9 0 1 0 9-9 9.75 9.75 0 0 0-6.74 2.74L3 8", "M3 3v5h5"),
)

val IconJoyRight = lineIcon(
    "joy-right",
    strokeWidth = 2.4f,
    paths = listOf("M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8", "M21 3v5h-5"),
)

val IconHome = lineIcon("home", paths = listOf("M3 11l9-7 9 7v9a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z"))

val IconShare = lineIcon(
    "share",
    paths = listOf("M12 3v12", "M8 7l4-4 4 4", "M5 13v6a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-6"),
)

val IconPlay = lineIcon("play", paths = listOf("M8 5v14l11-7z"))
