package com.ngstudio.trafficjam.ui.main

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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.trafficjam.core.data.LEVELS_PER_COLLECTION
import com.ngstudio.trafficjam.core.data.PlayerState
import com.ngstudio.trafficjam.game.render.Themes
import com.ngstudio.trafficjam.ui.components.GameIcon
import com.ngstudio.trafficjam.ui.components.Icon
import com.ngstudio.trafficjam.ui.components.OutlinedText
import com.ngstudio.trafficjam.ui.components.WorldArt
import com.ngstudio.trafficjam.ui.theme.GameText
import com.ngstudio.trafficjam.ui.theme.TitleFont
import com.ngstudio.trafficjam.ui.components.drawLeaf
import com.ngstudio.trafficjam.ui.components.drawLeafSprig
import androidx.compose.ui.graphics.StrokeCap

internal val WoodInk = Color(0xFF4A2A12)

// ---- Header -------------------------------------------------------------------------------

/**
 * Level Map header: a blue sky with clouds and leafy branches in the corners, a wooden plank
 * sign with bolts and leaves, and a cream pill with the player's stars and progress.
 */
@Composable
fun LevelsHeader(stars: Int, cleared: Int, total: Int) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBehind { drawSkyWithLeaves() },
    ) {
        Column(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 8.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Wooden plank sign.
            Box(
                Modifier
                    .widthIn(min = 250.dp)
                    .drawBehind { drawPlank() }
                    .padding(horizontal = 40.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                OutlinedText("Level Map", style = GameText.Display, fontSize = 36.sp, strokeWidth = 10f, outline = WoodInk)
            }
            Spacer(Modifier.height(8.dp))
            // Cream progress pill.
            Row(
                Modifier
                    .drawBehind {
                        val r = CornerRadius(size.height / 2)
                        drawRoundRect(WoodInk.copy(alpha = 0.35f), Offset(0f, 3.dp.toPx()), size, r)
                        drawRoundRect(Color(0xFF8A5A30), Offset.Zero, size, r)
                        drawRoundRect(
                            Brush.verticalGradient(listOf(Color(0xFFFFFCF2), Color(0xFFFFEBC8))),
                            Offset(2.5.dp.toPx(), 2.5.dp.toPx()), Size(size.width - 5.dp.toPx(), size.height - 5.dp.toPx()),
                            CornerRadius(size.height / 2 - 2.5.dp.toPx()),
                        )
                    }
                    .padding(horizontal = 16.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameIcon(Icon.STAR, 24.dp)
                Spacer(Modifier.width(6.dp))
                Text(
                    "$stars Stars  ·  $cleared / $total Cleared",
                    style = GameText.Label.copy(color = WoodInk, fontFamily = TitleFont, fontSize = 16.sp),
                )
            }
        }
    }
}

private fun DrawScope.drawSkyWithLeaves() {
    val w = size.width
    val h = size.height
    val u = w / 360f
    drawRect(Brush.verticalGradient(listOf(Color(0xFF3FB6F5), Color(0xFF8FD8FB), Color(0xFFCDEFFF))))
    fun cloud(cx: Float, cy: Float, s: Float) {
        val c = Color.White
        drawCircle(c, 14 * s, Offset(cx - 14 * s, cy + 4 * s))
        drawCircle(c, 19 * s, Offset(cx + 4 * s, cy - 2 * s))
        drawCircle(c, 13 * s, Offset(cx + 22 * s, cy + 5 * s))
        drawRoundRect(c, Offset(cx - 28 * s, cy + 4 * s), Size(62 * s, 13 * s), CornerRadius(6 * s))
    }
    cloud(w * 0.08f, h * 0.7f, 1f * u)
    cloud(w * 0.92f, h * 0.68f, 1.05f * u)
    cloud(w * 0.82f, h * 0.9f, 0.7f * u)
    // Leafy branches hanging into the top corners: a twig with leaves along it.
    fun branch(start: Offset, flip: Float) {
        val end = start + Offset(flip * 70 * u, 46 * u)
        drawLine(Color(0xFF5D3A24), start, end, 3.5f * u, StrokeCap.Round)
        for (i in 0 until 6) {
            val t = 0.15f + i * 0.16f
            val p = Offset(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t)
            drawLeaf(p, flip * 150f + (if (i % 2 == 0) 35f else -35f), (22 - i) * u, 8 * u)
        }
        drawLeafSprig(end, flip * 125f, 0.8f)
    }
    branch(Offset(-6 * u, -4 * u), 1f)
    branch(Offset(w + 6 * u, -4 * u), -1f)
}

/** Wooden plank sign with bolts and leaf clusters; colors default to natural wood. */
internal fun DrawScope.drawPlank(
    light: Color = Color(0xFFE5A562),
    mid: Color = Color(0xFFC97D3D),
    dark: Color = Color(0xFFB0662C),
    lipColor: Color = Color(0xFF8A4F22),
    leaves: Boolean = true,
) {
    val r = CornerRadius(16.dp.toPx())
    val lip = 6.dp.toPx()
    val faceH = size.height - lip
    // Leaf clusters peeking out from both ends.
    if (leaves) {
        // Leaf sprigs fanning out from behind both ends.
        drawLeafSprig(Offset(10.dp.toPx(), size.height * 0.62f), -72f, 1f)
        drawLeafSprig(Offset(size.width - 10.dp.toPx(), size.height * 0.62f), 72f, 1f)
    }
    drawRoundRect(Color.Black.copy(alpha = 0.22f), Offset(0f, lip + 4.dp.toPx()), Size(size.width, faceH), r)
    val o = 3.dp.toPx()
    drawRoundRect(WoodInk, Offset(-o, -o), Size(size.width + 2 * o, size.height + 2 * o), CornerRadius(r.x + o))
    drawRoundRect(lipColor, Offset(0f, lip), Size(size.width, faceH), r)
    drawRoundRect(Brush.verticalGradient(listOf(light, mid, dark), 0f, faceH), Offset.Zero, Size(size.width, faceH), r)
    // Wood grain.
    for (k in 1..3) {
        val y = faceH * k / 4f
        drawLine(dark.copy(alpha = 0.5f), Offset(22.dp.toPx(), y), Offset(size.width - 22.dp.toPx(), y + (k % 2) * 2.dp.toPx()), 1.5f.dp.toPx())
    }
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.4f), Color.Transparent), 3.dp.toPx(), faceH * 0.45f),
        Offset(14.dp.toPx(), 3.dp.toPx()), Size(size.width - 28.dp.toPx(), faceH * 0.36f), CornerRadius(10.dp.toPx()),
    )
    // Bolts.
    for ((fx, fy) in listOf(0.06f to 0.25f, 0.94f to 0.25f, 0.06f to 0.72f, 0.94f to 0.72f)) {
        val c = Offset(size.width * fx, faceH * fy)
        drawCircle(WoodInk, 5.dp.toPx(), c)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFB9C0CC)), c - Offset(1.5f.dp.toPx(), 1.5f.dp.toPx()), 5.dp.toPx()), 3.6f.dp.toPx(), c)
    }
}

// ---- World banner -------------------------------------------------------------------------

/**
 * The banner at the start of each world: the world's full scene (sized so nothing at the top
 * is cut off) with a purple badge showing the world number, its name and progress.
 */
@Composable
fun WorldBanner(c: Int, player: PlayerState) {
    val theme = Themes.forCollection(c)
    val unlocked = player.isCollectionUnlocked(c)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        // The postcard puts its ground at 64% of its height and needs width / 2.6 above it,
        // so the banner must be at least width / 1.66 tall to show the whole scene.
        val h = maxWidth / 1.6f
        Box(Modifier.fillMaxWidth().height(h)) {
            WorldArt(theme, Modifier.fillMaxSize())
            Column(
                Modifier.align(Alignment.Center).offset(y = (-6).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(contentAlignment = Alignment.TopCenter) {
                    // Purple badge.
                    Column(
                        Modifier
                            .padding(top = 14.dp)
                            .widthIn(min = 230.dp)
                            .drawBehind { drawBadge() }
                            .padding(start = 22.dp, end = 22.dp, top = 18.dp, bottom = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        OutlinedText(Themes.collectionName(c), style = GameText.Title, fontSize = 27.sp, strokeWidth = 8f, outline = Color(0xFF2E1A78))
                        if (unlocked) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${player.completedIn(c)} / $LEVELS_PER_COLLECTION  ·  ",
                                    style = GameText.Label.copy(color = Color.White, fontFamily = TitleFont, fontSize = 15.sp),
                                )
                                GameIcon(Icon.STAR, 18.dp)
                                Text(" ${player.starsIn(c)}", style = GameText.Label.copy(color = Color.White, fontFamily = TitleFont, fontSize = 15.sp))
                            }
                        }
                    }
                    // Gold "WORLD n" tab on top of the badge.
                    Box(
                        Modifier
                            .drawBehind {
                                val r = CornerRadius(size.height / 2)
                                drawRoundRect(Color(0xFF9A5B00), Offset(0f, 3.dp.toPx()), size, r)
                                drawRoundRect(Color(0xFF9A5B00), Offset.Zero, size, r)
                                drawRoundRect(
                                    Brush.verticalGradient(listOf(Color(0xFFFFE680), Color(0xFFFFC21A))),
                                    Offset(2.dp.toPx(), 2.dp.toPx()), Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()), CornerRadius(size.height / 2 - 2.dp.toPx()),
                                )
                            }
                            .padding(horizontal = 18.dp, vertical = 3.dp),
                    ) {
                        Text("WORLD $c", style = GameText.Label.copy(color = Color(0xFF6B3A00), fontFamily = TitleFont, fontSize = 15.sp))
                    }
                }
                if (!unlocked) {
                    Row(
                        Modifier
                            .padding(top = 6.dp)
                            .drawBehind { drawRoundRect(Color(0xCC1B1035), Offset.Zero, size, CornerRadius(size.height / 2)) }
                            .padding(horizontal = 12.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        GameIcon(Icon.LOCK, 18.dp)
                        Text(" Locked", style = GameText.Small.copy(color = Color.White, fontFamily = TitleFont, fontSize = 13.sp))
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawBadge() {
    val r = CornerRadius(26.dp.toPx())
    val lip = 5.dp.toPx()
    val faceH = size.height - lip
    val ink = Color(0xFF2E1A78)
    drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(0f, lip + 4.dp.toPx()), Size(size.width, faceH), r)
    drawRoundRect(ink, Offset(-2.dp.toPx(), -2.dp.toPx()), Size(size.width + 4.dp.toPx(), size.height + 4.dp.toPx()), CornerRadius(r.x + 2.dp.toPx()))
    drawRoundRect(Color(0xFF3F1F9E), Offset(0f, lip), Size(size.width, faceH), r)
    drawRoundRect(Color.White, Offset.Zero, Size(size.width, faceH), r)
    drawRoundRect(
        Brush.verticalGradient(listOf(Color(0xFF8E62F5), Color(0xFF6A3FD9), Color(0xFF5530C2)), 0f, faceH),
        Offset(3.5f.dp.toPx(), 3.5f.dp.toPx()), Size(size.width - 7.dp.toPx(), faceH - 7.dp.toPx()), CornerRadius(r.x - 3.5f.dp.toPx()),
    )
    drawRoundRect(
        Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.3f), Color.Transparent), 4.dp.toPx(), faceH * 0.45f),
        Offset(18.dp.toPx(), 5.dp.toPx()), Size(size.width - 36.dp.toPx(), faceH * 0.3f), CornerRadius(10.dp.toPx()),
    )
}
