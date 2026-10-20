package io.github.giamat13.turnfall.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp

/**
 * Text laid out the way the page lays it out: a line is exactly its line height, with the spare space split evenly
 * above and below the glyphs. Compose adds font padding by default, which would make every line taller than the
 * page's, so this turns that off.
 */
@Composable
fun GameText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style.merge(
            TextStyle(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
                lineHeightStyle = LineHeightStyle(LineHeightStyle.Alignment.Center, LineHeightStyle.Trim.None),
            ),
        ),
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * A box exactly one line tall. Text inside it is measured at its own height and centred on the line, which is what
 * the page does with its line boxes. Use the text with `Modifier.wrapContentHeight(unbounded = true)`.
 */
@Composable
fun LineBox(lineHeight: Dp, modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    Box(
        modifier.fillMaxWidth().height(lineHeight),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
