package com.ngstudio.trafficjam.game.gen

import com.ngstudio.trafficjam.game.model.Level
import com.ngstudio.trafficjam.game.model.VehicleSpec

/** Fast playouts used to check that generated levels can be finished with the open slots. */
object Solver {

    fun solves(level: Level) = solvesIntended(level) || solvesGreedy(level)

    /** Drives vehicles in reverse placement order, parking only when the queue needs it. */
    fun solvesIntended(level: Level): Boolean {
        val order = level.vehicles.reversed()
        var next = 0
        return playout(level) { grid, _, _ ->
            while (next < order.size && grid.occ.none { it == order[next].id }) next++
            if (next < order.size && grid.isFree(order[next])) order[next] else null
        }
    }

    /** Always picks a free vehicle matching the queue front, else the colour needed soonest. */
    fun solvesGreedy(level: Level): Boolean = playout(level) { grid, remaining, front ->
        val free = remaining.filter { grid.isFree(it) }
        free.firstOrNull { it.color == level.queue[front] }
            ?: free.minByOrNull { v ->
                var i = front
                while (i < level.queue.size && level.queue[i] != v.color) i++
                i
            }
    }

    private inline fun playout(
        level: Level,
        pick: (Grid, List<VehicleSpec>, Int) -> VehicleSpec?,
    ): Boolean {
        val grid = Grid(level.cols, level.rows)
        level.vehicles.forEach { grid.put(it) }
        val remaining = level.vehicles.toMutableList()
        val parkedColor = IntArray(level.openSlots)
        val parkedSeats = IntArray(level.openSlots)
        var parked = 0
        var front = 0
        val queue = level.queue
        var guard = 0
        while (guard++ < 100_000) {
            // Board as many as possible.
            var boarded = true
            while (boarded && front < queue.size) {
                boarded = false
                for (s in 0 until parked) {
                    if (parkedColor[s] == queue[front] && parkedSeats[s] > 0) {
                        parkedSeats[s]--
                        front++
                        if (parkedSeats[s] == 0) {
                            for (k in s until parked - 1) {
                                parkedColor[k] = parkedColor[k + 1]
                                parkedSeats[k] = parkedSeats[k + 1]
                            }
                            parked--
                        }
                        boarded = true
                        break
                    }
                }
            }
            if (front >= queue.size) return true
            if (parked >= level.openSlots) return false
            val v = pick(grid, remaining, front) ?: return false
            grid.remove(v)
            remaining.remove(v)
            parkedColor[parked] = v.color
            parkedSeats[parked] = v.type.seats
            parked++
        }
        return false
    }
}
