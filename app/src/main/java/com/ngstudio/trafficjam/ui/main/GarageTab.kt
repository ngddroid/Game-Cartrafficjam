package com.ngstudio.trafficjam.ui.main

import com.ngstudio.trafficjam.core.ads.AdConfig
import com.ngstudio.trafficjam.core.analytics.Analytics
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
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
import com.ngstudio.trafficjam.App
import com.ngstudio.trafficjam.core.audio.Sfx
import com.ngstudio.trafficjam.core.data.PlayerState
import com.ngstudio.trafficjam.game.model.JamColor
import com.ngstudio.trafficjam.game.model.VehicleType
import com.ngstudio.trafficjam.game.render.Skin
import com.ngstudio.trafficjam.game.render.VehicleArt
import com.ngstudio.trafficjam.game.render.jamColor
import com.ngstudio.trafficjam.game.render.shade
import com.ngstudio.trafficjam.game.render.tint
import com.ngstudio.trafficjam.ui.components.ButtonColor
import com.ngstudio.trafficjam.ui.components.CoinPill
import com.ngstudio.trafficjam.ui.components.GameButton
import com.ngstudio.trafficjam.ui.components.GameIcon
import com.ngstudio.trafficjam.ui.components.Icon
import com.ngstudio.trafficjam.ui.components.LocalAudio
import com.ngstudio.trafficjam.ui.components.OutlinedText
import com.ngstudio.trafficjam.ui.components.VehicleSprite
import com.ngstudio.trafficjam.ui.theme.GameText
import com.ngstudio.trafficjam.ui.theme.TitleFont
import com.ngstudio.trafficjam.ui.theme.LocalTabPalette
import com.ngstudio.trafficjam.ui.components.toyCard
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius

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
    val pal = LocalTabPalette.current
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
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        item(span = { GridItemSpan(2) }) {
            Column {
                GarageHeader(owned = player.ownedSkins.size, total = Skin.entries.size)
                Showroom(preview, type, color, player.coins)
                Row(
                    Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    VehicleType.entries.forEach { t ->
                        TypePill(t.name.lowercase().replaceFirstChar { it.uppercase() }, t == type) { audio.play(Sfx.Click); type = t }
                    }
                    Spacer(Modifier.weight(1f))
                }
                Box(Modifier.padding(horizontal = 12.dp)) {
                    ColorBar(color) { audio.play(Sfx.Click); color = it }
                }
                Spacer(Modifier.height(12.dp))
                SkinActions(preview, equipped, player, onBuy = {
                    if (progress.spendCoins(preview.price)) {
                        audio.play(Sfx.Unlock)
                        progress.buySkin(preview.id)
                        Analytics.spendCoins("skin_${preview.id}", preview.price)
                        Analytics.skinUnlocked(preview.id, "coins")
                    } else audio.play(Sfx.Warning)
                }, onAd = {
                    container.ads.showRewarded("skin_${preview.id}", onReward = {
                        progress.buySkin(preview.id)
                        Analytics.skinUnlocked(preview.id, "ad")
                    })
                }, onEquip = {
                    audio.play(Sfx.Unlock)
                    progress.selectSkin(preview.id)
                })
                Spacer(Modifier.height(10.dp))
                PaintPacksBanner()
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
    val pal = LocalTabPalette.current
    val density = LocalDensity.current
    val unitPx = with(density) { (if (type == VehicleType.BUS) 21.dp else 26.dp).toPx() }
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
            .height(210.dp),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            // Garage interior: walls, shutter door, lamps, tool chest, tyres, plant, tiled floor.
            drawGarageInterior(glow)
            // Neon turntable with yellow lights.
            val c = Offset(w / 2, h * 0.7f)
            val rx = w * 0.36f
            val ry = h * 0.16f
            drawTurntable(c, rx, ry, angle, glow)
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
        Column(Modifier.padding(start = 10.dp, top = 8.dp)) {
            LaurelTitle(skin.title, skin.tagline)
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
            skin == equipped -> EquippedBadge(Modifier.weight(1f).padding(horizontal = 14.dp))
            owned -> GameButton("Equip", onEquip, Modifier.weight(1f), color = ButtonColor.BLUE, height = 58.dp)
            else -> {
                GameButton(
                    "${skin.price}", onBuy, Modifier.weight(1f), color = ButtonColor.GREEN, height = 58.dp, fontSize = 22.sp,
                    enabled = player.coins >= skin.price, leading = { GameIcon(Icon.COIN, 30.dp) },
                )
                if (AdConfig.enabled) GameButton("Free", onAd, Modifier.weight(1f), color = ButtonColor.ORANGE, height = 58.dp, fontSize = 22.sp, leading = { GameIcon(Icon.AD, 30.dp) })
            }
        }
    }
}

@Composable
private fun PackCard(skin: Skin, owned: Boolean, equipped: Boolean, previewing: Boolean, onClick: () -> Unit) {
    val accent = skin.accent()
    val t = rememberInfiniteTransition(label = "card")
    val bob by t.animateFloat(-2f, 2f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "bob")
    // Gold frame with a glow for the pack being previewed, a white frame for the others.
    val frameTop = if (previewing) Color(0xFFFFE680) else Color.White
    val frameBottom = if (previewing) Color(0xFFFFB300) else Color(0xFFEDE4FF)
    val ink = if (previewing) Color(0xFFB86E00) else Color(0xFF5A3FB8)
    Column(
        Modifier
            .scale(if (previewing) 1.03f else 1f)
            .drawBehind {
                val r = CornerRadius(22.dp.toPx())
                if (previewing) {
                    drawRoundRect(Color(0xFFFFD54F).copy(alpha = 0.45f), Offset(-6.dp.toPx(), -6.dp.toPx()), Size(size.width + 12.dp.toPx(), size.height + 12.dp.toPx()), CornerRadius(r.x + 6.dp.toPx()))
                }
                drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(0f, 6.dp.toPx()), size, r)
                drawRoundRect(ink, Offset(-2.dp.toPx(), -2.dp.toPx()), Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()), CornerRadius(r.x + 2.dp.toPx()))
                drawRoundRect(Brush.verticalGradient(listOf(frameTop, frameBottom)), Offset.Zero, size, r)
            }
            .padding(5.dp)
            .clip(RoundedCornerShape(17.dp))
            .clickable(onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawSunburst(accent)
                drawOval(Color.White.copy(alpha = 0.35f), Offset(size.width * 0.12f, size.height * 0.55f), Size(size.width * 0.76f, size.height * 0.36f))
            }
            Row(Modifier.offset(y = bob.dp), verticalAlignment = Alignment.CenterVertically) {
                VehicleSprite(VehicleType.VAN, (skin.previewColor() + 3) % 8, skin, 13.dp, rotation = 60f, modifier = Modifier.offset(x = 14.dp, y = (-6).dp))
                VehicleSprite(VehicleType.CAR, skin.previewColor(), skin, 17.dp, rotation = 60f, modifier = Modifier.offset(x = (-26).dp, y = 8.dp))
            }
            if (equipped) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .padding(7.dp)
                        .drawBehind {
                            val r = CornerRadius(size.height / 2)
                            drawRoundRect(Color(0xFF1B5E20), Offset(0f, 2.dp.toPx()), size, r)
                            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFF6FD06A), Color(0xFF2E9E3A))), Offset.Zero, size, r)
                        }
                        .padding(horizontal = 9.dp, vertical = 2.dp),
                ) {
                    OutlinedText("IN USE", style = GameText.Button, fontSize = 13.sp, strokeWidth = 5f, outline = Color(0xFF1B5E20), depth = 0f)
                }
            }
        }
        // Cream name strip.
        Box(
            Modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFFBF0), Color(0xFFFFEFD2))))
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(skin.title, style = GameText.Label.copy(fontFamily = TitleFont, fontSize = 19.sp, color = Color(0xFF3B1E6E)), maxLines = 1)
        }
    }
}
