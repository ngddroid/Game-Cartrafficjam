package com.puzzel.lab.cartrafficjam.game.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.puzzel.lab.cartrafficjam.game.engine.GameLayout
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/** The moving parts of each world's scenery, drawn every frame over the static scene. */
object SceneAmbient {

    fun ferrisCenter(w: Float, base: Float, u: Float) = Offset(w * 0.74f, base - 58 * u)
    fun millHub(w: Float, base: Float, u: Float) = Offset(w * 0.86f, base - 64 * u)

    private val stars = Random(3).let { r -> List(40) { Triple(r.nextFloat(), r.nextFloat(), r.nextFloat() * 6f) } }
    private val flakes = Random(4).let { r -> List(60) { Triple(r.nextFloat(), r.nextFloat(), 0.5f + r.nextFloat()) } }

    /** Parts that turn, frozen at rest, for postcards. */
    fun staticPose(scope: DrawScope, th: SceneTheme, w: Float, base: Float, u: Float) = with(scope) {
        when (th.backdrop) {
            Backdrop.FAIR -> ferris(ferrisCenter(w, base, u), u, 0f)
            Backdrop.FARM -> windmill(millHub(w, base, u), u, 0f)
            else -> Unit
        }
    }

    fun draw(scope: DrawScope, lay: GameLayout, th: SceneTheme, t: Float) = with(scope) {
        val u = lay.u
        val w = lay.width
        val top = lay.topInset
        val base = top + 120 * u
        if (!th.dark && th.backdrop != Backdrop.SNOW) {
            // Drifting clouds high in the sky
            for (k in 0 until 2) {
                val span = w + 160 * u
                val x = ((t * (8 + k * 5) * u + k * span * 0.55f) % span) - 80 * u
                cloud(Offset(x, top + (12 + k * 10) * u), u * (0.5f + k * 0.12f))
            }
            // A pair of birds crossing every ~14 s
            val cycle = (t % 14f) / 14f
            if (cycle < 0.6f) {
                val bx = -20 * u + cycle / 0.6f * (w + 40 * u)
                for (k in 0 until 2) bird(Offset(bx - k * 14 * u, top + (22 + k * 5) * u + sin(t * 2f + k) * 3 * u), u, t + k * 0.3f)
            }
        }
        when (th.backdrop) {
            Backdrop.FAIR -> ferris(ferrisCenter(w, base, u), u, t * 10f)
            Backdrop.FARM -> windmill(millHub(w, base, u), u, t * 40f)
            Backdrop.PIER -> {
                val sea = top + 66 * u
                for (k in 0 until 7) {
                    val y = sea + 10 * u + (k % 3) * 12 * u
                    val x = ((k * 61 * u + t * 14 * u * (1 + k % 2)) % (w + 40 * u)) - 20 * u
                    drawArc(Color.White.copy(alpha = 0.55f), 200f, 140f, false, Offset(x, y), Size(22 * u, 8 * u), style = Stroke(1.5f * u))
                }
                for (k in 0 until 6) {
                    val a = (sin(t * 3f + k * 1.7f) + 1) / 2
                    drawCircle(Color.White.copy(alpha = a * 0.8f), 1.3f * u, Offset((k * 57 % 340 + 10) * u, sea + (8 + k * 5) * u))
                }
            }
            Backdrop.SNOW -> for ((fx, fy, sp) in flakes) {
                val y = ((fy * lay.height + t * 22 * u * sp) % lay.height)
                val x = fx * w + sin(t * sp + fy * 10) * 6 * u
                drawCircle(Color.White.copy(alpha = 0.85f), 1.3f * u * sp, Offset(x, y))
            }
            Backdrop.NEON, Backdrop.SPACE -> {
                for ((sx, sy, ph) in stars) {
                    val a = (sin(t * 2.2f + ph) + 1) / 2
                    if (a > 0.6f) drawCircle(Color.White.copy(alpha = (a - 0.6f) * 2.2f), 1.4f * u, Offset(sx * w, top + sy * 80 * u))
                }
                if (th.backdrop == Backdrop.NEON) {
                    val flick = if ((t * 7).toInt() % 23 == 0) 0.2f else 1f
                    drawRoundRect(Color(0xFFFF4FD8).copy(alpha = 0.25f * flick), Offset(w * 0.36f - 4 * u, base - 78 * u), Size(78 * u, 30 * u), CornerRadius(8 * u))
                } else {
                    val blink = if ((t * 1.5f).toInt() % 2 == 0) 1f else 0.15f
                    drawCircle(Color(0xFFFF5252).copy(alpha = blink), 2.6f * u, Offset(w * 0.52f + 14 * u, base - 47 * u))
                    val shoot = (t % 9f) / 9f
                    if (shoot < 0.12f) {
                        val p = shoot / 0.12f
                        val a = Offset(w * (0.2f + p * 0.5f), top + (10 + p * 30) * u)
                        drawLine(Brush.linearGradient(listOf(Color.Transparent, Color.White), a - Offset(30 * u, 12 * u), a), a - Offset(30 * u, 12 * u), a, 1.6f * u, StrokeCap.Round)
                    }
                }
            }
            Backdrop.JUNGLE -> for (k in 0 until 8) {
                val x = (k * 47 % 330 + 15) * u + sin(t * 0.8f + k) * 10 * u
                val y = base - (20 + (k * 13) % 60) * u + cos(t * 1.1f + k * 2) * 6 * u
                val a = (sin(t * 3f + k) + 1) / 2
                drawCircle(Color(0x55FFFF8D).copy(alpha = 0.35f * a), 5 * u, Offset(x, y))
                drawCircle(Color(0xFFFFFF8D).copy(alpha = a), 1.6f * u, Offset(x, y))
            }
            Backdrop.DESERT -> {
                val p = (t % 11f) / 11f
                val x = -20 * u + p * (w + 40 * u)
                val y = lay.queueY - 4 * u - abs(sin(p * 40f)) * 8 * u
                rotate(t * 300f, Offset(x, y)) {
                    drawCircle(Color(0xFFB98A45), 7 * u, Offset(x, y), style = Stroke(1.6f * u))
                    drawLine(Color(0xFFB98A45), Offset(x - 6 * u, y), Offset(x + 6 * u, y), 1.2f * u)
                    drawLine(Color(0xFFB98A45), Offset(x, y - 6 * u), Offset(x, y + 6 * u), 1.2f * u)
                }
            }
            else -> Unit
        }
    }

    private fun DrawScope.cloud(c: Offset, u: Float) {
        val col = Color.White.copy(alpha = 0.85f)
        drawCircle(col, 14 * u, c)
        drawCircle(col, 11 * u, c + Offset(-16 * u, 4 * u))
        drawCircle(col, 12 * u, c + Offset(16 * u, 3 * u))
        drawRoundRect(col, c + Offset(-26 * u, 2 * u), Size(52 * u, 12 * u), CornerRadius(6 * u))
    }

    private fun DrawScope.bird(c: Offset, u: Float, t: Float) {
        val flap = sin(t * 12f) * 4 * u
        val p = Path().apply {
            moveTo(c.x - 6 * u, c.y - flap)
            quadraticTo(c.x - 3 * u, c.y - 2 * u, c.x, c.y)
            quadraticTo(c.x + 3 * u, c.y - 2 * u, c.x + 6 * u, c.y - flap)
        }
        drawPath(p, Color(0xFF37474F), style = Stroke(1.5f * u, cap = StrokeCap.Round))
    }

    private fun DrawScope.ferris(c: Offset, u: Float, deg: Float) {
        val r = 50 * u
        drawCircle(Color(0xFF8E24AA), r, c, style = Stroke(4 * u))
        drawCircle(Color(0xFFCE93D8), r - 6 * u, c, style = Stroke(1.5f * u))
        val colors = listOf(Color(0xFFFF7043), Color(0xFF42A5F5), Color(0xFFFFCA28), Color(0xFF66BB6A))
        for (k in 0 until 10) {
            val a = (k * 36.0 + deg) * PI / 180
            val p = Offset(c.x + (r * cos(a)).toFloat(), c.y + (r * sin(a)).toFloat())
            drawLine(Color(0xFFCE93D8), c, p, 1.4f * u)
            drawCircle(Color(0xFFFFEB3B), 1.8f * u, p)
            drawLine(Color(0xFF6A1B9A), p, p + Offset(0f, 5 * u), 1f * u)
            drawRoundRect(colors[k % 4], p + Offset(-6 * u, 4 * u), Size(12 * u, 9 * u), CornerRadius(3 * u))
            drawRect(Color.White.copy(alpha = 0.6f), p + Offset(-4 * u, 5.5f * u), Size(8 * u, 3 * u))
        }
        drawCircle(Color(0xFFFFD54F), 5 * u, c)
    }

    private fun DrawScope.windmill(c: Offset, u: Float, deg: Float) {
        rotate(deg, c) {
            for (k in 0 until 4) rotate(k * 90f, c) {
                drawLine(Color(0xFF6D4C41), c, c - Offset(0f, 34 * u), 2 * u)
                drawRect(Color(0xFFFFF8E1), Offset(c.x - 1 * u, c.y - 34 * u), Size(9 * u, 24 * u))
                drawRect(Color(0xFFBCAAA4), Offset(c.x - 1 * u, c.y - 34 * u), Size(9 * u, 24 * u), style = Stroke(1f * u))
            }
        }
        drawCircle(Color(0xFF5D4037), 3.5f * u, c)
    }
}
