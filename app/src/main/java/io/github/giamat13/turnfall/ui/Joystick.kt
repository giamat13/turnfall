package io.github.giamat13.turnfall.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import io.github.giamat13.turnfall.GameViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * The handle: a bar that slides left and right to turn the world. Pushed past half way, the half it leans toward
 * lights up, and letting go turns the world that way. It springs back to the middle.
 */
@Composable
fun Joystick(
    vm: GameViewModel,
    height: Dp,
    knobWidth: Dp,
    knobHeight: Dp,
    maxWidth: Dp,
    modifier: Modifier = Modifier,
) {
    val p = LocalPalette.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val shape = RoundedCornerShape(percent = 50)
    val knobShape = RoundedCornerShape(16.dp)
    val nudge = remember { Animatable(0f) }
    val returning = remember { Animatable(0f) }
    var dragX by remember { mutableFloatStateOf(0f) }
    var dragging by remember { mutableStateOf(false) }
    var trackWidth by remember { mutableFloatStateOf(0f) }
    val knobWidthPx = with(density) { knobWidth.toPx() }
    val disabled = vm.busy || vm.won || vm.failed

    // On the first visit the knob nudges twice, so the player sees that it moves.
    LaunchedEffect(vm.teach) {
        if (vm.teach) {
            repeat(2) { nudge.animateTo(0f, NUDGE_SPEC) }
        } else {
            nudge.snapTo(0f)
        }
    }

    Box(
        modifier
            .widthIn(max = maxWidth)
            .height(height)
            .graphicsLayer { alpha = if (disabled) 0.4f else 1f }
            .lowerEdge(shape, p.edge, 4.dp)
            .clip(shape)
            .background(p.surface)
            .onSizeChanged { trackWidth = it.width.toFloat() }
            .pointerInput(vm) {
                val edgeGap = 4.dp.toPx()
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (!vm.joyDown()) return@awaitEachGesture
                    dragging = true
                    val origin = down.position.x
                    val radius = trackWidth / 2f - knobWidthPx / 2f - edgeGap
                    var finished = false
                    while (!finished) {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull { it.id == down.id } ?: break
                        if (change.pressed) {
                            dragX = (change.position.x - origin).coerceIn(-radius, radius)
                            if (radius > 0f) vm.joyMoved(dragX / radius)
                        } else {
                            finished = true
                        }
                        change.consume()
                    }
                    dragging = false
                    val from = dragX
                    scope.launch {
                        returning.snapTo(from)
                        returning.animateTo(0f, tween(durationMillis = 180))
                    }
                    vm.joyUp(cancelled = false)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        // The dashed centre line, between the two arrows.
        Canvas(Modifier.fillMaxSize()) {
            val y = size.height / 2
            val inset = 52.dp.toPx()
            drawLine(
                color = p.line,
                start = Offset(inset, y),
                end = Offset(size.width - inset, y),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx())),
            )
        }
        // The half the handle leans toward takes the tile colour, as the page's highlight does.
        if (vm.joyPull != 0) {
            Box(
                Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(0.5f)
                    .align(if (vm.joyPull < 0) Alignment.CenterStart else Alignment.CenterEnd)
                    .background(p.tile),
            )
        }
        VectorIcon(
            icon = IconJoyLeft,
            tint = if (vm.joyPull < 0) p.ink else p.ink2,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 14.dp)
                .size(22.dp),
        )
        VectorIcon(
            icon = IconJoyRight,
            tint = if (vm.joyPull > 0) p.ink else p.ink2,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 14.dp)
                .size(22.dp),
        )
        Box(
            Modifier
                .offset { IntOffset(((if (dragging) dragX else returning.value) + nudge.value).roundToInt(), 0) }
                .size(knobWidth, knobHeight)
                .clip(knobShape)
                .background(p.sun)
                .border(2.dp, p.sunEdge, knobShape)
                .drawBehind { drawGrip(p.onSun) },
        )
    }
}

/** The three grip lines on the knob. */
private fun DrawScope.drawGrip(color: Color) {
    val centerX = size.width / 2
    val centerY = size.height / 2
    val lineHeight = 20.dp.toPx()
    val lineWidth = 2.dp.toPx()
    val gap = 7.dp.toPx()
    for (dx in listOf(-gap, 0f, gap)) {
        drawRoundRect(
            color = color.copy(alpha = 0.6f),
            topLeft = Offset(centerX + dx - lineWidth / 2, centerY - lineHeight / 2),
            size = Size(lineWidth, lineHeight),
            cornerRadius = CornerRadius(lineWidth / 2),
        )
    }
}

/** The knob's first-visit nudge: left, then right, then back. */
private val NUDGE_SPEC = keyframes<Float> {
    durationMillis = 1200
    0f at 0
    -28f at 360
    28f at 840
    0f at 1200
}
