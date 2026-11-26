package com.puzzel.lab.cartrafficjam.game.modes

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import com.puzzel.lab.cartrafficjam.core.audio.Haptic
import com.puzzel.lab.cartrafficjam.core.audio.Sfx
import com.puzzel.lab.cartrafficjam.game.engine.GameFx
import com.puzzel.lab.cartrafficjam.game.gen.DifficultyCurve
import com.puzzel.lab.cartrafficjam.game.model.Difficulty
import com.puzzel.lab.cartrafficjam.game.model.VehicleType
import com.puzzel.lab.cartrafficjam.game.modes.ModeArt.vehicleSprite
import com.puzzel.lab.cartrafficjam.game.modes.ModeDraw.label
import com.puzzel.lab.cartrafficjam.game.render.Skin
import com.puzzel.lab.cartrafficjam.game.render.VehicleArt
import com.puzzel.lab.cartrafficjam.game.render.jamColor
import com.puzzel.lab.cartrafficjam.game.render.renderBitmap
import com.puzzel.lab.cartrafficjam.game.render.shade
import com.puzzel.lab.cartrafficjam.game.render.tint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

/**
 * Pixel Crowd: the picture is made of riders. Buses wait in three garage lanes; tapping a
 * lane sends its front bus to a free stop. Riders on the outside of the picture whose color
 * matches a waiting bus walk over and board, which uncovers the riders behind them.
 */
class PixelEngine(level: Int, private val skin: Skin, fx: GameFx) : ModeEngine(level, fx) {

    val picture = PixelPictures.forLevel(level)
    private val pw = picture.w
    private val ph = picture.h
    private val cells = picture.cells.copyOf()
    private val outside = BooleanArray(pw * ph)
    private var exposureDirty = true

    override val difficulty: Difficulty = DifficultyCurve.difficulty(level)

    enum class BState { LANE, DRIVING, PARKED, DEPARTING, GONE }

    class Bus(val color: Int, val cap: Int, val lane: Int) {
        var state = BState.LANE
        var stop = -1
        var filled = 0
        var reserved = 0
        var pos = Offset.Zero
        var heading = 180f
        var t = 0f
        var duration = 1f
        var measure: PathMeasure? = null
        var pathLen = 0f
        var departDelay = -1f
        var departPhase = 0
        var departFromX = 0f
        var bounceT = -1f
        var shakeT = -1f
        var boardTimer = 0f
        var smokeClock = 0f
        val need get() = cap - filled - reserved
    }

    class Stop(var open: Boolean) {
        var bus: Bus? = null
        val free get() = open && bus == null
    }

    /**
     * A rider running to their bus along a smoothed path that hugs the outside of the picture.
     * [xs]/[ys] are the path points, [cum] the running length at each point.
     */
    private class Walker(
        val color: Int,
        val bus: Bus,
        val xs: FloatArray,
        val ys: FloatArray,
        val cum: FloatArray,
        val speed: Float,
        val side: Float,
        val phase: Float,
    ) {
        var s = 0f
        var delay = 0f
        val length get() = cum.last()
        var pos = Offset(xs[0], ys[0])
        /** Direction of travel along the path, and a smoothed copy the sprite turns with. */
        var tx = 0f
        var ty = -1f
        var hx = 0f
        var hy = -1f
    }

    val lanes: List<ArrayDeque<Bus>>
    // Three stops open normally, two on late Super Hard levels; the rest open with an ad.
    val stops = Array(STOPS) { Stop(it < com.puzzel.lab.cartrafficjam.game.gen.ModeCurve.pixelOpenStops(level)) }
    private val walkers = ArrayList<Walker>()
    private val rnd = Random(level * 13 + 5)

    init {
        val plan = plan(picture, level)
        lanes = plan.mapIndexed { lane, list -> ArrayDeque(list.map { (c, cap) -> Bus(c, cap, lane) }) }
    }

    private val allBuses get() = lanes.flatten() + stops.mapNotNull { it.bus } + departing
    private val departing = ArrayList<Bus>()

    override val counter: Int get() = cells.count { it >= 0 }
    override val counterLabel = "Riders"
    override val reviveTitle = "Stops Jammed!"
    override val reviveMessage = "No bus can reach its riders.\nOpen another stop to keep going!"

    // --- Layout -------------------------------------------------------------------------------

    private var garageTop = 0f
    private var garageBottom = 0f
    private var roadY = 0f
    private var roadTop = 0f
    private var roadBottom = 0f
    private var stopTop = 0f
    private var stopH = 0f
    private var stopW = 0f
    private var cell = 0f
    private var picLeft = 0f
    private var picTop = 0f
    private lateinit var art: VehicleArt
    private lateinit var tiles: Array<ImageBitmap?>
    private var scene: ImageBitmap? = null

    private fun laneX(i: Int) = width * (1 + 2 * i) / 6f
    private fun garageSpot(i: Int) = Offset(laneX(i), garageTop + 44 * u)
    fun stopCenter(i: Int) = Offset(stopX(i), stopTop + stopH / 2)
    private fun stopX(i: Int) = 12 * u + stopW / 2 + i * (stopW + 8 * u)
    private fun stopRect(i: Int) = Rect(stopX(i) - stopW / 2, stopTop, stopX(i) + stopW / 2, stopTop + stopH)
    private fun cellCenter(i: Int) = Offset(picLeft + (i % pw + 0.5f) * cell, picTop + (i / pw + 0.5f) * cell)

    override fun layout() {
        garageTop = topInset + 54 * u
        garageBottom = garageTop + 84 * u
        roadTop = garageBottom
        roadBottom = roadTop + 34 * u
        roadY = (roadTop + roadBottom) / 2
        stopTop = roadBottom + 6 * u
        stopH = 80 * u
        stopW = (width - 24 * u - 3 * 8 * u) / STOPS
        val areaTop = stopTop + stopH + 22 * u
        val areaBottom = height - bottomInset - 26 * u
        cell = min(min((width - 26 * u) / pw, (areaBottom - areaTop) / ph), 24 * u)
        picLeft = (width - cell * pw) / 2
        picTop = areaTop + ((areaBottom - areaTop) - cell * ph) * 0.42f
        art = VehicleArt(12.2f * u, skin)
        val tilePx = cell.toInt().coerceAtLeast(6)
        tiles = arrayOfNulls(8)
        for (c in picture.colors(false)) tiles[c] = ModeArt.riderBlock(c, tilePx)
        navDirty = true
        scene = renderBitmap(width.toInt(), height.toInt()) { paintScene() }
        for ((i, lane) in lanes.withIndex()) for (b in lane) { b.pos = garageSpot(i); b.heading = 180f }
        for (s in stops) s.bus?.let { if (it.state == BState.PARKED) it.pos = stopCenter(it.stop) }
        for (b in lanes.flatten()) art.composite(VehicleType.BUS, b.color, false, 180f)
    }

    // --- Input --------------------------------------------------------------------------------

    override fun onTap(p: Offset) {
        if (!ready || phase != ModePhase.PLAYING) return
        particles.ring(p)
        for (i in 0 until STOPS) {
            if (!stops[i].open && stopRect(i).inflate(4 * u).contains(p)) {
                fx.sfx(Sfx.Click)
                onEvent(ModeEvent.LockedSpot(i))
                return
            }
        }
        if (p.y < garageTop - 6 * u || p.y > roadBottom) return
        val lane = (0 until LANES).minByOrNull { abs(laneX(it) - p.x) } ?: return
        if (abs(laneX(lane) - p.x) > width / 6f) return
        dispatch(lane)
    }

    private fun dispatch(lane: Int) {
        val bus = lanes[lane].firstOrNull()
        if (bus == null) {
            fx.sfx(Sfx.Tap)
            onEvent(ModeEvent.Toast("This garage is empty"))
            return
        }
        val stop = (0 until STOPS).filter { stops[it].free }.minByOrNull { abs(stopX(it) - laneX(lane)) }
        if (stop == null) {
            bus.shakeT = 0f
            fx.sfx(Sfx.Warning)
            fx.haptic(Haptic.Bump)
            onEvent(ModeEvent.Toast("Every bus stop is taken!"))
            return
        }
        fx.haptic(Haptic.Tap)
        lanes[lane].removeFirst()
        stops[stop].bus = bus
        bus.stop = stop
        bus.state = BState.DRIVING
        bus.t = 0f
        val from = bus.pos
        val to = stopCenter(stop)
        val pts = ArrayList<Offset>()
        for (q in listOf(from, Offset(from.x, roadY), Offset(to.x, roadY), to)) {
            if (pts.isEmpty() || (pts.last() - q).getDistance() > 2f) pts += q
        }
        val path = ModeDraw.roundedPath(pts, 20 * u)
        bus.measure = PathMeasure().apply { setPath(path, false) }
        bus.pathLen = bus.measure!!.length
        bus.duration = (0.3f + bus.pathLen / (900 * u)).coerceIn(0.45f, 1.1f)
        particles.smoke(from - Offset(0f, 30 * u), 0f, -1f, 4)
        fx.sfx(Sfx.EngineGo, 0.95f + rnd.nextFloat() * 0.15f)
    }

    override fun unlockSpot(index: Int) {
        val s = stops.getOrNull(index) ?: return
        if (s.open) return
        s.open = true
        fx.sfx(Sfx.Unlock)
        particles.stars(stopCenter(index), 14)
        if (phase == ModePhase.STUCK) phase = ModePhase.PLAYING
        stuckTimer = 0f
    }

    override fun onRevive() {
        val locked = (0 until STOPS).firstOrNull { !stops[it].open }
        if (locked != null) {
            unlockSpot(locked)
            return
        }
        // Every stop is open: send the buses that can't board back to the end of their lanes.
        if (exposureDirty) recomputeOutside()
        for (s in stops) {
            val b = s.bus ?: continue
            if (b.state == BState.PARKED && !hasExposed(b.color)) {
                s.bus = null
                particles.smoke(b.pos, 0f, -1f, 6)
                b.state = BState.LANE
                b.stop = -1
                b.pos = garageSpot(b.lane)
                b.heading = 180f
                lanes[b.lane].addLast(b)
            }
        }
        fx.sfx(Sfx.Whoosh)
        stuckTimer = 0f
    }

    // --- Exposure -----------------------------------------------------------------------------

    /** Flood fill of the empty cells connected to the picture's border. */
    private fun recomputeOutside() {
        outside.fill(false)
        val stack = IntArray(pw * ph)
        var sp = 0
        fun push(i: Int) {
            if (!outside[i] && cells[i] < 0) { outside[i] = true; stack[sp++] = i }
        }
        for (x in 0 until pw) { push(x); push((ph - 1) * pw + x) }
        for (y in 0 until ph) { push(y * pw); push(y * pw + pw - 1) }
        while (sp > 0) {
            val i = stack[--sp]
            val x = i % pw
            val y = i / pw
            if (x > 0) push(i - 1)
            if (x < pw - 1) push(i + 1)
            if (y > 0) push(i - pw)
            if (y < ph - 1) push(i + pw)
        }
        exposureDirty = false
    }

    private fun exposed(i: Int): Boolean {
        if (cells[i] < 0) return false
        val x = i % pw
        val y = i / pw
        if (x == 0 || y == 0 || x == pw - 1 || y == ph - 1) return true
        return outside[i - 1] || outside[i + 1] || outside[i - pw] || outside[i + pw]
    }

    private fun hasExposed(color: Int): Boolean {
        for (i in cells.indices) if (cells[i] == color && exposed(i)) return true
        return false
    }

    private fun nearestExposed(color: Int, to: Offset): Int {
        var best = -1
        var bestD = Float.MAX_VALUE
        for (i in cells.indices) {
            if (cells[i] != color || !exposed(i)) continue
            val d = (cellCenter(i) - to).getDistanceSquared()
            if (d < bestD) { bestD = d; best = i }
        }
        return best
    }

    // --- Frame --------------------------------------------------------------------------------

    private var stuckTimer = 0f
    private var winTimer = -1f
    private var popIndex = 0

    override fun step(dt: Float) {
        updateBuses(dt)
        board(dt)
        updateWalkers(dt)
        checkEnd(dt)
    }

    private fun updateBuses(dt: Float) {
        for (b in allBuses) {
            if (b.bounceT >= 0f) { b.bounceT += dt / 0.25f; if (b.bounceT >= 1f) b.bounceT = -1f }
            if (b.shakeT >= 0f) { b.shakeT += dt / 0.35f; if (b.shakeT >= 1f) b.shakeT = -1f }
            when (b.state) {
                BState.DRIVING -> {
                    b.t += dt
                    val p = (b.t / b.duration).coerceIn(0f, 1f)
                    val e = 1f - (1f - p).pow(2.2f)
                    val m = b.measure!!
                    val d = b.pathLen * e
                    b.pos = m.getPosition(d)
                    val tan = m.getTangent(d)
                    if (tan.getDistance() > 0.01f) b.heading = atan2(tan.x, -tan.y) * 180f / PI.toFloat()
                    b.smokeClock += dt
                    if (b.smokeClock > 0.08f && p < 0.85f) {
                        b.smokeClock = 0f
                        particles.smoke(b.pos - ModeDraw.headingVec(b.heading) * (art.bodyL(VehicleType.BUS) / 2), count = 1)
                    }
                    if (p >= 1f) {
                        b.state = BState.PARKED
                        b.pos = stopCenter(b.stop)
                        b.heading = 180f
                        b.bounceT = 0f
                        b.boardTimer = 0.15f
                        fx.sfx(Sfx.Park)
                        particles.smoke(b.pos - Offset(0f, art.bodyL(VehicleType.BUS) / 2), 0f, -1f, 3)
                    }
                }
                BState.PARKED -> if (b.departDelay >= 0f) {
                    b.departDelay -= dt
                    if (b.departDelay < 0f) {
                        b.state = BState.DEPARTING
                        b.departPhase = 0
                        b.t = 0f
                        stops[b.stop].bus = null
                        departing += b
                    }
                }
                BState.DEPARTING -> {
                    b.t += dt
                    val sc = stopCenter(b.stop)
                    if (b.departPhase == 0) {
                        val p = (b.t / 0.28f).coerceIn(0f, 1f)
                        val e = p * p * (3 - 2 * p)
                        b.pos = Offset(sc.x, sc.y + (roadY - sc.y) * e)
                        if (p >= 1f) {
                            b.departPhase = 1
                            b.t = 0f
                            b.departFromX = b.pos.x
                            fx.sfx(Sfx.Horn, 0.95f + rnd.nextFloat() * 0.1f)
                        }
                    } else {
                        val turn = (b.t / 0.22f).coerceIn(0f, 1f)
                        b.heading = 180f + 90f * (turn * turn * (3 - 2 * turn))
                        val dist = 200 * u * b.t + 1300 * u * b.t * b.t
                        b.pos = Offset(b.departFromX - dist, roadY)
                        b.smokeClock += dt
                        if (b.smokeClock > 0.06f) {
                            b.smokeClock = 0f
                            particles.smoke(b.pos + Offset(art.bodyL(VehicleType.BUS) / 2, 0f), 1f, 0f, 1)
                        }
                        if (b.pos.x < -120 * u) b.state = BState.GONE
                    }
                }
                else -> Unit
            }
        }
        departing.removeAll { it.state == BState.GONE }
    }

    // --- Riders running to the bus ------------------------------------------------------------

    /** Walking grid: the picture plus a [NAV_MARGIN]-cell ring of open ground around it. */
    private val nw = pw + NAV_MARGIN * 2
    private val nh = ph + NAV_MARGIN * 2
    private val navDist = Array(STOPS) { IntArray(nw * nh) }
    private val navFresh = BooleanArray(STOPS)
    private var navDirty = true

    private fun navFree(ex: Int, ey: Int): Boolean {
        if (ex !in 0 until nw || ey !in 0 until nh) return false
        val x = ex - NAV_MARGIN
        val y = ey - NAV_MARGIN
        if (x !in 0 until pw || y !in 0 until ph) return true
        return cells[y * pw + x] < 0
    }

    private fun navCenter(ex: Int, ey: Int) =
        Offset(picLeft + (ex - NAV_MARGIN + 0.5f) * cell, picTop + (ey - NAV_MARGIN + 0.5f) * cell)

    /** Where the stream leaves the picture for stop [s]: the ground above it, under the stop. */
    private fun exitCell(s: Int): Int {
        val ex = ((stopX(s) - picLeft) / cell + NAV_MARGIN).toInt().coerceIn(0, nw - 1)
        return ex
    }

    /** Walking distance (x10, diagonals x14) from every open cell to stop [s]'s exit. */
    private fun navField(s: Int): IntArray {
        if (navDirty) { navFresh.fill(false); navDirty = false }
        val d = navDist[s]
        if (navFresh[s]) return d
        d.fill(Int.MAX_VALUE)
        val q = java.util.PriorityQueue<Long>()
        val start = exitCell(s)
        d[start] = 0
        q.add(start.toLong())
        while (q.isNotEmpty()) {
            val top = q.poll()!!
            val i = (top and 0xFFFFF).toInt()
            val dist = (top shr 20).toInt()
            if (dist > d[i]) continue
            val x = i % nw
            val y = i / nw
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (!navFree(nx, ny)) continue
                if (dx != 0 && dy != 0 && (!navFree(x + dx, y) || !navFree(x, y + dy))) continue
                val nd = dist + if (dx != 0 && dy != 0) 14 else 10
                val ni = ny * nw + nx
                if (nd < d[ni]) {
                    d[ni] = nd
                    q.add((nd.toLong() shl 20) or ni.toLong())
                }
            }
        }
        navFresh[s] = true
        return d
    }

    /** Shortest walking distance from picture cell [i] out to the field, via an open neighbour. */
    private fun cellNavDist(i: Int, d: IntArray): Int {
        val ex = i % pw + NAV_MARGIN
        val ey = i / pw + NAV_MARGIN
        var best = Int.MAX_VALUE
        for (dy in -1..1) for (dx in -1..1) {
            if ((dx == 0) == (dy == 0)) continue
            val nx = ex + dx
            val ny = ey + dy
            if (!navFree(nx, ny)) continue
            val v = d[ny * nw + nx]
            if (v < best) best = v
        }
        return best
    }

    /** Smoothed route from picture cell [i] around the outside of the picture to the bus door. */
    private fun route(i: Int, s: Int, d: IntArray, door: Offset): List<Offset> {
        val pts = ArrayList<Offset>()
        pts += cellCenter(i)
        var ex = i % pw + NAV_MARGIN
        var ey = i / pw + NAV_MARGIN
        // Step out into the best open neighbour, then roll downhill to the exit.
        var cur = -1
        var best = Int.MAX_VALUE
        for ((dx, dy) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            val nx = ex + dx
            val ny = ey + dy
            if (navFree(nx, ny) && d[ny * nw + nx] < best) { best = d[ny * nw + nx]; cur = ny * nw + nx }
        }
        var guard = 0
        while (cur >= 0 && guard++ < nw * nh) {
            ex = cur % nw
            ey = cur / nw
            pts += navCenter(ex, ey)
            if (d[cur] == 0) break
            var next = -1
            var nb = d[cur]
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = ex + dx
                val ny = ey + dy
                if (!navFree(nx, ny)) continue
                if (dx != 0 && dy != 0 && (!navFree(ex + dx, ey) || !navFree(ex, ey + dy))) continue
                val v = d[ny * nw + nx]
                if (v < nb) { nb = v; next = ny * nw + nx }
            }
            cur = next
        }
        // Up from the exit to the front of the bus.
        val exit = pts.last()
        pts += Offset((exit.x + door.x) / 2, (exit.y + door.y) / 2 + cell * 0.3f)
        pts += door
        var out: List<Offset> = pts
        repeat(2) { out = chaikin(out) }
        return out
    }

    private fun chaikin(p: List<Offset>): List<Offset> {
        if (p.size < 3) return p
        val out = ArrayList<Offset>(p.size * 2)
        out += p.first()
        for (k in 0 until p.size - 1) {
            out += p[k] * 0.75f + p[k + 1] * 0.25f
            out += p[k] * 0.25f + p[k + 1] * 0.75f
        }
        out += p.last()
        return out
    }

    private fun door(b: Bus) = stopCenter(b.stop) + Offset(0f, art.bodyL(VehicleType.BUS) * 0.42f)

    private fun board(dt: Float) {
        if (phase == ModePhase.WON) return
        for ((si, s) in stops.withIndex()) {
            val b = s.bus ?: continue
            if (b.state != BState.PARKED || b.departDelay >= 0f || b.need <= 0) continue
            b.boardTimer -= dt
            var guard = 0
            var burst = 0f
            while (b.boardTimer <= 0f && b.need > 0 && guard < 6) {
                if (exposureDirty) recomputeOutside()
                val field = navField(si)
                // The rider of this color with the shortest walk goes next.
                var pick = -1
                var pickD = Int.MAX_VALUE
                for (i in cells.indices) {
                    if (cells[i] != b.color || !exposed(i)) continue
                    val dd = cellNavDist(i, field)
                    if (dd < pickD) { pickD = dd; pick = i }
                }
                if (pick < 0) { b.boardTimer = 0f; break }
                val pts = route(pick, si, field, door(b) + Offset((rnd.nextFloat() - 0.5f) * 10 * u, 0f))
                cells[pick] = -1
                exposureDirty = true
                navDirty = true
                val xs = FloatArray(pts.size) { pts[it].x }
                val ys = FloatArray(pts.size) { pts[it].y }
                val cum = FloatArray(pts.size)
                for (k in 1 until pts.size) cum[k] = cum[k - 1] + (pts[k] - pts[k - 1]).getDistance()
                walkers += Walker(
                    b.color, b, xs, ys, cum,
                    speed = RUN_SPEED * u * (0.88f + rnd.nextFloat() * 0.24f),
                    side = (rnd.nextFloat() - 0.5f) * 0.7f * cell,
                    phase = rnd.nextFloat() * 6.28f,
                ).also { it.delay = burst }
                burst += 0.008f
                b.reserved++
                b.boardTimer += BOARD_INTERVAL
                guard++
            }
        }
    }

    private fun updateWalkers(dt: Float) {
        val it = walkers.iterator()
        while (it.hasNext()) {
            val w = it.next()
            if (w.delay > 0f) { w.delay -= dt; w.pos = walkerPos(w); continue }
            // Ease in from standing, run, then crowd up and shuffle in at the bus door.
            val accel = min(1f, 0.35f + w.s / (cell * 1.5f))
            val queue = min(1f, 0.4f + (w.length - w.s) / (cell * 3f))
            w.s += w.speed * accel * queue * dt
            w.pos = walkerPos(w)
            val k = min(1f, dt * 10f)
            w.hx += (w.tx - w.hx) * k
            w.hy += (w.ty - w.hy) * k
            if (w.s < w.length) continue
            it.remove()
            val b = w.bus
            b.reserved--
            b.filled++
            b.bounceT = 0f
            if (popIndex++ % 2 == 0) {
                val pops = arrayOf(Sfx.Pop1, Sfx.Pop2, Sfx.Pop3)
                fx.sfx(pops[(popIndex / 2) % 3], 0.95f + (b.filled.toFloat() / b.cap) * 0.25f)
            }
            if (b.filled >= b.cap) {
                b.departDelay = 0.3f
                fx.sfx(Sfx.Full)
                particles.stars(b.pos, 10)
            }
        }
    }

    /** Point [Walker.s] along the route, nudged sideways so the stream runs several abreast. */
    private fun walkerPos(w: Walker): Offset {
        val s = w.s.coerceIn(0f, w.length)
        var k = 1
        while (k < w.cum.size - 1 && w.cum[k] < s) k++
        val segLen = (w.cum[k] - w.cum[k - 1]).coerceAtLeast(1e-3f)
        val f = ((s - w.cum[k - 1]) / segLen).coerceIn(0f, 1f)
        val x = w.xs[k - 1] + (w.xs[k] - w.xs[k - 1]) * f
        val y = w.ys[k - 1] + (w.ys[k] - w.ys[k - 1]) * f
        val tx = (w.xs[k] - w.xs[k - 1]) / segLen
        val ty = (w.ys[k] - w.ys[k - 1]) / segLen
        // Spread out once clear of the picture, squeeze back together at the bus door.
        val spread = min(1f, s / cell) * min(1f, (w.length - s) / (cell * 2.5f))
        w.tx = tx
        w.ty = ty
        return Offset(x - ty * w.side * spread, y + tx * w.side * spread)
    }

    private fun checkEnd(dt: Float) {
        if (phase != ModePhase.PLAYING) return
        val busy = walkers.isNotEmpty() || departing.isNotEmpty() ||
            stops.any { s -> s.bus?.let { it.state == BState.DRIVING || it.departDelay >= 0f } == true }
        if (counter == 0 && !busy && stops.all { it.bus == null }) {
            if (winTimer < 0f) winTimer = 0f
            winTimer += dt
            if (winTimer > 0.4f) win()
            return
        }
        if (busy) { stuckTimer = 0f; return }
        if (exposureDirty) recomputeOutside()
        val canBoard = stops.any { s -> s.bus?.let { it.state == BState.PARKED && it.need > 0 && hasExposed(it.color) } == true }
        val canDispatch = stops.any { it.free } && lanes.any { it.isNotEmpty() }
        if (!canBoard && !canDispatch) {
            stuckTimer += dt
            if (stuckTimer > 0.7f) { stuckTimer = 0f; stuck() }
        } else stuckTimer = 0f
    }

    // --- Drawing ------------------------------------------------------------------------------

    override fun draw(scope: DrawScope, t: Float) = with(scope) {
        val sceneBmp = scene ?: return@with
        translate(screenShake.x, screenShake.y) {
            drawImage(sceneBmp)
            drawLanesInfo()
            for (i in 0 until STOPS) if (!stops[i].open) lockedStop(i, t)

            // The picture, one rider per cell. Riders who can see their bus waiting bounce on
            // the spot; everyone else sways gently.
            if (exposureDirty) recomputeOutside()
            var eager = 0
            for (s in stops) s.bus?.let { if (it.state == BState.PARKED && it.need > 0) eager = eager or (1 shl it.color) }
            for (i in cells.indices) {
                val c = cells[i]
                if (c < 0) continue
                val tile = tiles[c] ?: continue
                val x = i % pw
                val y = i / pw
                val bob = if (eager and (1 shl c) != 0 && exposed(i)) {
                    -kotlin.math.abs(sin(t * 11f + x * 1.3f + y * 0.9f)) * 2.6f * u
                } else sin(t * 3.2f + x * 0.7f + y * 0.45f) * 0.6f * u
                drawImage(tile, Offset(picLeft + x * cell, picTop + y * cell + bob))
            }

            // Buses waiting at the garage doors (front of each lane).
            for ((i, lane) in lanes.withIndex()) {
                val b = lane.firstOrNull() ?: continue
                drawBus(b, garageSpot(i), t, showNeed = true)
            }
            val moving = (stops.mapNotNull { it.bus } + departing).sortedBy { it.pos.y }
            for (b in moving) drawBus(b, b.pos, t, showNeed = b.state != BState.DEPARTING)

            // Riders running to their bus, drawn back to front. They leave the picture as plain
            // blocks and sprout stubby legs and arms once they get going.
            walkers.sortBy { it.pos.y }
            for (w in walkers) drawRunner(w)
            particles.draw(this)
        }
    }

    private fun DrawScope.drawRunner(w: Walker) {
        val tile = tiles[w.color] ?: return
        val p = w.pos
        val moving = w.delay <= 0f
        val end = ((w.length - w.s) / (cell * 0.8f)).coerceIn(0f, 1f)
        val sc = 0.94f * (0.55f + 0.45f * end)
        val size = cell * sc
        // Legs grow in over the first stretch and tuck away as the rider climbs aboard.
        val sprout = if (!moving) 0f else min(1f, w.s / (cell * 0.7f)) * min(1f, end * 1.6f)
        val step = sin(w.s / (cell * 0.36f) + w.phase)
        val lift = kotlin.math.abs(step) * 2.6f * u * sprout + if (w.s < cell * 0.5f) (1f - w.s / (cell * 0.5f)) * 4f * u else 0f
        val hl = kotlin.math.hypot(w.hx, w.hy).coerceAtLeast(1e-3f)
        val hx = w.hx / hl
        val hy = w.hy / hl
        val nx = -hy
        val ny = hx
        val col = jamColor(w.color)
        val limb = col.shade(0.5f)

        drawOval(Color.Black.copy(alpha = 0.2f * (0.5f + 0.5f * end)), Offset(p.x - size * 0.45f, p.y + size * 0.42f), Size(size * 0.9f, size * 0.32f))
        if (sprout > 0.02f) {
            // Legs: two stubs under the block, striding back and forth along the path.
            for (k in intArrayOf(-1, 1)) {
                val stride = step * k * size * 0.24f * sprout
                val hip = Offset(p.x + nx * k * size * 0.2f, p.y + size * 0.42f + ny * k * size * 0.08f)
                val foot = Offset(
                    hip.x + hx * stride - hx * size * 0.06f,
                    hip.y + size * 0.17f * sprout + hy * stride,
                )
                drawLine(limb, hip, foot, size * 0.22f, StrokeCap.Round)
                drawCircle(limb.shade(0.75f), size * 0.13f * sprout + 0.5f, foot + Offset(0f, size * 0.04f))
            }
            // Arms: little nubs on the sides swinging opposite to the legs.
            for (k in intArrayOf(-1, 1)) {
                val swing = -step * k * size * 0.16f * sprout
                val shoulder = Offset(p.x + nx * k * size * 0.46f, p.y + size * 0.12f + ny * k * size * 0.2f - lift)
                val hand = Offset(
                    shoulder.x + nx * k * size * 0.1f * sprout + hx * swing,
                    shoulder.y + size * 0.12f * sprout + hy * swing,
                )
                drawLine(limb, shoulder, hand, size * 0.15f, StrokeCap.Round)
                drawCircle(col.shade(0.7f), size * 0.09f * sprout + 0.5f, hand)
            }
        }
        // The body leans into the direction it runs.
        val lean = (atan2(hx, -hy) * 180f / PI.toFloat()).coerceIn(-32f, 32f) * sprout
        withTransform({
            rotate(lean, p)
            scale(sc, sc, p)
        }) {
            drawImage(tile, Offset(p.x - cell / 2, p.y - cell / 2 - lift / sc))
        }
    }

    private fun DrawScope.drawBus(b: Bus, at: Offset, t: Float, showNeed: Boolean) {
        val bounce = if (b.bounceT >= 0f) 1f + 0.06f * sin(b.bounceT * PI.toFloat()) else 1f
        val shake = if (b.shakeT >= 0f) sin(b.shakeT * 38f) * 5f * (1f - b.shakeT) else 0f
        // Heading changes every frame while driving to a stop, turning away or shaking: draw it live.
        val turning = b.state == BState.DRIVING || b.state == BState.DEPARTING || b.shakeT >= 0f
        val roof = vehicleSprite(art, VehicleType.BUS, b.color, at, b.heading + shake, bounce, live = turning)
        if (!showNeed) return
        val left = if (b.state == BState.LANE) b.cap - b.filled else b.cap - b.filled
        val c = at - Offset(0f, roof)
        val col = jamColor(b.color)
        drawCircle(col.shade(0.45f), 11.5f * u, c + Offset(0f, 1.2f * u))
        drawCircle(Color.White, 11 * u, c)
        drawCircle(col, 11 * u, c, style = Stroke(2 * u))
        label("$left", c, if (left >= 100) 9.5f * u else 11.5f * u, col.shade(0.55f))
    }

    /** Small dots above each garage door showing the next buses in that lane. */
    private fun DrawScope.drawLanesInfo() {
        for ((i, lane) in lanes.withIndex()) {
            val x = laneX(i)
            val y = garageTop + 8 * u
            if (lane.isEmpty()) {
                val c = garageSpot(i)
                drawLine(Color(0xFFF2453D), c - Offset(9 * u, 9 * u), c + Offset(9 * u, 9 * u), 4 * u, StrokeCap.Round)
                drawLine(Color(0xFFF2453D), c - Offset(-9 * u, 9 * u), c + Offset(-9 * u, 9 * u), 4 * u, StrokeCap.Round)
                continue
            }
            val next = lane.drop(1).take(3)
            val startX = x - (next.size - 1) * 7 * u
            for ((k, b) in next.withIndex()) {
                val p = Offset(startX + k * 14 * u, y)
                drawCircle(Color.Black.copy(alpha = 0.25f), 5.5f * u, p + Offset(0f, 1 * u))
                drawCircle(jamColor(b.color), 5 * u, p)
                drawCircle(Color.White, 5 * u, p, style = Stroke(1.2f * u))
            }
            if (lane.size > 4) label("+${lane.size - 4}", Offset(x + 30 * u, y), 9 * u, Color.White, Color(0xFF0D2C6B))
        }
    }

    private fun DrawScope.lockedStop(i: Int, t: Float) {
        val r = stopRect(i)
        drawRoundRect(Color(0xFF3C4250).copy(alpha = 0.45f), r.topLeft, r.size, CornerRadius(8 * u))
        val c = Offset(r.center.x, r.center.y - 8 * u)
        val pulse = 1f + 0.08f * sin(t * 5f + i)
        drawCircle(Color(0xFF2E7D32), 15 * u * pulse, c + Offset(0f, 2 * u))
        drawCircle(Brush.verticalGradient(listOf(Color(0xFF9CE65C), Color(0xFF4CAF50)), c.y - 15 * u, c.y + 15 * u), 15 * u * pulse, c)
        drawLine(Color.White, c - Offset(7 * u, 0f), c + Offset(7 * u, 0f), 3.5f * u, StrokeCap.Round)
        drawLine(Color.White, c - Offset(0f, 7 * u), c + Offset(0f, 7 * u), 3.5f * u, StrokeCap.Round)
        val b = Offset(r.center.x, r.bottom - 16 * u)
        drawRoundRect(Color(0xFFE65100), b - Offset(15 * u, 7 * u), Size(30 * u, 15 * u), CornerRadius(4 * u))
        drawRoundRect(Color(0xFFFFB300), b - Offset(15 * u, 8 * u), Size(30 * u, 14 * u), CornerRadius(4 * u))
        label("AD", b - Offset(0f, 1 * u), 10 * u)
    }

    private fun DrawScope.paintScene() {
        val w = size.width
        val h = size.height
        // Plaza tiles
        drawRect(Brush.verticalGradient(listOf(Color(0xFFE4EEFF), Color(0xFFC9DBFA))))
        val tile = 30 * u
        var y = roadBottom
        var row = 0
        while (y < h) {
            var x = if (row % 2 == 0) 0f else -tile / 2
            while (x < w) {
                drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(x + 1.5f * u, y + 1.5f * u), Size(tile - 3 * u, tile - 3 * u), CornerRadius(4 * u))
                x += tile
            }
            y += tile; row++
        }
        // Sky behind the garage
        drawRect(Brush.verticalGradient(listOf(Color(0xFF6EC6FF), Color(0xFFB3E1FF)), 0f, garageBottom), size = Size(w, garageBottom))
        // Garage building
        val gTop = garageTop - 2 * u
        drawRoundRect(Color(0xFF0D47A1), Offset(4 * u, gTop + 4 * u), Size(w - 8 * u, garageBottom - gTop), CornerRadius(14 * u))
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF1E88E5)), gTop, garageBottom),
            Offset(4 * u, gTop), Size(w - 8 * u, garageBottom - gTop), CornerRadius(14 * u),
        )
        drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(14 * u, gTop + 4 * u), Size(w - 28 * u, 6 * u), CornerRadius(3 * u))
        for (i in 0 until LANES) {
            val c = laneX(i)
            val dw = 64 * u
            val top = garageTop + 18 * u
            drawRoundRect(Color(0xFF0B2A5C), Offset(c - dw / 2 - 3 * u, top - 3 * u), Size(dw + 6 * u, garageBottom - top + 3 * u), CornerRadius(12 * u, 12 * u))
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFF1B2A44), Color(0xFF3A4D6E)), top, garageBottom),
                Offset(c - dw / 2, top), Size(dw, garageBottom - top), CornerRadius(10 * u, 10 * u),
            )
            // shutter lines
            var sy = top + 6 * u
            while (sy < top + 16 * u) { drawLine(Color.White.copy(alpha = 0.12f), Offset(c - dw / 2 + 4 * u, sy), Offset(c + dw / 2 - 4 * u, sy), 1.4f * u); sy += 4 * u }
        }
        // Road
        drawRect(Color(0xFF59606B), Offset(0f, roadTop), Size(w, roadBottom - roadTop))
        drawRect(Color.Black.copy(alpha = 0.25f), Offset(0f, roadTop), Size(w, 3 * u))
        drawLine(
            Color.White.copy(alpha = 0.75f), Offset(0f, roadY), Offset(w, roadY), 2.4f * u,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14 * u, 12 * u)),
        )
        drawRect(Color(0xFFE9ECEF), Offset(0f, roadBottom), Size(w, 4 * u))
        // Bus stop bays
        for (i in 0 until STOPS) {
            val r = stopRect(i)
            drawRoundRect(Color(0xFF8A93A0), r.topLeft + Offset(0f, 3 * u), r.size, CornerRadius(8 * u))
            drawRoundRect(Color(0xFFA7B0BC), r.topLeft, r.size, CornerRadius(8 * u))
            drawRoundRect(Color(0xFFFFD54F), r.topLeft + Offset(3 * u, 3 * u), Size(r.width - 6 * u, r.height - 6 * u), CornerRadius(6 * u), style = Stroke(2 * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7 * u, 5 * u))))
        }
        // Bus stop sign by the first bay
        val sx = 7 * u
        drawRect(Color(0xFF546E7A), Offset(sx - 1 * u, stopTop + stopH - 4 * u), Size(2 * u, 22 * u))
        drawRoundRect(Color(0xFF1565C0), Offset(sx - 7 * u, stopTop + stopH + 2 * u), Size(14 * u, 12 * u), CornerRadius(3 * u))
        // Plate under the picture
        val plate = Rect(picLeft - 10 * u, picTop - 10 * u, picLeft + cell * pw + 10 * u, picTop + cell * ph + 10 * u)
        drawRoundRect(Color(0x22000000), plate.topLeft + Offset(0f, 6 * u), plate.size, CornerRadius(18 * u))
        drawRoundRect(Color.White.copy(alpha = 0.55f), plate.topLeft, plate.size, CornerRadius(18 * u))
        drawRoundRect(Color(0xFF90B4F0), plate.topLeft, plate.size, CornerRadius(18 * u), style = Stroke(2 * u))
    }

    companion object {
        const val LANES = 3
        const val STOPS = 4
        private const val BOARD_INTERVAL = 0.013f
        private const val RUN_SPEED = 175f
        private const val NAV_MARGIN = 2

        /**
         * Splits the picture into buses. It peels the picture the way a player would, one bus at
         * a time, so taking the buses in plan order always works; each bus then goes to a random
         * lane, which keeps that order reachable.
         */
        fun plan(picture: PixelPicture, level: Int): List<List<Pair<Int, Int>>> {
            val rng = Random(level * 7_919L + 4_242L)
            val w = picture.w
            val h = picture.h
            val cells = picture.cells.copyOf()
            // Buses get smaller as levels go on, so there are more of them to juggle; peaks
            // (Hard / Super Hard) shrink them further and breathers enlarge them.
            val maxCap = com.puzzel.lab.cartrafficjam.game.gen.ModeCurve.pixelBusCap(level)
            val out = BooleanArray(w * h)
            fun flood() {
                out.fill(false)
                val st = ArrayDeque<Int>()
                fun push(i: Int) { if (!out[i] && cells[i] < 0) { out[i] = true; st.addLast(i) } }
                for (x in 0 until w) { push(x); push((h - 1) * w + x) }
                for (y in 0 until h) { push(y * w); push(y * w + w - 1) }
                while (st.isNotEmpty()) {
                    val i = st.removeLast()
                    val x = i % w
                    val y = i / w
                    if (x > 0) push(i - 1)
                    if (x < w - 1) push(i + 1)
                    if (y > 0) push(i - w)
                    if (y < h - 1) push(i + w)
                }
            }
            fun exposed(i: Int): Boolean {
                if (cells[i] < 0) return false
                val x = i % w
                val y = i / w
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) return true
                return out[i - 1] || out[i + 1] || out[i - w] || out[i + w]
            }
            /** How many riders of [c] a bus of size [cap] could collect starting now. */
            fun peel(c: Int, cap: Int, commit: Boolean): Int {
                val saved = if (commit) null else cells.copyOf()
                var taken = 0
                while (taken < cap) {
                    flood()
                    val ex = cells.indices.filter { cells[it] == c && exposed(it) }
                    if (ex.isEmpty()) break
                    for (i in ex) {
                        if (taken >= cap) break
                        cells[i] = -1
                        taken++
                    }
                }
                if (saved != null) saved.copyInto(cells)
                return taken
            }

            val buses = ArrayList<Pair<Int, Int>>()
            while (cells.any { it >= 0 }) {
                val colors = cells.filter { it >= 0 }.distinct()
                val reach = colors.associateWith { peel(it, maxCap, false) }.filterValues { it > 0 }
                val best = reach.values.max()
                // Prefer full-size buses but keep some variety.
                val pool = reach.filterValues { it >= best * 0.6f }.keys.toList()
                val c = pool[rng.nextInt(pool.size)]
                val n = peel(c, maxCap, true)
                buses += c to n
            }
            // Fold tiny leftovers into the previous bus of the same color when they are adjacent.
            val merged = ArrayList<Pair<Int, Int>>()
            for (b in buses) {
                val last = merged.lastOrNull()
                if (last != null && last.first == b.first && last.second + b.second <= maxCap + 8) {
                    merged[merged.size - 1] = last.first to last.second + b.second
                } else merged += b
            }
            val lanes = List(LANES) { ArrayList<Pair<Int, Int>>() }
            for (b in merged) {
                val minLen = lanes.minOf { it.size }
                val options = lanes.indices.filter { lanes[it].size <= minLen + 1 }
                lanes[options[rng.nextInt(options.size)]] += b
            }
            return lanes
        }
    }
}
