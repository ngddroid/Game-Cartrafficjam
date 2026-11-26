package com.ngstudio.trafficjam.game.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

enum class PKind { SMOKE, SPARK, CONFETTI, STAR, RING }

class Particle(
    val kind: PKind,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val life: Float,
    val size: Float,
    val color: Color,
    var rot: Float = 0f,
    val spin: Float = 0f,
    val gravity: Float = 0f,
) {
    var age = 0f
    val p get() = (age / life).coerceIn(0f, 1f)
}

class Particles(private val u: Float) {
    val list = ArrayList<Particle>(256)
    private val rnd = Random(7)

    fun update(dt: Float) {
        val it = list.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.age += dt
            if (p.age >= p.life) { it.remove(); continue }
            p.vy += p.gravity * dt
            if (p.kind == PKind.SMOKE) { p.vx *= 0.92f; p.vy *= 0.92f }
            if (p.kind == PKind.CONFETTI) p.vx *= 0.985f
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.rot += p.spin * dt
        }
        while (list.size > 400) list.removeAt(0)
    }

    fun smoke(at: Offset, dirX: Float = 0f, dirY: Float = 0f, count: Int = 3) {
        repeat(count) {
            list += Particle(
                PKind.SMOKE, at.x + rnd.nextFloat() * 4 * u, at.y + rnd.nextFloat() * 4 * u,
                dirX * 60 * u + (rnd.nextFloat() - 0.5f) * 40 * u, dirY * 60 * u + (rnd.nextFloat() - 0.5f) * 40 * u - 10 * u,
                0.45f + rnd.nextFloat() * 0.3f, (5 + rnd.nextFloat() * 5) * u, Color(0xFFF2F2F2),
            )
        }
    }

    fun sparks(at: Offset, count: Int = 10, color: Color = Color(0xFFFFE066)) {
        repeat(count) {
            val a = rnd.nextFloat() * 2 * PI.toFloat()
            val s = (80 + rnd.nextFloat() * 160) * u
            list += Particle(PKind.SPARK, at.x, at.y, cos(a) * s, sin(a) * s, 0.25f + rnd.nextFloat() * 0.2f, (1.6f + rnd.nextFloat() * 1.4f) * u, color)
        }
    }

    fun ring(at: Offset, color: Color = Color.White, size: Float = 26f) {
        list += Particle(PKind.RING, at.x, at.y, 0f, 0f, 0.4f, size * u, color)
    }

    fun stars(at: Offset, count: Int, color: Color = Color(0xFFFFF59D)) {
        repeat(count) {
            val a = rnd.nextFloat() * 2 * PI.toFloat()
            val s = (30 + rnd.nextFloat() * 90) * u
            list += Particle(PKind.STAR, at.x, at.y, cos(a) * s, sin(a) * s - 30 * u, 0.6f + rnd.nextFloat() * 0.4f, (3 + rnd.nextFloat() * 4) * u, color, spin = 180f)
        }
    }

    fun confetti(width: Float, top: Float, count: Int) {
        val colors = listOf(0xFFF2453D, 0xFF2F7CF6, 0xFF3DBE4B, 0xFFFFC21A, 0xFFFF6FB5, 0xFF9B5CF0, 0xFF23C9D9)
        repeat(count) {
            list += Particle(
                PKind.CONFETTI, rnd.nextFloat() * width, top - rnd.nextFloat() * 120 * u,
                (rnd.nextFloat() - 0.5f) * 120 * u, (60 + rnd.nextFloat() * 120) * u,
                2.2f + rnd.nextFloat() * 1.2f, (4 + rnd.nextFloat() * 4) * u, Color(colors[rnd.nextInt(colors.size)]),
                rot = rnd.nextFloat() * 360f, spin = (rnd.nextFloat() - 0.5f) * 720f, gravity = 120 * u,
            )
        }
    }

    fun draw(scope: DrawScope) = with(scope) {
        for (p in list) {
            val a = 1f - p.p
            when (p.kind) {
                PKind.SMOKE -> drawCircle(p.color.copy(alpha = 0.75f * a), p.size * (0.6f + p.p * 1.2f), Offset(p.x, p.y))
                PKind.SPARK -> drawCircle(p.color.copy(alpha = a), p.size * (1f - p.p * 0.5f), Offset(p.x, p.y))
                PKind.RING -> drawCircle(p.color.copy(alpha = 0.8f * a), p.size * (0.3f + p.p), Offset(p.x, p.y), style = Stroke(3 * u * a + 0.5f))
                PKind.STAR -> rotate(p.rot, Offset(p.x, p.y)) { drawPath(star(p.x, p.y, p.size * (1f - p.p * 0.4f)), p.color.copy(alpha = a)) }
                PKind.CONFETTI -> rotate(p.rot, Offset(p.x, p.y)) {
                    drawRect(p.color.copy(alpha = if (p.p > 0.8f) (1 - p.p) * 5 else 1f), Offset(p.x - p.size / 2, p.y - p.size / 4), Size(p.size, p.size / 2))
                }
            }
        }
    }

    private fun star(cx: Float, cy: Float, r: Float) = Path().apply {
        moveTo(cx, cy - r)
        quadraticTo(cx, cy, cx + r, cy)
        quadraticTo(cx, cy, cx, cy + r)
        quadraticTo(cx, cy, cx - r, cy)
        quadraticTo(cx, cy, cx, cy - r)
        close()
    }
}
