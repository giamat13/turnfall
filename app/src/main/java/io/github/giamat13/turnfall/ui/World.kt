package io.github.giamat13.turnfall.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.giamat13.turnfall.BallState
import io.github.giamat13.turnfall.Fx
import io.github.giamat13.turnfall.GameViewModel
import io.github.giamat13.turnfall.game.Board
import io.github.giamat13.turnfall.game.Cell
import io.github.giamat13.turnfall.game.DX
import io.github.giamat13.turnfall.game.DY
import io.github.giamat13.turnfall.game.End
import kotlin.math.floor
import kotlin.math.min

/** The page's ease for the rotation of the world, and the ease of the ball's size and fade. */
private val TURN_EASE = CubicBezierEasing(0.45f, 0.05f, 0.25f, 1f)
private val BALL_EASE = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1f)

/** The board shakes after a move that goes nowhere, or a fall into a pit. */
private val SHAKE_SPEC = keyframes<Float> {
    durationMillis = 320
    0f at 0
    -5f at 80
    5f at 240
    0f at 320
}

/** The turns counter swells when a turn is made. */
internal val BUMP_SPEC = keyframes<Float> {
    durationMillis = 320
    1f at 0
    1.3f at 128
    1f at 320
}

/**
 * The board: the world on a square card with a lower edge. The world is a stage that turns with the gravity, and the
 * ball, the trail and the preview are drawn inside it, so they turn with it.
 */
@Composable
fun BoardCard(vm: GameViewModel, side: Dp, modifier: Modifier = Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    val shake = remember { Animatable(0f) }
    LaunchedEffect(vm.shakeTick) {
        if (vm.shakeTick > 0) {
            shake.snapTo(0f)
            shake.animateTo(0f, SHAKE_SPEC)
        }
    }
    Box(
        modifier = modifier
            .offset(x = shake.value.dp)
            .size(side)
            .lowerEdge(shape, p.edge, 6.dp)
            .clip(shape)
            .background(p.surface)
            .padding(10.dp),
    ) {
        WorldCanvas(vm, Modifier.fillMaxSize())
    }
}

@Composable
private fun WorldCanvas(vm: GameViewModel, modifier: Modifier) {
    val p = LocalPalette.current
    val ballScale by animateFloatAsState(
        targetValue = when (vm.ballState) {
            BallState.REST -> 1f
            BallState.MUD -> 0.8f
            BallState.DROP -> 0.25f
            BallState.SINK -> 0f
        },
        animationSpec = tween(durationMillis = 300, easing = BALL_EASE),
        label = "ballScale",
    )
    val ballAlpha by animateFloatAsState(
        targetValue = if (vm.ballState == BallState.DROP) 0.25f else 1f,
        animationSpec = tween(durationMillis = 300, easing = BALL_EASE),
        label = "ballAlpha",
    )
    // 0 right after a landing, 1 when the squash is over (the ball is round).
    val squash = remember { Animatable(1f) }
    LaunchedEffect(vm.landTick) {
        if (vm.landTick > 0) {
            squash.snapTo(0f)
            squash.animateTo(1f, tween(durationMillis = 380))
        }
    }
    Canvas(modifier.graphicsLayer { rotationZ = vm.rotation.value }) {
        val board = vm.board
        val u = size.minDimension / board.n
        drawWorld(board, u, p, if (vm.ringOn) vm.ring.value else null)
        vm.fx?.let { drawPreview(it, u, p) }
        drawBall(vm, u, p, ballScale, ballAlpha, squash.value)
    }
}

/** The world: tiles, walls, mud, pits, arrows, springs and the hole with its goal ring. */
private fun DrawScope.drawWorld(board: Board, u: Float, p: Palette, pulse: Float?) {
    for (y in 0 until board.n) {
        for (x in 0 until board.n) {
            val c = board.cell(x, y)
            if (c == Cell.WALL) {
                roundCell(p.wallLip, u, x + 0.04f, y + 0.1f, 0.92f, 0.9f, 0.14f)
                roundCell(p.wall, u, x + 0.04f, y + 0.03f, 0.92f, 0.9f, 0.14f)
                continue
            }
            val tile = when (c) {
                Cell.MUD -> p.mud
                Cell.PIT -> p.pit
                else -> p.tile
            }
            roundCell(tile, u, x + 0.05f, y + 0.05f, 0.9f, 0.9f, 0.14f)
            if (c == Cell.MUD) {
                val spot = p.mudInk.copy(alpha = 0.45f)
                dot(spot, u, x + 0.3f, y + 0.36f, 0.05f)
                dot(spot, u, x + 0.66f, y + 0.6f, 0.04f)
                dot(spot, u, x + 0.44f, y + 0.74f, 0.035f)
            }
            if (Cell.isArrow(c)) drawArrow(x, y, c - Cell.ARROW, u, p)
            if (c == Cell.SPRING) drawSpring(x, y, u, p)
            if (c == Cell.PIT) dot(p.pitInk, u, x + 0.5f, y + 0.5f, 0.26f)
            if (c == Cell.HOLE) {
                dot(p.hole, u, x + 0.5f, y + 0.5f, 0.3f)
                drawGoal(x, y, u, p, pulse)
            }
        }
    }
}

/** The goal ring. While it pulses (after a win) it grows and fades, instead of standing still. */
private fun DrawScope.drawGoal(x: Int, y: Int, u: Float, p: Palette, pulse: Float?) {
    val center = Offset((x + 0.5f) * u, (y + 0.5f) * u)
    if (pulse == null) {
        drawCircle(p.vermilion, radius = 0.38f * u, center = center, style = Stroke(width = 0.07f * u))
        return
    }
    val grow = 1f + 0.9f * pulse
    drawCircle(
        p.vermilion.copy(alpha = 1f - pulse),
        radius = 0.38f * grow * u,
        center = center,
        style = Stroke(width = 0.07f * grow * u),
    )
}

/** The path the ball would take, and the cell where it would stop. */
private fun DrawScope.drawPreview(fx: Fx, u: Float, p: Palette) {
    if (fx.trail.size > 1) {
        val path = Path()
        fx.trail.forEachIndexed { i, q ->
            val px = (q.x + 0.5f) * u
            val py = (q.y + 0.5f) * u
            if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
        }
        drawPath(
            path,
            p.ink.copy(alpha = 0.55f),
            style = Stroke(
                width = 0.08f * u,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.14f * u, 0.14f * u)),
            ),
        )
    }
    val center = Offset((fx.x + 0.5f) * u, (fx.y + 0.5f) * u)
    val radius = 0.3f * u
    val dashed = Stroke(width = 0.05f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(0.1f * u, 0.08f * u)))
    val solid = Stroke(width = 0.05f * u)
    when (fx.end) {
        End.WIN -> {
            drawCircle(p.sun, radius, center)
            drawCircle(p.ink, radius, center, style = solid)
        }
        End.MUD -> {
            drawCircle(p.mud, radius, center)
            drawCircle(p.mudInk, radius, center, style = solid)
        }
        End.PIT -> {
            drawCircle(p.vermilion.copy(alpha = 0.7f), radius, center)
            drawCircle(p.vermilion, radius, center, style = dashed)
        }
        End.WALL -> {
            drawCircle(p.ink.copy(alpha = 0.16f), radius, center)
            drawCircle(p.ink, radius, center, style = dashed)
        }
    }
}

/**
 * The ball, drawn where it is: on its cell, or along its path while it falls. Its size and fade follow its state,
 * and the squash of a landing is drawn on top.
 */
private fun DrawScope.drawBall(
    vm: GameViewModel,
    u: Float,
    p: Palette,
    ballScale: Float,
    ballAlpha: Float,
    squashT: Float,
) {
    val path = vm.walkPath
    val center = if (path != null && path.size > 1) {
        val steps = path.size - 1
        val s = vm.walkProgress.value * steps
        val i = min(steps - 1, floor(s).toInt())
        val q = s - i
        val a = path[i]
        val b = path[i + 1]
        Offset((a.x + (b.x - a.x) * q + 0.5f) * u, (a.y + (b.y - a.y) * q + 0.5f) * u)
    } else {
        Offset((vm.pos.x + 0.5f) * u, (vm.pos.y + 0.5f) * u)
    }
    val sx = ballScale * squashX(squashT)
    val sy = ballScale * squashY(squashT)
    withTransform({ scale(sx, sy, pivot = center) }) {
        drawCircle(p.sun.copy(alpha = ballAlpha), radius = 0.3f * u, center = center)
        drawCircle(p.sunEdge.copy(alpha = ballAlpha), radius = 0.3f * u, center = center, style = Stroke(width = 0.05f * u))
        drawCircle(
            Color.White.copy(alpha = 0.8f * ballAlpha),
            radius = 0.07f * u,
            center = Offset(center.x - 0.1f * u, center.y - 0.1f * u),
        )
    }
}

/** The squash of a landing: wider and shorter at first, then round. [t] runs from 0 to 1. */
private fun squashX(t: Float): Float =
    if (t < 0.45f) lerp(1.2f, 0.94f, t / 0.45f) else lerp(0.94f, 1f, (t - 0.45f) / 0.55f)

private fun squashY(t: Float): Float =
    if (t < 0.45f) lerp(0.8f, 1.06f, t / 0.45f) else lerp(1.06f, 1f, (t - 0.45f) / 0.55f)

private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

private fun DrawScope.roundCell(color: Color, u: Float, x: Float, y: Float, w: Float, h: Float, r: Float) {
    drawRoundRect(
        color = color,
        topLeft = Offset(x * u, y * u),
        size = Size(w * u, h * u),
        cornerRadius = CornerRadius(r * u),
    )
}

private fun DrawScope.dot(color: Color, u: Float, x: Float, y: Float, r: Float) {
    drawCircle(color, radius = r * u, center = Offset(x * u, y * u))
}

/** An arrow: a triangle that points where a falling ball is sent. */
private fun DrawScope.drawArrow(x: Int, y: Int, d: Int, u: Float, p: Palette) {
    val cx = x + 0.5f
    val cy = y + 0.5f
    val dx = DX[d].toFloat()
    val dy = DY[d].toFloat()
    val px = -dy
    val py = dx
    val path = Path().apply {
        moveTo((cx + 0.3f * dx) * u, (cy + 0.3f * dy) * u)
        lineTo((cx - 0.12f * dx + 0.2f * px) * u, (cy - 0.12f * dy + 0.2f * py) * u)
        lineTo((cx - 0.12f * dx - 0.2f * px) * u, (cy - 0.12f * dy - 0.2f * py) * u)
        close()
    }
    drawPath(path, p.ink.copy(alpha = 0.75f))
}

/** A spring: a zigzag that sends the ball back. */
private fun DrawScope.drawSpring(x: Int, y: Int, u: Float, p: Palette) {
    val path = Path()
    for (k in 0..6) {
        val dy = if (k == 0 || k == 6) 0f else if (k % 2 == 1) -0.14f else 0.14f
        val px = (x + 0.2f + k * 0.1f) * u
        val py = (y + 0.5f + dy) * u
        if (k == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    drawPath(path, p.ink, style = Stroke(width = 0.07f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
}
