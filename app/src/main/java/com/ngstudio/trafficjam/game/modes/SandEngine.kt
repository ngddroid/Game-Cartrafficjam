package com.ngstudio.trafficjam.game.modes

import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.ngstudio.trafficjam.BuildConfig
import com.ngstudio.trafficjam.core.audio.Haptic
import com.ngstudio.trafficjam.core.audio.Sfx
import com.ngstudio.trafficjam.game.engine.GameFx
import com.ngstudio.trafficjam.game.gen.DifficultyCurve
import com.ngstudio.trafficjam.game.gen.Grid
import com.ngstudio.trafficjam.game.gen.LevelGenerator
import com.ngstudio.trafficjam.game.gen.ModeCurve
import com.ngstudio.trafficjam.game.model.Difficulty
import com.ngstudio.trafficjam.game.model.Level
import com.ngstudio.trafficjam.game.model.VehicleSpec
import com.ngstudio.trafficjam.game.modes.ModeArt.vehicleSprite
import com.ngstudio.trafficjam.game.modes.ModeDraw.headingVec
import com.ngstudio.trafficjam.game.modes.ModeDraw.label
import com.ngstudio.trafficjam.game.render.Skin
import com.ngstudio.trafficjam.game.render.VehicleArt
import com.ngstudio.trafficjam.game.render.jamColor
import com.ngstudio.trafficjam.game.render.renderBitmap
import com.ngstudio.trafficjam.game.render.shade
import com.ngstudio.trafficjam.game.render.tint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Sand Sweep: a picture made of colored sand hangs above a looping conveyor belt. Tapping a
 * free truck in the jam below sends it onto the belt; as it rides under the picture it scoops
 * sand of its own color from the bottom edge, and the sand above slumps down to fill the gap.
 */
class SandEngine(level: Int, private val skin: Skin, fx: GameFx) : ModeEngine(level, fx) {

    private val setup = build(level)
    val picture = setup.picture
    private val lot: Level = setup.lot
    override val difficulty: Difficulty = lot.difficulty

    // --- Sand ---------------------------------------------------------------------------------

    /** -1 for empty, otherwise color * 8 + shade. */
    private val grains = setup.grains.copyOf()
    private val total = grains.count { it >= 0 }
    private var left = total
    private val pixels = IntArray(GW * GH)
    private val bitmap: Bitmap = Bitmap.createBitmap(GW, GH, Bitmap.Config.ARGB_8888)
    private val image: ImageBitmap = bitmap.asImageBitmap()
    private val palette = IntArray(8 * 8) { i ->
        val c = jamColor(i / 8)
        val f = 0.66f + (i % 8) * 0.06f
        (if (f > 1f) c.tint((f - 1f) * 1.8f) else c.shade(f)).toArgb()
    }
    private val emptyArgb = EMPTY.toArgb()
    private var sandMoving = false
    private var sweep = false
    /** True when grains changed since the sand bitmap was last refreshed. */
    private var sandDirty = true

    override val counter: Int get() = if (left == 0) 0 else max(1, ceil(left * 100f / total).toInt())
    override val counterLabel = "% Sand"
    override val reviveTitle = "Belt Jammed!"
    override val reviveMessage = "None of the trucks on the belt can reach their sand.\nGet more room to keep going!"

    // --- Vehicles -----------------------------------------------------------------------------

    /**
     * LOT → DRIVING (out of the lot to the merge lane) → WAITING (queued in the lane) →
     * MERGING (curving up onto the belt) → BELT → LEAVING (full, speeding off) → GONE.
     */
    enum class TState { LOT, DRIVING, WAITING, MERGING, BELT, LEAVING, GONE }

    class Truck(val spec: VehicleSpec, val cap: Int) {
        val color = spec.color
        val type = spec.type
        var state = TState.LOT
        var eaten = 0
        var pos = Offset.Zero
        var home = Offset.Zero
        var heading = 0f
        var scale = 1f
        var b = 0f
        var t = 0f
        var duration = 1f
        var measure: PathMeasure? = null
        var pathLen = 0f
        var startScale = 1f
        var bumpT = -1f
        var bumpPx = 0f
        var bumped = false
        var shakeT = -1f
        var popT = 0f
        var popDelay = 0f
        var bounceT = -1f
        var eatBudget = 0f
        var idle = 0f
        var leaveV = 0f
        var smokeClock = 0f
        /** Order of arrival in the merge lane. */
        var seq = 0
        var mergeFrom = Offset.Zero
        val full get() = eaten >= cap
    }

    /** A grain pouring from the bottom of the picture into a van's bay. */
    private class Flyer(val color: Int, val light: Boolean, val from: Offset, val truck: Truck, val dx: Float, val dur: Float) {
        var t = 0f
    }

    private val trucks: List<Truck> = lot.vehicles.map { Truck(it, setup.caps[it.id]) }
    private val byId = trucks.associateBy { it.spec.id }
    private val grid = Grid(lot.cols, lot.rows).also { g -> lot.vehicles.forEach { g.put(it) } }
    private val flyers = ArrayList<Flyer>()
    private val rnd = Random(level * 31 + 9)

    var capacity = ModeCurve.sandBeltCapacity(level)
        private set
    val onBelt get() = trucks.count {
        it.state == TState.DRIVING || it.state == TState.WAITING || it.state == TState.MERGING || (it.state == TState.BELT && !it.full)
    }
    private var seqCounter = 0

    // --- Layout -------------------------------------------------------------------------------

    private var grain = 0f
    private var picLeft = 0f
    private var picTop = 0f
    private var picBottom = 0f
    private var beltTop = 0f
    private var beltBottom = 0f
    private var beltY = 0f
    private var beltLen = 0f
    private var margin = 0f
    private var entryX = 0f
    private var lotRect = Rect.Zero
    private var track = Rect.Zero
    private var unit = 0f
    private var boardCenter = Offset.Zero
    private var beltScale = 0.8f
    private var pill = Rect.Zero
    private var plus = Rect.Zero
    private lateinit var art: VehicleArt
    private lateinit var vans: SandVan
    /** Lot trucks from back to front, so nearer ones overlap farther ones. */
    private var lotOrder: List<Truck> = emptyList()
    private var scene: ImageBitmap? = null

    private fun boardToScreen(gx: Float, gy: Float) =
        Offset(boardCenter.x + (gx - lot.cols / 2f) * unit, boardCenter.y + (gy - lot.rows / 2f) * unit)

    private fun screenToBoard(p: Offset) =
        Offset((p.x - boardCenter.x) / unit + lot.cols / 2f, (p.y - boardCenter.y) / unit + lot.rows / 2f)

    private fun beltX(b: Float) = b - margin

    override fun layout() {
        grain = (width - 24 * u) / GW
        picLeft = 12 * u
        picTop = topInset + 58 * u
        picBottom = picTop + grain * GH
        beltTop = picBottom + 2 * u
        beltBottom = beltTop + 42 * u
        beltY = (beltTop + beltBottom) / 2
        margin = 70 * u
        beltLen = width + margin * 2
        entryX = 64 * u
        val pillY = beltBottom + 33 * u
        pill = Rect(width - 104 * u, pillY - 13 * u, width - 16 * u, pillY + 13 * u)
        plus = Rect(pill.right - 26 * u, pill.top + 2 * u, pill.right - 2 * u, pill.bottom - 2 * u)
        lotRect = Rect(10 * u, pill.bottom + 12 * u, width - 10 * u, height - bottomInset - 14 * u)
        track = Rect(lotRect.left + 14 * u, lotRect.top + 10 * u, lotRect.right - 14 * u, lotRect.bottom - 10 * u)
        val availW = lotRect.width - 36 * u
        val availH = lotRect.height - 30 * u
        unit = min(min(availW / lot.cols, availH / lot.rows), 23 * u)
        boardCenter = lotRect.center
        art = VehicleArt(unit, skin)
        beltScale = min(1f, (34 * u) / (unit * 2))
        vans = SandVan(art.bodyW() * beltScale)
        pourPaint.strokeWidth = grain * 1.3f
        pillFace = Brush.verticalGradient(listOf(Color(0xFF2A3F78), Color(0xFF17264F)), pill.top, pill.bottom)
        scene = renderBitmap(width.toInt(), height.toInt()) { paintScene() }
        trucks.forEachIndexed { i, v ->
            v.home = boardToScreen(v.spec.centerX, v.spec.centerY)
            if (v.state == TState.LOT) {
                v.pos = v.home
                v.heading = v.spec.dir.degrees
            }
            v.popDelay = 0.15f + i * min(0.03f, 0.8f / trucks.size)
            art.composite(v.type, v.color, false, v.heading)
            vans.look(v.type, v.color, length(v))
        }
        lotOrder = trucks.sortedBy { it.home.y }
    }

    // --- Input --------------------------------------------------------------------------------

    override fun onTap(p: Offset) {
        if (!ready || phase != ModePhase.PLAYING) return
        particles.ring(p)
        if (pill.inflate(6 * u).contains(p)) {
            fx.sfx(Sfx.Click)
            if (capacity < MAX_CAPACITY) onEvent(ModeEvent.LockedSpot(0)) else onEvent(ModeEvent.Toast("The belt is as long as it gets!"))
            return
        }
        val v = hit(p) ?: return
        fx.haptic(Haptic.Tap)
        if (v.bumpT >= 0f || v.popT < 1f) return
        val d = grid.blockDistance(v.spec)
        if (d >= 0) {
            v.bumpT = 0f
            v.bumped = false
            v.bumpPx = max(0.2f, d + 0.1f) * unit
            fx.sfx(Sfx.Tap)
            return
        }
        if (onBelt >= capacity) {
            v.shakeT = 0f
            fx.sfx(Sfx.Warning)
            fx.haptic(Haptic.Bump)
            onEvent(ModeEvent.Toast("The belt is full!"))
            return
        }
        drive(v)
    }

    private fun hit(p: Offset): Truck? {
        val b = screenToBoard(p)
        if (b.x >= 0 && b.y >= 0) {
            val id = grid.at(b.x.toInt(), b.y.toInt())
            if (id >= 0) return byId[id]
        }
        var best: Truck? = null
        var bestD = 0.8f
        for (v in trucks) {
            if (v.state != TState.LOT) continue
            val s = v.spec
            val dx = max(0f, max(s.x - b.x, b.x - (s.x + s.w)))
            val dy = max(0f, max(s.y - b.y, b.y - (s.y + s.h)))
            val dd = kotlin.math.hypot(dx, dy)
            if (dd < bestD) { bestD = dd; best = v }
        }
        return best
    }

    private fun drive(v: Truck) {
        grid.remove(v.spec)
        v.state = TState.DRIVING
        v.t = 0f
        v.startScale = v.scale
        val pts = exitPoints(v.pos, headingVec(v.heading))
        val path = ModeDraw.roundedPath(pts, 16 * u)
        v.measure = PathMeasure().apply { setPath(path, false) }
        v.pathLen = v.measure!!.length
        v.duration = (0.35f + v.pathLen / (1100 * u)).coerceIn(0.55f, 1.5f)
        particles.smoke(v.pos - headingVec(v.heading) * (art.bodyL(v.type) / 2), -headingVec(v.heading).x, -headingVec(v.heading).y, 5)
        fx.sfx(Sfx.EngineGo, 0.95f + rnd.nextFloat() * 0.15f)
    }

    /** Out of the lot along the truck's arrow, round the lot edge, and up onto the belt. */
    private fun exitPoints(start: Offset, d: Offset): List<Offset> {
        val t = track
        val tx = when {
            d.x > 1e-4f -> (t.right - start.x) / d.x
            d.x < -1e-4f -> (t.left - start.x) / d.x
            else -> Float.MAX_VALUE
        }
        val ty = when {
            d.y > 1e-4f -> (t.bottom - start.y) / d.y
            d.y < -1e-4f -> (t.top - start.y) / d.y
            else -> Float.MAX_VALUE
        }
        val k = min(tx, ty)
        val hitP = Offset((start.x + d.x * k).coerceIn(t.left, t.right), (start.y + d.y * k).coerceIn(t.top, t.bottom))
        val pts = arrayListOf(start, hitP)
        when {
            ty <= tx && d.y < 0 -> Unit
            ty <= tx && d.y > 0 -> {
                val goLeft = (hitP.x - t.left) < (t.right - hitP.x)
                val sx = if (goLeft) t.left else t.right
                pts += Offset(sx, t.bottom)
                pts += Offset(sx, t.top)
            }
            tx < ty && d.x > 0 -> pts += Offset(t.right, t.top)
            else -> pts += Offset(t.left, t.top)
        }
        pts += Offset(t.left, t.top)
        // This truck is already DRIVING, so it counts itself; the lane spot is one less.
        pts += Offset(t.left, laneY)
        pts += laneSpot(trucks.count { it.state == TState.DRIVING || it.state == TState.WAITING } - 1)
        val out = ArrayList<Offset>()
        for (p in pts) if (out.isEmpty() || (out.last() - p).getDistance() > 2f) out += p
        return out
    }

    /** The merge lane runs left to right just under the belt; trucks queue there facing east. */
    private val laneY get() = beltBottom + 12 * u + art.bodyW() * beltScale / 2
    private val laneSpacing get() = art.bodyL(com.ngstudio.trafficjam.game.model.VehicleType.BUS) * beltScale + 6 * u
    /** The lane ends at the middle of the screen, so trucks merge onto the belt at the center. */
    private val laneEnd get() = width / 2
    private val laneFront get() = laneEnd - 54 * u

    /** Queue position [k]: 0 is the front, nearest the merge point. */
    private fun laneSpot(k: Int) = Offset(laneFront - k * laneSpacing, laneY)

    private val mergeEnd get() = Offset(laneEnd + 20 * u, beltY)

    override fun unlockSpot(index: Int) {
        if (capacity >= MAX_CAPACITY) return
        capacity++
        fx.sfx(Sfx.Unlock)
        particles.stars(pill.center, 14)
        if (phase == ModePhase.STUCK) phase = ModePhase.PLAYING
        stuckTimer = 0f
    }

    override fun onRevive() {
        if (capacity < MAX_CAPACITY) {
            unlockSpot(0)
            return
        }
        // Belt maxed out: lift the trucks that have nothing to scoop back to their lot spots.
        for (v in trucks) {
            if (v.state != TState.BELT && v.state != TState.WAITING && v.state != TState.MERGING) continue
            if (v.full || canEat(v.color)) continue
            v.state = TState.LOT
            v.pos = v.home
            v.heading = v.spec.dir.degrees
            v.scale = 1f
            grid.put(v.spec)
            particles.stars(v.home, 8)
        }
        fx.sfx(Sfx.Crane)
        stuckTimer = 0f
    }

    // --- Frame --------------------------------------------------------------------------------

    private var stuckTimer = 0f
    private var winTimer = -1f
    private var popIndex = 0

    override fun step(dt: Float) {
        val t0 = if (BuildConfig.DEBUG && DebugFlags.perf) System.nanoTime() else 0L
        if (BuildConfig.DEBUG && DebugFlags.autoplay) autoplay(dt)
        settle()
        settle()
        val t1 = if (BuildConfig.DEBUG && DebugFlags.perf) System.nanoTime() else 0L
        updateTrucks(dt)
        updateFlyers(dt)
        checkEnd(dt)
        if (BuildConfig.DEBUG && DebugFlags.perf) perf?.step(t1 - t0, System.nanoTime() - t1)
    }

    private var autoClock = 0f

    /** Debug only: sends a free truck to the belt every so often while there is room. */
    private fun autoplay(dt: Float) {
        autoClock += dt
        if (autoClock < 0.3f || phase != ModePhase.PLAYING || !ready) return
        autoClock = 0f
        if (onBelt >= capacity) return
        val v = trucks.firstOrNull { it.state == TState.LOT && it.popT >= 1f && grid.blockDistance(it.spec) < 0 } ?: return
        drive(v)
    }

    private val perf = if (BuildConfig.DEBUG) PerfLog("SandPerf") else null

    /** One pass of falling sand: straight down if free, else slide diagonally. */
    private fun settle() {
        var moved = false
        sweep = !sweep
        for (y in GH - 2 downTo 0) {
            val row = y * GW
            val below = row + GW
            for (k in 0 until GW) {
                val x = if (sweep) k else GW - 1 - k
                val g = grains[row + x]
                if (g < 0) continue
                if (grains[below + x] < 0) {
                    grains[below + x] = g; grains[row + x] = -1; moved = true
                    continue
                }
                val lFree = x > 0 && grains[below + x - 1] < 0 && grains[row + x - 1] < 0
                val rFree = x < GW - 1 && grains[below + x + 1] < 0 && grains[row + x + 1] < 0
                if (!lFree && !rFree) continue
                // Sand only sometimes slides sideways, so the picture sags instead of melting.
                if (rnd.nextFloat() > SLIDE_CHANCE) continue
                val first = if ((x + y + (if (sweep) 1 else 0)) % 2 == 0) -1 else 1
                for (dx in intArrayOf(first, -first)) {
                    val nx = x + dx
                    if (nx < 0 || nx >= GW) continue
                    if (grains[below + nx] < 0 && grains[row + nx] < 0) {
                        grains[below + nx] = g; grains[row + x] = -1; moved = true
                        break
                    }
                }
            }
        }
        sandMoving = moved
        if (moved) sandDirty = true
    }

    /** Bottom grain of [color] within suction reach in column [x], or -1. */
    private fun reachable(x: Int, color: Int): Int {
        for (y in GH - 1 downTo GH - SUCK_DEPTH) {
            val g = grains[y * GW + x]
            if (g >= 0 && g / 8 == color) return y * GW + x
        }
        return -1
    }

    private fun canEat(color: Int): Boolean {
        for (x in 0 until GW) if (reachable(x, color) >= 0) return true
        return false
    }

    private fun updateTrucks(dt: Float) {
        for (v in trucks) {
            if (v.popT < 1f && time > v.popDelay) v.popT = min(1f, v.popT + dt / 0.35f)
            if (v.bounceT >= 0f) { v.bounceT += dt / 0.25f; if (v.bounceT >= 1f) v.bounceT = -1f }
            if (v.shakeT >= 0f) { v.shakeT += dt / 0.35f; if (v.shakeT >= 1f) v.shakeT = -1f }
            when (v.state) {
                TState.LOT -> lotIdle(v, dt)
                TState.DRIVING -> {
                    v.t += dt
                    val p = (v.t / v.duration).coerceIn(0f, 1f)
                    val e = 1f - (1f - p).pow(2.1f)
                    val m = v.measure!!
                    val d = v.pathLen * e
                    v.pos = m.getPosition(d)
                    val tan = m.getTangent(d)
                    if (tan.getDistance() > 0.01f) v.heading = atan2(tan.x, -tan.y) * 180f / PI.toFloat()
                    v.scale = v.startScale + (beltScale - v.startScale) * min(1f, p * 1.4f)
                    v.smokeClock += dt
                    if (v.smokeClock > 0.07f && p < 0.85f) {
                        v.smokeClock = 0f
                        particles.smoke(v.pos - headingVec(v.heading) * (art.bodyL(v.type) * v.scale / 2), count = 1)
                    }
                    if (p >= 1f) {
                        v.state = TState.WAITING
                        v.scale = beltScale
                        v.seq = seqCounter++
                        // From here on it is drawn as the side-on van: hide the swap in a hop and a puff.
                        v.bounceT = 0f
                        particles.smoke(v.pos, count = 3)
                    }
                }
                TState.WAITING -> {
                    // Creep up the lane to this truck's place in the queue.
                    val rank = trucks.count { it.state == TState.WAITING && it.seq < v.seq }
                    val target = laneSpot(rank)
                    val d = target - v.pos
                    val dist = d.getDistance()
                    val stepLen = 160 * u * dt
                    v.pos = if (dist <= stepLen) target else v.pos + d / dist * stepLen
                    v.heading += ModeDraw.shortestAngle(v.heading, 90f) * min(1f, dt * 9f)
                    if (abs(ModeDraw.shortestAngle(v.heading, 90f)) < 0.5f) v.heading = 90f
                    val merging = trucks.any { it.state == TState.MERGING }
                    val settled = abs(ModeDraw.shortestAngle(v.heading, 90f)) < 4f
                    if (rank == 0 && dist < 1f && settled && !merging && gapAt(mergeEnd.x + margin, v, BELT_SPEED * u * MERGE_TIME)) {
                        v.state = TState.MERGING
                        v.t = 0f
                        v.mergeFrom = v.pos
                    }
                }
                TState.MERGING -> {
                    // Lane change: an S-curve from the lane up onto the belt, ending at belt speed.
                    v.t += dt
                    val p = (v.t / MERGE_TIME).coerceIn(0f, 1f)
                    val e = p * (2 - p)
                    val a = v.mergeFrom
                    val b = mergeEnd
                    val c1 = Offset(a.x + (b.x - a.x) * 0.45f, a.y)
                    val c2 = Offset(a.x + (b.x - a.x) * 0.55f, b.y)
                    val q = 1 - e
                    v.pos = a * (q * q * q) + c1 * (3 * q * q * e) + c2 * (3 * q * e * e) + b * (e * e * e)
                    val tan = (c1 - a) * (3 * q * q) + (c2 - c1) * (6 * q * e) + (b - c2) * (3 * e * e)
                    if (tan.getDistance() > 0.01f) v.heading = atan2(tan.x, -tan.y) * 180f / PI.toFloat()
                    if (p >= 1f) {
                        v.state = TState.BELT
                        v.b = b.x + margin
                        v.heading = 90f
                        v.bounceT = 0f
                        fx.sfx(Sfx.Park)
                        particles.smoke(v.pos - Offset(art.bodyL(v.type) * v.scale / 2, 0f), -1f, 0f, 2)
                    }
                }
                TState.BELT -> {
                    v.b = (v.b + BELT_SPEED * u * dt) % beltLen
                    // A small rattle as the belt carries it along.
                    v.pos = Offset(beltX(v.b), beltY + sin(time * 26f + v.spec.id * 1.7f) * 0.35f * u)
                    v.heading = 90f
                    eat(v, dt)
                }
                TState.LEAVING -> {
                    // Pulls out into the fast lane just above the belt and races off.
                    v.leaveV += 700 * u * dt
                    v.t += dt
                    val hop = min(1f, v.t / 0.25f)
                    v.pos = Offset(v.pos.x + v.leaveV * dt, beltY - hop * 9 * u)
                    v.smokeClock += dt
                    if (v.smokeClock > 0.05f) {
                        v.smokeClock = 0f
                        particles.smoke(v.pos - Offset(art.bodyL(v.type) * v.scale / 2, 0f), -1f, 0f, 1)
                    }
                    if (v.pos.x > width + 120 * u) v.state = TState.GONE
                }
                TState.GONE -> Unit
            }
        }
    }

    /** Blocked trucks lurch to their blocker and spring back, like the classic lot. */
    private fun lotIdle(v: Truck, dt: Float) {
        var off = 0f
        if (v.bumpT >= 0f) {
            v.bumpT += dt
            val hitT = 0.1f
            if (v.bumpT < hitT) {
                val p = v.bumpT / hitT
                off = v.bumpPx * p * p
            } else {
                if (!v.bumped) {
                    v.bumped = true
                    fx.sfx(Sfx.Bump, 0.9f + rnd.nextFloat() * 0.2f)
                    fx.haptic(Haptic.Bump)
                    val hv = headingVec(v.heading)
                    particles.sparks(v.home + hv * (v.bumpPx + art.bodyL(v.type) / 2), 8)
                    shakeScreen()
                    val blocker = grid.blocker(v.spec)
                    if (blocker >= 0) byId[blocker]?.shakeT = 0f
                }
                val tt = v.bumpT - hitT
                off = v.bumpPx * exp(-9f * tt) * cos(20f * tt)
                if (tt > 0.38f) { v.bumpT = -1f; off = 0f }
            }
        }
        v.pos = v.home + headingVec(v.heading) * off
    }

    private fun length(v: Truck) = art.bodyL(v.type) * beltScale

    /** True when a truck merging at belt position [b] leaves room front and back. */
    private fun gapAt(b: Float, me: Truck, ahead: Float = 0f): Boolean {
        for (o in trucks) {
            if (o === me || o.state != TState.BELT || o.full) continue
            var d = abs((o.b + ahead) % beltLen - b)
            d = min(d, beltLen - d)
            if (d < (length(o) + length(me)) / 2 + GAP * u) return false
        }
        return true
    }

    private fun eat(v: Truck, dt: Float) {
        if (v.full) return
        v.eatBudget += dt * EAT_RATE
        val half = length(v) / 2
        val gx0 = ((v.pos.x - half - picLeft) / grain).toInt().coerceAtLeast(0)
        val gx1 = ((v.pos.x + half - picLeft) / grain).toInt().coerceAtMost(GW - 1)
        val cx = ((v.pos.x - picLeft) / grain)
        var ate = false
        while (v.eatBudget >= 1f && !v.full && gx0 <= gx1) {
            var best = -1
            var bestD = Float.MAX_VALUE
            for (x in gx0..gx1) {
                val i = reachable(x, v.color)
                if (i >= 0) {
                    val d = abs(x - cx) + (GH - 1 - i / GW) * 2f + rnd.nextFloat() * 3f
                    if (d < bestD) { bestD = d; best = i }
                }
            }
            if (best < 0) break
            grains[best] = -1
            sandDirty = true
            left--
            v.eaten++
            v.eatBudget -= 1f
            ate = true
            if (flyers.size < MAX_FLYERS && rnd.nextFloat() < 0.45f) {
                val look = vans.look(v.type, v.color, length(v))
                flyers += Flyer(
                    v.color, rnd.nextBoolean(),
                    Offset(picLeft + (best % GW + 0.5f) * grain, picTop + (best / GW + 0.5f) * grain), v,
                    (rnd.nextFloat() - 0.5f) * look.bay.width * 0.5f, 0.24f + rnd.nextFloat() * 0.12f,
                )
            }
        }
        if (!ate) v.eatBudget = min(v.eatBudget, 1f)
        if (ate) {
            v.idle = 0f
            if (popIndex++ % 9 == 0) fx.sfx(Sfx.Pop1, 0.9f + v.eaten.toFloat() / v.cap * 0.3f)
        }
        if (v.full) {
            v.state = TState.LEAVING
            v.leaveV = BELT_SPEED * u
            v.t = 0f
            v.bounceT = 0f
            fx.sfx(Sfx.Full)
            fx.sfx(Sfx.Horn, 1.05f)
            particles.stars(v.pos, 10)
        }
    }

    private fun updateFlyers(dt: Float) {
        val it = flyers.iterator()
        while (it.hasNext()) {
            val f = it.next()
            f.t += dt / f.dur
            if (f.t >= 1f) it.remove()
        }
    }

    private fun checkEnd(dt: Float) {
        if (phase != ModePhase.PLAYING) return
        if (left == 0) {
            if (trucks.all { it.state == TState.GONE || it.state == TState.LOT }) {
                if (winTimer < 0f) winTimer = 0f
                winTimer += dt
                if (winTimer > 0.4f) win()
            }
            return
        }
        val busy = sandMoving || flyers.isNotEmpty() || trucks.any { it.state == TState.DRIVING || it.state == TState.MERGING || it.bumpT >= 0f }
        val belt = trucks.filter { (it.state == TState.BELT || it.state == TState.WAITING || it.state == TState.MERGING) && !it.full }
        val lotLeft = trucks.any { it.state == TState.LOT }
        val canAdd = lotLeft && onBelt < capacity
        if (busy || canAdd || belt.any { canEat(it.color) }) { stuckTimer = 0f; return }
        stuckTimer += dt
        if (stuckTimer > 1.0f) { stuckTimer = 0f; stuck() }
    }

    // --- Drawing ------------------------------------------------------------------------------

    override fun draw(scope: DrawScope, t: Float) {
        val t0 = if (BuildConfig.DEBUG && DebugFlags.perf) System.nanoTime() else 0L
        drawFrame(scope)
        if (BuildConfig.DEBUG && DebugFlags.perf) perf?.draw(System.nanoTime() - t0, onBelt)
    }

    private fun drawFrame(scope: DrawScope) = with(scope) {
        val bg = scene ?: return@with
        // Only recolor and re-upload the sand picture when grains actually moved or were scooped.
        if (sandDirty) {
            recolor()
            bitmap.setPixels(pixels, 0, GW, 0, 0, GW, GH)
            sandDirty = false
        }
        translate(screenShake.x, screenShake.y) {
            drawImage(bg)
            drawImage(
                image, IntOffset.Zero, IntSize(GW, GH),
                IntOffset(picLeft.roundToInt(), picTop.roundToInt()), IntSize((grain * GW).roundToInt(), (grain * GH).roundToInt()),
                filterQuality = FilterQuality.Low,
            )
            drawBelt()
            drawPill()

            // Plain loops over the trucks: no per-frame lists or sorting.
            for (v in trucks) if (v.state == TState.BELT) truck(v)
            drawPour()
            for (v in trucks) if (v.state == TState.BELT) fillTag(v)
            for (v in trucks) if (v.state == TState.WAITING) truck(v)
            for (v in lotOrder) if (v.state == TState.LOT) truck(v)
            for (v in trucks) if (v.state == TState.DRIVING || v.state == TState.MERGING) truck(v)
            for (v in trucks) {
                if (v.state != TState.LEAVING) continue
                // Speed lines streaming off the back.
                val back = v.pos.x - length(v) / 2
                val k = ((v.leaveV / u - BELT_SPEED) / 300f).coerceIn(0f, 1f)
                for (j in -1..1) {
                    val y = v.pos.y + j * 6 * u
                    drawLine(Color.White.copy(alpha = 0.7f * k), Offset(back - 6 * u, y), Offset(back - (18 + 22 * k) * u - abs(j) * 8 * u, y), 2 * u, StrokeCap.Round)
                }
                truck(v)
            }
            particles.draw(this)
        }
    }

    /** Grain colors into pixels; the top grain of every heap catches the light. */
    private fun recolor() {
        for (i in grains.indices) {
            val g = grains[i]
            pixels[i] = when {
                g < 0 -> emptyArgb
                i >= GW && grains[i - GW] < 0 -> palette[g or 7]
                i >= GW * 2 && grains[i - GW * 2] < 0 -> palette[min(g + 2, g or 7)]
                else -> palette[g]
            }
        }
    }

    private val pourPaint = android.graphics.Paint().apply { strokeCap = android.graphics.Paint.Cap.SQUARE }
    private var pourPts = FloatArray(MAX_FLYERS * 2)

    /** The streams of sand falling from the picture into the vans, one batched call per shade. */
    private fun DrawScope.drawPour() {
        if (flyers.isEmpty()) return
        var mask = 0
        for (f in flyers) mask = mask or (1 shl (f.color * 2 + if (f.light) 1 else 0))
        drawIntoCanvas { canvas ->
            val nc = canvas.nativeCanvas
            for (key in 0 until 16) {
                if (mask and (1 shl key) == 0) continue
                val color = key / 2
                val light = key % 2 == 1
                var n = 0
                for (f in flyers) {
                    if (f.color != color || f.light != light) continue
                    val v = f.truck
                    val look = vans.look(v.type, v.color, length(v))
                    val to = v.pos + vans.mouth(look) + Offset(f.dx, 0f)
                    val p = f.t.coerceIn(0f, 1f)
                    pourPts[n++] = f.from.x + (to.x - f.from.x) * p
                    pourPts[n++] = f.from.y + (to.y - f.from.y) * p * p
                }
                val c = jamColor(color)
                pourPaint.color = (if (light) c.tint(0.45f) else c.tint(0.12f)).toArgb()
                nc.drawPoints(pourPts, 0, n, pourPaint)
            }
        }
    }

    private fun DrawScope.truck(v: Truck) {
        val pop = if (v.popT >= 1f) 1f else ModeDraw.easeOutBack(v.popT)
        if (pop <= 0.01f) return
        val bounce = if (v.bounceT >= 0f) 1f + 0.07f * sin(v.bounceT * PI.toFloat()) else 1f
        if (v.state == TState.WAITING || v.state == TState.MERGING || v.state == TState.BELT || v.state == TState.LEAVING) {
            // On the road it is the side-on van with its open bay, like a real sand truck.
            val look = vans.look(v.type, v.color, length(v))
            val tilt = (ModeDraw.shortestAngle(90f, v.heading) * 0.45f).coerceIn(-16f, 16f)
            val fill = v.eaten.toFloat() / v.cap
            with(vans) { draw(look, v.pos, v.scale / beltScale * pop * bounce, tilt, fill) }
            return
        }
        val shake = if (v.shakeT >= 0f) sin(v.shakeT * 38f) * 6f * (1f - v.shakeT) else 0f
        val sc = v.scale * pop * bounce
        // Heading changes every frame while driving out or shaking: draw it live.
        val turning = v.state == TState.DRIVING || v.shakeT >= 0f
        vehicleSprite(art, v.type, v.color, v.pos, v.heading + shake, sc, live = turning)
    }

    /**
     * The belt is a light road in a navy tray. Its dashed center line slides at exactly belt
     * speed, so the trucks visibly ride on it. Everything else about it is in the scene bitmap.
     */
    private fun DrawScope.drawBelt() {
        val dash = 22 * u
        val off = (time * BELT_SPEED * u) % dash
        var d = -dash + off
        val dashCol = Color.White.copy(alpha = 0.9f)
        while (d < width) {
            drawLine(dashCol, Offset(d, beltY), Offset(d + dash * 0.5f, beltY), 2.4f * u, StrokeCap.Round)
            d += dash
        }
    }

    /** End brackets where the belt curls under. */
    private fun DrawScope.beltBrackets() {
        for (left in listOf(true, false)) {
            val bx = if (left) 0f else width - 16 * u
            val r = Rect(bx, beltTop - 14 * u, bx + 16 * u, beltBottom + 14 * u)
            drawRoundRect(Color(0xFF1B2850), r.topLeft + Offset(0f, 3 * u), r.size, CornerRadius(8 * u))
            drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF3D5AA8), Color(0xFF26397A)), r.left, r.right), r.topLeft, r.size, CornerRadius(8 * u))
            drawRoundRect(Color.White, r.topLeft + Offset(3 * u, 3 * u), Size(r.width - 6 * u, r.height - 6 * u), CornerRadius(6 * u), style = Stroke(2 * u))
        }
    }

    private fun DrawScope.fillTag(v: Truck) {
        val pct = (v.eaten * 100 / v.cap)
        label("$pct%", Offset(v.pos.x, beltBottom + 8 * u), 10.5f * u, Color.White, Color(0xFF1B2850), 3 * u)
    }

    private var pillFace: Brush = Brush.verticalGradient(listOf(Color(0xFF2A3F78), Color(0xFF17264F)))

    private fun DrawScope.drawPill() {
        val r = pill
        val cr = CornerRadius(r.height / 2)
        drawRoundRect(Color(0xFF4FC3F7).copy(alpha = 0.35f), r.topLeft - Offset(3 * u, 3 * u), Size(r.width + 6 * u, r.height + 6 * u), CornerRadius(r.height / 2 + 3 * u))
        drawRoundRect(Color(0xFF0E1A3A), r.topLeft + Offset(0f, 2.5f * u), r.size, cr)
        drawRoundRect(pillFace, r.topLeft, r.size, cr)
        drawRoundRect(Color(0xFF4FC3F7), r.topLeft, r.size, cr, style = Stroke(2.5f * u))
        val full = onBelt >= capacity
        label("$onBelt/$capacity", Offset(r.left + (r.width - plus.width) / 2 + 2 * u, r.center.y), 14 * u, if (full) Color(0xFFFF8A80) else Color.White, Color(0xFF0E1A3A), 3 * u)
        if (capacity < MAX_CAPACITY) {
            val c = plus.center
            drawCircle(Color(0xFF2E7D32), plus.height / 2 + 1 * u, c + Offset(0f, 1 * u))
            drawCircle(Color(0xFF5FC12F), plus.height / 2, c)
            drawCircle(Color.White, plus.height / 2, c, style = Stroke(1.5f * u))
            drawLine(Color.White, c - Offset(5 * u, 0f), c + Offset(5 * u, 0f), 2.6f * u, StrokeCap.Round)
            drawLine(Color.White, c - Offset(0f, 5 * u), c + Offset(0f, 5 * u), 2.6f * u, StrokeCap.Round)
        }
    }

    private fun DrawScope.paintScene() {
        val w = size.width
        drawRect(Brush.verticalGradient(listOf(Color(0xFFDDE2EC), Color(0xFFCDD4E1))))
        // Thick white picture frame with a soft drop shadow.
        val frame = Rect(picLeft - 9 * u, picTop - 9 * u, picLeft + grain * GW + 9 * u, picBottom + 6 * u)
        drawRoundRect(Color(0x26000000), frame.topLeft + Offset(0f, 6 * u), frame.size, CornerRadius(24 * u))
        drawRoundRect(Color.White, frame.topLeft, frame.size, CornerRadius(24 * u))
        drawRoundRect(Color(0xFFC9D2E2), frame.topLeft, frame.size, CornerRadius(24 * u), style = Stroke(1.5f * u))
        drawRoundRect(EMPTY, Offset(picLeft, picTop), Size(grain * GW, picBottom - picTop), CornerRadius(14 * u, 14 * u))
        // Belt tray
        drawRect(Color(0xFF1B2850), Offset(0f, beltTop - 6 * u), Size(w, beltBottom - beltTop + 12 * u))
        drawRect(Color(0xFF2E4285), Offset(0f, beltTop - 6 * u), Size(w, 3 * u))
        drawRect(Brush.verticalGradient(listOf(Color(0xFFB3BBC8), Color(0xFF959FAF)), beltTop, beltBottom), Offset(0f, beltTop), Size(w, beltBottom - beltTop))
        drawRect(Color.White.copy(alpha = 0.85f), Offset(0f, beltTop + 1.5f * u), Size(w, 1.6f * u))
        drawRect(Color.White.copy(alpha = 0.85f), Offset(0f, beltBottom - 3 * u), Size(w, 1.6f * u))
        beltBrackets()
        // Merge lane: a light road from the lot up and along under the belt.
        val lw = art.bodyW() * beltScale + 12 * u
        val road = Brush.verticalGradient(listOf(Color(0xFFBCC4D0), Color(0xFFA9B2C0)))
        val laneTop = laneY - lw / 2
        drawRoundRect(Color(0x22000000), Offset(track.left - lw / 2, laneTop + 3 * u), Size(mergeEnd.x - track.left + lw / 2 - 20 * u, lw), CornerRadius(lw / 2))
        drawRoundRect(road, Offset(track.left - lw / 2, laneTop), Size(mergeEnd.x - track.left + lw / 2 - 20 * u, lw), CornerRadius(lw / 2))
        var cx = track.left + 26 * u
        while (cx < mergeEnd.x - 34 * u) {
            drawLine(Color.White, Offset(cx - 5 * u, laneY - 4.5f * u), Offset(cx, laneY), 2.2f * u, StrokeCap.Round)
            drawLine(Color.White, Offset(cx - 5 * u, laneY + 4.5f * u), Offset(cx, laneY), 2.2f * u, StrokeCap.Round)
            cx += 20 * u
        }
        // The lot is open ground: just a soft shadow under the jam so it doesn't float.
        val minX = trucks.minOf { boardToScreen(it.spec.x.toFloat(), 0f).x }
        val maxX = trucks.maxOf { boardToScreen((it.spec.x + it.spec.w).toFloat(), 0f).x }
        val minY = trucks.minOf { boardToScreen(0f, it.spec.y.toFloat()).y }
        val maxY = trucks.maxOf { boardToScreen(0f, (it.spec.y + it.spec.h).toFloat()).y }
        drawOval(
            Brush.radialGradient(listOf(Color(0x1A1B2850), Color.Transparent), Offset((minX + maxX) / 2, (minY + maxY) / 2), (maxX - minX) * 0.7f),
            Offset(minX - 20 * u, minY - 16 * u), Size(maxX - minX + 40 * u, maxY - minY + 32 * u),
        )
    }

    // --- Level --------------------------------------------------------------------------------

    private class Setup(val picture: PixelPicture, val grains: IntArray, val lot: Level, val caps: IntArray)

    companion object {
        /** The grey-blue behind the sand, showing wherever it has been scooped away. */
        private val EMPTY = Color(0xFF8E9AAC)
        const val GW = 180
        const val GH = 128
        const val MAX_CAPACITY = 8
        private const val BELT_SPEED = 52f
        private const val EAT_RATE = 330f
        private const val MERGE_TIME = 0.5f
        private const val GAP = 8f
        private const val SLIDE_CHANCE = 0.75f
        private const val MAX_FLYERS = 520
        /** How many rows up from the bottom a truck's suction reaches. */
        private const val SUCK_DEPTH = 5

        /**
         * Turns the pixel picture into fine sand: scales it up, rounds off the blocky steps
         * with a majority filter, then shades each grain with soft waves, grain noise and a
         * darker rim where two colors meet.
         */
        private fun paint(picture: PixelPicture, rng: Random): IntArray {
            // Leave a margin of background sand around the picture.
            val kf = min(GW.toFloat() / picture.w, GH.toFloat() / picture.h) * 0.86f
            val ox = (GW - picture.w * kf) / 2f
            val oy = (GH - picture.h * kf) / 2f
            fun colorAt(px: Int, py: Int): Int {
                val c = if (px in 0 until picture.w && py in 0 until picture.h) picture.at(px, py) else -1
                return if (c < 0) picture.background else c
            }
            // Bilinear vote between the four nearest picture pixels turns stair steps into
            // straight diagonals and rounded corners.
            val votes = FloatArray(8)
            var base = IntArray(GW * GH) { i ->
                val fx = (i % GW + 0.5f - ox) / kf - 0.5f
                val fy = (i / GW + 0.5f - oy) / kf - 0.5f
                val x0 = kotlin.math.floor(fx).toInt()
                val y0 = kotlin.math.floor(fy).toInt()
                val tx = fx - x0
                val ty = fy - y0
                votes.fill(0f)
                votes[colorAt(x0, y0)] += (1 - tx) * (1 - ty)
                votes[colorAt(x0 + 1, y0)] += tx * (1 - ty)
                votes[colorAt(x0, y0 + 1)] += (1 - tx) * ty
                votes[colorAt(x0 + 1, y0 + 1)] += tx * ty
                var best = 0
                for (c in 1 until 8) if (votes[c] > votes[best]) best = c
                best
            }
            val r = 2
            val offs = ArrayList<Int>()
            for (dy in -r..r) for (dx in -r..r) if (dx * dx + dy * dy <= r * r) offs += (dy shl 8) or (dx and 0xFF)
            val counts = IntArray(8)
            repeat(1) {
                val out = IntArray(GW * GH)
                for (y in 0 until GH) for (x in 0 until GW) {
                    counts.fill(0)
                    for (o in offs) {
                        val dx = (o and 0xFF).toByte().toInt()
                        val dy = o shr 8
                        val xx = (x + dx).coerceIn(0, GW - 1)
                        val yy = (y + dy).coerceIn(0, GH - 1)
                        counts[base[yy * GW + xx]]++
                    }
                    var best = base[y * GW + x]
                    for (c in 0 until 8) if (counts[c] > counts[best]) best = c
                    out[y * GW + x] = best
                }
                base = out
            }
            // Paint-like shading: long soft brush strokes, a light from the top-left that makes
            // each shape look raised, and a crisp darker rim where colors meet.
            return IntArray(GW * GH) { i ->
                val x = i % GW
                val y = i / GW
                val c = base[i]
                fun diff(dx: Int, dy: Int): Boolean {
                    val xx = x + dx
                    val yy = y + dy
                    return xx in 0 until GW && yy in 0 until GH && base[yy * GW + xx] != c
                }
                val stroke = sin(x * 0.16f + y * 0.09f + 2.2f * sin(y * 0.045f + x * 0.012f))
                val swirl = sin(x * 0.035f - y * 0.06f + 1.3f)
                var shade = 5f + stroke * 0.75f + swirl * 0.6f
                if (rng.nextInt(7) == 0) shade += if (rng.nextBoolean()) 0.6f else -0.6f
                when {
                    diff(1, 0) || diff(-1, 0) || diff(0, 1) || diff(0, -1) -> shade -= 3.2f
                    diff(-3, -3) || diff(-2, -2) -> shade += 1.4f
                    diff(3, 3) || diff(2, 2) -> shade -= 1.6f
                    diff(-4, -4) -> shade += 0.7f
                    diff(4, 4) -> shade -= 0.8f
                }
                c * 8 + shade.roundToInt().coerceIn(0, 7)
            }
        }

        private fun build(n: Int): Setup {
            val picture = PixelPictures.forLevel(n)
            val rng = Random(n * 4_391L + 77L)

            val grains = paint(picture, rng)
            val colors = picture.colors(true)
            val count = IntArray(8)
            val depth = FloatArray(8)
            for (i in grains.indices) {
                val c = grains[i] / 8
                count[c]++
                depth[c] += (GH - 1 - i / GW).toFloat() / GH
            }
            for (c in colors) depth[c] /= count[c]

            val difficulty = DifficultyCurve.difficulty(n)
            val vehicles = ModeCurve.sandTrucks(n, colors.size)
            val lot = LevelGenerator.custom(n, n * 9_173L + 12_345L) { r ->
                DifficultyCurve.sized(
                    vehicles = vehicles,
                    colors = 1,
                    shape = ModeCurve.lotShape(n),
                    rotation = 0f,
                    scramble = 0f,
                    difficulty = difficulty,
                )
            }

            // How many trucks each color gets, roughly by how much sand it has.
            val trucks = lot.vehicles.size
            val share = colors.associateWith { max(1, (trucks * count[it] / grains.size.toFloat()).roundToInt()) }.toMutableMap()
            while (share.values.sum() > trucks) {
                val c = share.filter { it.value > 1 }.maxByOrNull { it.value }?.key ?: break
                share[c] = share[c]!! - 1
            }
            while (share.values.sum() < trucks) {
                val c = colors.maxBy { count[it].toFloat() / share[it]!! }
                share[c] = share[c]!! + 1
            }
            // Colors near the bottom of the picture come out of the lot first. The jitter loosens
            // that order as levels go on, so some trucks have to wait on the belt.
            val jitter = ModeCurve.sandOrderJitter(n)
            val tokens = ArrayList<Pair<Float, Int>>()
            for (c in colors) {
                val m = share[c]!!
                for (j in 0 until m) tokens += (depth[c] * 1.1f + (j + 0.5f) / m + rng.nextFloat() * jitter) to c
            }
            tokens.sortBy { it.first }
            // Vehicles are listed so that reversed order is a removal order.
            val removal = lot.vehicles.reversed()
            val colorOf = HashMap<Int, Int>()
            removal.forEachIndexed { i, v -> colorOf[v.id] = tokens.getOrNull(i)?.second ?: colors.first() }
            val recolored = lot.vehicles.map { it.copy(color = colorOf[it.id]!!) }

            // Capacity follows vehicle size and adds up to exactly the sand of that color.
            val caps = IntArray(recolored.size)
            for (c in colors) {
                val mine = recolored.filter { it.color == c }
                if (mine.isEmpty()) continue
                val seats = mine.sumOf { it.type.seats }
                var given = 0
                for (v in mine) {
                    caps[v.id] = max(1, count[c] * v.type.seats / seats)
                    given += caps[v.id]
                }
                var rest = count[c] - given
                var idx = 0
                while (rest != 0) {
                    val v = mine[idx % mine.size]
                    if (rest > 0) { caps[v.id]++; rest-- } else if (caps[v.id] > 1) { caps[v.id]--; rest++ }
                    idx++
                }
            }
            return Setup(picture, grains, lot.copy(vehicles = recolored), caps)
        }
    }
}
