package com.ngstudio.trafficjam.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.trafficjam.ui.components.Icon
import com.ngstudio.trafficjam.ui.components.OutlinedText
import com.ngstudio.trafficjam.ui.components.drawGameIcon
import com.ngstudio.trafficjam.ui.theme.GameText
import com.ngstudio.trafficjam.ui.theme.TitleFont

// ---- Header ------------------------------------------------------------------------------

private val HeaderInk = Color(0xFF0E2E57)

/**
 * The Game Modes header: a little roadside scene (signpost with a gamepad, skyline, bushes, a
 * curved road with a school bus and a traffic light) behind a big 3D "Game Modes" title and a
 * purple subtitle ribbon.
 */
@Composable
fun ModesHeader() {
    Box(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(118.dp),
        contentAlignment = Alignment.TopCenter,
    ) {
        Canvas(Modifier.fillMaxSize()) { drawHeaderClouds() }
        Column(Modifier.padding(top = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row {
                OutlinedText("Game ", style = GameText.Display, fontSize = 40.sp, strokeWidth = 11f, outline = HeaderInk, color = Color.White)
                OutlinedText("Modes", style = GameText.Display, fontSize = 40.sp, strokeWidth = 11f, outline = HeaderInk, color = Color(0xFFFFC93C))
            }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier
                    .drawBehind {
                        val r = CornerRadius(size.height / 2)
                        drawRoundRect(HeaderInk, Offset(0f, 3.dp.toPx()), size, r)
                        drawRoundRect(HeaderInk, Offset.Zero, size, r)
                        drawRoundRect(
                            Brush.verticalGradient(listOf(Color(0xFF3FA6EE), Color(0xFF2E7FC8))),
                            Offset(2.dp.toPx(), 2.dp.toPx()), Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), CornerRadius(size.height / 2 - 2.dp.toPx()),
                        )
                        drawRoundRect(
                            Color.White.copy(alpha = 0.25f), Offset(size.height * 0.4f, 4.dp.toPx()),
                            Size(size.width - size.height * 0.8f, 5.dp.toPx()), CornerRadius(3.dp.toPx()),
                        )
                    }
                    .padding(horizontal = 18.dp, vertical = 5.dp),
            ) {
                Text("Four ways to beat the traffic", style = GameText.Label.copy(color = Color.White, fontFamily = TitleFont, fontSize = 16.sp))
            }
        }
    }
}

private fun DrawScope.drawHeaderClouds() {
    val w = size.width
    val h = size.height
    val u = w / 360f
    // Just soft clouds and gold sparkles; the one shared town scene shows through behind.
    fun cloud(cx: Float, cy: Float, s: Float) {
        val c = Color.White.copy(alpha = 0.85f)
        drawCircle(c, 13 * s, Offset(cx - 12 * s, cy + 3 * s))
        drawCircle(c, 17 * s, Offset(cx + 4 * s, cy - 2 * s))
        drawCircle(c, 11 * s, Offset(cx + 20 * s, cy + 4 * s))
        drawRoundRect(c, Offset(cx - 25 * s, cy + 3 * s), Size(55 * s, 12 * s), CornerRadius(6 * s))
    }
    cloud(w * 0.11f, h * 0.12f, 1.1f * u)
    cloud(w * 0.88f, h * 0.08f, 0.9f * u)
    drawLine(Color(0xFFFFD43B), Offset(w * 0.27f, h * 0.06f), Offset(w * 0.3f, h * 0.13f), 4 * u, StrokeCap.Round)
    drawLine(Color(0xFFFFD43B), Offset(w * 0.33f, h * 0.02f), Offset(w * 0.34f, h * 0.1f), 4 * u, StrokeCap.Round)
}
