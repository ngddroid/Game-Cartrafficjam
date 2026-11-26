package com.ngstudio.trafficjam.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import com.ngstudio.trafficjam.game.model.VehicleType
import com.ngstudio.trafficjam.game.render.Skin
import com.ngstudio.trafficjam.game.render.VehicleArt
import com.ngstudio.trafficjam.ui.theme.GameText
import com.ngstudio.trafficjam.ui.theme.Sunset

/** "TRAFFIC JAM RUSH" stacked logo with a gentle idle wobble. */
@Composable
fun GameLogo(modifier: Modifier = Modifier, scale: Float = 1f) {
    val t = rememberInfiniteTransition(label = "logo")
    val wob by t.animateFloat(-1.5f, 1.5f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "wob")
    Column(
        modifier.graphicsLayer { rotationZ = wob; scaleX = scale; scaleY = scale },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OutlinedText("TRAFFIC", style = GameText.Display, fontSize = 40.sp, color = Color(0xFFFFE14D), outline = Color(0xFF7A3B00), strokeWidth = 14f)
        Box(Modifier.offset(y = (-14).dp)) {
            OutlinedText("JAM", style = GameText.Display, fontSize = 72.sp, color = Color(0xFFFF8A1F), outline = Color(0xFF6B2300), strokeWidth = 18f)
        }
        Box(
            Modifier
                .offset(y = (-26).dp)
                .graphicsLayer { rotationZ = -4f }
                .drawBehind {
                    val r = CornerRadius(14.dp.toPx())
                    drawRoundRect(Color(0xFF8E1B1B), Offset(0f, 5.dp.toPx()), size, r)
                    drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFF6E6E), Color(0xFFE53935))), Offset.Zero, size, r)
                    drawRoundRect(Color.White.copy(alpha = 0.3f), Offset(10.dp.toPx(), 4.dp.toPx()), Size(size.width - 20.dp.toPx(), 8.dp.toPx()), CornerRadius(4.dp.toPx()))
                },
        ) {
            OutlinedText(
                "RUSH", Modifier.padding(horizontal = 22.dp).offset(y = (-2).dp), style = GameText.Display,
                fontSize = 36.sp, color = Color.White, outline = Color(0xFF7F0000), strokeWidth = 10f,
            )
        }
    }
}

/** Coney, the traffic-cone mascot. */
@Composable
fun Mascot(size: Dp, modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "coney")
    val blink by t.animateFloat(0f, 1f, infiniteRepeatable(tween(3200, easing = LinearEasing)), label = "blink")
    val bounce by t.animateFloat(0f, 1f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "b")
    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 100f
        val eyesOpen = blink < 0.95f
        val by = -bounce * 2f * s
        // base
        drawRoundRect(Color(0xFF263238), Offset(14 * s, 82 * s), Size(72 * s, 12 * s), CornerRadius(5 * s))
        val cone = Path().apply {
            moveTo(44 * s, 8 * s + by); quadraticTo(50 * s, 2 * s + by, 56 * s, 8 * s + by)
            lineTo(78 * s, 84 * s); lineTo(22 * s, 84 * s); close()
        }
        drawPath(cone, Brush.horizontalGradient(listOf(Color(0xFFFFA040), Color(0xFFFF7A00), Color(0xFFE65100)), 22 * s, 78 * s))
        drawPath(cone, Color(0xFF8A2E00), style = Stroke(3 * s))
        // stripes
        drawRect(Color.White, Offset(34 * s, 30 * s + by * 0.6f), Size(32 * s, 8 * s))
        drawRect(Color.White, Offset(27 * s, 60 * s), Size(46 * s, 8 * s))
        // face
        val ey = 46 * s + by * 0.5f
        if (eyesOpen) {
            drawCircle(Color.White, 6 * s, Offset(42 * s, ey))
            drawCircle(Color.White, 6 * s, Offset(58 * s, ey))
            drawCircle(Color(0xFF263238), 3.4f * s, Offset(43 * s, ey + 0.5f * s))
            drawCircle(Color(0xFF263238), 3.4f * s, Offset(59 * s, ey + 0.5f * s))
            drawCircle(Color.White, 1.2f * s, Offset(44 * s, ey - 1 * s))
            drawCircle(Color.White, 1.2f * s, Offset(60 * s, ey - 1 * s))
        } else {
            drawLine(Color(0xFF263238), Offset(37 * s, ey), Offset(47 * s, ey), 2.5f * s, StrokeCap.Round)
            drawLine(Color(0xFF263238), Offset(53 * s, ey), Offset(63 * s, ey), 2.5f * s, StrokeCap.Round)
        }
        drawArc(Color(0xFF6D1B00), 10f, 160f, false, Offset(43 * s, ey + 3 * s), Size(14 * s, 9 * s), style = Stroke(2.6f * s, cap = StrokeCap.Round))
        drawCircle(Color(0x66FF5252), 3.5f * s, Offset(35 * s, ey + 6 * s))
        drawCircle(Color(0x66FF5252), 3.5f * s, Offset(65 * s, ey + 6 * s))
        drawOval(Color.White.copy(alpha = 0.4f), Offset(40 * s, 12 * s + by), Size(6 * s, 14 * s))
    }
}

/** Mascot inside a framed avatar tile. */
@Composable
fun AvatarTile(size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(size)
            .drawBehind {
                val r = CornerRadius(14.dp.toPx())
                drawRoundRect(Sunset.Outline, Offset(0f, 3.dp.toPx()), this.size, r)
                drawRoundRect(Color.White, Offset.Zero, this.size, r)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFFD3A5), Color(0xFFFF9A62))), Offset(3.dp.toPx(), 3.dp.toPx()), Size(this.size.width - 6.dp.toPx(), this.size.height - 6.dp.toPx()), CornerRadius(11.dp.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) { Mascot(size * 0.84f) }
}

/** Shared art caches for menu sprites, keyed by pixel unit and skin. */
private object MenuArt {
    private val cache = HashMap<Pair<Int, Skin>, VehicleArt>()
    fun get(unitPx: Float, skin: Skin) = cache.getOrPut(unitPx.roundToInt() to skin) { VehicleArt(unitPx.roundToInt().toFloat(), skin) }
}

/** A 3D vehicle rendered with the in-game sprite stack, for menus. [rotation] is clockwise from north. */
@Composable
fun VehicleSprite(type: VehicleType, color: Int, skin: Skin, unit: Dp, modifier: Modifier = Modifier, rotation: Float = 0f) {
    val density = LocalDensity.current
    val unitPx = with(density) { unit.toPx() }
    val art = remember(unitPx, skin) { MenuArt.get(unitPx, skin) }
    val comp = art.composite(type, color, false, rotation)
    val shadow = art.shadow(type)
    val residual = art.residual(rotation)
    val w = with(density) { comp.bitmap.width.toDp() }
    val h = with(density) { comp.bitmap.height.toDp() }
    Canvas(modifier.size(w, h)) {
        withTransform({
            translate(comp.groundX + art.unit * 0.22f, comp.groundY + art.unit * 0.36f)
            rotate(rotation, Offset.Zero)
        }) { drawImage(shadow, Offset(-shadow.width / 2f, -shadow.height / 2f)) }
        withTransform({
            translate(comp.groundX, comp.groundY)
            rotate(residual, Offset.Zero)
        }) { drawImage(comp.bitmap, Offset(-comp.groundX, -comp.groundY)) }
    }
}

@Composable
fun SunRays(modifier: Modifier = Modifier, color: Color = Color(0x55FFFFFF)) {
    val t = rememberInfiniteTransition(label = "rays")
    val r by t.animateFloat(0f, 360f, infiniteRepeatable(tween(30000, easing = LinearEasing)), label = "r")
    Canvas(modifier) {
        rotate(r) {
            for (i in 0 until 16) {
                rotate(i * 22.5f) {
                    val p = Path().apply {
                        moveTo(center.x, center.y)
                        lineTo(center.x + size.maxDimension, center.y - size.maxDimension * 0.12f)
                        lineTo(center.x + size.maxDimension, center.y + size.maxDimension * 0.12f)
                        close()
                    }
                    drawPath(p, Brush.radialGradient(listOf(color, Color.Transparent), center, size.maxDimension * 0.6f))
                }
            }
        }
    }
}
