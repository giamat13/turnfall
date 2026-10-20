package io.github.giamat13.turnfall.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.giamat13.turnfall.GameViewModel
import io.github.giamat13.turnfall.game.Chapters
import kotlinx.coroutines.delay
import kotlin.random.Random

/** A vector icon, tinted with [tint]. */
@Composable
fun VectorIcon(icon: ImageVector, tint: Color, modifier: Modifier = Modifier) {
    androidx.compose.foundation.Image(
        painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(icon),
        contentDescription = null,
        colorFilter = androidx.compose.ui.graphics.ColorFilter.tint(tint),
        modifier = modifier,
    )
}

/** The top bar of the play screen: a button back to the menu, and the daily puzzle, levels and settings buttons. */
@Composable
fun TopBar(vm: GameViewModel) {
    Row(
        Modifier
            .fillMaxWidth()
            .height(44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        RoundIconButton("Main menu", IconHome) { vm.showMenu() }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RoundIconButton("Daily puzzle", IconCalendar) { vm.playToday() }
            RoundIconButton("Levels", IconLevels) { vm.openLevels() }
            RoundIconButton("Settings", IconSettings) { vm.openSettings() }
        }
    }
}

/** A round 44 dp button with a 3 dp lower edge, as the page's `.icon-btn`. */
@Composable
fun RoundIconButton(description: String, icon: ImageVector, onClick: () -> Unit) {
    val p = LocalPalette.current
    Chunky(
        modifier = Modifier
            .size(44.dp)
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = p.surface,
        edgeHeight = 3.dp,
        pressFeedback = false,
        onClick = onClick,
    ) {
        VectorIcon(icon, p.ink, Modifier.size(20.dp))
    }
}

/** The line above the readout, in the display face. */
@Composable
fun Kicker(text: String, size: TextUnit, modifier: Modifier = Modifier) {
    LineBox(lineHeight = (size.value * 1.1f).dp, modifier = modifier) {
        GameText(
            text = text,
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(unbounded = true),
            style = TextStyle(
                color = LocalPalette.current.ink,
                fontFamily = SecularFamily,
                fontSize = size,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/** The readout: the turns on the left, the dial in the middle, the goal on the right. */
@Composable
fun Dash(vm: GameViewModel, compact: Boolean, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(20.dp)
    val bump = remember { Animatable(1f) }
    LaunchedEffect(vm.bumpTick) {
        if (vm.bumpTick > 0) {
            bump.snapTo(1f)
            bump.animateTo(1f, BUMP_SPEC)
        }
    }
    val numberSize = if (compact) 28.sp else 34.sp
    Row(
        modifier
            .fillMaxWidth()
            .lowerEdge(shape, p.edge, 4.dp)
            .clip(shape)
            .background(p.surface)
            .padding(horizontal = 16.dp, vertical = if (compact) 6.dp else 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Readout(
            label = "Turns",
            value = vm.turns.toString(),
            color = if (vm.turns > vm.lv.par) p.vermilion else p.ink,
            size = numberSize,
            endAligned = false,
            scale = bump.value,
            modifier = Modifier.weight(1f),
        )
        Dial(angle = vm.rotation.value)
        Readout(
            label = "Goal",
            value = vm.lv.par.toString(),
            color = p.ink,
            size = numberSize,
            endAligned = true,
            scale = 1f,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun Readout(
    label: String,
    value: String,
    color: Color,
    size: TextUnit,
    endAligned: Boolean,
    scale: Float,
    modifier: Modifier,
) {
    val p = LocalPalette.current
    val align = if (endAligned) TextAlign.End else TextAlign.Start
    Column(
        modifier,
        horizontalAlignment = if (endAligned) Alignment.End else Alignment.Start,
    ) {
        GameText(
            text = label,
            style = TextStyle(
                color = p.ink2,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp,
                textAlign = align,
            ),
        )
        LineBox(lineHeight = (size.value * 1.05f).dp) {
            GameText(
                text = value,
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(unbounded = true)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    },
                style = TextStyle(
                    color = color,
                    fontFamily = SecularFamily,
                    fontSize = size,
                    textAlign = align,
                ),
            )
        }
    }
}

/** The dial: a ring with a tick, and a red dot that turns with the world, so it always marks the world's bottom. */
@Composable
private fun Dial(angle: Float) {
    val p = LocalPalette.current
    Box(
        Modifier
            .size(50.dp)
            .drawBehind {
                // The tick hangs just below the ring.
                drawRect(
                    color = p.ink,
                    topLeft = Offset(size.width / 2 - 1.5.dp.toPx(), size.height + 2.dp.toPx()),
                    size = Size(3.dp.toPx(), 6.dp.toPx()),
                )
            },
    ) {
        Box(
            Modifier
                .matchParentSize()
                .border(3.dp, p.ink, CircleShape),
        )
        Box(
            Modifier
                .matchParentSize()
                .graphicsLayer { rotationZ = angle },
        ) {
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 4.dp)
                    .size(11.dp)
                    .clip(CircleShape)
                    .background(p.vermilion),
            )
        }
    }
}

/** The line under the world: what just happened. Its height is fixed, so the world never moves when the text changes. */
@Composable
fun MessageLine(text: String, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    GameText(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 24.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = TextStyle(
            color = p.ink2,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 24.sp,
            textAlign = TextAlign.Center,
        ),
    )
}

/** Undo, hint and restart: the buttons under the joystick. The counts show what is left. */
@Composable
fun ToolRow(vm: GameViewModel, compact: Boolean, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        ToolButton(
            icon = IconUndo,
            label = "Undo",
            badge = vm.undoLeft,
            enabled = vm.canUndo(),
            compact = compact,
            onClick = { vm.undo() },
        )
        ToolButton(
            icon = IconHint,
            label = "Hint",
            badge = vm.hintsLeft,
            enabled = vm.canHint(),
            compact = compact,
            onClick = { vm.useHint() },
        )
        ToolButton(
            icon = IconRestart,
            label = "Restart",
            badge = null,
            enabled = true,
            compact = compact,
            onClick = { vm.restart() },
        )
    }
}

@Composable
private fun RowScope.ToolButton(
    icon: ImageVector,
    label: String,
    badge: Int?,
    enabled: Boolean,
    compact: Boolean,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    Chunky(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = if (compact) 44.dp else 52.dp)
            .semantics { contentDescription = label },
        shape = RoundedCornerShape(18.dp),
        color = p.surface,
        enabled = enabled,
        onClick = onClick,
    ) {
        VectorIcon(icon, p.ink, Modifier.size(22.dp))
        if (badge != null) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 7.dp, y = (-7).dp)
                    .defaultMinSize(minWidth = 24.dp)
                    .height(24.dp)
                    .clip(CircleShape)
                    .background(p.ink)
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                GameText(
                    text = badge.toString(),
                    style = TextStyle(
                        color = p.paper,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        lineHeight = 13.sp,
                    ),
                )
            }
        }
    }
}

/**
 * The play area. Phones held upright show the world above the readout and the controls. Phones held sideways
 * show the world on the left and the readout and controls in a column on the right, as the page does.
 */
@Composable
fun PlayArea(vm: GameViewModel, modifier: Modifier = Modifier) {
    val config = LocalConfiguration.current
    val height = config.screenHeightDp
    val landscape = config.screenWidthDp > height
    if (landscape && height <= 560) {
        LandscapePlay(vm, modifier)
    } else {
        PortraitPlay(vm, shortScreen = !landscape && height <= 700, modifier = modifier)
    }
}

@Composable
private fun PortraitPlay(vm: GameViewModel, shortScreen: Boolean, modifier: Modifier) {
    Column(modifier.fillMaxSize()) {
        Kicker(vm.kicker, 32.sp, Modifier.padding(top = 4.dp))
        Dash(vm, compact = false, modifier = Modifier.padding(top = 10.dp))
        Box(
            Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BoardCard(vm, side = minOf(maxWidth, maxHeight, 360.dp))
            }
        }
        MessageLine(vm.message, Modifier.padding(top = 12.dp))
        Joystick(
            vm = vm,
            height = if (shortScreen) 56.dp else 64.dp,
            knobWidth = if (shortScreen) 64.dp else 68.dp,
            knobHeight = if (shortScreen) 44.dp else 48.dp,
            maxWidth = 300.dp,
            modifier = Modifier
                .padding(top = 10.dp)
                .align(Alignment.CenterHorizontally),
        )
        ToolRow(vm, compact = false, modifier = Modifier.padding(top = 10.dp))
    }
}

@Composable
private fun LandscapePlay(vm: GameViewModel, modifier: Modifier) {
    Row(
        modifier.fillMaxSize(),
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            BoxWithConstraints(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                BoardCard(vm, side = minOf(maxWidth, maxHeight, 360.dp))
            }
        }
        Column(Modifier.width(340.dp).fillMaxHeight()) {
            Kicker(vm.kicker, 26.sp, Modifier.padding(top = 6.dp))
            Dash(vm, compact = true, modifier = Modifier.padding(top = 8.dp, bottom = 10.dp))
            Spacer(Modifier.weight(1f))
            MessageLine(vm.message, Modifier.padding(top = 6.dp))
            Joystick(
                vm = vm,
                height = 56.dp,
                knobWidth = 64.dp,
                knobHeight = 44.dp,
                maxWidth = 260.dp,
                modifier = Modifier
                    .padding(top = 10.dp)
                    .align(Alignment.CenterHorizontally),
            )
            ToolRow(vm, compact = true, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/** The win card: stars, the turns against par, the buttons, and confetti for three stars. */
@Composable
fun WinOverlay(vm: GameViewModel) {
    val p = LocalPalette.current
    val fade = remember { Animatable(0f) }
    val rise = remember { Animatable(0f) }
    val pop = remember { Animatable(0.4f) }
    val clock = remember { Animatable(0f) }
    val pieces = remember(vm.winShown) { confettiPieces() }
    LaunchedEffect(vm.winShown) {
        if (vm.winShown) {
            fade.snapTo(0f)
            fade.animateTo(1f, tween(durationMillis = 300))
        }
    }
    LaunchedEffect(vm.winShown) {
        if (vm.winShown) {
            rise.snapTo(0f)
            rise.animateTo(1f, tween(durationMillis = 450, easing = RISE_EASE))
        }
    }
    LaunchedEffect(vm.winShown) {
        if (vm.winShown) {
            pop.snapTo(0.4f)
            delay(100)
            pop.animateTo(1f, POP_SPEC)
        }
    }
    LaunchedEffect(vm.winShown) {
        if (vm.winShown && vm.winConfetti) {
            clock.snapTo(0f)
            clock.animateTo(CONFETTI_MS, tween(durationMillis = CONFETTI_MS.toInt(), easing = LinearEasing))
        }
    }
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = fade.value }
            .background(p.scrim)
            .pointerInput(Unit) { detectTapGestures { } },
        contentAlignment = Alignment.Center,
    ) {
        val cardShape = RoundedCornerShape(26.dp)
        Box(
            Modifier
                .padding(16.dp)
                .widthIn(max = 340.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    translationY = (1f - rise.value) * 12.dp.toPx()
                    alpha = rise.value
                }
                .lowerEdge(cardShape, p.edge, 8.dp)
                .clip(cardShape)
                .background(p.surface),
        ) {
            if (vm.winConfetti) Confetti(clock.value, pieces)
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                GameText(
                    text = vm.winStars,
                    modifier = Modifier.graphicsLayer {
                        scaleX = pop.value
                        scaleY = pop.value
                        alpha = ((pop.value - 0.4f) / 0.6f).coerceIn(0f, 1f)
                    },
                    style = TextStyle(
                        color = p.vermilion,
                        fontFamily = SecularFamily,
                        fontSize = 44.sp,
                        lineHeight = 44.sp,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
                GameText(
                    text = vm.winTitle,
                    style = TextStyle(
                        color = p.ink,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center,
                    ),
                )
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    WinButton(
                        modifier = Modifier.weight(1f),
                        color = p.sun,
                        onClick = { vm.winNext() },
                    ) {
                        GameText(
                            text = vm.winNext,
                            style = TextStyle(color = p.onSun, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp),
                        )
                    }
                    if (vm.winMapShown) {
                        WinButton(
                            modifier = Modifier.weight(1f),
                            color = p.surface,
                            description = "Chapter map",
                            onClick = { vm.openMap(Chapters.of(vm.level), mapColorsOf(p)) },
                        ) {
                            VectorIcon(IconMap, p.ink, Modifier.size(22.dp))
                        }
                    }
                    WinButton(
                        modifier = Modifier.weight(1f),
                        color = p.surface,
                        description = "Levels",
                        onClick = { vm.openLevels() },
                    ) {
                        VectorIcon(IconLevels, p.ink, Modifier.size(22.dp))
                    }
                    WinButton(
                        modifier = Modifier.weight(1f),
                        color = p.surface,
                        description = "Share result",
                        onClick = { vm.shareResult() },
                    ) {
                        VectorIcon(IconShare, p.ink, Modifier.size(22.dp))
                        GameText(
                            text = "Share",
                            style = TextStyle(color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WinButton(
    modifier: Modifier,
    color: Color,
    description: String? = null,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit,
) {
    val p = LocalPalette.current
    Chunky(
        modifier = modifier
            .heightIn(min = 52.dp)
            .let { if (description != null) it.semantics { contentDescription = description } else it },
        shape = RoundedCornerShape(18.dp),
        color = color,
        edge = p.edge,
        onClick = onClick,
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            content = content,
        )
    }
}

/** One confetti piece: where it starts, how far it sways, how long it falls, when it starts, and its colour. */
private class ConfettiPiece(
    val left: Float,
    val sway: Float,
    val duration: Float,
    val delay: Float,
    val colorIndex: Int,
)

private const val CONFETTI_MS = 3000f

private fun confettiPieces(): List<ConfettiPiece> {
    val random = Random(System.nanoTime())
    return List(18) { k ->
        ConfettiPiece(
            left = random.nextFloat(),
            sway = (random.nextFloat() - 0.5f) * 140f,
            duration = 1300f + random.nextFloat() * 900f,
            delay = random.nextFloat() * 350f,
            colorIndex = k % 4,
        )
    }
}

@Composable
private fun BoxScope.Confetti(time: Float, pieces: List<ConfettiPiece>) {
    val p = LocalPalette.current
    val colors = listOf(p.sun, p.vermilion, p.wall, p.mud)
    val fall = CubicBezierEasing(0.42f, 0f, 1f, 1f)
    Canvas(Modifier.matchParentSize()) {
        val pieceWidth = 9.dp.toPx()
        val pieceHeight = 13.dp.toPx()
        for (piece in pieces) {
            // The page's pieces wait at the top until their delay ends, then fall and spin out.
            val k = ((time - piece.delay) / piece.duration).coerceIn(0f, 1f)
            val e = fall.transform(k)
            val x = piece.left * size.width + piece.sway * e
            val y = -14.dp.toPx() + 440.dp.toPx() * e
            withTransform({
                translate(x, y)
                rotate(600f * e, pivot = Offset(pieceWidth / 2, pieceHeight / 2))
            }) {
                drawRoundRect(
                    color = colors[piece.colorIndex].copy(alpha = 1f - e),
                    size = Size(pieceWidth, pieceHeight),
                    cornerRadius = CornerRadius(3.dp.toPx()),
                )
            }
        }
    }
}

private val RISE_EASE = CubicBezierEasing(0.2f, 0.8f, 0.2f, 1f)

private val POP_SPEC = keyframes<Float> {
    durationMillis = 600
    0.4f at 0
    1.12f at 360
    1f at 600
}

/** The colours of the chapter map, from the palette of the screen that shows it. */
fun mapColorsOf(p: Palette): MapColors = MapColors(
    paper = p.surface.toArgb(),
    ink = p.ink.toArgb(),
    ink2 = p.ink2.toArgb(),
    tile = p.tile.toArgb(),
    sun = p.sun.toArgb(),
    vermilion = p.vermilion.toArgb(),
)
