package io.github.giamat13.turnfall

import android.app.Application
import android.content.Intent
import android.view.Choreographer
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.MonotonicFrameClock
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.giamat13.turnfall.data.Progress
import io.github.giamat13.turnfall.data.Settings
import io.github.giamat13.turnfall.data.Speed
import io.github.giamat13.turnfall.data.Store
import io.github.giamat13.turnfall.data.ThemeChoice
import io.github.giamat13.turnfall.feedback.Buzzer
import io.github.giamat13.turnfall.feedback.Tone
import io.github.giamat13.turnfall.feedback.Tones
import io.github.giamat13.turnfall.feedback.Wave
import io.github.giamat13.turnfall.feedback.saveToGallery
import io.github.giamat13.turnfall.game.Board
import io.github.giamat13.turnfall.game.Cell
import io.github.giamat13.turnfall.game.Chapters
import io.github.giamat13.turnfall.game.End
import io.github.giamat13.turnfall.game.Level
import io.github.giamat13.turnfall.game.LevelBank
import io.github.giamat13.turnfall.game.Pos
import io.github.giamat13.turnfall.game.Slide
import io.github.giamat13.turnfall.game.dateKey
import io.github.giamat13.turnfall.game.dayLabel
import io.github.giamat13.turnfall.game.hint
import io.github.giamat13.turnfall.game.parseBoard
import io.github.giamat13.turnfall.game.slide
import io.github.giamat13.turnfall.game.stars
import io.github.giamat13.turnfall.game.starText
import io.github.giamat13.turnfall.game.turnWord
import io.github.giamat13.turnfall.ui.MapColors
import io.github.giamat13.turnfall.ui.renderChapterMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.roundToInt

/** The menu is the start. The play screen is the game. */
enum class Screen { MENU, PLAY }

/** Campaign levels, or the daily puzzle of one date. */
enum class Mode { CAMPAIGN, DAILY }

/** The screens that cover the menu or the play screen. They stack: the last one opened is on top. */
enum class Sheet { LEVELS, SETTINGS, MAP }

/** The ball's look: at rest, sunk in mud, dropped into a pit, or sunk into the hole. */
enum class BallState { REST, MUD, DROP, SINK }

/** One state the player can go back to with Undo. */
class Attempt(val pos: Pos, val gravity: Int, val angle: Int, val turns: Int)

/** The path of a preview, and where the ball would stop. [trail] is empty when the ball would not move. */
class Fx(val trail: List<Pos>, val end: End, val x: Int, val y: Int)

/**
 * The game, as the page's script runs it: a turn rotates the world, then the ball falls and walks its path, and the
 * result is recorded. The timings and messages are the page's. The menu is the Android addition at the start.
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val store = Store(application)
    private val tones = Tones { settings.sound }
    private val buzzer = Buzzer(application) { settings.vibe }
    private val bank = LevelBank.fromJson(
        application.assets.open("levels.json").bufferedReader().use { it.readText() },
    )

    var progress by mutableStateOf(store.loadProgress())
        private set
    var settings by mutableStateOf(store.loadSettings())
        private set

    /** The screen on show. The app starts on the menu. */
    var screen by mutableStateOf(Screen.MENU)
        private set

    var mode by mutableStateOf(Mode.CAMPAIGN)
        private set
    var level by mutableIntStateOf(0)
        private set
    var dayKey by mutableStateOf("")
        private set
    var lv: Level by mutableStateOf(bank.levelAt(0))
        private set
    var board: Board by mutableStateOf(parseBoard(lv.rows))
        private set
    var kicker by mutableStateOf("")
        private set

    var pos by mutableStateOf(Pos(0, 0))
        private set
    var gravity by mutableIntStateOf(0)
        private set

    /** The total turn of the world in degrees: the page's `th`. */
    var angle by mutableIntStateOf(0)
        private set
    var turns by mutableIntStateOf(0)
        private set
    private val hist = mutableStateListOf<Attempt>()
    var undoLeft by mutableIntStateOf(2)
        private set
    var hintsLeft by mutableIntStateOf(3)
        private set
    var busy by mutableStateOf(false)
        private set
    var won by mutableStateOf(false)
        private set
    var failed by mutableStateOf(false)
        private set
    var message by mutableStateOf("")
        private set
    var fx by mutableStateOf<Fx?>(null)
        private set
    var ballState by mutableStateOf(BallState.REST)
        private set

    /** On the first visit the joystick knob nudges, so the player finds out that it moves. */
    var teach by mutableStateOf(false)
        private set

    /** -1 when the handle is pulled left past half way, 1 for right, 0 otherwise. */
    var joyPull by mutableIntStateOf(0)
        private set

    /** The world's rotation in degrees, animated. The dial's needle turns with it. */
    val rotation = Animatable(0f)

    /** Progress along the ball's path while it falls, from 0 to 1. */
    val walkProgress = Animatable(0f)

    /** The cells of the ball's path while it falls; null when the ball is not moving. */
    var walkPath by mutableStateOf<List<Pos>?>(null)
        private set

    /** Progress of the goal's ring pulse, from 0 to 1, shown while [ringOn]. */
    val ring = Animatable(0f)
    var ringOn by mutableStateOf(false)
        private set

    /** Each change starts an animation: the turns counter bumps, the board shakes, the ball lands. */
    var bumpTick by mutableIntStateOf(0)
        private set
    var shakeTick by mutableIntStateOf(0)
        private set
    var landTick by mutableIntStateOf(0)
        private set

    var winShown by mutableStateOf(false)
        private set
    var winStars by mutableStateOf("")
        private set
    var winTitle by mutableStateOf("")
        private set
    var winNext by mutableStateOf("Next")
        private set
    var winMapShown by mutableStateOf(false)
        private set
    var winConfetti by mutableStateOf(false)
        private set

    private var lastResult = ""

    val sheets = mutableStateListOf<Sheet>()
    var sheetChapter by mutableIntStateOf(0)
        private set
    var mapChapter by mutableIntStateOf(0)
        private set
    var mapImage by mutableStateOf<ImageBitmap?>(null)
        private set
    var mapNote by mutableStateOf(MAP_HINT)
        private set
    private var mapBitmap: android.graphics.Bitmap? = null

    var resetArmed by mutableStateOf(false)
        private set
    private var resetJob: Job? = null
    private var joyDir = 0

    fun canUndo(): Boolean = !(busy || won || undoLeft <= 0 || hist.isEmpty())

    fun canHint(): Boolean = !(busy || won || failed || hintsLeft <= 0)

    /** The number of the level that Play goes to, for the menu. */
    fun nextLevelNumber(): Int = firstUnfinished() + 1

    private fun turnMillis(): Long = Math.round(TURN_MS * settings.speed.factor).toLong()

    private fun restBall(): BallState = if (board.cell(pos.x, pos.y) == Cell.MUD) BallState.MUD else BallState.REST

    private fun restMessage(): String = if (failed) PIT_MESSAGE else ""

    private fun loadBoard(l: Level) {
        lv = l
        board = parseBoard(l.rows)
    }

    /** A fresh attempt: the ball settles under gravity, and the undo and hint allowances are back. */
    private fun startAttempt() {
        val start = checkNotNull(board.start)
        val s0 = slide(board, start.x, start.y, 0, keepPath = false)
        pos = Pos(s0.x, s0.y)
        gravity = 0
        angle = 0
        turns = 0
        hist.clear()
        undoLeft = 2
        hintsLeft = 3
        busy = false
        won = false
        failed = false
        fx = null
        walkPath = null
        viewModelScope.launch { rotation.snapTo(0f) }
        ballState = restBall()
        message = restMessage()
    }

    /** Prepares a campaign level without showing it. */
    private fun prepareLevel(index: Int) {
        mode = Mode.CAMPAIGN
        level = index
        loadBoard(bank.levelAt(index))
        startAttempt()
        kicker = "Level ${index + 1}"
    }

    /** Shows a campaign level on the play screen. */
    fun loadLevel(index: Int) {
        prepareLevel(index)
        screen = Screen.PLAY
        sheets.clear()
        winShown = false
    }

    /** Shows the daily puzzle of [key] on the play screen. */
    fun loadDaily(key: String) {
        mode = Mode.DAILY
        dayKey = key
        loadBoard(bank.dailyFor(key))
        startAttempt()
        kicker = "Daily ${dayLabel(key)}"
        screen = Screen.PLAY
        sheets.clear()
        winShown = false
    }

    /** The menu's Play: the first campaign level that is not finished yet. */
    fun playFromMenu() = loadLevel(firstUnfinished())

    /** Back to the menu. The attempt is left as it is; the sheets and the win card are closed. */
    fun showMenu() {
        sheets.clear()
        winShown = false
        screen = Screen.MENU
    }

    fun restart() {
        if (busy) return
        if (mode == Mode.DAILY) loadDaily(dayKey) else loadLevel(level)
    }

    /** Today's puzzle, the same for everyone on this UTC date. */
    fun playToday() = loadDaily(todayKey())

    /** A turn: the world rotates (dir 1 is right, clockwise; -1 is left), then the ball falls. */
    fun turn(dir: Int) {
        if (busy || won || failed) return
        clearFx()
        hist.add(Attempt(pos, gravity, angle, turns))
        turns += 1
        bumpTick++
        teach = false
        buzzer.buzz(8)
        angle += 90 * dir
        gravity = (gravity + dir + 4) % 4
        busy = true
        ballState = BallState.REST
        animateAngle()
        message = ""
        tones.play(Tone(330.0, 0.08, Wave.TRIANGLE, 0.04))
        viewModelScope.launch {
            delay(turnMillis())
            fall()
        }
    }

    private fun animateAngle() {
        viewModelScope.launch(ViewFrames) {
            rotation.animateTo(angle.toFloat(), tween(durationMillis = turnMillis().toInt(), easing = TURN_EASING))
        }
    }

    private fun fall() {
        val r = slide(board, pos.x, pos.y, gravity, keepPath = true)
        if (!r.moved) {
            busy = false
            shakeTick++
            message = "No movement"
            return
        }
        walkBall(r)
    }

    /** The ball moves cell by cell along the path, at the speed of the setting. At Off it jumps. */
    private fun walkBall(r: Slide) {
        val factor = settings.speed.factor
        val path = listOf(pos) + r.path
        viewModelScope.launch(ViewFrames) {
            if (factor > 0f) {
                walkPath = path
                walkProgress.snapTo(0f)
                walkProgress.animateTo(
                    1f,
                    tween(durationMillis = (STEP_MS * factor * r.path.size).roundToInt(), easing = LinearEasing),
                )
                walkPath = null
            }
            arrive(r)
        }
    }

    private fun arrive(r: Slide) {
        pos = Pos(r.x, r.y)
        when (r.end) {
            End.WIN -> finish()
            End.PIT -> {
                failed = true
                busy = false
                tones.play(Tone(90.0, 0.35, Wave.SAWTOOTH, 0.035))
                buzzer.buzz(40, 60, 40)
                ballState = BallState.DROP
                shakeTick++
                message = PIT_MESSAGE
            }
            else -> {
                busy = false
                if (r.end == End.MUD) {
                    ballState = BallState.MUD
                    message = MUD_MESSAGE
                    tones.play(Tone(120.0, 0.12, Wave.SQUARE, 0.025))
                } else {
                    landTick++
                    message = restMessage()
                    tones.play(Tone(180.0, 0.09, Wave.SINE, 0.05))
                }
            }
        }
    }

    private fun finish() {
        won = true
        busy = false
        failed = false
        val st = stars(turns, lv.par)
        record(st)
        ballState = BallState.SINK
        tones.play(
            Tone(523.0, 0.12, Wave.TRIANGLE, 0.06),
            Tone(659.0, 0.12, Wave.TRIANGLE, 0.06, 0.12),
            Tone(784.0, 0.3, Wave.TRIANGLE, 0.06, 0.24),
        )
        buzzer.buzz(30)
        pulseGoal()
        message = ""
        // The result shows once the ball has sunk. A restart in the meantime leaves won false, so nothing shows.
        viewModelScope.launch {
            delay(500)
            if (won) showWin(st)
        }
    }

    private fun pulseGoal() {
        viewModelScope.launch(ViewFrames) {
            ringOn = true
            repeat(2) {
                ring.snapTo(0f)
                ring.animateTo(1f, tween(durationMillis = 900, easing = EASE_OUT))
            }
            ringOn = false
        }
    }

    private fun record(st: Int) {
        progress = if (mode == Mode.DAILY) {
            progress.withDailyResult(dayKey, st, turns)
        } else {
            progress.withLevelResult(level, st, turns)
        }
        store.saveProgress(progress)
    }

    private fun showWin(st: Int) {
        val campaign = mode == Mode.CAMPAIGN
        winStars = starText(st)
        winTitle = "${turnWord(turns)} · goal ${lv.par}"
        winNext = if (campaign) "Next" else "Levels"
        winConfetti = st == 3
        winMapShown = campaign && progress.isChapterDone(Chapters.of(level))
        lastResult = resultText(st)
        winShown = true
        // The next level is made while this one is on screen, so it is ready as soon as the player asks for it.
        if (campaign) viewModelScope.launch(Dispatchers.Default) { bank.levelAt(level + 1) }
    }

    private fun resultText(st: Int): String {
        val where = if (mode == Mode.DAILY) "Daily puzzle, ${dayLabel(dayKey)}" else "Level ${level + 1}"
        return "Turnfall · $where · ${starText(st)} · ${turnWord(turns)} (goal ${lv.par})"
    }

    /** Shares the result through the phone's share sheet. */
    fun shareResult() {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, lastResult)
        }
        val chooser = Intent.createChooser(send, "Share your result").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        getApplication<Application>().startActivity(chooser)
    }

    /** Undo: the last turn, the ball's place and the world's angle come back. Two per attempt. */
    fun undo() {
        if (busy || won || undoLeft <= 0 || hist.isEmpty()) return
        clearFx()
        val h = hist.removeAt(hist.lastIndex)
        pos = h.pos
        gravity = h.gravity
        angle = h.angle
        turns = h.turns
        undoLeft -= 1
        failed = false
        ballState = restBall()
        animateAngle()
        message = restMessage()
    }

    /** A hint shows the next turn of the fewest-turn solution, as the path it would take. Three per attempt. */
    fun useHint() {
        if (busy || won || failed) return
        if (hintsLeft <= 0) {
            message = "No hints left"
            return
        }
        val d = hint(board, pos.x, pos.y, gravity)
        if (d == null) {
            message = "No path"
            return
        }
        hintsLeft -= 1
        previewFor(if (d == 'R') 1 else -1)
        message = "Hint: $message"
    }

    private fun previewFor(dir: Int) {
        if (busy || won || failed) return
        val r = slide(board, pos.x, pos.y, (gravity + dir + 4) % 4, keepPath = true)
        fx = Fx(if (r.path.isEmpty()) emptyList() else listOf(pos) + r.path, r.end, r.x, r.y)
        message = when {
            !r.moved -> "Stays put"
            r.end == End.WIN -> "Into the hole!"
            r.end == End.PIT -> "Into a pit!"
            r.end == End.MUD -> "Mud"
            else -> "Wall"
        }
    }

    private fun clearFx() {
        fx = null
    }

    private fun clearPreview() {
        clearFx()
        if (!busy && !won) message = restMessage()
    }

    /** The handle went down. Returns false when it cannot move now. */
    fun joyDown(): Boolean {
        if (busy || won || failed) return false
        teach = false
        joyDir = 0
        return true
    }

    /** The handle is at [fraction] of its travel: -1 at the left end, 1 at the right end. */
    fun joyMoved(fraction: Float) {
        val dir = when {
            fraction >= 0.5f -> 1
            fraction <= -0.5f -> -1
            else -> 0
        }
        joyPull = dir
        if (dir != joyDir) {
            joyDir = dir
            if (dir != 0) previewFor(dir) else clearPreview()
        }
    }

    /** The handle was let go. Past half way it turns the world; otherwise nothing happens. */
    fun joyUp(cancelled: Boolean) {
        val dir = joyDir
        joyDir = 0
        joyPull = 0
        clearFx()
        if (!cancelled && dir != 0 && !busy && !won && !failed) turn(dir) else message = restMessage()
    }

    fun openLevels() {
        if (mode == Mode.CAMPAIGN) sheetChapter = Chapters.of(level)
        sheetChapter = sheetChapter.coerceIn(0, progress.maxChapter(level))
        open(Sheet.LEVELS)
    }

    fun previousChapter() {
        if (sheetChapter > 0) sheetChapter -= 1
    }

    fun nextChapter() {
        if (sheetChapter < progress.maxChapter(level)) sheetChapter += 1
    }

    fun openSettings() {
        armReset(false)
        open(Sheet.SETTINGS)
    }

    /** Opens the chapter map. The picture is drawn in the colours of [colors]. */
    fun openMap(chapter: Int, colors: MapColors) {
        mapChapter = chapter
        mapNote = MAP_HINT
        mapImage = null
        open(Sheet.MAP)
        val snapshot = progress
        viewModelScope.launch {
            val bitmap = withContext(Dispatchers.Default) {
                renderChapterMap(getApplication(), chapter, snapshot, colors)
            }
            mapBitmap = bitmap
            mapImage = bitmap.asImageBitmap()
        }
    }

    /** Long-press on the map: saves the picture to the gallery. */
    fun saveMap() {
        val bitmap = mapBitmap ?: return
        val uri = saveToGallery(getApplication(), bitmap, "turnfall-chapter-${mapChapter + 1}.png")
        mapNote = if (uri != null) "Saved to Pictures/Turnfall" else "Could not save it."
    }

    /** Closes the top screen, if one is open. */
    fun closeTop() {
        if (sheets.isNotEmpty()) sheets.removeAt(sheets.lastIndex)
    }

    fun close(sheet: Sheet) {
        sheets.remove(sheet)
    }

    private fun open(sheet: Sheet) {
        if (!sheets.contains(sheet)) sheets.add(sheet)
    }

    fun winNext() {
        if (mode == Mode.DAILY) {
            winShown = false
            openLevels()
        } else {
            loadLevel(level + 1)
        }
    }

    fun setSpeed(speed: Speed) = changeSettings(settings.copy(speed = speed))

    fun setTheme(theme: ThemeChoice) = changeSettings(settings.copy(theme = theme))

    fun toggleSound() {
        changeSettings(settings.copy(sound = !settings.sound))
        tones.play(Tone(660.0, 0.08, Wave.TRIANGLE, 0.05))
    }

    fun toggleVibe() {
        changeSettings(settings.copy(vibe = !settings.vibe))
        buzzer.buzz(20)
    }

    private fun changeSettings(next: Settings) {
        settings = next
        store.saveSettings(settings)
    }

    /** The reset takes two taps: the first arms it for four seconds. */
    fun resetTapped() {
        if (!resetArmed) {
            armReset(true)
            return
        }
        progress = Progress()
        store.saveProgress(progress)
        armReset(false)
        message = "Oops"
    }

    private fun armReset(on: Boolean) {
        resetJob?.cancel()
        resetArmed = on
        if (on) {
            resetJob = viewModelScope.launch {
                delay(4000)
                resetArmed = false
            }
        }
    }

    /** The first level that is not finished yet. */
    private fun firstUnfinished(): Int {
        var first = 0
        while (progress.best.containsKey(first.toString())) first++
        return first
    }

    private fun todayKey(): String = dateKey(LocalDate.now(ZoneOffset.UTC))

    companion object {
        const val TURN_MS = 420
        const val STEP_MS = 110
        const val MAP_HINT = "Long-press the picture to save it."
        const val PIT_MESSAGE = "Fell in a pit"
        const val MUD_MESSAGE = "Stuck in mud"
    }

    init {
        teach = progress.best.isEmpty()
        prepareLevel(firstUnfinished())
    }
}

private val TURN_EASING = CubicBezierEasing(0.45f, 0.05f, 0.25f, 1f)
private val EASE_OUT = CubicBezierEasing(0f, 0f, 0.58f, 1f)

/**
 * Animatable needs a frame clock in its coroutine. viewModelScope has none, and launching there crashes on the
 * first animateTo. This clock takes its frames from Choreographer, the same source Compose uses.
 */
private val ViewFrames = object : MonotonicFrameClock {
    override suspend fun <R> withFrameNanos(onFrame: (Long) -> R): R = suspendCancellableCoroutine { cont ->
        val callback = Choreographer.FrameCallback { nanos -> cont.resumeWith(runCatching { onFrame(nanos) }) }
        Choreographer.getInstance().postFrameCallback(callback)
        cont.invokeOnCancellation { Choreographer.getInstance().removeFrameCallback(callback) }
    }
}
