package io.github.giamat13.turnfall.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.toPath
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.giamat13.turnfall.GameViewModel
import io.github.giamat13.turnfall.Mode
import io.github.giamat13.turnfall.Sheet
import io.github.giamat13.turnfall.data.Speed
import io.github.giamat13.turnfall.data.ThemeChoice
import io.github.giamat13.turnfall.game.Chapters
import io.github.giamat13.turnfall.game.Tag
import io.github.giamat13.turnfall.game.dateKey
import io.github.giamat13.turnfall.game.dayLabel
import io.github.giamat13.turnfall.game.starText
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

/** Whether the screen is held sideways and short: the layout with two columns. */
@Composable
private fun isWideScreen(): Boolean {
    val config = LocalConfiguration.current
    return config.screenWidthDp > config.screenHeightDp && config.screenHeightDp <= 560
}

/**
 * A full-screen sheet with a title and a close button, as the page's `.sheet`. The content sits in a column that
 * is at most 460 dp wide (840 dp on a short phone held sideways).
 */
@Composable
fun SheetFrame(title: String, onClose: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val p = LocalPalette.current
    val maxWidth = if (isWideScreen()) 840.dp else 460.dp
    Box(
        Modifier
            .fillMaxSize()
            .background(p.paper)
            .padding(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 14.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            Modifier
                .widthIn(max = maxWidth)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GameText(
                    text = title,
                    style = TextStyle(
                        color = p.ink,
                        fontFamily = SecularFamily,
                        fontSize = 30.sp,
                        lineHeight = 32.sp,
                    ),
                )
                RoundIconButton("Close", IconClose, onClose)
            }
            content()
        }
    }
}

/** The levels screen: today's puzzle, the chapter with its six levels, and the chapter map. */
@Composable
fun LevelsSheet(vm: GameViewModel) {
    val p = LocalPalette.current
    val today = dateKey(LocalDate.now(ZoneOffset.UTC))
    val wide = isWideScreen()
    SheetFrame(title = "Levels", onClose = { vm.close(Sheet.LEVELS) }) {
        if (wide) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LevelsControls(vm, today)
                }
                Column(Modifier.weight(1.25f), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LevelsPath(vm, wide = true)
                    MapButton(vm)
                }
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                LevelsControls(vm, today)
                LevelsPath(vm, wide = false)
                MapButton(vm)
            }
        }
    }
}

/** The daily card, the chapter arrows, and the chapter's mechanics as small pictures. */
@Composable
private fun ColumnScope.LevelsControls(vm: GameViewModel, today: String) {
    val p = LocalPalette.current
    val dayStars = vm.progress.daily[today]?.s ?: 0
    val cardShape = RoundedCornerShape(20.dp)
    Chunky(
        modifier = Modifier.fillMaxWidth(),
        shape = cardShape,
        color = p.surface,
        onClick = { vm.loadDaily(today) },
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                GameText(
                    text = "Daily puzzle",
                    style = TextStyle(color = p.ink, fontFamily = SecularFamily, fontSize = 22.sp, lineHeight = 24.sp),
                )
                GameText(
                    text = dayLabel(today),
                    style = TextStyle(color = p.ink2, fontSize = 13.sp, lineHeight = 19.sp),
                )
            }
            GameText(
                text = starText(dayStars),
                style = TextStyle(color = p.ink, fontSize = 15.sp, letterSpacing = 1.sp, lineHeight = 18.sp),
            )
        }
    }
    val chapter = vm.sheetChapter
    val maxChapter = vm.progress.maxChapter(vm.level)
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Chunky(
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = "Previous chapter" },
            shape = CircleShape,
            color = p.surface,
            enabled = chapter > 0,
            pressFeedback = false,
            onClick = { vm.previousChapter() },
        ) {
            VectorIcon(IconChevronLeft, p.ink, Modifier.size(22.dp))
        }
        GameText(
            text = Chapters.title(chapter),
            modifier = Modifier.weight(1f),
            style = TextStyle(
                color = p.ink,
                fontFamily = SecularFamily,
                fontSize = 26.sp,
                lineHeight = 28.sp,
                textAlign = TextAlign.Center,
            ),
        )
        Chunky(
            modifier = Modifier
                .size(44.dp)
                .semantics { contentDescription = "Next chapter" },
            shape = CircleShape,
            color = p.surface,
            enabled = chapter < maxChapter,
            pressFeedback = false,
            onClick = { vm.nextChapter() },
        ) {
            VectorIcon(IconChevronRight, p.ink, Modifier.size(22.dp))
        }
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        for (tag in Chapters.info(chapter).tags) {
            TagChip(tag)
        }
    }
}

/** A mechanic of a chapter, as a small picture on a raised tile. */
@Composable
private fun TagChip(tag: Tag) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(10.dp)
    Box(
        Modifier
            .size(32.dp)
            .lowerEdge(shape, p.edge, 3.dp)
            .clip(shape)
            .background(p.surface),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(20.dp)) {
            val k = size.width / 24f
            withTransform({ scale(k, k, pivot = Offset.Zero) }) {
                when (tag) {
                    Tag.WALL -> drawRoundRect(
                        color = p.wall,
                        topLeft = Offset(4f, 4f),
                        size = Size(16f, 16f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f),
                    )
                    Tag.MUD -> {
                        drawCircle(p.mud, radius = 4.5f, center = Offset(9f, 10f))
                        drawCircle(p.mud, radius = 3.5f, center = Offset(16f, 15f))
                    }
                    Tag.PIT -> {
                        drawCircle(p.pit, radius = 8f, center = Offset(12f, 12f))
                        drawCircle(p.pitInk, radius = 3.5f, center = Offset(12f, 12f))
                    }
                    Tag.ARROW -> drawPath(addPathNodes("M12 20 4.5 9h15z").toPath(Path()), p.ink)
                    Tag.SPRING -> drawPath(
                        addPathNodes("M3 12l3-4 3 8 3-8 3 8 3-8 3 4").toPath(Path()),
                        p.ink,
                        style = Stroke(width = 2.2f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}

/** Where the six levels of a chapter sit on the path, in percent of the box: along the top, then back along the bottom. */
private val PATH_XY = listOf(18f to 26f, 50f to 26f, 82f to 26f, 82f to 74f, 50f to 74f, 18f to 74f)

/** The six levels of the chapter on a winding path. The current level pulses. */
@Composable
private fun ColumnScope.LevelsPath(vm: GameViewModel, wide: Boolean) {
    val p = LocalPalette.current
    val config = LocalConfiguration.current
    val pathHeight = if (wide) 200.dp else (config.screenHeightDp * 0.40f).coerceIn(230f, 330f).dp
    BoxWithConstraints(Modifier
        .fillMaxWidth()
        .height(pathHeight)) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val radiusPx = with(density) { 29.dp.toPx() }
        Canvas(Modifier.matchParentSize()) {
            val path = Path()
            PATH_XY.forEachIndexed { i, (x, y) ->
                val px = x / 100f * size.width
                val py = y / 100f * size.height
                if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
            }
            drawPath(
                path,
                p.ink2.copy(alpha = 0.35f),
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
        PATH_XY.forEachIndexed { k, (x, y) ->
            val index = vm.sheetChapter * Chapters.SIZE + k
            val best = vm.progress.best[index.toString()]
            val stars = best?.s ?: 0
            val current = vm.mode == Mode.CAMPAIGN && index == vm.level
            LevelButton(
                number = index + 1,
                stars = stars,
                current = current,
                modifier = Modifier.offset {
                    IntOffset(
                        (x / 100f * widthPx - radiusPx).roundToInt(),
                        (y / 100f * heightPx - radiusPx).roundToInt(),
                    )
                },
                onClick = { vm.loadLevel(index) },
            )
        }
    }
}

/** A level: a round button with its number, and its best stars under it. */
@Composable
private fun LevelButton(
    number: Int,
    stars: Int,
    current: Boolean,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val p = LocalPalette.current
    val done = stars > 0
    val transition = rememberInfiniteTransition(label = "level pulse")
    val breathing = transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 800), RepeatMode.Reverse),
        label = "pulse",
    )
    val pulse = if (current) breathing.value else 1f
    Box(modifier.size(58.dp)) {
        Chunky(
            modifier = Modifier
                .size(58.dp)
                .graphicsLayer {
                    scaleX = pulse
                    scaleY = pulse
                }
                .drawBehind {
                    // The white ring of the current level, the page's second box-shadow.
                    if (current) {
                        drawCircle(p.surface, radius = size.minDimension / 2 + 5.dp.toPx())
                    }
                }
                .semantics {
                    contentDescription = buildString {
                        append("Level $number")
                        if (done) append(", $stars of 3 stars")
                    }
                },
            shape = CircleShape,
            color = when {
                done -> p.wall
                current -> p.sun
                else -> p.surface
            },
            pressFeedback = false,
            onClick = onClick,
        ) {
            GameText(
                text = number.toString(),
                style = TextStyle(
                    color = when {
                        done -> p.onInk
                        current -> p.onSun
                        else -> p.ink
                    },
                    fontFamily = SecularFamily,
                    fontSize = 24.sp,
                    lineHeight = 24.sp,
                ),
            )
        }
        GameText(
            text = starText(stars),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 58.dp + 5.dp)
                .wrapContentWidth(unbounded = true),
            style = TextStyle(
                color = if (done) p.ink else p.ink2,
                fontSize = 11.sp,
                letterSpacing = 1.sp,
                lineHeight = 11.sp,
            ),
        )
    }
}

/** The chapter map button, enabled once every level of the chapter is finished. */
@Composable
private fun MapButton(vm: GameViewModel) {
    val p = LocalPalette.current
    Chunky(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(18.dp),
        color = p.surface,
        enabled = vm.progress.isChapterDone(vm.sheetChapter),
        onClick = { vm.openMap(vm.sheetChapter, mapColorsOf(p)) },
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            VectorIcon(IconMap, p.ink, Modifier.size(22.dp))
            GameText(
                text = "Map",
                style = TextStyle(color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp),
            )
        }
    }
}

/** The settings screen: speed, theme and feedback, then the figures and the reset. */
@Composable
fun SettingsSheet(vm: GameViewModel) {
    val p = LocalPalette.current
    val wide = isWideScreen()
    val settings = vm.settings
    SheetFrame(title = "Settings", onClose = { vm.close(Sheet.SETTINGS) }) {
        val choices: @Composable ColumnScope.() -> Unit = {
            SettingField("Speed") {
                SegButton("Normal", settings.speed == Speed.NORMAL) { vm.setSpeed(Speed.NORMAL) }
                SegButton("Fast", settings.speed == Speed.FAST) { vm.setSpeed(Speed.FAST) }
                SegButton("Off", settings.speed == Speed.OFF) { vm.setSpeed(Speed.OFF) }
            }
            SettingField("Theme") {
                SegButton("Auto", settings.theme == ThemeChoice.AUTO) { vm.setTheme(ThemeChoice.AUTO) }
                SegButton("Light", settings.theme == ThemeChoice.LIGHT) { vm.setTheme(ThemeChoice.LIGHT) }
                SegButton("Dark", settings.theme == ThemeChoice.DARK) { vm.setTheme(ThemeChoice.DARK) }
            }
            SettingField("Feedback") {
                SegButton("Sound", settings.sound) { vm.toggleSound() }
                SegButton("Vibration", settings.vibe) { vm.toggleVibe() }
            }
        }
        val figures: @Composable ColumnScope.() -> Unit = {
            StatsGrid(vm)
            ResetButton(vm)
        }
        if (wide) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp), content = choices)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(14.dp), content = figures)
            }
        } else {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                choices()
                figures()
            }
        }
    }
}

@Composable
private fun SettingField(label: String, choices: @Composable RowScope.() -> Unit) {
    val p = LocalPalette.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        GameText(
            text = label,
            style = TextStyle(color = p.ink, fontSize = 15.sp, fontWeight = FontWeight.Bold, lineHeight = 17.sp),
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), content = choices)
    }
}

@Composable
private fun RowScope.SegButton(label: String, selected: Boolean, onClick: () -> Unit) {
    val p = LocalPalette.current
    Chunky(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 48.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) p.sun else p.surface,
        pressFeedback = false,
        onClick = onClick,
    ) {
        GameText(
            text = label,
            style = TextStyle(
                color = if (selected) p.onSun else p.ink,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/** The figures from this phone's progress, two to a row; an odd last figure takes the whole row. */
@Composable
private fun StatsGrid(vm: GameViewModel) {
    val stats = vm.progress.stats(LocalDate.now(ZoneOffset.UTC))
    val cells = listOf(
        "Solved" to stats.solved,
        "Three stars" to stats.threeStars,
        "Stars" to stats.stars,
        "Daily puzzles" to stats.dailyPuzzles,
        "Streak" to stats.streak,
    )
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        cells.chunked(2).forEach { row ->
            if (row.size == 2) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    for ((label, value) in row) {
                        StatCell(label, value, Modifier.weight(1f))
                    }
                }
            } else {
                StatCell(row[0].first, row[0].second, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: Int, modifier: Modifier) {
    val p = LocalPalette.current
    val shape = RoundedCornerShape(16.dp)
    Column(
        modifier
            .lowerEdge(shape, p.edge, 3.dp)
            .clip(shape)
            .background(p.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        GameText(
            text = label,
            style = TextStyle(color = p.ink2, fontSize = 12.sp, lineHeight = 16.sp),
        )
        GameText(
            text = value.toString(),
            style = TextStyle(color = p.ink, fontFamily = SecularFamily, fontSize = 24.sp, lineHeight = 26.sp),
        )
    }
}

/** Reset: the first tap arms it for four seconds and says "Sure?", the second tap clears the progress. */
@Composable
private fun ResetButton(vm: GameViewModel) {
    val p = LocalPalette.current
    val armed = vm.resetArmed
    Chunky(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp),
        shape = RoundedCornerShape(18.dp),
        color = if (armed) p.vermilion else p.surface,
        onClick = { vm.resetTapped() },
    ) {
        GameText(
            text = if (armed) "Sure?" else "Reset",
            style = TextStyle(
                color = if (armed) Color.White else p.vermilion,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center,
            ),
        )
    }
}

/** The chapter map as a picture: long-press to save it to the gallery. */
@Composable
fun MapSheet(vm: GameViewModel) {
    val p = LocalPalette.current
    val config = LocalConfiguration.current
    SheetFrame(title = Chapters.title(vm.mapChapter), onClose = { vm.close(Sheet.MAP) }) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                val side = minOf(maxWidth, (config.screenHeightDp * 0.56f).dp, 420.dp)
                Box(
                    Modifier
                        .size(side)
                        .clip(RoundedCornerShape(22.dp))
                        .background(p.surface)
                        .combinedClickable(onClick = {}, onLongClick = { vm.saveMap() }),
                ) {
                    vm.mapImage?.let { image ->
                        Image(
                            bitmap = image,
                            contentDescription = "Map of the finished chapter",
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
            GameText(
                text = vm.mapNote,
                style = TextStyle(
                    color = p.ink2,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 24.sp,
                    textAlign = TextAlign.Center,
                ),
            )
        }
    }
}
