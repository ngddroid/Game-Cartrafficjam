package com.puzzel.lab.cartrafficjam.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.puzzel.lab.cartrafficjam.game.render.shade
import com.puzzel.lab.cartrafficjam.game.render.tint
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import com.puzzel.lab.cartrafficjam.ui.theme.Sunset

enum class Icon { COIN, STAR, STAR_EMPTY, LOCK, HOME, MAP, COLLECTION, MODES, GARAGE, GEAR, SHUFFLE, CRANE, SORT, AD, GIFT, PLAY, TROPHY, HAND, CHECK, HEART, SAD, GAMEPAD }

/** Chunky, colourful icons drawn in a 100×100 design space. */
@Composable
fun GameIcon(icon: Icon, size: Dp, modifier: Modifier = Modifier) {
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 100f
        scale(s, s, Offset.Zero) { draw(icon) }
    }
}

/** Draws [icon] into a canvas at [topLeft], [sizePx] wide (for icons inside illustrations). */
fun DrawScope.drawGameIcon(icon: Icon, topLeft: Offset, sizePx: Float) {
    translate(topLeft.x, topLeft.y) {
        scale(sizePx / 100f, sizePx / 100f, Offset.Zero) { draw(icon) }
    }
}

private fun DrawScope.draw(icon: Icon) {
    when (icon) {
        Icon.COIN -> coin()
        Icon.STAR -> star(Color(0xFFFFD54F), Color(0xFFFF8F00))
        Icon.STAR_EMPTY -> star(Color(0xFF8D99AE), Color(0xFF5C6680))
        Icon.LOCK -> lock()
        Icon.HOME -> home()
        Icon.MAP -> map()
        Icon.COLLECTION -> collection()
        Icon.MODES -> modes()
        Icon.GARAGE -> car(Offset(50f, 56f), Color(0xFFF2453D))
        Icon.GEAR -> gear()
        Icon.SHUFFLE -> shuffle()
        Icon.CRANE -> crane()
        Icon.SORT -> sort()
        Icon.AD -> ad()
        Icon.GIFT -> gift()
        Icon.PLAY -> {
            val p = Path().apply { moveTo(30f, 18f); lineTo(84f, 50f); lineTo(30f, 82f); close() }
            drawPath(p, Color.White)
            drawPath(p, Color(0x33000000), style = Stroke(4f, join = StrokeJoin.Round))
        }
        Icon.TROPHY -> trophy()
        Icon.HAND -> hand()
        Icon.HEART -> heart()
        Icon.SAD -> sad()
        Icon.GAMEPAD -> gamepad()
        Icon.CHECK -> {
            val p = Path().apply { moveTo(18f, 52f); lineTo(42f, 76f); lineTo(84f, 26f) }
            drawPath(p, Color(0xFF1B5E20), style = Stroke(20f, cap = StrokeCap.Round, join = StrokeJoin.Round))
            drawPath(p, Color(0xFF76FF03), style = Stroke(12f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
    }
}

private fun DrawScope.coin() {
    drawCircle(Color(0xFFB26A00), 47f, Offset(50f, 53f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFC107), Color(0xFFFF8F00)), Offset(38f, 36f), 60f), 45f, Offset(50f, 50f))
    drawCircle(Color(0xFFE69500), 33f, Offset(50f, 50f), style = Stroke(4f))
    // embossed car
    val c = Color(0xFFD98300)
    drawRoundRect(c, Offset(30f, 46f), Size(40f, 14f), CornerRadius(6f))
    drawRoundRect(c, Offset(38f, 36f), Size(24f, 14f), CornerRadius(6f))
    drawRoundRect(Color(0xFFFFE082), Offset(41f, 39f), Size(18f, 8f), CornerRadius(3f))
    drawCircle(c, 6f, Offset(38f, 62f))
    drawCircle(c, 6f, Offset(62f, 62f))
    drawOval(Color.White.copy(alpha = 0.55f), Offset(24f, 16f), Size(24f, 12f))
}

fun starPath(cx: Float, cy: Float, r: Float, inner: Float = 0.48f): Path = Path().apply {
    for (i in 0 until 10) {
        val a = -PI / 2 + i * PI / 5
        val rr = if (i % 2 == 0) r else r * inner
        val x = cx + (rr * cos(a)).toFloat()
        val y = cy + (rr * sin(a)).toFloat()
        if (i == 0) moveTo(x, y) else lineTo(x, y)
    }
    close()
}

private fun DrawScope.star(fill: Color, edge: Color) {
    val p = starPath(50f, 54f, 46f)
    drawPath(starPath(50f, 58f, 46f), edge.shade(0.7f))
    drawPath(p, Brush.verticalGradient(listOf(fill.tint(0.35f), fill, edge), 10f, 95f))
    drawPath(p, edge, style = Stroke(4f, join = StrokeJoin.Round))
    drawOval(Color.White.copy(alpha = 0.5f), Offset(36f, 30f), Size(16f, 10f))
}

private fun DrawScope.lock() {
    drawArc(Color(0xFF5D6B82), 180f, 180f, false, Offset(28f, 14f), Size(44f, 48f), style = Stroke(11f))
    drawArc(Color(0xFF9BA8BF), 180f, 180f, false, Offset(28f, 14f), Size(44f, 48f), style = Stroke(5f))
    drawRoundRect(Color(0xFF8A5A00), Offset(18f, 42f), Size(64f, 50f), CornerRadius(12f))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFB300)), 40f, 90f), Offset(18f, 40f), Size(64f, 48f), CornerRadius(12f))
    drawCircle(Color(0xFF6D4C00), 7f, Offset(50f, 60f))
    drawRoundRect(Color(0xFF6D4C00), Offset(47f, 60f), Size(6f, 16f), CornerRadius(3f))
}

private fun DrawScope.home() {
    drawRoundRect(Color(0xFF8D6E63), Offset(66f, 14f), Size(10f, 22f), CornerRadius(2f))
    drawRoundRect(Color(0xFFC79A6B), Offset(20f, 44f), Size(60f, 48f), CornerRadius(6f))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFF3E0), Color(0xFFFFE0B2))), Offset(22f, 44f), Size(56f, 44f), CornerRadius(6f))
    val roof = Path().apply { moveTo(8f, 50f); lineTo(50f, 10f); lineTo(92f, 50f); close() }
    drawPath(roof, Brush.verticalGradient(listOf(Color(0xFFFF6F5E), Color(0xFFD32F2F)), 10f, 50f))
    drawPath(roof, Color(0xFF8E1B1B), style = Stroke(4f, join = StrokeJoin.Round))
    drawRoundRect(Color(0xFFFFB300), Offset(40f, 60f), Size(20f, 28f), CornerRadius(10f, 10f))
    drawCircle(Color(0xFF8D6E00), 2.5f, Offset(55f, 75f))
}

private fun DrawScope.map() {
    val panels = listOf(Color(0xFF81D4FA), Color(0xFF4FC3F7), Color(0xFF81D4FA))
    for (i in 0 until 3) {
        val x = 12f + i * 26f
        val p = Path().apply {
            moveTo(x, if (i % 2 == 0) 22f else 14f); lineTo(x + 26f, if (i % 2 == 0) 14f else 22f)
            lineTo(x + 26f, if (i % 2 == 0) 82f else 90f); lineTo(x, if (i % 2 == 0) 90f else 82f); close()
        }
        drawPath(p, panels[i])
        drawPath(p, Color(0xFF0277BD), style = Stroke(3f, join = StrokeJoin.Round))
    }
    val route = Path().apply { moveTo(20f, 76f); cubicTo(36f, 50f, 50f, 80f, 62f, 50f); quadraticTo(70f, 34f, 78f, 36f) }
    drawPath(route, Color(0xFFE53935), style = Stroke(4f, cap = StrokeCap.Round, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(7f, 6f))))
    drawCircle(Color(0xFFE53935), 10f, Offset(78f, 28f))
    drawCircle(Color.White, 4f, Offset(78f, 28f))
}

/** Four glossy tiles, one per game mode, with a play arrow on top. */
private fun DrawScope.modes() {
    val tiles = listOf(
        Offset(12f, 12f) to Color(0xFFFFA000), Offset(52f, 12f) to Color(0xFF2F7CF6),
        Offset(12f, 52f) to Color(0xFFE0409A), Offset(52f, 52f) to Color(0xFFE6A100),
    )
    for ((o, c) in tiles) {
        drawRoundRect(Sunset.Outline, o + Offset(0f, 4f), Size(36f, 36f), CornerRadius(10f))
        drawRoundRect(c, o, Size(36f, 36f), CornerRadius(10f))
        drawRoundRect(Color.White.copy(alpha = 0.35f), o + Offset(5f, 4f), Size(26f, 9f), CornerRadius(4f))
    }
    drawCircle(Sunset.Outline, 19f, Offset(50f, 52f))
    drawCircle(Color.White, 16f, Offset(50f, 50f))
    val p = Path().apply { moveTo(45f, 41f); lineTo(59f, 50f); lineTo(45f, 59f); close() }
    drawPath(p, Color(0xFF5FC12F))
}

private fun DrawScope.collection() {
    rotate(-14f, Offset(50f, 50f)) { card(Color(0xFF9B5CF0)) }
    rotate(8f, Offset(50f, 50f)) { card(Color(0xFF2F7CF6)) }
    card(Color(0xFFFFC21A))
    car(Offset(50f, 56f), Color(0xFFF2453D), 0.48f)
}

private fun DrawScope.card(c: Color) {
    drawRoundRect(c.shade(0.6f), Offset(24f, 14f), Size(52f, 74f), CornerRadius(9f))
    drawRoundRect(Brush.verticalGradient(listOf(c.tint(0.3f), c)), Offset(26f, 14f), Size(48f, 70f), CornerRadius(8f))
    drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(31f, 20f), Size(38f, 40f), CornerRadius(5f))
}

private fun DrawScope.car(c: Offset, color: Color, k: Float = 1f) {
    scale(k, k, c) {
        drawRoundRect(Color(0xFF222222), Offset(c.x - 34f, c.y + 6f), Size(68f, 8f), CornerRadius(4f))
        val body = Path().apply {
            moveTo(c.x - 42f, c.y + 12f); lineTo(c.x - 42f, c.y - 4f); quadraticTo(c.x - 40f, c.y - 10f, c.x - 30f, c.y - 12f)
            lineTo(c.x - 20f, c.y - 28f); quadraticTo(c.x - 16f, c.y - 32f, c.x - 8f, c.y - 32f); lineTo(c.x + 16f, c.y - 32f)
            quadraticTo(c.x + 22f, c.y - 32f, c.x + 26f, c.y - 26f); lineTo(c.x + 34f, c.y - 12f)
            quadraticTo(c.x + 42f, c.y - 10f, c.x + 42f, c.y - 2f); lineTo(c.x + 42f, c.y + 12f); close()
        }
        drawPath(body, Brush.verticalGradient(listOf(color.tint(0.3f), color, color.shade(0.75f)), c.y - 32f, c.y + 12f))
        drawPath(body, color.shade(0.5f), style = Stroke(3.5f, join = StrokeJoin.Round))
        val glass = Path().apply {
            moveTo(c.x - 24f, c.y - 13f); lineTo(c.x - 15f, c.y - 26f); lineTo(c.x - 1f, c.y - 26f); lineTo(c.x - 1f, c.y - 13f); close()
            moveTo(c.x + 3f, c.y - 13f); lineTo(c.x + 3f, c.y - 26f); lineTo(c.x + 16f, c.y - 26f); lineTo(c.x + 25f, c.y - 13f); close()
        }
        drawPath(glass, Color(0xFF90CAF9))
        drawRoundRect(Color(0xFFFFF59D), Offset(c.x + 34f, c.y - 6f), Size(8f, 6f), CornerRadius(3f))
        for (wx in listOf(-24f, 24f)) {
            drawCircle(Color(0xFF263238), 11f, Offset(c.x + wx, c.y + 12f))
            drawCircle(Color(0xFFB0BEC5), 5f, Offset(c.x + wx, c.y + 12f))
        }
    }
}

private fun DrawScope.gear() {
    val c = Offset(50f, 50f)
    for (i in 0 until 8) {
        rotate(i * 45f, c) {
            drawRoundRect(Color(0xFFD98A00), Offset(42f, 6f), Size(16f, 20f), CornerRadius(4f))
        }
    }
    drawCircle(Color(0xFFD98A00), 36f, c)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFEB8A), Color(0xFFFFC21A)), Offset(40f, 40f), 50f), 31f, c)
    drawCircle(Color(0xFFD98A00), 13f, c)
    drawCircle(Color.White, 9f, c)
}

private fun DrawScope.circleArrow(start: Float, color: Color) {
    drawArc(color.shade(0.7f), start, 140f, false, Offset(14f, 14f), Size(72f, 72f), style = Stroke(14f, cap = StrokeCap.Round))
    drawArc(color, start, 140f, false, Offset(14f, 14f), Size(72f, 72f), style = Stroke(9f, cap = StrokeCap.Round))
    val a = (start + 140f) * PI / 180
    val tip = Offset(50f + 36f * cos(a).toFloat(), 50f + 36f * sin(a).toFloat())
    val dir = Offset(-sin(a).toFloat(), cos(a).toFloat())
    val n = Offset(cos(a).toFloat(), sin(a).toFloat())
    val p = Path().apply {
        moveTo(tip.x + dir.x * 14f, tip.y + dir.y * 14f)
        lineTo(tip.x + n.x * 12f, tip.y + n.y * 12f)
        lineTo(tip.x - n.x * 12f, tip.y - n.y * 12f)
        close()
    }
    drawPath(p, color)
}

private fun DrawScope.shuffle() {
    circleArrow(200f, Color(0xFF66BB6A))
    circleArrow(20f, Color(0xFF42A5F5))
    car(Offset(50f, 56f), Color(0xFF9B5CF0), 0.45f)
}

private fun DrawScope.crane() {
    drawLine(Color(0xFF455A64), Offset(50f, 4f), Offset(50f, 34f), 4f)
    drawArc(Color(0xFFFFB300), 0f, 180f, false, Offset(40f, 26f), Size(20f, 18f), style = Stroke(5f, cap = StrokeCap.Round))
    car(Offset(50f, 66f), Color(0xFF9B5CF0), 0.82f)
    val crown = Path().apply { moveTo(62f, 84f); lineTo(64f, 70f); lineTo(72f, 78f); lineTo(80f, 66f); lineTo(88f, 78f); lineTo(96f, 70f); lineTo(98f, 84f); close() }
    drawPath(crown, Color(0xFFFFD54F))
    drawPath(crown, Color(0xFFF57F17), style = Stroke(3f, join = StrokeJoin.Round))
}

private fun DrawScope.sort() {
    val r = Path().apply { moveTo(8f, 30f); lineTo(44f, 30f); lineTo(44f, 18f); lineTo(64f, 38f); lineTo(44f, 58f); lineTo(44f, 46f); lineTo(8f, 46f); close() }
    drawPath(r, Color(0xFF29B6F6)); drawPath(r, Color(0xFF0277BD), style = Stroke(3f, join = StrokeJoin.Round))
    val l = Path().apply { moveTo(60f, 62f); lineTo(24f, 62f); lineTo(24f, 50f); lineTo(4f, 70f); lineTo(24f, 90f); lineTo(24f, 78f); lineTo(60f, 78f); close() }
    drawPath(l, Color(0xFF66BB6A)); drawPath(l, Color(0xFF2E7D32), style = Stroke(3f, join = StrokeJoin.Round))
    drawCircle(Color(0xFF7E57C2), 13f, Offset(78f, 38f))
    drawRoundRect(Color(0xFF7E57C2), Offset(62f, 54f), Size(32f, 36f), CornerRadius(14f))
    drawCircle(Color.White.copy(alpha = 0.5f), 4f, Offset(74f, 34f))
}

private fun DrawScope.ad() {
    drawRoundRect(Color(0xFF0D47A1), Offset(8f, 22f), Size(84f, 62f), CornerRadius(12f))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF1E88E5))), Offset(8f, 18f), Size(84f, 62f), CornerRadius(12f))
    for (i in 0 until 4) drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(16f + i * 20f, 22f), Size(10f, 7f), CornerRadius(2f))
    val p = Path().apply { moveTo(40f, 38f); lineTo(64f, 52f); lineTo(40f, 66f); close() }
    drawPath(p, Color.White)
}

private fun DrawScope.gift() {
    drawRoundRect(Color(0xFFB71C1C), Offset(14f, 42f), Size(72f, 52f), CornerRadius(8f))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFF6E6E), Color(0xFFE53935))), Offset(14f, 40f), Size(72f, 50f), CornerRadius(8f))
    drawRoundRect(Color(0xFFD32F2F), Offset(8f, 30f), Size(84f, 18f), CornerRadius(6f))
    drawRoundRect(Color(0xFFFF7961), Offset(8f, 28f), Size(84f, 16f), CornerRadius(6f))
    drawRect(Color(0xFFFFD54F), Offset(43f, 28f), Size(14f, 62f))
    drawOval(Color(0xFFFFD54F), Offset(22f, 10f), Size(28f, 22f), style = Stroke(7f))
    drawOval(Color(0xFFFFD54F), Offset(50f, 10f), Size(28f, 22f), style = Stroke(7f))
    drawCircle(Color(0xFFFFB300), 7f, Offset(50f, 28f))
}

private fun DrawScope.trophy() {
    drawArc(Color(0xFFFFB300), 90f, 180f, false, Offset(8f, 20f), Size(30f, 36f), style = Stroke(7f))
    drawArc(Color(0xFFFFB300), -90f, 180f, false, Offset(62f, 20f), Size(30f, 36f), style = Stroke(7f))
    val cup = Path().apply { moveTo(22f, 14f); lineTo(78f, 14f); quadraticTo(78f, 62f, 50f, 66f); quadraticTo(22f, 62f, 22f, 14f); close() }
    drawPath(cup, Brush.horizontalGradient(listOf(Color(0xFFFFE082), Color(0xFFFFC107), Color(0xFFFF8F00)), 22f, 78f))
    drawRect(Color(0xFFFF8F00), Offset(44f, 64f), Size(12f, 14f))
    drawRoundRect(Color(0xFF8D6E63), Offset(28f, 78f), Size(44f, 14f), CornerRadius(4f))
    drawPath(starPath(50f, 36f, 12f), Color.White.copy(alpha = 0.9f))
}

private fun DrawScope.hand() {
    // A white glove pointing up-left, with a dark outline.
    val p = Path().apply {
        moveTo(30f, 6f)
        quadraticTo(38f, 2f, 42f, 10f)
        lineTo(46f, 40f)
        quadraticTo(52f, 34f, 58f, 40f)
        quadraticTo(64f, 36f, 70f, 42f)
        quadraticTo(78f, 40f, 82f, 48f)
        lineTo(86f, 70f)
        quadraticTo(88f, 90f, 70f, 96f)
        lineTo(46f, 96f)
        quadraticTo(36f, 92f, 30f, 82f)
        lineTo(16f, 60f)
        quadraticTo(14f, 52f, 22f, 52f)
        quadraticTo(28f, 54f, 32f, 62f)
        lineTo(24f, 14f)
        quadraticTo(24f, 8f, 30f, 6f)
        close()
    }
    drawPath(p, Color(0x55000000), style = Stroke(10f, join = StrokeJoin.Round))
    drawPath(p, Brush.verticalGradient(listOf(Color.White, Color(0xFFE3E8F0))))
    drawPath(p, Color(0xFF4A3A2A), style = Stroke(4f, join = StrokeJoin.Round))
    drawLine(Color(0xFF4A3A2A), Offset(58f, 41f), Offset(60f, 56f), 3f, StrokeCap.Round)
    drawLine(Color(0xFF4A3A2A), Offset(70f, 43f), Offset(72f, 58f), 3f, StrokeCap.Round)
    drawRoundRect(Color(0xFF42A5F5), Offset(42f, 88f), Size(36f, 10f), CornerRadius(4f))
}

private fun DrawScope.heart() {
    val p = Path().apply {
        moveTo(50f, 90f)
        cubicTo(10f, 62f, 4f, 40f, 16f, 24f)
        cubicTo(28f, 8f, 46f, 14f, 50f, 30f)
        cubicTo(54f, 14f, 72f, 8f, 84f, 24f)
        cubicTo(96f, 40f, 90f, 62f, 50f, 90f)
        close()
    }
    drawPath(p, Color(0xFF8E1B1B), style = Stroke(8f, join = StrokeJoin.Round))
    drawPath(p, Brush.verticalGradient(listOf(Color(0xFFFF8A80), Color(0xFFF2453D), Color(0xFFC62828)), 10f, 90f))
    drawOval(Color.White.copy(alpha = 0.55f), Offset(22f, 24f), Size(18f, 12f))
    car(Offset(52f, 56f), Color.White, 0.42f)
    drawCircle(Color(0xFF5FC12F), 14f, Offset(80f, 76f))
    drawLine(Color.White, Offset(72f, 76f), Offset(88f, 76f), 5f, StrokeCap.Round)
    drawLine(Color.White, Offset(80f, 68f), Offset(80f, 84f), 5f, StrokeCap.Round)
}

private fun DrawScope.sad() {
    drawCircle(Color(0xFFB26A00), 46f, Offset(50f, 53f))
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFCA28), Color(0xFFFF9800)), Offset(40f, 36f), 62f), 44f, Offset(50f, 50f))
    // squeezed eyes
    for (sx in listOf(-1f, 1f)) {
        val p = Path().apply {
            moveTo(50f + sx * 30f, 34f); lineTo(50f + sx * 14f, 40f); lineTo(50f + sx * 30f, 46f)
        }
        drawPath(p, Color(0xFF4E342E), style = Stroke(5f, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    drawOval(Color(0xFF6D2E1F), Offset(32f, 56f), Size(36f, 26f))
    drawOval(Color(0xFFE57373), Offset(40f, 68f), Size(20f, 12f))
    drawOval(Color(0xFF4FC3F7), Offset(10f, 44f), Size(10f, 16f))
    drawOval(Color(0xFF4FC3F7), Offset(80f, 44f), Size(10f, 16f))
}

/** White game controller with colored buttons and a purple outline (the Modes tab). */
private fun DrawScope.gamepad() {
    val ink = Color(0xFF3B2A8C)
    val body = Path().apply {
        moveTo(26f, 30f)
        cubicTo(36f, 25f, 64f, 25f, 74f, 30f)
        cubicTo(90f, 36f, 98f, 70f, 88f, 78f)
        cubicTo(80f, 85f, 70f, 74f, 63f, 66f)
        lineTo(37f, 66f)
        cubicTo(30f, 74f, 20f, 85f, 12f, 78f)
        cubicTo(2f, 70f, 10f, 36f, 26f, 30f)
        close()
    }
    drawPath(body, ink, style = Stroke(9f, join = StrokeJoin.Round))
    drawPath(body, Brush.verticalGradient(listOf(Color.White, Color(0xFFE4DEFF)), 25f, 85f))
    drawOval(Color.White.copy(alpha = 0.9f), Offset(28f, 31f), Size(30f, 8f))
    // D-pad
    drawRoundRect(ink, Offset(22f, 47f), Size(20f, 7f), CornerRadius(2f))
    drawRoundRect(ink, Offset(28.5f, 40.5f), Size(7f, 20f), CornerRadius(2f))
    // Buttons
    drawCircle(Color(0xFFFF4D6D), 5f, Offset(70f, 43f))
    drawCircle(Color(0xFF2F9BFF), 5f, Offset(62f, 51f))
    drawCircle(Color(0xFFFFC21A), 5f, Offset(78f, 51f))
    drawCircle(Color(0xFF3DBE4B), 5f, Offset(70f, 59f))
}
