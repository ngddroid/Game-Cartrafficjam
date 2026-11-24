package com.puzzel.lab.cartrafficjam

import com.puzzel.lab.cartrafficjam.game.modes.LoopLevels
import com.puzzel.lab.cartrafficjam.game.modes.PixelEngine
import com.puzzel.lab.cartrafficjam.game.modes.PixelPictures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModeLevelsTest {
    @Test
    fun loopLevelsBuildQuicklyWithMatchingSeats() {
        for (n in 1..40) {
            val t0 = System.nanoTime()
            val l = LoopLevels.get(n)
            val ms = (System.nanoTime() - t0) / 1e6
            val seats = l.vehicles.sumOf { it.type.seats * l.seatScale }
            println("loop $n: ${l.vehicles.size} vehicles, ${l.queue.size} riders, ${"%.1f".format(ms)} ms")
            assertEquals(seats, l.queue.size)
        }
    }

    @Test
    fun pixelPlansCoverEveryRider() {
        for (n in 1..200) {
            val pic = PixelPictures.forLevel(n)
            val t0 = System.nanoTime()
            val lanes = PixelEngine.plan(pic, n)
            val ms = (System.nanoTime() - t0) / 1e6
            for (c in 0 until 8) {
                val cells = pic.cells.count { it == c }
                val cap = lanes.flatten().filter { it.first == c }.sumOf { it.second }
                assertEquals("level $n color $c", cells, cap)
            }
            println("pixel $n ${pic.name}: ${lanes.map { it.size }} buses, ${"%.1f".format(ms)} ms")
            assertTrue(ms < 500)
        }
    }
}
