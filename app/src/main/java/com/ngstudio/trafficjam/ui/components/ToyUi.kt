package com.ngstudio.trafficjam.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.trafficjam.game.render.shade
import com.ngstudio.trafficjam.game.render.tint
import com.ngstudio.trafficjam.ui.theme.GameText
import com.ngstudio.trafficjam.ui.theme.LocalTabPalette
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate

/**
 * Toy billboard used as the header of every main tab: a glossy board in the tab's colors with a
 * thick cartoon outline, metal bolts and two wooden posts, the title in 3D lettering, and an
 * optional cream ribbon underneath for the subtitle.
 */
@Composable
fun ToySign(title: String, subtitle: (@Composable () -> Unit)? = null) {
    val pal = LocalTabPalette.current
    Column(
        Modifier.fillMaxWidth().statusBarsPadding().padding(top = 6.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .widthIn(min = 230.dp)
                .drawBehind {
                    // Two wooden posts behind the board, running down out of sight.
                    for (fx in floatArrayOf(0.22f, 0.78f)) {
                        val x = size.width * fx
                        drawRoundRect(Color(0xFF5D3A24), Offset(x - 5.dp.toPx(), size.height * 0.5f), Size(10.dp.toPx(), size.height * 0.75f), CornerRadius(3.dp.toPx()))
                        drawRoundRect(Color(0xFF8D5A3B), Offset(x - 3.5.dp.toPx(), size.height * 0.5f), Size(5.dp.toPx(), size.height * 0.75f), CornerRadius(2.dp.toPx()))
                    }
                    drawToyBoard(pal.headerTop, pal.headerBottom, pal.outline, 18.dp.toPx())
                    // Bolts in the corners.
                    for ((bx, by) in listOf(0.06f to 0.22f, 0.94f to 0.22f, 0.06f to 0.7f, 0.94f to 0.7f)) {
                        val c = Offset(size.width * bx, size.height * by)
                        drawCircle(pal.outline, 4.5.dp.toPx(), c)
                        drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFB9C0CC)), c - Offset(1.dp.toPx(), 1.dp.toPx()), 4.dp.toPx()), 3.2.dp.toPx(), c)
                    }
                }
                .padding(horizontal = 30.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center,
        ) {
            OutlinedText(title, style = GameText.Title, fontSize = 30.sp, strokeWidth = 8f, outline = pal.outline)
        }
        if (subtitle != null) {
            Box(
                Modifier
                    .padding(top = 2.dp)
                    .drawBehind {
                        val r = CornerRadius(size.height / 2)
                        drawRoundRect(pal.outline, Offset(0f, 2.dp.toPx()), size, r)
                        drawRoundRect(pal.outline, Offset.Zero, size, r)
                        drawRoundRect(
                            Brush.verticalGradient(listOf(Color(0xFFFFFBEF), Color(0xFFFFE7BF))),
                            Offset(2.dp.toPx(), 2.dp.toPx()), Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), CornerRadius(size.height / 2 - 2.dp.toPx()),
                        )
                    }
                    .padding(horizontal = 14.dp, vertical = 3.dp),
                contentAlignment = Alignment.Center,
            ) { subtitle() }
        }
    }
}

/** The glossy board shape shared by signs: outline, darker lip, light bevel, gradient face, shine. */
fun DrawScope.drawToyBoard(top: Color, bottom: Color, outline: Color, radius: Float) =
    drawToyBoardAt(Offset.Zero, size, top, bottom, outline, radius)

/** [drawToyBoard] at any position and size (for tiles that stick out of their slot). */
fun DrawScope.drawToyBoardAt(at: Offset, sz: Size, top: Color, bottom: Color, outline: Color, radius: Float) {
    val lip = 5.dp.toPx()
    val o = 3.dp.toPx()
    val faceH = sz.height - lip
    val r = CornerRadius(radius)
    drawRoundRect(Color.Black.copy(alpha = 0.22f), at + Offset(0f, lip + 3.dp.toPx()), Size(sz.width, faceH), r)
    drawRoundRect(outline, at + Offset(-o, -o), Size(sz.width + 2 * o, sz.height + 2 * o), CornerRadius(radius + o))
    drawRoundRect(bottom.shade(0.62f), at + Offset(0f, lip), Size(sz.width, faceH), r)
    drawRoundRect(top.tint(0.5f), at, Size(sz.width, faceH), r)
    drawRoundRect(
        Brush.verticalGradient(listOf(top.tint(0.15f), top, bottom), at.y, at.y + faceH),
        at + Offset(2.5.dp.toPx(), 2.5.dp.toPx()), Size(sz.width - 5.dp.toPx(), faceH - 5.dp.toPx()), CornerRadius(radius - 2.dp.toPx()),
    )
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.45f), Color.White.copy(alpha = 0.05f)), at.y + 4.dp.toPx(), at.y + faceH * 0.5f),
        at + Offset(radius * 0.7f, 4.dp.toPx()), Size(sz.width - radius * 1.4f, faceH * 0.38f), CornerRadius(faceH * 0.19f),
    )
}

/**
 * Premium toy card: soft drop shadow, a dark cartoon outline, a colored 3D lip and frame, and a
 * cream face with a glossy top. Pad the content with [inner] so it sits on the cream face.
 */
fun Modifier.toyCard(accent: Color, outline: Color, radius: Dp = 22.dp, inner: Dp = 10.dp): Modifier =
    this
        .drawBehind {
            val lip = 6.dp.toPx()
            val o = 2.5.dp.toPx()
            val rr = radius.toPx()
            val faceH = size.height - lip
            drawRoundRect(Color.Black.copy(alpha = 0.2f), Offset(0f, lip + 4.dp.toPx()), Size(size.width, faceH), CornerRadius(rr))
            drawRoundRect(outline, Offset(-o, -o), Size(size.width + 2 * o, size.height + 2 * o), CornerRadius(rr + o))
            drawRoundRect(accent.shade(0.6f), Offset(0f, lip), Size(size.width, faceH), CornerRadius(rr))
            drawRoundRect(Brush.verticalGradient(listOf(accent.tint(0.35f), accent), 0f, faceH), Offset.Zero, Size(size.width, faceH), CornerRadius(rr))
            val f = 5.dp.toPx()
            drawRoundRect(
                Brush.verticalGradient(listOf(Color(0xFFFFFDF6), Color(0xFFFFF3DA), Color(0xFFFFE6BE)), f, faceH - f),
                Offset(f, f), Size(size.width - 2 * f, faceH - 2 * f), CornerRadius(rr - f),
            )
            drawRoundRect(
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.7f), Color.Transparent), f, f + 18.dp.toPx()),
                Offset(f + 8.dp.toPx(), f + 2.dp.toPx()), Size(size.width - 2 * f - 16.dp.toPx(), 14.dp.toPx()), CornerRadius(7.dp.toPx()),
            )
        }
        .padding(start = inner, end = inner, top = inner, bottom = inner + 6.dp)

/** Spacing helper for stacks of toy cards (room for the lip and outline). */
val ToyGap = Arrangement.spacedBy(18.dp)

/**
 * One cartoon leaf: an almond shape with a dark edge, a light-to-dark gradient, a center vein
 * and a soft highlight. [angle] is in degrees, 0 pointing straight up from [base].
 */
fun DrawScope.drawLeaf(base: Offset, angle: Float, length: Float, width: Float) {
    rotate(angle, base) {
        val tip = Offset(base.x, base.y - length)
        val p = Path().apply {
            moveTo(base.x, base.y)
            cubicTo(base.x - width, base.y - length * 0.25f, base.x - width * 0.7f, base.y - length * 0.85f, tip.x, tip.y)
            cubicTo(base.x + width * 0.7f, base.y - length * 0.85f, base.x + width, base.y - length * 0.25f, base.x, base.y)
            close()
        }
        drawPath(p, Color(0xFF1E6B2C), style = Stroke(width * 0.22f, join = StrokeJoin.Round))
        drawPath(p, Brush.horizontalGradient(listOf(Color(0xFF9BE865), Color(0xFF52C247), Color(0xFF2C9440)), base.x - width, base.x + width))
        drawLine(Color(0xFF1F7A30).copy(alpha = 0.7f), base, Offset(base.x, base.y - length * 0.85f), width * 0.12f)
        val shine = Path().apply {
            moveTo(base.x - width * 0.15f, base.y - length * 0.2f)
            quadraticTo(base.x - width * 0.65f, base.y - length * 0.45f, base.x - width * 0.2f, base.y - length * 0.75f)
            quadraticTo(base.x - width * 0.35f, base.y - length * 0.45f, base.x - width * 0.15f, base.y - length * 0.2f)
            close()
        }
        drawPath(shine, Color.White.copy(alpha = 0.35f))
    }
}

/** A sprig of three leaves fanning out from [base] around [angle] (0 = up), sized by [scale] (1 ≈ 30dp). */
fun DrawScope.drawLeafSprig(base: Offset, angle: Float, scale: Float = 1f) {
    val u = 1.dp.toPx() * scale
    drawLeaf(base, angle - 38f, 24 * u, 9 * u)
    drawLeaf(base, angle + 38f, 22 * u, 8.5f * u)
    drawLeaf(base, angle, 30 * u, 10.5f * u)
}
