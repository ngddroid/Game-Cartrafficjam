package com.ngstudio.trafficjam.game.gen

import com.ngstudio.trafficjam.game.model.Difficulty
import com.ngstudio.trafficjam.game.model.Dir
import com.ngstudio.trafficjam.game.model.JamColor
import com.ngstudio.trafficjam.game.model.Level
import com.ngstudio.trafficjam.game.model.VehicleSpec
import com.ngstudio.trafficjam.game.model.VehicleType
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

enum class Shape(val fill: Float) {
    RECT(1f), CIRCLE(0.78f), DIAMOND(0.55f), HEART(0.62f), FLOWER(0.62f), RING(0.64f), CROSS(0.6f), HEX(0.76f),

    // Used by the game modes (Crowd Loop, Sand Sweep); Classic keeps its own set.
    STAR(0.55f), TRIANGLE(0.52f), CLOVER(0.7f), HOURGLASS(0.62f), CRESCENT(0.48f);

    /** Whether the cell centre (u, v) in [-1, 1]² lies inside the shape. */
    fun contains(u: Float, v: Float): Boolean {
        val r = sqrt(u * u + v * v)
        return when (this) {
            RECT -> true
            CIRCLE -> r <= 1.04f
            DIAMOND -> abs(u) + abs(v) <= 1.12f
            HEART -> {
                val x = u * 1.22f
                val y = -v * 1.22f + 0.25f
                (x * x + y * y - 1).pow(3) - x * x * y * y * y <= 0f
            }
            FLOWER -> r <= 0.72f + 0.32f * cos(5 * atan2(v, u))
            RING -> r <= 1.04f && r >= 0.36f
            CROSS -> abs(u) <= 0.46f || abs(v) <= 0.46f
            HEX -> abs(v) <= 0.92f && abs(u) * 0.866f + abs(v) * 0.5f <= 0.95f
            STAR -> {
                // Five points, one straight up (v grows downwards).
                val k = (cos(5 * (atan2(v, u) + Math.PI.toFloat() / 2)) + 1) / 2
                r <= 0.5f + 0.56f * k * k
            }
            TRIANGLE -> v >= -0.98f && v <= 0.98f && abs(u) <= (v + 1f) * 0.52f
            CLOVER -> listOf(0.46f to 0f, -0.46f to 0f, 0f to 0.46f, 0f to -0.46f)
                .any { (cx, cy) -> (u - cx) * (u - cx) + (v - cy) * (v - cy) <= 0.3f }
            HOURGLASS -> abs(v) <= 0.98f && abs(u) <= abs(v) + 0.28f
            CRESCENT -> r <= 1.04f && (u - 0.5f) * (u - 0.5f) + (v + 0.12f) * (v + 0.12f) >= 0.6f
        }
    }
}

data class LevelParams(
    val vehicles: Int,
    val colors: Int,
    val cols: Int,
    val rows: Int,
    val shape: Shape,
    val rotation: Float,
    val typeWeights: FloatArray,
    val mysteryRatio: Float,
    val scramble: Float,
    val difficulty: Difficulty,
)

object DifficultyCurve {

    fun difficulty(n: Int) = when {
        n >= 10 && n % 10 == 0 -> Difficulty.SUPER_HARD
        n >= 5 && n % 5 == 0 -> Difficulty.HARD
        else -> Difficulty.NORMAL
    }

    fun params(n: Int, rng: Random): LevelParams {
        val diff = difficulty(n)
        val base = when {
            n <= 10 -> 3f * n + 2
            n <= 50 -> 32f + (n - 10) * 0.45f
            n <= 300 -> 50f + (n - 50) * 0.05f
            else -> 62.5f + min(9.5f, (n - 300) * 0.004f)
        }
        val breather = n > 5 && (n % 10 == 1 || n % 10 == 6)
        val mult = when {
            diff == Difficulty.SUPER_HARD -> 1.25f
            diff == Difficulty.HARD -> 1.12f
            breather -> 0.8f
            else -> 1f
        }
        val jitter = 0.94f + rng.nextFloat() * 0.12f
        val vehicles = (base * mult * jitter).roundToInt().coerceIn(4, 62)

        var colors = when {
            n <= 3 -> 2
            n <= 6 -> 3
            n <= 14 -> 4
            n <= 32 -> 5
            n <= 90 -> 6
            n <= 220 -> 7
            else -> 8
        }
        if (diff != Difficulty.NORMAL) colors += 1
        colors = colors.coerceIn(2, min(8, vehicles / 2))

        val shape = if (n < 6) Shape.RECT else pickShape(rng)
        val rotation = if (n < 8) 0f else when (rng.nextInt(5)) {
            0, 1 -> 0f
            2 -> 14f
            3 -> -14f
            else -> if (rng.nextBoolean()) 22f else -22f
        }

        val weights = if (n <= 5) floatArrayOf(0.45f, 0.4f, 0.15f) else floatArrayOf(0.38f, 0.34f, 0.28f)
        val avgArea = 2 * (3 * weights[0] + 4 * weights[1] + 5 * weights[2])
        val cells = vehicles * avgArea / 0.86f / shape.fill
        val aspect = if (rotation == 0f) 1.1f else 1.0f
        val maxCols = if (rotation == 0f) 23 else 20
        val cols = sqrt(cells / aspect).roundToInt().coerceIn(9, maxCols)
        val rows = (cols * aspect).roundToInt().coerceIn(9, maxCols + 2)
        // When the grid is capped, ask only for what fits.
        val fits = (cols * rows * shape.fill * 0.86f / avgArea).toInt()
        val target = vehicles.coerceAtMost(fits.coerceAtLeast(4))

        val mystery = if (n < 25) 0f else min(0.22f, 0.06f + (n - 25) * 0.0008f)
        val scramble = when {
            n <= 3 -> 0f
            n <= 20 -> 1.2f
            else -> 2.0f
        } + if (diff == Difficulty.NORMAL) 0f else 0.6f

        return LevelParams(target, colors.coerceAtMost(max(2, target / 2)), cols, rows, shape, rotation, weights, mystery, min(scramble, 2.8f), diff)
    }

    /**
     * Parameters for a lot of roughly [vehicles] vehicles, sized the same way as the main curve.
     * Used by the other game modes, which want smaller or larger lots than the classic curve.
     */
    fun sized(
        vehicles: Int,
        colors: Int,
        shape: Shape,
        rotation: Float,
        scramble: Float,
        mystery: Float = 0f,
        weights: FloatArray = floatArrayOf(0.38f, 0.34f, 0.28f),
        difficulty: Difficulty = Difficulty.NORMAL,
    ): LevelParams {
        val avgArea = 2 * (3 * weights[0] + 4 * weights[1] + 5 * weights[2])
        val cells = vehicles * avgArea / 0.86f / shape.fill
        val aspect = if (rotation == 0f) 1.1f else 1.0f
        val maxCols = if (rotation == 0f) 23 else 20
        val cols = sqrt(cells / aspect).roundToInt().coerceIn(7, maxCols)
        val rows = (cols * aspect).roundToInt().coerceIn(7, maxCols + 2)
        val fits = (cols * rows * shape.fill * 0.86f / avgArea).toInt()
        val target = vehicles.coerceAtMost(fits.coerceAtLeast(3))
        return LevelParams(target, colors.coerceIn(1, 8), cols, rows, shape, rotation, weights, mystery, scramble, difficulty)
    }

    fun randomShape(rng: Random) = pickShape(rng)

    private fun pickShape(rng: Random): Shape {
        val r = rng.nextFloat()
        return when {
            r < 0.30f -> Shape.RECT
            r < 0.42f -> Shape.CIRCLE
            r < 0.52f -> Shape.HEART
            r < 0.62f -> Shape.HEX
            r < 0.71f -> Shape.DIAMOND
            r < 0.80f -> Shape.FLOWER
            r < 0.90f -> Shape.RING
            else -> Shape.CROSS
        }
    }
}

/** Occupancy grid shared by the generator, the solver and the live game. */
class Grid(val cols: Int, val rows: Int) {
    val occ = IntArray(cols * rows) { -1 }

    fun at(x: Int, y: Int) = if (x in 0 until cols && y in 0 until rows) occ[y * cols + x] else -1

    fun put(v: VehicleSpec) = v.forEachCell { x, y -> occ[y * cols + x] = v.id }
    fun remove(v: VehicleSpec) = v.forEachCell { x, y -> if (occ[y * cols + x] == v.id) occ[y * cols + x] = -1 }

    /**
     * Free rows of units in front of [v] before it would touch another vehicle, or -1 if the
     * whole strip to the board edge is clear.
     */
    fun blockDistance(v: VehicleSpec): Int {
        var d = 0
        var step = 1
        while (true) {
            var hit = false
            var outside = true
            when (v.dir) {
                Dir.N, Dir.S -> {
                    val yy = if (v.dir == Dir.N) v.y - step else v.y + v.h - 1 + step
                    if (yy in 0 until rows) {
                        outside = false
                        for (xx in v.x until v.x + v.w) if (occ[yy * cols + xx] >= 0) hit = true
                    }
                }
                Dir.E, Dir.W -> {
                    val xx = if (v.dir == Dir.W) v.x - step else v.x + v.w - 1 + step
                    if (xx in 0 until cols) {
                        outside = false
                        for (yy in v.y until v.y + v.h) if (occ[yy * cols + xx] >= 0) hit = true
                    }
                }
            }
            if (outside) return -1
            if (hit) return d
            d++
            step++
        }
    }

    /** The vehicle id standing in [v]'s way, or -1. */
    fun blocker(v: VehicleSpec): Int {
        val d = blockDistance(v)
        if (d < 0) return -1
        val step = d + 1
        when (v.dir) {
            Dir.N, Dir.S -> {
                val yy = if (v.dir == Dir.N) v.y - step else v.y + v.h - 1 + step
                for (xx in v.x until v.x + v.w) occ[yy * cols + xx].let { if (it >= 0) return it }
            }
            Dir.E, Dir.W -> {
                val xx = if (v.dir == Dir.W) v.x - step else v.x + v.w - 1 + step
                for (yy in v.y until v.y + v.h) occ[yy * cols + xx].let { if (it >= 0) return it }
            }
        }
        return -1
    }

    fun isFree(v: VehicleSpec) = blockDistance(v) < 0
}

object LevelGenerator {

    private val cache = object : LinkedHashMap<Int, Level>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Level>?) = size > 12
    }

    fun get(n: Int): Level = synchronized(cache) { cache.getOrPut(n) { generate(n) } }

    fun generate(n: Int): Level {
        if (n == 1) return tutorialLevel()
        for (attempt in 0 until 12) {
            val rng = Random(n * 7919L + 104729L * attempt + 17)
            val level = build(n, rng, scrambleOverride = if (attempt >= 8) 0f else null, minFill = 0.7f)
            if (level != null && Solver.solvesIntended(level)) return level
        }
        // Scramble 0 always solves in removal order, so this cannot fail.
        for (k in 0 until 20) build(n, Random(n * 31L + k), scrambleOverride = 0f, minFill = 0.4f)?.let { return it }
        error("level $n could not be generated")
    }

    /**
     * A lot built from explicit [params] rather than the classic curve. The vehicles are listed
     * so that the reversed list is a guaranteed removal order.
     */
    fun custom(number: Int, seed: Long, params: (Random) -> LevelParams): Level {
        for (attempt in 0 until 12) {
            val rng = Random(seed + 104729L * attempt)
            val level = build(number, rng, scrambleOverride = if (attempt >= 8) 0f else null, minFill = 0.7f, fixed = params(rng))
            if (level != null && Solver.solvesIntended(level)) return level
        }
        for (k in 0 until 30) {
            val rng = Random(seed * 31L + k)
            build(number, rng, scrambleOverride = 0f, minFill = 0.3f, fixed = params(rng))?.let { return it }
        }
        error("custom level $number could not be generated")
    }

    /** Level 1: three vehicles that teach tap, match and blocking. */
    private fun tutorialLevel(): Level {
        val cols = 10
        val rows = 10
        val vehicles = listOf(
            VehicleSpec(0, VehicleType.VAN, JamColor.GREEN.ordinal, Dir.N, 1, 4),
            VehicleSpec(1, VehicleType.CAR, JamColor.CYAN.ordinal, Dir.E, 5, 2),
            VehicleSpec(2, VehicleType.BUS, JamColor.BLUE.ordinal, Dir.N, 5, 4),
        )
        val queue = IntArray(6) { JamColor.GREEN.ordinal } +
            IntArray(4) { JamColor.CYAN.ordinal } +
            IntArray(10) { JamColor.BLUE.ordinal }
        return Level(1, cols, rows, BooleanArray(cols * rows) { true }, vehicles, queue, 0f, Difficulty.NORMAL, 3)
    }

    private fun build(n: Int, rng: Random, scrambleOverride: Float?, minFill: Float, fixed: LevelParams? = null): Level? {
        val p = fixed ?: DifficultyCurve.params(n, rng)
        val cols = p.cols
        val rows = p.rows
        val mask = BooleanArray(cols * rows) { i ->
            val x = i % cols
            val y = i / cols
            val u = (x + 0.5f) / cols * 2 - 1
            val v = (y + 0.5f) / rows * 2 - 1
            p.shape.contains(u, v)
        }
        if ((0 until cols * rows).none { mask[it] }) return null
        val grid = Grid(cols, rows)
        val cx = (cols - 1) / 2f
        val cy = (rows - 1) / 2f

        fun fits(v: VehicleSpec): Boolean {
            if (v.x < 0 || v.y < 0 || v.x + v.w > cols || v.y + v.h > rows) return false
            v.forEachCell { xx, yy -> if (!mask[yy * cols + xx] || grid.occ[yy * cols + xx] >= 0) return false }
            return true
        }

        fun pickType(): VehicleType {
            val r = rng.nextFloat()
            return when {
                r < p.typeWeights[0] -> VehicleType.CAR
                r < p.typeWeights[0] + p.typeWeights[1] -> VehicleType.VAN
                else -> VehicleType.BUS
            }
        }

        // 1) Dense packing, centre first, orientation decided now (direction later).
        val packed = ArrayList<VehicleSpec>()
        val cells = (0 until cols * rows).filter { mask[it] }
            .map { c -> c to (c % cols - cx).let { it * it } + (c / cols - cy).let { it * it } + rng.nextFloat() * cols * 2 }
            .sortedBy { it.second }
            .map { it.first }
        val passes = listOf<() -> VehicleType>({ pickType() }, { if (rng.nextBoolean()) VehicleType.VAN else VehicleType.CAR }, { VehicleType.CAR })
        for (pass in passes) {
            for (c in cells) {
                if (packed.size >= p.vehicles) break
                if (grid.occ[c] >= 0) continue
                val horizontalFirst = rng.nextBoolean()
                val type = pass()
                for (h in listOf(horizontalFirst, !horizontalFirst)) {
                    val v = VehicleSpec(packed.size, type, 0, if (h) Dir.E else Dir.S, c % cols, c / cols)
                    if (fits(v)) {
                        grid.put(v)
                        packed += v
                        break
                    }
                }
            }
        }

        // 2) Peel from the outside in. Each vehicle gets a direction that is clear of the
        //    vehicles still on the board; the peel order is a guaranteed solution.
        val remaining = packed.toMutableList()
        val peeled = ArrayList<VehicleSpec>()
        while (remaining.isNotEmpty()) {
            val options = ArrayList<VehicleSpec>()
            for (v in remaining) {
                val dirs = if (v.dir.horizontal) listOf(Dir.E, Dir.W) else listOf(Dir.N, Dir.S)
                for (d in dirs) {
                    val cand = v.copy(dir = d)
                    if (grid.isFree(cand)) options += cand
                }
            }
            if (options.isEmpty()) {
                // Rare deadlock: lift one vehicle out of the puzzle entirely.
                val drop = remaining.removeAt(rng.nextInt(remaining.size))
                grid.remove(drop)
                continue
            }
            val pick = options[rng.nextInt(options.size)]
            remaining.removeAll { it.id == pick.id }
            grid.remove(pick)
            peeled += pick
        }
        // Vehicles are listed so that reversed order == removal order.
        val placed = peeled.reversed().mapIndexed { i, v -> v.copy(id = i) }
        if (placed.size < max(3, (p.vehicles * minFill).toInt())) return null

        // Colours: balanced round-robin over a shuffled order.
        val colorPool = JamColor.entries.indices.shuffled(rng).take(p.colors)
        val order = placed.indices.shuffled(rng)
        val colorOf = IntArray(placed.size)
        order.forEachIndexed { i, idx -> colorOf[idx] = colorPool[i % colorPool.size] }

        val vehicles = placed.mapIndexed { i, v ->
            v.copy(color = colorOf[i], mystery = rng.nextFloat() < p.mysteryRatio)
        }

        // Queue: removal order is reverse placement; each group may drift up to `scramble` places.
        val scramble = scrambleOverride ?: p.scramble
        val removal = vehicles.reversed()
        val keyed = removal.mapIndexed { i, v -> (i + rng.nextFloat() * scramble) to v }.sortedBy { it.first }
        val queue = IntArray(keyed.sumOf { it.second.type.seats })
        var q = 0
        for ((_, v) in keyed) repeat(v.type.seats) { queue[q++] = v.color }

        return Level(
            number = n,
            cols = cols,
            rows = rows,
            mask = mask,
            vehicles = vehicles,
            queue = queue,
            rotation = p.rotation,
            difficulty = p.difficulty,
            colorCount = p.colors,
        )
    }
}
