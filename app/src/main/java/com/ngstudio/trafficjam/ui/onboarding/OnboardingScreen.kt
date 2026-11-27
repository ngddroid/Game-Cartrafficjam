package com.ngstudio.trafficjam.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.trafficjam.ui.components.Day
import com.ngstudio.trafficjam.ui.components.DaySkyBackground
import com.ngstudio.trafficjam.App
import com.ngstudio.trafficjam.core.audio.Music
import com.ngstudio.trafficjam.core.audio.Sfx
import com.ngstudio.trafficjam.game.model.JamColor
import com.ngstudio.trafficjam.game.model.VehicleType
import com.ngstudio.trafficjam.game.render.PassengerArt
import com.ngstudio.trafficjam.game.render.Skin
import com.ngstudio.trafficjam.game.render.Themes
import com.ngstudio.trafficjam.ui.components.ButtonColor
import com.ngstudio.trafficjam.ui.components.GameButton
import com.ngstudio.trafficjam.ui.components.GameIcon
import com.ngstudio.trafficjam.ui.components.Icon
import com.ngstudio.trafficjam.ui.components.LocalAudio
import com.ngstudio.trafficjam.ui.components.Mascot
import com.ngstudio.trafficjam.ui.components.OutlinedText
import com.ngstudio.trafficjam.ui.components.SkyBackground
import com.ngstudio.trafficjam.ui.components.SunRays
import com.ngstudio.trafficjam.ui.components.VehicleSprite
import com.ngstudio.trafficjam.ui.components.WorldArt
import com.ngstudio.trafficjam.ui.theme.GameText
import kotlinx.coroutines.launch
import com.ngstudio.trafficjam.ui.theme.Sunset

private class Page(val title: String, val body: String)

private val pages = listOf(
    Page("Tap to Drive", "Every vehicle drives the way its arrow points. Tap one with a clear road and it zooms off to a parking spot."),
    Page("Match the Colors", "Passengers only board a vehicle of their own color. Cars seat 4, vans 6 and buses 10."),
    Page("Keep a Spot Free", "Parking spots are limited. Fill them all with the wrong colors and you're stuck, so plan ahead or use a booster!"),
    Page("5000 Levels", "Travel through 50 colorful worlds, from busy stations to space ports. Ready to clear the jam?"),
)

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val audio = LocalAudio.current
    val pager = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { audio.playMusic(Music.Home) }
    LaunchedEffect(pager.currentPage) { if (pager.currentPage > 0) audio.play(Sfx.Whoosh) }
    BackHandler(pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }

    DaySkyBackground {
        SunRays(Modifier.fillMaxSize().offset(y = (-180).dp), Color(0x33FFFFFF))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            HorizontalPager(pager, Modifier.weight(1f)) { index ->
                val page = pages[index]
                Column(
                    Modifier.fillMaxSize().padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Spacer(Modifier.height(28.dp))
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(330.dp)
                            .clip(RoundedCornerShape(32.dp))
                            .background(Color.White.copy(alpha = 0.5f))
                            .border(3.dp, Color.White.copy(alpha = 0.95f), RoundedCornerShape(32.dp)),
                        contentAlignment = Alignment.Center,
                    ) {
                        when (index) {
                            0 -> TapDemo()
                            1 -> MatchDemo()
                            2 -> SlotsDemo()
                            else -> WorldsDemo()
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    OutlinedText(page.title, style = GameText.Display, fontSize = 38.sp, color = Color(0xFFFFE14D), outline = Day.Ink, strokeWidth = 12f)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        page.body,
                        style = GameText.Body.copy(color = Day.Ink, fontSize = 18.sp, textAlign = TextAlign.Center, lineHeight = 25.sp),
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
                repeat(pages.size) { i ->
                    val w by animateDpAsState(if (i == pager.currentPage) 30.dp else 12.dp, label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(w, 12.dp)
                            .background(if (i == pager.currentPage) Day.Accent else Day.Ink.copy(alpha = 0.22f), CircleShape),
                    )
                }
            }
            val last = pager.currentPage == pages.lastIndex
            GameButton(
                if (last) "Let's Go!" else "Next",
                {
                    if (last) {
                        App.container.progress.finishOnboarding()
                        onFinished()
                    } else scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                },
                Modifier.fillMaxWidth().padding(horizontal = 40.dp, vertical = 10.dp),
                color = if (last) ButtonColor.GREEN else ButtonColor.ORANGE,
                height = 64.dp,
                fontSize = 28.sp,
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

// --- Illustrations ------------------------------------------------------------------------------

@Composable
private fun LotPanel(content: @Composable () -> Unit) {
    Box(
        Modifier
            .size(270.dp, 270.dp)
            .drawBehind {
                drawRoundRect(Color(0xFFB9BBB2), Offset(0f, 6.dp.toPx()), size, CornerRadius(26.dp.toPx()))
                drawRoundRect(Color(0xFFD4D6CF), Offset.Zero, size, CornerRadius(26.dp.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) { content() }
}

@Composable
private fun TapDemo() {
    val t = rememberInfiniteTransition(label = "tap")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "p")
    val audio = LocalAudio.current
    LotPanel {
        // Blocked bus and two neighbours
        Box(Modifier.offset((-70).dp, 10.dp)) { VehicleSprite(VehicleType.BUS, JamColor.BLUE.ordinal, Skin.CLASSIC, 22.dp) }
        Box(Modifier.offset((-14).dp, (-60).dp)) { VehicleSprite(VehicleType.VAN, JamColor.GREEN.ordinal, Skin.CLASSIC, 22.dp, rotation = 90f) }
        val drive = ((p - 0.38f) / 0.4f).coerceIn(0f, 1f)
        val ease = drive * drive
        Box(
            Modifier
                .offset(40.dp + 220.dp * ease, 40.dp)
                .graphicsLayer { alpha = if (p > 0.85f) 0f else 1f },
        ) { VehicleSprite(VehicleType.CAR, JamColor.RED.ordinal, Skin.CLASSIC, 22.dp, rotation = 90f) }
        val hand = when {
            p < 0.25f -> p / 0.25f
            else -> 1f
        }
        val press = if (p in 0.28f..0.38f) 0.85f else 1f
        LaunchedEffect(p > 0.3f && p < 0.33f) { if (p > 0.3f && p < 0.33f) audio.play(Sfx.Tap) }
        Box(
            Modifier
                .offset(130.dp - 80.dp * hand, 140.dp - 80.dp * hand)
                .scale(press)
                .graphicsLayer { alpha = if (p > 0.6f) (1f - (p - 0.6f) * 4f).coerceIn(0f, 1f) else 1f },
        ) { GameIcon(Icon.HAND, 70.dp) }
    }
}

@Composable
private fun MatchDemo() {
    val t = rememberInfiniteTransition(label = "match")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3000, easing = LinearEasing)), label = "p")
    val density = LocalDensity.current
    val pa = remember { with(density) { PassengerArt(18.dp.toPx(), 32.dp.toPx()) } }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(280.dp, 170.dp)) {
            // slot
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = 18.dp)
                    .size(74.dp, 130.dp)
                    .border(3.dp, Color.White, RoundedCornerShape(8.dp))
                    .background(Color(0x22000000), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) { VehicleSprite(VehicleType.VAN, JamColor.BLUE.ordinal, Skin.CLASSIC, 22.dp) }
            // passengers walking left into the van
            val colors = listOf(JamColor.BLUE, JamColor.BLUE, JamColor.BLUE, JamColor.PINK, JamColor.PINK)
            colors.forEachIndexed { i, c ->
                val k = ((p * 1.6f - i * 0.18f)).coerceIn(0f, 1f)
                val boarded = c == JamColor.BLUE && k >= 1f
                val x = 110.dp + 30.dp * i - if (c == JamColor.BLUE) 70.dp * k else 54.dp * ((p * 1.6f - 0.54f).coerceIn(0f, 1f))
                val hop = kotlin.math.abs(kotlin.math.sin(p * 40f + i)) * 4f
                if (!boarded) {
                    Image(pa.get(c.ordinal), null, Modifier.align(Alignment.CenterStart).offset(x = x, y = (-hop).dp))
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Bottom) {
            SeatBadge(VehicleType.CAR, JamColor.RED.ordinal, 4)
            SeatBadge(VehicleType.VAN, JamColor.GREEN.ordinal, 6)
            SeatBadge(VehicleType.BUS, JamColor.YELLOW.ordinal, 10)
        }
    }
}

@Composable
private fun SeatBadge(type: VehicleType, color: Int, seats: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        VehicleSprite(type, color, Skin.CLASSIC, 12.dp, rotation = 90f)
        Spacer(Modifier.height(6.dp))
        Text(
            "$seats seats",
            style = GameText.Small.copy(color = Color.White, fontSize = 13.sp),
            modifier = Modifier.background(Color(0x66000000), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun SlotsDemo() {
    val t = rememberInfiniteTransition(label = "slots")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3400, easing = LinearEasing)), label = "p")
    val colors = listOf(JamColor.RED, JamColor.YELLOW, JamColor.PURPLE, JamColor.GREEN)
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            colors.forEachIndexed { i, c ->
                val k = ((p - i * 0.12f) / 0.12f).coerceIn(0f, 1f)
                Box(
                    Modifier
                        .size(54.dp, 96.dp)
                        .border(3.dp, Color.White, RoundedCornerShape(6.dp))
                        .background(if (p > 0.55f && p < 0.8f) Color(0x55F2453D) else Color(0x22000000), RoundedCornerShape(6.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.graphicsLayer { alpha = k; translationY = (1 - k) * 120f }) {
                        VehicleSprite(VehicleType.CAR, c.ordinal, Skin.CLASSIC, 13.dp)
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        val warn = if (p > 0.55f) 1f else 0f
        Box(Modifier.height(80.dp), contentAlignment = Alignment.Center) {
            if (p < 0.78f) {
                Text(
                    "No spots left!",
                    style = GameText.Label.copy(color = Color.White, fontSize = 18.sp),
                    modifier = Modifier
                        .graphicsLayer { alpha = warn; scaleX = 0.8f + warn * 0.2f; scaleY = 0.8f + warn * 0.2f }
                        .background(Color(0xFFF2453D), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 6.dp),
                )
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    val k = ((p - 0.78f) / 0.1f).coerceIn(0f, 1f)
                    listOf(Icon.SHUFFLE, Icon.CRANE, Icon.SORT).forEach {
                        Box(Modifier.scale(k)) { GameIcon(it, 64.dp) }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorldsDemo() {
    val t = rememberInfiniteTransition(label = "worlds")
    val fan by t.animateFloat(0.85f, 1f, infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "fan")
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        val shown = listOf(0, 2, 5, 9)
        shown.forEachIndexed { i, ti ->
            val angle = (i - 1.5f) * 13f * fan
            Box(
                Modifier
                    .offset(x = ((i - 1.5f) * 46f * fan).dp, y = (kotlin.math.abs(i - 1.5f) * 10f).dp)
                    .graphicsLayer { rotationZ = angle }
                    .size(120.dp, 168.dp)
                    .drawBehind {
                        drawRoundRect(Day.Ink, Offset(0f, 5.dp.toPx()), size, CornerRadius(18.dp.toPx()))
                        drawRoundRect(Color.White, Offset.Zero, size, CornerRadius(18.dp.toPx()))
                    }
                    .padding(5.dp)
                    .clip(RoundedCornerShape(14.dp)),
            ) {
                WorldArt(Themes.all[ti], Modifier.fillMaxSize())
                Text(
                    Themes.all[ti].name,
                    style = GameText.Small.copy(color = Color.White, fontSize = 11.sp),
                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color(0x99000000)).padding(4.dp),
                    textAlign = TextAlign.Center,
                )
            }
        }
        Box(Modifier.align(Alignment.TopCenter).padding(top = 6.dp)) {
            OutlinedText("50 Worlds", style = GameText.Title, fontSize = 26.sp, strokeWidth = 7f, outline = Day.Ink)
        }
        Mascot(80.dp, Modifier.align(Alignment.BottomEnd).padding(8.dp))
        Box(
            Modifier
                .align(Alignment.BottomStart)
                .padding(10.dp)
                .drawBehind { drawCircle(Color.White.copy(alpha = 0.4f), size.minDimension / 2, style = Stroke(3.dp.toPx())) },
        ) { GameIcon(Icon.TROPHY, 60.dp) }
    }
}
