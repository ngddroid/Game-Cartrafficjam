package com.puzzel.lab.cartrafficjam.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.cartrafficjam.App
import com.puzzel.lab.cartrafficjam.core.audio.Sfx
import com.puzzel.lab.cartrafficjam.game.model.JamColor
import com.puzzel.lab.cartrafficjam.game.model.VehicleType
import com.puzzel.lab.cartrafficjam.game.render.Skin
import com.puzzel.lab.cartrafficjam.ui.components.GameLogo
import com.puzzel.lab.cartrafficjam.ui.components.LocalAudio
import com.puzzel.lab.cartrafficjam.ui.components.Mascot
import com.puzzel.lab.cartrafficjam.ui.components.OutlinedText
import com.puzzel.lab.cartrafficjam.ui.components.SkyBackground
import com.puzzel.lab.cartrafficjam.ui.components.SunRays
import com.puzzel.lab.cartrafficjam.ui.components.VehicleSprite
import com.puzzel.lab.cartrafficjam.ui.theme.GameText
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onDone: (onboardingDone: Boolean) -> Unit) {
    val audio = LocalAudio.current
    val logoIn = remember { Animatable(0f) }
    val load = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(150)
        audio.play(Sfx.Whoosh)
        logoIn.animateTo(1f, spring(dampingRatio = 0.45f, stiffness = 220f))
    }
    LaunchedEffect(Unit) {
        load.animateTo(1f, tween(2300, easing = FastOutSlowInEasing))
        while (!App.container.progress.now.loaded) delay(30)
        delay(150)
        onDone(App.container.progress.now.onboardingDone)
    }
    val road = rememberInfiniteTransition(label = "road")
    val dash by road.animateFloat(0f, 1f, infiniteRepeatable(tween(500, easing = LinearEasing)), label = "dash")

    SkyBackground {
        SunRays(Modifier.fillMaxSize().offset(y = (-120).dp))
        Column(
            Modifier.fillMaxSize().statusBarsPadding().padding(top = 70.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            GameLogo(
                Modifier.graphicsLayer {
                    translationY = (1f - logoIn.value) * -600f
                    rotationZ = (1f - logoIn.value) * -12f
                },
                scale = 1.1f,
            )
        }

        // Road with traffic
        Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(300.dp)) {
            Canvas(Modifier.fillMaxSize()) {
                drawRect(Brush.verticalGradient(listOf(Color(0xFF8BC34A), Color(0xFF558B2F))), Offset(0f, 0f), Size(size.width, 50.dp.toPx()))
                val top = 40.dp.toPx()
                drawRect(Color(0xFF5B6270), Offset(0f, top), Size(size.width, 110.dp.toPx()))
                drawRect(Color.White, Offset(0f, top + 4.dp.toPx()), Size(size.width, 3.dp.toPx()))
                drawRect(Color.White, Offset(0f, top + 103.dp.toPx()), Size(size.width, 3.dp.toPx()))
                val step = 46.dp.toPx()
                drawLine(
                    Color(0xFFFFD54F), Offset(-step + dash * step, top + 55.dp.toPx()), Offset(size.width + step, top + 55.dp.toPx()),
                    4.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(24.dp.toPx(), 22.dp.toPx())),
                )
                drawRect(Brush.verticalGradient(listOf(Color(0xFF7CB342), Color(0xFF33691E))), Offset(0f, top + 110.dp.toPx()), Size(size.width, size.height))
            }
            val carT = rememberInfiniteTransition(label = "cars")
            val x1 by carT.animateFloat(-0.3f, 1.3f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "x1")
            val x2 by carT.animateFloat(1.3f, -0.3f, infiniteRepeatable(tween(3400, easing = LinearEasing)), label = "x2")
            BoxWithConstraints(Modifier.fillMaxSize()) {
                val w = constraints.maxWidth.toFloat()
                Box(Modifier.offset(y = 38.dp).graphicsLayer { translationX = x2 * w }) {
                    VehicleSprite(VehicleType.BUS, JamColor.YELLOW.ordinal, Skin.CLASSIC, 13.dp, rotation = -90f)
                }
                Box(Modifier.offset(y = 100.dp).graphicsLayer { translationX = x1 * w }) {
                    VehicleSprite(VehicleType.CAR, JamColor.RED.ordinal, Skin.CLASSIC, 13.dp, rotation = 90f)
                }
            }
            Mascot(110.dp, Modifier.align(Alignment.TopEnd).offset(x = (-14).dp, y = (-86).dp))
        }

        // Loading bar
        Column(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 22.dp, start = 40.dp, end = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(26.dp)
                    .background(Color(0xFF0D2C6B), RoundedCornerShape(13.dp))
                    .padding(4.dp)
                    .drawBehind {
                        val w = size.width * load.value
                        drawRoundRect(Color(0x33000000), Offset.Zero, size, CornerRadius(size.height / 2))
                        if (w > size.height) {
                            drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFF176), Color(0xFFFFB300))), Offset.Zero, Size(w, size.height), CornerRadius(size.height / 2))
                            drawRoundRect(Color.White.copy(alpha = 0.45f), Offset(6.dp.toPx(), 2.dp.toPx()), Size(w - 12.dp.toPx(), size.height * 0.3f), CornerRadius(4.dp.toPx()))
                        }
                    },
            )
            Spacer(Modifier.height(8.dp))
            Text("Loading ${(load.value * 100).toInt()}%", style = GameText.Label.copy(color = Color.White, fontSize = 15.sp))
            Spacer(Modifier.height(4.dp))
            OutlinedText("5000 Levels to Explore", style = GameText.Title, fontSize = 18.sp, color = Color(0xFFFFE082), strokeWidth = 5f)
        }
    }
}
