package com.ngstudio.trafficjam.ui.main

import com.ngstudio.trafficjam.core.ads.AdConfig
import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.trafficjam.core.audio.Sfx
import com.ngstudio.trafficjam.core.data.PlayerState
import com.ngstudio.trafficjam.game.engine.CROWD_LANES
import com.ngstudio.trafficjam.game.engine.CrowdPath
import com.ngstudio.trafficjam.game.model.GameMode
import androidx.compose.foundation.layout.offset
import com.ngstudio.trafficjam.game.model.JamColor
import androidx.compose.foundation.layout.aspectRatio
import com.ngstudio.trafficjam.game.modes.ModeArt
import com.ngstudio.trafficjam.game.modes.ModeDraw
import com.ngstudio.trafficjam.game.modes.PixelPictures
import com.ngstudio.trafficjam.game.render.PassengerArt
import com.ngstudio.trafficjam.game.render.jamColor
import com.ngstudio.trafficjam.game.render.shade
import com.ngstudio.trafficjam.game.render.tint
import com.ngstudio.trafficjam.ui.components.GameIcon
import com.ngstudio.trafficjam.ui.components.Icon
import com.ngstudio.trafficjam.ui.components.LocalAudio
import com.ngstudio.trafficjam.ui.components.OutlinedText
import com.ngstudio.trafficjam.ui.theme.GameText
import com.ngstudio.trafficjam.ui.components.drawToyBoard
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random
import com.ngstudio.trafficjam.ui.theme.LocalTabPalette
import com.ngstudio.trafficjam.ui.components.toyCard
import com.ngstudio.trafficjam.ui.components.ToySign
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.ngstudio.trafficjam.ui.theme.TitleFont

/** The Modes tab: four big cards, one per way to play. */
@Composable
fun ModesTab(player: PlayerState, onPlay: (GameMode) -> Unit) {
    // Drives the staggered slide-in of the cards each time the tab opens.
    val intro = remember { Animatable(0f) }
    LaunchedEffect(Unit) { intro.animateTo(1f, tween(1000, easing = LinearEasing)) }
    Column(Modifier.fillMaxSize()) {
        ModesHeader()
        // No scrolling: the four cards fill the space under the header as a 2x2 grid.
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 13.dp, end = 13.dp, top = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            for (row in 0..1) {
                Row(
                    Modifier.weight(1f).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(13.dp),
                ) {
                    for (col in 0..1) {
                        val i = row * 2 + col
                        val m = GameMode.entries[i]
                        ModeCard(
                            m, player.modeLevel(m), player.modeFinished(m), onPlay,
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .graphicsLayer {
                                    val p = seg(intro.value, i * 0.1f, i * 0.1f + 0.5f)
                                    val e = easeOutBack(p)
                                    alpha = p
                                    translationY = (1f - e) * 70.dp.toPx()
                                    scaleX = 0.9f + 0.1f * e
                                    scaleY = 0.9f + 0.1f * e
                                },
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TabHeader(title: String, subtitle: String? = null) {
    val pal = LocalTabPalette.current
    // A toy billboard in the tab's colors, with the subtitle on a cream ribbon.
    ToySign(title, subtitle?.let { text -> { Text(text, style = GameText.Small.copy(color = pal.outline, fontSize = 13.sp)) } })
}

@Composable
private fun ModeCard(mode: GameMode, level: Int, finished: Boolean, onPlay: (GameMode) -> Unit, modifier: Modifier = Modifier) {
    val pal = LocalTabPalette.current
    val audio = LocalAudio.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.45f, stiffness = 700f), label = "press")
    val pulse = rememberInfiniteTransition(label = "play")
    val beat by pulse.animateFloat(
        0f, 1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "beat",
    )
    val accent = Color(mode.accent)
    val dark = Color(mode.accentDark)
    Box(modifier.fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
        Column(
            Modifier
                .fillMaxSize()
                .graphicsLayer { scaleX = press; scaleY = press }
                .clip(RoundedCornerShape(26.dp))
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val r = CornerRadius(26.dp.toPx())
                    // Thick colored border, then a lighter inner body with a chunky bottom lip.
                    drawRoundRect(dark.shade(0.95f), Offset.Zero, size, r)
                    val inset = 5.dp.toPx()
                    val lip = 10.dp.toPx()
                    drawRoundRect(
                        Brush.verticalGradient(listOf(accent.tint(0.3f), accent, accent.tint(0.12f))),
                        Offset(inset, inset), Size(w - 2 * inset, h - inset - lip), CornerRadius(20.dp.toPx()),
                    )
                    drawRoundRect(
                        Color.White.copy(alpha = 0.32f),
                        Offset(inset + 12.dp.toPx(), inset + 6.dp.toPx()), Size(w - 2 * inset - 24.dp.toPx(), 9.dp.toPx()), CornerRadius(5.dp.toPx()),
                    )
                }
                .clickable(interaction, null) {
                    audio.play(Sfx.Click)
                    onPlay(mode)
                }
                .padding(start = 11.dp, end = 11.dp, top = 20.dp, bottom = 11.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            // Preview in a white rounded frame, filling the top of the card.
            Box(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .drawBehind {
                        drawRoundRect(dark.copy(alpha = 0.28f), Offset(0f, 3.dp.toPx()), size, CornerRadius(16.dp.toPx()))
                        drawRoundRect(Color.White, Offset.Zero, size, CornerRadius(16.dp.toPx()))
                    }
                    .padding(4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Brush.verticalGradient(listOf(accent.tint(0.35f), accent))),
            ) {
                ModePreview(mode, Modifier.fillMaxSize())
            }
            Spacer(Modifier.height(7.dp))
            // Small emblem + title.
            Row(verticalAlignment = Alignment.CenterVertically) {
                Canvas(Modifier.size(28.dp)) { drawEmblem(titleEmblem(mode), center, size.minDimension * 0.36f) }
                Spacer(Modifier.width(6.dp))
                OutlinedText(mode.title, style = GameText.Title, fontSize = 19.sp, strokeWidth = 5f, color = Color.White, outline = dark.shade(0.85f))
            }
            Spacer(Modifier.height(3.dp))
            Text(
                mode.tagline,
                style = GameText.Small.copy(color = Color.White.copy(alpha = 0.92f), fontSize = 10.5.sp, lineHeight = 12.5.sp),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.weight(0.001f))
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    if (finished) "Done!" else "Level $level",
                    style = GameText.Label.copy(color = dark.shade(0.9f), fontFamily = TitleFont, fontSize = 13.sp),
                    modifier = Modifier
                        .drawBehind {
                            drawRoundRect(dark.copy(alpha = 0.22f), Offset(0f, 2.dp.toPx()), size, CornerRadius(11.dp.toPx()))
                            drawRoundRect(Color(0xFFFFF6E3), Offset.Zero, size, CornerRadius(11.dp.toPx()))
                        }
                        .padding(horizontal = 11.dp, vertical = 4.dp),
                )
                Spacer(Modifier.weight(1f))
                // Round green play button: white ring, glossy dome and a rounded arrow.
                Box(
                    Modifier
                        .size(40.dp)
                        .graphicsLayer {
                            val k = 1f + 0.05f * beat
                            scaleX = k
                            scaleY = k
                        }
                        .drawBehind {
                            val rad = size.minDimension / 2
                            val cc = center
                            val g = 2.dp.toPx() + 5.dp.toPx() * beat
                            // Soft breathing glow.
                            drawCircle(Color(0xFF8BE05A).copy(alpha = 0.55f * (1f - beat)), rad + g, cc)
                            // Drop shadow under the button.
                            drawCircle(Color(0xFF14421A).copy(alpha = 0.35f), rad, cc + Offset(0f, 3.dp.toPx()))
                            // White rim.
                            drawCircle(Color.White, rad, cc)
                            // Green dome: bright at the top, deep at the bottom.
                            val br = rad - 2.6.dp.toPx()
                            drawCircle(Color(0xFF2E7D32), br, cc)
                            drawCircle(
                                Brush.verticalGradient(listOf(Color(0xFFB4F06A), Color(0xFF57C03A), Color(0xFF3E9E2C)), cc.y - br, cc.y + br),
                                br - 1.4.dp.toPx(), cc,
                            )
                            // Glossy top highlight.
                            drawOval(
                                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.6f), Color.White.copy(alpha = 0f)), cc.y - br * 0.9f, cc.y),
                                Offset(cc.x - br * 0.62f, cc.y - br * 0.78f), Size(br * 1.24f, br * 0.82f),
                            )
                            // Rounded white play arrow.
                            val hh = rad * 0.46f
                            val tx = cc.x + rad * 0.1f + 2.dp.toPx() * beat
                            val tri = Path().apply {
                                moveTo(tx - hh * 0.62f, cc.y - hh)
                                lineTo(tx - hh * 0.62f, cc.y + hh)
                                lineTo(tx + hh * 0.9f, cc.y)
                                close()
                            }
                            drawPath(tri, Color(0x33123A12))
                            drawPath(tri, Color.White)
                            drawPath(tri, Color.White, style = Stroke(hh * 0.42f, cap = StrokeCap.Round, join = StrokeJoin.Round))
                        },
                )
            }
        }
        // Emblem badge popping out of the top.
        Canvas(
            Modifier.align(Alignment.TopCenter).offset(y = (-22).dp).size(52.dp),
        ) { drawEmblem(topEmblem(mode), center, size.minDimension * 0.34f) }
    }
}

private fun topEmblem(m: GameMode) = when (m) {
    GameMode.CLASSIC -> Emblem.CROWN
    GameMode.LOOP -> Emblem.CROWD
    GameMode.PIXEL -> Emblem.HEART
    GameMode.SAND -> Emblem.SAND
}

private fun titleEmblem(m: GameMode) = if (m == GameMode.CLASSIC) Emblem.CAR else topEmblem(m)

/** Little looping animation that shows what each mode is about. Every loop ends where it began. */
@Composable
internal fun ModePreview(mode: GameMode, modifier: Modifier) {
    val period = when (mode) {
        GameMode.CLASSIC -> (ClassicMini.PERIOD_SEC * 1000).toInt()
        GameMode.LOOP -> (LoopMini.PERIOD_SEC * 1000).toInt()
        GameMode.PIXEL -> (PixelMini.periodSec * 1000).toInt()
        GameMode.SAND -> 6500
    }
    val anim = rememberInfiniteTransition(label = "preview")
    val t by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(period, easing = LinearEasing), RepeatMode.Restart), label = "t")
    Canvas(modifier) {
        when (mode) {
            GameMode.CLASSIC -> classicPreview(t)
            GameMode.LOOP -> loopPreview(t)
            GameMode.PIXEL -> pixelPreview(t)
            GameMode.SAND -> sandPreview(t)
        }
    }
}

// ---- easing helpers ----

/** Progress of [t] through the window [a, b], clamped to 0..1. */
private fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun smooth(x: Float) = x * x * (3f - 2f * x)
private fun easeIn(x: Float) = x * x
private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x)
private fun easeOutBack(x: Float): Float {
    val c1 = 1.70158f
    val y = x - 1f
    return 1f + (c1 + 1f) * y * y * y + c1 * y * y
}
private fun mix(a: Float, b: Float, f: Float) = a + (b - a) * f
private fun mix(a: Offset, b: Offset, f: Float) = Offset(mix(a.x, b.x, f), mix(a.y, b.y, f))
private fun bump(x: Float) = if (x <= 0f || x >= 1f) 0f else sin(x * PI.toFloat())

private fun bezier(p0: Offset, p1: Offset, p2: Offset, p3: Offset, u: Float): Offset {
    val v = 1f - u
    return p0 * (v * v * v) + p1 * (3f * v * v * u) + p2 * (3f * v * u * u) + p3 * (u * u * u)
}

/** Heading (degrees, 0 = up, 90 = right) of the curve at [u]. */
private fun bezierHeading(p0: Offset, p1: Offset, p2: Offset, p3: Offset, u: Float): Float {
    val v = 1f - u
    val d = (p1 - p0) * (3f * v * v) + (p2 - p1) * (6f * v * u) + (p3 - p2) * (3f * u * u)
    return Math.toDegrees(atan2(d.x, -d.y).toDouble()).toFloat()
}

/** A top-down car pointing up at [angle] 0; [sx]/[sy] scale it across/along its length. */
private fun DrawScope.miniCar(c: Offset, color: Color, angle: Float, len: Float, wid: Float, sx: Float = 1f, sy: Float = sx, arrow: Boolean = true) {
    if (sx <= 0.01f || sy <= 0.01f) return
    withTransform({
        rotate(angle, c)
        scale(sx, sy, c)
    }) {
        drawRoundRect(Color.Black.copy(alpha = 0.2f), Offset(c.x - wid / 2 + 2f, c.y - len / 2 + 3f), Size(wid, len), CornerRadius(wid * 0.3f))
        drawRoundRect(color.shade(0.7f), Offset(c.x - wid / 2, c.y - len / 2 + 2f), Size(wid, len), CornerRadius(wid * 0.3f))
        drawRoundRect(color, Offset(c.x - wid / 2, c.y - len / 2), Size(wid, len - 2f), CornerRadius(wid * 0.3f))
        drawRoundRect(Color(0xFFCFE9FF), Offset(c.x - wid * 0.32f, c.y - len * 0.28f), Size(wid * 0.64f, len * 0.18f), CornerRadius(3f))
        if (!arrow) return@withTransform
        val a = Path().apply {
            moveTo(c.x, c.y - len * 0.05f)
            lineTo(c.x + wid * 0.22f, c.y + len * 0.12f)
            lineTo(c.x - wid * 0.22f, c.y + len * 0.12f)
            close()
        }
        drawPath(a, Color.White)
        drawLine(Color.White, Offset(c.x, c.y + len * 0.1f), Offset(c.x, c.y + len * 0.3f), wid * 0.12f, StrokeCap.Round)
    }
}

/** Expanding ring that shows where a finger tapped. */
private fun DrawScope.tapRipple(at: Offset, p: Float) {
    if (p <= 0f || p >= 1f) return
    val s = size.width
    drawCircle(Color.White.copy(alpha = 0.85f * (1f - p)), s * (0.05f + 0.12f * easeOut(p)), at, style = Stroke(3f))
    drawCircle(Color.White.copy(alpha = 0.35f * (1f - p)), s * 0.04f, at)
}

// ---- Classic: a car leaves the jam, parks, and its color boards from the station line ----

/**
 * A small copy of the real Classic board: the City Station platform with its single-file line
 * behind the rope barrier (along the platform, then up the right edge, like the game), the row
 * of stalls with the VIP bay, the road, and the jam in the lot. A tapped car drives out and parks,
 * the front passengers of its color hop in one by one while the line shuffles up, and the full
 * car backs out and drives off. Three cars (red, blue, yellow) take turns so the loop is seamless.
 */
private object ClassicMini {
    const val SEATS = 4
    const val CARS = 3
    const val PERIOD_SEC = 10.8f
    /** A passenger's walk to the car lasts this many boardings. */
    const val WALK = 2f
    val colors = intArrayOf(JamColor.RED.ordinal, JamColor.BLUE.ordinal, JamColor.YELLOW.ordinal)

    fun riderColor(i: Int) = colors[(i / SEATS) % colors.size]

    // Tile-relative geometry.
    const val FLOOR_TOP = 0.20f
    const val EDGE_Y = 0.47f
    const val STALL_TOP = 0.49f
    const val STALL_BOTTOM = 0.685f
    const val ROAD_TOP = 0.69f
    const val ROAD_BOTTOM = 0.775f
    const val LOT_TOP = 0.79f
    const val QUEUE_Y = 0.43f
    const val QUEUE_X0 = 0.07f
    const val QUEUE_X1 = 0.93f
    const val QUEUE_TOP = 0.05f
    const val STALL_W = 0.145f
    const val STALL_GAP = 0.012f
    const val STALL_START = 0.19f
    const val STALLS = 5
    /** Each car parks in its own stall. */
    val carStall = intArrayOf(1, 2, 3)

    private var forSize = -1f
    var tu = 0f
        private set
    lateinit var art: PassengerArt
        private set

    val textPaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }

    fun prepare(s: Float) {
        if (s == forSize) return
        forSize = s
        tu = s / 360f * 0.95f
        art = PassengerArt(15f * tu, 27f * tu)
    }

    fun stallRect(i: Int, s: Float): Rect {
        val x = (STALL_START + i * (STALL_W + STALL_GAP)) * s
        return Rect(x, STALL_TOP * s, x + STALL_W * s, STALL_BOTTOM * s)
    }

    /** The line: along the platform from the front at the left, then up the right edge. */
    fun queuePoint(d: Float, s: Float): Offset {
        val row = (QUEUE_X1 - QUEUE_X0) * s
        return if (d <= row) Offset(QUEUE_X0 * s + d, QUEUE_Y * s) else Offset(QUEUE_X1 * s, QUEUE_Y * s - (d - row))
    }

    fun queueLength(s: Float) = (QUEUE_X1 - QUEUE_X0) * s + (QUEUE_Y - QUEUE_TOP) * s
}

private fun DrawScope.classicPreview(t: Float) {
    val s = size.width
    val m = ClassicMini
    m.prepare(s)
    val tu = m.tu
    val time = t * m.PERIOD_SEC
    val spacing = 15.5f * tu

    // --- City Station: sky, skyline, canopy on pillars ---
    val floorTop = m.FLOOR_TOP * s
    drawRect(Brush.verticalGradient(listOf(Color(0xFFBFE6FF), Color(0xFFE3F4FF)), 0f, floorTop), size = Size(s, floorTop))
    val skyline = floatArrayOf(0.10f, 0.07f, 0.13f, 0.09f, 0.12f, 0.06f, 0.11f, 0.08f)
    for ((i, h) in skyline.withIndex()) {
        val x = i * s * 0.13f - s * 0.02f
        drawRect(Color(0xFFB7D7F0), Offset(x, floorTop - h * s - s * 0.02f), Size(s * 0.11f, h * s + s * 0.02f))
        for (wy in 0 until 2) drawRect(Color.White.copy(alpha = 0.45f), Offset(x + s * 0.025f, floorTop - h * s + wy * s * 0.03f), Size(s * 0.02f, s * 0.012f))
    }
    for ((cx, cy) in listOf(0.2f to 0.05f, 0.72f to 0.035f)) {
        drawCircle(Color.White, s * 0.03f, Offset(cx * s, cy * s))
        drawCircle(Color.White, s * 0.022f, Offset(cx * s - s * 0.03f, cy * s + s * 0.008f))
        drawCircle(Color.White, s * 0.024f, Offset(cx * s + s * 0.03f, cy * s + s * 0.006f))
    }
    drawRect(Brush.verticalGradient(listOf(Color(0xFFECEFF1), Color(0xFFB0BEC5)), s * 0.085f, s * 0.11f), Offset(0f, s * 0.085f), Size(s, s * 0.025f))
    drawRect(Color(0xFF90A4AE), Offset(0f, s * 0.11f), Size(s, s * 0.008f))
    var px = s * 0.08f
    while (px < s) { drawRect(Color(0xFF78909C), Offset(px, s * 0.118f), Size(s * 0.011f, floorTop - s * 0.118f)); px += s * 0.25f }
    // Departure board.
    drawRoundRect(Color(0xFF263238), Offset(s * 0.6f, s * 0.13f), Size(s * 0.11f, s * 0.04f), CornerRadius(2f))
    for (k in 0 until 2) drawRect(Color(0xFFFFC107).copy(alpha = 0.9f), Offset(s * 0.61f, s * 0.138f + k * s * 0.013f), Size(s * (0.055f + k * 0.025f), s * 0.006f))

    // --- Tiled platform floor ---
    val edgeY = m.EDGE_Y * s
    val tile = s * 0.06f
    var ty = floorTop
    var r = 0
    while (ty < edgeY) {
        var tx = 0f
        var c = 0
        while (tx < s) {
            drawRect(if ((r + c) % 2 == 0) Color(0xFFEDEFF2) else Color(0xFFE4E7EB), Offset(tx, ty), Size(tile, min(tile, edgeY - ty)))
            tx += tile; c++
        }
        drawLine(Color(0xFFC9CED6), Offset(0f, ty), Offset(s, ty), 1f)
        ty += tile; r++
    }
    drawRect(Brush.verticalGradient(listOf(Color(0x33000000), Color.Transparent), floorTop, floorTop + s * 0.03f), Offset(0f, floorTop), Size(s, s * 0.03f))
    // Lamp post at the head of the line.
    val lampX = s * 0.035f
    drawRect(Color(0xFF455A64), Offset(lampX - 1.2f * tu, m.QUEUE_Y * s - 46f * tu), Size(2.4f * tu, 44f * tu))
    drawCircle(Color(0xFFFFF59D), 4.5f * tu, Offset(lampX, m.QUEUE_Y * s - 48f * tu))
    drawCircle(Color(0xFF455A64), 4.5f * tu, Offset(lampX, m.QUEUE_Y * s - 48f * tu), style = Stroke(1.2f * tu))
    // Rope barrier along the line.
    val postY = m.QUEUE_Y * s - 24f * tu
    val posts = generateSequence(m.QUEUE_X0 * s + 34f * tu) { it + 44f * tu }.takeWhile { it < m.QUEUE_X1 * s - 26f * tu }.toList()
    for (k in 0 until posts.size - 1) {
        val a = Offset(posts[k], postY)
        val b = Offset(posts[k + 1], postY)
        val rope = Path().apply { moveTo(a.x, a.y); quadraticTo((a.x + b.x) / 2, a.y + 7f * tu, b.x, b.y) }
        drawPath(rope, Color(0xFF7F1D1D), style = Stroke(2.6f * tu, cap = StrokeCap.Round))
        drawPath(rope, Color(0xFFC62828), style = Stroke(1.6f * tu, cap = StrokeCap.Round))
    }
    for (x in posts) {
        drawRoundRect(Color(0xFFB8860B), Offset(x - 1.3f * tu, postY), Size(2.6f * tu, 16f * tu), CornerRadius(tu))
        drawCircle(Color(0xFFFFC107), 2.4f * tu, Offset(x, postY))
    }

    // --- Platform edge, stalls with the VIP bay, road ---
    drawRect(Color(0xFFFFD54F), Offset(0f, edgeY), Size(s, s * 0.01f))
    drawRect(Color(0xFFE9ECEF), Offset(0f, edgeY + s * 0.01f), Size(s, m.STALL_TOP * s - edgeY - s * 0.01f))
    drawRect(Color(0xFF9EA3AB), Offset(0f, m.STALL_TOP * s), Size(s, (m.ROAD_TOP - m.STALL_TOP) * s))
    m.textPaint.textSize = 9f * tu
    m.textPaint.color = android.graphics.Color.argb(140, 255, 255, 255)
    for (i in 0 until m.STALLS) {
        val st = m.stallRect(i, s)
        val line = 2.6f * tu
        drawRect(Color.White, Offset(st.left - line / 2, st.top), Size(line, st.height))
        if (i == m.STALLS - 1) drawRect(Color.White, Offset(st.right - line / 2, st.top), Size(line, st.height))
        drawRoundRect(Color(0xFFDCE0E5), Offset(st.left + st.width * 0.2f, st.top + 3f * tu), Size(st.width * 0.6f, 3f * tu), CornerRadius(1.5f * tu))
        drawContext.canvas.nativeCanvas.drawText("${i + 1}", st.center.x, st.bottom - 4f * tu, m.textPaint)
    }
    val vip = Rect(s * 0.02f, m.STALL_TOP * s + 3f * tu, m.STALL_START * s - s * 0.02f, m.STALL_BOTTOM * s - 2f * tu)
    drawRoundRect(Color(0xFF8E1B1B), vip.topLeft + Offset(0f, 2f * tu), vip.size, CornerRadius(5f * tu))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFE53935), Color(0xFFB71C1C)), vip.top, vip.bottom), vip.topLeft, vip.size, CornerRadius(5f * tu))
    drawRoundRect(Color(0xFFFFD54F), vip.topLeft + Offset(2.5f * tu, 2.5f * tu), Size(vip.width - 5f * tu, vip.height - 5f * tu), CornerRadius(3f * tu), style = Stroke(1.4f * tu))
    m.textPaint.textSize = 10f * tu
    m.textPaint.color = android.graphics.Color.rgb(255, 224, 130)
    drawContext.canvas.nativeCanvas.drawText("VIP", vip.center.x, vip.center.y + 3.5f * tu, m.textPaint)
    val roadTop = m.ROAD_TOP * s
    val roadBottom = m.ROAD_BOTTOM * s
    val roadY = (roadTop + roadBottom) / 2
    drawRect(Color(0xFF6E737B), Offset(0f, roadTop), Size(s, roadBottom - roadTop))
    drawRect(Color.White.copy(alpha = 0.85f), Offset(0f, roadTop + 1.5f * tu), Size(s, 1.5f * tu))
    drawRect(Color.White.copy(alpha = 0.85f), Offset(0f, roadBottom - 3f * tu), Size(s, 1.5f * tu))
    drawLine(Color.White.copy(alpha = 0.7f), Offset(0f, roadY), Offset(s, roadY), 2f * tu, pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f * tu, 10f * tu)))

    // --- The lot: grass, curb, asphalt ---
    drawRect(Color(0xFF8FC25A), Offset(0f, roadBottom), Size(s, s - roadBottom))
    val lot = Rect(s * 0.03f, m.LOT_TOP * s, s * 0.97f, s * 1.04f)
    drawRoundRect(Color(0xFFE6E9EC), lot.topLeft - Offset(2f * tu, 2f * tu), Size(lot.width + 4f * tu, lot.height + 4f * tu), CornerRadius(14f * tu))
    drawRoundRect(Color(0xFFD4D6CF), lot.topLeft, lot.size, CornerRadius(12f * tu))
    drawRoundRect(Color(0xFFB9BBB2), Offset(s * 0.1f, lot.top + s * 0.02f), Size(s * 0.8f, s * 0.2f), CornerRadius(4f * tu))

    // --- Which car, and how far boarding has got ---
    val sub = t * m.CARS
    val cycle = min(m.CARS - 1, floor(sub).toInt())
    val tau = sub - cycle
    val boarded = (m.SEATS + m.WALK) * seg(tau, 0.30f, 0.66f)
    val left = min(m.SEATS.toFloat(), boarded)
    val gone = cycle * m.SEATS + left
    val goneI = floor(gone).toInt()
    // Each time the front passenger leaves, the whole line steps up one place.
    val step = smooth(seg(gone - goneI, 0f, 0.7f))
    val lineShift = goneI + step
    val shuffling = step > 0f && step < 1f
    // A passenger leaves the line the moment it starts walking to the car.
    val firstWaiting = ceil(gone).toInt()

    // --- The line, drawn back to front ---
    val pw = m.art.w
    val ph = m.art.h
    val maxD = m.queueLength(s)
    var last = firstWaiting
    while ((last + 1 - lineShift) * spacing <= maxD) last++
    for (i in last downTo firstWaiting) {
        val d = max(0f, i - lineShift) * spacing
        val p = m.queuePoint(d, s)
        val bob = if (shuffling) 0f else sin(time * 3f + i * 0.7f) * 0.6f * tu
        val frame = if (shuffling) ((time * 9f).toInt() + i) % 4 else 4
        drawOval(Color.Black.copy(alpha = 0.18f), Offset(p.x - pw * 0.42f, p.y - ph * 0.08f), Size(pw * 0.84f, ph * 0.18f))
        drawImage(m.art.get(m.riderColor(i), frame), Offset(p.x - pw / 2, p.y - ph - bob))
    }

    // --- The jam in the lot ---
    val carLen = s * 0.15f
    val carWid = s * 0.085f
    miniCar(Offset(s * 0.22f, s * 0.90f), jamColor(JamColor.PINK.ordinal), 180f, carLen, carWid)
    miniCar(Offset(s * 0.71f, s * 0.85f), jamColor(JamColor.GREEN.ordinal), 90f, s * 0.2f, carWid)
    miniCar(Offset(s * 0.71f, s * 0.95f), jamColor(JamColor.ORANGE.ordinal), 270f, carLen, carWid)

    // --- The car: lot → stall, boards, backs out and drives off ---
    val carColor = jamColor(m.colors[cycle])
    val lotPos = Offset(s * 0.42f, s * 0.90f)
    val stall = m.stallRect(m.carStall[cycle], s)
    val stallC = stall.center
    val tap = seg(tau, 0.0f, 0.09f)
    val filled = if (boarded < m.WALK) 0 else ((boarded - m.WALK).toInt() + 1).coerceAtMost(m.SEATS)
    var pos = lotPos
    var angle = 0f
    var scale = 1f + 0.12f * bump(tap)
    var parked = false
    var color = carColor
    when {
        tau < 0.08f -> Unit
        tau < 0.30f -> {
            val p0 = lotPos
            val p1 = Offset(lotPos.x, s * 0.68f)
            val p2 = Offset(stallC.x, s * 0.80f)
            val q = 1f - seg(tau, 0.08f, 0.30f)
            val u = 1f - q * q * sqrt(q)
            pos = bezier(p0, p1, p2, stallC, u)
            angle = bezierHeading(p0, p1, p2, stallC, u)
        }
        tau < 0.70f -> {
            pos = stallC
            parked = true
            if (boarded >= m.WALK && boarded < m.SEATS + m.WALK) scale = 1f + 0.07f * bump(boarded - floor(boarded))
        }
        tau < 0.76f -> {
            pos = Offset(stallC.x, mix(stallC.y, roadY, smooth(seg(tau, 0.70f, 0.76f))))
            parked = true
        }
        tau < 0.88f -> {
            val q = seg(tau, 0.76f, 0.88f)
            angle = 90f * smooth(seg(q, 0f, 0.3f))
            pos = Offset(stallC.x + s * (0.3f * q + 1.6f * q * q), roadY)
            parked = true
        }
        else -> {
            // The next car in the jam pops into its place.
            color = jamColor(m.colors[(cycle + 1) % m.colors.size])
            scale = easeOutBack(seg(tau, 0.88f, 0.97f))
        }
    }
    if (tau in 0.78f..0.88f) {
        val q = seg(tau, 0.78f, 0.86f)
        val back = pos.x - carLen / 2
        for (j in -1..1) {
            val y = pos.y + j * s * 0.022f
            drawLine(Color.White.copy(alpha = 0.7f * q), Offset(back - s * 0.02f, y), Offset(back - s * (0.07f + 0.08f * q) - abs(j) * s * 0.025f, y), s * 0.011f, StrokeCap.Round)
        }
    }
    miniCar(pos, color, angle, carLen, carWid, scale, arrow = !parked)
    if (parked) {
        // One head per seat on the roof as passengers climb in.
        withTransform({
            rotate(angle, pos)
            scale(scale, scale, pos)
        }) {
            val stepY = carLen * 0.2f
            val rr = min(carWid * 0.16f, stepY * 0.42f)
            for (h in 0 until m.SEATS) {
                val c = Offset(pos.x + (if (h % 2 == 0) -1 else 1) * carWid * 0.22f, pos.y + carLen * 0.02f + (h / 2) * stepY)
                if (h < filled) {
                    drawCircle(carColor.shade(0.55f), rr + 0.8f, c + Offset(0f, 0.8f))
                    drawCircle(carColor.tint(0.25f), rr, c)
                    drawCircle(Color.White.copy(alpha = 0.7f), rr * 0.28f, c - Offset(rr * 0.38f, rr * 0.38f))
                } else {
                    drawCircle(Color.Black.copy(alpha = 0.22f), rr * 0.75f, c)
                }
            }
        }
    }

    // --- Passengers hopping from the front of the line into the car ---
    if (tau in 0.30f..0.70f) {
        val front = m.queuePoint(0f, s)
        for (j in 0 until m.SEATS) {
            val w = (boarded - j) / m.WALK
            if (w <= 0f || w >= 1f) continue
            val p = mix(front, stallC, w) - Offset(0f, sin(w * PI.toFloat()) * 18f * tu)
            val sc = 1f - w * 0.35f
            withTransform({ scale(sc, sc, p) }) {
                drawImage(m.art.get(m.colors[cycle], ((time * 9f).toInt() + j) % 4), Offset(p.x - pw / 2, p.y - ph))
            }
        }
    }
    tapRipple(lotPos, tap)
}

// ---- Crowd Loop: the crowd lines up round the ring, a bus parks and its color boards ----

/**
 * A small copy of the real Crowd Loop board, built from the game's own pieces: the same
 * [CrowdPath] shape (front of the line at the throat by the stalls, round the ring, tail off the
 * right edge), the same [PassengerArt] riders packed four abreast, and the same rules: a tapped
 * bus parks in a stall, the front riders of its color hop in one by one, the line steps up a row
 * at a time, and the full bus backs out and drives off. Three buses (blue, red, yellow) take turns
 * so the loop ends exactly where it began.
 */
private object LoopMini {
    const val LANES = CROWD_LANES
    const val ROWS_PER_BUS = 6
    const val PER_BUS = ROWS_PER_BUS * LANES
    const val BUSES = 3
    const val PERIOD_SEC = 13.5f
    /** Riders' walk to the bus lasts this many boardings. */
    const val WALK = 4f
    val colors = intArrayOf(JamColor.BLUE.ordinal, JamColor.RED.ordinal, JamColor.YELLOW.ordinal)

    fun rowColor(row: Int) = colors[(row / ROWS_PER_BUS) % colors.size]

    // Tile-relative geometry.
    const val RING_CX = 0.5f
    const val RING_CY = 0.27f
    const val RING_RX = 0.36f
    const val RING_RY = 0.19f
    const val EDGE_Y = 0.595f
    const val STALL_TOP = 0.615f
    const val STALL_BOTTOM = 0.795f
    const val ROAD_TOP = 0.80f
    const val ROAD_BOTTOM = 0.885f
    const val STALL_W = 0.13f
    const val STALL_GAP = 0.012f
    const val STALL_START = 0.24f
    const val STALLS = 5
    const val BUS_STALL = 1

    private var forSize = -1f
    var tu = 0f
        private set
    lateinit var path: CrowdPath
        private set
    lateinit var art: PassengerArt
        private set

    val badgePaint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
        color = android.graphics.Color.WHITE
    }

    /** Builds the path and rider sprites for a tile [s] pixels wide (once per size). */
    fun prepare(s: Float) {
        if (s == forSize) return
        forSize = s
        tu = s / 360f * 0.95f
        art = PassengerArt(12f * tu, 21.5f * tu)
        val pts = ArrayList<Offset>()
        pts += Offset(0.31f * s, 0.585f * s)
        pts += Offset(0.29f * s, 0.50f * s)
        var a = 128.0
        while (a <= 412.0) {
            val r = Math.toRadians(a)
            pts += Offset((RING_CX + RING_RX * cos(r).toFloat()) * s, (RING_CY + RING_RY * sin(r).toFloat()) * s)
            a += 6.0
        }
        pts += Offset(0.84f * s, 0.50f * s)
        pts += Offset(1.06f * s, 0.53f * s)
        pts += Offset(1.6f * s, 0.535f * s)
        path = CrowdPath(pts, 0.75f * tu)
    }

    fun stallRect(i: Int, s: Float): Rect {
        val x = (STALL_START + i * (STALL_W + STALL_GAP)) * s
        return Rect(x, STALL_TOP * s, x + STALL_W * s, STALL_BOTTOM * s)
    }

    // Reused per frame to draw the crowd back to front.
    val riderX = FloatArray(800)
    val riderY = FloatArray(800)
    val riderC = IntArray(800)
    val order = ArrayList<Int>(800)
}

private fun DrawScope.loopPreview(t: Float) {
    val s = size.width
    val m = LoopMini
    m.prepare(s)
    val tu = m.tu
    val time = t * m.PERIOD_SEC
    val spacing = 8.4f * tu
    val laneGap = 7.4f * tu
    fun side(lane: Int) = (lane - (m.LANES - 1) / 2f) * laneGap

    // --- Park: lawn, inner field and the ring road the crowd stands on ---
    val edgeY = m.EDGE_Y * s
    drawRect(Color(0xFF8BCB5A), Offset.Zero, Size(s, edgeY))
    var stripe = 0f
    var k = 0
    while (stripe < edgeY) {
        if (k % 2 == 0) drawRect(Color.White.copy(alpha = 0.06f), Offset(0f, stripe), Size(s, s * 0.06f))
        stripe += s * 0.06f; k++
    }
    val rc = Offset(m.RING_CX * s, m.RING_CY * s)
    val roadW = m.LANES * laneGap + 12f * tu
    val inRx = m.RING_RX * s - roadW / 2 - 3f * tu
    val inRy = m.RING_RY * s - roadW / 2 - 3f * tu
    drawOval(Color(0xFFA4DB6E), Offset(rc.x - inRx, rc.y - inRy), Size(inRx * 2, inRy * 2))
    drawOval(Color.White.copy(alpha = 0.18f), Offset(rc.x - inRx, rc.y - inRy), Size(inRx * 2, inRy * 2), style = Stroke(1.5f * tu))
    val bed = Offset(rc.x, rc.y + inRy * 0.35f)
    drawOval(Color(0xFF6D4C41), Offset(bed.x - s * 0.08f, bed.y - s * 0.025f), Size(s * 0.16f, s * 0.05f))
    drawOval(Color(0xFF8D6E63), Offset(bed.x - s * 0.072f, bed.y - s * 0.021f), Size(s * 0.144f, s * 0.04f))
    val flowers = listOf(Color(0xFFFF5A7A), Color(0xFFFFEB3B), Color.White, Color(0xFFBA68C8))
    for (f in 0 until 10) drawCircle(flowers[f % 4], s * 0.007f, Offset(bed.x + (f - 4.5f) * s * 0.013f, bed.y + (if (f % 2 == 0) -1 else 1) * s * 0.008f))
    for ((tx, ty) in listOf(rc.x - inRx * 0.6f to rc.y - inRy * 0.1f, rc.x + inRx * 0.6f to rc.y)) {
        drawCircle(Color.Black.copy(alpha = 0.15f), s * 0.045f, Offset(tx + 2f, ty + 3f))
        drawCircle(Color(0xFF388E3C), s * 0.045f, Offset(tx, ty))
        drawCircle(Color(0xFF66BB6A), s * 0.028f, Offset(tx - s * 0.01f, ty - s * 0.012f))
    }
    val road = Path().apply {
        val pts = m.path.samples(6)
        moveTo(pts[0].x, pts[0].y + 30f * tu)
        for (p in pts) lineTo(p.x, p.y)
    }
    drawPath(road, Color.Black.copy(alpha = 0.16f), style = Stroke(roadW + 8f * tu, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(road, Color(0xFFE6E9EC), style = Stroke(roadW + 6f * tu, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(road, Color(0xFF7D838D), style = Stroke(roadW, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawPath(road, Color.White.copy(alpha = 0.45f), style = Stroke(1.6f * tu, cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(9f * tu, 9f * tu))))

    // --- Platform edge, parking stalls, road and lot ---
    drawRect(Color(0xFFFFD54F), Offset(0f, edgeY), Size(s, s * 0.012f))
    drawRect(Color(0xFFE9ECEF), Offset(0f, edgeY + s * 0.012f), Size(s, m.STALL_TOP * s - edgeY - s * 0.012f))
    drawRect(Color(0xFF5E6B7A), Offset(0f, m.STALL_TOP * s), Size(s, (m.ROAD_TOP - m.STALL_TOP) * s))
    for (i in 0 until m.STALLS) {
        val r = m.stallRect(i, s)
        drawRoundRect(Color(0xFF6E7B8B), r.topLeft + Offset(1f, 2f), Size(r.width - 2f, r.height - 4f), CornerRadius(3f * tu))
        drawRect(Color.White.copy(alpha = 0.8f), Offset(r.left, r.top + 2f), Size(1.5f * tu, r.height - 4f))
        drawRect(Color.White.copy(alpha = 0.8f), Offset(r.right - 1.5f * tu, r.top + 2f), Size(1.5f * tu, r.height - 4f))
    }
    // VIP stall on the left, like the game.
    val vip = Rect(0.02f * s, m.STALL_TOP * s + 2f, m.STALL_START * s - 0.015f * s, m.STALL_BOTTOM * s - 2f)
    drawRoundRect(Color(0xFF6E7B8B), vip.topLeft, vip.size, CornerRadius(3f * tu))
    drawRoundRect(Color(0xFFFFD54F).copy(alpha = 0.7f), vip.topLeft, vip.size, CornerRadius(3f * tu), style = Stroke(1.5f * tu))
    val roadTop = m.ROAD_TOP * s
    val roadBottom = m.ROAD_BOTTOM * s
    val roadY = (roadTop + roadBottom) / 2
    drawRect(Color(0xFF4F5866), Offset(0f, roadTop), Size(s, roadBottom - roadTop))
    drawRect(Color.White.copy(alpha = 0.85f), Offset(0f, roadTop + 1.5f * tu), Size(s, 1.5f * tu))
    drawRect(Color.White.copy(alpha = 0.85f), Offset(0f, roadBottom - 3f * tu), Size(s, 1.5f * tu))
    drawLine(Color.White.copy(alpha = 0.7f), Offset(0f, roadY), Offset(s, roadY), 2f * tu, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f * tu, 9f * tu)))
    drawRect(Color(0xFFC9CFD8), Offset(0f, roadBottom), Size(s, s - roadBottom))

    // --- Which bus, and how far boarding has got ---
    val sub = t * m.BUSES
    val cycle = min(m.BUSES - 1, floor(sub).toInt())
    val tau = sub - cycle
    val boarded = (m.PER_BUS + m.WALK) * seg(tau, 0.30f, 0.70f)
    val left = min(m.PER_BUS.toFloat(), boarded)
    val gone = cycle * m.PER_BUS + left
    val goneI = floor(gone).toInt()
    val inRow = gone - (goneI / m.LANES) * m.LANES
    val step = smooth(seg(inRow, m.LANES - 1.5f, m.LANES.toFloat()))
    // The line moves up a whole row once the front row has boarded.
    val shift = (goneI / m.LANES) + step
    val shuffling = step > 0f && step < 1f

    // --- The crowd, four abreast, drawn back to front ---
    val firstRow = goneI / m.LANES
    // A rider leaves the crowd the moment it starts walking to the bus.
    val firstWaiting = ceil(gone).toInt()
    var n = 0
    var row = firstRow
    while (n < m.riderX.size - m.LANES) {
        val rel = max(0f, row - shift)
        val d = rel * spacing
        if (d > m.path.length - 6f * tu) break
        for (lane in 0 until m.LANES) {
            if (row * m.LANES + lane < firstWaiting) continue
            val p = m.path.point(d, side(lane))
            m.riderX[n] = p.x
            m.riderY[n] = p.y
            m.riderC[n] = m.rowColor(row)
            n++
        }
        row++
    }
    m.order.clear()
    for (i in 0 until n) m.order += i
    m.order.sortBy { m.riderY[it] }
    val pw = m.art.w
    val ph = m.art.h
    for (i in m.order) {
        val x = m.riderX[i]
        val y = m.riderY[i]
        val bob = if (shuffling) 0f else sin(time * 3.4f + i * 0.9f) * 0.5f * tu
        val frame = if (shuffling) ((time * 9f).toInt() + i) % 4 else 4
        drawOval(Color.Black.copy(alpha = 0.2f), Offset(x - pw * 0.4f, y - ph * 0.08f), Size(pw * 0.8f, ph * 0.16f))
        drawImage(m.art.get(m.riderC[i], frame), Offset(x - pw / 2, y - ph - bob))
    }

    // --- The bus: lot → stall, boards, backs out and drives off ---
    val busColor = jamColor(m.colors[cycle])
    val busLen = s * 0.17f
    val busWid = s * 0.095f
    val lotPos = Offset(s * 0.3f, s * 0.945f)
    val stall = m.stallRect(m.BUS_STALL, s)
    val stallC = stall.center
    val tap = seg(tau, 0.0f, 0.09f)
    val filled = if (boarded < m.WALK) 0 else ((boarded - m.WALK).toInt() + 1).coerceAtMost(m.PER_BUS)
    var busPos = lotPos
    var busAngle = 90f
    var busScale = 1f + 0.12f * bump(tap)
    var parked = false
    var color = busColor
    when {
        tau < 0.08f -> Unit
        tau < 0.30f -> {
            val p0 = lotPos
            val p1 = Offset(s * 0.58f, lotPos.y)
            val p2 = Offset(stallC.x, s * 0.86f)
            val u = 1f - (1f - seg(tau, 0.08f, 0.30f)).let { it * it * sqrt(it) } // the game's exit easing (≈ 1-(1-p)^2.3)
            busPos = bezier(p0, p1, p2, stallC, u)
            busAngle = bezierHeading(p0, p1, p2, stallC, u)
        }
        tau < 0.74f -> {
            busPos = stallC
            busAngle = 0f
            parked = true
            // A little bounce each time a rider lands.
            if (boarded >= m.WALK && boarded < m.PER_BUS + m.WALK) busScale = 1f + 0.07f * bump(boarded - floor(boarded))
        }
        tau < 0.80f -> {
            busPos = Offset(stallC.x, mix(stallC.y, roadY, smooth(seg(tau, 0.74f, 0.80f))))
            busAngle = 0f
            parked = true
        }
        tau < 0.90f -> {
            val q = seg(tau, 0.80f, 0.90f)
            busAngle = 90f * smooth(seg(q, 0f, 0.3f))
            busPos = Offset(stallC.x + s * (0.3f * q + 1.6f * q * q), roadY)
            parked = true
        }
        else -> {
            // The next bus in the jam pops into the lot.
            color = jamColor(m.colors[(cycle + 1) % m.colors.size])
            busScale = easeOutBack(seg(tau, 0.90f, 0.98f))
        }
    }

    // Other vehicles waiting in the lot.
    miniCar(Offset(s * 0.70f, s * 0.945f), jamColor(JamColor.GREEN.ordinal), 270f, busLen, busWid)
    miniCar(Offset(s * 0.92f, s * 0.945f), jamColor(JamColor.PINK.ordinal), 90f, busLen, busWid)

    miniCar(busPos, color, busAngle, busLen, busWid, busScale, arrow = !parked)
    if (parked) {
        // Heads filling the roof, one per four riders like the game's big buses.
        withTransform({
            rotate(busAngle, busPos)
            scale(busScale, busScale, busPos)
        }) {
            val heads = 8
            val shown = filled * heads / m.PER_BUS
            val stepY = busLen * 0.13f
            val r = min(busWid * 0.15f, stepY * 0.45f)
            for (h in 0 until heads) {
                val c = Offset(busPos.x + (if (h % 2 == 0) -1 else 1) * busWid * 0.22f, busPos.y - busLen * 0.04f + (h / 2) * stepY)
                if (h < shown) {
                    drawCircle(busColor.shade(0.55f), r + 0.8f, c + Offset(0f, 0.8f))
                    drawCircle(busColor.tint(0.25f), r, c)
                    drawCircle(Color.White.copy(alpha = 0.7f), r * 0.28f, c - Offset(r * 0.38f, r * 0.38f))
                } else {
                    drawCircle(Color.Black.copy(alpha = 0.22f), r * 0.75f, c)
                }
            }
        }
    }
    // Seats-left tag at the foot of the stall while the bus is parked.
    if (tau in 0.30f..0.74f) {
        val seatsLeft = m.PER_BUS - filled
        val c = Offset(stallC.x, stall.bottom + 1f * tu)
        val w = 26f * tu
        val h = 13f * tu
        drawRoundRect(busColor.shade(0.5f), Offset(c.x - w / 2, c.y - h / 2 + 1.5f * tu), Size(w, h), CornerRadius(5f * tu))
        drawRoundRect(busColor.shade(0.85f), Offset(c.x - w / 2, c.y - h / 2), Size(w, h), CornerRadius(5f * tu))
        m.badgePaint.textSize = 10f * tu
        drawContext.canvas.nativeCanvas.drawText("$seatsLeft", c.x, c.y + 3.6f * tu, m.badgePaint)
    }
    // Speed lines as it leaves.
    if (tau in 0.82f..0.90f) {
        val q = seg(tau, 0.82f, 0.88f)
        val back = busPos.x - busLen / 2
        for (j in -1..1) {
            val y = busPos.y + j * s * 0.025f
            drawLine(Color.White.copy(alpha = 0.7f * q), Offset(back - s * 0.02f, y), Offset(back - s * (0.07f + 0.08f * q) - abs(j) * s * 0.025f, y), s * 0.012f, StrokeCap.Round)
        }
    }

    // --- Riders hopping from the front of the line into the bus ---
    if (tau in 0.30f..0.74f) {
        for (j in 0 until m.PER_BUS) {
            val w = (boarded - j) / m.WALK
            if (w <= 0f || w >= 1f) continue
            val from = m.path.point(0f, side(j % m.LANES))
            val p = mix(from, stallC, w) - Offset(0f, sin(w * PI.toFloat()) * 18f * tu)
            val sc = 1f - w * 0.35f
            withTransform({ scale(sc, sc, p) }) {
                drawImage(m.art.get(m.colors[cycle], ((time * 9f).toInt() + j) % 4), Offset(p.x - pw / 2, p.y - ph))
            }
        }
    }
    tapRipple(lotPos, tap)
}

// ---- Pixel Crowd: a bus leaves the garage, and the picture's outside riders run to it ----

/** The picture the Pixel Crowd and Sand Sweep previews are made from (the game's first one). */
private val previewPic = PixelPictures.all.first()

/**
 * A small copy of the real Pixel Crowd board: the blue garage with three lanes, the road, four
 * bus stops (the last one locked) and the picture made of rider blocks on its plate. Using the
 * game's own rules, a tapped lane sends its front bus to the nearest free stop; riders of its
 * color that are on the outside of the picture run out around the edge to the bus door, which
 * uncovers the riders behind them. The red bus clears the outline, the pink bus then peels the
 * pink riders it uncovered, and the picture rebuilds. Everything is worked out once, in units of
 * the tile width, and played back.
 */
private object PixelMini {
    private val pic = previewPic
    val pw = pic.w
    val ph = pic.h
    private const val MARGIN = 2
    private val nw = pw + MARGIN * 2
    private val nh = ph + MARGIN * 2

    // Tile-relative geometry (fractions of the tile width).
    const val GARAGE_TOP = 0.03f
    const val GARAGE_BOTTOM = 0.22f
    const val ROAD_Y = 0.26f
    const val ROAD_TOP = 0.22f
    const val ROAD_BOTTOM = 0.30f
    const val STOP_TOP = 0.315f
    const val STOP_H = 0.19f
    const val STOPS = 4
    const val OPEN_STOPS = 3
    const val BUS_LEN = 0.15f
    const val BUS_WID = 0.08f
    val stopW = (1f - 24f / 360f - 3 * 8f / 360f) / STOPS
    fun stopX(i: Int) = 12f / 360f + stopW / 2 + i * (stopW + 8f / 360f)
    fun laneX(i: Int) = (1 + 2 * i) / 6f
    const val GARAGE_SPOT_Y = 0.145f
    val cell = min(0.93f / pw, 0.42f / ph)
    val picLeft = (1f - cell * pw) / 2
    const val PIC_TOP = 0.565f

    private const val RUN_SPEED = 175f / 360f // tile widths per second, the game's speed
    private const val BOARD_INTERVAL = 0.05f

    /** One rider leaving the picture: its cell, when it sets off, and its route to the door. */
    class Run(val cell: Int, val color: Int, val bus: Int, val start: Float, val xs: FloatArray, val ys: FloatArray, val cum: FloatArray, val side: Float, val phase: Float) {
        val length get() = cum.last()
        val arrive get() = start + length / RUN_SPEED
    }

    /** A bus's trip: which lane and stop, and when it is tapped, parks, fills and leaves. */
    class Trip(val color: Int, val lane: Int, val stop: Int) {
        var tap = 0f
        val driveStart get() = tap + 0.1f
        var driveEnd = 0f
        var full = 0f
        val departStart get() = full + 0.3f
        var cap = 0
        val px = FloatArray(48)
        val py = FloatArray(48)
        val heading = FloatArray(48)
    }

    val trips: List<Trip>
    val runs = ArrayList<Run>()
    /** Index of the run that took each cell, or -1. */
    val runOf = IntArray(pw * ph) { -1 }
    val restoreStart: Float
    val periodSec: Float

    fun stopCenter(i: Int) = Offset(stopX(i), STOP_TOP + STOP_H / 2)
    fun door(i: Int) = stopCenter(i) + Offset(0f, BUS_LEN * 0.42f)
    fun cellCenter(i: Int) = Offset(picLeft + (i % pw + 0.5f) * cell, PIC_TOP + (i / pw + 0.5f) * cell)

    init {
        val cells = pic.cells.copyOf()
        val rnd = Random(11)
        val pink = JamColor.PINK.ordinal
        val red = JamColor.RED.ordinal
        // The red bus takes the whole outline; the pink bus then takes the pink it uncovered.
        val a = Trip(red, 0, 0)
        a.cap = cells.count { it == red }
        val b = Trip(pink, 2, 2)
        b.cap = 24
        trips = listOf(a, b)
        for ((ti, trip) in trips.withIndex()) {
            // The pink lane is tapped as the red bus pulls away.
            plan(trip, if (ti == 0) 0.3f else trips[0].departStart + 0.15f)
            val outside = BooleanArray(pw * ph)
            var launch = trip.driveEnd + 0.12f
            var lastArrive = launch
            repeat(trip.cap) {
                flood(cells, outside)
                val field = navField(cells, trip.stop)
                var pick = -1
                var pickD = Int.MAX_VALUE
                for (i in cells.indices) {
                    if (cells[i] != trip.color || !exposed(i, cells, outside)) continue
                    val dd = cellNavDist(i, cells, field)
                    if (dd < pickD) { pickD = dd; pick = i }
                }
                if (pick < 0) return@repeat
                val door = door(trip.stop) + Offset((rnd.nextFloat() - 0.5f) * 10f / 360f, 0f)
                val pts = route(pick, cells, field, door)
                cells[pick] = -1
                val xs = FloatArray(pts.size) { pts[it].x }
                val ys = FloatArray(pts.size) { pts[it].y }
                val cum = FloatArray(pts.size)
                for (k in 1 until pts.size) cum[k] = cum[k - 1] + (pts[k] - pts[k - 1]).getDistance()
                val run = Run(pick, trip.color, ti, launch, xs, ys, cum, (rnd.nextFloat() - 0.5f) * 0.7f * cell, rnd.nextFloat() * 6.28f)
                runOf[pick] = runs.size
                runs += run
                lastArrive = max(lastArrive, run.arrive)
                launch += BOARD_INTERVAL
            }
            trip.cap = runs.count { it.bus == ti }
            trip.full = lastArrive
        }
        restoreStart = trips[1].departStart + 1.4f
        periodSec = restoreStart + 1.6f
    }

    /** Sets when [trip] is tapped and samples its drive: out of the garage, along the road, into the stop. */
    private fun plan(trip: Trip, tapAt: Float) = with(trip) {
        tap = tapAt
        val from = Offset(laneX(lane), GARAGE_SPOT_Y)
        val to = stopCenter(stop)
        val pts = ArrayList<Offset>()
        for (q in listOf(from, Offset(from.x, ROAD_Y), Offset(to.x, ROAD_Y), to)) {
            if (pts.isEmpty() || (pts.last() - q).getDistance() > 0.003f) pts += q
        }
        val path = ModeDraw.roundedPath(pts, 20f / 360f)
        val m = PathMeasure().apply { setPath(path, false) }
        val len = m.length
        for (k in px.indices) {
            val d = len * k / (px.size - 1)
            val p = m.getPosition(d)
            val tan = m.getTangent(d)
            px[k] = p.x
            py[k] = p.y
            heading[k] = if (tan.getDistance() > 0.01f) Math.toDegrees(atan2(tan.x, -tan.y).toDouble()).toFloat() else 180f
        }
        driveEnd = driveStart + (0.3f + len / (900f / 360f)).coerceIn(0.45f, 1.1f)
    }

    private fun flood(cells: IntArray, outside: BooleanArray) {
        outside.fill(false)
        val stack = IntArray(pw * ph)
        var sp = 0
        fun push(i: Int) { if (!outside[i] && cells[i] < 0) { outside[i] = true; stack[sp++] = i } }
        for (x in 0 until pw) { push(x); push((ph - 1) * pw + x) }
        for (y in 0 until ph) { push(y * pw); push(y * pw + pw - 1) }
        while (sp > 0) {
            val i = stack[--sp]
            val x = i % pw
            val y = i / pw
            if (x > 0) push(i - 1)
            if (x < pw - 1) push(i + 1)
            if (y > 0) push(i - pw)
            if (y < ph - 1) push(i + pw)
        }
    }

    fun exposed(i: Int, cells: IntArray, outside: BooleanArray): Boolean {
        if (cells[i] < 0) return false
        val x = i % pw
        val y = i / pw
        if (x == 0 || y == 0 || x == pw - 1 || y == ph - 1) return true
        return outside[i - 1] || outside[i + 1] || outside[i - pw] || outside[i + pw]
    }

    private fun navFree(cells: IntArray, ex: Int, ey: Int): Boolean {
        if (ex !in 0 until nw || ey !in 0 until nh) return false
        val x = ex - MARGIN
        val y = ey - MARGIN
        if (x !in 0 until pw || y !in 0 until ph) return true
        return cells[y * pw + x] < 0
    }

    private fun navCenter(ex: Int, ey: Int) = Offset(picLeft + (ex - MARGIN + 0.5f) * cell, PIC_TOP + (ey - MARGIN + 0.5f) * cell)

    /** Walking distance from every open cell to the ground above stop [s] (the game's field). */
    private fun navField(cells: IntArray, s: Int): IntArray {
        val d = IntArray(nw * nh) { Int.MAX_VALUE }
        val q = java.util.PriorityQueue<Long>()
        val start = ((stopX(s) - picLeft) / cell + MARGIN).toInt().coerceIn(0, nw - 1)
        d[start] = 0
        q.add(start.toLong())
        while (q.isNotEmpty()) {
            val top = q.poll()!!
            val i = (top and 0xFFFFF).toInt()
            val dist = (top shr 20).toInt()
            if (dist > d[i]) continue
            val x = i % nw
            val y = i / nw
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = x + dx
                val ny = y + dy
                if (!navFree(cells, nx, ny)) continue
                if (dx != 0 && dy != 0 && (!navFree(cells, x + dx, y) || !navFree(cells, x, y + dy))) continue
                val nd = dist + if (dx != 0 && dy != 0) 14 else 10
                val ni = ny * nw + nx
                if (nd < d[ni]) { d[ni] = nd; q.add((nd.toLong() shl 20) or ni.toLong()) }
            }
        }
        return d
    }

    private fun cellNavDist(i: Int, cells: IntArray, d: IntArray): Int {
        val ex = i % pw + MARGIN
        val ey = i / pw + MARGIN
        var best = Int.MAX_VALUE
        for ((dx, dy) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            if (!navFree(cells, ex + dx, ey + dy)) continue
            best = min(best, d[(ey + dy) * nw + ex + dx])
        }
        return best
    }

    /** The game's route: out of the picture, downhill round the outside to the exit, up to the door. */
    private fun route(i: Int, cells: IntArray, d: IntArray, door: Offset): List<Offset> {
        val pts = ArrayList<Offset>()
        pts += cellCenter(i)
        var ex = i % pw + MARGIN
        var ey = i / pw + MARGIN
        var cur = -1
        var best = Int.MAX_VALUE
        for ((dx, dy) in listOf(-1 to 0, 1 to 0, 0 to -1, 0 to 1)) {
            val nx = ex + dx
            val ny = ey + dy
            if (navFree(cells, nx, ny) && d[ny * nw + nx] < best) { best = d[ny * nw + nx]; cur = ny * nw + nx }
        }
        var guard = 0
        while (cur >= 0 && guard++ < nw * nh) {
            ex = cur % nw
            ey = cur / nw
            pts += navCenter(ex, ey)
            if (d[cur] == 0) break
            var next = -1
            var nb = d[cur]
            for (dy in -1..1) for (dx in -1..1) {
                if (dx == 0 && dy == 0) continue
                val nx = ex + dx
                val ny = ey + dy
                if (!navFree(cells, nx, ny)) continue
                if (dx != 0 && dy != 0 && (!navFree(cells, ex + dx, ey) || !navFree(cells, ex, ey + dy))) continue
                val v = d[ny * nw + nx]
                if (v < nb) { nb = v; next = ny * nw + nx }
            }
            cur = next
        }
        val exit = pts.last()
        pts += Offset((exit.x + door.x) / 2, (exit.y + door.y) / 2 + cell * 0.3f)
        pts += door
        var out: List<Offset> = pts
        repeat(2) { out = chaikin(out) }
        return out
    }

    private fun chaikin(p: List<Offset>): List<Offset> {
        if (p.size < 3) return p
        val out = ArrayList<Offset>(p.size * 2)
        out += p.first()
        for (k in 0 until p.size - 1) {
            out += p[k] * 0.75f + p[k + 1] * 0.25f
            out += p[k] * 0.25f + p[k + 1] * 0.75f
        }
        out += p.last()
        return out
    }

    /** Position and direction [dist] along a run's route (unit space). */
    fun along(r: Run, dist: Float, out: FloatArray) {
        val s = dist.coerceIn(0f, r.length)
        var k = 1
        while (k < r.cum.size - 1 && r.cum[k] < s) k++
        val segLen = (r.cum[k] - r.cum[k - 1]).coerceAtLeast(1e-6f)
        val f = ((s - r.cum[k - 1]) / segLen).coerceIn(0f, 1f)
        out[0] = r.xs[k - 1] + (r.xs[k] - r.xs[k - 1]) * f
        out[1] = r.ys[k - 1] + (r.ys[k] - r.ys[k - 1]) * f
        out[2] = (r.xs[k] - r.xs[k - 1]) / segLen
        out[3] = (r.ys[k] - r.ys[k - 1]) / segLen
    }

    fun runDist(r: Run, sec: Float) = max(0f, sec - r.start) * RUN_SPEED

    // Size-dependent art.
    private var forSize = -1f
    val tiles = arrayOfNulls<ImageBitmap>(8)
    var tilePx = 0
        private set
    val paint = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = android.graphics.Paint.Align.CENTER
        typeface = android.graphics.Typeface.DEFAULT_BOLD
    }
    val tmp = FloatArray(4)
    val cellsNow = IntArray(pw * ph)
    val outsideNow = BooleanArray(pw * ph)

    fun prepare(s: Float) {
        if (s == forSize) return
        forSize = s
        tilePx = (cell * s).toInt().coerceAtLeast(4)
        tiles.fill(null)
        for (c in pic.colors(false)) tiles[c] = ModeArt.riderBlock(c, tilePx)
    }

    val source get() = pic
}

private fun DrawScope.pixelPreview(t: Float) {
    val s = size.width
    val m = PixelMini
    m.prepare(s)
    val sec = t * m.periodSec
    val u = s / 360f
    val cellPx = m.cell * s

    // --- Plaza, sky, garage, road, stops and the plate (the game's scene) ---
    drawRect(Brush.verticalGradient(listOf(Color(0xFFE4EEFF), Color(0xFFC9DBFA))))
    val tile = 30f * u
    var ty = m.ROAD_BOTTOM * s
    var row = 0
    while (ty < s) {
        var tx = if (row % 2 == 0) 0f else -tile / 2
        while (tx < s) {
            drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(tx + 1.5f * u, ty + 1.5f * u), Size(tile - 3f * u, tile - 3f * u), CornerRadius(4f * u))
            tx += tile
        }
        ty += tile; row++
    }
    val gBottom = m.GARAGE_BOTTOM * s
    drawRect(Brush.verticalGradient(listOf(Color(0xFF6EC6FF), Color(0xFFB3E1FF)), 0f, gBottom), size = Size(s, gBottom))
    val gTop = m.GARAGE_TOP * s
    drawRoundRect(Color(0xFF0D47A1), Offset(4f * u, gTop + 4f * u), Size(s - 8f * u, gBottom - gTop), CornerRadius(14f * u))
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF1E88E5)), gTop, gBottom), Offset(4f * u, gTop), Size(s - 8f * u, gBottom - gTop), CornerRadius(14f * u))
    drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(14f * u, gTop + 4f * u), Size(s - 28f * u, 6f * u), CornerRadius(3f * u))
    for (i in 0 until 3) {
        val c = m.laneX(i) * s
        val dw = 64f * u
        val top = gTop + 18f * u
        drawRoundRect(Color(0xFF0B2A5C), Offset(c - dw / 2 - 3f * u, top - 3f * u), Size(dw + 6f * u, gBottom - top + 3f * u), CornerRadius(12f * u, 12f * u))
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF1B2A44), Color(0xFF3A4D6E)), top, gBottom), Offset(c - dw / 2, top), Size(dw, gBottom - top), CornerRadius(10f * u, 10f * u))
    }
    val roadTop = m.ROAD_TOP * s
    val roadBottom = m.ROAD_BOTTOM * s
    drawRect(Color(0xFF59606B), Offset(0f, roadTop), Size(s, roadBottom - roadTop))
    drawRect(Color.Black.copy(alpha = 0.25f), Offset(0f, roadTop), Size(s, 3f * u))
    drawLine(Color.White.copy(alpha = 0.75f), Offset(0f, m.ROAD_Y * s), Offset(s, m.ROAD_Y * s), 2.4f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f * u, 12f * u)))
    drawRect(Color(0xFFE9ECEF), Offset(0f, roadBottom), Size(s, 4f * u))
    for (i in 0 until m.STOPS) {
        val r = Rect((m.stopX(i) - m.stopW / 2) * s, m.STOP_TOP * s, (m.stopX(i) + m.stopW / 2) * s, (m.STOP_TOP + m.STOP_H) * s)
        drawRoundRect(Color(0xFF8A93A0), r.topLeft + Offset(0f, 3f * u), r.size, CornerRadius(8f * u))
        drawRoundRect(Color(0xFFA7B0BC), r.topLeft, r.size, CornerRadius(8f * u))
        drawRoundRect(Color(0xFFFFD54F), r.topLeft + Offset(3f * u, 3f * u), Size(r.width - 6f * u, r.height - 6f * u), CornerRadius(6f * u), style = Stroke(2f * u, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7f * u, 5f * u))))
        if (i >= m.OPEN_STOPS) {
            // Locked stop: watch an ad to open it.
            drawRoundRect(Color(0xFF3C4250).copy(alpha = 0.45f), r.topLeft, r.size, CornerRadius(8f * u))
            val c = Offset(r.center.x, r.center.y - 8f * u)
            drawCircle(Color(0xFF2E7D32), 13f * u, c + Offset(0f, 2f * u))
            drawCircle(Brush.verticalGradient(listOf(Color(0xFF9CE65C), Color(0xFF4CAF50)), c.y - 13f * u, c.y + 13f * u), 13f * u, c)
            drawLine(Color.White, c - Offset(6f * u, 0f), c + Offset(6f * u, 0f), 3f * u, StrokeCap.Round)
            drawLine(Color.White, c - Offset(0f, 6f * u), c + Offset(0f, 6f * u), 3f * u, StrokeCap.Round)
            if (AdConfig.enabled) {
                val bb = Offset(r.center.x, r.bottom - 14f * u)
                drawRoundRect(Color(0xFFFFB300), bb - Offset(14f * u, 7f * u), Size(28f * u, 13f * u), CornerRadius(4f * u))
                m.paint.color = android.graphics.Color.WHITE
                m.paint.textSize = 9.5f * u
                drawContext.canvas.nativeCanvas.drawText("AD", bb.x, bb.y + 3f * u, m.paint)
            }
        }
    }
    val picLeft = m.picLeft * s
    val picTop = m.PIC_TOP * s
    val plate = Rect(picLeft - 10f * u, picTop - 10f * u, picLeft + cellPx * m.pw + 10f * u, picTop + cellPx * m.ph + 10f * u)
    drawRoundRect(Color(0x22000000), plate.topLeft + Offset(0f, 6f * u), plate.size, CornerRadius(18f * u))
    drawRoundRect(Color.White.copy(alpha = 0.55f), plate.topLeft, plate.size, CornerRadius(18f * u))
    drawRoundRect(Color(0xFF90B4F0), plate.topLeft, plate.size, CornerRadius(18f * u), style = Stroke(2f * u))

    // --- Bus states ---
    val restore = sec >= m.restoreStart
    fun parkedNeed(ti: Int): Boolean {
        val tr = m.trips[ti]
        return sec >= tr.driveEnd && sec < tr.departStart
    }
    var eager = 0
    for (ti in m.trips.indices) if (parkedNeed(ti) && sec < m.trips[ti].full) eager = eager or (1 shl m.trips[ti].color)

    // --- The picture: riders still standing, bouncing if their bus is waiting and they're on the edge ---
    val src = m.source
    for (i in m.cellsNow.indices) {
        val ri = m.runOf[i]
        m.cellsNow[i] = if (ri >= 0 && sec >= m.runs[ri].start && !restore) -1 else src.cells[i]
    }
    // Outside flood fill for "exposed" (same rule as the game).
    run {
        val o = m.outsideNow
        o.fill(false)
        val stack = IntArray(m.pw * m.ph)
        var sp = 0
        fun push(i: Int) { if (!o[i] && m.cellsNow[i] < 0) { o[i] = true; stack[sp++] = i } }
        for (x in 0 until m.pw) { push(x); push((m.ph - 1) * m.pw + x) }
        for (y in 0 until m.ph) { push(y * m.pw); push(y * m.pw + m.pw - 1) }
        while (sp > 0) {
            val i = stack[--sp]
            val x = i % m.pw
            val y = i / m.pw
            if (x > 0) push(i - 1)
            if (x < m.pw - 1) push(i + 1)
            if (y > 0) push(i - m.pw)
            if (y < m.ph - 1) push(i + m.pw)
        }
    }
    val nRuns = max(1, m.runs.size)
    for (i in m.cellsNow.indices) {
        val c = m.cellsNow[i]
        if (c < 0) continue
        val tileImg = m.tiles[c] ?: continue
        val x = i % m.pw
        val y = i / m.pw
        val ri = m.runOf[i]
        // Riders that left pop back in when the picture rebuilds.
        val pop = if (restore && ri >= 0) easeOutBack(seg(sec, m.restoreStart + ri.toFloat() / nRuns * 0.9f, m.restoreStart + ri.toFloat() / nRuns * 0.9f + 0.3f)) else 1f
        if (pop <= 0.01f) continue
        val bob = if (eager and (1 shl c) != 0 && m.exposed(i, m.cellsNow, m.outsideNow)) {
            -abs(sin(sec * 11f + x * 1.3f + y * 0.9f)) * 2.6f * u
        } else sin(sec * 3.2f + x * 0.7f + y * 0.45f) * 0.6f * u
        val o = Offset(picLeft + x * cellPx, picTop + y * cellPx + bob)
        if (pop >= 0.999f) {
            drawImage(tileImg, o)
        } else {
            val ctr = o + Offset(cellPx / 2, cellPx / 2)
            withTransform({ scale(pop, pop, ctr) }) { drawImage(tileImg, o) }
        }
    }

    // --- Garage lanes: the bus at each door, with how many riders it needs ---
    val red = JamColor.RED.ordinal
    val pink = JamColor.PINK.ordinal
    val yellow = JamColor.YELLOW.ordinal
    val busLen = m.BUS_LEN * s
    val busWid = m.BUS_WID * s
    fun badge(at: Offset, color: Int, left: Int) {
        val col = jamColor(color)
        drawCircle(col.shade(0.45f), 11.5f * u, at + Offset(0f, 1.2f * u))
        drawCircle(Color.White, 11f * u, at)
        drawCircle(col, 11f * u, at, style = Stroke(2f * u))
        m.paint.color = col.shade(0.55f).toArgb()
        m.paint.textSize = 11f * u
        drawContext.canvas.nativeCanvas.drawText("$left", at.x, at.y + 4f * u, m.paint)
    }
    // Lane fronts: the dispatched bus is replaced by the next one, until the board resets.
    val laneFront = intArrayOf(red, yellow, pink)
    val laneNext = arrayOf(intArrayOf(yellow, pink), intArrayOf(red), intArrayOf(pink, yellow))
    for (i in 0 until 3) {
        val trip = m.trips.firstOrNull { it.lane == i }
        val dispatched = trip != null && sec >= trip.driveStart && !restore
        val color = if (dispatched) laneNext[i][0] else laneFront[i]
        val nextDots = if (dispatched) laneNext[i].drop(1) else laneNext[i].toList()
        val spot = Offset(m.laneX(i) * s, m.GARAGE_SPOT_Y * s)
        // The replacement bus rolls up to the door; the original pops back on reset.
        val sc = when {
            dispatched && trip != null -> easeOutBack(seg(sec, trip.driveStart + 0.25f, trip.driveStart + 0.55f))
            restore && trip != null -> easeOutBack(seg(sec, m.restoreStart, m.restoreStart + 0.35f))
            else -> 1f
        }
        miniCar(spot, jamColor(color), 180f, busLen, busWid, sc)
        if (sc > 0.5f) badge(spot, color, if (color == pink) 24 else if (color == red) m.trips[0].cap else 18)
        val dy = gTop + 8f * u
        val startX = m.laneX(i) * s - (nextDots.size - 1) * 7f * u
        for ((k, nc) in nextDots.withIndex()) {
            val p = Offset(startX + k * 14f * u, dy)
            drawCircle(Color.Black.copy(alpha = 0.25f), 5.5f * u, p + Offset(0f, u))
            drawCircle(jamColor(nc), 5f * u, p)
            drawCircle(Color.White, 5f * u, p, style = Stroke(1.2f * u))
        }
    }

    // --- Dispatched buses: drive to the stop, park facing down, fill, back out, race off left ---
    for ((ti, tr) in m.trips.withIndex()) {
        if (sec < tr.driveStart || restore) continue
        val stopC = m.stopCenter(tr.stop) * s
        val arrived = m.runs.count { it.bus == ti && sec >= it.arrive }
        var pos: Offset
        var heading = 180f
        var bounce = 1f
        var showBadge = true
        when {
            sec < tr.driveEnd -> {
                val p = seg(sec, tr.driveStart, tr.driveEnd)
                val e = 1f - (1f - p).let { it * it * sqrt(sqrt(it)) } // ≈ the game's 1-(1-p)^2.2
                val f = e * (tr.px.size - 1)
                val k = min(tr.px.size - 2, floor(f).toInt())
                val q = f - k
                pos = Offset(mix(tr.px[k], tr.px[k + 1], q) * s, mix(tr.py[k], tr.py[k + 1], q) * s)
                heading = tr.heading[k]
            }
            sec < tr.departStart -> {
                pos = stopC
                // A little bounce each time a rider climbs in.
                val lastIn = m.runs.filter { it.bus == ti && it.arrive <= sec }.maxOfOrNull { it.arrive }
                if (lastIn != null) bounce = 1f + 0.06f * bump(seg(sec, lastIn, lastIn + 0.25f))
            }
            sec < tr.departStart + 0.28f -> {
                pos = Offset(stopC.x, mix(stopC.y, m.ROAD_Y * s, smooth(seg(sec, tr.departStart, tr.departStart + 0.28f))))
                showBadge = false
            }
            else -> {
                val tt = sec - tr.departStart - 0.28f
                heading = 180f + 90f * smooth(seg(tt, 0f, 0.22f))
                pos = Offset(stopC.x - (200f * u * tt + 1300f * u * tt * tt), m.ROAD_Y * s)
                showBadge = false
                if (pos.x < -0.3f * s) continue
            }
        }
        miniCar(pos, jamColor(tr.color), heading, busLen, busWid, bounce, arrow = sec < tr.driveEnd)
        if (showBadge) badge(pos, tr.color, tr.cap - arrived)
    }

    // --- Riders running round the outside of the picture to their bus ---
    if (!restore) {
        // Drawn back to front, like the game.
        val active = m.runs.indices.filter { val r = m.runs[it]; sec >= r.start && sec < r.arrive }
            .sortedBy { m.along(m.runs[it], m.runDist(m.runs[it], sec), m.tmp); m.tmp[1] }
        val tileH = cellPx
        for (idx in active) {
            val r = m.runs[idx]
            val tileImg = m.tiles[r.color] ?: continue
            val d = m.runDist(r, sec)
            m.along(r, d, m.tmp)
            val hx = m.tmp[2]
            val hy = m.tmp[3]
            val spread = min(1f, d / m.cell) * min(1f, (r.length - d) / (m.cell * 2.5f))
            val p = Offset((m.tmp[0] - hy * r.side * spread) * s, (m.tmp[1] + hx * r.side * spread) * s)
            val end = ((r.length - d) / (m.cell * 0.8f)).coerceIn(0f, 1f)
            val sc = 0.94f * (0.55f + 0.45f * end)
            val sizePx = tileH * sc
            // Legs grow in over the first stretch and tuck away as the rider climbs aboard.
            val sprout = min(1f, d / (m.cell * 0.7f)) * min(1f, end * 1.6f)
            val step = sin(d / (m.cell * 0.36f) + r.phase)
            val lift = abs(step) * 2.6f * u * sprout + if (d < m.cell * 0.5f) (1f - d / (m.cell * 0.5f)) * 4f * u else 0f
            val nx = -hy
            val ny = hx
            val col = jamColor(r.color)
            val limb = col.shade(0.5f)
            drawOval(Color.Black.copy(alpha = 0.2f * (0.5f + 0.5f * end)), Offset(p.x - sizePx * 0.45f, p.y + sizePx * 0.42f), Size(sizePx * 0.9f, sizePx * 0.32f))
            if (sprout > 0.02f) {
                for (k in intArrayOf(-1, 1)) {
                    val stride = step * k * sizePx * 0.24f * sprout
                    val hip = Offset(p.x + nx * k * sizePx * 0.2f, p.y + sizePx * 0.42f + ny * k * sizePx * 0.08f)
                    val foot = Offset(hip.x + hx * stride - hx * sizePx * 0.06f, hip.y + sizePx * 0.17f * sprout + hy * stride)
                    drawLine(limb, hip, foot, sizePx * 0.22f, StrokeCap.Round)
                    drawCircle(limb.shade(0.75f), sizePx * 0.13f * sprout + 0.5f, foot + Offset(0f, sizePx * 0.04f))
                }
                for (k in intArrayOf(-1, 1)) {
                    val swing = -step * k * sizePx * 0.16f * sprout
                    val shoulder = Offset(p.x + nx * k * sizePx * 0.46f, p.y + sizePx * 0.12f + ny * k * sizePx * 0.2f - lift)
                    val hand = Offset(shoulder.x + nx * k * sizePx * 0.1f * sprout + hx * swing, shoulder.y + sizePx * 0.12f * sprout + hy * swing)
                    drawLine(limb, shoulder, hand, sizePx * 0.15f, StrokeCap.Round)
                    drawCircle(col.shade(0.7f), sizePx * 0.09f * sprout + 0.5f, hand)
                }
            }
            val lean = (Math.toDegrees(atan2(hx, -hy).toDouble()).toFloat()).coerceIn(-32f, 32f) * sprout
            withTransform({
                rotate(lean, p)
                scale(sc, sc, p)
            }) {
                drawImage(tileImg, Offset(p.x - cellPx / 2, p.y - cellPx / 2 - lift / sc))
            }
        }
    }

    // Tap ripples on the garage doors.
    for (tr in m.trips) tapRipple(Offset(m.laneX(tr.lane) * s, m.GARAGE_SPOT_Y * s), seg(sec, tr.tap - 0.05f, tr.tap + 0.25f))
}

// ---- Sand Sweep: tap a truck out of the jam, it rides the belt and scoops its own color ----

/**
 * A small copy of the real Sand Sweep board. The preview picture is painted into fine sand grains
 * the same way the game paints its levels (soft brush shading, darker rims), then the cyan truck's
 * ride along the belt is simulated once with the game's rules: it scoops cyan grains within reach
 * of the bottom, and the sand above falls straight down or slides diagonally into the gaps. The
 * preview then just plays the recorded frames back, drawn as one smoothed bitmap like the game.
 */
private object SandMini {
    const val GW = 90
    const val GH = 48
    private const val DEPTH = 4
    private const val EAT_PER_FRAME = 7
    private const val SLIDE_CHANCE = 0.35f
    const val RIDE_FRAMES = 150
    const val SETTLE_FRAMES = 30
    const val FRAMES = RIDE_FRAMES + SETTLE_FRAMES

    // Tile-relative geometry shared with [sandPreview].
    const val PIC_LEFT = 0.08f
    const val PIC_W = 0.84f
    const val MERGE_X = 0.3f
    const val BELT_END = 0.9f
    const val TRUCK_LEN = 0.2f

    val eatColor = previewPic.background

    /** Grain codes (color * 8 + shade, or -1) for each recorded frame. */
    private val frames: Array<ByteArray>
    private val start: ByteArray
    /** Grains eaten so far at each frame. */
    val eatenAt = IntArray(FRAMES)
    /** Up to three grain indices scooped during each frame, -1 for none. */
    val flyers = Array(FRAMES) { IntArray(3) { -1 } }

    private val palette = IntArray(64) { i ->
        val c = jamColor(i / 8)
        val f = 0.66f + (i % 8) * 0.06f
        (if (f > 1f) c.tint((f - 1f) * 1.8f) else c.shade(f)).toArgb()
    }
    private val emptyArgb = Color(0xFFB8C2D3).toArgb()
    private val pixels = IntArray(GW * GH)
    private val bitmap: Bitmap = Bitmap.createBitmap(GW, GH, Bitmap.Config.ARGB_8888)
    private val image: ImageBitmap = bitmap.asImageBitmap()
    private val startBitmap: Bitmap = Bitmap.createBitmap(GW, GH, Bitmap.Config.ARGB_8888)
    val startImage: ImageBitmap = startBitmap.asImageBitmap()
    private var shown = Int.MIN_VALUE

    init {
        start = paint()
        val grid = start.copyOf()
        val rnd = Random(7)
        var eaten = 0
        var sweep = false
        val cell = PIC_W / GW
        val half = TRUCK_LEN / 2 / cell
        frames = Array(FRAMES) { k ->
            if (k < RIDE_FRAMES) {
                val truckX = MERGE_X + (BELT_END - MERGE_X) * k / (RIDE_FRAMES - 1)
                val cx = (truckX - PIC_LEFT) / cell
                val gx0 = ceil(cx - half).toInt().coerceAtLeast(0)
                val gx1 = floor(cx + half).toInt().coerceAtMost(GW - 1)
                var flown = 0
                repeat(EAT_PER_FRAME) {
                    var best = -1
                    var bestD = Float.MAX_VALUE
                    for (x in gx0..gx1) {
                        for (y in GH - 1 downTo GH - DEPTH) {
                            val g = grid[y * GW + x].toInt()
                            if (g >= 0 && g / 8 == eatColor) {
                                val d = abs(x - cx) + (GH - 1 - y) * 2f + rnd.nextFloat() * 3f
                                if (d < bestD) { bestD = d; best = y * GW + x }
                                break
                            }
                        }
                    }
                    if (best < 0) return@repeat
                    grid[best] = -1
                    eaten++
                    if (flown < 3 && rnd.nextFloat() < 0.5f) flyers[k][flown++] = best
                }
            }
            repeat(2) { sweep = !sweep; settle(grid, rnd, sweep) }
            eatenAt[k] = eaten
            grid.copyOf()
        }
        fill(start)
        startBitmap.setPixels(pixels, 0, GW, 0, 0, GW, GH)
    }

    /** The picture as it looks at [frame] (-1 = untouched). */
    fun imageAt(frame: Int): ImageBitmap {
        if (frame < 0) return startImage
        if (frame != shown) {
            fill(frames[frame])
            bitmap.setPixels(pixels, 0, GW, 0, 0, GW, GH)
            shown = frame
        }
        return image
    }

    fun grainColor(index: Int): Color = jamColor(start[index].toInt().let { if (it < 0) eatColor * 8 else it } / 8)

    private fun fill(src: ByteArray) {
        for (i in src.indices) {
            val g = src[i].toInt()
            pixels[i] = if (g < 0) emptyArgb else palette[g]
        }
    }

    /** One pass of falling sand, the same rule the game uses. */
    private fun settle(g: ByteArray, rnd: Random, sweep: Boolean) {
        for (y in GH - 2 downTo 0) {
            val row = y * GW
            val below = row + GW
            for (k in 0 until GW) {
                val x = if (sweep) k else GW - 1 - k
                val v = g[row + x]
                if (v < 0) continue
                if (g[below + x] < 0) {
                    g[below + x] = v; g[row + x] = -1
                    continue
                }
                val lFree = x > 0 && g[below + x - 1] < 0 && g[row + x - 1] < 0
                val rFree = x < GW - 1 && g[below + x + 1] < 0 && g[row + x + 1] < 0
                if (!lFree && !rFree) continue
                if (rnd.nextFloat() > SLIDE_CHANCE) continue
                val first = if ((x + y + (if (sweep) 1 else 0)) % 2 == 0) -1 else 1
                for (dx in intArrayOf(first, -first)) {
                    val nx = x + dx
                    if (nx < 0 || nx >= GW) continue
                    if (g[below + nx] < 0 && g[row + nx] < 0) {
                        g[below + nx] = v; g[row + x] = -1
                        break
                    }
                }
            }
        }
    }

    /** The game's painter, scaled down: smooth edges, brush-stroke shading and darker rims. */
    private fun paint(): ByteArray {
        val p = previewPic
        val rng = Random(3)
        val kf = min(GW.toFloat() / p.w, GH.toFloat() / p.h) * 0.9f
        val ox = (GW - p.w * kf) / 2f
        val oy = (GH - p.h * kf) / 2f
        fun colorAt(px: Int, py: Int): Int {
            val c = if (px in 0 until p.w && py in 0 until p.h) p.at(px, py) else -1
            return if (c < 0) p.background else c
        }
        val votes = FloatArray(8)
        val base = IntArray(GW * GH) { i ->
            val fx = (i % GW + 0.5f - ox) / kf - 0.5f
            val fy = (i / GW + 0.5f - oy) / kf - 0.5f
            val x0 = floor(fx).toInt()
            val y0 = floor(fy).toInt()
            val tx = fx - x0
            val ty = fy - y0
            votes.fill(0f)
            votes[colorAt(x0, y0)] += (1 - tx) * (1 - ty)
            votes[colorAt(x0 + 1, y0)] += tx * (1 - ty)
            votes[colorAt(x0, y0 + 1)] += (1 - tx) * ty
            votes[colorAt(x0 + 1, y0 + 1)] += tx * ty
            var best = 0
            for (c in 1 until 8) if (votes[c] > votes[best]) best = c
            best
        }
        return ByteArray(GW * GH) { i ->
            val x = i % GW
            val y = i / GW
            val c = base[i]
            fun diff(dx: Int, dy: Int): Boolean {
                val xx = x + dx
                val yy = y + dy
                return xx in 0 until GW && yy in 0 until GH && base[yy * GW + xx] != c
            }
            // Frequencies doubled from the game's because this grid is about half as fine.
            val stroke = sin(x * 0.32f + y * 0.18f + 2.2f * sin(y * 0.09f + x * 0.024f))
            val swirl = sin(x * 0.07f - y * 0.12f + 1.3f)
            var shade = 5f + stroke * 0.75f + swirl * 0.6f
            if (rng.nextInt(7) == 0) shade += if (rng.nextBoolean()) 0.6f else -0.6f
            when {
                diff(1, 0) || diff(-1, 0) || diff(0, 1) || diff(0, -1) -> shade -= 3.2f
                diff(-1, -1) -> shade += 1.4f
                diff(1, 1) -> shade -= 1.6f
                diff(-2, -2) -> shade += 0.7f
                diff(2, 2) -> shade -= 0.8f
            }
            (c * 8 + shade.roundToInt().coerceIn(0, 7)).toByte()
        }
    }
}

private fun DrawScope.sandPreview(t: Float) {
    val s = size.width
    val m = SandMini
    val cell = s * m.PIC_W / m.GW
    val picLeft = s * m.PIC_LEFT
    val picTop = s * 0.06f
    val picW = cell * m.GW
    val picH = cell * m.GH
    val picBottom = picTop + picH
    val beltTop = picBottom + s * 0.05f
    val beltBottom = beltTop + s * 0.12f
    val beltY = (beltTop + beltBottom) / 2
    val laneY = beltBottom + s * 0.065f
    val truckLen = s * m.TRUCK_LEN
    val truckWid = s * 0.11f
    val cyan = jamColor(m.eatColor)
    val twoPi = 2 * PI.toFloat()

    // --- Truck timeline ---
    val lotPos = Offset(s * 0.38f, s * 0.87f)
    val mergeFrom = Offset(s * 0.16f, laneY)
    val mergeTo = Offset(s * m.MERGE_X, beltY)
    val beltEnd = s * m.BELT_END
    val tap = seg(t, 0.03f, 0.11f)
    val ride = seg(t, 0.32f, 0.80f)
    var truckPos = lotPos
    var truckAngle = 270f
    var truckScale = 1f + 0.12f * bump(tap)
    when {
        t < 0.10f -> Unit
        t < 0.24f -> {
            // Out of the lot and round into the merge lane.
            val p0 = lotPos
            val p1 = Offset(s * 0.12f, lotPos.y)
            val p2 = Offset(s * 0.06f, laneY)
            val u = smooth(seg(t, 0.10f, 0.24f))
            truckPos = bezier(p0, p1, p2, mergeFrom, u)
            truckAngle = bezierHeading(p0, p1, p2, mergeFrom, u)
        }
        t < 0.32f -> {
            // The same S-curve lane change the game uses to get up onto the belt.
            val u = easeOut(seg(t, 0.24f, 0.32f))
            val c1 = Offset(mix(mergeFrom.x, mergeTo.x, 0.45f), mergeFrom.y)
            val c2 = Offset(mix(mergeFrom.x, mergeTo.x, 0.55f), mergeTo.y)
            truckPos = bezier(mergeFrom, c1, c2, mergeTo, u)
            truckAngle = bezierHeading(mergeFrom, c1, c2, mergeTo, u)
        }
        t < 0.80f -> {
            truckPos = Offset(mix(mergeTo.x, beltEnd, ride), beltY + sin(t * twoPi * 40f) * s * 0.004f)
            truckAngle = 90f
        }
        t < 0.88f -> {
            // Full: hops into the fast lane and races off.
            val k = seg(t, 0.80f, 0.88f)
            truckPos = Offset(beltEnd + easeIn(k) * s * 0.6f, beltY - min(1f, k * 3f) * s * 0.035f)
            truckAngle = 90f
        }
        else -> truckScale = easeOutBack(seg(t, 0.88f, 0.98f))
    }

    // --- Sand picture: play back the simulated frames ---
    val framePos = when {
        t < 0.32f -> -1f
        t < 0.80f -> ride * (m.RIDE_FRAMES - 1)
        else -> m.RIDE_FRAMES + seg(t, 0.80f, 0.86f) * (m.SETTLE_FRAMES - 1)
    }
    val frame = floor(framePos).toInt()
    drawRoundRect(Color.Black.copy(alpha = 0.15f), Offset(picLeft - s * 0.03f, picTop - s * 0.03f + 2f), Size(picW + s * 0.06f, picH + s * 0.05f), CornerRadius(s * 0.06f))
    drawRoundRect(Color.White, Offset(picLeft - s * 0.03f, picTop - s * 0.03f), Size(picW + s * 0.06f, picH + s * 0.05f), CornerRadius(s * 0.06f))
    val dstOffset = IntOffset(picLeft.roundToInt(), picTop.roundToInt())
    val dstSize = IntSize(picW.roundToInt(), picH.roundToInt())
    drawImage(m.imageAt(frame), IntOffset.Zero, IntSize(m.GW, m.GH), dstOffset, dstSize, filterQuality = FilterQuality.Low)
    // The picture is refilled for the next loop.
    val restore = smooth(seg(t, 0.86f, 0.96f))
    if (restore > 0f) {
        drawImage(m.startImage, IntOffset.Zero, IntSize(m.GW, m.GH), dstOffset, dstSize, alpha = restore, filterQuality = FilterQuality.Low)
    }

    // --- Belt: navy tray, studs and a dashed line moving at the truck's belt speed ---
    drawRect(Color(0xFF1B2850), Offset(0f, beltTop - s * 0.02f), Size(s, beltBottom - beltTop + s * 0.04f))
    drawRect(Brush.verticalGradient(listOf(Color(0xFFB3BBC8), Color(0xFF959FAF)), beltTop, beltBottom), Offset(0f, beltTop), Size(s, beltBottom - beltTop))
    val beltSpeed = (beltEnd - mergeTo.x) / 0.48f // px per loop
    val dash = beltSpeed / 10f
    var d = -dash + (t * beltSpeed) % dash
    while (d < s) {
        drawLine(Color.White.copy(alpha = 0.9f), Offset(d, beltY), Offset(d + dash * 0.5f, beltY), s * 0.016f, StrokeCap.Round)
        d += dash
    }
    val stud = beltSpeed / 20f
    var sx = -stud + (t * beltSpeed) % stud
    while (sx < s) {
        drawCircle(Color(0xFF6C7686), s * 0.008f, Offset(sx, beltTop + s * 0.018f))
        drawCircle(Color(0xFF6C7686), s * 0.008f, Offset(sx, beltBottom - s * 0.018f))
        sx += stud
    }
    for (leftSide in listOf(true, false)) {
        val bx = if (leftSide) 0f else s - s * 0.06f
        val o = Offset(bx, beltTop - s * 0.045f)
        val sz = Size(s * 0.06f, beltBottom - beltTop + s * 0.09f)
        drawRoundRect(Brush.horizontalGradient(listOf(Color(0xFF3D5AA8), Color(0xFF26397A)), o.x, o.x + sz.width), o, sz, CornerRadius(s * 0.03f))
        drawRoundRect(Color.White, o + Offset(2f, 2f), Size(sz.width - 4f, sz.height - 4f), CornerRadius(s * 0.025f), style = Stroke(1.5f))
    }

    // Merge lane with its chevrons, like in the game.
    val lw = truckWid + s * 0.03f
    drawRoundRect(Color(0xFFA9B2C0).copy(alpha = 0.8f), Offset(-lw / 2, laneY - lw / 2), Size(mergeTo.x + lw / 2 - s * 0.02f, lw), CornerRadius(lw / 2))
    var cx = s * 0.06f
    while (cx < mergeTo.x - s * 0.06f) {
        drawLine(Color.White, Offset(cx - s * 0.02f, laneY - s * 0.02f), Offset(cx, laneY), s * 0.012f, StrokeCap.Round)
        drawLine(Color.White, Offset(cx - s * 0.02f, laneY + s * 0.02f), Offset(cx, laneY), s * 0.012f, StrokeCap.Round)
        cx += s * 0.08f
    }

    // --- Grains flying from the bottom of the picture into the truck ---
    if (frame in 0 until m.RIDE_FRAMES) {
        val trail = 8
        for (j in max(0, frame - trail + 1)..frame) {
            val p = ((frame - j) + (framePos - frame)) / trail
            for (idx in m.flyers[j]) {
                if (idx < 0) continue
                val from = Offset(picLeft + (idx % m.GW + 0.5f) * cell, picTop + (idx / m.GW + 0.5f) * cell)
                val pos = mix(from, truckPos, easeIn(p))
                val sz = cell * (1.8f - 1.0f * p)
                drawRect(m.grainColor(idx).tint(0.15f), Offset(pos.x - sz / 2, pos.y - sz / 2), Size(sz, sz))
            }
        }
    }

    // --- The jam in the lot ---
    miniCar(Offset(s * 0.62f, s * 0.87f), jamColor(JamColor.RED.ordinal), 0f, truckLen, truckWid)
    miniCar(Offset(s * 0.84f, s * 0.87f), jamColor(JamColor.PINK.ordinal), 180f, truckLen, truckWid)
    tapRipple(lotPos, tap)

    // Speed lines while it races off.
    if (t in 0.80f..0.88f) {
        val k = seg(t, 0.80f, 0.86f)
        val back = truckPos.x - truckLen / 2
        for (j in -1..1) {
            val y = truckPos.y + j * s * 0.03f
            drawLine(Color.White.copy(alpha = 0.75f * k), Offset(back - s * 0.02f, y), Offset(back - s * (0.08f + 0.1f * k) - abs(j) * s * 0.03f, y), s * 0.014f, StrokeCap.Round)
        }
    }
    if (t in 0.80f..0.88f && truckPos.x > s * 1.2f) return
    miniCar(truckPos, cyan, truckAngle, truckLen, truckWid, truckScale)

    // The scooped sand heaped on its roof.
    val fill = when {
        t < 0.32f || t >= 0.88f -> 0f
        t >= 0.80f -> 1f
        else -> m.eatenAt[frame].toFloat() / max(1, m.eatenAt[m.RIDE_FRAMES - 1])
    }
    if (fill > 0.01f) {
        val f = 0.35f + 0.65f * sqrt(fill)
        val w = truckLen * 0.55f * f
        val h = truckWid * 0.62f * f
        val tl = Offset(truckPos.x - w / 2, truckPos.y - h / 2)
        drawOval(cyan.shade(0.6f), tl + Offset(0f, h * 0.18f), Size(w, h))
        drawOval(Brush.radialGradient(listOf(cyan.tint(0.35f), cyan, cyan.shade(0.82f)), truckPos - Offset(w * 0.15f, h * 0.25f), w * 0.6f), tl, Size(w, h))
    }
}
