package com.puzzel.lab.cartrafficjam.game.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import com.puzzel.lab.cartrafficjam.game.engine.GameEngine
import com.puzzel.lab.cartrafficjam.game.engine.SLOT_COUNT
import com.puzzel.lab.cartrafficjam.game.engine.VIP_SLOT
import com.puzzel.lab.cartrafficjam.game.engine.VState
import com.puzzel.lab.cartrafficjam.game.engine.Vehicle
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

object GameRenderer {

    private val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        textAlign = android.graphics.Paint.Align.CENTER
    }

    fun DrawScope.drawGame(e: GameEngine, timeSec: Float) {
        val scene = e.scene ?: return
        val lay = e.layout
        val u = lay.u
        translate(e.screenShake.x, e.screenShake.y) {
            drawImage(scene)
            if (!lay.loop) SceneAmbient.draw(this, lay, e.theme, timeSec)

            // Locked slots and VIP highlight
            for (i in 0 until SLOT_COUNT) if (!e.slots[i].open) lockedSlot(e, i, timeSec)
            if (e.craneArmed) {
                val r = lay.slotRect(VIP_SLOT)
                val a = 0.4f + 0.3f * sin(timeSec * 8f)
                drawRoundRect(Color(0xFFFFD54F).copy(alpha = a), r.topLeft, r.size, CornerRadius(6 * u), style = Stroke(4 * u))
            }

            if (lay.loop) drawCrowd(e, timeSec) else drawQueue(e, timeSec)

            // Shadows first so no vehicle's shadow falls on another's roof.
            for (v in e.vehicles) if (v.state != VState.GONE) vehicle(e, v, Layer.SHADOW)

            val lot = e.vehicles.filter { it.state == VState.LOT }.sortedBy { it.pos.y }
            for (v in lot) vehicle(e, v, Layer.BODY)
            if (e.craneArmed) {
                val a = 0.35f + 0.25f * sin(timeSec * 9f)
                for (v in lot) drawCircle(Color(0xFFFFE066).copy(alpha = a), e.art.bodyW() * 0.55f, v.pos, style = Stroke(2.5f * u))
            }

            for (v in e.vehicles) if (v.inSlot) vehicle(e, v, Layer.BODY)
            if (e.level.seatScale > 1) for (v in e.vehicles) if (v.inSlot && v.state == VState.PARKED) seatsLeftBadge(e, v)
            val moving = e.vehicles.filter { it.state == VState.EXITING || (it.state == VState.DEPARTING && it.departPhase == 1) }
            for (v in moving.sortedBy { it.pos.y }) vehicle(e, v, Layer.BODY)
            for (v in e.vehicles) if (v.state == VState.CRANE) {
                drawLine(Color(0xFF455A64), Offset(v.pos.x, -10f), v.pos - Offset(0f, 6 * u), 2.5f * u, StrokeCap.Round)
                drawCircle(Color(0xFFFFC107), 5 * u, v.pos - Offset(0f, 6 * u))
                vehicle(e, v, Layer.BODY)
            }

            // Walkers hop into vehicles
            val pw = e.passengerArt.w
            val ph = e.passengerArt.h
            for (w in e.walkers) {
                val p = e.walkerPos(w)
                val s = 1f - w.t * 0.35f
                withTransform({ scale(s, s, p) }) {
                    drawImage(e.passengerArt.get(w.color, (w.t * 8).toInt() % 4), Offset(p.x - pw / 2, p.y - ph))
                }
            }

            e.particles.draw(this)
        }
    }

    private enum class Layer { SHADOW, BODY }

    private fun DrawScope.vehicle(e: GameEngine, v: Vehicle, layer: Layer) {
        val unit = e.layout.unit
        val pop = if (v.popT >= 1f) 1f else easeOutBack(v.popT)
        if (pop <= 0.01f) return
        val bounce = if (v.bounceT >= 0f) 1f + 0.07f * sin(v.bounceT * PI.toFloat()) else 1f
        val flip = if (v.flipT >= 0f) max(0.08f, abs(cos(v.flipT * PI.toFloat()))) else 1f
        val shake = if (v.shakeT >= 0f) sin(v.shakeT * 38f) * 6f * (1f - v.shakeT) else 0f
        val s = v.scale * pop * bounce
        val lift = v.lift
        if (layer == Layer.SHADOW) {
            val bmp = e.art.shadow(v.type)
            val c = v.pos + Offset(unit * 0.22f, unit * 0.36f) * s * (1f + lift * 3f)
            withTransform({
                translate(c.x, c.y)
                rotate(v.heading + shake, Offset.Zero)
                scale(s * flip * (1f + lift * 0.2f), s * (1f + lift * 0.2f), Offset.Zero)
            }) {
                drawImage(bmp, Offset(-bmp.width / 2f, -bmp.height / 2f), alpha = 1f - lift * 0.55f)
            }
            return
        }
        val angle = v.heading + shake
        val comp = e.art.composite(v.type, v.color, v.mystery, angle)
        val raise = lift * 26 * e.layout.u
        withTransform({
            translate(v.pos.x, v.pos.y - raise)
            rotate(e.art.residual(angle), Offset.Zero)
            scale(s * flip, s, Offset.Zero)
        }) {
            drawImage(comp.bitmap, Offset(-comp.groundX, -comp.groundY))
        }
        if (v.mystery && v.state == VState.LOT) {
            // Upright "?" badge so it reads correctly whatever way the vehicle faces.
            val c = Offset(v.pos.x, v.pos.y - raise - comp.roofZ * s)
            val r = e.art.bodyW() * 0.3f * s
            drawCircle(Color(0xFF3E4652), r + 1.5f, c)
            drawCircle(Color.White, r, c)
            textPaint.color = android.graphics.Color.rgb(62, 70, 82)
            textPaint.textSize = r * 1.5f
            drawContext.canvas.nativeCanvas.drawText("?", c.x, c.y + r * 0.52f, textPaint)
        }
        if (v.inSlot || v.state == VState.DEPARTING) {
            withTransform({
                translate(v.pos.x, v.pos.y - raise - comp.roofZ * s)
                rotate(v.heading, Offset.Zero)
                scale(s, s, Offset.Zero)
            }) { seats(e, v) }
        }
    }

    /** Little passenger heads poking out of the roof of a parked vehicle. */
    private fun DrawScope.seats(e: GameEngine, v: Vehicle) {
        val roof = e.art.roofRect(v.type)
        // Big Crowd Loop buses show one head per [seatScale] riders.
        val heads = v.type.seats
        val shown = v.filled * heads / v.seats
        val rowsN = heads / 2
        val step = roof.height * 0.9f / rowsN
        val r = min(roof.width * 0.24f, step * 0.44f)
        val col = jamColor(v.color)
        val startY = roof.top + roof.height * 0.05f + step / 2
        for (i in 0 until heads) {
            val cx = if (i % 2 == 0) -roof.width * 0.24f else roof.width * 0.24f
            val c = Offset(cx, startY + (i / 2) * step)
            if (i < shown) {
                drawCircle(col.shade(0.55f), r + 1f, c + Offset(0f, 1f))
                drawCircle(
                    androidx.compose.ui.graphics.Brush.radialGradient(listOf(col.tint(0.5f), col, col.shade(0.8f)), c - Offset(r * 0.4f, r * 0.4f), r * 1.5f),
                    r, c,
                )
                drawCircle(Color.White.copy(alpha = 0.7f), r * 0.25f, c - Offset(r * 0.38f, r * 0.38f))
            } else {
                drawCircle(Color.Black.copy(alpha = 0.22f), r * 0.75f, c)
            }
        }
    }

    /** Seats still free, on a tag at the foot of the stall. */
    private fun DrawScope.seatsLeftBadge(e: GameEngine, v: Vehicle) {
        val u = e.layout.u
        val r = e.layout.slotRect(v.slot)
        val left = v.seats - v.filled
        val c = Offset(r.center.x, r.bottom + 3 * u)
        val w = 26 * u
        val h = 13 * u
        val col = jamColor(v.color)
        drawRoundRect(col.shade(0.5f), Offset(c.x - w / 2, c.y - h / 2 + 1.5f * u), Size(w, h), CornerRadius(5 * u))
        drawRoundRect(col.shade(0.85f), Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(5 * u))
        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 10 * u
        drawContext.canvas.nativeCanvas.drawText("$left", c.x, c.y + 3.6f * u, textPaint)
    }

    private val crowdOrder = ArrayList<Int>()

    /** Crowd Loop: hundreds of riders packed four abreast, drawn back to front. */
    private fun DrawScope.drawCrowd(e: GameEngine, time: Float) {
        val lay = e.layout
        val pw = e.passengerArt.w
        val ph = e.passengerArt.h
        val maxS = lay.queueVisibleLength + 30 * lay.u
        crowdOrder.clear()
        var i = e.qHead
        while (i < e.queue.size) {
            if (e.qS[i] <= maxS) crowdOrder += i
            else if (e.queueTarget(i) > maxS) break
            i++
        }
        val ys = HashMap<Int, Float>(crowdOrder.size * 2)
        for (k in crowdOrder) ys[k] = e.queuePos(k).y
        crowdOrder.sortBy { ys[it] }
        for (k in crowdOrder) {
            val p = e.queuePos(k)
            val walking = e.qS[k] - e.queueTarget(k) > 1f
            val bob = if (walking) 0f else sin(time * 3.4f + k * 0.9f) * 0.5f * lay.u
            drawOval(Color.Black.copy(alpha = 0.2f), Offset(p.x - pw * 0.4f, p.y - ph * 0.08f), Size(pw * 0.8f, ph * 0.16f))
            val frame = if (walking) ((time * 9f).toInt() + k) % 4 else 4
            drawImage(e.passengerArt.get(e.queue[k], frame), Offset(p.x - pw / 2, p.y - ph - bob))
        }
    }

    private fun DrawScope.drawQueue(e: GameEngine, time: Float) {
        val lay = e.layout
        val pw = e.passengerArt.w
        val ph = e.passengerArt.h
        val visible = (lay.queueVisibleLength / lay.passengerSpacing).toInt() + 1
        val last = min(e.queue.size - 1, e.qHead + visible)
        for (i in last downTo e.qHead) {
            val s = e.qS[i]
            if (s > lay.queueVisibleLength + 4) continue
            val p = lay.queuePoint(s)
            val target = (i - e.qHead) * lay.passengerSpacing
            val walking = s - target > 1f
            val bob = if (walking) 0f else sin(time * 3f + i * 0.7f) * 0.6f * lay.u
            drawOval(Color.Black.copy(alpha = 0.18f), Offset(p.x - pw * 0.42f, p.y - ph * 0.08f), Size(pw * 0.84f, ph * 0.18f))
            val frame = if (walking) ((time * 9f).toInt() + i) % 4 else 4
            drawImage(e.passengerArt.get(e.queue[i], frame), Offset(p.x - pw / 2, p.y - ph - bob))
        }
    }

    private fun DrawScope.lockedSlot(e: GameEngine, i: Int, time: Float) {
        val u = e.layout.u
        val r = e.layout.slotRect(i)
        drawRoundRect(Color(0xFF3C4250).copy(alpha = 0.5f), r.topLeft + Offset(1.5f * u, 0f), androidx.compose.ui.geometry.Size(r.width - 3 * u, r.height), CornerRadius(3 * u))
        // Striped barrier arm across the stall
        val by = r.top + r.height * 0.62f
        val bh = 6 * u
        drawRect(Color.Black.copy(alpha = 0.25f), Offset(r.left, by + 2 * u), androidx.compose.ui.geometry.Size(r.width, bh))
        var x = r.left
        var k = 0
        while (x < r.right) {
            val wdt = minOf(6 * u, r.right - x)
            drawRect(if (k % 2 == 0) Color(0xFFFFC107) else Color(0xFF263238), Offset(x, by), androidx.compose.ui.geometry.Size(wdt, bh))
            x += 6 * u; k++
        }
        drawCircle(Color(0xFFE53935), 2.2f * u, Offset(r.left + 3 * u, by + bh / 2))
        val c = Offset(r.center.x, r.top + 18 * u)
        val pulse = 1f + 0.08f * sin(time * 5f + i)
        drawCircle(Color(0xFF2E7D32), 11 * u * pulse, c + Offset(0f, 1.5f * u))
        drawCircle(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color(0xFF9CE65C), Color(0xFF4CAF50)), c.y - 11 * u, c.y + 11 * u), 11 * u * pulse, c)
        drawLine(Color.White, c - Offset(5.5f * u, 0f), c + Offset(5.5f * u, 0f), 3 * u, StrokeCap.Round)
        drawLine(Color.White, c - Offset(0f, 5.5f * u), c + Offset(0f, 5.5f * u), 3 * u, StrokeCap.Round)
        val b = Offset(r.center.x, r.top + 38 * u)
        drawRoundRect(Color(0xFFE65100), b - Offset(13 * u, 7 * u), androidx.compose.ui.geometry.Size(26 * u, 15 * u), CornerRadius(4 * u))
        drawRoundRect(Color(0xFFFFB300), b - Offset(13 * u, 8 * u), androidx.compose.ui.geometry.Size(26 * u, 14 * u), CornerRadius(4 * u))
        textPaint.color = android.graphics.Color.WHITE
        textPaint.textSize = 10 * u
        drawContext.canvas.nativeCanvas.drawText("AD", b.x, b.y + 2.6f * u, textPaint)
    }

    private fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1
        val x = t - 1
        return 1 + c3 * x * x * x + c1 * x * x
    }
}
