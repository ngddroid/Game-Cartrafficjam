package com.puzzel.lab.cartrafficjam.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.puzzel.lab.cartrafficjam.App
import com.puzzel.lab.cartrafficjam.core.ads.FakeAdManager
import com.puzzel.lab.cartrafficjam.core.audio.AudioManager
import com.puzzel.lab.cartrafficjam.core.audio.Haptic
import com.puzzel.lab.cartrafficjam.core.audio.Sfx
import com.puzzel.lab.cartrafficjam.game.render.shade
import com.puzzel.lab.cartrafficjam.game.render.tint
import com.puzzel.lab.cartrafficjam.ui.theme.BodyFont
import com.puzzel.lab.cartrafficjam.ui.theme.GameColors
import com.puzzel.lab.cartrafficjam.ui.theme.GameText
import com.puzzel.lab.cartrafficjam.ui.theme.TitleFont
import kotlinx.coroutines.delay

val LocalAudio = compositionLocalOf<AudioManager> { App.container.audio }

/** Game-style text: a thick dark stroke under a filled face. */
@Composable
fun OutlinedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = GameText.Title,
    color: Color = Color.White,
    outline: Color = Color(0xFF2B1B0E),
    strokeWidth: Float = 8f,
    fontSize: TextUnit = style.fontSize,
    textAlign: TextAlign = TextAlign.Center,
) {
    Box(modifier) {
        Text(
            text, style = style.copy(color = outline, fontSize = fontSize, drawStyle = Stroke(strokeWidth, join = androidx.compose.ui.graphics.StrokeJoin.Round), shadow = null),
            textAlign = textAlign,
        )
        Text(text, style = style.copy(color = color, fontSize = fontSize, shadow = null), textAlign = textAlign)
    }
}

enum class ButtonColor(val base: Color) {
    ORANGE(Color(0xFFFFA000)), GREEN(Color(0xFF5FC12F)), BLUE(Color(0xFF2F8CF6)), PURPLE(Color(0xFF8E5CF0)),
    RED(Color(0xFFF2453D)), YELLOW(Color(0xFFFFC21A)), GRAY(Color(0xFF9AA3AE)),
}

/** Glossy 3D pill button with a pressed-down squash, click sound and haptic. */
@Composable
fun GameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: ButtonColor = ButtonColor.ORANGE,
    height: Dp = 60.dp,
    fontSize: TextUnit = 24.sp,
    enabled: Boolean = true,
    sound: Boolean = true,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
) {
    val audio = LocalAudio.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 1f else 0f, spring(stiffness = Spring.StiffnessHigh), label = "press")
    val base = if (enabled) color.base else ButtonColor.GRAY.base
    val lip = 6.dp
    Box(
        modifier
            .height(height)
            .graphicsLayer { scaleX = 1f - press * 0.03f; scaleY = 1f - press * 0.03f }
            .clickable(interaction, indication = null, enabled = enabled) {
                if (sound) audio.play(Sfx.Click)
                audio.haptic(Haptic.Tap)
                onClick()
            }
            .drawBehind {
                val r = CornerRadius(size.height / 2)
                val lipPx = lip.toPx() * (1f - press * 0.7f)
                val top = lip.toPx() - lipPx
                drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(0f, lip.toPx() + 2.dp.toPx()), Size(size.width, size.height - lip.toPx()), r)
                drawRoundRect(base.shade(0.62f), Offset(0f, top + lipPx), Size(size.width, size.height - lip.toPx()), r)
                drawRoundRect(Color(0xFF2B1B0E).copy(alpha = 0.55f), Offset(0f, top), Size(size.width, size.height - lip.toPx()), r, style = Stroke(2.dp.toPx()))
                drawRoundRect(
                    Brush.verticalGradient(listOf(base.tint(0.35f), base, base.shade(0.9f)), top, top + size.height - lip.toPx()),
                    Offset(1.5.dp.toPx(), top + 1.5.dp.toPx()), Size(size.width - 3.dp.toPx(), size.height - lip.toPx() - 3.dp.toPx()), r,
                )
                drawRoundRect(
                    Color.White.copy(alpha = 0.38f), Offset(size.height * 0.28f, top + 4.dp.toPx()),
                    Size(size.width - size.height * 0.56f, (size.height - lip.toPx()) * 0.3f), CornerRadius(size.height / 4),
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Row(
            Modifier
                .offset(y = -lip / 2 + lip * press * 0.7f)
                .padding(horizontal = 18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            leading?.let { it(); Spacer(Modifier.width(8.dp)) }
            OutlinedText(text, style = GameText.Button, fontSize = fontSize, outline = base.shade(0.45f), strokeWidth = 6f)
            trailing?.let { Spacer(Modifier.width(8.dp)); it() }
        }
    }
}

/** Round icon button (gear, close, back…) with a lip and gloss. */
@Composable
fun RoundButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    color: Color = Color(0xFF2F8CF6),
    content: @Composable BoxScope.() -> Unit,
) {
    val audio = LocalAudio.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val press by animateFloatAsState(if (pressed) 0.9f else 1f, spring(stiffness = Spring.StiffnessHigh), label = "rb")
    Box(
        modifier
            .size(size)
            .scale(press)
            .clickable(interaction, null) { audio.play(Sfx.Click); audio.haptic(Haptic.Tap); onClick() }
            .drawBehind {
                val r = this.size.minDimension / 2
                drawCircle(Color.Black.copy(alpha = 0.25f), r, center + Offset(0f, 3.dp.toPx()))
                drawCircle(Color.White, r, center)
                drawCircle(Brush.verticalGradient(listOf(color.tint(0.35f), color, color.shade(0.8f))), r - 3.dp.toPx(), center)
                drawOval(Color.White.copy(alpha = 0.4f), Offset(r * 0.45f, r * 0.25f), Size(r * 1.1f, r * 0.5f))
            },
        contentAlignment = Alignment.Center,
        content = content,
    )
}

@Composable
fun CloseButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    RoundButton(onClick, modifier, size = 44.dp, color = Color(0xFFF2453D)) {
        Canvas(Modifier.size(18.dp)) {
            val w = 5.dp.toPx()
            drawLine(Color.White, Offset.Zero, Offset(size.width, size.height), w, StrokeCap.Round)
            drawLine(Color.White, Offset(size.width, 0f), Offset(0f, size.height), w, StrokeCap.Round)
        }
    }
}

/** Cream card with a thick blue frame and a ribbon title; the house style for every dialog. */
@Composable
fun GamePanel(
    title: String,
    modifier: Modifier = Modifier,
    frame: Color = Color(0xFF2F7CF6),
    onClose: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(modifier.widthIn(max = 380.dp)) {
        Column(
            Modifier
                .padding(top = 26.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(30.dp))
                .background(frame.shade(0.7f), RoundedCornerShape(30.dp))
                .padding(bottom = 6.dp)
                .background(Brush.verticalGradient(listOf(frame.tint(0.25f), frame)), RoundedCornerShape(30.dp))
                .padding(9.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFFDF5), GameColors.Cream, Color(0xFFFFEDC2))), RoundedCornerShape(22.dp))
                .padding(top = 40.dp, start = 18.dp, end = 18.dp, bottom = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
        // Ribbon title
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .height(56.dp)
                .widthIn(min = 200.dp)
                .drawBehind {
                    val r = CornerRadius(22.dp.toPx())
                    drawRoundRect(frame.shade(0.55f), Offset(0f, 5.dp.toPx()), size, r)
                    drawRoundRect(Brush.verticalGradient(listOf(frame.tint(0.45f), frame.tint(0.1f))), Offset.Zero, Size(size.width, size.height - 2.dp.toPx()), r)
                    drawRoundRect(Color.White.copy(alpha = 0.3f), Offset(14.dp.toPx(), 5.dp.toPx()), Size(size.width - 28.dp.toPx(), 12.dp.toPx()), CornerRadius(6.dp.toPx()))
                }
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            OutlinedText(title, style = GameText.Title, fontSize = 28.sp, outline = frame.shade(0.4f), strokeWidth = 7f)
        }
        if (onClose != null) {
            CloseButton(onClose, Modifier.align(Alignment.TopEnd).offset(x = 6.dp, y = 14.dp))
        }
    }
}

/** Full-screen modal overlay with a dimmed scrim and a springy entrance. */
@Composable
fun GameDialog(
    visible: Boolean,
    onDismiss: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val audio = LocalAudio.current
    LaunchedEffect(visible) { if (visible) audio.play(Sfx.Open) }
    if (visible && onDismiss != null) BackHandler { onDismiss() }
    AnimatedVisibility(visible, enter = fadeIn(tween(160)), exit = fadeOut(tween(140)), modifier = Modifier.zIndex(10f)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0xB3000814))
                .clickable(remember { MutableInteractionSource() }, null) { },
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = true,
                enter = scaleIn(spring(dampingRatio = 0.55f, stiffness = 420f), initialScale = 0.6f),
                exit = scaleOut(targetScale = 0.8f),
            ) {
                Box(Modifier.padding(22.dp), contentAlignment = Alignment.Center, content = content)
            }
        }
    }
}

@Composable
fun CoinPill(coins: Int, modifier: Modifier = Modifier, onPlus: (() -> Unit)? = null) {
    val shown = remember { Animatable(coins.toFloat()) }
    LaunchedEffect(coins) { shown.animateTo(coins.toFloat(), tween(650, easing = FastOutSlowInEasing)) }
    Box(modifier.height(40.dp), contentAlignment = Alignment.CenterStart) {
        Box(
            Modifier
                .padding(start = 18.dp)
                .height(32.dp)
                .widthIn(min = 92.dp)
                .background(Color(0x55000000), RoundedCornerShape(16.dp))
                .padding(2.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFFFFFBEF), Color(0xFFF6E7C4))), RoundedCornerShape(14.dp))
                .padding(start = 26.dp, end = if (onPlus != null) 30.dp else 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(shown.value.toInt().toString(), style = GameText.Label.copy(fontFamily = TitleFont, fontSize = 18.sp, color = Color(0xFF4A2E12)))
        }
        GameIcon(Icon.COIN, 40.dp)
        if (onPlus != null) {
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF5FC12F))
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { onPlus() },
                contentAlignment = Alignment.Center,
            ) { Text("+", style = GameText.Button.copy(fontSize = 18.sp)) }
        }
    }
}

/** Big ON/OFF switch with a sliding knob. */
@Composable
fun GameToggle(checked: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val audio = LocalAudio.current
    val x by animateFloatAsState(if (checked) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "toggle")
    val track = androidx.compose.ui.graphics.lerp(Color(0xFFB0B7C3), Color(0xFF5FC12F), x)
    Box(
        modifier
            .size(76.dp, 38.dp)
            .clickable(remember { MutableInteractionSource() }, null) { audio.play(Sfx.Click); audio.haptic(Haptic.Tap); onChange(!checked) }
            .drawBehind {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(track.shade(0.65f), Offset(0f, 3.dp.toPx()), size, r)
                drawRoundRect(track, Offset.Zero, Size(size.width, size.height - 2.dp.toPx()), r)
                drawRoundRect(Color.Black.copy(alpha = 0.12f), Offset(3.dp.toPx(), 3.dp.toPx()), Size(size.width - 6.dp.toPx(), size.height * 0.4f), r)
            },
    ) {
        Text(
            if (checked) "ON" else "OFF",
            style = GameText.Small.copy(color = Color.White, fontFamily = TitleFont, fontSize = 13.sp),
            modifier = Modifier.align(if (checked) Alignment.CenterStart else Alignment.CenterEnd).padding(horizontal = 10.dp),
        )
        Box(
            Modifier
                .offset(x = 3.dp + 38.dp * x, y = 2.dp)
                .size(32.dp)
                .shadow(3.dp, CircleShape)
                .background(Brush.verticalGradient(listOf(Color.White, Color(0xFFE6EAF0))), CircleShape),
        )
    }
}

/** Row of stars that pop in one after another. */
@Composable
fun StarRow(stars: Int, size: Dp, modifier: Modifier = Modifier, animate: Boolean = false, onStar: (Int) -> Unit = {}) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(size * -0.08f), verticalAlignment = Alignment.Bottom) {
        for (i in 0 until 3) {
            val earned = i < stars
            val anim = remember { Animatable(if (animate) 0f else 1f) }
            LaunchedEffect(animate) {
                if (animate) {
                    delay(350L + i * 380L)
                    if (earned) onStar(i)
                    anim.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 300f))
                }
            }
            val lift = if (i == 1) -size * 0.18f else 0.dp
            Box(Modifier.offset(y = lift).graphicsLayer { scaleX = anim.value; scaleY = anim.value; rotationZ = (1 - anim.value) * 90f }) {
                GameIcon(if (earned) Icon.STAR else Icon.STAR_EMPTY, if (i == 1) size * 1.15f else size)
            }
        }
    }
}

/** A horizontal ribbon banner with folded tails (used for "Level Complete!" and friends). */
@Composable
fun RibbonBanner(text: String, color: Color, modifier: Modifier = Modifier, fontSize: TextUnit = 30.sp) {
    Box(
        modifier
            .height(64.dp)
            .drawBehind {
                val w = size.width
                val h = size.height
                val tail = 28.dp.toPx()
                val dark = color.shade(0.6f)
                // tails
                val lt = androidx.compose.ui.graphics.Path().apply {
                    moveTo(0f, h * 0.25f); lineTo(tail * 1.6f, h * 0.25f); lineTo(tail * 1.6f, h); lineTo(0f, h); lineTo(tail * 0.6f, h * 0.62f); close()
                }
                val rt = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w, h * 0.25f); lineTo(w - tail * 1.6f, h * 0.25f); lineTo(w - tail * 1.6f, h); lineTo(w, h); lineTo(w - tail * 0.6f, h * 0.62f); close()
                }
                drawPath(lt, dark)
                drawPath(rt, dark)
                drawRoundRect(color.shade(0.5f), Offset(tail, 4.dp.toPx()), Size(w - 2 * tail, h * 0.8f), CornerRadius(10.dp.toPx()))
                drawRoundRect(Brush.verticalGradient(listOf(color.tint(0.35f), color)), Offset(tail, 0f), Size(w - 2 * tail, h * 0.78f), CornerRadius(10.dp.toPx()))
                drawRoundRect(Color.White.copy(alpha = 0.3f), Offset(tail + 10.dp.toPx(), 5.dp.toPx()), Size(w - 2 * tail - 20.dp.toPx(), 10.dp.toPx()), CornerRadius(5.dp.toPx()))
            }
            .padding(horizontal = 48.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        OutlinedText(text, Modifier.padding(top = 6.dp), style = GameText.Title, fontSize = fontSize, outline = color.shade(0.4f), strokeWidth = 7f)
    }
}

/** "Watch an ad to unlock" dialog used for levels, collections, slots and skins. */
@Composable
fun WatchAdDialog(
    visible: Boolean,
    title: String,
    message: String,
    onWatch: () -> Unit,
    onDismiss: () -> Unit,
    icon: Icon = Icon.LOCK,
) {
    GameDialog(visible, onDismiss) {
        GamePanel(title, onClose = onDismiss) {
            val pulse = rememberInfiniteTransition(label = "lock")
            val s by pulse.animateFloat(0.94f, 1.06f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "s")
            Box(
                Modifier
                    .size(120.dp)
                    .background(Brush.radialGradient(listOf(Color(0xFFFFF3C4), Color(0x00FFF3C4))), CircleShape),
                contentAlignment = Alignment.Center,
            ) { GameIcon(icon, 86.dp, Modifier.scale(s)) }
            Spacer(Modifier.height(8.dp))
            Text(message, style = GameText.Body.copy(fontSize = 17.sp, textAlign = TextAlign.Center))
            Spacer(Modifier.height(20.dp))
            GameButton(
                "Watch Ad", onWatch, Modifier.fillMaxWidth(), color = ButtonColor.GREEN,
                leading = { GameIcon(Icon.AD, 34.dp) },
            )
        }
    }
}

@Composable
fun ConfirmDialog(
    visible: Boolean,
    title: String,
    message: String,
    confirm: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    cancel: String = "Cancel",
) {
    GameDialog(visible, onDismiss) {
        GamePanel(title, onClose = onDismiss) {
            Text(message, style = GameText.Body.copy(fontSize = 17.sp, textAlign = TextAlign.Center))
            Spacer(Modifier.height(22.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GameButton(cancel, onDismiss, Modifier.weight(1f), color = ButtonColor.BLUE, fontSize = 20.sp)
                GameButton(confirm, onConfirm, Modifier.weight(1f), color = ButtonColor.RED, fontSize = 20.sp)
            }
        }
    }
}

/** Placeholder full-screen rewarded ad. Swap for a real network later. */
@Composable
fun AdOverlay(ads: FakeAdManager) {
    val req = ads.active
    val audio = LocalAudio.current
    AnimatedVisibility(req != null, enter = fadeIn(), exit = fadeOut(), modifier = Modifier.zIndex(100f)) {
        var left by remember { mutableIntStateOf(3) }
        var done by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            audio.duck(true)
            while (left > 0) { delay(1000); left-- }
            done = true
        }
        BackHandler { }
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFF1B1446), Color(0xFF3A2475))))
                .clickable(remember { MutableInteractionSource() }, null) { },
            contentAlignment = Alignment.Center,
        ) {
            val spin = rememberInfiniteTransition(label = "ad")
            val a by spin.animateFloat(0f, 360f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "a")
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                GameIcon(Icon.AD, 110.dp, Modifier.graphicsLayer { rotationZ = if (done) 0f else a * 0.05f })
                Spacer(Modifier.height(18.dp))
                OutlinedText("Rewarded Ad", style = GameText.Title, fontSize = 30.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    "Ads will appear here once an ad network is connected.",
                    style = GameText.Body.copy(color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center),
                    modifier = Modifier.padding(horizontal = 40.dp),
                )
                Spacer(Modifier.height(26.dp))
                if (done) {
                    GameButton("Claim Reward", {
                        audio.duck(false)
                        audio.play(Sfx.Unlock)
                        ads.finish(true)
                    }, Modifier.width(240.dp), color = ButtonColor.GREEN)
                } else {
                    Box(
                        Modifier.size(64.dp).border(4.dp, Color.White.copy(alpha = 0.8f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Text("$left", style = GameText.Title.copy(fontSize = 28.sp)) }
                }
            }
            if (done) {
                CloseButton({ audio.duck(false); ads.finish(false) }, Modifier.align(Alignment.TopEnd).padding(top = 48.dp, end = 16.dp))
            }
        }
    }
}

/** Short message that slides in near the top and fades away. */
@Composable
fun GameToast(message: String?, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        message != null,
        modifier = modifier,
        enter = fadeIn() + scaleIn(initialScale = 0.7f),
        exit = fadeOut(),
    ) {
        Text(
            message ?: "",
            style = GameText.Label.copy(color = Color.White, fontFamily = BodyFont, fontSize = 16.sp),
            modifier = Modifier
                .background(Color(0xE61A3A6B), RoundedCornerShape(20.dp))
                .border(2.dp, Color.White.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
                .padding(horizontal = 18.dp, vertical = 10.dp),
        )
    }
}

/** Soft sky backdrop with drifting clouds shared by menu screens. */
@Composable
fun SkyBackground(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val t = rememberInfiniteTransition(label = "sky")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(40000, easing = LinearEasing)), label = "drift")
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF5CC8FF), Color(0xFF2D8EF0), Color(0xFF1E6FD9))))
            .drawBehind {
                val w = size.width
                fun cloud(x: Float, y: Float, s: Float, a: Float) {
                    val c = Color.White.copy(alpha = a)
                    drawCircle(c, 28 * s, Offset(x, y))
                    drawCircle(c, 22 * s, Offset(x - 30 * s, y + 8 * s))
                    drawCircle(c, 24 * s, Offset(x + 30 * s, y + 6 * s))
                    drawRoundRect(c, Offset(x - 52 * s, y + 4 * s), Size(104 * s, 24 * s), CornerRadius(12 * s))
                }
                val span = w + 300f
                cloud((drift * span + 0.1f * span) % span - 150f, size.height * 0.1f, 1.4f, 0.35f)
                cloud((drift * span * 0.7f + 0.6f * span) % span - 150f, size.height * 0.22f, 1.0f, 0.25f)
                cloud((drift * span * 1.3f + 0.35f * span) % span - 150f, size.height * 0.42f, 1.2f, 0.18f)
                cloud((drift * span * 0.5f + 0.85f * span) % span - 150f, size.height * 0.63f, 0.9f, 0.15f)
            },
        content = content,
    )
}
