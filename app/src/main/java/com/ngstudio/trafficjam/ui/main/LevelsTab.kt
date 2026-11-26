package com.ngstudio.trafficjam.ui.main

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import com.ngstudio.trafficjam.game.render.Backdrop
import com.ngstudio.trafficjam.game.render.SceneTheme
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.trafficjam.core.audio.Sfx
import com.ngstudio.trafficjam.core.data.LEVELS_PER_COLLECTION
import com.ngstudio.trafficjam.core.data.PlayerState
import com.ngstudio.trafficjam.core.data.TOTAL_COLLECTIONS
import com.ngstudio.trafficjam.core.data.collectionOf
import com.ngstudio.trafficjam.game.render.Themes
import com.ngstudio.trafficjam.game.render.shade
import com.ngstudio.trafficjam.game.render.tint
import com.ngstudio.trafficjam.ui.components.GameIcon
import com.ngstudio.trafficjam.ui.components.Icon
import com.ngstudio.trafficjam.ui.components.LocalAudio
import com.ngstudio.trafficjam.ui.components.Mascot
import com.ngstudio.trafficjam.ui.components.OutlinedText
import com.ngstudio.trafficjam.ui.components.WorldArt
import com.ngstudio.trafficjam.ui.theme.GameText
import com.ngstudio.trafficjam.ui.theme.TitleFont
import kotlin.math.sin
import com.ngstudio.trafficjam.ui.theme.LocalTabPalette
import com.ngstudio.trafficjam.ui.components.ToySign
import androidx.compose.runtime.getValue
import com.ngstudio.trafficjam.core.data.TOTAL_LEVELS

private const val ROW_DP = 92
private const val ITEMS_PER_COLLECTION = LEVELS_PER_COLLECTION + 1

private fun nodeX(level: Int) = 0.5f + 0.27f * sin(level * 0.8f)

private fun indexOfLevel(level: Int): Int {
    val c = collectionOf(level)
    return (c - 1) * ITEMS_PER_COLLECTION + 1 + (level - 1) % LEVELS_PER_COLLECTION
}

@Composable
fun LevelsTab(player: PlayerState, scrollToCollection: Int, onScrolled: () -> Unit, onLevel: (Int) -> Unit) {
    val pal = LocalTabPalette.current
    val current = player.nextLevel
    val list = rememberLazyListState(initialFirstVisibleItemIndex = (indexOfLevel(current) - 3).coerceAtLeast(0))
    LaunchedEffect(scrollToCollection) {
        if (scrollToCollection > 0) {
            list.animateScrollToItem((scrollToCollection - 1) * ITEMS_PER_COLLECTION)
            onScrolled()
        }
    }

    Column(Modifier.fillMaxSize()) {
        // Sky header with the wooden Level Map sign; the map scrolls underneath it.
        LevelsHeader(stars = player.stars.sum(), cleared = player.completedCount, total = TOTAL_LEVELS)
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list) {
            for (c in 1..TOTAL_COLLECTIONS) {
                item(key = "h$c", contentType = "header") { WorldBanner(c, player) }
                items(LEVELS_PER_COLLECTION, key = { "l${(c - 1) * LEVELS_PER_COLLECTION + it + 1}" }, contentType = { "level" }) { i ->
                    val level = (c - 1) * LEVELS_PER_COLLECTION + i + 1
                    LevelRow(level, player, current, onLevel)
                }
            }
        }
    }
}

@Composable
private fun LevelRow(level: Int, player: PlayerState, current: Int, onLevel: (Int) -> Unit) {
    val theme = Themes.forLevel(level)
    val idxInWorld = (level - 1) % LEVELS_PER_COLLECTION
    val prevX = if (idxInWorld == 0) 0.5f else nodeX(level - 1)
    val x = nodeX(level)
    val nextX = if (idxInWorld == LEVELS_PER_COLLECTION - 1) 0.5f else nodeX(level + 1)
    val ground = theme.outside
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(ROW_DP.dp)
            .drawBehind {
                drawRect(Brush.horizontalGradient(listOf(ground.shade(0.9f), ground, ground.shade(0.9f))))
                val rnd = kotlin.random.Random(level * 7919)
                repeat(10) {
                    val c = if (rnd.nextBoolean()) ground.tint(0.12f) else ground.shade(0.88f)
                    val p = Offset(rnd.nextFloat() * size.width, rnd.nextFloat() * size.height)
                    drawLine(c, p, p + Offset(1.5f, -4.dp.toPx()), 1.2.dp.toPx(), StrokeCap.Round)
                }
                groundDetail(Offset(size.width * (0.06f + rnd.nextFloat() * 0.88f), size.height * (0.15f + rnd.nextFloat() * 0.7f)), ground, rnd.nextInt(4), x)
                // Scenery on the side away from the road
                val side = if (x > 0.5f) 0.16f else 0.84f
                mapProp(theme, Offset(size.width * (side + (rnd.nextFloat() - 0.5f) * 0.12f), size.height * 0.62f), level)
                if (rnd.nextFloat() < 0.5f) {
                    val fx = size.width * (if (side < 0.5f) 0.04f + rnd.nextFloat() * 0.06f else 0.9f + rnd.nextFloat() * 0.06f)
                    drawCircle(listOf(Color.White, Color(0xFFFFEB3B), Color(0xFFFF80AB))[level % 3], 2.5.dp.toPx(), Offset(fx, size.height * 0.3f))
                }
                val top = Offset((prevX + x) / 2 * size.width, 0f)
                val mid = Offset(x * size.width, size.height / 2)
                val bot = Offset((x + nextX) / 2 * size.width, size.height)
                // The road starts at a world's first node and ends at its last one (rounded ends),
                // so it never runs into the world banner and gets cut off.
                val firstInWorld = idxInWorld == 0
                val lastInWorld = idxInWorld == LEVELS_PER_COLLECTION - 1
                val road = Path().apply {
                    val k = size.height * 0.28f
                    if (firstInWorld) moveTo(mid.x, mid.y) else {
                        moveTo(top.x, top.y)
                        cubicTo(top.x, top.y + k, mid.x, mid.y - k, mid.x, mid.y)
                    }
                    if (!lastInWorld) cubicTo(mid.x, mid.y + k, bot.x, bot.y - k, bot.x, bot.y)
                }
                if (!(firstInWorld && lastInWorld)) {
                    val cap = if (firstInWorld || lastInWorld) StrokeCap.Round else StrokeCap.Butt
                    drawPath(road, Color.Black.copy(alpha = 0.15f), style = Stroke(46.dp.toPx(), cap = cap))
                    drawPath(road, Color(0xFFF1F3F6), style = Stroke(42.dp.toPx(), cap = cap))
                    drawPath(road, Brush.horizontalGradient(listOf(theme.road.tint(0.08f), theme.road, theme.road.shade(0.9f))), style = Stroke(34.dp.toPx(), cap = cap))
                    drawPath(
                        road, Color.White.copy(alpha = 0.85f),
                        style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10.dp.toPx(), 10.dp.toPx()))),
                    )
                }
            },
    ) {
        val w = maxWidth
        Box(Modifier.offset(x = w * x - 39.dp, y = 7.dp)) {
            LevelNode(level, player, current, onLevel)
        }
        if (level == current) {
            Mascot(56.dp, Modifier.offset(x = (w * x + 30.dp).coerceAtMost(w - 58.dp), y = 18.dp))
        }
    }
}

@Composable
private fun LevelNode(level: Int, player: PlayerState, current: Int, onLevel: (Int) -> Unit) {
    val audio = LocalAudio.current
    val unlocked = player.isLevelUnlocked(level)
    val stars = player.starsOf(level)
    val isCurrent = level == current
    val base = when {
        isCurrent -> Color(0xFFFF9800)
        stars > 0 -> Color(0xFF5FC12F)
        unlocked -> Color(0xFF8E5CF0)
        else -> Color(0xFFA9B1BE)
    }
    val pulse = rememberInfiniteTransition(label = "node")
    val s by pulse.animateFloat(1f, if (isCurrent) 1.1f else 1f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "s")
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        Modifier
            .size(78.dp)
            .scale(s * if (pressed) 0.9f else 1f)
            .clickable(interaction, null) { audio.play(Sfx.Click); onLevel(level) },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(66.dp)
                .drawBehind {
                    val r = size.minDimension / 2
                    if (isCurrent) {
                        // Soft glowing halo and a bright ring around the current level.
                        drawCircle(Brush.radialGradient(listOf(Color(0xAAFFF176), Color.Transparent), center, r + 16.dp.toPx()), r + 16.dp.toPx(), center)
                        drawCircle(Color(0xFFFFF176), r + 5.dp.toPx(), center, style = Stroke(3.5f.dp.toPx()))
                    }
                    drawCircle(Color.Black.copy(alpha = 0.28f), r, center + Offset(0f, 6.dp.toPx()))
                    drawCircle(base.shade(0.55f), r, center + Offset(0f, 2.5f.dp.toPx()))
                    drawCircle(Color.White, r, center)
                    drawCircle(base.shade(0.6f), r - 4.5f.dp.toPx(), center)
                    drawCircle(Brush.verticalGradient(listOf(base.tint(0.4f), base, base.shade(0.82f))), r - 6.dp.toPx(), center)
                    drawOval(
                        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.White.copy(alpha = 0.1f)), r * 0.2f, r * 0.85f),
                        Offset(r * 0.42f, r * 0.2f), Size(r * 1.16f, r * 0.62f),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            if (unlocked) {
                OutlinedText("$level", style = GameText.Title, fontSize = if (level >= 1000) 17.sp else 22.sp, strokeWidth = 6f, outline = base.shade(0.4f))
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    GameIcon(Icon.LOCK, 26.dp)
                    Text("$level", style = GameText.Small.copy(color = Color.White, fontFamily = TitleFont, fontSize = 11.sp))
                }
            }
        }
        if (stars > 0) {
            Row(Modifier.align(Alignment.BottomCenter).offset(y = 6.dp)) {
                for (i in 0 until 3) GameIcon(if (i < stars) Icon.STAR else Icon.STAR_EMPTY, 18.dp)
            }
        }
        if (unlocked && stars == 0 && !isCurrent) {
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFC107))
                    .border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { GameIcon(Icon.PLAY, 12.dp) }
        }
    }
}

/** A small piece of world-appropriate scenery beside the map road. */
private fun DrawScope.mapProp(theme: SceneTheme, c: Offset, seed: Int) {
    val u = 1.dp.toPx()
    drawOval(Color.Black.copy(alpha = 0.15f), Offset(c.x - 14 * u, c.y + 8 * u), Size(28 * u, 7 * u))
    when (theme.backdrop) {
        Backdrop.SNOW -> {
            for (k in 0 until 3) {
                val y0 = c.y + 6 * u - k * 10 * u
                val wdt = (26 - k * 6) * u
                drawPath(Path().apply { moveTo(c.x, y0 - 16 * u); lineTo(c.x + wdt / 2, y0); lineTo(c.x - wdt / 2, y0); close() }, Color(0xFF2E7D32))
                drawPath(Path().apply { moveTo(c.x, y0 - 16 * u); lineTo(c.x + wdt / 4, y0 - 8 * u); lineTo(c.x - wdt / 4, y0 - 8 * u); close() }, Color.White)
            }
        }
        Backdrop.DESERT -> {
            drawRoundRect(Color(0xFF558B2F), Offset(c.x - 5 * u, c.y - 26 * u), Size(10 * u, 34 * u), CornerRadius(5 * u))
            drawRoundRect(Color(0xFF558B2F), Offset(c.x - 15 * u, c.y - 16 * u), Size(8 * u, 14 * u), CornerRadius(4 * u))
            drawRoundRect(Color(0xFF558B2F), Offset(c.x + 7 * u, c.y - 20 * u), Size(8 * u, 14 * u), CornerRadius(4 * u))
        }
        Backdrop.SPACE -> {
            drawOval(Color(0xFF2F3656), Offset(c.x - 16 * u, c.y - 4 * u), Size(32 * u, 12 * u))
            drawOval(Color(0xFF1B2140), Offset(c.x - 12 * u, c.y - 2 * u), Size(24 * u, 7 * u))
            drawCircle(Color(0xFF00E5FF).copy(alpha = 0.7f), 2 * u, Offset(c.x + 10 * u, c.y - 18 * u))
        }
        Backdrop.NEON -> {
            drawRect(Color(0xFF37474F), Offset(c.x - 1.5f * u, c.y - 30 * u), Size(3 * u, 36 * u))
            drawCircle(Brush.radialGradient(listOf(Color(0x88E040FB), Color.Transparent), Offset(c.x, c.y - 32 * u), 18 * u), 18 * u, Offset(c.x, c.y - 32 * u))
            drawCircle(Color(0xFFF8BBFF), 4 * u, Offset(c.x, c.y - 32 * u))
        }
        Backdrop.FAIR -> {
            val cols = listOf(Color(0xFFFF4081), Color(0xFFFFEB3B), Color(0xFF29B6F6))
            for (k in 0 until 3) {
                val b = Offset(c.x + (k - 1) * 9 * u, c.y - (24 + (k % 2) * 6) * u)
                drawLine(Color.White.copy(alpha = 0.7f), b, c + Offset(0f, 4 * u), 0.8f * u)
                drawOval(cols[(seed + k) % 3], b - Offset(6 * u, 8 * u), Size(12 * u, 15 * u))
            }
        }
        Backdrop.PIER -> {
            drawOval(Color(0xFF29B6F6), Offset(c.x - 18 * u, c.y - 6 * u), Size(36 * u, 14 * u))
            drawPath(Path().apply { moveTo(c.x - 8 * u, c.y - 4 * u); lineTo(c.x + 8 * u, c.y - 4 * u); lineTo(c.x + 5 * u, c.y + 1 * u); lineTo(c.x - 5 * u, c.y + 1 * u); close() }, Color(0xFFFF7043))
            drawPath(Path().apply { moveTo(c.x, c.y - 20 * u); lineTo(c.x + 7 * u, c.y - 5 * u); lineTo(c.x, c.y - 5 * u); close() }, Color.White)
        }
        else -> {
            val leaf = when (theme.backdrop) {
                Backdrop.FARM -> Color(0xFFFF8F00)
                Backdrop.JUNGLE -> Color(0xFF2E7D32)
                else -> Color(0xFF43A047)
            }
            // Big fluffy cartoon tree: trunk, layered canopy and a highlight.
            drawOval(Color.Black.copy(alpha = 0.14f), Offset(c.x - 26 * u, c.y + 4 * u), Size(52 * u, 12 * u))
            drawRoundRect(Color(0xFF5D3A24), Offset(c.x - 5 * u, c.y - 16 * u), Size(10 * u, 26 * u), CornerRadius(3 * u))
            drawRoundRect(Color(0xFF8D5A3B), Offset(c.x - 3 * u, c.y - 16 * u), Size(4 * u, 24 * u), CornerRadius(2 * u))
            drawCircle(leaf.shade(0.7f), 24 * u, c + Offset(0f, -30 * u))
            drawCircle(leaf.shade(0.85f), 17 * u, c + Offset(-15 * u, -24 * u))
            drawCircle(leaf.shade(0.85f), 17 * u, c + Offset(15 * u, -24 * u))
            drawCircle(leaf, 20 * u, c + Offset(-3 * u, -35 * u))
            drawCircle(leaf.tint(0.2f), 13 * u, c + Offset(-8 * u, -40 * u))
            drawCircle(leaf.tint(0.45f), 5 * u, c + Offset(-11 * u, -45 * u))
        }
    }
}

/** A small touch of life on the grass: a tuft, a daisy, a pair of rocks or a bush, away from the road. */
private fun DrawScope.groundDetail(at: Offset, ground: Color, kind: Int, roadX: Float) {
    val u = 1.dp.toPx()
    // Keep clear of the road.
    if (kotlin.math.abs(at.x / size.width - roadX) < 0.22f) return
    when (kind) {
        0 -> for (k in -1..1) drawLine(ground.shade(0.7f), at, at + Offset(k * 5 * u, -9 * u), 2 * u, StrokeCap.Round)
        1 -> {
            drawLine(Color(0xFF3E8E2E), at, at + Offset(0f, 9 * u), 1.5f * u)
            for (k in 0 until 5) {
                val a = k * 72.0 * Math.PI / 180.0
                drawCircle(Color.White, 3 * u, at + Offset((kotlin.math.cos(a) * 4 * u).toFloat(), (kotlin.math.sin(a) * 4 * u).toFloat()))
            }
            drawCircle(Color(0xFFFFC21A), 2.4f * u, at)
        }
        2 -> {
            drawOval(Color.Black.copy(alpha = 0.12f), Offset(at.x - 16 * u, at.y + 5 * u), Size(32 * u, 7 * u))
            drawOval(Color(0xFF8E8A85), Offset(at.x - 14 * u, at.y - 8 * u), Size(20 * u, 15 * u))
            drawOval(Color(0xFFB9B4AC), Offset(at.x - 12 * u, at.y - 8 * u), Size(15 * u, 9 * u))
            drawOval(Color(0xFF9E9A94), Offset(at.x + 2 * u, at.y - 3 * u), Size(13 * u, 10 * u))
        }
        else -> {
            drawCircle(ground.shade(0.62f), 11 * u, at + Offset(-8 * u, 2 * u))
            drawCircle(ground.shade(0.72f), 13 * u, at)
            drawCircle(ground.shade(0.62f), 10 * u, at + Offset(10 * u, 3 * u))
            drawCircle(ground.tint(0.15f), 4 * u, at + Offset(-3 * u, -6 * u))
        }
    }
}
