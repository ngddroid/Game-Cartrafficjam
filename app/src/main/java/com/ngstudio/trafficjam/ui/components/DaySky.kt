package com.ngstudio.trafficjam.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import kotlin.math.PI
import kotlin.math.sin

/** Colors of the bright daytime look used by the splash and onboarding screens. */
object Day {
    val SkyTop = Color(0xFF4FB3F6)
    val SkyMid = Color(0xFF8ED2FF)
    val SkyBottom = Color(0xFFD6F2FF)
    /** Navy used for outlines and text on the light sky. */
    val Ink = Color(0xFF12386B)
    val HillFar = Color(0xFFB4E38A)
    val HillMid = Color(0xFF93D35F)
    val HillNear = Color(0xFF7CC84A)
    val Accent = Color(0xFFFF8A1F)
}

/**
 * A sunny sky: blue gradient, a glowing sun, fluffy clouds drifting by and, when [hillsTop] is
 * given, rolling green hills whose crest sits [hillsTop] above the bottom edge.
 */
@Composable
fun DaySkyBackground(
    modifier: Modifier = Modifier,
    hillsTop: Dp? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val t = rememberInfiniteTransition(label = "daysky")
    val drift by t.animateFloat(0f, 1f, infiniteRepeatable(tween(46000, easing = LinearEasing)), label = "drift")
    val glow by t.animateFloat(0f, 1f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "glow")
    Box(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Day.SkyTop, Day.SkyMid, Day.SkyBottom)))
            .drawBehind {
                drawSun(Offset(size.width * 0.84f, size.height * 0.11f), size.minDimension * 0.085f, glow)
                val span = size.width + 360f
                cloud((drift * span + 0.05f * span) % span - 180f, size.height * 0.08f, 1.25f)
                cloud((drift * span * 0.7f + 0.55f * span) % span - 180f, size.height * 0.2f, 0.95f)
                cloud((drift * span * 1.25f + 0.3f * span) % span - 180f, size.height * 0.37f, 1.1f)
                cloud((drift * span * 0.55f + 0.8f * span) % span - 180f, size.height * 0.54f, 0.8f)
                if (hillsTop != null) drawHills(size.height - hillsTop.toPx())
            },
        content = content,
    )
}

private fun DrawScope.drawSun(c: Offset, r: Float, glow: Float) {
    drawCircle(Brush.radialGradient(listOf(Color(0x66FFF3B0), Color(0x00FFF3B0)), c, r * (2.6f + 0.25f * glow)), r * (2.6f + 0.25f * glow), c)
    drawCircle(Color(0xFFFFC93C), r * 1.06f, c)
    drawCircle(Brush.radialGradient(listOf(Color(0xFFFFF59D), Color(0xFFFFD54F)), c - Offset(r * 0.25f, r * 0.25f), r), r, c)
}

/** A fluffy cloud with a soft blue-grey underside so it reads as 3D. */
private fun DrawScope.cloud(x: Float, y: Float, s: Float) {
    val shade = Color(0xFFDCEFFC)
    drawRoundRect(shade, Offset(x - 56 * s, y + 10 * s), Size(112 * s, 26 * s), CornerRadius(13 * s))
    drawCircle(shade, 24 * s, Offset(x - 30 * s, y + 12 * s))
    drawCircle(shade, 26 * s, Offset(x + 30 * s, y + 10 * s))
    val white = Color.White
    drawCircle(white, 30 * s, Offset(x, y))
    drawCircle(white, 22 * s, Offset(x - 32 * s, y + 6 * s))
    drawCircle(white, 25 * s, Offset(x + 30 * s, y + 4 * s))
    drawRoundRect(white, Offset(x - 54 * s, y + 4 * s), Size(108 * s, 22 * s), CornerRadius(11 * s))
}

/** Three layers of rolling hills, lightest at the back. */
private fun DrawScope.drawHills(crest: Float) {
    val w = size.width
    fun hill(color: Color, base: Float, amp: Float, waves: Float, phase: Float) {
        val p = Path().apply {
            moveTo(0f, size.height)
            var x = 0f
            while (x <= w) {
                lineTo(x, base - amp * (0.5f + 0.5f * sin((x / w) * waves * 2f * PI.toFloat() + phase)))
                x += 6f
            }
            lineTo(w, size.height)
            close()
        }
        drawPath(p, color)
    }
    hill(Day.HillFar, crest + 10f, 70f, 1.3f, 0.6f)
    hill(Day.HillMid, crest + 40f, 55f, 1.8f, 2.4f)
    hill(Day.HillNear, crest + 70f, 40f, 2.4f, 4.1f)
}
