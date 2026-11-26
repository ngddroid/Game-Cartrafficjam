package com.ngstudio.trafficjam.game.render

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.nativeCanvas
import com.ngstudio.trafficjam.game.engine.CROWD_LANES
import com.ngstudio.trafficjam.game.engine.GameLayout
import com.ngstudio.trafficjam.game.engine.SLOT_COUNT
import com.ngstudio.trafficjam.game.engine.VIP_SLOT
import com.ngstudio.trafficjam.game.model.Level
import kotlin.math.sin
import kotlin.random.Random

/** Paints everything that never moves during a level, plus the world postcards for menus. */
object SceneArt {

    fun render(layout: GameLayout, level: Level, theme: SceneTheme): ImageBitmap =
        renderBitmap(layout.width.toInt(), layout.height.toInt()) { paint(layout, level, theme) }

    // --- Textures ---------------------------------------------------------------------------

    private fun noiseTile(base: Color, size: Int, seed: Int, speckles: Int, spread: Float): ImageBitmap =
        renderBitmap(size, size) {
            drawRect(base)
            val rnd = Random(seed)
            repeat(4) {
                drawCircle(base.shade(0.97f + rnd.nextFloat() * 0.05f).copy(alpha = 0.35f), size * (0.15f + rnd.nextFloat() * 0.25f), Offset(rnd.nextFloat() * size, rnd.nextFloat() * size))
            }
            repeat(speckles) {
                val f = 1f + (rnd.nextFloat() - 0.5f) * spread
                drawCircle(base.shade(f), 0.5f + rnd.nextFloat() * 1.1f, Offset(rnd.nextFloat() * size, rnd.nextFloat() * size))
            }
        }

    private fun grassTile(base: Color, size: Int): ImageBitmap = renderBitmap(size, size) {
        drawRect(base)
        val rnd = Random(5)
        repeat(260) {
            val x = rnd.nextFloat() * size
            val y = rnd.nextFloat() * size
            val c = if (rnd.nextBoolean()) base.tint(0.14f) else base.shade(0.86f)
            drawLine(c, Offset(x, y), Offset(x + (rnd.nextFloat() - 0.5f) * 3f, y - 2f - rnd.nextFloat() * 4f), 1.1f, StrokeCap.Round)
        }
        repeat(5) {
            val c = listOf(Color.White, Color(0xFFFFEB3B), Color(0xFFFF8A80))[it % 3]
            drawCircle(c.copy(alpha = 0.9f), 1.6f, Offset(rnd.nextFloat() * size, rnd.nextFloat() * size))
        }
    }

    private fun DrawScope.textured(tile: ImageBitmap, topLeft: Offset, size: Size) {
        drawRect(ShaderBrush(ImageShader(tile, TileMode.Repeated, TileMode.Repeated)), topLeft, size)
    }

    // --- Full game scene --------------------------------------------------------------------

    private fun DrawScope.paint(lay: GameLayout, level: Level, th: SceneTheme) {
        val u = lay.u
        val w = size.width
        val px = (96 * u / 2).toInt().coerceAtLeast(64)

        // Grass under everything
        textured(grassTile(th.outside, px), Offset.Zero, size)

        if (lay.loop) {
            ringPark(lay, th, u, px)
        } else {
            // Sky + backdrop
            val floorTop = lay.topInset + 120 * u
            sky(th, w, floorTop, lay.topInset)
            backdrop(th, w, lay.topInset, floorTop, u)

            // Platform floor
            floor(th, Offset(0f, floorTop), Size(w, lay.slotsTop - floorTop), u, px)
            drawRect(Brush.verticalGradient(listOf(Color(0x33000000), Color.Transparent), floorTop, floorTop + 10 * u), Offset(0f, floorTop), Size(w, 10 * u))
            floorProps(th, lay, u)
            queueBarrier(lay, th, u)
        }

        // Platform edge: tactile strip + curb
        drawRect(Color(0xFFFFD54F), Offset(0f, lay.slotsTop - 9 * u), Size(w, 4 * u))
        var dx = 2 * u
        while (dx < w) { drawCircle(Color(0xFFF9A825), 0.8f * u, Offset(dx, lay.slotsTop - 7 * u)); dx += 4 * u }
        drawRect(Color(0xFFE9ECEF), Offset(0f, lay.slotsTop - 4 * u), Size(w, 4 * u))
        drawRect(Color(0xFFB4BAC2), Offset(0f, lay.slotsTop), Size(w, 3 * u))

        // Parking stalls
        textured(noiseTile(th.slotBand, px, 11, 500, 0.18f), Offset(0f, lay.slotsTop + 3 * u), Size(w, lay.slotsBottom - lay.slotsTop - 3 * u))
        for (i in 0..VIP_SLOT) stall(lay, i, u)

        // Road
        textured(noiseTile(th.road, px, 3, 600, 0.22f), Offset(0f, lay.roadTop), Size(w, lay.roadBottom - lay.roadTop))
        drawRect(Color.Black.copy(alpha = 0.18f), Offset(0f, lay.roadTop), Size(w, 3 * u))
        drawRect(Color.White.copy(alpha = 0.85f), Offset(0f, lay.roadTop + 3 * u), Size(w, 2 * u))
        drawRect(Color.White.copy(alpha = 0.85f), Offset(0f, lay.roadBottom - 3 * u), Size(w, 2 * u))
        drawLine(
            if (th.dark) Color(0xFFFFE066) else Color.White.copy(alpha = 0.7f), Offset(0f, lay.roadY), Offset(w, lay.roadY), 2.4f * u,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(14 * u, 12 * u)),
        )
        // Manhole cover
        drawCircle(th.road.shade(0.75f), 6 * u, Offset(w * 0.7f, lay.roadY + 6 * u))
        drawCircle(th.road.shade(0.9f), 4.5f * u, Offset(w * 0.7f, lay.roadY + 6 * u), style = Stroke(1f * u))

        lot(lay, level, th, u, px)

        // Hedge and flowers along the bottom
        val barTop = lay.lotRect.bottom + 8 * u
        for (k in 0..(w / (22 * u)).toInt()) {
            val x = k * 22 * u + 6 * u
            bush(Offset(x, barTop + 6 * u), 13 * u, th.outside.shade(0.85f))
        }
    }

    /** Crowd Loop: a park seen from above with the ring road the crowd shuffles around. */
    private fun DrawScope.ringPark(lay: GameLayout, th: SceneTheme, u: Float, px: Int) {
        val crowd = lay.crowd ?: return
        val w = size.width
        val top = lay.topInset
        // Sunny park lawn with a soft mowing pattern.
        val lawn = Color(0xFF8BCB5A)
        drawRect(lawn, Offset(0f, 0f), Size(w, lay.slotsTop))
        var k = 0
        var y = top + 40 * u
        while (y < lay.slotsTop) {
            if (k % 2 == 0) drawRect(Color.White.copy(alpha = 0.06f), Offset(0f, y), Size(w, 22 * u))
            y += 22 * u; k++
        }
        textured(grassTile(lawn, px), Offset(0f, top + 40 * u), Size(w, lay.slotsTop - top - 40 * u))
        drawRect(Brush.verticalGradient(listOf(Color(0xFF5FA8E8), Color(0xFF8BCB5A)), 0f, top + 48 * u), size = Size(w, top + 48 * u))

        // Inner field: a lighter lawn inside the ring with a flower bed and trees.
        val c = lay.ringCenter
        drawOval(Color(0xFFA4DB6E), Offset(c.x - lay.ringRx + 18 * u, c.y - lay.ringRy + 18 * u), Size((lay.ringRx - 18 * u) * 2, (lay.ringRy - 18 * u) * 2))
        drawOval(Color.White.copy(alpha = 0.18f), Offset(c.x - lay.ringRx + 18 * u, c.y - lay.ringRy + 18 * u), Size((lay.ringRx - 18 * u) * 2, (lay.ringRy - 18 * u) * 2), style = Stroke(2 * u))
        val bed = Offset(c.x, c.y + 40 * u)
        drawOval(Color(0xFF6D4C41), Offset(bed.x - 30 * u, bed.y - 9 * u), Size(60 * u, 18 * u))
        drawOval(Color(0xFF8D6E63), Offset(bed.x - 27 * u, bed.y - 8 * u), Size(54 * u, 14 * u))
        val rnd = Random(7)
        repeat(22) {
            val fx = bed.x + (rnd.nextFloat() - 0.5f) * 48 * u
            val fy = bed.y + (rnd.nextFloat() - 0.5f) * 10 * u
            val fc = listOf(Color(0xFFFF5A7A), Color(0xFFFFEB3B), Color.White, Color(0xFFBA68C8))[it % 4]
            drawCircle(fc, 2.2f * u, Offset(fx, fy))
        }
        tree(Offset(c.x - 78 * u, c.y + 8 * u), u, Color(0xFF43A047))
        tree(Offset(c.x + 80 * u, c.y + 12 * u), u, Color(0xFF2E7D32))
        bush(Offset(c.x - 56 * u, c.y + 40 * u), 10 * u, Color(0xFF558B2F))
        bush(Offset(c.x + 58 * u, c.y + 42 * u), 11 * u, Color(0xFF689F38))

        // The road the crowd stands on.
        val road = Path().apply {
            val pts = crowd.samples(6)
            moveTo(pts[0].x, pts[0].y + 30 * u)
            for (p in pts) lineTo(p.x, p.y)
        }
        val width = CROWD_LANES * lay.laneGap + 12 * u
        drawPath(road, Color.Black.copy(alpha = 0.16f), style = Stroke(width + 8 * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(road, Color(0xFFE6E9EC), style = Stroke(width + 6 * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(road, Color(0xFFB7BEC6), style = Stroke(width + 1.5f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(
            road, ShaderBrush(ImageShader(noiseTile(Color(0xFF7D838D), px, 29, 520, 0.2f), TileMode.Repeated, TileMode.Repeated)),
            style = Stroke(width, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
        drawPath(
            road, Color.White.copy(alpha = 0.45f),
            style = Stroke(1.6f * u, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9 * u, 9 * u))),
        )

        // Trees and benches around the outside.
        tree(Offset(22 * u, top + 92 * u), u, Color(0xFF388E3C))
        tree(Offset(w - 20 * u, top + 104 * u), u, Color(0xFF43A047))
        bush(Offset(16 * u, top + 236 * u), 11 * u, Color(0xFF558B2F))
        bush(Offset(w - 16 * u, top + 236 * u), 12 * u, Color(0xFF558B2F))
        bush(Offset(160 * u, top + 294 * u), 10 * u, Color(0xFF689F38))
        bush(Offset(210 * u, top + 290 * u), 12 * u, Color(0xFF7CB342))
    }

    private fun DrawScope.sky(th: SceneTheme, w: Float, bottom: Float, top: Float) {
        drawRect(Brush.verticalGradient(listOf(th.skyTop, th.skyBottom), 0f, bottom), size = Size(w, bottom))
        if (!th.dark) {
            drawCircle(Brush.radialGradient(listOf(Color.White.copy(alpha = 0.55f), Color.Transparent), Offset(w * 0.82f, top + 22f), w * 0.35f), w * 0.35f, Offset(w * 0.82f, top + 22f))
        }
    }

    // --- Floors -----------------------------------------------------------------------------

    private fun DrawScope.floor(th: SceneTheme, tl: Offset, sz: Size, u: Float, px: Int) {
        val c = th.platform
        val tile = when (th.backdrop) {
            Backdrop.PIER, Backdrop.FARM -> renderBitmap(px, px) {
                drawRect(c)
                val ph = px / 4f
                for (k in 0 until 4) {
                    drawRect(c.shade(if (k % 2 == 0) 1f else 0.95f), Offset(0f, k * ph), Size(px.toFloat(), ph))
                    drawLine(c.shade(0.72f), Offset(0f, k * ph), Offset(px.toFloat(), k * ph), 1.2f)
                    val sx = ((k * 37) % px).toFloat()
                    drawLine(c.shade(0.78f), Offset(sx, k * ph), Offset(sx, (k + 1) * ph), 1.2f)
                    drawCircle(c.shade(0.6f), 0.9f, Offset(sx + 4, k * ph + ph / 2))
                }
            }
            Backdrop.DESERT, Backdrop.SNOW -> noiseTile(c, px, 21, 260, 0.1f)
            Backdrop.ZOO, Backdrop.JUNGLE -> renderBitmap(px, px) {
                drawRect(c.shade(0.85f))
                val rnd = Random(9)
                repeat(14) {
                    val o = Offset(rnd.nextFloat() * px, rnd.nextFloat() * px)
                    drawRoundRect(c.shade(0.95f + rnd.nextFloat() * 0.12f), o, Size(px * 0.28f, px * 0.2f), CornerRadius(px * 0.08f))
                }
            }
            Backdrop.FAIR -> renderBitmap(px, px) {
                val h = px / 2f
                drawRect(c)
                drawRect(c.shade(0.9f), Offset(0f, 0f), Size(h, h))
                drawRect(c.shade(0.9f), Offset(h, h), Size(h, h))
            }
            Backdrop.SPACE -> renderBitmap(px, px) {
                drawRect(c)
                drawRect(c.shade(0.8f), Offset.Zero, Size(px.toFloat(), px.toFloat()), style = Stroke(1.5f))
                for (k in 1..3) drawLine(c.tint(0.08f), Offset(k * px / 4f, 0f), Offset(k * px / 4f, px.toFloat()), 0.8f)
            }
            else -> renderBitmap(px, px) {
                val h = px / 2f
                for (i in 0 until 2) for (j in 0 until 2) {
                    drawRect(c.shade(if ((i + j) % 2 == 0) 1f else 0.96f), Offset(i * h, j * h), Size(h, h))
                }
                for (k in 0..2) {
                    drawLine(c.shade(0.82f), Offset(k * h, 0f), Offset(k * h, px.toFloat()), 1f)
                    drawLine(c.shade(0.82f), Offset(0f, k * h), Offset(px.toFloat(), k * h), 1f)
                }
                if (th.dark) drawRect(th.accent.copy(alpha = 0.12f), Offset(1f, 1f), Size(h - 2, h - 2))
            }
        }
        textured(tile, tl, sz)
    }

    private fun DrawScope.floorProps(th: SceneTheme, lay: GameLayout, u: Float) {
        val y = lay.queueY - 30 * u
        lampPost(Offset(14 * u, lay.queueY - 2 * u), u, th)
        when (th.backdrop) {
            Backdrop.STATION, Backdrop.NEON, Backdrop.SPACE -> {
                bench(Offset(size.width * 0.52f, y + 2 * u), u, if (th.dark) Color(0xFF7C4DFF) else Color(0xFF5C6BC0))
                plantPot(Offset(size.width * 0.86f, y + 4 * u), u)
            }
            Backdrop.ZOO, Backdrop.JUNGLE, Backdrop.FARM -> {
                bush(Offset(size.width * 0.5f, y + 2 * u), 14 * u, th.accent)
                bush(Offset(size.width * 0.9f, y + 2 * u), 12 * u, th.accent.shade(0.85f))
            }
            Backdrop.FAIR -> {
                balloon(Offset(size.width * 0.86f, y - 20 * u), u, Color(0xFFFF4081))
                balloon(Offset(size.width * 0.9f, y - 26 * u), u, Color(0xFFFFEB3B))
                balloon(Offset(size.width * 0.82f, y - 24 * u), u, Color(0xFF29B6F6))
            }
            Backdrop.PIER -> {
                for (k in 0 until 3) drawCircle(Color(0xFFE53935), 4 * u, Offset(size.width * (0.5f + k * 0.18f), y + 6 * u), style = Stroke(2 * u))
            }
            Backdrop.SNOW -> snowman(Offset(size.width * 0.88f, y + 8 * u), u)
            Backdrop.DESERT -> cactus(Offset(size.width * 0.88f, y + 10 * u), u * 0.7f)
        }
    }

    private fun DrawScope.queueBarrier(lay: GameLayout, th: SceneTheme, u: Float) {
        val postY = lay.queueY - 24 * u
        val rope = if (th.dark) Color(0xFFE040FB) else Color(0xFFC62828)
        val xs = generateSequence(lay.queueStartX + 34 * u) { it + 44 * u }.takeWhile { it < lay.queueEndX - 26 * u }.toList()
        for (k in 0 until xs.size - 1) {
            val a = Offset(xs[k], postY)
            val b = Offset(xs[k + 1], postY)
            val p = Path().apply { moveTo(a.x, a.y); quadraticTo((a.x + b.x) / 2, a.y + 7 * u, b.x, b.y) }
            drawPath(p, rope.shade(0.6f), style = Stroke(2.6f * u, cap = StrokeCap.Round))
            drawPath(p, rope, style = Stroke(1.6f * u, cap = StrokeCap.Round))
        }
        for (x in xs) {
            drawOval(Color.Black.copy(alpha = 0.18f), Offset(x - 4 * u, postY + 14 * u), Size(8 * u, 3 * u))
            drawRoundRect(Color(0xFFB8860B), Offset(x - 1.3f * u, postY), Size(2.6f * u, 16 * u), CornerRadius(u))
            drawRoundRect(Color(0xFFFFD54F), Offset(x - 0.7f * u, postY), Size(1f * u, 16 * u), CornerRadius(u))
            drawCircle(Color(0xFFFFC107), 2.4f * u, Offset(x, postY))
            drawCircle(Color.White.copy(alpha = 0.6f), 0.8f * u, Offset(x - 0.8f * u, postY - 0.8f * u))
        }
    }

    // --- Stalls -----------------------------------------------------------------------------

    private fun DrawScope.stall(lay: GameLayout, i: Int, u: Float) {
        val r = lay.slotRect(i)
        if (i == VIP_SLOT) {
            drawRoundRect(Color(0xFF8E1B1B), r.topLeft + Offset(0f, 2 * u), r.size, CornerRadius(6 * u))
            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE53935), Color(0xFFB71C1C)), r.top, r.bottom), r.topLeft, r.size, CornerRadius(6 * u))
            drawRoundRect(Color(0xFFFFD54F), r.topLeft + Offset(3 * u, 3 * u), Size(r.width - 6 * u, r.height - 6 * u), CornerRadius(4 * u), style = Stroke(1.6f * u))
            crown(Offset(r.center.x, r.top + 18 * u), 9 * u)
            text("VIP", Offset(r.center.x, r.bottom - 14 * u), 11 * u, Color(0xFFFFE082))
            return
        }
        val line = 2.6f * u
        drawRect(Color.Black.copy(alpha = 0.15f), Offset(r.left - line / 2 + u, r.top + u), Size(line, r.height))
        drawRect(Color.White, Offset(r.left - line / 2, r.top), Size(line, r.height))
        if (i == SLOT_COUNT - 1) drawRect(Color.White, Offset(r.right - line / 2, r.top), Size(line, r.height))
        // wheel stop
        drawRoundRect(Color(0xFF9EA4AC), Offset(r.left + r.width * 0.2f, r.top + 4 * u), Size(r.width * 0.6f, 4 * u), CornerRadius(1.5f * u))
        drawRoundRect(Color(0xFFDCE0E5), Offset(r.left + r.width * 0.2f, r.top + 3 * u), Size(r.width * 0.6f, 2.5f * u), CornerRadius(1.5f * u))
        text("${i + 1}", Offset(r.center.x, r.bottom - 7 * u), 9 * u, Color.White.copy(alpha = 0.55f))
    }

    // --- Lot --------------------------------------------------------------------------------

    private fun DrawScope.lot(lay: GameLayout, level: Level, th: SceneTheme, u: Float, px: Int) {
        val lot = lay.lotRect
        val r = CornerRadius(18 * u)
        // Curb with a visible side
        drawRoundRect(Color.Black.copy(alpha = 0.18f), lot.topLeft + Offset(0f, 8 * u), lot.size, r)
        drawRoundRect(Color(0xFFA9AFB6), lot.topLeft + Offset(0f, 4 * u), lot.size, r)
        drawRoundRect(Color(0xFFE6E9EC), lot.topLeft - Offset(3 * u, 3 * u), Size(lot.width + 6 * u, lot.height + 6 * u), CornerRadius(20 * u))
        // Asphalt
        val asphalt = noiseTile(th.lot, px, 17, 520, 0.12f)
        val inner = lot.deflate(3 * u)
        val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(inner, CornerRadius(15 * u))) }
        clipPath(clip) {
            textured(asphalt, inner.topLeft, inner.size)
            drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.12f), Color.Transparent), inner.top, inner.top + 18 * u), inner.topLeft, Size(inner.width, 18 * u))
        }
        // Driving loop with chevrons
        val t = lay.track
        val loop = androidx.compose.ui.geometry.Rect(t.left, t.top + 22 * u, t.right, t.bottom)
        drawRoundRect(
            Color.White.copy(alpha = 0.45f), loop.topLeft, loop.size, CornerRadius(18 * u),
            style = Stroke(1.6f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8 * u, 8 * u))),
        )
        for (y in listOf(loop.center.y - 30 * u, loop.center.y + 40 * u)) {
            chevron(Offset(loop.left, y), u, up = true)
            chevron(Offset(loop.right, y), u, up = true)
        }
        // P markers in the corners
        text("P", Offset(lot.left + 18 * u, lot.bottom - 16 * u), 14 * u, Color.White.copy(alpha = 0.35f))
        text("P", Offset(lot.right - 18 * u, lot.bottom - 16 * u), 14 * u, Color.White.copy(alpha = 0.35f))

        // Board pad in the level's shape with a painted border
        val pad = th.lot.shade(0.9f)
        withTransform({
            translate(lay.boardCenter.x, lay.boardCenter.y)
            rotate(lay.rotationDeg, Offset.Zero)
        }) {
            val unit = lay.unit
            val ox = -level.cols / 2f * unit
            val oy = -level.rows / 2f * unit
            val g = unit * 0.35f
            for (gy in 0 until level.rows) for (gx in 0 until level.cols) {
                if (!level.inMask(gx, gy)) continue
                drawRect(pad, Offset(ox + gx * unit - g, oy + gy * unit - g), Size(unit + 2 * g, unit + 2 * g))
            }
            val lw = 1.6f * u
            val paint = Color.White.copy(alpha = 0.8f)
            for (gy in 0 until level.rows) for (gx in 0 until level.cols) {
                if (!level.inMask(gx, gy)) continue
                val x0 = ox + gx * unit - g
                val y0 = oy + gy * unit - g
                val x1 = ox + (gx + 1) * unit + g
                val y1 = oy + (gy + 1) * unit + g
                if (!level.inMask(gx, gy - 1)) drawLine(paint, Offset(x0, y0), Offset(x1, y0), lw)
                if (!level.inMask(gx, gy + 1)) drawLine(paint, Offset(x0, y1), Offset(x1, y1), lw)
                if (!level.inMask(gx - 1, gy)) drawLine(paint, Offset(x0, y0), Offset(x0, y1), lw)
                if (!level.inMask(gx + 1, gy)) drawLine(paint, Offset(x1, y0), Offset(x1, y1), lw)
            }
        }
    }

    private fun DrawScope.chevron(c: Offset, u: Float, up: Boolean) {
        val d = if (up) 1f else -1f
        val p = Path().apply {
            moveTo(c.x - 5 * u, c.y + d * 3 * u)
            lineTo(c.x, c.y - d * 3 * u)
            lineTo(c.x + 5 * u, c.y + d * 3 * u)
        }
        drawPath(p, Color.White.copy(alpha = 0.55f), style = Stroke(2.2f * u, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }

    // --- Postcards --------------------------------------------------------------------------

    private val postcards = HashMap<Triple<Int, Int, Int>, ImageBitmap>()

    /** A small scenic card of a world: sky, backdrop, floor and road. */
    fun postcard(th: SceneTheme, wPx: Int, hPx: Int): ImageBitmap = synchronized(postcards) {
        postcards.getOrPut(Triple(Themes.all.indexOf(th), wPx, hPx)) {
            renderBitmap(wPx, hPx) {
                val w = wPx.toFloat()
                val h = hPx.toFloat()
                val u = w / 290f
                val base = minOf(w / 2.6f, h * 0.64f)
                val tile = (48 * u).toInt().coerceAtLeast(24)
                sky(th, w, base, 0f)
                backdrop(th, w, 0f, base, u)
                SceneAmbient.staticPose(this, th, w, base, u)
                floor(th, Offset(0f, base), Size(w, h - base), u, tile)
                val roadTop = base + (h - base) * 0.42f
                val roadH = (h - base) * 0.42f
                drawRect(th.road, Offset(0f, roadTop), Size(w, roadH))
                drawRect(Color.White.copy(alpha = 0.8f), Offset(0f, roadTop), Size(w, roadH * 0.06f))
                drawLine(
                    Color.White.copy(alpha = 0.7f), Offset(0f, roadTop + roadH / 2), Offset(w, roadTop + roadH / 2), roadH * 0.06f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.05f, w * 0.04f)),
                )
                textured(grassTile(th.outside, tile), Offset(0f, roadTop + roadH), Size(w, h - roadTop - roadH))
            }
        }
    }

    // --- Backdrops --------------------------------------------------------------------------

    /** The world's signature skyline between [top] and the floor line [base]. */
    fun DrawScope.backdrop(th: SceneTheme, w: Float, top: Float, base: Float, u: Float) {
        val rnd = Random(th.name.hashCode())
        when (th.backdrop) {
            Backdrop.STATION -> {
                skyline(w, base - 18 * u, u, Color(0xFFB7D7F0), rnd, 70f)
                cloud(Offset(70 * u, top + 26 * u), u); cloud(Offset(250 * u, top + 16 * u), u * 0.8f)
                // Canopy on pillars
                drawRect(Color(0xFF90A4AE), Offset(0f, top + 38 * u), Size(w, 4 * u))
                drawRect(Brush.verticalGradient(listOf(Color(0xFFECEFF1), Color(0xFFB0BEC5)), top + 30 * u, top + 40 * u), Offset(0f, top + 30 * u), Size(w, 9 * u))
                var x = 30 * u
                while (x < w) { drawRect(Color(0xFF78909C), Offset(x, top + 40 * u), Size(4 * u, base - top - 40 * u)); x += 90 * u }
                train(w, top + 50 * u, base, u)
                // Departure board
                val bx = w * 0.62f
                drawRect(Color(0xFF37474F), Offset(bx + 18 * u, top + 41 * u), Size(1.5f * u, 8 * u))
                drawRoundRect(Color(0xFF263238), Offset(bx, top + 46 * u), Size(40 * u, 13 * u), CornerRadius(2 * u))
                for (k in 0 until 2) drawRect(Color(0xFFFFC107).copy(alpha = 0.9f), Offset(bx + 4 * u, top + 49.5f * u + k * 4.5f * u), Size((20 + k * 9) * u, 2 * u))
            }
            Backdrop.ZOO -> {
                cloud(Offset(80 * u, top + 24 * u), u); cloud(Offset(290 * u, top + 34 * u), u * 0.7f)
                hills(w, base - 6 * u, u, Color(0xFFAED581), Color(0xFF8BC34A), 50f)
                for (i in 0 until 6) tree(Offset((22 + i * 64) * u + rnd.nextFloat() * 14 * u, base - 22 * u), u * (0.9f + rnd.nextFloat() * 0.35f), Color(0xFF66BB6A))
                giraffe(Offset(w * 0.2f, base - 12 * u), u)
                elephant(Offset(w * 0.86f, base - 10 * u), u)
                fence(w, base - 2 * u, u, Color(0xFFB07D4E))
                // Arch gate
                val ax = w * 0.55f
                for (sx in listOf(-42f, 34f)) {
                    drawRoundRect(Color(0xFF6D4C41), Offset(ax + sx * u, base - 68 * u), Size(8 * u, 66 * u), CornerRadius(2 * u))
                    drawRoundRect(Color(0xFF8D6E63), Offset(ax + (sx + 1.5f) * u, base - 68 * u), Size(3 * u, 66 * u), CornerRadius(2 * u))
                }
                drawRoundRect(Color(0xFF4E342E), Offset(ax - 52 * u, base - 84 * u), Size(104 * u, 26 * u), CornerRadius(10 * u))
                drawRoundRect(Color(0xFF6D4C41), Offset(ax - 50 * u, base - 84 * u), Size(100 * u, 22 * u), CornerRadius(9 * u))
                for (k in 0 until 3) drawCircle(listOf(Color(0xFFFFCA28), Color(0xFF66BB6A), Color(0xFFFF7043))[k], 3 * u, Offset(ax - 40 * u + k * 40 * u, base - 88 * u))
                text("ZOO", Offset(ax, base - 73 * u), 15 * u, Color(0xFFFFE082))
            }
            Backdrop.FAIR -> {
                cloud(Offset(60 * u, top + 22 * u), u, Color(0xFFFFF0F7))
                // Ferris wheel legs (the wheel itself turns in the ambient layer)
                val c = SceneAmbient.ferrisCenter(w, base, u)
                drawLine(Color(0xFF7B1FA2), c, Offset(c.x - 30 * u, base), 4.5f * u, StrokeCap.Round)
                drawLine(Color(0xFF7B1FA2), c, Offset(c.x + 30 * u, base), 4.5f * u, StrokeCap.Round)
                carousel(Offset(w * 0.24f, base), u)
                // Bunting
                val bunt = Path().apply { moveTo(0f, top + 44 * u); quadraticTo(w * 0.25f, top + 58 * u, w * 0.5f, top + 44 * u) }
                drawPath(bunt, Color(0xFF8D6E63), style = Stroke(1f * u))
                for (k in 0 until 9) {
                    val t = k / 9f
                    val bx = w * 0.5f * t + 6 * u
                    val by = top + 44 * u + sin(t * Math.PI).toFloat() * 7 * u
                    val col = listOf(Color(0xFFFF4081), Color(0xFFFFEB3B), Color(0xFF29B6F6), Color(0xFF66BB6A))[k % 4]
                    drawPath(Path().apply { moveTo(bx - 4 * u, by); lineTo(bx + 4 * u, by); lineTo(bx, by + 8 * u); close() }, col)
                }
                stand(Offset(w * 0.52f, base), u)
            }
            Backdrop.PIER -> {
                cloud(Offset(90 * u, top + 24 * u), u); cloud(Offset(240 * u, top + 38 * u), u * 0.7f)
                val sea = top + 66 * u
                drawPath(Path().apply { moveTo(0f, sea); quadraticTo(w * 0.2f, sea - 18 * u, w * 0.4f, sea); close() }, Color(0xFF81C784))
                drawRect(Brush.verticalGradient(listOf(Color(0xFF4FC3F7), Color(0xFF0288D1), Color(0xFF01579B)), sea, base), Offset(0f, sea), Size(w, base - sea))
                lighthouse(Offset(w * 0.84f, base - 6 * u), u)
                sailboat(Offset(w * 0.32f, sea + 22 * u), u)
                drawRect(Color(0xFF6D4C41), Offset(0f, base - 14 * u), Size(w, 3 * u))
                var x = 6 * u
                while (x < w) {
                    drawRect(Color(0xFF5D4037), Offset(x, base - 18 * u), Size(4 * u, 18 * u))
                    drawRect(Color(0xFF8D6E63), Offset(x + 1 * u, base - 18 * u), Size(1.2f * u, 18 * u))
                    x += 28 * u
                }
            }
            Backdrop.DESERT -> {
                drawCircle(Color(0xFFFFE082), 30 * u, Offset(w * 0.8f, top + 36 * u))
                drawCircle(Color(0xFFFFF59D), 20 * u, Offset(w * 0.8f, top + 36 * u))
                hills(w, base - 30 * u, u, Color(0xFFF6D7A0), Color(0xFFEBC27C), 26f)
                pyramid(Offset(w * 0.22f, base - 4 * u), 80 * u)
                pyramid(Offset(w * 0.42f, base - 10 * u), 46 * u)
                hills(w, base, u, Color(0xFFF3CF8E), Color(0xFFE2AF62), 18f)
                palm(Offset(w * 0.66f, base - 2 * u), u * 0.9f)
                camel(Offset(w * 0.86f, base - 4 * u), u)
                for (i in 0 until 2) cactus(Offset((120 + i * 160) * u, base - 2 * u), u * 0.8f)
            }
            Backdrop.SNOW -> {
                for (i in 0 until 3) mountain(Offset((40 + i * 130) * u, base), (88 + i % 2 * 18) * u, u)
                cabin(Offset(w * 0.18f, base - 2 * u), u)
                for (i in 0 until 6) pine(Offset((60 + i * 52) * u + rnd.nextFloat() * 10 * u, base - 2 * u), u * (0.8f + rnd.nextFloat() * 0.45f))
                drawRect(Color.White, Offset(0f, base - 6 * u), Size(w, 6 * u))
            }
            Backdrop.NEON -> {
                for (i in 0 until 50) drawCircle(Color.White.copy(alpha = rnd.nextFloat() * 0.8f), rnd.nextFloat() * 1.3f * u, Offset(rnd.nextFloat() * w, top + rnd.nextFloat() * 70 * u))
                drawCircle(Color(0xFFFFF9C4), 14 * u, Offset(w * 0.14f, top + 28 * u))
                drawCircle(Color(0xFF3A2475), 11 * u, Offset(w * 0.14f + 6 * u, top + 25 * u))
                var x = -10 * u
                var i = 0
                while (x < w) {
                    val bw = (34 + rnd.nextInt(26)) * u
                    val bh = (50 + rnd.nextInt(56)) * u
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF2D2266), Color(0xFF1B1446)), base - bh, base), Offset(x, base - bh), Size(bw, bh))
                    var wy = base - bh + 8 * u
                    while (wy < base - 8 * u) {
                        var wx = x + 5 * u
                        while (wx < x + bw - 8 * u) {
                            if (rnd.nextFloat() < 0.5f) drawRect(listOf(Color(0xFFFFEB3B), Color(0xFF80DEEA), Color(0xFFFF80AB))[rnd.nextInt(3)].copy(alpha = 0.85f), Offset(wx, wy), Size(4 * u, 5 * u))
                            wx += 9 * u
                        }
                        wy += 11 * u
                    }
                    x += bw + 4 * u
                    i++
                }
                neon(Offset(w * 0.36f, base - 74 * u), Size(70 * u, 22 * u), Color(0xFFFF4FD8), u)
                neon(Offset(w * 0.7f, base - 92 * u), Size(50 * u, 18 * u), Color(0xFF40E0FF), u)
            }
            Backdrop.FARM -> {
                cloud(Offset(70 * u, top + 28 * u), u)
                hills(w, base - 10 * u, u, Color(0xFFE6C17A), Color(0xFFD4A85A), 40f)
                // field stripes
                for (k in 0 until 5) drawLine(Color(0xFFC58F45).copy(alpha = 0.5f), Offset(k * 80 * u, base - 4 * u), Offset(k * 80 * u + 60 * u, base - 30 * u), 2 * u)
                barn(Offset(w * 0.6f, base - 2 * u), u)
                val mill = SceneAmbient.millHub(w, base, u)
                drawPath(Path().apply { moveTo(mill.x - 7 * u, base - 2 * u); lineTo(mill.x - 3 * u, mill.y); lineTo(mill.x + 3 * u, mill.y); lineTo(mill.x + 7 * u, base - 2 * u); close() }, Color(0xFFEFEBE9))
                for (i in 0 until 3) hay(Offset((30 + i * 44) * u, base - 4 * u), u)
                for (i in 0 until 3) tree(Offset((300 + i * 26) * u, base - 16 * u), u * 0.8f, Color(0xFFFF8F00))
                fence(w, base - 2 * u, u, Color(0xFFFFF3E0))
            }
            Backdrop.JUNGLE -> {
                hills(w, base - 20 * u, u, Color(0xFF81C784), Color(0xFF4CAF50), 60f)
                // Waterfall cliff
                val wx = w * 0.72f
                drawRoundRect(Color(0xFF795548), Offset(wx - 34 * u, base - 96 * u), Size(68 * u, 96 * u), CornerRadius(10 * u))
                drawRect(Brush.verticalGradient(listOf(Color(0xFFB3E5FC), Color(0xFF4FC3F7)), base - 92 * u, base), Offset(wx - 10 * u, base - 92 * u), Size(20 * u, 92 * u))
                drawOval(Color.White.copy(alpha = 0.8f), Offset(wx - 18 * u, base - 8 * u), Size(36 * u, 8 * u))
                for (i in 0 until 5) palm(Offset((20 + i * 66) * u, base - 2 * u), u * (0.95f + rnd.nextFloat() * 0.25f))
                tent(Offset(w * 0.42f, base), 40 * u, Color(0xFFFFA726), u)
                for (k in 0 until 4) leaf(Offset(k * w / 3.2f, base + 2 * u), u, k % 2 == 0)
            }
            Backdrop.SPACE -> {
                for (i in 0 until 70) drawCircle(Color.White.copy(alpha = 0.3f + rnd.nextFloat() * 0.7f), rnd.nextFloat() * 1.4f * u, Offset(rnd.nextFloat() * w, top + rnd.nextFloat() * 100 * u))
                drawCircle(Brush.radialGradient(listOf(Color(0xFFFFAB91), Color(0xFFFF7043), Color(0xFFBF360C)), Offset(w * 0.15f, top + 30 * u), 30 * u), 22 * u, Offset(w * 0.18f, top + 38 * u))
                drawOval(Color(0xFFFFCC80), Offset(w * 0.18f - 36 * u, top + 34 * u), Size(72 * u, 9 * u), style = Stroke(2 * u))
                drawArc(Color(0xFF263238).copy(alpha = 0.7f), 180f, 180f, true, Offset(-20 * u, base - 30 * u), Size(w * 0.7f, 60 * u))
                dome(Offset(w * 0.12f, base), 36 * u)
                dome(Offset(w * 0.34f, base), 24 * u)
                dish(Offset(w * 0.52f, base), u)
                rocket(Offset(w * 0.8f, base), u)
            }
        }
    }

    // --- Props ------------------------------------------------------------------------------

    private fun DrawScope.skyline(w: Float, base: Float, u: Float, color: Color, rnd: Random, maxH: Float) {
        var x = 0f
        while (x < w) {
            val bw = (20 + rnd.nextInt(30)) * u
            val bh = (20 + rnd.nextFloat() * maxH) * u
            drawRect(color, Offset(x, base - bh), Size(bw - 3 * u, bh))
            x += bw
        }
    }

    private fun DrawScope.train(w: Float, top: Float, base: Float, u: Float) {
        val h = base - top - 4 * u
        val carW = 200 * u
        var x = -30 * u
        while (x < w) {
            drawRoundRect(Color(0xFF78909C), Offset(x, top + 4 * u), Size(carW, h), CornerRadius(14 * u))
            drawRoundRect(Brush.verticalGradient(listOf(Color.White, Color(0xFFE3E8EE), Color(0xFFCFD8DC)), top, top + h), Offset(x, top), Size(carW, h), CornerRadius(14 * u))
            // windows
            var wx = x + 12 * u
            while (wx < x + carW - 30 * u) {
                drawRoundRect(Color(0xFF37475E), Offset(wx, top + 10 * u), Size(26 * u, 18 * u), CornerRadius(4 * u))
                drawRoundRect(Brush.linearGradient(listOf(Color(0x66FFFFFF), Color.Transparent), Offset(wx, top + 10 * u), Offset(wx + 20 * u, top + 28 * u)), Offset(wx, top + 10 * u), Size(26 * u, 18 * u), CornerRadius(4 * u))
                wx += 34 * u
            }
            // door
            val dx = x + carW - 46 * u
            drawRoundRect(Color(0xFFB0BEC5), Offset(dx, top + 6 * u), Size(24 * u, h - 8 * u), CornerRadius(3 * u))
            drawRoundRect(Color(0xFF8FB4D9), Offset(dx + 3 * u, top + 10 * u), Size(18 * u, 16 * u), CornerRadius(3 * u))
            drawLine(Color(0xFF90A4AE), Offset(dx + 12 * u, top + 6 * u), Offset(dx + 12 * u, top + h - 2 * u), 1f * u)
            drawRect(Color(0xFFFF8F3A), Offset(x + 4 * u, top + h - 22 * u), Size(carW - 8 * u, 5 * u))
            drawRect(Color(0xFF37474F), Offset(x + 4 * u, top + h - 17 * u), Size(carW - 8 * u, 3 * u))
            x += carW + 6 * u
        }
    }

    private fun DrawScope.cloud(c: Offset, u: Float, color: Color = Color.White) {
        drawOval(Color.Black.copy(alpha = 0.04f), c + Offset(-28 * u, 10 * u), Size(60 * u, 10 * u))
        drawCircle(color.copy(alpha = 0.95f), 14 * u, c)
        drawCircle(color.copy(alpha = 0.95f), 11 * u, c + Offset(-16 * u, 4 * u))
        drawCircle(color.copy(alpha = 0.95f), 12 * u, c + Offset(16 * u, 3 * u))
        drawRoundRect(color.copy(alpha = 0.95f), c + Offset(-26 * u, 2 * u), Size(52 * u, 12 * u), CornerRadius(6 * u))
        drawCircle(Color.White.copy(alpha = 0.6f), 6 * u, c + Offset(-4 * u, -5 * u))
    }

    private fun DrawScope.hills(w: Float, base: Float, u: Float, light: Color, dark: Color, height: Float) {
        val hh = height * u
        val p = Path().apply {
            moveTo(0f, base)
            lineTo(0f, base - hh * 0.5f)
            cubicTo(w * 0.2f, base - hh, w * 0.35f, base - hh * 0.2f, w * 0.55f, base - hh * 0.6f)
            cubicTo(w * 0.75f, base - hh, w * 0.9f, base - hh * 0.3f, w, base - hh * 0.65f)
            lineTo(w, base); close()
        }
        drawPath(p, Brush.verticalGradient(listOf(light, dark), base - hh, base))
    }

    private fun DrawScope.tree(c: Offset, u: Float, color: Color) {
        drawOval(Color.Black.copy(alpha = 0.12f), Offset(c.x - 10 * u, c.y + 9 * u), Size(20 * u, 5 * u))
        drawRoundRect(Color(0xFF6D4C41), Offset(c.x - 3 * u, c.y - 6 * u), Size(6 * u, 18 * u), CornerRadius(2 * u))
        drawCircle(color.shade(0.75f), 17 * u, c + Offset(2 * u, -12 * u))
        drawCircle(color, 14 * u, c + Offset(-3 * u, -16 * u))
        drawCircle(color.tint(0.12f), 9 * u, c + Offset(-6 * u, -20 * u))
        drawCircle(color.tint(0.35f), 4 * u, c + Offset(-9 * u, -24 * u))
    }

    private fun DrawScope.bush(c: Offset, s: Float, color: Color) {
        drawOval(Color.Black.copy(alpha = 0.12f), Offset(c.x - s, c.y + s * 0.3f), Size(s * 2, s * 0.5f))
        drawCircle(color.shade(0.78f), s * 0.7f, c + Offset(s * 0.35f, 0f))
        drawCircle(color, s * 0.75f, c + Offset(-s * 0.3f, -s * 0.1f))
        drawCircle(color.tint(0.15f), s * 0.5f, c + Offset(0f, -s * 0.35f))
        drawCircle(color.tint(0.4f), s * 0.15f, c + Offset(-s * 0.2f, -s * 0.55f))
    }

    private fun DrawScope.pine(c: Offset, u: Float) {
        drawRect(Color(0xFF6D4C41), Offset(c.x - 2.5f * u, c.y - 8 * u), Size(5 * u, 8 * u))
        for (k in 0 until 3) {
            val y0 = c.y - 6 * u - k * 12 * u
            val wdt = (26 - k * 6) * u
            drawPath(Path().apply { moveTo(c.x, y0 - 20 * u); lineTo(c.x + wdt / 2, y0); lineTo(c.x - wdt / 2, y0); close() }, Color(0xFF2E7D32))
            drawPath(Path().apply { moveTo(c.x, y0 - 20 * u); lineTo(c.x + wdt / 2, y0); lineTo(c.x, y0); close() }, Color(0xFF1B5E20))
            drawPath(Path().apply { moveTo(c.x, y0 - 20 * u); lineTo(c.x + wdt / 4, y0 - 10 * u); lineTo(c.x - wdt / 4, y0 - 9 * u); close() }, Color.White)
        }
    }

    private fun DrawScope.palm(c: Offset, u: Float) {
        val top = c + Offset(8 * u, -66 * u)
        val trunk = Path().apply { moveTo(c.x, c.y); quadraticTo(c.x + 2 * u, c.y - 40 * u, top.x, top.y) }
        drawPath(trunk, Color(0xFF6D4C41), style = Stroke(7 * u, cap = StrokeCap.Round))
        drawPath(trunk, Color(0xFF8D6E63), style = Stroke(3 * u, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(3 * u, 3 * u))))
        for (k in 0 until 7) {
            rotate(k * 52f - 20f, top) {
                drawOval(Color(0xFF2E7D32), top + Offset(0f, -5 * u), Size(34 * u, 11 * u))
                drawOval(Color(0xFF43A047), top + Offset(2 * u, -4 * u), Size(28 * u, 6 * u))
            }
        }
        drawCircle(Color(0xFF5D4037), 4 * u, top + Offset(-3 * u, 5 * u))
        drawCircle(Color(0xFF5D4037), 4 * u, top + Offset(4 * u, 6 * u))
    }

    private fun DrawScope.leaf(c: Offset, u: Float, flip: Boolean) {
        rotate(if (flip) -30f else 30f, c) {
            drawOval(Color(0xFF1B5E20), c - Offset(18 * u, 10 * u), Size(36 * u, 14 * u))
            drawLine(Color(0xFF66BB6A), c - Offset(16 * u, 3 * u), c + Offset(16 * u, -3 * u), 1.2f * u)
        }
    }

    private fun DrawScope.cactus(c: Offset, u: Float) {
        val g = Color(0xFF558B2F)
        val l = Color(0xFF7CB342)
        drawRoundRect(g, Offset(c.x - 6 * u, c.y - 44 * u), Size(12 * u, 44 * u), CornerRadius(6 * u))
        drawRoundRect(l, Offset(c.x - 3 * u, c.y - 42 * u), Size(3 * u, 40 * u), CornerRadius(2 * u))
        drawRoundRect(g, Offset(c.x - 18 * u, c.y - 32 * u), Size(9 * u, 20 * u), CornerRadius(5 * u))
        drawRect(g, Offset(c.x - 13 * u, c.y - 17 * u), Size(9 * u, 6 * u))
        drawRoundRect(g, Offset(c.x + 9 * u, c.y - 36 * u), Size(9 * u, 18 * u), CornerRadius(5 * u))
        drawRect(g, Offset(c.x + 4 * u, c.y - 23 * u), Size(9 * u, 6 * u))
        drawCircle(Color(0xFFFF4081), 3 * u, Offset(c.x, c.y - 45 * u))
    }

    private fun DrawScope.hay(c: Offset, u: Float) {
        drawOval(Color.Black.copy(alpha = 0.12f), Offset(c.x - 18 * u, c.y - 3 * u), Size(36 * u, 6 * u))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300)), c.y - 22 * u, c.y), Offset(c.x - 17 * u, c.y - 22 * u), Size(34 * u, 22 * u), CornerRadius(6 * u))
        drawLine(Color(0xFFE69500), Offset(c.x - 17 * u, c.y - 11 * u), Offset(c.x + 17 * u, c.y - 11 * u), 1.5f * u)
    }

    private fun DrawScope.tent(c: Offset, s: Float, color: Color, u: Float) {
        val p = Path().apply { moveTo(c.x, c.y - s * 1.1f); lineTo(c.x + s * 0.75f, c.y); lineTo(c.x - s * 0.75f, c.y); close() }
        drawPath(p, color)
        drawPath(Path().apply { moveTo(c.x, c.y - s * 1.1f); lineTo(c.x + s * 0.75f, c.y); lineTo(c.x + s * 0.2f, c.y); close() }, color.shade(0.8f))
        drawPath(Path().apply { moveTo(c.x, c.y - s * 0.6f); lineTo(c.x + s * 0.2f, c.y); lineTo(c.x - s * 0.2f, c.y); close() }, Color(0xFF4E342E))
        drawLine(color.shade(0.5f), Offset(c.x, c.y - s * 1.1f), Offset(c.x, c.y - s * 1.3f), 2 * u)
    }

    private fun DrawScope.balloon(c: Offset, u: Float, color: Color) {
        drawLine(Color(0x99FFFFFF), c + Offset(0f, 9 * u), c + Offset(0f, 30 * u), 0.8f * u)
        drawOval(Brush.radialGradient(listOf(color.tint(0.4f), color), c - Offset(3 * u, 4 * u), 12 * u), c - Offset(7 * u, 9 * u), Size(14 * u, 18 * u))
        drawCircle(Color.White.copy(alpha = 0.6f), 2.2f * u, c - Offset(3 * u, 4 * u))
    }

    private fun DrawScope.fence(w: Float, y: Float, u: Float, color: Color) {
        drawRect(color.shade(0.82f), Offset(0f, y - 14 * u), Size(w, 3 * u))
        drawRect(color.shade(0.82f), Offset(0f, y - 6 * u), Size(w, 3 * u))
        var x = 4 * u
        while (x < w) {
            drawRoundRect(color, Offset(x, y - 20 * u), Size(5 * u, 20 * u), CornerRadius(2 * u))
            drawRect(color.tint(0.25f), Offset(x + 1 * u, y - 19 * u), Size(1.4f * u, 18 * u))
            x += 14 * u
        }
    }

    private fun DrawScope.bench(c: Offset, u: Float, color: Color) {
        drawOval(Color.Black.copy(alpha = 0.15f), Offset(c.x - 4 * u, c.y + 1 * u), Size(60 * u, 5 * u))
        drawRoundRect(color.shade(0.7f), Offset(c.x, c.y - 16 * u), Size(52 * u, 8 * u), CornerRadius(2 * u))
        drawRoundRect(color, Offset(c.x, c.y - 9 * u), Size(52 * u, 6 * u), CornerRadius(2 * u))
        drawRect(Color(0xFF455A64), Offset(c.x + 4 * u, c.y - 3 * u), Size(3 * u, 4 * u))
        drawRect(Color(0xFF455A64), Offset(c.x + 45 * u, c.y - 3 * u), Size(3 * u, 4 * u))
    }

    private fun DrawScope.plantPot(c: Offset, u: Float) {
        drawOval(Color.Black.copy(alpha = 0.15f), Offset(c.x - 9 * u, c.y - 1 * u), Size(18 * u, 4 * u))
        drawPath(Path().apply { moveTo(c.x - 7 * u, c.y - 12 * u); lineTo(c.x + 7 * u, c.y - 12 * u); lineTo(c.x + 5 * u, c.y); lineTo(c.x - 5 * u, c.y); close() }, Color(0xFFD84315))
        bush(Offset(c.x, c.y - 16 * u), 9 * u, Color(0xFF43A047))
    }

    private fun DrawScope.lampPost(c: Offset, u: Float, th: SceneTheme) {
        drawOval(Color.Black.copy(alpha = 0.15f), Offset(c.x - 9 * u, c.y - 3 * u), Size(18 * u, 6 * u))
        drawRect(Color(0xFF37474F), Offset(c.x - 2 * u, c.y - 78 * u), Size(4 * u, 78 * u))
        drawRect(Color(0xFF546E7A), Offset(c.x - 1 * u, c.y - 78 * u), Size(1 * u, 78 * u))
        drawRoundRect(Color(0xFF37474F), Offset(c.x - 8 * u, c.y - 95 * u), Size(16 * u, 20 * u), CornerRadius(3 * u))
        val glow = if (th.dark) Color(0xFFFFF59D) else Color(0xFFE3F2FD)
        drawRoundRect(glow, Offset(c.x - 5.5f * u, c.y - 92 * u), Size(11 * u, 14 * u), CornerRadius(2 * u))
        if (th.dark) drawCircle(Brush.radialGradient(listOf(Color(0x66FFF59D), Color.Transparent), Offset(c.x, c.y - 85 * u), 30 * u), 30 * u, Offset(c.x, c.y - 85 * u))
    }

    private fun DrawScope.crown(c: Offset, s: Float) {
        val p = Path().apply {
            moveTo(c.x - s, c.y + s * 0.6f); lineTo(c.x - s, c.y - s * 0.4f); lineTo(c.x - s * 0.45f, c.y + s * 0.05f)
            lineTo(c.x, c.y - s * 0.7f); lineTo(c.x + s * 0.45f, c.y + s * 0.05f); lineTo(c.x + s, c.y - s * 0.4f)
            lineTo(c.x + s, c.y + s * 0.6f); close()
        }
        drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFC107)), c.y - s, c.y + s))
        drawPath(p, Color(0xFFE65100), style = Stroke(s * 0.12f, join = StrokeJoin.Round))
    }

    private fun DrawScope.giraffe(c: Offset, u: Float) {
        val y = Color(0xFFFFC94D)
        val b = Color(0xFFB0702A)
        drawPath(Path().apply { moveTo(c.x - 4 * u, c.y - 20 * u); lineTo(c.x + 4 * u, c.y - 20 * u); lineTo(c.x + 10 * u, c.y - 70 * u); lineTo(c.x + 2 * u, c.y - 72 * u); close() }, y)
        drawRoundRect(y, Offset(c.x - 4 * u, c.y - 80 * u), Size(20 * u, 11 * u), CornerRadius(5 * u))
        drawLine(b, Offset(c.x + 3 * u, c.y - 80 * u), Offset(c.x + 2 * u, c.y - 86 * u), 1.6f * u, StrokeCap.Round)
        drawLine(b, Offset(c.x + 8 * u, c.y - 80 * u), Offset(c.x + 8 * u, c.y - 86 * u), 1.6f * u, StrokeCap.Round)
        drawCircle(Color(0xFF3E2723), 1.4f * u, Offset(c.x + 6 * u, c.y - 76 * u))
        for (k in 0 until 4) drawCircle(b, 2 * u, Offset(c.x + 2 * u + k * 1.8f * u, c.y - 30 * u - k * 10 * u))
    }

    private fun DrawScope.elephant(c: Offset, u: Float) {
        val g = Color(0xFF90A4AE)
        drawOval(g, Offset(c.x - 22 * u, c.y - 30 * u), Size(40 * u, 26 * u))
        drawCircle(g, 12 * u, Offset(c.x - 22 * u, c.y - 32 * u))
        drawOval(g.shade(0.85f), Offset(c.x - 24 * u, c.y - 40 * u), Size(14 * u, 18 * u))
        drawPath(Path().apply { moveTo(c.x - 32 * u, c.y - 30 * u); quadraticTo(c.x - 40 * u, c.y - 18 * u, c.x - 34 * u, c.y - 8 * u) }, g, style = Stroke(5 * u, cap = StrokeCap.Round))
        drawCircle(Color(0xFF263238), 1.4f * u, Offset(c.x - 26 * u, c.y - 35 * u))
        drawCircle(Color.White.copy(alpha = 0.4f), 4 * u, Offset(c.x - 4 * u, c.y - 26 * u))
    }

    private fun DrawScope.carousel(c: Offset, u: Float) {
        drawRoundRect(Color(0xFFCE93D8), Offset(c.x - 40 * u, c.y - 10 * u), Size(80 * u, 10 * u), CornerRadius(3 * u))
        for (k in 0 until 5) drawLine(Color(0xFFFFD54F), Offset(c.x - 32 * u + k * 16 * u, c.y - 10 * u), Offset(c.x - 32 * u + k * 16 * u, c.y - 46 * u), 1.6f * u)
        for (k in 0 until 4) drawRoundRect(listOf(Color.White, Color(0xFFFFCDD2), Color(0xFFBBDEFB), Color(0xFFFFF9C4))[k], Offset(c.x - 30 * u + k * 16 * u, c.y - 26 * u + (k % 2) * 5 * u), Size(10 * u, 7 * u), CornerRadius(3 * u))
        val roof = Path().apply { moveTo(c.x, c.y - 74 * u); lineTo(c.x + 46 * u, c.y - 46 * u); lineTo(c.x - 46 * u, c.y - 46 * u); close() }
        drawPath(roof, Color(0xFFEC407A))
        for (k in 0 until 4) {
            val p = Path().apply { moveTo(c.x, c.y - 74 * u); lineTo(c.x - 46 * u + k * 23 * u + 11 * u, c.y - 46 * u); lineTo(c.x - 46 * u + k * 23 * u, c.y - 46 * u); close() }
            drawPath(p, Color.White)
        }
        for (k in 0 until 8) drawCircle(Color(0xFFFFEB3B), 2 * u, Offset(c.x - 42 * u + k * 12 * u, c.y - 46 * u))
        drawCircle(Color(0xFFFFD54F), 4 * u, Offset(c.x, c.y - 76 * u))
    }

    private fun DrawScope.stand(c: Offset, u: Float) {
        drawRoundRect(Color(0xFFFFF3E0), Offset(c.x - 20 * u, c.y - 26 * u), Size(40 * u, 26 * u), CornerRadius(3 * u))
        for (k in 0 until 5) drawRect(if (k % 2 == 0) Color(0xFFFF4081) else Color.White, Offset(c.x - 24 * u + k * 9.6f * u, c.y - 38 * u), Size(9.6f * u, 12 * u))
        drawCircle(Color(0xFFF8BBD0), 7 * u, Offset(c.x + 8 * u, c.y - 46 * u))
        drawLine(Color(0xFFFFF3E0), Offset(c.x + 8 * u, c.y - 40 * u), Offset(c.x + 8 * u, c.y - 34 * u), 1.4f * u)
    }

    private fun DrawScope.lighthouse(c: Offset, u: Float) {
        val p = Path().apply { moveTo(c.x - 14 * u, c.y); lineTo(c.x - 9 * u, c.y - 82 * u); lineTo(c.x + 9 * u, c.y - 82 * u); lineTo(c.x + 14 * u, c.y); close() }
        drawPath(p, Brush.horizontalGradient(listOf(Color.White, Color(0xFFE0E0E0)), c.x - 14 * u, c.x + 14 * u))
        for (k in 0 until 3) drawRect(Color(0xFFE53935), Offset(c.x - (13 - k * 1.5f) * u, c.y - (20 + k * 22) * u), Size((26 - k * 3) * u, 9 * u))
        drawRoundRect(Color(0xFF37474F), Offset(c.x - 11 * u, c.y - 92 * u), Size(22 * u, 10 * u), CornerRadius(2 * u))
        drawRect(Color(0xFFFFEE58), Offset(c.x - 7 * u, c.y - 91 * u), Size(14 * u, 8 * u))
        drawPath(Path().apply { moveTo(c.x - 12 * u, c.y - 92 * u); lineTo(c.x, c.y - 102 * u); lineTo(c.x + 12 * u, c.y - 92 * u); close() }, Color(0xFFC62828))
    }

    private fun DrawScope.sailboat(c: Offset, u: Float) {
        drawPath(Path().apply { moveTo(c.x - 22 * u, c.y); lineTo(c.x + 22 * u, c.y); lineTo(c.x + 15 * u, c.y + 9 * u); lineTo(c.x - 15 * u, c.y + 9 * u); close() }, Color(0xFFFF7043))
        drawLine(Color(0xFF5D4037), Offset(c.x, c.y), Offset(c.x, c.y - 34 * u), 1.6f * u)
        drawPath(Path().apply { moveTo(c.x + 1 * u, c.y - 33 * u); lineTo(c.x + 18 * u, c.y - 3 * u); lineTo(c.x + 1 * u, c.y - 3 * u); close() }, Color.White)
        drawPath(Path().apply { moveTo(c.x - 1 * u, c.y - 26 * u); lineTo(c.x - 13 * u, c.y - 3 * u); lineTo(c.x - 1 * u, c.y - 3 * u); close() }, Color(0xFFFFF59D))
    }

    private fun DrawScope.pyramid(c: Offset, s: Float) {
        drawPath(Path().apply { moveTo(c.x, c.y - s); lineTo(c.x + s, c.y); lineTo(c.x - s, c.y); close() }, Color(0xFFE6BB73))
        drawPath(Path().apply { moveTo(c.x, c.y - s); lineTo(c.x + s, c.y); lineTo(c.x + s * 0.15f, c.y); close() }, Color(0xFFC99A52))
        for (k in 1..4) {
            val y = c.y - s * k / 5f
            val hw = s * (1 - k / 5f)
            drawLine(Color(0xFFB98A45).copy(alpha = 0.5f), Offset(c.x - hw, y), Offset(c.x + hw, y), 1f)
        }
    }

    private fun DrawScope.camel(c: Offset, u: Float) {
        val col = Color(0xFFC68B4E)
        drawOval(col, Offset(c.x - 20 * u, c.y - 30 * u), Size(36 * u, 16 * u))
        drawCircle(col, 8 * u, Offset(c.x - 4 * u, c.y - 30 * u))
        drawPath(Path().apply { moveTo(c.x + 14 * u, c.y - 24 * u); quadraticTo(c.x + 24 * u, c.y - 30 * u, c.x + 22 * u, c.y - 42 * u) }, col, style = Stroke(5 * u, cap = StrokeCap.Round))
        drawOval(col, Offset(c.x + 18 * u, c.y - 46 * u), Size(12 * u, 7 * u))
        for (dx in listOf(-16f, -8f, 4f, 12f)) drawLine(col.shade(0.85f), Offset(c.x + dx * u, c.y - 16 * u), Offset(c.x + dx * u, c.y), 2.5f * u)
    }

    private fun DrawScope.mountain(c: Offset, hgt: Float, u: Float) {
        drawPath(Path().apply { moveTo(c.x - hgt, c.y); lineTo(c.x, c.y - hgt); lineTo(c.x + hgt, c.y); close() }, Color(0xFF90A4AE))
        drawPath(Path().apply { moveTo(c.x, c.y - hgt); lineTo(c.x + hgt, c.y); lineTo(c.x + hgt * 0.2f, c.y); close() }, Color(0xFF78909C))
        val cap = Path().apply {
            moveTo(c.x - hgt * 0.3f, c.y - hgt * 0.7f); lineTo(c.x, c.y - hgt); lineTo(c.x + hgt * 0.3f, c.y - hgt * 0.7f)
            lineTo(c.x + hgt * 0.12f, c.y - hgt * 0.76f); lineTo(c.x - 2 * u, c.y - hgt * 0.68f); lineTo(c.x - hgt * 0.14f, c.y - hgt * 0.75f); close()
        }
        drawPath(cap, Color.White)
    }

    private fun DrawScope.cabin(c: Offset, u: Float) {
        drawRect(Color(0xFF8D5A3B), Offset(c.x - 22 * u, c.y - 26 * u), Size(44 * u, 26 * u))
        for (k in 0 until 4) drawLine(Color(0xFF6D4027), Offset(c.x - 22 * u, c.y - 26 * u + k * 6.5f * u), Offset(c.x + 22 * u, c.y - 26 * u + k * 6.5f * u), 1f * u)
        drawPath(Path().apply { moveTo(c.x - 28 * u, c.y - 24 * u); lineTo(c.x, c.y - 44 * u); lineTo(c.x + 28 * u, c.y - 24 * u); close() }, Color.White)
        drawRect(Color(0xFF6D4027), Offset(c.x + 10 * u, c.y - 48 * u), Size(6 * u, 12 * u))
        drawRect(Color(0xFFFFE082), Offset(c.x - 14 * u, c.y - 18 * u), Size(10 * u, 9 * u))
    }

    private fun DrawScope.snowman(c: Offset, u: Float) {
        drawOval(Color.Black.copy(alpha = 0.12f), Offset(c.x - 12 * u, c.y - 2 * u), Size(24 * u, 5 * u))
        drawCircle(Color.White, 11 * u, Offset(c.x, c.y - 10 * u))
        drawCircle(Color.White, 8 * u, Offset(c.x, c.y - 26 * u))
        drawCircle(Color(0xFFE3F2FD), 8 * u, Offset(c.x + 2 * u, c.y - 26 * u), style = Stroke(1f * u))
        drawCircle(Color(0xFF263238), 1.2f * u, Offset(c.x - 3 * u, c.y - 28 * u))
        drawCircle(Color(0xFF263238), 1.2f * u, Offset(c.x + 3 * u, c.y - 28 * u))
        drawPath(Path().apply { moveTo(c.x, c.y - 26 * u); lineTo(c.x + 7 * u, c.y - 25 * u); lineTo(c.x, c.y - 24 * u); close() }, Color(0xFFFF7043))
        drawRect(Color(0xFFE53935), Offset(c.x - 8 * u, c.y - 20 * u), Size(16 * u, 3 * u))
    }

    private fun DrawScope.barn(c: Offset, u: Float) {
        drawRect(Brush.horizontalGradient(listOf(Color(0xFFD32F2F), Color(0xFFB71C1C)), c.x - 40 * u, c.x + 40 * u), Offset(c.x - 40 * u, c.y - 56 * u), Size(80 * u, 56 * u))
        for (k in 0 until 6) drawLine(Color(0xFF9A1B1B), Offset(c.x - 40 * u + k * 16 * u, c.y - 56 * u), Offset(c.x - 40 * u + k * 16 * u, c.y), 1f * u)
        val roof = Path().apply { moveTo(c.x - 48 * u, c.y - 54 * u); lineTo(c.x - 24 * u, c.y - 80 * u); lineTo(c.x + 24 * u, c.y - 80 * u); lineTo(c.x + 48 * u, c.y - 54 * u); close() }
        drawPath(roof, Color(0xFF5D4037))
        drawPath(roof, Color(0xFF3E2723), style = Stroke(2 * u))
        drawRect(Color.White, Offset(c.x - 14 * u, c.y - 36 * u), Size(28 * u, 36 * u), style = Stroke(2.5f * u))
        drawLine(Color.White, Offset(c.x - 14 * u, c.y - 36 * u), Offset(c.x + 14 * u, c.y), 2.5f * u)
        drawLine(Color.White, Offset(c.x + 14 * u, c.y - 36 * u), Offset(c.x - 14 * u, c.y), 2.5f * u)
        drawCircle(Color(0xFFFFF8E1), 6 * u, Offset(c.x, c.y - 64 * u))
    }

    private fun DrawScope.neon(tl: Offset, s: Size, color: Color, u: Float) {
        drawRoundRect(Color(0xFF120D30), tl, s, CornerRadius(4 * u))
        for (k in 3 downTo 1) drawRoundRect(color.copy(alpha = 0.12f * k), tl - Offset(k * 1.5f * u, k * 1.5f * u), Size(s.width + k * 3 * u, s.height + k * 3 * u), CornerRadius(6 * u), style = Stroke(2 * u))
        drawRoundRect(color, tl, s, CornerRadius(4 * u), style = Stroke(1.6f * u))
        drawLine(color, tl + Offset(s.width * 0.2f, s.height * 0.5f), tl + Offset(s.width * 0.8f, s.height * 0.5f), 2 * u, StrokeCap.Round)
    }

    private fun DrawScope.dome(c: Offset, r: Float) {
        drawArc(Brush.radialGradient(listOf(Color(0xFFE0F7FA), Color(0xFF4DD0E1), Color(0xFF00838F)), c - Offset(r * 0.3f, r * 0.6f), r * 1.4f), 180f, 180f, true, Offset(c.x - r, c.y - r), Size(r * 2, r * 2))
        drawArc(Color.White.copy(alpha = 0.5f), 200f, 60f, false, Offset(c.x - r * 0.8f, c.y - r * 0.8f), Size(r * 1.6f, r * 1.6f), style = Stroke(r * 0.08f))
        drawRect(Color(0xFF455A64), Offset(c.x - r, c.y - r * 0.08f), Size(r * 2, r * 0.08f))
    }

    private fun DrawScope.dish(c: Offset, u: Float) {
        drawLine(Color(0xFF90A4AE), c, c - Offset(0f, 26 * u), 3 * u)
        drawArc(Color(0xFFECEFF1), 20f, 180f, true, Offset(c.x - 18 * u, c.y - 46 * u), Size(36 * u, 26 * u))
        drawLine(Color(0xFF90A4AE), c - Offset(-2 * u, 34 * u), c - Offset(-14 * u, 46 * u), 1.5f * u)
        drawCircle(Color(0xFFFF5252), 2.5f * u, c - Offset(-14 * u, 47 * u))
    }

    private fun DrawScope.rocket(c: Offset, u: Float) {
        drawRect(Color(0xFF546E7A), Offset(c.x - 26 * u, c.y - 104 * u), Size(5 * u, 104 * u))
        for (k in 0 until 6) drawLine(Color(0xFF78909C), Offset(c.x - 26 * u, c.y - k * 18 * u), Offset(c.x - 21 * u, c.y - k * 18 * u - 9 * u), 1.4f * u)
        val body = Path().apply {
            moveTo(c.x, c.y - 112 * u); quadraticTo(c.x + 16 * u, c.y - 88 * u, c.x + 14 * u, c.y - 26 * u)
            lineTo(c.x - 14 * u, c.y - 26 * u); quadraticTo(c.x - 16 * u, c.y - 88 * u, c.x, c.y - 112 * u); close()
        }
        drawPath(body, Brush.horizontalGradient(listOf(Color.White, Color(0xFFCFD8DC)), c.x - 14 * u, c.x + 14 * u))
        drawCircle(Color(0xFF29B6F6), 6 * u, Offset(c.x, c.y - 74 * u))
        drawCircle(Color.White.copy(alpha = 0.6f), 2 * u, Offset(c.x - 2 * u, c.y - 76 * u))
        drawPath(Path().apply { moveTo(c.x - 14 * u, c.y - 48 * u); lineTo(c.x - 26 * u, c.y - 18 * u); lineTo(c.x - 12 * u, c.y - 26 * u); close() }, Color(0xFFE53935))
        drawPath(Path().apply { moveTo(c.x + 14 * u, c.y - 48 * u); lineTo(c.x + 26 * u, c.y - 18 * u); lineTo(c.x + 12 * u, c.y - 26 * u); close() }, Color(0xFFE53935))
        drawRect(Color(0xFF37474F), Offset(c.x - 36 * u, c.y - 18 * u), Size(72 * u, 14 * u))
    }

    fun DrawScope.text(s: String, c: Offset, size: Float, color: Color) {
        drawContext.canvas.nativeCanvas.drawText(s, c.x, c.y + size * 0.35f, android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.argb((color.alpha * 255).toInt(), (color.red * 255).toInt(), (color.green * 255).toInt(), (color.blue * 255).toInt())
            textSize = size
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            textAlign = android.graphics.Paint.Align.CENTER
        })
    }
}
