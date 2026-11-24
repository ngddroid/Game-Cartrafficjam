package com.puzzel.lab.cartrafficjam.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.cartrafficjam.core.audio.Sfx
import com.puzzel.lab.cartrafficjam.core.data.PlayerState
import com.puzzel.lab.cartrafficjam.game.model.GameMode
import com.puzzel.lab.cartrafficjam.game.model.JamColor
import com.puzzel.lab.cartrafficjam.game.modes.PixelPictures
import com.puzzel.lab.cartrafficjam.game.render.jamColor
import com.puzzel.lab.cartrafficjam.game.render.shade
import com.puzzel.lab.cartrafficjam.game.render.tint
import com.puzzel.lab.cartrafficjam.ui.components.GameIcon
import com.puzzel.lab.cartrafficjam.ui.components.Icon
import com.puzzel.lab.cartrafficjam.ui.components.LocalAudio
import com.puzzel.lab.cartrafficjam.ui.components.OutlinedText
import com.puzzel.lab.cartrafficjam.ui.theme.GameText
import com.puzzel.lab.cartrafficjam.ui.theme.TitleFont
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The Modes tab: four big cards, one per way to play. */
@Composable
fun ModesTab(player: PlayerState, onPlay: (GameMode) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TabHeader("Game Modes", "Four ways to beat the traffic")
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(GameMode.entries.toList(), key = { it.name }) { m -> ModeCard(m, player.modeLevel(m), onPlay) }
        }
    }
}

@Composable
fun TabHeader(title: String, subtitle: String? = null) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF1C5BD0), Color(0xFF2F7CF6))))
            .statusBarsPadding()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedText(title, style = GameText.Title, fontSize = 28.sp, strokeWidth = 7f, outline = Color(0xFF0D2C6B))
            if (subtitle != null) Text(subtitle, style = GameText.Small.copy(color = Color.White, fontSize = 13.sp))
        }
    }
}

@Composable
private fun ModeCard(mode: GameMode, level: Int, onPlay: (GameMode) -> Unit) {
    val audio = LocalAudio.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val accent = Color(mode.accent)
    val dark = Color(mode.accentDark)
    Row(
        Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.97f else 1f)
            .drawBehind {
                drawRoundRect(Color(0xFF0D2C6B), Offset(0f, 5.dp.toPx()), size, CornerRadius(22.dp.toPx()))
                drawRoundRect(Color.White, Offset.Zero, size, CornerRadius(22.dp.toPx()))
            }
            .padding(5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFFFF8E7), Color(0xFFFFEFCB))))
            .clickable(interaction, null) {
                audio.play(Sfx.Click)
                onPlay(mode)
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Brush.verticalGradient(listOf(accent.tint(0.35f), accent))),
        ) {
            ModePreview(mode, Modifier.fillMaxSize())
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            OutlinedText(mode.title, style = GameText.Title, fontSize = 23.sp, strokeWidth = 6f, outline = dark)
            Text(
                mode.tagline,
                style = GameText.Small.copy(color = Color(0xFF6D4C41), fontSize = 12.5.sp, lineHeight = 15.sp),
                maxLines = 3,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Level $level",
                    style = GameText.Label.copy(color = Color.White, fontFamily = TitleFont, fontSize = 14.sp),
                    modifier = Modifier
                        .background(dark, RoundedCornerShape(10.dp))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                )
                Spacer(Modifier.weight(1f))
                Box(
                    Modifier
                        .size(46.dp, 40.dp)
                        .drawBehind {
                            val r = CornerRadius(14.dp.toPx())
                            drawRoundRect(Color(0xFF2E7D32), Offset(0f, 4.dp.toPx()), size, r)
                            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF9CE65C), Color(0xFF4CAF50))), Offset.Zero, Size(size.width, size.height - 2.dp.toPx()), r)
                        },
                    contentAlignment = Alignment.Center,
                ) { GameIcon(Icon.PLAY, 26.dp) }
            }
        }
    }
}

/** Little looping animation that shows what each mode is about. */
@Composable
private fun ModePreview(mode: GameMode, modifier: Modifier) {
    val anim = rememberInfiniteTransition(label = "preview")
    val t by anim.animateFloat(0f, 1f, infiniteRepeatable(tween(4000, easing = LinearEasing), RepeatMode.Restart), label = "t")
    Canvas(modifier) {
        when (mode) {
            GameMode.CLASSIC -> classicPreview(t)
            GameMode.LOOP -> loopPreview(t)
            GameMode.PIXEL -> pixelPreview(t)
            GameMode.SAND -> sandPreview(t)
        }
    }
}

private fun DrawScope.miniCar(c: Offset, color: Color, angle: Float, len: Float, wid: Float) {
    rotate(angle, c) {
        drawRoundRect(Color.Black.copy(alpha = 0.2f), Offset(c.x - wid / 2 + 2f, c.y - len / 2 + 3f), Size(wid, len), CornerRadius(wid * 0.3f))
        drawRoundRect(color.shade(0.7f), Offset(c.x - wid / 2, c.y - len / 2 + 2f), Size(wid, len), CornerRadius(wid * 0.3f))
        drawRoundRect(color, Offset(c.x - wid / 2, c.y - len / 2), Size(wid, len - 2f), CornerRadius(wid * 0.3f))
        drawRoundRect(Color(0xFFCFE9FF), Offset(c.x - wid * 0.32f, c.y - len * 0.28f), Size(wid * 0.64f, len * 0.18f), CornerRadius(3f))
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

private fun DrawScope.classicPreview(t: Float) {
    val s = size.width
    drawRoundRect(Color(0xFF7D838D), Offset(s * 0.08f, s * 0.3f), Size(s * 0.84f, s * 0.62f), CornerRadius(s * 0.08f))
    for (k in 0 until 4) drawRect(Color.White.copy(alpha = 0.8f), Offset(s * (0.12f + k * 0.2f), s * 0.06f), Size(s * 0.14f, s * 0.18f), style = Stroke(2f))
    val out = ((t * 2f) % 1f)
    val e = out * out
    miniCar(Offset(s * 0.3f, s * 0.62f - e * s * 0.5f), jamColor(JamColor.RED.ordinal), 0f, s * 0.3f, s * 0.15f)
    miniCar(Offset(s * 0.52f, s * 0.62f), jamColor(JamColor.BLUE.ordinal), 0f, s * 0.3f, s * 0.15f)
    miniCar(Offset(s * 0.74f, s * 0.6f), jamColor(JamColor.YELLOW.ordinal), 90f, s * 0.26f, s * 0.15f)
}

private fun DrawScope.loopPreview(t: Float) {
    val s = size.width
    val c = Offset(s / 2, s * 0.44f)
    val rx = s * 0.36f
    val ry = s * 0.26f
    drawOval(Color(0xFF8BCB5A), Offset(c.x - rx - 10f, c.y - ry - 10f), Size((rx + 10f) * 2, (ry + 10f) * 2))
    drawOval(Color(0xFF7D838D), Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2), style = Stroke(s * 0.12f))
    val colors = listOf(JamColor.RED, JamColor.YELLOW, JamColor.BLUE)
    for (i in 0 until 54) {
        val a = (i / 54f + t) * 2 * PI.toFloat()
        val lane = (i % 2) * 2 - 1
        val r = 1f + lane * 0.06f
        val p = Offset(c.x + cos(a) * rx * r, c.y + sin(a) * ry * r)
        val col = jamColor(colors[(i / 18) % 3].ordinal)
        drawCircle(col.shade(0.7f), s * 0.028f, p + Offset(0f, 1.5f))
        drawCircle(col, s * 0.026f, p)
    }
    miniCar(Offset(s * 0.5f, s * 0.86f), jamColor(JamColor.BLUE.ordinal), 0f, s * 0.24f, s * 0.13f)
}

private val previewPic = PixelPictures.all.first()

private fun DrawScope.pixelPreview(t: Float) {
    val p = previewPic
    val s = size.width
    val cell = s * 0.86f / p.w
    val ox = (s - cell * p.w) / 2
    val oy = s * 0.2f
    // Peel the outline away from the outside, then rebuild.
    val peel = (t * 1.6f).coerceAtMost(1f)
    for (y in 0 until p.h) for (x in 0 until p.w) {
        val c = p.at(x, y)
        if (c < 0) continue
        val edge = (x + y * 1.3f) / (p.w + p.h * 1.3f)
        if (c == JamColor.RED.ordinal && edge < peel) continue
        val col = jamColor(c)
        drawRoundRect(col.shade(0.7f), Offset(ox + x * cell, oy + y * cell + 1f), Size(cell - 1f, cell - 1f), CornerRadius(cell * 0.3f))
        drawRoundRect(col, Offset(ox + x * cell, oy + y * cell), Size(cell - 1f, cell - 1.5f), CornerRadius(cell * 0.3f))
    }
    miniCar(Offset(s * 0.5f, s * 0.1f), jamColor(JamColor.RED.ordinal), 180f, s * 0.16f, s * 0.12f)
}

private fun DrawScope.sandPreview(t: Float) {
    val s = size.width
    val top = s * 0.08f
    val h = s * 0.55f
    val left = s * 0.08f
    val w = s * 0.84f
    drawRoundRect(Color.White, Offset(left - 3f, top - 3f), Size(w + 6f, h + 6f), CornerRadius(8f))
    val cols = 20
    val cw = w / cols
    val eatX = ((t * 1.3f) % 1f) * (w + s * 0.2f) - s * 0.1f
    for (i in 0 until cols) {
        val x = left + i * cw
        val dip = (1f - ((x - left - eatX) / (s * 0.25f)).let { it * it }).coerceAtLeast(0f) * h * 0.18f
        val ch = h - dip
        drawRect(jamColor(JamColor.YELLOW.ordinal), Offset(x, top + dip), Size(cw + 0.5f, ch))
        val heart = h * (0.25f + 0.2f * sin(i / cols.toFloat() * PI.toFloat()))
        drawRect(jamColor(JamColor.RED.ordinal), Offset(x, top + h * 0.15f + dip), Size(cw + 0.5f, heart))
    }
    drawRect(Color(0xFF545D6B), Offset(0f, top + h + 4f), Size(s, s * 0.16f))
    drawLine(Color(0xFFFFD54F), Offset(0f, top + h + 4f + s * 0.08f), Offset(s, top + h + 4f + s * 0.08f), 2f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)))
    miniCar(Offset(left + eatX, top + h + 4f + s * 0.08f), jamColor(JamColor.YELLOW.ordinal), 90f, s * 0.22f, s * 0.12f)
    for (k in 0 until 3) {
        val gy = top + h + (k + 1) * 4f
        drawRect(jamColor(JamColor.YELLOW.ordinal), Offset(left + eatX - 4f + k * 3f, gy - 6f), Size(3f, 3f))
    }
}
