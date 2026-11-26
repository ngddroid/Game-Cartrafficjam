package com.puzzel.lab.cartrafficjam.ui.main

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.cartrafficjam.App
import com.puzzel.lab.cartrafficjam.core.audio.Sfx
import com.puzzel.lab.cartrafficjam.core.data.LEVELS_PER_COLLECTION
import com.puzzel.lab.cartrafficjam.core.data.PlayerState
import com.puzzel.lab.cartrafficjam.core.data.TOTAL_LEVELS
import com.puzzel.lab.cartrafficjam.core.data.collectionOf
import com.puzzel.lab.cartrafficjam.game.model.JamColor
import com.puzzel.lab.cartrafficjam.game.model.VehicleType
import com.puzzel.lab.cartrafficjam.game.render.Skin
import com.puzzel.lab.cartrafficjam.game.render.Themes
import com.puzzel.lab.cartrafficjam.ui.components.AvatarTile
import com.puzzel.lab.cartrafficjam.ui.components.ButtonColor
import com.puzzel.lab.cartrafficjam.ui.components.CoinPill
import com.puzzel.lab.cartrafficjam.ui.components.GameButton
import com.puzzel.lab.cartrafficjam.ui.components.GameDialog
import com.puzzel.lab.cartrafficjam.ui.components.GameIcon
import com.puzzel.lab.cartrafficjam.ui.components.GameLogo
import com.puzzel.lab.cartrafficjam.ui.components.GamePanel
import com.puzzel.lab.cartrafficjam.ui.components.Icon
import com.puzzel.lab.cartrafficjam.ui.components.LocalAudio
import com.puzzel.lab.cartrafficjam.ui.components.Mascot
import com.puzzel.lab.cartrafficjam.ui.components.OutlinedText
import com.puzzel.lab.cartrafficjam.ui.components.RoundButton
import com.puzzel.lab.cartrafficjam.ui.components.VehicleSprite
import com.puzzel.lab.cartrafficjam.ui.components.WorldArt
import com.puzzel.lab.cartrafficjam.ui.theme.GameText
import com.puzzel.lab.cartrafficjam.ui.theme.TitleFont
import com.puzzel.lab.cartrafficjam.ui.theme.Sunset

private val DAILY = listOf(20, 30, 40, 60, 80, 100, 200)

private var dailyShownThisSession = false

@Composable
fun HomeTab(player: PlayerState, onPlay: () -> Unit, onSettings: () -> Unit, onWorld: (Int) -> Unit) {
    val progress = App.container.progress
    var showDaily by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(player.loaded) {
        if (player.loaded && progress.dailyAvailable && !dailyShownThisSession && player.completedCount > 0) {
            dailyShownThisSession = true
            showDaily = true
        }
    }
    val next = player.nextLevel
    val world = collectionOf(next)
    val theme = Themes.forCollection(world)

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AvatarTile(54.dp)
                Spacer(Modifier.width(10.dp))
                CoinPill(player.coins)
                Spacer(Modifier.weight(1f))
                RoundButton(onSettings, size = 48.dp) { GameIcon(Icon.GEAR, 32.dp) }
            }
            Spacer(Modifier.height(4.dp))
            GameLogo(Modifier.align(Alignment.CenterHorizontally), scale = 0.82f)

            // Current world card
            Box(
                Modifier
                    .padding(horizontal = 26.dp)
                    .fillMaxWidth()
                    .offset(y = (-14).dp)
                    .drawBehind {
                        drawRoundRect(Sunset.Outline, Offset(0f, 6.dp.toPx()), size, CornerRadius(24.dp.toPx()))
                        drawRoundRect(Color.White, Offset.Zero, size, CornerRadius(24.dp.toPx()))
                    }
                    .padding(5.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onWorld(world) },
            ) {
                WorldArt(theme, Modifier.fillMaxWidth().height(132.dp))
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Sunset.Outline.copy(alpha = 0.8f))))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedText(Themes.collectionName(world), style = GameText.Title, fontSize = 20.sp, strokeWidth = 6f, textAlign = TextAlign.Start)
                        Spacer(Modifier.weight(1f))
                        GameIcon(Icon.STAR, 20.dp)
                        Text(" ${player.starsIn(world)}", style = GameText.Label.copy(color = Color.White, fontFamily = TitleFont))
                    }
                    Spacer(Modifier.height(4.dp))
                    val done = player.completedIn(world)
                    Box(
                        Modifier.fillMaxWidth().height(14.dp).background(Color(0x66000000), CircleShape).padding(2.dp),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(done / LEVELS_PER_COLLECTION.toFloat())
                                .height(10.dp)
                                .background(Brush.horizontalGradient(listOf(Color(0xFFB2FF59), Color(0xFF64DD17))), CircleShape),
                        )
                    }
                    Text(
                        "World $world · $done / $LEVELS_PER_COLLECTION levels",
                        style = GameText.Small.copy(color = Color.White.copy(alpha = 0.9f)),
                    )
                }
            }

            Spacer(Modifier.weight(1f))
            TrafficStrip()
            Spacer(Modifier.height(10.dp))

            // Play button
            val pulse = rememberInfiniteTransition(label = "play")
            val s by pulse.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s")
            GameButton(
                if (next >= TOTAL_LEVELS && player.isCompleted(TOTAL_LEVELS)) "Replay $next" else "Level $next",
                onPlay,
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(250.dp)
                    .scale(s),
                color = ButtonColor.YELLOW,
                height = 78.dp,
                fontSize = 36.sp,
            )
            Spacer(Modifier.height(18.dp))
        }

        // Side buttons
        Column(Modifier.align(Alignment.CenterStart).padding(start = 10.dp).offset(y = 112.dp)) {
            SideButton(Icon.GIFT, "Daily", badge = progress.dailyAvailable) { showDaily = true }
        }
        Mascot(96.dp, Modifier.align(Alignment.CenterEnd).padding(end = 8.dp).offset(y = 112.dp))
    }

    DailyDialog(showDaily, onClose = { showDaily = false })
}

@Composable
private fun SideButton(icon: Icon, label: String, badge: Boolean, onClick: () -> Unit) {
    val audio = LocalAudio.current
    val t = rememberInfiniteTransition(label = "side")
    val r by t.animateFloat(-8f, 8f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "r")
    Box(Modifier.size(78.dp).clickable { audio.play(Sfx.Click); onClick() }) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.align(Alignment.Center)) {
            Box(
                Modifier
                    .size(62.dp)
                    .background(Brush.verticalGradient(listOf(Color(0xFFFFE9B0), Color(0xFFFFC46B))), RoundedCornerShape(18.dp))
                    .border(3.dp, Color.White, RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) { GameIcon(icon, 48.dp, Modifier.graphicsLayer { rotationZ = if (badge) r else 0f }) }
            OutlinedText(label, Modifier.offset(y = (-8).dp), style = GameText.Button, fontSize = 15.sp, strokeWidth = 5f)
        }
        if (badge) {
            Box(
                Modifier.align(Alignment.TopEnd).size(20.dp).background(Color(0xFFF2453D), CircleShape).border(2.dp, Color.White, CircleShape),
                contentAlignment = Alignment.Center,
            ) { Text("!", style = GameText.Button.copy(fontSize = 13.sp)) }
        }
    }
}

/** A strip of road with a few vehicles cruising past. */
@Composable
private fun TrafficStrip() {
    val t = rememberInfiniteTransition(label = "traffic")
    val a by t.animateFloat(0f, 1f, infiniteRepeatable(tween(5200, easing = LinearEasing)), label = "a")
    val b by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3800, easing = LinearEasing)), label = "b")
    Box(Modifier.fillMaxWidth().height(96.dp)) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Color(0xFF8BC34A), Offset(0f, 0f), Size(size.width, 10.dp.toPx()))
            drawRect(Color(0xFF5B6270), Offset(0f, 10.dp.toPx()), Size(size.width, 76.dp.toPx()))
            drawRect(Color.White, Offset(0f, 12.dp.toPx()), Size(size.width, 2.dp.toPx()))
            drawRect(Color.White, Offset(0f, 82.dp.toPx()), Size(size.width, 2.dp.toPx()))
            drawLine(
                Color(0xFFFFD54F), Offset(0f, 48.dp.toPx()), Offset(size.width, 48.dp.toPx()), 3.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(18.dp.toPx(), 14.dp.toPx())),
            )
            drawRect(Color(0xFF7CB342), Offset(0f, 86.dp.toPx()), Size(size.width, 10.dp.toPx()))
        }
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val w = constraints.maxWidth.toFloat()
            val pad = w * 0.35f
            fun x(p: Float) = -pad + p * (w + pad * 2)
            Box(Modifier.offset(y = 14.dp).graphicsLayer { translationX = x(a) }) {
                VehicleSprite(VehicleType.VAN, JamColor.GREEN.ordinal, Skin.CLASSIC, 11.dp, rotation = 90f)
            }
            Box(Modifier.offset(y = 46.dp).graphicsLayer { translationX = x(1f - b) }) {
                VehicleSprite(VehicleType.BUS, JamColor.BLUE.ordinal, Skin.CLASSIC, 11.dp, rotation = -90f)
            }
            Box(Modifier.offset(y = 14.dp).graphicsLayer { translationX = x((a + 0.5f) % 1f) }) {
                VehicleSprite(VehicleType.CAR, JamColor.PINK.ordinal, Skin.CLASSIC, 11.dp, rotation = 90f)
            }
        }
    }
}

@Composable
private fun DailyDialog(visible: Boolean, onClose: () -> Unit) {
    val container = App.container
    val progress = container.progress
    val audio = LocalAudio.current
    val idx = progress.dailyIndex
    val available = progress.dailyAvailable
    var claimed by remember { mutableStateOf(false) }
    LaunchedEffect(claimed) {
        if (claimed) {
            kotlinx.coroutines.delay(1100)
            onClose()
        }
    }
    GameDialog(visible, onClose) {
        GamePanel("Daily Reward", frame = Color(0xFFFF8A1F), onClose = onClose) {
            Text("Come back every day for bigger prizes!", style = GameText.Body.copy(textAlign = TextAlign.Center))
            Spacer(Modifier.height(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { for (d in 0 until 4) DayTile(d, idx, available && !claimed, Modifier.weight(1f)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { for (d in 4 until 7) DayTile(d, idx, available && !claimed, Modifier.weight(if (d == 6) 2f else 1f)) }
            }
            Spacer(Modifier.height(18.dp))
            if (available && !claimed) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GameButton("Claim", {
                        progress.claimDaily(DAILY[idx])
                        audio.play(Sfx.CoinBurst)
                        claimed = true
                    }, Modifier.weight(1f), color = ButtonColor.GREEN, fontSize = 20.sp)
                    GameButton("x2", {
                        container.ads.showRewarded("daily_x2", onReward = {
                            progress.claimDaily(DAILY[idx] * 2)
                            audio.play(Sfx.CoinBurst)
                            claimed = true
                        })
                    }, Modifier.weight(1f), color = ButtonColor.ORANGE, fontSize = 20.sp, leading = { GameIcon(Icon.AD, 28.dp) })
                }
            } else {
                Text("Come back tomorrow!", style = GameText.Label.copy(color = Color(0xFF2E7D32), fontSize = 18.sp))
            }
        }
    }
}

@Composable
private fun DayTile(day: Int, today: Int, canClaim: Boolean, modifier: Modifier) {
    val isToday = day == today && canClaim
    val done = day < today || (day == today && !canClaim)
    val t = rememberInfiniteTransition(label = "day")
    val s by t.animateFloat(1f, 1.06f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "s")
    Box(
        modifier
            .height(92.dp)
            .scale(if (isToday) s else 1f)
            .background(
                when {
                    isToday -> Brush.verticalGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFC107)))
                    done -> Brush.verticalGradient(listOf(Color(0xFFC8E6C9), Color(0xFF81C784)))
                    else -> Brush.verticalGradient(listOf(Color(0xFFFFFFFF), Color(0xFFFFE6C7)))
                },
                RoundedCornerShape(14.dp),
            )
            .border(2.5.dp, if (isToday) Color(0xFFFF8F00) else Color(0xFFE0B97F), RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Day ${day + 1}", style = GameText.Small.copy(fontFamily = TitleFont, fontSize = 12.sp, color = Color(0xFF8A4B14)))
            GameIcon(if (day == 6) Icon.GIFT else Icon.COIN, 36.dp)
            Text("${DAILY[day]}", style = GameText.Label.copy(fontFamily = TitleFont, fontSize = 15.sp, color = Color(0xFF6D3A00)))
        }
        if (done) GameIcon(Icon.CHECK, 34.dp, Modifier.align(Alignment.TopEnd).offset(4.dp, (-6).dp))
    }
}
