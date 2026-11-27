package com.ngstudio.trafficjam.ui.main

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.ngstudio.trafficjam.game.render.shade
import com.ngstudio.trafficjam.game.render.tint

/** The chunky cartoon badges that sit on top of each Modes card and beside each title. */
enum class Emblem { CROWN, CAR, CROWD, HEART, SAND }

/** Draws [e] centred on [c], sized so it fits in a box of about 2*[s] across, with a white rim. */
fun DrawScope.drawEmblem(e: Emblem, c: Offset, s: Float) {
    when (e) {
        Emblem.CROWN -> crown(c, s)
        Emblem.CAR -> car(c, s)
        Emblem.CROWD -> crowd(c, s)
        Emblem.HEART -> heart(c, s)
        Emblem.SAND -> sand(c, s)
    }
}

/** Fill [path], then a white rim and a darker toon edge around it. */
private fun DrawScope.toon(path: Path, fill: Brush, edge: Color, rim: Float, edgeW: Float) {
    drawPath(path, Color.White, style = Stroke(rim, join = StrokeJoin.Round, cap = StrokeCap.Round))
    drawPath(path, fill)
    drawPath(path, edge, style = Stroke(edgeW, join = StrokeJoin.Round))
}

private fun DrawScope.crown(c: Offset, s: Float) {
    val gold = Color(0xFFFFC21A)
    val bottom = c.y + s * 0.7f
    val band = c.y + s * 0.2f
    val dip = c.y + s * 0.05f
    val left = c.x - s
    val right = c.x + s
    val p = Path().apply {
        moveTo(left, bottom)
        lineTo(left, band)
        lineTo(c.x - s * 0.52f, c.y - s * 0.5f)
        lineTo(c.x - s * 0.26f, dip)
        lineTo(c.x, c.y - s * 0.85f)
        lineTo(c.x + s * 0.26f, dip)
        lineTo(c.x + s * 0.52f, c.y - s * 0.5f)
        lineTo(right, band)
        lineTo(right, bottom)
        close()
    }
    toon(p, Brush.verticalGradient(listOf(Color(0xFFFFE36E), gold), c.y - s, bottom), Color(0xFFB26A00), s * 0.42f, s * 0.12f)
    // gems on the three peaks
    for (gx in floatArrayOf(c.x - s * 0.52f, c.x, c.x + s * 0.52f)) {
        drawCircle(Color.White, s * 0.18f, Offset(gx, if (gx == c.x) c.y - s * 0.85f else c.y - s * 0.5f))
        drawCircle(Color(0xFFE5392F), s * 0.11f, Offset(gx, if (gx == c.x) c.y - s * 0.85f else c.y - s * 0.5f))
    }
    drawCircle(Color(0xFFB26A00), s * 0.1f, Offset(c.x, band + s * 0.18f))
}

private fun DrawScope.car(c: Offset, s: Float) {
    val red = Color(0xFFE5392F)
    val body = Path().apply {
        moveTo(c.x - s, c.y + s * 0.25f)
        lineTo(c.x - s, c.y)
        lineTo(c.x - s * 0.55f, c.y)
        lineTo(c.x - s * 0.28f, c.y - s * 0.5f)
        lineTo(c.x + s * 0.4f, c.y - s * 0.5f)
        lineTo(c.x + s * 0.72f, c.y)
        lineTo(c.x + s, c.y + s * 0.08f)
        lineTo(c.x + s, c.y + s * 0.25f)
        close()
    }
    toon(body, Brush.verticalGradient(listOf(red.tint(0.25f), red), c.y - s * 0.5f, c.y + s * 0.3f), red.shade(0.6f), s * 0.34f, s * 0.1f)
    // window
    val win = Path().apply {
        moveTo(c.x - s * 0.4f, c.y - s * 0.08f)
        lineTo(c.x - s * 0.22f, c.y - s * 0.42f)
        lineTo(c.x + s * 0.28f, c.y - s * 0.42f)
        lineTo(c.x + s * 0.5f, c.y - s * 0.08f)
        close()
    }
    drawPath(win, Color(0xFFBFE6FF))
    // wheels
    for (wx in floatArrayOf(c.x - s * 0.5f, c.x + s * 0.5f)) {
        drawCircle(Color.White, s * 0.3f, Offset(wx, c.y + s * 0.3f))
        drawCircle(Color(0xFF2B2B33), s * 0.22f, Offset(wx, c.y + s * 0.3f))
        drawCircle(Color(0xFFBFC6D2), s * 0.09f, Offset(wx, c.y + s * 0.3f))
    }
}

private fun DrawScope.crowd(c: Offset, s: Float) {
    val blue = Color(0xFF2F7CF6)
    fun person(px: Float, py: Float, r: Float, col: Color) {
        val body = Path().apply {
            moveTo(px - r, py + r * 1.1f)
            cubicTo(px - r, py + r * 0.1f, px - r * 0.8f, py - r * 0.2f, px, py - r * 0.2f)
            cubicTo(px + r * 0.8f, py - r * 0.2f, px + r, py + r * 0.1f, px + r, py + r * 1.1f)
            close()
        }
        drawPath(body, Color.White, style = Stroke(r * 0.5f, join = StrokeJoin.Round))
        drawCircle(Color.White, r * 0.75f, Offset(px, py - r * 0.55f))
        drawPath(body, col)
        drawCircle(col, r * 0.52f, Offset(px, py - r * 0.55f))
    }
    person(c.x - s * 0.55f, c.y + s * 0.1f, s * 0.5f, blue.shade(0.8f))
    person(c.x + s * 0.55f, c.y + s * 0.1f, s * 0.5f, blue.shade(0.8f))
    person(c.x, c.y - s * 0.05f, s * 0.6f, blue)
}

private fun DrawScope.heart(c: Offset, s: Float) {
    val pink = Color(0xFFF53D9A)
    val p = Path().apply {
        moveTo(c.x, c.y + s * 0.85f)
        cubicTo(c.x - s * 1.35f, c.y - s * 0.15f, c.x - s * 0.55f, c.y - s * 0.95f, c.x, c.y - s * 0.3f)
        cubicTo(c.x + s * 0.55f, c.y - s * 0.95f, c.x + s * 1.35f, c.y - s * 0.15f, c.x, c.y + s * 0.85f)
        close()
    }
    toon(p, Brush.verticalGradient(listOf(pink.tint(0.3f), pink), c.y - s, c.y + s), pink.shade(0.65f), s * 0.42f, s * 0.12f)
    drawCircle(Color.White.copy(alpha = 0.6f), s * 0.16f, Offset(c.x - s * 0.35f, c.y - s * 0.3f))
}

private fun DrawScope.sand(c: Offset, s: Float) {
    val tan = Color(0xFFF0B429)
    // a rounded dune
    val p = Path().apply {
        moveTo(c.x - s, c.y + s * 0.6f)
        cubicTo(c.x - s * 0.7f, c.y - s * 0.9f, c.x + s * 0.2f, c.y - s * 0.5f, c.x + s * 0.45f, c.y + s * 0.1f)
        cubicTo(c.x + s * 0.6f, c.y - s * 0.35f, c.x + s * 0.95f, c.y - s * 0.1f, c.x + s, c.y + s * 0.6f)
        close()
    }
    toon(p, Brush.verticalGradient(listOf(Color(0xFFFFE08A), tan), c.y - s, c.y + s * 0.6f), tan.shade(0.7f), s * 0.38f, s * 0.1f)
    // grains
    for (g in listOf(Offset(c.x - s * 0.35f, c.y + s * 0.1f), Offset(c.x + s * 0.1f, c.y + s * 0.25f), Offset(c.x + s * 0.55f, c.y + s * 0.2f))) {
        drawCircle(tan.shade(0.75f), s * 0.08f, g)
    }
}
