package com.ngstudio.trafficjam.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.trafficjam.App
import com.ngstudio.trafficjam.core.audio.Music
import com.ngstudio.trafficjam.core.audio.Sfx
import com.ngstudio.trafficjam.game.model.GameMode
import com.ngstudio.trafficjam.game.modes.PixelPicture
import com.ngstudio.trafficjam.game.modes.PixelPictures
import androidx.compose.ui.graphics.graphicsLayer
import com.ngstudio.trafficjam.game.render.shade
import com.ngstudio.trafficjam.game.render.tint
import com.ngstudio.trafficjam.ui.main.ModePreview
import com.ngstudio.trafficjam.ui.components.ButtonColor
import com.ngstudio.trafficjam.ui.components.Day
import com.ngstudio.trafficjam.ui.components.DaySkyBackground
import com.ngstudio.trafficjam.ui.components.GameButton
import com.ngstudio.trafficjam.ui.components.LocalAudio
import com.ngstudio.trafficjam.ui.components.OutlinedText
import com.ngstudio.trafficjam.ui.components.SunRays
import com.ngstudio.trafficjam.ui.theme.GameText
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** One onboarding screen per game mode, each with its own illustration and the mode's own words. */
private val modes = GameMode.entries

// Onboarding shows a different picture than the Modes tab (which uses the heart) so the two screens
// don't look identical. The Pixel Crowd choreography peels a red outline then the pink underneath, so
// its picture needs both: the cupcake (red cherry + pink frosting) reads well and clearly isn't a heart.
private val onboardPixelPic = PixelPictures.all.first { it.name == "Cupcake" }
private val onboardSandPic = PixelPictures.all.first { it.name == "Star" }

/** A distinct preview picture for the Pixel Crowd and Sand Sweep pages; null leaves the mode's default. */
private fun onboardingPicture(mode: GameMode): PixelPicture? = when (mode) {
    GameMode.PIXEL -> onboardPixelPic
    GameMode.SAND -> onboardSandPic
    else -> null
}

@Composable
fun OnboardingScreen(onFinished: () -> Unit) {
    val audio = LocalAudio.current
    val pager = rememberPagerState { modes.size }
    val scope = rememberCoroutineScope()
    LaunchedEffect(Unit) { audio.playMusic(Music.Home) }
    LaunchedEffect(pager.currentPage) { if (pager.currentPage > 0) audio.play(Sfx.Whoosh) }
    BackHandler(pager.currentPage > 0) { scope.launch { pager.animateScrollToPage(pager.currentPage - 1) } }

    DaySkyBackground {
        SunRays(Modifier.fillMaxSize().offset(y = (-180).dp), Color(0x33FFFFFF))
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            HorizontalPager(pager, Modifier.weight(1f)) { index ->
                val mode = modes[index]
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
                        // The accurate Modes gameplay preview, but reframed a little so it doesn't
                        // look identical to the Modes tab: zoomed in slightly, on a cooler-tinted
                        // card, with a soft gloss top and shaded bottom.
                        Box(
                            Modifier
                                .fillMaxSize()
                                .padding(5.dp)
                                .clip(RoundedCornerShape(28.dp))
                                .background(Brush.verticalGradient(listOf(Color(mode.accent).tint(0.55f), Color(mode.accent).shade(0.9f)))),
                        ) {
                            // Onboarding holds the Sand picture full so the star stays clearly readable (no sweep clearing it).
                            ModePreview(mode, Modifier.fillMaxSize().clip(RoundedCornerShape(28.dp)), onboardingPicture(mode), holdPicture = mode == GameMode.SAND)
                            Box(
                                Modifier.fillMaxSize().background(
                                    Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent, Color.Black.copy(alpha = 0.12f))),
                                ),
                            )
                        }
                    }
                    Spacer(Modifier.height(28.dp))
                    OutlinedText(mode.title, style = GameText.Display, fontSize = 36.sp, color = Color(0xFFFFE14D), outline = Day.Ink, strokeWidth = 12f)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        mode.tagline,
                        style = GameText.Body.copy(color = Day.Ink, fontSize = 18.sp, textAlign = TextAlign.Center, lineHeight = 25.sp),
                    )
                }
            }

            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), horizontalArrangement = Arrangement.Center) {
                repeat(modes.size) { i ->
                    val w by animateDpAsState(if (i == pager.currentPage) 30.dp else 12.dp, label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 4.dp)
                            .size(w, 12.dp)
                            .background(if (i == pager.currentPage) Day.Accent else Day.Ink.copy(alpha = 0.22f), CircleShape),
                    )
                }
            }
            val last = pager.currentPage == modes.lastIndex
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

// --- Per-mode illustrations --------------------------------------------------------------------
// A deliberately different, flat "poster" look from the detailed board previews on the Modes tab.

@Composable
private fun ModeIntro(mode: GameMode) {
    val period = when (mode) {
        GameMode.CLASSIC -> 4200
        GameMode.LOOP -> 4600
        GameMode.PIXEL -> 3600
        GameMode.SAND -> 4000
    }
    val t = rememberInfiniteTransition(label = "intro")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(period, easing = LinearEasing), RepeatMode.Restart), label = "p")
    val base = Color(mode.accent)
    val dark = Color(mode.accentDark)
    Canvas(Modifier.fillMaxSize().padding(18.dp).clip(RoundedCornerShape(24.dp))) {
        // Soft wash in the mode's color so each screen feels branded (clipped to the rounded frame).
        drawRect(Brush.verticalGradient(listOf(base.tint(0.75f), base.tint(0.55f))))
        when (mode) {
            GameMode.CLASSIC -> classicIntro(p)
            GameMode.LOOP -> loopIntro(p, base, dark)
            GameMode.PIXEL -> pixelIntro(p, base, dark)
            GameMode.SAND -> sandIntro(p, base, dark)
        }
    }
}

// ---- shared helpers ----

private fun seg(t: Float, a: Float, b: Float) = ((t - a) / (b - a)).coerceIn(0f, 1f)
private fun smooth(x: Float) = x * x * (3f - 2f * x)
private fun easeIn(x: Float) = x * x
private fun easeOut(x: Float) = 1f - (1f - x) * (1f - x)
private fun frac(x: Float) = x - floor(x)

/** A clean five-point star outline. */
private fun starShape(cx: Float, cy: Float, rOut: Float, rIn: Float): Path {
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) rOut else rIn
        val a = i * PI.toFloat() / 5f - PI.toFloat() / 2f
        val x = cx + r * cos(a)
        val y = cy + r * sin(a)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    return path
}
private fun mix(a: Float, b: Float, f: Float) = a + (b - a) * f
private fun bump(x: Float) = if (x <= 0f || x >= 1f) 0f else sin(x * PI.toFloat())

/** A glossy side-view cartoon car centred at [c], with gradient body, reflections and detailed wheels. */
private fun DrawScope.glossyCar(c: Offset, wid: Float, color: Color) {
    val hgt = wid * 0.46f
    val left = c.x - wid / 2
    val top = c.y - hgt / 2
    // ground shadow
    drawOval(Color.Black.copy(alpha = 0.18f), Offset(left + wid * 0.05f, top + hgt * 0.9f), Size(wid * 0.9f, hgt * 0.3f))
    // cabin hump
    val cab = Path().apply {
        val cl = left + wid * 0.25f; val cr = left + wid * 0.75f
        moveTo(cl, top + hgt * 0.52f)
        cubicTo(cl + wid * 0.02f, top + hgt * 0.02f, cl + wid * 0.1f, top, cl + wid * 0.2f, top)
        lineTo(cr - wid * 0.18f, top)
        cubicTo(cr - wid * 0.06f, top + hgt * 0.02f, cr, top + hgt * 0.12f, cr + wid * 0.02f, top + hgt * 0.52f)
        close()
    }
    drawPath(cab, color.shade(0.9f))
    // body with vertical gradient
    val bodyTop = top + hgt * 0.4f
    drawRoundRect(
        Brush.verticalGradient(listOf(color.tint(0.35f), color, color.shade(0.78f)), bodyTop, top + hgt),
        Offset(left, bodyTop), Size(wid, hgt * 0.58f), CornerRadius(hgt * 0.34f),
    )
    drawRoundRect(color.shade(0.68f), Offset(left, top + hgt * 0.8f), Size(wid, hgt * 0.18f), CornerRadius(hgt * 0.16f))
    // windshield glass
    val win = Path().apply {
        val cl = left + wid * 0.3f; val cr = left + wid * 0.7f
        moveTo(cl, top + hgt * 0.46f)
        cubicTo(cl + wid * 0.02f, top + hgt * 0.14f, cl + wid * 0.07f, top + hgt * 0.1f, cl + wid * 0.14f, top + hgt * 0.1f)
        lineTo(cr - wid * 0.12f, top + hgt * 0.1f)
        cubicTo(cr - wid * 0.05f, top + hgt * 0.1f, cr, top + hgt * 0.18f, cr, top + hgt * 0.46f)
        close()
    }
    drawPath(win, Brush.verticalGradient(listOf(Color(0xFFDFF2FF), Color(0xFF7FB7E0))))
    drawLine(Color.White.copy(alpha = 0.75f), Offset(left + wid * 0.37f, top + hgt * 0.42f), Offset(left + wid * 0.48f, top + hgt * 0.14f), hgt * 0.06f, StrokeCap.Round)
    // body top gloss
    drawRoundRect(Color.White.copy(alpha = 0.3f), Offset(left + wid * 0.08f, bodyTop + hgt * 0.05f), Size(wid * 0.84f, hgt * 0.1f), CornerRadius(hgt * 0.06f))
    // lights
    drawCircle(Color(0xFFFFF3B0), hgt * 0.09f, Offset(left + wid * 0.97f, bodyTop + hgt * 0.22f))
    drawCircle(Color(0xFFFF6B6B), hgt * 0.07f, Offset(left + wid * 0.03f, bodyTop + hgt * 0.22f))
    // wheels
    val wy = top + hgt * 0.9f
    for (wx in listOf(left + wid * 0.27f, left + wid * 0.73f)) {
        drawCircle(Color(0xFF23262B), hgt * 0.3f, Offset(wx, wy))
        drawCircle(Color(0xFF3A3F47), hgt * 0.3f, Offset(wx, wy), style = Stroke(hgt * 0.06f))
        drawCircle(Color(0xFFCED4DE), hgt * 0.13f, Offset(wx, wy))
        drawCircle(Color(0xFF8A929E), hgt * 0.05f, Offset(wx, wy))
        drawCircle(Color.White.copy(alpha = 0.8f), hgt * 0.04f, Offset(wx - hgt * 0.08f, wy - hgt * 0.08f))
    }
}

/** A glossy side-view bus centred at [c]. */
private fun DrawScope.glossyBus(c: Offset, wid: Float, color: Color) {
    val hgt = wid * 0.52f
    val left = c.x - wid / 2
    val top = c.y - hgt / 2
    drawOval(Color.Black.copy(alpha = 0.18f), Offset(left + wid * 0.05f, top + hgt * 0.88f), Size(wid * 0.9f, hgt * 0.26f))
    drawRoundRect(color.shade(0.72f), Offset(left, top + hgt * 0.06f), Size(wid, hgt * 0.82f), CornerRadius(hgt * 0.24f))
    drawRoundRect(
        Brush.verticalGradient(listOf(color.tint(0.3f), color, color.shade(0.82f)), top, top + hgt * 0.85f),
        Offset(left, top), Size(wid, hgt * 0.82f), CornerRadius(hgt * 0.24f),
    )
    drawRoundRect(Color.White.copy(alpha = 0.3f), Offset(left + wid * 0.07f, top + hgt * 0.08f), Size(wid * 0.86f, hgt * 0.12f), CornerRadius(hgt * 0.06f))
    for (i in 0 until 4) {
        drawRoundRect(
            Brush.verticalGradient(listOf(Color(0xFFDFF2FF), Color(0xFF8FC2E6))),
            Offset(left + wid * (0.1f + i * 0.2f), top + hgt * 0.26f), Size(wid * 0.15f, hgt * 0.3f), CornerRadius(hgt * 0.06f),
        )
    }
    drawCircle(Color(0xFFFFF3B0), hgt * 0.07f, Offset(left + wid * 0.96f, top + hgt * 0.6f))
    val wy = top + hgt * 0.84f
    for (wx in listOf(left + wid * 0.24f, left + wid * 0.76f)) {
        drawCircle(Color(0xFF23262B), hgt * 0.17f, Offset(wx, wy))
        drawCircle(Color(0xFFCED4DE), hgt * 0.07f, Offset(wx, wy))
    }
}

/** A five-point star centred at [c]. */
private fun DrawScope.star5(c: Offset, rOut: Float, rIn: Float, rot: Float, color: Color) {
    if (rOut <= 0.5f) return
    val path = Path()
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) rOut else rIn
        val a = rot + i * PI.toFloat() / 5f - PI.toFloat() / 2f
        val x = c.x + r * cos(a); val y = c.y + r * sin(a)
        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
    }
    path.close()
    drawPath(path, color)
}

/** A radiating burst of little sparks, [prog] 0..1. */
private fun DrawScope.sparkleBurst(c: Offset, radius: Float, prog: Float, color: Color) {
    if (prog <= 0f || prog >= 1f) return
    val a = 1f - prog
    val d = radius * (0.25f + 0.95f * prog)
    for (i in 0 until 8) {
        val ang = i * PI.toFloat() / 4f + prog * 1.2f
        val pos = Offset(c.x + d * cos(ang), c.y + d * sin(ang))
        drawCircle(color.copy(alpha = a), radius * 0.13f * (1f - prog), pos)
    }
    drawCircle(Color.White.copy(alpha = 0.55f * a), d, c, style = Stroke(radius * 0.05f))
}

/** A glossy rounded "person" tile with a little face, for the pixel picture. */
private fun DrawScope.personTile(cx: Float, cy: Float, cell: Float, scale: Float, color: Color) {
    if (scale <= 0.01f) return
    val s = cell * 0.46f * scale
    drawRoundRect(color.shade(0.7f), Offset(cx - s, cy - s + 2f), Size(s * 2, s * 2), CornerRadius(s * 0.5f))
    drawRoundRect(
        Brush.verticalGradient(listOf(color.tint(0.4f), color, color.shade(0.82f)), cy - s, cy + s),
        Offset(cx - s, cy - s), Size(s * 2, s * 2), CornerRadius(s * 0.5f),
    )
    drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(cx - s * 0.7f, cy - s * 0.72f), Size(s * 1.4f, s * 0.5f), CornerRadius(s * 0.3f))
    val eo = s * 0.34f
    drawCircle(Color.White, s * 0.2f, Offset(cx - eo, cy - s * 0.03f))
    drawCircle(Color.White, s * 0.2f, Offset(cx + eo, cy - s * 0.03f))
    drawCircle(Color(0xFF3A2230), s * 0.1f, Offset(cx - eo, cy))
    drawCircle(Color(0xFF3A2230), s * 0.1f, Offset(cx + eo, cy))
}

/** A standing person: round body and head in [color]. */
private fun DrawScope.person(c: Offset, r: Float, color: Color) {
    drawCircle(color.shade(0.75f), r, Offset(c.x, c.y + r * 0.55f + 1f))
    drawCircle(color, r, Offset(c.x, c.y + r * 0.5f))
    drawCircle(color.tint(0.25f), r * 0.58f, Offset(c.x, c.y - r * 0.45f))
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.18f, Offset(c.x - r * 0.2f, c.y - r * 0.6f))
}

/** A popping tick in a white-ringed green coin, scaled by [s] (0..1). */
private fun DrawScope.checkPop(c: Offset, r: Float, s: Float) {
    if (s <= 0f) return
    val rr = r * s
    drawCircle(Color.White, rr, c)
    drawCircle(Color(0xFF3DBE4B), rr * 0.84f, c)
    val p = Path().apply {
        moveTo(c.x - rr * 0.4f, c.y + rr * 0.02f)
        lineTo(c.x - rr * 0.08f, c.y + rr * 0.34f)
        lineTo(c.x + rr * 0.46f, c.y - rr * 0.34f)
    }
    drawPath(p, Color.White, style = Stroke(rr * 0.22f, cap = StrokeCap.Round, join = StrokeJoin.Round))
}

// ---- Classic: three colored cars drop into their matching bays ----

/** A standing passenger with a colored body and head and a little face. */
private fun DrawScope.rider(c: Offset, r: Float, color: Color) {
    if (r <= 0.5f) return
    drawOval(Color.Black.copy(alpha = 0.15f), Offset(c.x - r * 0.8f, c.y + r * 0.95f), Size(r * 1.6f, r * 0.45f))
    val body = Path().apply {
        moveTo(c.x - r * 0.78f, c.y + r * 1.05f)
        cubicTo(c.x - r * 0.78f, c.y + r * 0.15f, c.x - r * 0.55f, c.y - r * 0.05f, c.x, c.y - r * 0.05f)
        cubicTo(c.x + r * 0.55f, c.y - r * 0.05f, c.x + r * 0.78f, c.y + r * 0.15f, c.x + r * 0.78f, c.y + r * 1.05f)
        close()
    }
    drawPath(body, color.shade(0.8f))
    drawPath(body, Brush.verticalGradient(listOf(color.tint(0.28f), color), c.y - r * 0.1f, c.y + r * 1.05f))
    drawCircle(color.shade(0.8f), r * 0.56f, Offset(c.x, c.y - r * 0.46f + 1f))
    drawCircle(color.tint(0.3f), r * 0.54f, Offset(c.x, c.y - r * 0.5f))
    drawCircle(Color.White.copy(alpha = 0.6f), r * 0.14f, Offset(c.x - r * 0.2f, c.y - r * 0.62f))
    drawCircle(Color(0xFF3A2230), r * 0.09f, Offset(c.x - r * 0.17f, c.y - r * 0.5f))
    drawCircle(Color(0xFF3A2230), r * 0.09f, Offset(c.x + r * 0.17f, c.y - r * 0.5f))
}

/** A glossy side-view dump truck with a [load] of sand (0..1) in its bed. */
private fun DrawScope.glossyTruck(c: Offset, wid: Float, color: Color, load: Float) {
    val hgt = wid * 0.5f
    val left = c.x - wid / 2
    val top = c.y - hgt / 2
    drawOval(Color.Black.copy(alpha = 0.18f), Offset(left + wid * 0.04f, top + hgt * 0.88f), Size(wid * 0.92f, hgt * 0.26f))
    // dump bed (rear/left)
    val bedW = wid * 0.6f
    drawRoundRect(
        Brush.verticalGradient(listOf(color.tint(0.3f), color, color.shade(0.8f)), top + hgt * 0.2f, top + hgt * 0.72f),
        Offset(left, top + hgt * 0.2f), Size(bedW, hgt * 0.52f), CornerRadius(hgt * 0.1f),
    )
    if (load > 0.02f) {
        val lh = hgt * 0.34f * load.coerceAtMost(1f)
        drawRoundRect(Color(0xFFE6B45A), Offset(left + wid * 0.04f, top + hgt * 0.2f - lh), Size(bedW - wid * 0.08f, lh + hgt * 0.1f), CornerRadius(hgt * 0.08f))
        drawRoundRect(Color(0xFFF3CE86), Offset(left + wid * 0.04f, top + hgt * 0.2f - lh), Size(bedW - wid * 0.08f, hgt * 0.05f), CornerRadius(hgt * 0.04f))
    }
    // cab (front/right)
    val cabL = left + wid * 0.62f
    drawRoundRect(
        Brush.verticalGradient(listOf(color.tint(0.3f), color, color.shade(0.82f)), top + hgt * 0.26f, top + hgt * 0.74f),
        Offset(cabL, top + hgt * 0.26f), Size(wid * 0.36f, hgt * 0.48f), CornerRadius(hgt * 0.12f),
    )
    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFDFF2FF), Color(0xFF8FC2E6))), Offset(cabL + wid * 0.05f, top + hgt * 0.32f), Size(wid * 0.21f, hgt * 0.24f), CornerRadius(hgt * 0.06f))
    drawCircle(Color(0xFFFFF3B0), hgt * 0.07f, Offset(left + wid * 0.98f, top + hgt * 0.66f))
    val wy = top + hgt * 0.84f
    for (wx in listOf(left + wid * 0.17f, left + wid * 0.44f, left + wid * 0.8f)) {
        drawCircle(Color(0xFF23262B), hgt * 0.17f, Offset(wx, wy))
        drawCircle(Color(0xFFCED4DE), hgt * 0.07f, Offset(wx, wy))
    }
}

/** Fills [b] with smooth, fine-grained sand: a warm layered gradient, soft sand-art bands, and a
 *  dense stable speckle so it reads as real sand rather than beads. Call inside a clip. */
private fun DrawScope.drawSmoothSand(b: Rect) {
    drawRect(
        Brush.verticalGradient(
            0f to Color(0xFFF6DC95), 0.42f to Color(0xFFEBC067), 0.74f to Color(0xFFD9A648), 1f to Color(0xFFC3872F),
            startY = b.top, endY = b.bottom,
        ),
        b.topLeft, b.size,
    )
    // Soft wavy sand-art layers.
    for (band in listOf(0.24f, 0.46f, 0.68f, 0.86f)) {
        val yb = b.top + b.height * band
        val path = Path().apply {
            moveTo(b.left - 6f, yb)
            var x = b.left - 6f
            while (x <= b.right + 6f) {
                lineTo(x, yb + sin(x * 0.045f + band * 9f) * b.height * 0.016f)
                x += 6f
            }
        }
        drawPath(path, Color(0x2E9A6519), style = Stroke(b.height * 0.02f))
    }
    // Fine speckle at stable pseudo-random spots (two sizes) for a grainy sand texture.
    for (i in 0 until 520) {
        val fx = frac(sin(i * 12.9898f) * 43758.547f)
        val fy = frac(sin(i * 78.233f) * 43758.547f)
        val x = b.left + fx * b.width
        val y = b.top + fy * b.height
        val s = frac(sin(i * 3.17f) * 91.73f)
        val col = when {
            s < 0.4f -> Color(0x44FFF1C8)
            s < 0.74f -> Color(0x3C8A571A)
            else -> Color(0x34FFFFFF)
        }
        drawCircle(col, 0.8f + s * 1.5f, Offset(x, y))
    }
}

// Classic explains the real loop: a car parks at the platform and waiting riders of its own
// colour board it one by one; when every seat is full the car drives off, and the next colour
// pulls in. Riders of other colours keep waiting.
private fun DrawScope.classicIntro(p: Float) {
    val w = size.width
    val h = size.height
    val cols = listOf(Color(0xFFF2453D), Color(0xFF2F7CF6), Color(0xFFFFC21A))
    val seats = 4
    val cars = 3
    val sub = p * cars
    val cycle = min(cars - 1, floor(sub).toInt())
    val tau = sub - cycle
    val carColor = cols[cycle]

    // Clouds.
    for ((cxf, cyf, sf) in listOf(Triple(0.22f, 0.14f, 1f), Triple(0.8f, 0.1f, 0.78f))) {
        val cx = (cxf * w + p * w * 0.03f) % (w * 1.12f)
        val cy = cyf * h; val s = sf * w * 0.07f
        drawCircle(Color.White.copy(alpha = 0.85f), s, Offset(cx, cy))
        drawCircle(Color.White.copy(alpha = 0.85f), s * 0.8f, Offset(cx - s, cy + s * 0.2f))
        drawCircle(Color.White.copy(alpha = 0.85f), s * 0.85f, Offset(cx + s, cy + s * 0.15f))
    }

    // Platform, parking stall, and the road along the bottom.
    val platTop = h * 0.42f
    drawRect(Color(0xFFE9E5D9), Offset(0f, platTop), Size(w, h - platTop))
    drawRect(Color(0xFFF4F1E8), Offset(0f, platTop), Size(w, h * 0.012f))
    val roadTop = h * 0.76f
    drawRect(Color(0xFF8A8F99), Offset(0f, roadTop), Size(w, h - roadTop))
    drawLine(Color.White.copy(alpha = 0.8f), Offset(0f, (roadTop + h) / 2f), Offset(w, (roadTop + h) / 2f), h * 0.012f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(w * 0.06f, w * 0.05f)))

    val stallCx = w * 0.66f
    val carY = h * 0.6f
    val stallW = w * 0.38f
    val carWid = stallW * 0.76f
    drawRoundRect(Color.White.copy(alpha = 0.85f), Offset(stallCx - stallW / 2, carY - h * 0.12f), Size(stallW, h * 0.26f), CornerRadius(h * 0.02f), style = Stroke(3f))

    val board = seg(tau, 0.16f, 0.66f)
    val consumed = board * seats
    val filled = floor(consumed).toInt().coerceIn(0, seats)
    val leaving = tau >= 0.72f

    val carX = when {
        tau < 0.12f -> mix(w * 1.35f, stallCx, smooth(seg(tau, 0f, 0.12f)))
        tau < 0.72f -> stallCx
        else -> mix(stallCx, w * 1.4f, smooth(seg(tau, 0.72f, 0.9f)))
    }

    // Queue of this car's colour, advancing toward the front as riders board.
    val qY = h * 0.86f
    val qFront = w * 0.1f
    val spacing = w * 0.12f
    val rr = h * 0.042f
    for (j in 0 until seats) {
        if (consumed - j <= 0f) {
            val x = qFront + (j - consumed) * spacing
            val bob = sin(p * 16f + j) * h * 0.006f
            rider(Offset(x, qY - bob), rr, carColor)
        }
    }
    // Two riders of the next colour waiting their turn (they do not board this car).
    val waitColor = cols[(cycle + 1) % cars]
    for (i in 0 until 2) {
        val x = qFront + (seats - consumed + 0.7f + i) * spacing
        if (x < w * 1.05f) rider(Offset(x, qY + sin(p * 13f + i) * h * 0.005f), rr, waitColor)
    }

    // The car.
    glossyCar(Offset(carX, carY), carWid, carColor)
    // Seat pips under the car show it filling up.
    if (!leaving) {
        val pipY = carY + carWid * 0.34f
        for (s in 0 until seats) {
            val px = carX + (s - (seats - 1) / 2f) * carWid * 0.17f
            drawCircle(Color.White, carWid * 0.055f, Offset(px, pipY))
            drawCircle(if (s < filled) carColor else Color.Black.copy(alpha = 0.2f), carWid * 0.04f, Offset(px, pipY))
        }
        if (filled >= seats) {
            sparkleBurst(Offset(carX, carY - h * 0.02f), w * 0.13f, seg(tau, 0.66f, 0.92f), carColor.tint(0.3f))
            checkPop(Offset(carX, carY - h * 0.16f), w * 0.05f, seg(tau, 0.66f, 0.78f))
        }
    }

    // Riders walking from the front of the line into the car door.
    if (!leaving) {
        val door = Offset(carX - carWid * 0.1f, carY + h * 0.03f)
        for (j in 0 until seats) {
            val wj = (consumed - j).coerceIn(0f, 1f)
            if (wj <= 0f || wj >= 1f) continue
            val x = mix(qFront, door.x, wj)
            val y = mix(qY, door.y, wj) - sin(wj * PI.toFloat()) * h * 0.07f
            rider(Offset(x, y), rr * (1f - wj * 0.35f), carColor)
        }
    }

    // Speed lines as the full car drives off.
    if (leaving) {
        val q = seg(tau, 0.72f, 0.88f)
        for (j in -1..1) {
            val y = carY + j * h * 0.03f
            drawLine(Color.White.copy(alpha = 0.7f * (1f - q)), Offset(carX - carWid * 0.6f, y), Offset(carX - carWid * 0.6f - w * (0.05f + 0.06f * q), y), h * 0.01f, StrokeCap.Round)
        }
    }
}

// ---- Crowd Loop: a crowd circles the ring road and a bus at the throat fills up ----

private fun DrawScope.loopIntro(p: Float, base: Color, dark: Color) {
    val w = size.width
    val h = size.height
    val c = Offset(w * 0.5f, h * 0.4f)
    val rx = w * 0.33f
    val ry = h * 0.24f
    val cols = listOf(Color(0xFF2F7CF6), Color(0xFFF2453D), Color(0xFFFFC21A))

    // Ring road.
    drawOval(dark.shade(0.9f).copy(alpha = 0.25f), Offset(c.x - rx, c.y - ry + 4f), Size(rx * 2, ry * 2), style = Stroke(h * 0.11f))
    drawOval(Color(0xFF7D838D), Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2), style = Stroke(h * 0.1f))
    drawOval(Color.White.copy(alpha = 0.6f), Offset(c.x - rx, c.y - ry), Size(rx * 2, ry * 2), style = Stroke(1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 9f))))
    // Grassy centre.
    drawOval(base.tint(0.4f), Offset(c.x - rx * 0.6f, c.y - ry * 0.55f), Size(rx * 1.2f, ry * 1.1f))

    // Crowd marching round the ring.
    val n = 14
    val headRot = p * 2f * PI.toFloat()
    for (i in 0 until n) {
        val a = i.toFloat() / n * 2f * PI.toFloat() + headRot
        val pos = Offset(c.x + rx * cos(a), c.y + ry * sin(a))
        val bob = sin(p * 24f + i) * h * 0.006f
        person(Offset(pos.x, pos.y - bob), h * 0.03f, cols[i % 3])
    }

    // The bus parked at the throat (bottom), filling as riders board.
    val bw = w * 0.42f
    val bh = h * 0.18f
    val bl = c.x - bw / 2
    val bt = h * 0.74f
    drawRoundRect(Color.Black.copy(alpha = 0.15f), Offset(bl, bt + bh * 0.55f + 3f), Size(bw, bh * 0.6f), CornerRadius(bh * 0.4f))
    drawRoundRect(cols[0].shade(0.85f), Offset(bl, bt), Size(bw, bh), CornerRadius(bh * 0.35f))
    // Fill level climbs with p and resets each loop.
    val fill = seg(p, 0.1f, 0.92f)
    drawRoundRect(cols[0], Offset(bl + 3f, bt + bh * (1f - fill) + 3f), Size(bw - 6f, bh * fill - 6f), CornerRadius(bh * 0.3f))
    // Windows.
    for (wi in 0 until 4) {
        drawRoundRect(Color(0xFFCFE9FF).copy(alpha = 0.85f), Offset(bl + bw * (0.1f + wi * 0.21f), bt + bh * 0.2f), Size(bw * 0.15f, bh * 0.3f), CornerRadius(bh * 0.08f))
    }
    for (wx in listOf(bl + bw * 0.22f, bl + bw * 0.78f)) {
        drawCircle(Color(0xFF2B2B33), bh * 0.2f, Offset(wx, bt + bh))
        drawCircle(Color(0xFFBFC6D2), bh * 0.08f, Offset(wx, bt + bh))
    }
    // A rider hops from the ring into the bus.
    val hop = seg(p % (1f / 3f) * 3f, 0f, 1f)
    if (hop > 0f && hop < 1f) {
        val from = Offset(c.x, c.y + ry)
        val to = Offset(c.x, bt + bh * 0.3f)
        val pos = Offset(mix(from.x, to.x, hop), mix(from.y, to.y, hop) - bump(hop) * h * 0.12f)
        person(pos, h * 0.03f, cols[0])
    }
}

// ---- Pixel Crowd: a pixel heart made of people, peeled column by column by a passing bus ----

private fun DrawScope.pixelIntro(p: Float, base: Color, dark: Color) {
    val w = size.width
    val h = size.height
    val colsN = 11
    val rowsN = 9
    val gridW = w * 0.82f
    val cell = gridW / colsN
    val gridH = cell * rowsN
    val ox = (w - gridW) / 2f
    val oy = (h - gridH) / 2f - h * 0.08f
    val pink = Color(0xFFE0409A)

    fun heart(col: Int, row: Int): Boolean {
        val nx = ((col + 0.5f) / colsN * 2f - 1f) * 1.15f
        val ny = (1f - (row + 0.5f) / rowsN * 2f) * 1.15f + 0.15f
        val a = nx * nx + ny * ny - 1f
        return a * a * a - nx * nx * ny * ny * ny <= 0f
    }

    val busW = cell * 2.8f
    val laneY = oy + gridH + cell * 0.75f
    val busX = mix(-busW, w + busW, p)
    val appear = smooth(seg(p, 0f, 0.12f)) // gentle bounce-in each loop

    for (row in 0 until rowsN) {
        for (col in 0 until colsN) {
            if (!heart(col, row)) continue
            val cx = ox + col * cell + cell / 2
            val cy = oy + row * cell + cell / 2
            val d = (busX - cx) / (cell * 2.4f) // <0 before the bus, 0..1 popping, >1 gone
            when {
                d < 0f -> {
                    val bob = sin(p * 18f + col * 0.6f + row * 0.4f) * cell * 0.04f
                    personTile(cx, cy + bob, cell, appear, pink)
                }
                d < 1f -> {
                    val lift = d * cell * 1.9f
                    val sc = ((1f - d) * (1f + 0.3f * d)).coerceAtLeast(0f)
                    rotate(d * 80f, Offset(cx, cy - lift)) {
                        personTile(cx, cy - lift, cell, sc, pink)
                    }
                    star5(Offset(cx, cy - lift), cell * 0.42f * (1f - d), cell * 0.18f * (1f - d), d * 3f, Color(0xFFFFE14D).copy(alpha = 1f - d))
                }
                else -> drawCircle(dark.copy(alpha = 0.1f), cell * 0.11f, Offset(cx, cy))
            }
        }
    }

    // The bus doing the peeling, with a little sparkle trail.
    for (i in 0 until 3) {
        val tx = busX - busW * (0.5f + i * 0.26f)
        drawCircle(Color.White.copy(alpha = 0.5f - i * 0.15f), cell * (0.11f - i * 0.025f), Offset(tx, laneY - busW * 0.22f))
    }
    glossyBus(Offset(busX, laneY), busW, base)
}

// ---- Sand Sweep: a dump truck rides the conveyor belt and scoops the sand mound ----

// Sand Sweep explains the real loop: a star sculpted in fine sand sits on the belt; the freed truck
// rides along and the sand pours down into its bed column by column, scooping the picture away.
// Then the sand rains back to rebuild it, so the screen is never empty.
private fun DrawScope.sandIntro(p: Float, base: Color, dark: Color) {
    val w = size.width
    val h = size.height
    val beltY = h * 0.82f
    val beltL = w * 0.06f
    val beltR = w * 0.94f
    val beltH = h * 0.09f
    val rollR = beltH * 0.6f
    val beltTop = beltY - beltH / 2

    // The belt (rollers, band, moving chevrons) — the sand sits on top of it.
    drawCircle(Color(0xFF50555E), rollR, Offset(beltL, beltY))
    drawCircle(Color(0xFF50555E), rollR, Offset(beltR, beltY))
    drawCircle(Color(0xFF2B2E33), rollR * 0.4f, Offset(beltL, beltY))
    drawCircle(Color(0xFF2B2E33), rollR * 0.4f, Offset(beltR, beltY))
    drawRoundRect(Color(0xFF3A3D44), Offset(beltL, beltY - beltH / 2), Size(beltR - beltL, beltH), CornerRadius(beltH * 0.2f))
    run {
        val spacing = w * 0.09f
        val shift = (p * spacing * 6f) % spacing
        var cxx = beltL + shift
        while (cxx < beltR) {
            val ch = beltH * 0.26f
            drawLine(Color(0xFF6E737B), Offset(cxx, beltY - ch), Offset(cxx + ch, beltY), 3f, StrokeCap.Round)
            drawLine(Color(0xFF6E737B), Offset(cxx, beltY + ch), Offset(cxx + ch, beltY), 3f, StrokeCap.Round)
            cxx += spacing
        }
    }

    // A clean star painted in fine sand. The truck rides through once per loop (off-screen at both
    // ends, so the loop is seamless); where it is, it carves a gap and fine sand pours out of the
    // gap into its bed. The star is whole everywhere else, so it never looks empty.
    val plateL = w * 0.1f
    val plateR = w * 0.9f
    val plateTop = h * 0.13f
    val plateBot = beltTop - h * 0.012f
    val cx = (plateL + plateR) / 2f
    val cyc = (plateTop + plateBot) / 2f
    val rOut = min((plateR - plateL) / 2f, (plateBot - plateTop) / 2f)
    val star = starShape(cx, cyc, rOut, rOut * 0.46f)
    val bounds = Rect(cx - rOut, cyc - rOut, cx + rOut, cyc + rOut)

    val truckW = w * 0.2f
    val rIn = rOut * 0.46f
    // Hold the full star, then the truck rides gradually across clearing it, then it rises back.
    val sweepT = seg(p, 0.18f, 0.82f)   // truck travels across the star's width, clearing behind it
    val rebuild = seg(p, 0.85f, 1f)     // a fresh star rises back in from the bottom
    val frontX = mix(bounds.left - truckW * 0.8f, bounds.right + truckW * 0.8f, sweepT)

    // Which part of the star still has sand: right of the truck while sweeping (left stays cleared),
    // then rising from the bottom while rebuilding. Seamless and empty only for an instant.
    val pr = if (rebuild > 0f) {
        val fillY = mix(bounds.bottom + 8f, bounds.top - 8f, rebuild)
        Rect(bounds.left - 8f, fillY, bounds.right + 8f, size.height)
    } else {
        Rect(min(frontX, bounds.right + 8f), bounds.top - 8f, bounds.right + 8f, bounds.bottom + 8f)
    }

    clipRect(pr.left, pr.top, pr.right, pr.bottom) {
        drawPath(starShape(cx, cyc + h * 0.012f, rOut, rIn), Color.Black.copy(alpha = 0.12f))
    }
    clipPath(star) { clipRect(pr.left, pr.top, pr.right, pr.bottom) { drawSmoothSand(bounds) } }
    clipRect(pr.left, pr.top, pr.right, pr.bottom) { drawPath(star, Color(0xFF9C6A20), style = Stroke(w * 0.007f)) }

    // While sweeping, fine sand pours from the cleared edge down into the truck bed.
    if (rebuild <= 0f && frontX > plateL - truckW * 0.1f && frontX < plateR + truckW * 0.1f) {
        val streamTop = cyc + rOut * 0.05f
        val streamBot = beltTop - truckW * 0.1f
        for (i in 0 until 18) {
            val t2 = frac(i * 0.153f + p * 5f)
            val y = mix(streamTop, streamBot, t2)
            val x = frontX - truckW * 0.19f + sin(i * 2.1f + p * 24f) * truckW * 0.05f
            drawCircle(Color(0xFFDDAA4C).copy(alpha = (1f - t2) * 0.9f), truckW * (0.035f - t2 * 0.012f), Offset(x, y))
        }
    }

    // The truck rides across only while sweeping (off-screen during the rebuild), bed filling as it
    // scoops. Cool blue so it never blends with the sand.
    if (rebuild <= 0f) {
        val truckColor = Color(0xFF3B86E8)
        val truckC = Offset(frontX - truckW * 0.35f, beltY - rollR - truckW * 0.12f)
        glossyTruck(truckC, truckW, truckColor, smooth(sweepT))
    }
}
