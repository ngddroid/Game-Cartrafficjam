package com.ngstudio.trafficjam.game.engine

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import com.ngstudio.trafficjam.core.audio.Haptic
import com.ngstudio.trafficjam.core.audio.Sfx
import com.ngstudio.trafficjam.game.gen.Grid
import com.ngstudio.trafficjam.game.model.Level
import com.ngstudio.trafficjam.game.model.VehicleSpec
import com.ngstudio.trafficjam.game.model.VehicleType
import com.ngstudio.trafficjam.game.render.PassengerArt
import com.ngstudio.trafficjam.game.render.SceneArt
import com.ngstudio.trafficjam.game.render.SceneTheme
import com.ngstudio.trafficjam.game.render.Skin
import com.ngstudio.trafficjam.game.render.VehicleArt
import com.ngstudio.trafficjam.game.render.jamColor
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.random.Random

interface GameFx {
    fun sfx(s: Sfx, rate: Float = 1f)
    fun haptic(h: Haptic)
}

sealed interface GameEvent {
    data class Won(val stars: Int) : GameEvent
    data object Stuck : GameEvent
    data class LockedSlot(val index: Int) : GameEvent
    data object NoSpace : GameEvent
    data object VipBusy : GameEvent
}

enum class Phase { INTRO, PLAYING, STUCK, WON }

enum class VState { LOT, EXITING, PARKED, DEPARTING, CRANE, GONE }

class Vehicle(val spec: VehicleSpec, seatScale: Int = 1) {
    val id get() = spec.id
    val type get() = spec.type
    val seats = spec.type.seats * seatScale
    var color = spec.color
    var mystery = spec.mystery
    var state = VState.LOT
    var slot = -1
    var filled = 0
    var reserved = 0
    var parkedOrder = 0

    // Transform
    var pos = Offset.Zero
    var home = Offset.Zero
    var heading = 0f
    var scale = 1f
    var lift = 0f

    // Animation clocks (seconds; < 0 means idle)
    var t = 0f
    var duration = 0f
    var measure: PathMeasure? = null
    var pathLen = 0f
    var startScale = 1f
    var endScale = 1f
    var bumpT = -1f
    var bumpPx = 0f
    var bumped = false
    var shakeT = -1f
    var popT = 0f
    var popDelay = 0f
    var bounceT = -1f
    var flipT = -1f
    var departDelay = -1f
    var departPhase = 0
    var departFromX = 0f
    var smokeClock = 0f
    var craneFrom = Offset.Zero
    var craneCtrl = Offset.Zero
    var craneTo = Offset.Zero
    var startHeading = 0f

    val inSlot get() = state == VState.PARKED || (state == VState.DEPARTING && departPhase == 0)
}

class Walker(val color: Int, val from: Offset, val vehicle: Vehicle) {
    var t = 0f
}

class Slot(var open: Boolean) {
    var vehicle: Vehicle? = null
    val free get() = open && vehicle == null
}

/**
 * Owns all live game state and advances it each frame. Rendering reads it directly; Compose
 * observes only the small set of snapshot states below.
 */
class GameEngine(
    val level: Level,
    val theme: SceneTheme,
    private val skin: Skin,
    private val fx: GameFx,
    var onEvent: (GameEvent) -> Unit = {},
) {
    var frame by mutableLongStateOf(0L)
        private set
    var phase by mutableStateOf(Phase.INTRO)
        private set
    var remaining by mutableIntStateOf(level.queue.size)
        private set
    var craneArmed by mutableStateOf(false)
    var tutorialPoint by mutableStateOf<Offset?>(null)
        private set
    var tutorialText by mutableStateOf<String?>(null)
        private set

    var boostersUsed = 0
        private set
    var revivesUsed = 0
        private set

    val vehicles = level.vehicles.map { Vehicle(it, level.seatScale) }
    private val loop get() = layout.loop
    private val byId = vehicles.associateBy { it.id }
    val grid = Grid(level.cols, level.rows).also { g -> level.vehicles.forEach { g.put(it) } }
    val slots = Array(SLOT_COUNT + 1) { i -> Slot(i < level.openSlots || i == VIP_SLOT) }

    val queue = level.queue.copyOf()
    var qHead = 0
        private set
    lateinit var qS: FloatArray
        private set
    val walkers = ArrayList<Walker>()

    lateinit var layout: GameLayout
        private set
    lateinit var art: VehicleArt
        private set
    lateinit var passengerArt: PassengerArt
        private set
    var scene by mutableStateOf<ImageBitmap?>(null)
        private set
    lateinit var particles: Particles
        private set

    private var time = 0f
    private var parkCounter = 0
    private var boardCooldown = 0f
    private var stuckTimer = 0f
    private var winTimer = -1f
    private var popIndex = 0
    private var shakeScreen = 0f
    private val rnd = Random(level.number)

    val screenShake: Offset
        get() = if (shakeScreen <= 0f) Offset.Zero else Offset(
            sin(time * 90f) * 3f * layout.u * shakeScreen, cos(time * 77f) * 2f * layout.u * shakeScreen,
        )

    val ready get() = scene != null

    // --- Setup ------------------------------------------------------------------------------

    fun setSize(width: Float, height: Float, topInset: Float, bottomInset: Float) {
        if (::layout.isInitialized && layout.width == width && layout.height == height &&
            layout.topInset == topInset && layout.bottomInset == bottomInset
        ) return
        layout = GameLayout(width, height, topInset, bottomInset, level)
        art = VehicleArt(layout.unit, skin)
        passengerArt = PassengerArt(layout.passengerW, layout.passengerH)
        particles = Particles(layout.u)
        scene = SceneArt.render(layout, level, theme)
        qS = FloatArray(queue.size) { i -> queueTarget(i) + layout.width * 0.6f }
        vehicles.forEachIndexed { i, v ->
            v.home = layout.boardToScreen(v.spec.centerX, v.spec.centerY)
            if (v.state == VState.LOT) {
                v.pos = v.home
                v.heading = layout.rotationDeg + v.spec.dir.degrees
            }
            v.popDelay = 0.15f + i * min(0.018f, 0.7f / vehicles.size) + rnd.nextFloat() * 0.05f
        }
        // Pre-build the 3D sprites the level opens with, plus the parked pose of each kind.
        for (v in vehicles) {
            art.composite(v.type, v.color, v.mystery, v.heading)
            art.composite(v.type, v.color, false, 0f)
            art.shadow(v.type)
        }
    }

    private fun headingVec(deg: Float): Offset {
        val r = deg * PI.toFloat() / 180f
        return Offset(sin(r), -cos(r))
    }

    private val slotScale get() = layout.slotUnit / layout.unit

    fun slotCenter(i: Int) = layout.slotRect(i).center

    // --- Input ------------------------------------------------------------------------------

    fun onTap(p: Offset) {
        if (!ready || phase != Phase.PLAYING) return
        particles.ring(p)

        for (i in 0 until SLOT_COUNT) {
            if (!slots[i].open && layout.slotRect(i).inflate(2 * layout.u).contains(p)) {
                fx.sfx(Sfx.Click)
                onEvent(GameEvent.LockedSlot(i))
                return
            }
        }

        val v = hitVehicle(p) ?: return
        fx.haptic(Haptic.Tap)
        if (craneArmed) {
            crane(v)
            return
        }
        if (v.bumpT >= 0f) return
        val d = grid.blockDistance(v.spec)
        if (d >= 0) {
            v.bumpT = 0f
            v.bumped = false
            v.bumpPx = max(0.2f, d + 0.1f) * layout.unit
            fx.sfx(Sfx.Tap)
            return
        }
        val slot = (0 until SLOT_COUNT).firstOrNull { slots[it].free }
        if (slot == null) {
            v.shakeT = 0f
            fx.sfx(Sfx.Warning)
            fx.haptic(Haptic.Bump)
            onEvent(GameEvent.NoSpace)
            return
        }
        drive(v, slot)
    }

    private fun hitVehicle(p: Offset): Vehicle? {
        val b = layout.screenToBoard(p)
        val gx = b.x.toInt()
        val gy = b.y.toInt()
        if (b.x >= 0 && b.y >= 0) {
            val id = grid.at(gx, gy)
            if (id >= 0) return byId[id]
        }
        // Forgiving fallback: nearest footprint within ~0.8 units.
        var best: Vehicle? = null
        var bestD = 0.8f
        for (v in vehicles) {
            if (v.state != VState.LOT) continue
            val s = v.spec
            val dx = max(0f, max(s.x - b.x, b.x - (s.x + s.w)))
            val dy = max(0f, max(s.y - b.y, b.y - (s.y + s.h)))
            val d = hypot(dx, dy)
            if (d < bestD) { bestD = d; best = v }
        }
        return best
    }

    // --- Movement ---------------------------------------------------------------------------

    private fun drive(v: Vehicle, slot: Int) {
        grid.remove(v.spec)
        slots[slot].vehicle = v
        v.slot = slot
        v.state = VState.EXITING
        v.t = 0f
        v.startScale = 1f
        v.endScale = slotScale
        val pts = exitPoints(v.pos, headingVec(v.heading), slotCenter(slot))
        val path = roundedPath(pts, 16 * layout.u)
        v.measure = PathMeasure().apply { setPath(path, false) }
        v.pathLen = v.measure!!.length
        v.duration = (0.3f + v.pathLen / (1250 * layout.u)).coerceIn(0.5f, 1.35f)
        particles.smoke(v.pos - headingVec(v.heading) * (art.bodyL(v.type) / 2), -headingVec(v.heading).x, -headingVec(v.heading).y, 5)
        fx.sfx(Sfx.EngineGo, 0.95f + rnd.nextFloat() * 0.15f)
        afterLotChange()
    }

    private fun exitPoints(start: Offset, d: Offset, slot: Offset): List<Offset> {
        val t = layout.track
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
        val hit = Offset(
            (start.x + d.x * k).coerceIn(t.left, t.right),
            (start.y + d.y * k).coerceIn(t.top, t.bottom),
        )
        val pts = arrayListOf(start, hit)
        val onTop = ty <= tx && d.y < 0
        val onBottom = ty <= tx && d.y > 0
        val onRight = tx < ty && d.x > 0
        when {
            onTop -> Unit
            onBottom -> {
                val goLeft = (hit.x - t.left) < (t.right - hit.x)
                val sx = if (goLeft) t.left else t.right
                pts += Offset(sx, t.bottom)
                pts += Offset(sx, t.top)
            }
            onRight -> pts += Offset(t.right, t.top)
            else -> pts += Offset(t.left, t.top)
        }
        pts += Offset(slot.x, t.top)
        pts += slot
        // drop near-duplicates
        val out = ArrayList<Offset>()
        for (p in pts) if (out.isEmpty() || (out.last() - p).getDistance() > 2f) out += p
        return out
    }

    private fun roundedPath(pts: List<Offset>, radius: Float): Path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size - 1) {
            val a = pts[i - 1]
            val b = pts[i]
            val c = pts[i + 1]
            val inLen = (b - a).getDistance()
            val outLen = (c - b).getDistance()
            val r = min(radius, min(inLen, outLen) / 2)
            val p1 = b - (b - a) / inLen * r
            val p2 = b + (c - b) / outLen * r
            lineTo(p1.x, p1.y)
            quadraticTo(b.x, b.y, p2.x, p2.y)
        }
        lineTo(pts.last().x, pts.last().y)
    }

    private fun crane(v: Vehicle) {
        val vip = slots[VIP_SLOT]
        if (vip.vehicle != null) {
            onEvent(GameEvent.VipBusy)
            craneArmed = false
            return
        }
        craneArmed = false
        boostersUsed++
        grid.remove(v.spec)
        vip.vehicle = v
        v.slot = VIP_SLOT
        v.state = VState.CRANE
        v.t = 0f
        v.duration = 1.0f
        v.craneFrom = v.pos
        v.craneTo = slotCenter(VIP_SLOT)
        v.craneCtrl = Offset((v.pos.x + v.craneTo.x) / 2, min(v.pos.y, v.craneTo.y) - 140 * layout.u)
        v.startHeading = v.heading
        v.startScale = v.scale
        v.endScale = min(slotScale, layout.slotRect(VIP_SLOT).width / (VehicleType.WIDTH * 1.25f) / layout.unit)
        fx.sfx(Sfx.Crane)
        particles.stars(v.pos, 10)
        afterLotChange()
    }

    private fun afterLotChange() {
        for (o in vehicles) {
            if (o.state == VState.LOT && o.mystery && grid.isFree(o.spec)) {
                o.mystery = false
                o.flipT = 0f
                particles.stars(o.pos, 6, jamColor(o.color))
                fx.sfx(Sfx.Pop2, 1.2f)
            }
        }
    }

    // --- Boosters & rewards -----------------------------------------------------------------

    fun unlockSlot(i: Int) {
        if (slots[i].open) return
        slots[i].open = true
        fx.sfx(Sfx.Unlock)
        val c = slotCenter(i)
        particles.stars(c, 14)
        particles.ring(c, androidx.compose.ui.graphics.Color(0xFFFFE066), 40f)
        if (phase == Phase.STUCK) phase = Phase.PLAYING
        stuckTimer = 0f
    }

    fun revive() {
        revivesUsed++
        val locked = (0 until SLOT_COUNT).firstOrNull { !slots[it].open }
        if (locked != null) unlockSlot(locked) else sortQueue()
        phase = Phase.PLAYING
        stuckTimer = 0f
    }

    fun armCrane(): Boolean {
        if (slots[VIP_SLOT].vehicle != null) {
            onEvent(GameEvent.VipBusy)
            return false
        }
        craneArmed = true
        fx.sfx(Sfx.Booster)
        return true
    }

    /** Recolours lot vehicles so the free ones match what the queue needs next. */
    fun shuffle() {
        boostersUsed++
        val lot = vehicles.filter { it.state == VState.LOT }
        if (lot.isEmpty()) return
        // Demand left after parked vehicles take what they still have room for.
        val room = IntArray(8)
        for (s in slots) s.vehicle?.let { if (it.state != VState.DEPARTING) room[it.color] += it.seats - it.filled - it.reserved }
        val demand = ArrayList<Int>()
        for (i in qHead until queue.size) {
            val c = queue[i]
            if (room[c] > 0) room[c]-- else demand += c
        }
        val pool = HashMap<VehicleType, MutableList<Int>>()
        for (v in lot) pool.getOrPut(v.type) { mutableListOf() } += v.color
        val ordered = lot.sortedWith(compareBy<Vehicle> { if (grid.isFree(it.spec)) 0 else 1 }.thenByDescending { it.id })
        for (v in ordered) {
            val avail = pool[v.type]!!
            var chosen = -1
            var i = 0
            while (i < demand.size && chosen < 0) {
                if (demand[i] in avail) chosen = demand[i]
                i++
            }
            if (chosen < 0) chosen = avail.first()
            avail.remove(chosen)
            // consume that colour's demand
            var need = v.seats
            var j = 0
            while (j < demand.size && need > 0) {
                if (demand[j] == chosen) { demand.removeAt(j); need-- } else j++
            }
            v.color = chosen
            v.flipT = 0f
            v.popT = 0.6f
        }
        fx.sfx(Sfx.Booster)
        lot.forEach { particles.stars(it.pos, 2) }
    }

    /** Moves passengers that parked vehicles are waiting for to the front of the line. */
    fun sortQueue() {
        val needs = slots.mapNotNull { s -> s.vehicle?.takeIf { it.state == VState.PARKED || it.state == VState.EXITING || it.state == VState.CRANE } }
            .sortedBy { it.parkedOrder }
        val rest = ArrayList<Int>()
        for (i in qHead until queue.size) { rest += i }
        val front = ArrayList<Int>()
        for (v in needs) {
            var need = v.seats - v.filled - v.reserved
            val it = rest.iterator()
            while (it.hasNext() && need > 0) {
                val idx = it.next()
                if (queue[idx] == v.color) { front += idx; it.remove(); need-- }
            }
        }
        val order = front + rest
        val newColors = order.map { queue[it] }
        val newPos = order.map { qS[it] }
        for (k in order.indices) {
            queue[qHead + k] = newColors[k]
            qS[qHead + k] = newPos[k]
        }
        fx.sfx(Sfx.Whoosh)
    }

    fun useSort() {
        boostersUsed++
        sortQueue()
        fx.sfx(Sfx.Booster)
        phase = if (phase == Phase.STUCK) Phase.PLAYING else phase
        stuckTimer = 0f
    }

    // --- Frame update -----------------------------------------------------------------------

    fun update(dtRaw: Float) {
        if (!ready) return
        val dt = min(dtRaw, 1 / 30f)
        time += dt
        shakeScreen = max(0f, shakeScreen - dt * 4f)
        particles.update(dt)

        if (phase == Phase.INTRO) {
            if (vehicles.all { it.popT >= 1f } && time > 0.5f) phase = Phase.PLAYING
        }

        updateVehicles(dt)
        updateQueue(dt)
        updateWalkers(dt)
        checkEnd(dt)
        updateTutorial()
        remaining = queue.size - qHead
        frame++
    }

    private fun updateVehicles(dt: Float) {
        val u = layout.u
        for (v in vehicles) {
            if (v.popT < 1f) {
                if (time > v.popDelay) v.popT = min(1f, v.popT + dt / 0.35f)
            }
            if (v.flipT >= 0f) { v.flipT += dt / 0.35f; if (v.flipT >= 1f) v.flipT = -1f }
            if (v.bounceT >= 0f) { v.bounceT += dt / 0.25f; if (v.bounceT >= 1f) v.bounceT = -1f }
            if (v.shakeT >= 0f) { v.shakeT += dt / 0.35f; if (v.shakeT >= 1f) v.shakeT = -1f }

            when (v.state) {
                VState.LOT -> {
                    var off = 0f
                    if (v.bumpT >= 0f) {
                        v.bumpT += dt
                        val hit = 0.1f
                        if (v.bumpT < hit) {
                            val p = v.bumpT / hit
                            off = v.bumpPx * p * p
                        } else {
                            if (!v.bumped) {
                                v.bumped = true
                                fx.sfx(Sfx.Bump, 0.9f + rnd.nextFloat() * 0.2f)
                                fx.haptic(Haptic.Bump)
                                val hv = headingVec(v.heading)
                                val nose = v.home + hv * (v.bumpPx + art.bodyL(v.type) / 2)
                                particles.sparks(nose, 8)
                                shakeScreen = 0.5f
                                val blocker = grid.blocker(v.spec)
                                if (blocker >= 0) byId[blocker]?.shakeT = 0f
                            }
                            val tt = v.bumpT - hit
                            off = v.bumpPx * exp(-9f * tt) * cos(20f * tt)
                            if (tt > 0.38f) { v.bumpT = -1f; off = 0f }
                        }
                    }
                    v.pos = v.home + headingVec(v.heading) * off
                }
                VState.EXITING -> {
                    v.t += dt
                    val p = (v.t / v.duration).coerceIn(0f, 1f)
                    val e = 1f - (1f - p).pow(2.3f)
                    val m = v.measure!!
                    val d = v.pathLen * e
                    v.pos = m.getPosition(d)
                    val tan = m.getTangent(d)
                    if (tan.getDistance() > 0.01f) v.heading = atan2(tan.x, -tan.y) * 180f / PI.toFloat()
                    v.scale = v.startScale + (v.endScale - v.startScale) * min(1f, p * 1.3f)
                    v.smokeClock += dt
                    if (v.smokeClock > 0.07f && p < 0.85f) {
                        v.smokeClock = 0f
                        val back = v.pos - headingVec(v.heading) * (art.bodyL(v.type) * v.scale / 2)
                        particles.smoke(back, count = 1)
                    }
                    if (p >= 1f) park(v)
                }
                VState.CRANE -> {
                    v.t += dt
                    val p = (v.t / v.duration).coerceIn(0f, 1f)
                    val e = p * p * (3 - 2 * p)
                    val a = v.craneFrom
                    val c = v.craneCtrl
                    val b = v.craneTo
                    val q = 1 - e
                    v.pos = a * (q * q) + c * (2 * q * e) + b * (e * e)
                    v.lift = sin(p * PI.toFloat())
                    v.scale = v.startScale + (v.endScale - v.startScale) * e + v.lift * 0.25f
                    v.heading = v.startHeading + (shortestAngle(v.startHeading, 0f)) * e
                    if (p >= 1f) { v.lift = 0f; park(v) }
                }
                VState.PARKED -> {
                    if (v.departDelay >= 0f) {
                        v.departDelay -= dt
                        if (v.departDelay < 0f) {
                            v.state = VState.DEPARTING
                            v.departPhase = 0
                            v.t = 0f
                        }
                    }
                }
                VState.DEPARTING -> {
                    v.t += dt
                    val sc = slotCenter(v.slot)
                    if (v.departPhase == 0) {
                        val p = (v.t / 0.22f).coerceIn(0f, 1f)
                        val e = p * p * (3 - 2 * p)
                        v.pos = Offset(sc.x, sc.y + (layout.roadY - sc.y) * e)
                        if (p >= 1f) {
                            slots[v.slot].vehicle = null
                            v.departPhase = 1
                            v.t = 0f
                            v.departFromX = v.pos.x
                            fx.sfx(Sfx.Horn, 0.95f + rnd.nextFloat() * 0.1f)
                        }
                    } else {
                        val turn = (v.t / 0.2f).coerceIn(0f, 1f)
                        v.heading = 90f * (turn * turn * (3 - 2 * turn))
                        val dist = 260 * u * v.t + 1400 * u * v.t * v.t
                        v.pos = Offset(v.departFromX + dist, layout.roadY)
                        v.smokeClock += dt
                        if (v.smokeClock > 0.06f) {
                            v.smokeClock = 0f
                            particles.smoke(v.pos - Offset(art.bodyL(v.type) * v.scale / 2, 0f), -1f, 0f, 1)
                        }
                        if (v.pos.x > layout.width + 120 * u) v.state = VState.GONE
                    }
                }
                VState.GONE -> Unit
            }
        }
    }

    private fun shortestAngle(from: Float, to: Float): Float {
        var d = (to - from) % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }

    private fun park(v: Vehicle) {
        v.state = VState.PARKED
        v.pos = slotCenter(v.slot)
        v.heading = 0f
        v.scale = v.endScale
        v.parkedOrder = parkCounter++
        v.bounceT = 0f
        fx.sfx(Sfx.Park)
        particles.smoke(v.pos + Offset(0f, art.bodyL(v.type) * v.scale / 2), 0f, 1f, 3)
    }

    /**
     * Distance from the front of the line where passenger [i] should stand. In Crowd Loop the
     * line moves up a whole row at a time once the front row has boarded.
     */
    fun queueTarget(i: Int): Float =
        if (loop) (i / CROWD_LANES - qHead / CROWD_LANES) * layout.passengerSpacing
        else (i - qHead) * layout.passengerSpacing

    /** Screen position of waiting passenger [i]. */
    fun queuePos(i: Int): Offset =
        if (loop) layout.crowdPoint(qS[i], i % CROWD_LANES) else layout.queuePoint(qS[i])

    private fun updateQueue(dt: Float) {
        val sp = layout.passengerSpacing
        val speed = (if (loop) 150 else 260) * layout.u
        val visible = ((layout.queueVisibleLength / sp).toInt() + 2) * (if (loop) CROWD_LANES else 1)
        for (i in qHead until queue.size) {
            val target = queueTarget(i)
            if (i - qHead > visible) { qS[i] = target; continue }
            val cur = qS[i]
            qS[i] = if (cur > target) max(target, cur - speed * dt) else target
        }

        boardCooldown -= dt
        if (boardCooldown > 0f || qHead >= queue.size) return
        if (qS[qHead] > sp * 0.9f) return
        val c = queue[qHead]
        var best: Vehicle? = null
        for (s in slots) {
            val v = s.vehicle ?: continue
            if (v.state != VState.PARKED || v.color != c || v.departDelay >= 0f) continue
            if (v.filled + v.reserved >= v.seats) continue
            if (best == null || v.parkedOrder < best.parkedOrder) best = v
        }
        val v = best ?: return
        v.reserved++
        walkers += Walker(c, queuePos(qHead), v)
        qHead++
        boardCooldown = if (loop) 0.03f else 0.065f
    }

    private fun updateWalkers(dt: Float) {
        val it = walkers.iterator()
        while (it.hasNext()) {
            val w = it.next()
            w.t += dt / 0.24f
            if (w.t >= 1f) {
                it.remove()
                val v = w.vehicle
                v.reserved--
                v.filled++
                v.bounceT = 0f
                val pops = arrayOf(Sfx.Pop1, Sfx.Pop2, Sfx.Pop3)
                fx.sfx(pops[popIndex++ % 3], 0.95f + (v.filled.toFloat() / v.seats) * 0.25f)
                if (v.filled >= v.seats) {
                    v.departDelay = 0.28f
                    fx.sfx(Sfx.Full)
                    particles.stars(v.pos, 8)
                }
            }
        }
    }

    fun walkerPos(w: Walker): Offset {
        val p = w.t.coerceIn(0f, 1f)
        val to = w.vehicle.pos
        val base = w.from + (to - w.from) * p
        return base - Offset(0f, sin(p * PI.toFloat()) * 18 * layout.u)
    }

    private fun checkEnd(dt: Float) {
        if (phase != Phase.PLAYING) return
        if (vehicles.all { it.state == VState.GONE } && walkers.isEmpty()) {
            if (winTimer < 0f) winTimer = 0f
            winTimer += dt
            if (winTimer > 0.35f) {
                phase = Phase.WON
                particles.confetti(layout.width, layout.topInset, 140)
                val stars = when {
                    boostersUsed + revivesUsed == 0 -> 3
                    boostersUsed + revivesUsed <= 2 -> 2
                    else -> 1
                }
                onEvent(GameEvent.Won(stars))
            }
            return
        }
        val busy = walkers.isNotEmpty() || vehicles.any {
            it.state == VState.EXITING || it.state == VState.CRANE || it.state == VState.DEPARTING ||
                it.departDelay >= 0f || it.bumpT >= 0f
        }
        if (busy || qHead >= queue.size) { stuckTimer = 0f; return }
        val front = queue[qHead]
        val canBoard = slots.any { s ->
            val v = s.vehicle
            v != null && v.state == VState.PARKED && v.color == front && v.filled + v.reserved < v.seats
        }
        val freeSlot = (0 until SLOT_COUNT).any { slots[it].free }
        if (!canBoard && !freeSlot) {
            stuckTimer += dt
            if (stuckTimer > 0.6f) {
                phase = Phase.STUCK
                stuckTimer = 0f
                fx.sfx(Sfx.Warning)
                onEvent(GameEvent.Stuck)
            }
        } else {
            stuckTimer = 0f
        }
    }

    private fun updateTutorial() {
        if (level.number != 1 || level.seatScale != 1 || phase != Phase.PLAYING || qHead >= queue.size) {
            if (tutorialPoint != null) tutorialPoint = null
            if (phase == Phase.WON) tutorialText = null
            return
        }
        val moving = vehicles.any { it.state == VState.EXITING || it.bumpT >= 0f } || walkers.isNotEmpty()
        val front = queue[qHead]
        val parkedMatch = slots.any { it.vehicle?.color == front }
        val lotFree = vehicles.filter { it.state == VState.LOT && grid.isFree(it.spec) }
        val target = if (moving || parkedMatch) null else lotFree.firstOrNull { it.color == front } ?: lotFree.firstOrNull()
        tutorialPoint = target?.home
        val text = when {
            target == null -> tutorialText
            target.color != front -> "This one is blocking the bus. Move it out of the way!"
            target.type == VehicleType.VAN -> "Tap the van! A van carries 6 passengers of its color."
            target.type == VehicleType.CAR -> "Cars carry 4 passengers of the same color."
            else -> "Buses carry 10 passengers. Tap the bus!"
        }
        if (text != tutorialText) tutorialText = text
    }

}
