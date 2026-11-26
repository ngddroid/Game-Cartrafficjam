package com.ngstudio.trafficjam.game.modes

import com.ngstudio.trafficjam.game.gen.DifficultyCurve
import com.ngstudio.trafficjam.game.gen.LevelGenerator
import com.ngstudio.trafficjam.game.gen.ModeCurve
import com.ngstudio.trafficjam.game.gen.Solver
import com.ngstudio.trafficjam.game.model.Level
import com.ngstudio.trafficjam.game.model.QueueStyle

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
            // Every knob comes from ModeCurve: see it for the level-by-level ramp.
            val vehicles = ModeCurve.loopVehicles(n, jitter = rng.nextInt(2))
            val shape = ModeCurve.lotShape(n)
            val rotation = if (n < 8) 0f else listOf(0f, 0f, 12f, -12f)[rng.nextInt(4)]
            DifficultyCurve.sized(
                vehicles = vehicles,
                colors = ModeCurve.loopColors(n, vehicles),
                shape = shape,
                rotation = rotation,
                scramble = ModeCurve.loopScramble(n),
                mystery = ModeCurve.loopMystery(n),
                weights = ModeCurve.loopWeights(n),
                difficulty = difficulty,
            )
        }
        // Fewer open stalls on late Super Hard levels, but only if the solver still finishes the
        // level that way. It checks the unscaled crowd: each rider becoming four in a row
        // doesn't change whether the level can be finished.
        val slots = ModeCurve.loopOpenSlots(n)
        val openSlots = if (slots < base.openSlots && Solver.solves(base.copy(openSlots = slots))) slots else base.openSlots
        val queue = IntArray(base.queue.size * SEAT_SCALE) { base.queue[it / SEAT_SCALE] }
        return base.copy(queue = queue, seatScale = SEAT_SCALE, queueStyle = QueueStyle.LOOP, openSlots = openSlots)
    }
}
