package com.ngstudio.trafficjam.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.trafficjam.game.model.JamColor
import com.ngstudio.trafficjam.game.render.shade
import com.ngstudio.trafficjam.game.render.tint
import com.ngstudio.trafficjam.ui.components.GameIcon
import com.ngstudio.trafficjam.ui.components.Icon
import com.ngstudio.trafficjam.ui.components.OutlinedText
import com.ngstudio.trafficjam.ui.theme.GameText
import com.ngstudio.trafficjam.ui.theme.TitleFont
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import com.ngstudio.trafficjam.ui.components.drawLeafSprig

internal val GaragePurpleInk = Color(0xFF2E1A78)

// ---- Header -------------------------------------------------------------------------------

/**
 * Garage header: a sky with a skyline and bushes, an orange wooden plank between two checkered
 * racing flags, and a cream pill with how many paint packs the player owns.
 */
@Composable
fun GarageHeader(owned: Int, total: Int) {
    Box(Modifier.fillMaxWidth().drawBehind { drawGarageSky() }) {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Checkered flags crossing out from behind the plank, and leaves at its ends.
                Canvas(Modifier.size(330.dp, 86.dp)) {
                    val u = 1.dp.toPx()
                    val cx = size.width / 2
                    // Poles end behind the middle of the plank, so nothing pokes out below it;
                    // the cloth stays fully on screen.
                    flag(Offset(cx - 86 * u, size.height * 0.6f), Offset(cx - 114 * u, 8 * u), -1f)
                    flag(Offset(cx + 86 * u, size.height * 0.6f), Offset(cx + 114 * u, 8 * u), 1f)
                    drawLeafSprig(Offset(cx - 96 * u, size.height * 0.66f), -72f, 1.05f)
                    drawLeafSprig(Offset(cx + 96 * u, size.height * 0.66f), 72f, 1.05f)
                }
                Box(
                    Modifier
                        .width(212.dp)
                        .drawBehind { drawPlank(Color(0xFFFFB45A), Color(0xFFF58A2E), Color(0xFFE06A16), Color(0xFFA9470C), leaves = false) }
                        .padding(horizontal = 36.dp, vertical = 7.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    OutlinedText("Garage", style = GameText.Display, fontSize = 36.sp, strokeWidth = 10f, outline = WoodInk)
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .drawBehind {
                        // Leaf sprigs tucked behind both ends of the pill.
                        drawLeafSprig(Offset(6.dp.toPx(), size.height * 0.7f), -74f, 0.85f)
                        drawLeafSprig(Offset(size.width - 6.dp.toPx(), size.height * 0.7f), 74f, 0.85f)
                        val r = CornerRadius(size.height / 2)
                        drawRoundRect(GaragePurpleInk.copy(alpha = 0.35f), Offset(0f, 3.dp.toPx()), size, r)
                        drawRoundRect(Color(0xFF6A3FD9), Offset.Zero, size, r)
                        drawRoundRect(
                            Brush.verticalGradient(listOf(Color(0xFFFFFCF2), Color(0xFFFFEBC8))),
                            Offset(2.5.dp.toPx(), 2.5.dp.toPx()), Size(size.width - 5.dp.toPx(), size.height - 5.dp.toPx()),
                            CornerRadius(size.height / 2 - 2.5.dp.toPx()),
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 4.dp),
            ) {
                Text("$owned of $total paint packs owned", style = GameText.Label.copy(color = Color(0xFF3B1E6E), fontFamily = TitleFont, fontSize = 15.sp))
            }
            Spacer(Modifier.height(10.dp))
            // Lavender roof ledge where the sky meets the garage.
            Canvas(Modifier.fillMaxWidth().height(18.dp)) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFFB4BCF7), Color(0xFF7A80DD), Color(0xFF5A5FC4))), Offset(0f, 3.dp.toPx()), Size(size.width, size.height - 6.dp.toPx()))
                drawRect(Color.White.copy(alpha = 0.7f), Offset(0f, 3.dp.toPx()), Size(size.width, 2.dp.toPx()))
                drawRect(Color(0xFF8E96E8), Offset(0f, 0f), Size(size.width, 3.dp.toPx()))
                drawRect(Brush.verticalGradient(listOf(Color(0xFF2C2670), Color(0xFF3A3389)), size.height - 3.dp.toPx(), size.height), Offset(0f, size.height - 3.dp.toPx()), Size(size.width, 3.dp.toPx()))
            }
        }
    }
}

private fun DrawScope.drawGarageSky() {
    val w = size.width
    val h = size.height
    val u = w / 360f
    drawRect(Brush.verticalGradient(listOf(Color(0xFF3FB6F5), Color(0xFF9BDDFC))))
    // Pale skyline and green bushes along the bottom.
    for (i in 0 until 12) {
        val x = i * w / 11f - 10 * u
        val th = (26 + (i * 37 % 5) * 10) * u
        val top = h - th - 14 * u
        drawRoundRect(Color(0xFF78BFEA), Offset(x, top), Size(26 * u, th + 14 * u), CornerRadius(2 * u))
        for (r in 0 until (th / (9 * u)).toInt()) for (c in 0 until 3) {
            if ((r + c + i) % 3 != 0) drawRect(Color(0xFFB4E2FA), Offset(x + 3.5f * u + c * 7.5f * u, top + 4 * u + r * 9 * u), Size(4 * u, 4.5f * u))
        }
    }
    // Rounded bushes along the bottom with a glossy top.
    for (i in 0 until 9) {
        val cx = i * w / 8f
        drawCircle(Color(0xFF2E8B3A), 17 * u, Offset(cx, h - 2 * u))
        drawCircle(Color(0xFF45B04A), 13 * u, Offset(cx - 9 * u, h - 6 * u))
        drawCircle(Color(0xFF45B04A), 12 * u, Offset(cx + 9 * u, h - 5 * u))
        drawCircle(Color(0xFF6CD15F), 6 * u, Offset(cx - 11 * u, h - 13 * u))
    }
    fun cloud(cx: Float, cy: Float, s: Float) {
        drawCircle(Color.White, 12 * s, Offset(cx - 12 * s, cy + 3 * s))
        drawCircle(Color.White, 16 * s, Offset(cx + 3 * s, cy - 2 * s))
        drawRoundRect(Color.White, Offset(cx - 24 * s, cy + 2 * s), Size(50 * s, 11 * s), CornerRadius(5 * s))
    }
    cloud(w * 0.07f, h * 0.55f, 0.9f * u)
    cloud(w * 0.93f, h * 0.5f, 0.9f * u)
}

/** A checkered racing flag: a pole from [base] to [top], with waving cloth flying outwards. */
private fun DrawScope.flag(base: Offset, top: Offset, side: Float) {
    val u = 1.dp.toPx()
    drawLine(Color(0xFF3B2412), base, top, 6 * u, StrokeCap.Round)
    drawLine(Color(0xFF8D5A3B), base, top, 3.5f * u, StrokeCap.Round)
    val fw = 42 * u
    val fh = 30 * u
    val cols = 6
    val rows = 4
    fun wave(fx: Float) = kotlin.math.sin(fx * 3.6f) * 4 * u + fx * 5 * u
    fun pt(fx: Float, fy: Float) = Offset(top.x + side * fx * fw, top.y + 3 * u + fy * fh + wave(fx))
    for (r in 0 until rows) for (c in 0 until cols) {
        val x0 = c / cols.toFloat()
        val x1 = (c + 1) / cols.toFloat()
        val y0 = r / rows.toFloat()
        val y1 = (r + 1) / rows.toFloat()
        val cell = Path().apply {
            val a = pt(x0, y0); val b = pt(x1, y0); val cc = pt(x1, y1); val d = pt(x0, y1)
            moveTo(a.x, a.y); lineTo(b.x, b.y); lineTo(cc.x, cc.y); lineTo(d.x, d.y); close()
        }
        drawPath(cell, if ((r + c) % 2 == 0) Color(0xFF1E1E26) else Color.White)
    }
    // Outline around the whole cloth.
    val edge = Path().apply {
        var fx = 0f
        val a = pt(0f, 0f); moveTo(a.x, a.y)
        while (fx <= 1f) { val p = pt(fx, 0f); lineTo(p.x, p.y); fx += 0.1f }
        fx = 1f
        while (fx >= 0f) { val p = pt(fx, 1f); lineTo(p.x, p.y); fx -= 0.1f }
        close()
    }
    drawPath(edge, Color(0xFF1E1E26), style = Stroke(2 * u, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    drawCircle(Color(0xFF9A5B00), 5 * u, top)
    drawCircle(Color(0xFFFFC21A), 3.8f * u, top)
}

// ---- Showroom backdrop --------------------------------------------------------------------

/**
 * The garage interior behind the turntable: walls, a rolling shutter door, two wall lamps with
 * light cones, a red tool chest and a stack of tyres on the left, a potted plant on the right,
 * hazard stripes and a tiled floor.
 */
fun DrawScope.drawGarageInterior(glow: Float) {
    val w = size.width
    val h = size.height
    val u = w / 360f
    val floorY = h * 0.6f
    // Walls and the shutter door.
    drawRect(Brush.verticalGradient(listOf(Color(0xFF3A3389), Color(0xFF2C2670)), 0f, floorY), size = Size(w, floorY))
    for (x in listOf(w * 0.06f, w * 0.9f)) drawRect(Color(0xFF332D7D), Offset(x, 0f), Size(w * 0.04f, floorY))
    val door = Offset(w * 0.22f, h * 0.12f)
    val dw = w * 0.56f
    val dh = floorY - door.y
    drawRect(Color(0xFF232058), door - Offset(5 * u, 5 * u), Size(dw + 10 * u, dh + 5 * u))
    var y = door.y
    var k = 0
    while (y < floorY) {
        drawRect(if (k % 2 == 0) Color(0xFF4A4A9E) else Color(0xFF41418F), Offset(door.x, y), Size(dw, 8 * u))
        drawRect(Color(0xFF2E2E70), Offset(door.x, y + 7 * u), Size(dw, 1 * u))
        y += 8 * u; k++
    }
    // Wall lamps with light cones.
    for (side in listOf(-1f, 1f)) {
        val lx = if (side < 0) w * 0.1f else w * 0.9f
        val ly = h * 0.22f
        val cone = Path().apply {
            moveTo(lx, ly)
            lineTo(lx + side * -60 * u + side * 10 * u, floorY + 30 * u)
            lineTo(lx + side * -140 * u, floorY + 10 * u)
            close()
        }
        drawPath(cone, Brush.radialGradient(listOf(Color(0xFFFFF3C4).copy(alpha = 0.35f * glow), Color.Transparent), Offset(lx, ly), 170 * u))
        drawLine(Color(0xFF6B6F86), Offset(lx, ly - 22 * u), Offset(lx + side * 4 * u, ly), 3 * u, StrokeCap.Round)
        rotate(side * 30f, Offset(lx, ly)) {
            drawRoundRect(Color(0xFF8A90A8), Offset(lx - 12 * u, ly - 6 * u), Size(24 * u, 14 * u), CornerRadius(6 * u))
            drawOval(Color(0xFFFFF59D), Offset(lx - 9 * u, ly + 4 * u), Size(18 * u, 6 * u))
        }
    }
    // Floor tiles.
    drawRect(Brush.verticalGradient(listOf(Color(0xFF5A519C), Color(0xFF3B3384)), floorY, h), Offset(0f, floorY), Size(w, h - floorY))
    for (i in 0..8) {
        val x = i * w / 8f
        drawLine(Color.White.copy(alpha = 0.06f), Offset(w / 2 + (x - w / 2) * 0.55f, floorY), Offset(x, h), 1.5f * u)
    }
    for (i in 1..3) {
        val fy = floorY + (h - floorY) * (i / 4f) * (i / 4f) * 1.1f
        drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, fy), Offset(w, fy), 1.5f * u)
    }
    // Hazard stripes at the floor's edges.
    for (side in listOf(-1f, 1f)) {
        val sx = if (side < 0) 0f else w - 46 * u
        val sy = h * 0.8f
        clipRect(sx, sy, sx + 46 * u, sy + 12 * u) {
            drawRect(Color(0xFFFFC21A), Offset(sx, sy), Size(46 * u, 12 * u))
            var t = sx - 12 * u
            while (t < sx + 46 * u) {
                drawPath(Path().apply { moveTo(t, sy + 12 * u); lineTo(t + 8 * u, sy); lineTo(t + 14 * u, sy); lineTo(t + 6 * u, sy + 12 * u); close() }, Color(0xFF22222A))
                t += 14 * u
            }
        }
    }
    // Red tool chest and tyres on the left.
    val cx = w * 0.03f
    val cy = h * 0.42f
    drawRoundRect(Color(0xFF8E1B1B), Offset(cx, cy + 3 * u), Size(42 * u, 50 * u), CornerRadius(4 * u))
    drawRoundRect(Color(0xFFE53935), Offset(cx, cy), Size(42 * u, 48 * u), CornerRadius(4 * u))
    for (i in 0 until 3) {
        drawRect(Color(0xFFB71C1C), Offset(cx + 3 * u, cy + 6 * u + i * 14 * u), Size(36 * u, 11 * u))
        drawRoundRect(Color(0xFFDCDCE4), Offset(cx + 14 * u, cy + 10 * u + i * 14 * u), Size(14 * u, 3 * u), CornerRadius(1.5f * u))
    }
    for (i in 0 until 2) {
        val ty = h * 0.7f - i * 12 * u
        drawOval(Color(0xFF1E1E26), Offset(-6 * u, ty), Size(42 * u, 16 * u))
        drawOval(Color(0xFF3A3A46), Offset(4 * u, ty + 4 * u), Size(22 * u, 8 * u))
    }
    // Potted plant on the right.
    val px = w * 0.9f
    val py = h * 0.62f
    drawRoundRect(Color(0xFFE6E6EE), Offset(px - 13 * u, py), Size(26 * u, 22 * u), CornerRadius(3 * u))
    for ((a, l) in listOf(-40f to 22f, -10f to 28f, 20f to 24f, 50f to 18f)) {
        rotate(a, Offset(px, py)) {
            drawOval(Color(0xFF3FAE4E), Offset(px - 4 * u, py - l * u), Size(8 * u, l * u))
        }
    }
}

/** Neon-rimmed turntable with yellow lights: drawn under the vehicle. */
fun DrawScope.drawTurntable(c: Offset, rx: Float, ry: Float, angle: Float, glow: Float) {
    val u = 1.dp.toPx()
    drawOval(Color.Black.copy(alpha = 0.4f), Offset(c.x - rx, c.y - ry + 14 * u), Size(rx * 2, ry * 2))
    drawOval(Color(0xFF242650), Offset(c.x - rx, c.y - ry + 9 * u), Size(rx * 2, ry * 2))
    drawOval(Brush.verticalGradient(listOf(Color(0xFF4A4F80), Color(0xFF2F3361)), c.y - ry, c.y + ry), Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2))
    drawOval(Color(0xFF33D6FF).copy(alpha = 0.25f * glow), Offset(c.x - rx - 6 * u, c.y - ry - 4 * u), Size(rx * 2 + 12 * u, ry * 2 + 8 * u), style = Stroke(10 * u))
    drawOval(Color(0xFF5CE1FF), Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2), style = Stroke(3.5f * u))
    drawOval(Color.White.copy(alpha = 0.08f), Offset(c.x - rx * 0.6f, c.y - ry * 0.6f), Size(rx * 1.2f, ry * 1.2f), style = Stroke(1.5f * u))
    for (k in 0 until 12) {
        val a = Math.toRadians(k * 30.0 + angle)
        val p = Offset(c.x + (rx * 0.86f * kotlin.math.cos(a)).toFloat(), c.y + (ry * 0.86f * kotlin.math.sin(a)).toFloat())
        drawCircle(Color(0xFFFFE066).copy(alpha = 0.35f), 6 * u, p)
        drawCircle(Color(0xFFFFE066), 2.6f * u, p)
    }
}

// ---- Title with laurels -------------------------------------------------------------------

/** Paint pack name between two gold laurels, with its tagline on a purple pill below. */
@Composable
fun LaurelTitle(title: String, tagline: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Laurel(-1f)
            // The thick outline spreads past the text's measured width, so leave room.
            Spacer(Modifier.width(9.dp))
            OutlinedText(title, style = GameText.Display, fontSize = 32.sp, strokeWidth = 9f, outline = GaragePurpleInk)
            Spacer(Modifier.width(9.dp))
            Laurel(1f)
        }
        Box(
            Modifier
                .offset(y = (-2).dp)
                .drawBehind {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(Color(0xFFB48CFF).copy(alpha = 0.35f), Offset(-3.dp.toPx(), -3.dp.toPx()), Size(size.width + 6.dp.toPx(), size.height + 6.dp.toPx()), CornerRadius(r.x + 3.dp.toPx()))
                    drawRoundRect(GaragePurpleInk, Offset(0f, 2.dp.toPx()), size, r)
                    drawRoundRect(Color(0xFFC9B3FF), Offset.Zero, size, r)
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color(0xFFA77BFF), Color(0xFF7A45E6))),
                        Offset(2.dp.toPx(), 2.dp.toPx()), Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), CornerRadius(r.x - 2.dp.toPx()),
                    )
                }
                .padding(horizontal = 16.dp, vertical = 4.dp),
        ) {
            Text(tagline, style = GameText.Label.copy(color = Color.White, fontFamily = TitleFont, fontSize = 15.sp))
        }
    }
}

/**
 * A gold laurel branch: a stem curving up around the title with pointed leaves along it, like a
 * victory wreath. [side] -1 is the left branch (opening towards the title), 1 the right.
 */
@Composable
private fun Laurel(side: Float) {
    Canvas(Modifier.size(22.dp, 40.dp)) {
        val w = size.width
        val h = size.height
        // Mirror so both branches open towards the title in the middle.
        val inner = if (side < 0) w * 0.95f else w * 0.05f
        val outer = if (side < 0) w * 0.05f else w * 0.95f
        val p0 = Offset(inner, h * 0.98f)
        val p1 = Offset(outer - side * w * 0.15f, h * 0.62f)
        val p2 = Offset(inner - side * w * 0.12f, h * 0.06f)
        fun at(t: Float): Offset { val v = 1 - t; return p0 * (v * v) + p1 * (2 * v * t) + p2 * (t * t) }
        fun tangent(t: Float): Offset { val v = 1 - t; return (p1 - p0) * (2 * v) + (p2 - p1) * (2 * t) }
        val stem = Path().apply { moveTo(p0.x, p0.y); quadraticTo(p1.x, p1.y, p2.x, p2.y) }
        drawPath(stem, Color(0xFF9A5B00), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
        drawPath(stem, Color(0xFFE0A21A), style = Stroke(1.6f.dp.toPx(), cap = StrokeCap.Round))
        val lw = 5.dp.toPx()
        val lh = 11.dp.toPx()
        for (i in 0 until 5) {
            val t = 0.12f + i * 0.19f
            val p = at(t)
            val d = tangent(t)
            val heading = Math.toDegrees(kotlin.math.atan2(d.x.toDouble(), (-d.y).toDouble())).toFloat()
            // One leaf flaring outwards, a smaller one tucked inwards.
            for ((flare, k) in listOf(-side * 42f to 1f, side * 30f to 0.75f)) {
                rotate(heading + flare, p) {
                    val ww = lw * k
                    val hh = lh * k * (1f - i * 0.06f)
                    val leaf = Path().apply {
                        moveTo(p.x, p.y)
                        quadraticTo(p.x - ww, p.y - hh * 0.5f, p.x, p.y - hh)
                        quadraticTo(p.x + ww, p.y - hh * 0.5f, p.x, p.y)
                        close()
                    }
                    drawPath(leaf, Color(0xFF9A5B00), style = Stroke(1.4f.dp.toPx()))
                    drawPath(leaf, Brush.verticalGradient(listOf(Color(0xFFFFE680), Color(0xFFFFB300)), p.y - hh, p.y))
                    drawLine(Color(0xFFE0A21A), p, Offset(p.x, p.y - hh * 0.8f), 0.8f.dp.toPx())
                }
            }
        }
        // Berry at the tip.
        drawCircle(Color(0xFF9A5B00), 3.2f.dp.toPx(), p2)
        drawCircle(Color(0xFFFFD84D), 2.4f.dp.toPx(), p2)
    }
}

// ---- Pickers ------------------------------------------------------------------------------

/** Car / Van / Bus pill: white with a purple rim, orange when selected. */
@Composable
fun TypePill(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        Modifier
            .drawBehind {
                val r = CornerRadius(size.height / 2)
                val lip = 4.dp.toPx()
                val face = Size(size.width, size.height - lip)
                val ink = if (selected) Color(0xFF8A3A06) else Color(0xFF3B2296)
                if (selected) drawRoundRect(Color(0xFFFFC24D).copy(alpha = 0.45f), Offset(-4.dp.toPx(), -4.dp.toPx()), Size(size.width + 8.dp.toPx(), size.height + 6.dp.toPx()), CornerRadius(r.x + 4.dp.toPx()))
                drawRoundRect(ink, Offset(0f, lip), face, r)
                drawRoundRect(ink, Offset.Zero, face, r)
                drawRoundRect(
                    if (selected) Brush.verticalGradient(listOf(Color(0xFFFFDC6E), Color(0xFFFFA62B), Color(0xFFF08A12))) else Brush.verticalGradient(listOf(Color.White, Color(0xFFF1ECFF), Color(0xFFDCD0FF))),
                    Offset(3.dp.toPx(), 3.dp.toPx()), Size(face.width - 6.dp.toPx(), face.height - 6.dp.toPx()), CornerRadius(size.height / 2 - 3.dp.toPx()),
                )
                drawRoundRect(Color.White.copy(alpha = 0.5f), Offset(12.dp.toPx(), 5.dp.toPx()), Size(face.width - 24.dp.toPx(), face.height * 0.22f), CornerRadius(4.dp.toPx()))
            }
            .clickable(onClick = onClick)
            .padding(start = 22.dp, end = 22.dp, top = 7.dp, bottom = 11.dp),
    ) {
        if (selected) {
            OutlinedText(label, style = GameText.Button, fontSize = 19.sp, strokeWidth = 7f, outline = Color(0xFF8A3A06), depth = 0.06f)
        } else {
            Text(label, style = GameText.Label.copy(color = Color(0xFF2E1A78), fontFamily = TitleFont, fontSize = 19.sp))
        }
    }
}

/** The eight paint colors in a purple bar with arrows; the selected one gets a green check. */
@Composable
fun ColorBar(selected: Int, onSelect: (Int) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(GaragePurpleInk, Offset(0f, 3.dp.toPx()), size, r)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF6C4FD9), Color(0xFF4B36B0))), Offset.Zero, size, r)
                drawRoundRect(Color(0xFF8F76F0), Offset.Zero, size, r, style = Stroke(2.5f.dp.toPx()))
            }
            .padding(horizontal = 6.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarChevron(left = true)
        Row(Modifier.weight(1f), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
            JamColor.entries.forEach { c ->
                val sel = c.ordinal == selected
                Box(Modifier.weight(1f).aspectRatio(1f).clickable { onSelect(c.ordinal) }, contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize(if (sel) 0.98f else 0.84f)) {
                        val col = Color(c.hex)
                        val r = size.minDimension / 2
                        drawCircle(Color.Black.copy(alpha = 0.3f), r, center + Offset(0f, 2.dp.toPx()))
                        drawCircle(if (sel) Color.White else col.shade(0.6f), r, center)
                        drawCircle(Brush.verticalGradient(listOf(col.tint(0.35f), col, col.shade(0.85f))), r - (if (sel) 3.dp else 2.dp).toPx(), center)
                        drawOval(Color.White.copy(alpha = 0.45f), Offset(r * 0.5f, r * 0.3f), Size(r * 0.9f, r * 0.45f))
                    }
                    if (sel) {
                        Box(Modifier.align(Alignment.BottomEnd).offset(x = 3.dp, y = 3.dp).size(15.dp).drawBehind {
                            drawCircle(Color.White, size.minDimension / 2)
                            drawCircle(Color(0xFF43A047), size.minDimension / 2 - 1.5f.dp.toPx())
                        }, contentAlignment = Alignment.Center) { GameIcon(Icon.CHECK, 11.dp) }
                    }
                }
            }
        }
        BarChevron(left = false)
    }
}

/** White chevron at the ends of the color bar. */
@Composable
private fun BarChevron(left: Boolean) {
    Canvas(Modifier.size(22.dp, 28.dp)) {
        val x0 = if (left) size.width * 0.65f else size.width * 0.35f
        val x1 = if (left) size.width * 0.3f else size.width * 0.7f
        val p = Path().apply { moveTo(x0, size.height * 0.25f); lineTo(x1, size.height * 0.5f); lineTo(x0, size.height * 0.75f) }
        drawPath(p, Color.White, style = Stroke(3.5f.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

/** Glossy green "Equipped on all vehicles" pill. */
@Composable
fun EquippedBadge(modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(52.dp)
            .drawBehind {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(Color(0xFF2E7D32).copy(alpha = 0.5f), Offset(0f, 3.dp.toPx()), size, r)
                drawRoundRect(Color(0xFF43A047), Offset.Zero, size, r)
                drawRoundRect(
                    Brush.verticalGradient(listOf(Color(0xFFF2FFE6), Color(0xFFCFF2B8))),
                    Offset(3.dp.toPx(), 3.dp.toPx()), Size(size.width - 6.dp.toPx(), size.height - 6.dp.toPx()), CornerRadius(size.height / 2 - 3.dp.toPx()),
                )
            },
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GameIcon(Icon.CHECK, 28.dp)
        Spacer(Modifier.width(8.dp))
        Text("Equipped on all vehicles", style = GameText.Label.copy(color = Color(0xFF1B5E20), fontFamily = TitleFont, fontSize = 17.sp))
    }
}

/** "PAINT PACKS" on a purple brush-stroke banner with a paint roller. */
@Composable
fun PaintPacksBanner() {
    Box(Modifier.padding(start = 10.dp, top = 4.dp), contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier
                .drawBehind {
                    val p = Path().apply {
                        moveTo(10.dp.toPx(), 2.dp.toPx())
                        lineTo(size.width - 6.dp.toPx(), 0f)
                        lineTo(size.width, size.height * 0.35f)
                        lineTo(size.width - 10.dp.toPx(), size.height * 0.6f)
                        lineTo(size.width - 2.dp.toPx(), size.height)
                        lineTo(4.dp.toPx(), size.height - 2.dp.toPx())
                        lineTo(0f, size.height * 0.5f)
                        close()
                    }
                    drawPath(p, Brush.horizontalGradient(listOf(Color(0xFF8E5CF7), Color(0xFF6A3FD9))))
                    drawPath(p, GaragePurpleInk.copy(alpha = 0.4f), style = Stroke(2.dp.toPx()))
                }
                .padding(start = 46.dp, end = 26.dp, top = 5.dp, bottom = 6.dp),
        ) {
            OutlinedText("PAINT PACKS", style = GameText.Title, fontSize = 22.sp, strokeWidth = 7f, outline = GaragePurpleInk, depth = 0.07f)
        }
        // Paint roller.
        Canvas(Modifier.size(44.dp).offset(x = (-4).dp)) {
            val u = size.width / 44f
            rotate(-25f, center) {
                drawRoundRect(Color(0xFF9A5B00), Offset(4 * u, 8 * u), Size(30 * u, 16 * u), CornerRadius(6 * u))
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFE066), Color(0xFFFFB300)), 9 * u, 22 * u), Offset(5.5f * u, 9.5f * u), Size(27 * u, 13 * u), CornerRadius(5 * u))
                drawLine(Color(0xFF8A90A8), Offset(19 * u, 24 * u), Offset(19 * u, 32 * u), 2.5f * u)
                drawRoundRect(Color(0xFFE53935), Offset(15 * u, 31 * u), Size(8 * u, 12 * u), CornerRadius(3 * u))
            }
        }
    }
}

/** Sunburst rays behind the vehicles on a pack card. */
fun DrawScope.drawSunburst(accent: Color) {
    drawRect(Brush.radialGradient(listOf(accent.tint(0.55f), accent.tint(0.1f)), center, size.maxDimension * 0.7f))
    for (i in 0 until 14) {
        rotate(i * (360f / 14f), center) {
            val p = Path().apply {
                moveTo(center.x, center.y)
                lineTo(center.x + size.maxDimension, center.y - size.maxDimension * 0.12f)
                lineTo(center.x + size.maxDimension, center.y + size.maxDimension * 0.12f)
                close()
            }
            drawPath(p, Color.White.copy(alpha = 0.16f))
        }
    }
}
