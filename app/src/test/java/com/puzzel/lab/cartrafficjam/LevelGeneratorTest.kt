package com.puzzel.lab.cartrafficjam

import com.puzzel.lab.cartrafficjam.game.gen.Grid
import com.puzzel.lab.cartrafficjam.game.gen.LevelGenerator
import com.puzzel.lab.cartrafficjam.game.gen.Solver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {

    @Test
    fun allLevelsAreValidAndSolvable() {
        val start = System.nanoTime()
        var maxMs = 0.0
        var totalVehicles = 0L
        for (n in 1..5000) {
            val t = System.nanoTime()
            val level = LevelGenerator.generate(n)
            maxMs = maxOf(maxMs, (System.nanoTime() - t) / 1e6)
            totalVehicles += level.vehicles.size

            // Seats always equal passengers, per colour.
            for (c in 0 until 8) {
                val seats = level.vehicles.filter { it.color == c }.sumOf { it.type.seats }
                assertEquals("level $n colour $c", seats, level.queue.count { it == c })
            }
            // No overlaps, everything inside the mask.
            val grid = Grid(level.cols, level.rows)
            level.vehicles.forEach { v ->
                v.forEachCell { x, y ->
                    assertTrue("level $n off-mask", level.inMask(x, y))
                    assertEquals("level $n overlap", -1, grid.at(x, y))
                }
                grid.put(v)
            }
            assertTrue("level $n has no free vehicle", level.vehicles.any { grid.isFree(it) })
            assertTrue("level $n unsolvable", Solver.solves(level))
        }
        val totalS = (System.nanoTime() - start) / 1e9
        println("5000 levels in %.1fs, slowest %.1f ms, avg vehicles %.1f".format(totalS, maxMs, totalVehicles / 5000.0))
        assertTrue("slowest level took $maxMs ms", maxMs < 250)
    }

    @Test
    fun generationIsDeterministic() {
        for (n in listOf(2, 17, 250, 4999)) {
            val a = LevelGenerator.generate(n)
            val b = LevelGenerator.generate(n)
            assertEquals(a.vehicles, b.vehicles)
            assertTrue(a.queue.contentEquals(b.queue))
        }
    }

    @Test
    fun printCurve() {
        for (n in listOf(1, 2, 3, 5, 10, 20, 50, 100, 500, 1000, 5000)) {
            val l = LevelGenerator.generate(n)
            println("L$n ${l.cols}x${l.rows} vehicles=${l.vehicles.size} colors=${l.colorCount} passengers=${l.queue.size} rot=${l.rotation} ${l.difficulty}")
        }
    }
}
