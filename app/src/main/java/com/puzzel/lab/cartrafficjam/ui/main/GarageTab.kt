package com.puzzel.lab.cartrafficjam.ui.main

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.cartrafficjam.App
import com.puzzel.lab.cartrafficjam.core.audio.Sfx
import com.puzzel.lab.cartrafficjam.core.data.PlayerState
import com.puzzel.lab.cartrafficjam.game.model.JamColor
import com.puzzel.lab.cartrafficjam.game.model.VehicleType
import com.puzzel.lab.cartrafficjam.game.render.Skin
import com.puzzel.lab.cartrafficjam.game.render.VehicleArt
import com.puzzel.lab.cartrafficjam.game.render.jamColor
import com.puzzel.lab.cartrafficjam.game.render.shade
import com.puzzel.lab.cartrafficjam.game.render.tint
import com.puzzel.lab.cartrafficjam.ui.components.ButtonColor
import com.puzzel.lab.cartrafficjam.ui.components.CoinPill
import com.puzzel.lab.cartrafficjam.ui.components.GameButton
import com.puzzel.lab.cartrafficjam.ui.components.GameIcon
import com.puzzel.lab.cartrafficjam.ui.components.Icon
import com.puzzel.lab.cartrafficjam.ui.components.LocalAudio
import com.puzzel.lab.cartrafficjam.ui.components.OutlinedText
import com.puzzel.lab.cartrafficjam.ui.components.VehicleSprite
import com.puzzel.lab.cartrafficjam.ui.theme.GameText
import com.puzzel.lab.cartrafficjam.ui.theme.TitleFont

/** Accent used for each pack's card so the grid reads as distinct collections. */
private fun Skin.accent() = when (this) {
    Skin.CLASSIC -> Color(0xFF2F8CF6)
    Skin.CANDY -> Color(0xFFFF6FB5)
    Skin.RETRO -> Color(0xFF26A69A)
    Skin.TAXI -> Color(0xFFFFB300)
    Skin.RACING -> Color(0xFFE53935)
    Skin.SAFARI -> Color(0xFF7CB342)
    Skin.CHROME -> Color(0xFF78909C)
    Skin.TOY -> Color(0xFFFF8A1F)
    Skin.NEON -> Color(0xFFAA00FF)
    Skin.GALAXY -> Color(0xFF3D2C8D)
    Skin.GOLDEN -> Color(0xFFFFA000)
}

private fun Skin.previewColor() = when (this) {
    Skin.TAXI, Skin.GOLDEN -> JamColor.YELLOW
    Skin.CANDY -> JamColor.PINK
    Skin.RACING -> JamColor.RED
    Skin.SAFARI -> JamColor.GREEN
    Skin.NEON -> JamColor.CYAN
    Skin.GALAXY -> JamColor.PURPLE
    Skin.RETRO -> JamColor.CYAN
    Skin.TOY -> JamColor.ORANGE
    else -> JamColor.BLUE
}.ordinal

@Composable
fun GarageTab(player: PlayerState) {
    val container = App.container
    val progress = container.progress
    val audio = LocalAudio.current
    val equipped = Skin.of(player.skin)
    var previewId by rememberSaveable { mutableStateOf(equipped.id) }
    val preview = Skin.of(previewId)
    var type by rememberSaveable { mutableStateOf(VehicleType.BUS) }
    var color by rememberSaveable { mutableIntStateOf(preview.previewColor()) }

    LazyVerticalGrid(
        GridCells.Fixed(2),
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                TabHeader("Garage", "${player.ownedSkins.size} of ${Skin.entries.size} paint packs owned")
                Showroom(preview, type, color, player.coins)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VehicleType.entries.forEach { t ->
                        val sel = t == type
                        Text(
                            t.name.lowercase().replaceFirstChar { it.uppercase() },
                            style = GameText.Label.copy(fontFamily = TitleFont, fontSize = 15.sp, color = if (sel) Color.White else Color(0xFF1A3A6B)),
                            modifier = Modifier
                                .background(if (sel) Color(0xFFFF8A1F) else Color.White, RoundedCornerShape(14.dp))
                                .border(2.dp, if (sel) Color(0xFFB34700) else Color(0xFFBFD3EE), RoundedCornerShape(14.dp))
                                .clickable { audio.play(Sfx.Click); type = t }
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.weight(1f))
                }
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    JamColor.entries.forEach { c ->
                        val sel = c.ordinal == color
                        Box(
                            Modifier
                                .size(if (sel) 34.dp else 28.dp)
                                .clip(CircleShape)
                                .background(Brush.verticalGradient(listOf(Color(c.hex).tint(0.3f), Color(c.hex))))
                                .border(if (sel) 3.dp else 2.dp, if (sel) Color.White else Color(0x55000000), CircleShape)
                                .clickable { audio.play(Sfx.Click); color = c.ordinal },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                SkinActions(preview, equipped, player, onBuy = {
                    if (progress.spendCoins(preview.price)) {
                        audio.play(Sfx.Unlock)
                        progress.buySkin(preview.id)
                    } else audio.play(Sfx.Warning)
                }, onAd = {
                    container.ads.showRewarded("skin_${preview.id}", onReward = { progress.buySkin(preview.id) })
                }, onEquip = {
                    audio.play(Sfx.Unlock)
                    progress.selectSkin(preview.id)
                })
                Spacer(Modifier.height(10.dp))
                Text(
                    "PAINT PACKS",
                    style = GameText.Label.copy(color = Color.White, fontFamily = TitleFont, fontSize = 18.sp),
                    modifier = Modifier.padding(start = 16.dp),
                )
            }
        }
        items(Skin.entries, key = { it.id }) { skin ->
            Box(Modifier.padding(start = if (skin.ordinal % 2 == 0) 14.dp else 0.dp, end = if (skin.ordinal % 2 == 1) 14.dp else 0.dp)) {
                PackCard(
                    skin = skin,
                    owned = skin.id in player.ownedSkins,
                    equipped = skin == equipped,
                    previewing = skin == preview,
                ) {
                    audio.play(Sfx.Click)
                    previewId = skin.id
                    color = skin.previewColor()
                }
            }
        }
    }
}

@Composable
private fun Showroom(skin: Skin, type: VehicleType, color: Int, coins: Int) {
    val density = LocalDensity.current
    val unitPx = with(density) { (if (type == VehicleType.BUS) 27.dp else 33.dp).toPx() }
    val art = remember(skin, unitPx) { VehicleArt(unitPx, skin) }
    val spin = rememberInfiniteTransition(label = "turntable")
    val angle by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing)), label = "a")
    val glow by spin.animateFloat(0.4f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "g")
    val pop = remember { Animatable(1f) }
    LaunchedEffect(skin, type, color) {
        pop.snapTo(0.85f)
        pop.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 500f))
    }
    val accent = skin.accent()
    Box(
        Modifier
            .fillMaxWidth()
            .height(250.dp)
            .background(Brush.verticalGradient(listOf(accent.shade(0.45f), Color(0xFF1B2440), Color(0xFF111827)))),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Spotlights
            for (k in listOf(0.18f, 0.82f)) {
                val p = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * k - 10f, 0f); lineTo(w * k + 10f, 0f); lineTo(w * 0.5f + (w * k - w * 0.5f) * 0.1f + 120f, h * 0.8f); lineTo(w * 0.5f + (w * k - w * 0.5f) * 0.1f - 120f, h * 0.8f); close()
                }
                drawPath(p, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f * glow), Color.Transparent), 0f, h * 0.8f))
            }
            // Back wall stripes
            for (k in 0 until 8) drawRect(Color.White.copy(alpha = 0.03f), Offset(k * w / 8f, 0f), Size(w / 16f, h * 0.55f))
            // Floor
            drawRect(Brush.verticalGradient(listOf(Color(0xFF2B3550), Color(0xFF151B2E)), h * 0.55f, h), Offset(0f, h * 0.55f), Size(w, h * 0.45f))
            // Turntable
            val c = Offset(w / 2, h * 0.72f)
            val rx = w * 0.4f
            val ry = h * 0.17f
            drawOval(Color.Black.copy(alpha = 0.45f), Offset(c.x - rx, c.y - ry + 12f), Size(rx * 2, ry * 2))
            drawOval(Color(0xFF3C4766), Offset(c.x - rx, c.y - ry + 8f), Size(rx * 2, ry * 2))
            drawOval(Brush.verticalGradient(listOf(Color(0xFF7D8BB0), Color(0xFF4A5677)), c.y - ry, c.y + ry), Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2))
            drawOval(accent.copy(alpha = 0.55f + 0.3f * glow), Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2), style = Stroke(4f))
            for (k in 0 until 16) {
                val a = Math.toRadians(k * 22.5 + angle)
                val p = Offset(c.x + (rx * 0.92f * kotlin.math.cos(a)).toFloat(), c.y + (ry * 0.92f * kotlin.math.sin(a)).toFloat())
                drawCircle(if (k % 2 == 0) Color(0xFFFFF59D) else accent.tint(0.4f), 3.5f, p)
            }
            // Vehicle
            withTransform({ scale(pop.value, pop.value, c) }) {
                val shadow = art.shadow(type)
                withTransform({
                    translate(c.x + unitPx * 0.2f, c.y + unitPx * 0.3f)
                    rotate(angle, Offset.Zero)
                }) { drawImage(shadow, Offset(-shadow.width / 2f, -shadow.height / 2f)) }
                if (skin == Skin.NEON) drawOval(jamColor(color).copy(alpha = 0.35f), Offset(c.x - rx * 0.55f, c.y - ry * 0.5f), Size(rx * 1.1f, ry))
                art.drawLive(this, type, color, angle, c)
            }
        }
        Column(Modifier.padding(14.dp)) {
            OutlinedText(skin.title, style = GameText.Title, fontSize = 26.sp, strokeWidth = 7f, outline = Color(0xFF0D1530), textAlign = TextAlign.Start)
            Text(skin.tagline, style = GameText.Small.copy(color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp))
        }
        CoinPill(coins, Modifier.align(Alignment.TopEnd).padding(10.dp))
    }
}

@Composable
private fun SkinActions(skin: Skin, equipped: Skin, player: PlayerState, onBuy: () -> Unit, onAd: () -> Unit, onEquip: () -> Unit) {
    val owned = skin.id in player.ownedSkins
    Row(
        Modifier.fillMaxWidth().padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when {
            skin == equipped -> Row(
                Modifier
                    .weight(1f)
                    .height(56.dp)
                    .background(Color(0xFFE8F5E9), RoundedCornerShape(28.dp))
                    .border(3.dp, Color(0xFF43A047), RoundedCornerShape(28.dp)),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameIcon(Icon.CHECK, 28.dp)
                Text("  Equipped on all vehicles", style = GameText.Label.copy(color = Color(0xFF2E7D32), fontSize = 16.sp))
            }
            owned -> GameButton("Equip", onEquip, Modifier.weight(1f), color = ButtonColor.BLUE, height = 58.dp)
            else -> {
                GameButton(
                    "${skin.price}", onBuy, Modifier.weight(1f), color = ButtonColor.GREEN, height = 58.dp, fontSize = 22.sp,
                    enabled = player.coins >= skin.price, leading = { GameIcon(Icon.COIN, 30.dp) },
                )
                GameButton("Free", onAd, Modifier.weight(1f), color = ButtonColor.ORANGE, height = 58.dp, fontSize = 22.sp, leading = { GameIcon(Icon.AD, 30.dp) })
            }
        }
    }
}

@Composable
private fun PackCard(skin: Skin, owned: Boolean, equipped: Boolean, previewing: Boolean, onClick: () -> Unit) {
    val accent = skin.accent()
    val t = rememberInfiniteTransition(label = "card")
    val bob by t.animateFloat(-2f, 2f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "bob")
    Column(
        Modifier
            .scale(if (previewing) 1.03f else 1f)
            .drawBehind {
                val r = CornerRadius(20.dp.toPx())
                drawRoundRect(Color(0xFF0D2C6B), Offset(0f, 5.dp.toPx()), size, r)
                drawRoundRect(if (previewing) Color(0xFFFFD54F) else Color.White, Offset.Zero, size, r)
            }
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(116.dp)
                .background(Brush.verticalGradient(listOf(accent.tint(0.45f), accent.tint(0.15f)))),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawOval(Color.White.copy(alpha = 0.35f), Offset(size.width * 0.12f, size.height * 0.55f), Size(size.width * 0.76f, size.height * 0.36f))
            }
            Row(Modifier.offset(y = bob.dp), verticalAlignment = Alignment.CenterVertically) {
                VehicleSprite(VehicleType.VAN, (skin.previewColor() + 3) % 8, skin, 12.dp, rotation = 60f, modifier = Modifier.offset(x = 14.dp, y = (-6).dp))
                VehicleSprite(VehicleType.CAR, skin.previewColor(), skin, 16.dp, rotation = 60f, modifier = Modifier.offset(x = (-26).dp, y = 8.dp))
            }
            if (equipped) {
                Text(
                    "IN USE",
                    style = GameText.Small.copy(color = Color.White, fontFamily = TitleFont, fontSize = 11.sp),
                    modifier = Modifier.align(Alignment.TopStart).padding(6.dp).background(Color(0xFF43A047), RoundedCornerShape(8.dp)).padding(horizontal = 6.dp, vertical = 1.dp),
                )
            }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFFFFF8E7)).padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(skin.title, style = GameText.Label.copy(fontFamily = TitleFont, fontSize = 16.sp, color = Color(0xFF3E2723)), maxLines = 1)
            Spacer(Modifier.height(2.dp))
            if (owned) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GameIcon(Icon.CHECK, 16.dp)
                    Text(" Owned", style = GameText.Small.copy(color = Color(0xFF2E7D32)))
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    GameIcon(Icon.COIN, 16.dp)
                    Text(" ${skin.price}", style = GameText.Small.copy(color = Color(0xFF8A4B14), fontFamily = TitleFont))
                    Spacer(Modifier.width(6.dp))
                    Text("or", style = GameText.Small.copy(color = Color(0xFF8A4B14)))
                    Spacer(Modifier.width(4.dp))
                    GameIcon(Icon.AD, 16.dp)
                }
            }
        }
    }
}
