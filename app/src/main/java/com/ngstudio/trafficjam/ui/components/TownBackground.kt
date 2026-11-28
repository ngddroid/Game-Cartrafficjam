package com.ngstudio.trafficjam.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import com.ngstudio.trafficjam.game.render.shade
import com.ngstudio.trafficjam.game.render.tint
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/**
 * Toy-town backdrop for the menus: a sky in the tab's colors with a glowing sun and drifting
 * clouds, a misty city skyline with lit windows, rolling hills dotted with little houses and
 * round trees, and a road along the bottom with toy cars driving by.
 *
 * The sky and skyline take the tab's colors; the hills, houses and road stay the same in every
 * tab so it feels like one cartoon world seen at different times of day.
 */
@Composable
fun TownBackground(
    top: Color,
    mid: Color,
    bottom: Color,
    modifier: Modifier = Modifier,
    /** 1 shows the whole town; 0 leaves just the sky gradient (the Modes tab). */
    sceneryAlpha: Float = 1f,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = rememberInfiniteTransition(label = "town")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(48_000, easing = LinearEasing)), label = "drift")
    val traffic by t.animateFloat(0f, 1f, infiniteRepeatable(tween(14_000, easing = LinearEasing)), label = "traffic")
    Box(modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) { drawRect(Brush.verticalGradient(listOf(top, mid, bottom))) }
        if (sceneryAlpha > 0.01f) {
            Canvas(Modifier.fillMaxSize().graphicsLayer { alpha = sceneryAlpha }) { drawTown(top, mid, bottom, drift, traffic) }
        }
        content()
    }
}

private val roofColors = listOf(Color(0xFFF2453D), Color(0xFFFF8A1F), Color(0xFF8E5CF0), Color(0xFF2FB8C9), Color(0xFFEC407A), Color(0xFF3DBE4B))
private val wallColors = listOf(Color(0xFFFFF3DC), Color(0xFFFFE3C2), Color(0xFFFDF0F5), Color(0xFFE9F7EC), Color(0xFFFFF8E1))
private val carColors = listOf(Color(0xFFF2453D), Color(0xFFFFC21A), Color(0xFF3DBE4B), Color(0xFF8E5CF0), Color(0xFFFF6FB5))

/** Fixed layout of the skyline and the hill town, so nothing jumps between frames. */
private class TownLayout {
    val towers: List<FloatArray> // x, width, height (as fractions), window style
    val houses: List<FloatArray> // x, size, color index
    val trees: List<FloatArray> // x, size, hill (0 back, 1 front)
    init {
        val r = Random(42)
        var x = -0.04f
        val tw = ArrayList<FloatArray>()
        while (x < 1.04f) {
            val w = 0.07f + r.nextFloat() * 0.07f
            tw += floatArrayOf(x, w, 0.07f + r.nextFloat() * 0.13f, r.nextInt(3).toFloat())
            x += w * (0.75f + r.nextFloat() * 0.2f)
        }
        towers = tw
        houses = listOf(
            floatArrayOf(0.08f, 1.0f, 0f), floatArrayOf(0.26f, 0.8f, 1f), floatArrayOf(0.47f, 1.1f, 2f),
            floatArrayOf(0.66f, 0.85f, 3f), floatArrayOf(0.86f, 1.0f, 4f),
        )
        trees = listOf(
            floatArrayOf(0.0f, 1.0f, 0f), floatArrayOf(0.17f, 0.8f, 1f), floatArrayOf(0.37f, 1.1f, 0f),
            floatArrayOf(0.56f, 0.9f, 1f), floatArrayOf(0.76f, 1.05f, 0f), floatArrayOf(0.96f, 0.85f, 1f),
        )
    }
}

private val layout = TownLayout()

private fun DrawScope.drawTown(top: Color, mid: Color, bottom: Color, drift: Float, traffic: Float) {
    val w = size.width
    val h = size.height
    val u = w / 360f
    val horizon = h * 0.6f

    // Sky: the tab's gradient, fading lighter towards the horizon.
    drawRect(Brush.verticalGradient(listOf(top, mid, bottom.tint(0.25f)), 0f, horizon + h * 0.1f))

    // Sun with a soft glow and slowly turning rays.
    val sun = Offset(w * 0.8f, h * 0.17f)
    drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), sun, 120 * u), 120 * u, sun)
    rotate(drift * 360f, sun) {
        for (i in 0 until 12) {
            rotate(i * 30f, sun) {
                val p = Path().apply {
                    moveTo(sun.x, sun.y)
                    lineTo(sun.x + 150 * u, sun.y - 14 * u)
                    lineTo(sun.x + 150 * u, sun.y + 14 * u)
                    close()
                }
                drawPath(p, Brush.radialGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), sun, 150 * u))
            }
        }
    }
    drawCircle(Color(0xFFFFF3B0), 30 * u, sun)
    drawCircle(Color(0xFFFFE066), 24 * u, sun)
    drawCircle(Color.White.copy(alpha = 0.6f), 8 * u, sun - Offset(8 * u, 8 * u))

    // Drifting puffy clouds.
    fun cloud(cx: Float, cy: Float, s: Float, a: Float) {
        val c = Color.White.copy(alpha = a)
        drawRoundRect(c.copy(alpha = a * 0.5f), Offset(cx - 52 * s, cy + 10 * s), Size(104 * s, 22 * s), CornerRadius(11 * s))
        drawCircle(c, 26 * s, Offset(cx - 22 * s, cy + 6 * s))
        drawCircle(c, 34 * s, Offset(cx + 6 * s, cy - 4 * s))
        drawCircle(c, 22 * s, Offset(cx + 34 * s, cy + 8 * s))
        drawRoundRect(c, Offset(cx - 50 * s, cy + 6 * s), Size(100 * s, 24 * s), CornerRadius(12 * s))
    }
    val span = w + 240 * u
    cloud((drift * span + 0.15f * span) % span - 120 * u, h * 0.11f, 1.1f * u, 0.85f)
    cloud((drift * span * 0.7f + 0.65f * span) % span - 120 * u, h * 0.27f, 0.8f * u, 0.7f)
    cloud((drift * span * 1.25f + 0.4f * span) % span - 120 * u, h * 0.42f, 0.95f * u, 0.55f)

    // Misty city skyline in a deeper shade of the tab color.
    val towerColor = bottom.shade(0.78f).copy(alpha = 0.55f)
    val windowColor = Color(0xFFFFF1A8).copy(alpha = 0.75f)
    for (t in layout.towers) {
        val x = t[0] * w
        val tw = t[1] * w
        val th = t[2] * h
        val ty = horizon - th
        drawRoundRect(towerColor, Offset(x, ty), Size(tw, th + 40 * u), CornerRadius(3 * u))
        val cols = (tw / (9 * u)).toInt().coerceAtLeast(1)
        val rows = (th / (12 * u)).toInt()
        for (rr in 0 until rows) for (cc in 0 until cols) {
            if ((rr * 7 + cc * 3 + t[3].toInt()) % 3 == 0) continue
            drawRect(windowColor, Offset(x + 4 * u + cc * 9 * u, ty + 6 * u + rr * 12 * u), Size(4 * u, 5 * u))
        }
    }

    // Two rolling hills.
    fun hill(baseY: Float, amp: Float, phase: Float, color: Color, edge: Color) {
        val p = Path().apply {
            moveTo(0f, h)
            lineTo(0f, baseY)
            var x = 0f
            while (x <= w + 4f) {
                lineTo(x, baseY - amp * (0.5f + 0.5f * sin((x / w) * 2.2f * PI.toFloat() + phase)))
                x += 6f
            }
            lineTo(w, h)
            close()
        }
        drawPath(p, Brush.verticalGradient(listOf(color, color.shade(0.86f)), baseY - amp, h))
        // Light rim along the crest.
        var x = 0f
        while (x <= w) {
            val y = baseY - amp * (0.5f + 0.5f * sin((x / w) * 2.2f * PI.toFloat() + phase))
            drawCircle(edge, 2.2f * u, Offset(x, y + 1.5f * u))
            x += 5 * u
        }
    }
    val backHill = horizon + 18 * u
    val frontHill = horizon + 58 * u
    hill(backHill, 26 * u, 0.6f, Color(0xFF8FD46B), Color(0xFFB7EA8F))
    for (tr in layout.trees) if (tr[2] == 0f) tree(tr[0] * w, backHill - 14 * u, tr[1] * u)
    for (hs in layout.houses) house(hs[0] * w, backHill - 4 * u, hs[1] * u, hs[2].toInt())
    hill(frontHill, 22 * u, 2.4f, Color(0xFF5FC15A), Color(0xFF8FDC6E))
    for (tr in layout.trees) if (tr[2] == 1f) tree(tr[0] * w, frontHill - 6 * u, tr[1] * 1.15f * u)

    // Plain grassland below the hills — the Home screen overlays its own road strip, so there is
    // no second road here.
    val grassTop = frontHill + 35 * u
    drawRect(Brush.verticalGradient(listOf(Color(0xFF5FC15A), Color(0xFF43984A)), grassTop, h), Offset(0f, grassTop), Size(w, h - grassTop))
}

private fun DrawScope.tree(x: Float, groundY: Float, s: Float) {
    drawRoundRect(Color(0xFF8D5A3B), Offset(x - 3 * s, groundY - 16 * s), Size(6 * s, 18 * s), CornerRadius(2 * s))
    drawCircle(Color(0xFF2E8B45), 15 * s, Offset(x, groundY - 24 * s))
    drawCircle(Color(0xFF3DAA55), 13 * s, Offset(x - 2 * s, groundY - 26 * s))
    drawCircle(Color(0xFF6FD07A).copy(alpha = 0.8f), 5 * s, Offset(x - 6 * s, groundY - 31 * s))
}

private fun DrawScope.house(x: Float, groundY: Float, s: Float, i: Int) {
    val wall = wallColors[i % wallColors.size]
    val roof = roofColors[i % roofColors.size]
    val bw = 30 * s
    val bh = 22 * s
    val left = x - bw / 2
    val top = groundY - bh
    drawRoundRect(wall.shade(0.8f), Offset(left + 2 * s, top + 2 * s), Size(bw, bh), CornerRadius(3 * s))
    drawRoundRect(wall, Offset(left, top), Size(bw, bh), CornerRadius(3 * s))
    val r = Path().apply {
        moveTo(left - 5 * s, top + 2 * s)
        lineTo(x, top - 16 * s)
        lineTo(left + bw + 5 * s, top + 2 * s)
        close()
    }
    drawPath(r, roof.shade(0.75f))
    val r2 = Path().apply {
        moveTo(left - 4 * s, top)
        lineTo(x, top - 17 * s)
        lineTo(left + bw + 4 * s, top)
        close()
    }
    drawPath(r2, roof)
    drawLine(Color.White.copy(alpha = 0.45f), Offset(x - 2 * s, top - 13 * s), Offset(left + 2 * s, top - 2 * s), 2 * s, StrokeCap.Round)
    drawRoundRect(Color(0xFF8D5A3B), Offset(x - 4 * s, groundY - 12 * s), Size(8 * s, 12 * s), CornerRadius(2 * s))
    drawRoundRect(Color(0xFF7FD3F0), Offset(left + 4 * s, top + 5 * s), Size(7 * s, 6 * s), CornerRadius(1.5f * s))
    drawRoundRect(Color(0xFF7FD3F0), Offset(left + bw - 11 * s, top + 5 * s), Size(7 * s, 6 * s), CornerRadius(1.5f * s))
}

private fun DrawScope.toyCar(c: Offset, color: Color, dir: Int, u: Float) {
    val w = 30 * u
    val h = 13 * u
    val left = c.x - w / 2
    drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(left + 2 * u, c.y - h / 2 + 4 * u), Size(w, h), CornerRadius(5 * u))
    drawRoundRect(color.shade(0.6f), Offset(left, c.y - h / 2 + 2 * u), Size(w, h), CornerRadius(5 * u))
    drawRoundRect(color, Offset(left, c.y - h / 2), Size(w, h - 1 * u), CornerRadius(5 * u))
    val cabin = if (dir > 0) left + w * 0.2f else left + w * 0.38f
    drawRoundRect(Color(0xFFCFEFFF), Offset(cabin, c.y - h / 2 + 2.5f * u), Size(w * 0.42f, h * 0.45f), CornerRadius(2.5f * u))
    drawRoundRect(Color.White.copy(alpha = 0.45f), Offset(left + 4 * u, c.y - h / 2 + 1.5f * u), Size(w - 8 * u, 2 * u), CornerRadius(1 * u))
    drawCircle(Color(0xFF2B2B33), 3.2f * u, Offset(left + 7 * u, c.y + h / 2))
    drawCircle(Color(0xFF2B2B33), 3.2f * u, Offset(left + w - 7 * u, c.y + h / 2))
    val lamp = if (dir > 0) left + w - 2 * u else left + 2 * u
    drawCircle(Color(0xFFFFF59D), 1.8f * u, Offset(lamp, c.y))
}
