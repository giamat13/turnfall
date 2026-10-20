package io.github.giamat13.turnfall.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A raised surface with a lower edge, the page's `box-shadow: 0 4px 0 edge`. Pressing it pushes the face down into the
 * edge, the way the page's `:active` rule does, when [pressFeedback] is on (the page only does this for `.btn`).
 * The edge does not change the layout size.
 */
@Composable
fun Chunky(
    modifier: Modifier = Modifier,
    shape: Shape,
    color: Color,
    edge: Color = LocalPalette.current.edge,
    edgeHeight: Dp = 4.dp,
    enabled: Boolean = true,
    pressFeedback: Boolean = true,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pressing = pressed && enabled && pressFeedback && onClick != null
    val lift = if (pressing) 3.dp else 0.dp
    val depth = if (pressing) 1.dp else edgeHeight
    val clickable = if (onClick != null && enabled) {
        Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
    } else {
        Modifier
    }
    Box(
        modifier = modifier
            .graphicsLayer { alpha = if (enabled) 1f else 0.4f }
            .offset(y = lift)
            .lowerEdge(shape, edge, depth)
            .background(color, shape)
            .then(clickable),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

/**
 * A surface with the page's lower edge: the same shape, [height] lower, drawn behind the face. Used by the cards,
 * the board and the buttons.
 */
fun Modifier.lowerEdge(shape: Shape, edge: Color, height: Dp = 4.dp): Modifier = this.drawBehind {
    drawShapeAt(shape, edge, height.toPx())
}

/** Draws [shape] filled with [color], moved [dy] pixels down. The shape comes from the outline the shape gives. */
internal fun DrawScope.drawShapeAt(shape: Shape, color: Color, dy: Float) {
    when (val outline = shape.createOutline(size, layoutDirection, this)) {
        is Outline.Rectangle -> translate(top = dy) {
            drawRect(color, topLeft = Offset(outline.rect.left, outline.rect.top), size = Size(outline.rect.width, outline.rect.height))
        }
        is Outline.Rounded -> {
            val rr = outline.roundRect
            translate(top = dy) {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(rr.left, rr.top),
                    size = Size(rr.width, rr.height),
                    cornerRadius = CornerRadius(rr.topLeftCornerRadius.x, rr.topLeftCornerRadius.y),
                )
            }
        }
        is Outline.Generic -> translate(top = dy) {
            drawPath(outline.path, color)
        }
    }
}
