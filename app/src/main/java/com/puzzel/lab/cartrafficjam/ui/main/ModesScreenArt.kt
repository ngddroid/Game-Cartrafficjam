package com.puzzel.lab.cartrafficjam.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.cartrafficjam.ui.components.Icon
import com.puzzel.lab.cartrafficjam.ui.components.OutlinedText
import com.puzzel.lab.cartrafficjam.ui.components.drawGameIcon
import com.puzzel.lab.cartrafficjam.ui.theme.GameText
import com.puzzel.lab.cartrafficjam.ui.theme.TitleFont

// ---- Header ------------------------------------------------------------------------------

private val HeaderInk = Color(0xFF3B1E8C)

/**
 * The Game Modes header: a little roadside scene (signpost with a gamepad, skyline, bushes, a
 * curved road with a school bus and a traffic light) behind a big 3D "Game Modes" title and a
 * purple subtitle ribbon.
 */
@Composable
fun ModesHeader() {
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(160.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(Modifier.fillMaxSize()) { drawHeaderScene() }
        Column(Modifier.padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                OutlinedText("Game ", style = GameText.Display, fontSize = 40.sp, strokeWidth = 11f, outline = HeaderInk, color = Color.White)
                OutlinedText("Modes", style = GameText.Display, fontSize = 40.sp, strokeWidth = 11f, outline = HeaderInk, color = Color(0xFFFFC93C))
            }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .drawBehind {
                        val r = CornerRadius(size.height / 2)
                        drawRoundRect(HeaderInk, Offset(0f, 3.dp.toPx()), size, r)
                        drawRoundRect(HeaderInk, Offset.Zero, size, r)
                        drawRoundRect(
                            Brush.verticalGradient(listOf(Color(0xFF9B6BFF), Color(0xFF6C3FD6))),
                            Offset(2.dp.toPx(), 2.dp.toPx()), Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), CornerRadius(size.height / 2 - 2.dp.toPx()),
                        )
                        drawRoundRect(
                            Color.White.copy(alpha = 0.25f), Offset(size.height * 0.4f, 4.dp.toPx()),
                            Size(size.width - size.height * 0.8f, 5.dp.toPx()), CornerRadius(3.dp.toPx()),
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 5.dp),
            ) {
                Text("Four ways to beat the traffic", style = GameText.Label.copy(color = Color.White, fontFamily = TitleFont, fontSize = 16.sp))
            }
        }
    }
}

private fun DrawScope.drawHeaderScene() {
    val w = size.width
    val h = size.height
    val u = w / 360f

    // Soft clouds in the corners.
    fun cloud(cx: Float, cy: Float, s: Float) {
        val c = Color.White.copy(alpha = 0.75f)
        drawCircle(c, 13 * s, Offset(cx - 12 * s, cy + 3 * s))
        drawCircle(c, 17 * s, Offset(cx + 4 * s, cy - 2 * s))
        drawCircle(c, 11 * s, Offset(cx + 20 * s, cy + 4 * s))
        drawRoundRect(c, Offset(cx - 25 * s, cy + 3 * s), Size(55 * s, 12 * s), CornerRadius(6 * s))
    }
    cloud(w * 0.11f, h * 0.1f, 1.1f * u)
    cloud(w * 0.88f, h * 0.06f, 0.9f * u)

    // Gold sparkle lines above the title.
    drawLine(Color(0xFFFFD43B), Offset(w * 0.27f, h * 0.06f), Offset(w * 0.3f, h * 0.13f), 4 * u, StrokeCap.Round)
    drawLine(Color(0xFFFFD43B), Offset(w * 0.33f, h * 0.02f), Offset(w * 0.34f, h * 0.1f), 4 * u, StrokeCap.Round)

    // Lilac skyline.
    val towers = floatArrayOf(0.12f, 0.2f, 0.27f, 0.36f, 0.62f, 0.7f, 0.78f, 0.86f)
    val heights = floatArrayOf(0.14f, 0.22f, 0.12f, 0.18f, 0.16f, 0.24f, 0.13f, 0.19f)
    for (i in towers.indices) {
        val x = towers[i] * w
        val th = heights[i] * h
        val top = h * 0.8f - th
        drawRoundRect(Color(0xFFA48BF7).copy(alpha = 0.5f), Offset(x, top), Size(26 * u, th + 6 * u), CornerRadius(2 * u))
        for (r in 0 until (th / (9 * u)).toInt()) for (c in 0 until 3) {
            if ((r + c + i) % 2 == 0) drawRect(Color.White.copy(alpha = 0.35f), Offset(x + 4 * u + c * 7 * u, top + 4 * u + r * 9 * u), Size(4 * u, 4 * u))
        }
    }

    // Green bushes on both sides.
    fun bush(cx: Float, cy: Float, s: Float) {
        drawCircle(Color(0xFF2F8F3E), 16 * s, Offset(cx - 14 * s, cy + 4 * s))
        drawCircle(Color(0xFF3FAE4E), 20 * s, Offset(cx, cy - 4 * s))
        drawCircle(Color(0xFF3FAE4E), 15 * s, Offset(cx + 18 * s, cy + 2 * s))
        drawCircle(Color(0xFF6CCB5F).copy(alpha = 0.8f), 7 * s, Offset(cx - 5 * s, cy - 12 * s))
    }
    bush(w * 0.06f, h * 0.84f, 1.2f * u)
    bush(w * 0.2f, h * 0.86f, 0.9f * u)
    bush(w * 0.95f, h * 0.84f, 1.1f * u)

    // Curved road with lane dashes.
    val top = Path().apply {
        moveTo(-10f, h * 0.9f)
        quadraticTo(w * 0.5f, h * 0.68f, w + 10f, h * 0.9f)
        lineTo(w + 10f, h * 1.2f)
        quadraticTo(w * 0.5f, h * 0.98f, -10f, h * 1.2f)
        close()
    }
    drawPath(top, Brush.verticalGradient(listOf(Color(0xFF5C6178), Color(0xFF3F4357)), h * 0.75f, h))
    fun q(t: Float, y0: Float, yc: Float): Offset {
        val x = (1 - t) * (1 - t) * -10f + 2 * (1 - t) * t * (w * 0.5f) + t * t * (w + 10f)
        val y = (1 - t) * (1 - t) * y0 + 2 * (1 - t) * t * yc + t * t * y0
        return Offset(x, y)
    }
    var t = 0.02f
    while (t < 0.98f) {
        drawLine(Color.White.copy(alpha = 0.9f), q(t, h * 1.05f, h * 0.83f), q(t + 0.035f, h * 1.05f, h * 0.83f), 3 * u, StrokeCap.Round)
        t += 0.07f
    }
    // Light curb along the top edge.
    var k = 0f
    while (k <= 1f) {
        drawCircle(Color(0xFFB8BDD0), 1.8f * u, q(k, h * 0.9f, h * 0.68f))
        k += 0.012f
    }

    // Wooden signpost with a gamepad on the left.
    drawRoundRect(Color(0xFF5D3519), Offset(w * 0.1f - 4 * u, h * 0.38f), Size(9 * u, h * 0.52f), CornerRadius(2 * u))
    drawRoundRect(Color(0xFF8A5A30), Offset(w * 0.1f - 2.5f * u, h * 0.38f), Size(4 * u, h * 0.52f), CornerRadius(2 * u))
    val sign = Path().apply {
        moveTo(w * 0.02f, h * 0.24f)
        lineTo(w * 0.21f, h * 0.2f)
        lineTo(w * 0.26f, h * 0.33f)
        lineTo(w * 0.22f, h * 0.46f)
        lineTo(w * 0.03f, h * 0.5f)
        close()
    }
    drawPath(sign, Color(0xFF5D3519), style = Stroke(5 * u, join = StrokeJoin.Round))
    drawPath(sign, Brush.verticalGradient(listOf(Color(0xFFD99A57), Color(0xFFA8672F)), h * 0.2f, h * 0.5f))
    drawLine(Color(0xFF8A5A30), Offset(w * 0.04f, h * 0.36f), Offset(w * 0.2f, h * 0.33f), 1.5f * u)
    drawGameIcon(Icon.GAMEPAD, Offset(w * 0.07f, h * 0.22f), 46 * u)

    // Traffic light on the right.
    drawRoundRect(Color(0xFF6E7488), Offset(w * 0.93f - 3 * u, h * 0.3f), Size(7 * u, h * 0.58f), CornerRadius(2 * u))
    drawRoundRect(Color(0xFF2B2F42), Offset(w * 0.9f, h * 0.08f), Size(22 * u, 54 * u), CornerRadius(6 * u))
    drawRoundRect(Color(0xFF3B4058), Offset(w * 0.9f + 2 * u, h * 0.08f + 2 * u), Size(18 * u, 50 * u), CornerRadius(5 * u))
    for ((i, c) in listOf(Color(0xFFFF4D4D), Color(0xFFFFC21A), Color(0xFF3DDC5A)).withIndex()) {
        val cc = Offset(w * 0.9f + 11 * u, h * 0.08f + 10 * u + i * 16 * u)
        drawCircle(c.copy(alpha = 0.35f), 8 * u, cc)
        drawCircle(c, 5.5f * u, cc)
        drawCircle(Color.White.copy(alpha = 0.6f), 1.8f * u, cc - Offset(2 * u, 2 * u))
    }

    // Yellow school bus on the road.
    val bx = w * 0.72f
    val by = h * 0.62f
    val bw = 64 * u
    val bh = 34 * u
    drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(bx + 3 * u, by + bh - 2 * u), Size(bw, 8 * u), CornerRadius(4 * u))
    drawRoundRect(Color(0xFF8A5200), Offset(bx - 2 * u, by - 2 * u), Size(bw + 4 * u, bh + 4 * u), CornerRadius(9 * u))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFDD55), Color(0xFFFFB300)), by, by + bh), Offset(bx, by), Size(bw, bh), CornerRadius(8 * u))
    for (i in 0 until 4) drawRoundRect(Color(0xFF2E3A6E), Offset(bx + 6 * u + i * 12 * u, by + 6 * u), Size(9 * u, 10 * u), CornerRadius(2 * u))
    drawRoundRect(Color(0xFF7FD3F0), Offset(bx + bw - 13 * u, by + 6 * u), Size(9 * u, 12 * u), CornerRadius(2 * u))
    drawRect(Color(0xFF8A5200), Offset(bx, by + bh * 0.62f), Size(bw, 2.5f * u))
    drawCircle(Color(0xFFFFF59D), 2.5f * u, Offset(bx + bw - 2 * u, by + bh * 0.78f))
    for (x in listOf(bx + 14 * u, bx + bw - 14 * u)) {
        drawCircle(Color(0xFF2B2B33), 7 * u, Offset(x, by + bh))
        drawCircle(Color(0xFFB0BEC5), 2.8f * u, Offset(x, by + bh))
    }
}
