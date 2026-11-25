package com.puzzel.lab.cartrafficjam

import com.puzzel.lab.cartrafficjam.core.data.PlayerState
import com.puzzel.lab.cartrafficjam.core.data.TOTAL_LEVELS
import com.puzzel.lab.cartrafficjam.game.gen.DifficultyCurve
import com.puzzel.lab.cartrafficjam.game.gen.LevelGenerator
import com.puzzel.lab.cartrafficjam.game.gen.ModeCurve
import com.puzzel.lab.cartrafficjam.game.gen.Shape
import com.puzzel.lab.cartrafficjam.game.model.Difficulty
import com.puzzel.lab.cartrafficjam.game.model.GameMode
import com.puzzel.lab.cartrafficjam.game.modes.LoopLevels
import com.puzzel.lab.cartrafficjam.game.modes.PixelEngine
import com.puzzel.lab.cartrafficjam.game.modes.PixelPictures
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    /** Levels from every stretch of the 5000 build, fit the lot and finish quickly. */
    @Test
    fun loopLevelsBuildAcrossAllFiveThousand() {
        var worst = 0.0
        for (n in (1..TOTAL_LEVELS step 23) + listOf(4995, 4999, 5000)) {
            val t0 = System.nanoTime()
            val l = LoopLevels.get(n)
            worst = maxOf(worst, (System.nanoTime() - t0) / 1e6)
            assertEquals("level $n seats", l.vehicles.sumOf { it.type.seats * l.seatScale }, l.queue.size)
            assertTrue("level $n has ${l.vehicles.size} vehicles", l.vehicles.size in 3..20)
            assertTrue("level $n colors", l.colorCount in 2..8)
        }
        println("loop worst build ${"%.1f".format(worst)} ms")
        assertTrue(worst < 1500)
    }

    @Test
    fun pixelPlansCoverEveryRider() {
        for (n in (1..200) + (201..TOTAL_LEVELS step 37) + listOf(5000)) {
            val pic = PixelPictures.forLevel(n)
            val t0 = System.nanoTime()
            val lanes = PixelEngine.plan(pic, n)
            val ms = (System.nanoTime() - t0) / 1e6
            for (c in 0 until 8) {
                val cells = pic.cells.count { it == c }
                val cap = lanes.flatten().filter { it.first == c }.sumOf { it.second }
                assertEquals("level $n color $c", cells, cap)
            }
            if (n <= 20) println("pixel $n ${pic.name}: ${lanes.map { it.size }} buses, ${"%.1f".format(ms)} ms")
            assertTrue(ms < 500)
        }
    }

    @Test
    fun pixelFirstLapGoesFromSimpleToComplex() {
        val names = (1..PixelPictures.all.size).map { PixelPictures.forLevel(it).name }
        println("pixel first lap: $names")
        assertEquals(PixelPictures.all.size, names.toSet().size)
        val colors = (1..PixelPictures.all.size).map { PixelPictures.forLevel(it).colors(false).size }
        assertTrue("first lap starts simple: $colors", colors.take(10).average() < colors.takeLast(10).average())
    }

    @Test
    fun everyPictureIsWellFormedAndPlannable() {
        assertTrue("at least 60 pictures", PixelPictures.all.size >= 60)
        assertEquals("names are unique", PixelPictures.all.size, PixelPictures.all.map { it.name }.toSet().size)
        for (p in PixelPictures.all) {
            assertTrue("${p.name} size ${p.w}x${p.h}", p.w in 10..22 && p.h in 8..22)
            assertTrue("${p.name} has riders", p.cells.count { it >= 0 } >= 40)
            // Plan every picture at an early and a late level: every rider gets a seat.
            for (n in listOf(3, 2000)) {
                val lanes = PixelEngine.plan(p, n)
                for (c in 0 until 8) {
                    assertEquals("${p.name} @$n color $c", p.cells.count { it == c }, lanes.flatten().filter { it.first == c }.sumOf { it.second })
                }
            }
        }
    }

    @Test
    fun picturesNeverRepeatBackToBackAndAllGetUsed() {
        val names = (1..TOTAL_LEVELS).map { PixelPictures.forLevel(it).name }
        for (k in 1 until names.size) assertTrue("level ${k + 1} repeats ${names[k]}", names[k] != names[k - 1])
        val uses = names.groupingBy { it }.eachCount()
        println("picture uses over 5000 levels: min ${uses.values.min()}, max ${uses.values.max()}")
        assertEquals(PixelPictures.all.size, uses.size)
        // No picture comes back within 20 levels after the first lap.
        for (k in PixelPictures.all.size until names.size) {
            val window = names.subList(maxOf(0, k - 20), k)
            assertFalse("level ${k + 1} ${names[k]} seen in the last 20", names[k] in window)
        }
    }

    @Test
    fun lotShapesUnlockAndVary() {
        for (n in 1..TOTAL_LEVELS) {
            val s = ModeCurve.lotShape(n)
            assertTrue("level $n shape $s unlocked", s in ModeCurve.shapePool(n))
            if (n > 1) assertTrue("level $n repeats $s", s != ModeCurve.lotShape(n - 1) || s == Shape.RECT)
        }
        val late = (201..TOTAL_LEVELS).map { ModeCurve.lotShape(it) }.toSet()
        assertEquals(Shape.entries.toSet(), late)
    }

    @Test
    fun lateSuperHardLoopLevelsSometimesCloseAStall() {
        val tight = (150..TOTAL_LEVELS step 10).count { LoopLevels.get(it).openSlots == 3 }
        println("loop super hard levels with 3 stalls: $tight of ${(150..TOTAL_LEVELS step 10).count()}")
        assertTrue(tight > 0)
        assertTrue((1..149).all { LoopLevels.get(it).openSlots == 4 })
        assertEquals(2, ModeCurve.pixelOpenStops(200))
        assertEquals(3, ModeCurve.pixelOpenStops(205))
    }

    /** Sand lots (up to 26 trucks) build in every shape, including the new thin ones. */
    @Test
    fun sandLotsBuildInEveryShape() {
        for (n in (5..TOTAL_LEVELS step 41) + listOf(5000)) {
            val lot = LevelGenerator.custom(n, n * 9_173L + 12_345L) {
                DifficultyCurve.sized(vehicles = ModeCurve.sandTrucks(n, 5), colors = 1, shape = ModeCurve.lotShape(n), rotation = 0f, scramble = 0f)
            }
            assertTrue("sand $n ${ModeCurve.lotShape(n)}: ${lot.vehicles.size} trucks", lot.vehicles.size >= 6)
        }
    }

    @Test
    fun curvesStayInBoundsAndClimb() {
        for (n in 1..TOTAL_LEVELS) {
            assertTrue(ModeCurve.loopVehicles(n, 1) in 4..20)
            assertTrue(ModeCurve.loopScramble(n) in 0f..2.8f)
            assertTrue(ModeCurve.pixelBusCap(n) >= 10)
            assertTrue(ModeCurve.sandTrucks(n, 5) in 8..26)
            assertTrue(ModeCurve.sandBeltCapacity(n) in 5..6)
        }
        // Normal levels never get easier further in (peaks and breathers aside).
        fun normal(n: Int) = ModeCurve.tier(n) == Difficulty.NORMAL && !ModeCurve.breather(n)
        val normals = (1..TOTAL_LEVELS).filter(::normal)
        for ((a, b) in normals.zipWithNext()) {
            assertTrue("loop $a→$b", ModeCurve.loopVehicles(b, 0) >= ModeCurve.loopVehicles(a, 0))
            assertTrue("pixel $a→$b", ModeCurve.pixelBusCap(b) <= ModeCurve.pixelBusCap(a))
            assertTrue("sand $a→$b", ModeCurve.sandTrucks(b, 3) >= ModeCurve.sandTrucks(a, 3))
        }
    }

    @Test
    fun modesStopAtFiveThousand() {
        val fresh = PlayerState()
        assertEquals(1, fresh.modeLevel(GameMode.LOOP))
        assertFalse(fresh.modeFinished(GameMode.LOOP))
        val atEnd = PlayerState(modeLevels = mapOf(GameMode.PIXEL to TOTAL_LEVELS))
        assertEquals(TOTAL_LEVELS, atEnd.modeLevel(GameMode.PIXEL))
        assertFalse(atEnd.modeFinished(GameMode.PIXEL))
        val done = PlayerState(modeLevels = mapOf(GameMode.SAND to TOTAL_LEVELS + 1))
        assertEquals(TOTAL_LEVELS, done.modeLevel(GameMode.SAND))
        assertTrue(done.modeFinished(GameMode.SAND))
    }

    /** Not a check: prints the curve at milestone levels for the design notes. */
    @Test
    fun printMilestones() {
        // Normal levels (ending in 2-4, 7-9) show the base curve; 5x are Hard, x0 Super Hard.
        val levels = listOf(1, 2, 3, 4, 7, 9, 12, 17, 22, 32, 42, 52, 62, 82, 102, 152, 202, 302, 402, 502, 802, 1002, 1502, 2002, 3002, 4002, 4998)
        println("level | tier | loopVeh | loopCol | loopScr | loopMys | pixelCap | sandTrucks(5c) | sandJit | belt")
        for (n in levels) {
            val tier = if (ModeCurve.breather(n)) "breather" else ModeCurve.tier(n).name
            val v = ModeCurve.loopVehicles(n, 0)
            println(
                "$n | $tier | $v | ${ModeCurve.loopColors(n, v)} | ${"%.1f".format(ModeCurve.loopScramble(n))} | " +
                    "${"%.2f".format(ModeCurve.loopMystery(n))} | ${ModeCurve.pixelBusCap(n)} | ${ModeCurve.sandTrucks(n, 5)} | " +
                    "${"%.2f".format(ModeCurve.sandOrderJitter(n))} | ${ModeCurve.sandBeltCapacity(n)}",
            )
        }
        for (n in listOf(1, 11, 50, 51, 55, 56, 60, 300, 305, 310)) {
            val l = LoopLevels.get(n)
            val pic = PixelPictures.forLevel(n)
            val buses = PixelEngine.plan(pic, n).flatten()
            println("built $n: loop ${l.vehicles.size} veh / ${l.colorCount} colors / ${l.queue.size} riders; pixel ${pic.name} ${pic.colors(false).size} colors -> ${buses.size} buses")
        }
    }
}
