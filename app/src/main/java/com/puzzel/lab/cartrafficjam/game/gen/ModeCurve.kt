package com.puzzel.lab.cartrafficjam.game.gen

import com.puzzel.lab.cartrafficjam.game.model.Difficulty
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Difficulty for Crowd Loop, Pixel Crowd and Sand Sweep across their 5000 levels.
 *
 * Every mode follows the same shape as the classic curve:
 *  - A long ramp: fast over the first ~20 levels (teaching), steady to ~150, gentle to ~2000,
 *    then a near-flat cap so late levels stay fair on a phone screen.
 *  - A ten-level rhythm on top: levels ending in 5 are Hard, levels ending in 0 are Super Hard
 *    (both from level 10 on, labelled in game), and levels ending in 1 and 6 are breathers
 *    that ease off after each peak.
 *
 * Each value is read off a list of (level, value) anchors with straight lines in between, so
 * the curve is easy to tune: change an anchor and every level around it follows.
 */
object ModeCurve {

    fun tier(n: Int): Difficulty = DifficultyCurve.difficulty(n)

    /** The easy level after each peak: x1 and x6, from level 6 on. */
    fun breather(n: Int) = n > 5 && (n % 10 == 1 || n % 10 == 6)

    /** Piecewise-linear lookup through (level, value) anchors; flat past either end. */
    fun curve(n: Int, vararg pts: Pair<Int, Float>): Float {
        if (n <= pts.first().first) return pts.first().second
        for (k in 1 until pts.size) {
            val (x1, y1) = pts[k]
            if (n <= x1) {
                val (x0, y0) = pts[k - 1]
                return y0 + (y1 - y0) * (n - x0) / (x1 - x0).toFloat()
            }
        }
        return pts.last().second
    }

    // --- Lot shapes (Crowd Loop and Sand Sweep) ------------------------------------------------

    /** Shapes unlock in stages, so every few dozen levels there is a new kind of lot to read. */
    fun shapePool(n: Int): List<Shape> = buildList {
        add(Shape.RECT)
        if (n >= 4) { add(Shape.CIRCLE); add(Shape.HEX) }
        if (n >= 11) { add(Shape.DIAMOND); add(Shape.HEART) }
        if (n >= 31) { add(Shape.FLOWER); add(Shape.CROSS); add(Shape.RING) }
        if (n >= 81) { add(Shape.STAR); add(Shape.TRIANGLE); add(Shape.CLOVER) }
        if (n >= 201) { add(Shape.HOURGLASS); add(Shape.CRESCENT) }
    }

    /** Shapes with gaps and narrow parts, where blocking is hardest to read. */
    private val tricky = setOf(Shape.HEART, Shape.FLOWER, Shape.CROSS, Shape.RING, Shape.STAR, Shape.HOURGLASS, Shape.CRESCENT)

    private fun rawShape(n: Int): Shape {
        var pool = shapePool(n)
        // Super Hard levels get one of the awkward shapes once there are some to choose from.
        if (tier(n) == Difficulty.SUPER_HARD) pool.filter { it in tricky }.takeIf { it.isNotEmpty() }?.let { pool = it }
        return pool[kotlin.random.Random(n * 92_821L + 3).nextInt(pool.size)]
    }

    /** Every level's lot shape, worked out in order so each one can avoid the one before. */
    private val shapes: Array<Shape> by lazy {
        val out = Array(5001) { Shape.RECT }
        for (n in 1..5000) {
            if (n < 4 || breather(n)) continue
            var s = rawShape(n)
            if (s == out[n - 1]) {
                // Prefer another awkward shape on Super Hard levels, else any other unlocked one.
                val prev = out[n - 1]
                val all = shapePool(n).filter { it != prev }
                val hard = all.filter { it in tricky }
                s = (if (tier(n) == Difficulty.SUPER_HARD && hard.isNotEmpty()) hard else all).first()
            }
            out[n] = s
        }
        out
    }

    /**
     * The lot shape for level [n]: a plain rectangle on the first levels and on breathers,
     * otherwise an unlocked shape that differs from the level before.
     */
    fun lotShape(n: Int): Shape = shapes[n.coerceIn(1, 5000)]

    // --- Crowd Loop ---------------------------------------------------------------------------

    /**
     * Parking stalls open at the start. Super Hard levels from 150 on open one fewer, which
     * makes every parking choice count (the level is only used if the solver still finishes it
     * with three; the locked stalls can still be opened with an ad).
     */
    fun loopOpenSlots(n: Int): Int = if (n >= 150 && tier(n) == Difficulty.SUPER_HARD) 3 else 4

    /** Vehicles in the lot. The lot sits under the park, so it tops out at 20. */
    fun loopVehicles(n: Int, jitter: Int): Int {
        val base = curve(n, 1 to 4f, 5 to 6f, 20 to 11f, 60 to 14f, 150 to 16f, 500 to 17f, 2000 to 18f)
        val bonus = when {
            tier(n) == Difficulty.SUPER_HARD -> 2
            tier(n) == Difficulty.HARD -> 1
            breather(n) -> -2
            else -> 0
        }
        return (base.roundToInt() + bonus + jitter).coerceIn(4, 20)
    }

    /** Rider colors: more colors means more vehicles that can't use the front of the crowd. */
    fun loopColors(n: Int, vehicles: Int): Int {
        var c = when {
            n <= 3 -> 2
            n <= 8 -> 3
            n <= 20 -> 4
            n <= 60 -> 5
            n <= 150 -> 6
            n <= 400 -> 7
            else -> 8
        }
        if (tier(n) != Difficulty.NORMAL) c += 1
        if (breather(n)) c -= 1
        return c.coerceIn(2, min(8, vehicles / 2))
    }

    /** How far the crowd's color order drifts from the easy removal order (solver-checked). */
    fun loopScramble(n: Int): Float {
        val base = curve(n, 1 to 0f, 2 to 0f, 3 to 0.8f, 15 to 1.4f, 60 to 2.0f, 300 to 2.4f)
        val add = when {
            tier(n) == Difficulty.SUPER_HARD -> 0.6f
            tier(n) == Difficulty.HARD -> 0.3f
            breather(n) -> -0.4f
            else -> 0f
        }
        return (base + add).coerceIn(0f, 2.8f)
    }

    /** Share of vehicles hidden as "?" until they leave the lot. */
    fun loopMystery(n: Int): Float {
        if (n < 30 || breather(n)) return 0f
        val base = curve(n, 30 to 0.05f, 300 to 0.12f, 2000 to 0.18f)
        return base + if (tier(n) == Difficulty.SUPER_HARD) 0.04f else 0f
    }

    /** Car / van / bus mix: small vehicles early, big buses (more riders each) later. */
    fun loopWeights(n: Int): FloatArray =
        if (n <= 10) floatArrayOf(0.45f, 0.35f, 0.2f) else floatArrayOf(0.25f, 0.35f, 0.4f)

    // --- Pixel Crowd --------------------------------------------------------------------------

    /**
     * Largest bus. Smaller buses split the picture into more trips, so there are more buses to
     * juggle between the three garage lanes and the three open stops.
     */
    fun pixelBusCap(n: Int): Int {
        val base = curve(n, 1 to 48f, 4 to 40f, 10 to 32f, 25 to 28f, 50 to 24f, 120 to 20f, 300 to 18f, 800 to 16f, 2000 to 14f)
        val mult = when {
            tier(n) == Difficulty.SUPER_HARD -> 0.72f
            tier(n) == Difficulty.HARD -> 0.85f
            breather(n) -> 1.25f
            else -> 1f
        }
        return (base * mult).roundToInt().coerceAtLeast(10)
    }

    /**
     * Bus stops open at the start (of four). Super Hard levels from 200 on open only two. That
     * is still always solvable, because the next bus the picture needs is always at the front
     * of a garage lane; it just leaves little room for parking the wrong one.
     */
    fun pixelOpenStops(n: Int): Int = if (n >= 200 && tier(n) == Difficulty.SUPER_HARD) 2 else 3

    // --- Sand Sweep ---------------------------------------------------------------------------

    /** Trucks in the lot: a bigger jam to dig through, and the picture split into more loads. */
    fun sandTrucks(n: Int, colors: Int): Int {
        val base = curve(n, 1 to 6f, 10 to 10f, 40 to 15f, 120 to 19f, 400 to 23f, 1500 to 26f)
        val bonus = when {
            tier(n) == Difficulty.SUPER_HARD -> 3
            tier(n) == Difficulty.HARD -> 2
            breather(n) -> -3
            else -> 0
        }
        return max(colors + 3, base.roundToInt() + bonus).coerceAtMost(26)
    }

    /**
     * How loosely the lot order follows the picture. At 0 the colors at the bottom of the sand
     * always come out of the lot first; higher values send some trucks out before their sand
     * is reachable, so they ride the belt waiting and take up its room.
     */
    fun sandOrderJitter(n: Int): Float {
        val base = curve(n, 1 to 0.1f, 20 to 0.2f, 80 to 0.3f, 300 to 0.4f, 1500 to 0.5f)
        return when {
            tier(n) == Difficulty.SUPER_HARD -> base + 0.2f
            tier(n) == Difficulty.HARD -> base + 0.1f
            breather(n) -> 0.05f
            else -> base
        }
    }

    /** Trucks the belt holds at the start (the + button and revives raise it up to 8). */
    fun sandBeltCapacity(n: Int): Int = if (n <= 10 || breather(n)) 6 else 5
}
