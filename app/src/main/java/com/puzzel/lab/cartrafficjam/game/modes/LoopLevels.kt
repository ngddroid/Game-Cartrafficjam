package com.puzzel.lab.cartrafficjam.game.modes

import com.puzzel.lab.cartrafficjam.game.gen.DifficultyCurve
import com.puzzel.lab.cartrafficjam.game.gen.LevelGenerator
import com.puzzel.lab.cartrafficjam.game.gen.Shape
import com.puzzel.lab.cartrafficjam.game.model.Level
import com.puzzel.lab.cartrafficjam.game.model.QueueStyle
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Crowd Loop levels: a small lot of big-capacity vehicles and a crowd four times the size.
 * Every rider of the classic queue becomes [SEAT_SCALE] riders in a row, so the classic
 * solvability guarantee carries over unchanged.
 */
object LoopLevels {
    const val SEAT_SCALE = 4

    private val cache = object : LinkedHashMap<Int, Level>(8, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Level>?) = size > 6
    }

    fun get(n: Int): Level = synchronized(cache) { cache.getOrPut(n) { build(n) } }

    private fun build(n: Int): Level {
        val difficulty = DifficultyCurve.difficulty(n)
        val base = LevelGenerator.custom(n, n * 7_919L + 31_337L) { rng ->
            val vehicles = min(17, (4 + n * 0.85f).roundToInt() + rng.nextInt(2)) +
                if (difficulty == com.puzzel.lab.cartrafficjam.game.model.Difficulty.NORMAL) 0 else 1
            val colors = when {
                n <= 2 -> 2
                n <= 5 -> 3
                n <= 12 -> 4
                n <= 30 -> 5
                else -> 6
            }
            val shape = if (n < 4) Shape.RECT else DifficultyCurve.randomShape(rng)
            val rotation = if (n < 6) 0f else listOf(0f, 0f, 12f, -12f)[rng.nextInt(4)]
            DifficultyCurve.sized(
                vehicles = vehicles,
                colors = min(colors, vehicles / 2),
                shape = shape,
                rotation = rotation,
                scramble = if (n <= 2) 0f else 1.4f,
                mystery = if (n >= 20) 0.1f else 0f,
                weights = floatArrayOf(0.25f, 0.35f, 0.4f),
                difficulty = difficulty,
            )
        }
        val queue = IntArray(base.queue.size * SEAT_SCALE) { base.queue[it / SEAT_SCALE] }
        return base.copy(queue = queue, seatScale = SEAT_SCALE, queueStyle = QueueStyle.LOOP)
    }
}
