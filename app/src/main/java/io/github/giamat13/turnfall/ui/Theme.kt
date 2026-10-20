@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package io.github.giamat13.turnfall.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import io.github.giamat13.turnfall.R

/** The colours of the page, in its light and dark schemes. The names are the page's CSS variables. */
class Palette(
    val paper: Color,
    val surface: Color,
    val tile: Color,
    val mud: Color,
    val mudInk: Color,
    val pit: Color,
    val pitInk: Color,
    val wall: Color,
    val wallLip: Color,
    val ink: Color,
    val ink2: Color,
    val line: Color,
    val edge: Color,
    val sun: Color,
    val sunEdge: Color,
    val vermilion: Color,
    val hole: Color,
    val onSun: Color,
    val onInk: Color,
    val scrim: Color,
)

val LightPalette = Palette(
    paper = Color(0xFFE6EEFF),
    surface = Color(0xFFFFFFFF),
    tile = Color(0xFFD3E2FF),
    mud = Color(0xFFF2D08F),
    mudInk = Color(0xFF7D5A1E),
    pit = Color(0xFFE5583C),
    pitInk = Color(0xFF3A0D05),
    wall = Color(0xFF3D6CF2),
    wallLip = Color(0xFF2A4FC4),
    ink = Color(0xFF1C2857),
    ink2 = Color(0xFF5B6A9A),
    line = Color(28, 40, 87, 36),
    edge = Color(0xFFB7C8EE),
    sun = Color(0xFFFFC83D),
    sunEdge = Color(0xFF1C2857),
    vermilion = Color(0xFFF2552E),
    hole = Color(0xFF0D1538),
    onSun = Color(0xFF1C2857),
    onInk = Color(0xFFFFFFFF),
    scrim = Color(13, 21, 56, 115),
)

val DarkPalette = Palette(
    paper = Color(0xFF0C1334),
    surface = Color(0xFF161F4A),
    tile = Color(0xFF212D63),
    mud = Color(0xFF6E5A2E),
    mudInk = Color(0xFFF0D495),
    pit = Color(0xFFB8402A),
    pitInk = Color(0xFF080303),
    wall = Color(0xFF7D9FFF),
    wallLip = Color(0xFF6686F0),
    ink = Color(0xFFEEF2FF),
    ink2 = Color(0xFFA3B0D6),
    line = Color(238, 242, 255, 36),
    edge = Color(0xFF060B25),
    sun = Color(0xFFFFC83D),
    sunEdge = Color(0xFF050A20),
    vermilion = Color(0xFFFF6B4A),
    hole = Color(0xFF050A20),
    onSun = Color(0xFF0C1334),
    onInk = Color(0xFF0C1334),
    scrim = Color(5, 10, 32, 153),
)

val LocalPalette = staticCompositionLocalOf { LightPalette }

/** Gives every screen the palette of the chosen scheme. */
@Composable
fun TurnfallTheme(dark: Boolean, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalPalette provides if (dark) DarkPalette else LightPalette, content = content)
}

private fun assistant(weight: FontWeight, wght: Int) = Font(
    resId = R.font.assistant_variable,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(wght)),
)

/** Assistant, the page's text face, in the four weights the page uses. */
val AssistantFamily = FontFamily(
    assistant(FontWeight.Normal, 400),
    assistant(FontWeight.SemiBold, 600),
    assistant(FontWeight.Bold, 700),
    assistant(FontWeight.ExtraBold, 800),
)

/** Secular One, the page's display face for headings and numbers. */
val SecularFamily = FontFamily(Font(R.font.secular_one, FontWeight.Normal))
