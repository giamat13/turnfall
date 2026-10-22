package io.github.giamat13.turnfall.game

/**
 * The rules of Turnfall, ported from src/core.js. The page and this port must give the same answer for the same board,
 * so each function follows its JavaScript twin step by step. The unit tests check both on the same boards.
 *
 * The ball always falls toward the bottom of the screen. A turn rotates the world, so the gravity index g moves:
 * 0 down, 1 right, 2 up, 3 left. A right turn ('R') adds 1, a left turn ('L') adds 3.
 */

/** From this level index on the levels are made from their index, not designed. */
const val ENDLESS_FROM = 108

/** A ball that springs send back this many times stops on the spring. */
private const val MAX_BOUNCES = 4

/** What a level gets when it cannot be solved within 16 turns. */
const val UNSOLVED = 99

/** The search looks this many turns ahead for a hint and for the fewest turns. */
private const val SEARCH_DEPTH = 16

val DX = intArrayOf(0, 1, 0, -1)
val DY = intArrayOf(1, 0, -1, 0)

/** Cell codes after parsing. An arrow is [ARROW] plus its direction. */
object Cell {
    const val WALL = 0
    const val FLOOR = 1
    const val HOLE = 2
    const val MUD = 3
    const val PIT = 4
    const val SPRING = 5
    const val ARROW = 6

    fun isArrow(code: Int) = code >= ARROW
}

data class Pos(val x: Int, val y: Int)

/** A parsed board. Cells are stored row by row: cells[y * n + x]. [start] is null when the board has no start. */
class Board(val n: Int, private val cells: IntArray, val start: Pos?) {
    fun cell(x: Int, y: Int): Int = cells[y * n + x]
}

/** Which mechanics a parse keeps. A mechanic left out becomes plain floor (the page's opts.mud = false and so on). */
data class Mechanics(
    val mud: Boolean = true,
    val pits: Boolean = true,
    val arrows: Boolean = true,
    val springs: Boolean = true,
)

/** The mechanics a level needs. See [meets]. */
enum class Mechanic { MUD, PITS, ARROWS, SPRINGS }

/** The page's parse(). Rows use '#' wall, '.' floor, 'S' start, 'O' hole, 'm' mud, 'p' pit, 'z' spring and v > ^ < arrows. */
fun parseBoard(rows: List<String>, mechanics: Mechanics = Mechanics()): Board {
    val n = rows.size
    val cells = IntArray(n * n)
    var start: Pos? = null
    for (y in 0 until n) {
        for (x in 0 until n) {
            cells[y * n + x] = when (rows[y][x]) {
                '#' -> Cell.WALL
                'O' -> Cell.HOLE
                'm' -> if (mechanics.mud) Cell.MUD else Cell.FLOOR
                'p' -> if (mechanics.pits) Cell.PIT else Cell.FLOOR
                'z' -> if (mechanics.springs) Cell.SPRING else Cell.FLOOR
                'v' -> if (mechanics.arrows) Cell.ARROW + 0 else Cell.FLOOR
                '>' -> if (mechanics.arrows) Cell.ARROW + 1 else Cell.FLOOR
                '^' -> if (mechanics.arrows) Cell.ARROW + 2 else Cell.FLOOR
                '<' -> if (mechanics.arrows) Cell.ARROW + 3 else Cell.FLOOR
                'S' -> {
                    start = Pos(x, y)
                    Cell.FLOOR
                }
                else -> Cell.FLOOR
            }
        }
    }
    return Board(n, cells, start)
}

/** Where a slide ends: in the hole, in a pit, in mud, or against a wall, the edge, or the spring limit. */
enum class End { WIN, PIT, MUD, WALL }

/**
 * One slide. [moved] says whether the ball moved at all, [path] lists the cells it passed in order, and [loop] is set
 * when the ball runs on for ever (only arrows can do that).
 */
class Slide(
    val x: Int,
    val y: Int,
    val end: End,
    val moved: Boolean,
    val path: List<Pos>,
    val loop: Boolean = false,
)

private val NO_PATH: List<Pos> = emptyList()

/**
 * The ball slides in gravity direction [g0] from (x0, y0) until something stops it. An arrow turns it to the arrow's
 * direction; a spring sends it back the way it came. [keepPath] false skips the list of cells, which the search does
 * not need.
 */
fun slide(board: Board, x0: Int, y0: Int, g0: Int, keepPath: Boolean = true): Slide {
    val n = board.n
    val limit = 4 * n * n + 16
    val trail: MutableList<Pos>? = if (keepPath) ArrayList() else null
    var x = x0
    var y = y0
    var g = g0
    var steps = 0
    var bounces = 0
    while (true) {
        val nx = x + DX[g]
        val ny = y + DY[g]
        if (nx < 0 || ny < 0 || nx >= n || ny >= n || board.cell(nx, ny) == Cell.WALL) {
            return Slide(x, y, End.WALL, moved = steps > 0, path = trail ?: NO_PATH)
        }
        x = nx
        y = ny
        steps++
        trail?.add(Pos(x, y))
        if (steps > limit) return Slide(x, y, End.WALL, moved = true, path = trail ?: NO_PATH, loop = true)
        when (val k = board.cell(x, y)) {
            Cell.HOLE -> return Slide(x, y, End.WIN, moved = true, path = trail ?: NO_PATH)
            Cell.PIT -> return Slide(x, y, End.PIT, moved = true, path = trail ?: NO_PATH)
            Cell.MUD -> return Slide(x, y, End.MUD, moved = true, path = trail ?: NO_PATH)
            Cell.SPRING -> {
                bounces++
                if (bounces > MAX_BOUNCES) return Slide(x, y, End.WALL, moved = true, path = trail ?: NO_PATH)
                g = (g + 2) % 4
            }
            else -> if (Cell.isArrow(k)) g = k - Cell.ARROW
        }
    }
}

/** The fewest turns that reach the hole ([par]) and the turns themselves, as a string of 'R' and 'L'. */
class Found(val par: Int, val moves: String)

/**
 * Breadth-first search over (cell, gravity) from one state. [strict] forbids a turn that does not move the ball.
 * The first win found at the shallowest depth is returned, so the order of the loops matters: it decides the moves.
 */
fun search(board: Board, x0: Int, y0: Int, g0: Int, strict: Boolean, maxDepth: Int): Found? {
    val n = board.n
    val states = n * n * 4
    val root = (y0 * n + x0) * 4 + g0
    val seen = BooleanArray(states)
    val parent = IntArray(states)
    val turn = CharArray(states)
    seen[root] = true
    var frontier: List<Int> = listOf(root)
    for (depth in 1..maxDepth) {
        if (frontier.isEmpty()) break
        val next = ArrayList<Int>()
        for (state in frontier) {
            val x = (state / 4) % n
            val y = (state / 4) / n
            val g = state % 4
            for (t in 0..1) {
                val letter = if (t == 0) 'R' else 'L'
                val step = if (t == 0) 1 else 3
                val ng = (g + step) % 4
                val r = slide(board, x, y, ng, keepPath = false)
                if (strict && !r.moved) continue
                if (r.end == End.PIT) continue
                if (r.end == End.WIN) {
                    var moves = letter.toString()
                    var cur = state
                    while (cur != root) {
                        moves = "${turn[cur]}$moves"
                        cur = parent[cur]
                    }
                    return Found(depth, moves)
                }
                val nk = (r.y * n + r.x) * 4 + ng
                if (!seen[nk]) {
                    seen[nk] = true
                    parent[nk] = state
                    turn[nk] = letter
                    next.add(nk)
                }
            }
        }
        frontier = next
    }
    return null
}

/** The breadth-first search from the start of a level: the first fall, gravity down. Null when there is no win. */
fun solve(board: Board, strict: Boolean, maxDepth: Int): Found? {
    val start = board.start ?: return null
    val s0 = slide(board, start.x, start.y, 0, keepPath = false)
    if (s0.end == End.WIN || s0.end == End.PIT) return null
    return search(board, s0.x, s0.y, 0, strict, maxDepth)
}

/**
 * The next turn on a fewest-turn solution from where the ball is now: 'R', 'L', or null when no way on is left.
 * A turn that moves the ball is preferred; one that does not is used only when nothing else works.
 */
fun hint(board: Board, x: Int, y: Int, g: Int): Char? {
    val found = search(board, x, y, g, strict = true, maxDepth = SEARCH_DEPTH)
        ?: search(board, x, y, g, strict = false, maxDepth = SEARCH_DEPTH)
    return found?.let { it.moves[0] }
}

/**
 * A board is kept when its fewest turns is exactly [turns], both allowing and forbidding wasted turns.
 * Returns the optimal turns as a string of 'R' and 'L' (no wasted turns), or null.
 */
fun evaluate(rows: List<String>, turns: Int): String? {
    val board = parseBoard(rows)
    if (board.start == null) return null
    val free = solve(board, strict = false, maxDepth = turns)
    if (free == null || free.par != turns) return null
    val strict = solve(board, strict = true, maxDepth = turns)
    if (strict == null || strict.par != turns) return null
    return strict.moves
}

/** The fewest turns with the given mechanics; [UNSOLVED] when the board has no start or no win within 16 turns. */
fun parOf(rows: List<String>, mechanics: Mechanics = Mechanics()): Int {
    val board = parseBoard(rows, mechanics)
    if (board.start == null) return UNSOLVED
    return solve(board, strict = false, maxDepth = SEARCH_DEPTH)?.par ?: UNSOLVED
}

/** What a generated board must need: nothing, at least one of the four mechanics, or each one of a list. */
sealed interface Need {
    data object None : Need
    data object AnyMechanic : Need
    class Each(val mechanics: List<Mechanic>) : Need
}

private fun without(mechanic: Mechanic) = Mechanics(
    mud = mechanic != Mechanic.MUD,
    pits = mechanic != Mechanic.PITS,
    arrows = mechanic != Mechanic.ARROWS,
    springs = mechanic != Mechanic.SPRINGS,
)

/** A mechanic is needed when taking it away changes the fewest turns. */
fun meets(rows: List<String>, turns: Int, need: Need): Boolean = when (need) {
    Need.None -> true
    Need.AnyMechanic -> Mechanic.entries.any { parOf(rows, without(it)) != turns }
    is Need.Each -> need.mechanics.all { parOf(rows, without(it)) != turns }
}

/** Can the ball run on for ever from the start? Only arrows make such a loop. */
fun hasLoop(board: Board, start: Pos): Boolean {
    val s0 = slide(board, start.x, start.y, 0, keepPath = false)
    if (s0.loop) return true
    if (s0.end == End.WIN || s0.end == End.PIT) return false
    val n = board.n
    val seen = BooleanArray(n * n * 4)
    val stack = ArrayDeque<Int>()
    val first = (s0.y * n + s0.x) * 4
    seen[first] = true
    stack.addLast(first)
    while (stack.isNotEmpty()) {
        val state = stack.removeLast()
        val x = (state / 4) % n
        val y = (state / 4) / n
        val g = state % 4
        for (t in 0..1) {
            val step = if (t == 0) 1 else 3
            val ng = (g + step) % 4
            val r = slide(board, x, y, ng, keepPath = false)
            if (r.loop) return true
            if (r.end == End.WIN || r.end == End.PIT) continue
            val nk = (r.y * n + r.x) * 4 + ng
            if (!seen[nk]) {
                seen[nk] = true
                stack.addLast(nk)
            }
        }
    }
    return false
}

/** The page's looping(): a board with no arrow cannot loop, so only boards with arrows are searched. */
fun looping(rows: List<String>): Boolean {
    if (rows.none { row -> row.any { it == 'v' || it == '<' || it == '>' || it == '^' } }) return false
    val board = parseBoard(rows)
    val start = board.start ?: return false
    return hasLoop(board, start)
}

/** Stars: 3 at or under par, 2 within two turns of par, 1 otherwise. */
fun stars(turns: Int, par: Int): Int = when {
    turns <= par -> 3
    turns <= par + 2 -> 2
    else -> 1
}
