package io.github.giamat13.turnfall.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Typeface
import androidx.core.content.res.ResourcesCompat
import io.github.giamat13.turnfall.R
import io.github.giamat13.turnfall.data.Progress
import io.github.giamat13.turnfall.game.Chapters
import io.github.giamat13.turnfall.game.starText

/** The colours of the chapter map as ARGB values, taken from the palette of the screen that asks for it. */
class MapColors(
    val paper: Int,
    val ink: Int,
    val ink2: Int,
    val tile: Int,
    val sun: Int,
    val vermilion: Int,
)

private const val MAP_SIZE = 720

/** The six positions of a chapter's levels on the map, as fractions of the picture. */
private val MAP_POINTS = listOf(
    0.22f to 0.38f,
    0.5f to 0.34f,
    0.78f to 0.42f,
    0.62f to 0.6f,
    0.3f to 0.6f,
    0.5f to 0.76f,
)

/**
 * The chapter map as a picture, drawn as the page draws it on its canvas: the chapter's name, its stars so far, the six
 * levels on a path with their stars, and the game's name. Returns a 720 by 720 pixel bitmap.
 */
fun renderChapterMap(context: Context, chapter: Int, progress: Progress, colors: MapColors): Bitmap {
    val s = MAP_SIZE.toFloat()
    val bitmap = Bitmap.createBitmap(MAP_SIZE, MAP_SIZE, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val display = ResourcesCompat.getFont(context, R.font.secular_one)
    val text = ResourcesCompat.getFont(context, R.font.assistant_variable)

    canvas.drawColor(colors.paper)
    val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 8f
        color = colors.ink
    }
    canvas.drawRect(14f, 14f, s - 14f, s - 14f, frame)

    drawText(canvas, Chapters.info(chapter).name, s / 2, 96f, 60f, display, 400, colors.ink)
    var got = 0
    for (k in 0 until Chapters.SIZE) {
        got += progress.best[(chapter * Chapters.SIZE + k).toString()]?.s ?: 0
    }
    val prefix = if (chapter < Chapters.designed.size) "Chapter ${chapter + 1} · " else ""
    drawText(canvas, "$prefix$got of ${Chapters.MAX_STARS_PER_CHAPTER} stars", s / 2, 158f, 28f, text, 600, colors.ink2)

    val points = MAP_POINTS.map { (x, y) -> x * s to y * s }
    val line = Path()
    points.forEachIndexed { i, (x, y) ->
        if (i == 0) line.moveTo(x, y) else line.lineTo(x, y)
    }
    val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 10f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        color = colors.ink
    }
    canvas.drawPath(line, linePaint)

    points.forEachIndexed { k, (x, y) ->
        val stars = progress.best[(chapter * Chapters.SIZE + k).toString()]?.s ?: 0
        val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = when {
                stars == 3 -> colors.sun
                stars > 0 -> colors.tile
                else -> colors.paper
            }
        }
        canvas.drawCircle(x, y, 50f, fill)
        val ring = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = colors.ink
            if (stars == 0) pathEffect = DashPathEffect(floatArrayOf(10f, 9f), 0f)
        }
        canvas.drawCircle(x, y, 50f, ring)
        drawText(canvas, (k + 1).toString(), x, y + 2, 40f, display, 400, colors.ink)
        drawText(canvas, starText(stars), x, y + 80, 26f, text, 700, colors.vermilion)
    }

    drawText(canvas, "Turnfall", s / 2, s - 40, 30f, display, 400, colors.vermilion)
    return bitmap
}

/**
 * Text centred on [x] and on the middle of its line at [y], the way the page's canvas puts it ('middle' baseline).
 */
private fun drawText(
    canvas: Canvas,
    value: String,
    x: Float,
    y: Float,
    size: Float,
    typeface: Typeface?,
    weight: Int,
    color: Int,
) {
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        this.color = color
        textSize = size
        textAlign = Paint.Align.CENTER
        this.typeface = typeface ?: Typeface.DEFAULT
        fontVariationSettings = "'wght' $weight"
    }
    val metrics = paint.fontMetrics
    val baseline = y - (metrics.ascent + metrics.descent) / 2f
    canvas.drawText(value, x, baseline, paint)
}
