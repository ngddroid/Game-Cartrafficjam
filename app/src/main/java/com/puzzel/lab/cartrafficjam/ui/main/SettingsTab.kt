package com.puzzel.lab.cartrafficjam.ui.main

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.puzzel.lab.cartrafficjam.App
import com.puzzel.lab.cartrafficjam.BuildConfig
import com.puzzel.lab.cartrafficjam.core.audio.Sfx
import com.puzzel.lab.cartrafficjam.core.data.PlayerState
import com.puzzel.lab.cartrafficjam.game.render.shade
import com.puzzel.lab.cartrafficjam.game.render.tint
import com.puzzel.lab.cartrafficjam.ui.components.ConfirmDialog
import com.puzzel.lab.cartrafficjam.ui.components.GameToggle
import com.puzzel.lab.cartrafficjam.ui.components.LocalAudio
import com.puzzel.lab.cartrafficjam.ui.components.Mascot
import com.puzzel.lab.cartrafficjam.ui.theme.GameText
import com.puzzel.lab.cartrafficjam.ui.theme.TitleFont
import com.puzzel.lab.cartrafficjam.ui.theme.LocalTabPalette
import com.puzzel.lab.cartrafficjam.ui.components.toyCard
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import com.puzzel.lab.cartrafficjam.ui.components.OutlinedText
import com.puzzel.lab.cartrafficjam.ui.components.drawToyBoard
import com.puzzel.lab.cartrafficjam.ui.components.GameIcon
import androidx.compose.foundation.layout.Arrangement
import com.puzzel.lab.cartrafficjam.ui.components.drawLeaf
import com.puzzel.lab.cartrafficjam.ui.components.drawLeafSprig

@Composable
fun SettingsTab(player: PlayerState, onOpenDoc: (String) -> Unit) {
    val progress = App.container.progress
    val audio = LocalAudio.current
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
        // Leaves peeking in from the bottom corners.
        Canvas(Modifier.fillMaxSize()) {
            cornerLeaves(Offset(-10.dp.toPx(), size.height - 10.dp.toPx()), 1f)
            cornerLeaves(Offset(size.width + 10.dp.toPx(), size.height - 10.dp.toPx()), -1f)
        }
        Column(Modifier.fillMaxSize()) {
            SettingsHeader()
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 12.dp),
            ) {
                SectionCard("Audio Settings", Icons.AutoMirrored.Filled.VolumeUp, gold = true) {
                    ToggleItem(Icons.AutoMirrored.Filled.VolumeUp, Color(0xFF2F8CF6), "Sound Effects", player.sound, progress::setSound)
                    ToggleItem(Icons.Filled.MusicNote, Color(0xFFEC407A), "Music", player.music, progress::setMusic)
                    ToggleItem(Icons.Filled.Vibration, Color(0xFFFF8A1F), "Vibration", player.vibration) {
                        progress.setVibration(it)
                        if (it) audio.haptic(com.puzzel.lab.cartrafficjam.core.audio.Haptic.Bump)
                    }
                }
                Spacer(Modifier.height(18.dp))
                SectionCard("General", Icons.Filled.Settings, gold = false) {
                    MenuItem(Icons.Filled.Star, Color(0xFFFFB300), "Rate Us") {
                        val pkg = context.packageName
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri()))
                        } catch (_: ActivityNotFoundException) {
                            context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$pkg".toUri()))
                        }
                    }
                    MenuItem(Icons.Filled.Share, Color(0xFF1FAE7A), "Share with Friends") {
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "Can you clear the jam? Play Traffic Jam Rush: https://play.google.com/store/apps/details?id=${context.packageName}")
                        }
                        context.startActivity(Intent.createChooser(send, "Share"))
                    }
                    MenuItem(Icons.Filled.Shield, Color(0xFF2F8CF6), "Privacy Policy") { onOpenDoc("privacy") }
                    MenuItem(Icons.AutoMirrored.Filled.Article, Color(0xFF5C5BD6), "Terms of Service") { onOpenDoc("terms") }
                }
                Spacer(Modifier.height(16.dp))
                ResetBar { confirmReset = true }
            }
        }
    }

    ConfirmDialog(
        visible = confirmReset,
        title = "Reset?",
        message = "This erases your levels, stars, coins and garage. It can't be undone.",
        confirm = "Reset",
        onConfirm = {
            progress.resetProgress()
            audio.play(Sfx.Whoosh)
            confirmReset = false
        },
        onDismiss = { confirmReset = false },
    )
}

private val SettingsInk = Color(0xFF2B1C6E)

/** Purple glossy "Settings" banner with a gold gear on a pedestal, leaves and a skyline behind. */
@Composable
private fun SettingsHeader() {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBehind { drawCitySkyline() },
    ) {
        Box(
            Modifier.fillMaxWidth().statusBarsPadding().padding(top = 6.dp, bottom = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(contentAlignment = Alignment.CenterStart) {
                // Banner
                Box(
                    Modifier
                        .padding(start = 40.dp)
                        .drawBehind {
                            // Leaf sprigs behind both ends.
                            drawLeafSprig(Offset(4.dp.toPx(), size.height * 0.62f), -70f, 1f)
                            drawLeafSprig(Offset(size.width - 4.dp.toPx(), size.height * 0.62f), 70f, 1f)
                            drawToyBoard(Color(0xFFA486FF), Color(0xFF6A4FE0), SettingsInk, 20.dp.toPx())
                        }
                        .padding(start = 44.dp, end = 26.dp, top = 6.dp, bottom = 11.dp),
                ) {
                    OutlinedText("Settings", style = GameText.Display, fontSize = 36.sp, strokeWidth = 10f, outline = SettingsInk)
                }
                // Gold gear on a purple pedestal.
                Box(Modifier.size(78.dp), contentAlignment = Alignment.Center) {
                    Canvas(Modifier.fillMaxSize()) {
                        val c = Offset(size.width / 2, size.height * 0.82f)
                        drawOval(SettingsInk, Offset(c.x - 30.dp.toPx(), c.y - 8.dp.toPx()), Size(60.dp.toPx(), 18.dp.toPx()))
                        drawOval(Brush.verticalGradient(listOf(Color(0xFFA486FF), Color(0xFF5A3FC9)), c.y - 10.dp.toPx(), c.y + 6.dp.toPx()), Offset(c.x - 28.dp.toPx(), c.y - 10.dp.toPx()), Size(56.dp.toPx(), 14.dp.toPx()))
                    }
                    GameIcon(com.puzzel.lab.cartrafficjam.ui.components.Icon.GEAR, 58.dp, Modifier.offset(y = (-8).dp))
                }
            }
        }
    }
}

/** A lavender glass card with a tab label on its top-left edge (gold for audio, purple otherwise). */
@Composable
private fun SectionCard(title: String, icon: ImageVector, gold: Boolean, content: @Composable () -> Unit) {
    Box {
        Column(
            Modifier
                .padding(top = 18.dp)
                .fillMaxWidth()
                .drawBehind {
                    val r = CornerRadius(26.dp.toPx())
                    // Soft glow, white rim, then the lavender glass face.
                    drawRoundRect(Color(0xFFB9A4FF).copy(alpha = 0.35f), Offset(-4.dp.toPx(), -4.dp.toPx()), Size(size.width + 8.dp.toPx(), size.height + 8.dp.toPx()), CornerRadius(r.x + 4.dp.toPx()))
                    drawRoundRect(Color(0xFF5A40C8), Offset(0f, 4.dp.toPx()), size, r)
                    drawRoundRect(Color.White, Offset.Zero, Size(size.width, size.height - 2.dp.toPx()), r)
                    drawRoundRect(
                        Brush.linearGradient(listOf(Color(0xFFF7F2FF), Color(0xFFE9DEFF), Color(0xFFF3E9FF)), Offset.Zero, Offset(size.width, size.height)),
                        Offset(3.dp.toPx(), 3.dp.toPx()), Size(size.width - 6.dp.toPx(), size.height - 8.dp.toPx()), CornerRadius(r.x - 3.dp.toPx()),
                    )
                    drawRoundRect(
                        Brush.horizontalGradient(listOf(Color.Transparent, Color(0xFF7FE3FF).copy(alpha = 0.5f), Color.Transparent)),
                        Offset(24.dp.toPx(), size.height - 6.dp.toPx()), Size(size.width - 48.dp.toPx(), 2.dp.toPx()),
                    )
                }
                .padding(start = 10.dp, end = 10.dp, top = 22.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) { content() }
        // Tab label
        Row(
            Modifier
                .padding(start = 16.dp)
                .drawBehind {
                    val notch = 10.dp.toPx()
                    val top = if (gold) Color(0xFFFFD84D) else Color(0xFF9B7BFF)
                    val bottom = if (gold) Color(0xFFFFA41C) else Color(0xFF6A3FD9)
                    val ink = if (gold) Color(0xFF9A5B00) else SettingsInk
                    val p = androidx.compose.ui.graphics.Path().apply {
                        moveTo(10.dp.toPx(), 0f); lineTo(size.width, 0f); lineTo(size.width - notch, size.height)
                        lineTo(10.dp.toPx(), size.height); quadraticTo(0f, size.height, 0f, size.height - 10.dp.toPx())
                        lineTo(0f, 10.dp.toPx()); quadraticTo(0f, 0f, 10.dp.toPx(), 0f); close()
                    }
                    drawPath(p, ink, style = Stroke(5.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
                    drawPath(p, Brush.verticalGradient(listOf(top, bottom)))
                    drawRect(Color.White.copy(alpha = 0.3f), Offset(10.dp.toPx(), 3.dp.toPx()), Size(size.width - notch - 18.dp.toPx(), 4.dp.toPx()))
                }
                .padding(start = 12.dp, end = 24.dp, top = 5.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, tint = if (gold) Color(0xFF4A2A00) else Color.White, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(8.dp))
            if (gold) {
                Text(title, style = GameText.Label.copy(color = Color(0xFF4A2A00), fontFamily = TitleFont, fontSize = 19.sp))
            } else {
                OutlinedText(title, style = GameText.Button, fontSize = 19.sp, strokeWidth = 6f, outline = SettingsInk, depth = 0.06f)
            }
        }
    }
}

/** Glossy rounded-square icon tile with a darker lip. */
@Composable
private fun IconTile(icon: ImageVector, color: Color) {
    Box(
        Modifier
            .size(42.dp)
            .drawBehind {
                val r = CornerRadius(13.dp.toPx())
                val lip = 4.dp.toPx()
                val face = Size(size.width, size.height - lip)
                drawRoundRect(color.shade(0.55f), Offset(0f, lip), face, r)
                drawRoundRect(color.shade(0.55f), Offset.Zero, face, r)
                drawRoundRect(Brush.verticalGradient(listOf(color.tint(0.35f), color, color.shade(0.85f))), Offset(2.dp.toPx(), 2.dp.toPx()), Size(face.width - 4.dp.toPx(), face.height - 4.dp.toPx()), CornerRadius(r.x - 2.dp.toPx()))
                drawRoundRect(Color.White.copy(alpha = 0.4f), Offset(6.dp.toPx(), 4.dp.toPx()), Size(face.width - 12.dp.toPx(), face.height * 0.28f), CornerRadius(6.dp.toPx()))
            },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(23.dp).offset(y = (-2).dp)) }
}

/** The soft white pill every row sits on inside a section card. */
private fun Modifier.rowPill(): Modifier = this
    .fillMaxWidth()
    .drawBehind {
        val r = CornerRadius(20.dp.toPx())
        drawRoundRect(Color(0xFFCDBDF5).copy(alpha = 0.6f), Offset(0f, 2.dp.toPx()), size, r)
        drawRoundRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.95f), Color(0xFFF1EBFF))), Offset.Zero, size, r)
    }
    .padding(horizontal = 10.dp, vertical = 5.dp)

@Composable
private fun ToggleItem(icon: ImageVector, color: Color, label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.rowPill(), verticalAlignment = Alignment.CenterVertically) {
        IconTile(icon, color)
        Spacer(Modifier.width(12.dp))
        Text(label, style = GameText.Label.copy(color = SettingsInk, fontFamily = TitleFont, fontSize = 18.sp))
        Spacer(Modifier.weight(1f))
        GameToggle(value, onChange)
    }
}

@Composable
private fun MenuItem(icon: ImageVector, color: Color, label: String, onClick: () -> Unit) {
    val audio = LocalAudio.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .scale(if (pressed) 0.97f else 1f)
            .rowPill()
            .clickable(interaction, null) { audio.play(Sfx.Click); onClick() },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(icon, color)
        Spacer(Modifier.width(12.dp))
        Text(label, style = GameText.Label.copy(color = SettingsInk, fontFamily = TitleFont, fontSize = 18.sp), modifier = Modifier.weight(1f))
        Chevron(Color(0xFF6A3FD9))
    }
}

@Composable
private fun Chevron(color: Color) {
    Canvas(Modifier.size(22.dp)) {
        val p = androidx.compose.ui.graphics.Path().apply {
            moveTo(size.width * 0.35f, size.height * 0.18f)
            lineTo(size.width * 0.7f, size.height * 0.5f)
            lineTo(size.width * 0.35f, size.height * 0.82f)
        }
        drawPath(p, color, style = Stroke(4.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

/** Full-width glossy pink-red Reset Progress bar. */
@Composable
private fun ResetBar(onClick: () -> Unit) {
    val audio = LocalAudio.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        Modifier
            .fillMaxWidth()
            .scale(if (pressed) 0.97f else 1f)
            .drawBehind {
                val r = CornerRadius(size.height / 2)
                val lip = 5.dp.toPx()
                val face = Size(size.width, size.height - lip)
                drawRoundRect(Color(0xFFFF6FA0).copy(alpha = 0.35f), Offset(-4.dp.toPx(), -4.dp.toPx()), Size(size.width + 8.dp.toPx(), size.height + 6.dp.toPx()), CornerRadius(r.x + 4.dp.toPx()))
                drawRoundRect(Color(0xFFA0173F), Offset(0f, lip), face, r)
                drawRoundRect(Color(0xFFFFC2D4), Offset.Zero, face, r)
                drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFFF8FB2), Color(0xFFF2456E), Color(0xFFE02E5B))), Offset(2.5.dp.toPx(), 2.5.dp.toPx()), Size(face.width - 5.dp.toPx(), face.height - 5.dp.toPx()), CornerRadius(r.x - 2.5.dp.toPx()))
                drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(60.dp.toPx(), 5.dp.toPx()), Size(face.width - 90.dp.toPx(), face.height * 0.25f), CornerRadius(8.dp.toPx()))
            }
            .clickable(interaction, null) { audio.play(Sfx.Click); onClick() }
            .padding(start = 10.dp, end = 16.dp, top = 8.dp, bottom = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconTile(Icons.Filled.RestartAlt, Color(0xFFE53935))
        Spacer(Modifier.width(12.dp))
        OutlinedText("Reset Progress", Modifier.weight(1f), style = GameText.Title, fontSize = 22.sp, strokeWidth = 7f, outline = Color(0xFF9E1640), depth = 0.07f, textAlign = TextAlign.Start)
        Chevron(Color.White)
    }
}

/** A clump of leaves growing in from a bottom corner. */
private fun DrawScope.cornerLeaves(at: Offset, side: Float) {
    val u = 1.dp.toPx()
    for ((i, a) in listOf(-62f, -38f, -14f, 10f).withIndex()) {
        drawLeaf(at, side * -a, (52 + i * 6) * u, 15 * u)
    }
}

/**
 * City skyline behind the Settings banner: a pale far layer and a darker near layer of purple
 * towers on both sides, with rooftop details and a few lit windows, over a soft horizon glow.
 */
private fun DrawScope.drawCitySkyline() {
    val w = size.width
    val h = size.height
    val u = w / 360f
    val ground = h
    drawRect(Brush.verticalGradient(listOf(Color.Transparent, Color(0xFF6E58E0).copy(alpha = 0.35f)), h * 0.4f, h), Offset(0f, h * 0.4f), Size(w, h * 0.6f))
    // (x, width, height) in units of the 360-wide layout; far layer first, then near.
    val far = listOf(
        Triple(0f, 26f, 70f), Triple(24f, 20f, 92f), Triple(46f, 30f, 60f), Triple(78f, 18f, 80f),
        Triple(268f, 22f, 84f), Triple(292f, 30f, 66f), Triple(322f, 18f, 96f), Triple(340f, 26f, 74f),
    )
    val near = listOf(
        Triple(-6f, 34f, 52f), Triple(30f, 24f, 74f), Triple(56f, 34f, 44f), Triple(92f, 22f, 58f),
        Triple(250f, 24f, 56f), Triple(276f, 34f, 46f), Triple(312f, 24f, 70f), Triple(338f, 30f, 50f),
    )
    fun tower(x: Float, bw: Float, bh: Float, body: Color, lit: Color, dim: Color, seed: Int) {
        val left = x * u
        val top = ground - bh * u
        drawRect(body, Offset(left, top), Size(bw * u, bh * u))
        // Rooftop: a small block or antenna.
        if (seed % 2 == 0) drawRect(body, Offset(left + bw * u * 0.3f, top - 6 * u), Size(bw * u * 0.4f, 6 * u))
        else drawLine(body, Offset(left + bw * u / 2, top), Offset(left + bw * u / 2, top - 10 * u), 2 * u)
        val cols = ((bw - 6) / 7).toInt().coerceAtLeast(1)
        val rows = ((bh - 8) / 9).toInt()
        for (r in 0 until rows) for (c in 0 until cols) {
            val k = (r * 5 + c * 3 + seed) % 7
            val col = when (k) { 0 -> lit; 1, 2, 3 -> dim; else -> null } ?: continue
            drawRect(col, Offset(left + 4 * u + c * 7 * u, top + 5 * u + r * 9 * u), Size(3.5f * u, 4.5f * u))
        }
    }
    far.forEachIndexed { i, (x, bw, bh) -> tower(x, bw, bh, Color(0xFF4B3DAE), Color(0xFFB9A8FF).copy(alpha = 0.7f), Color(0xFF6A5AD0), i) }
    near.forEachIndexed { i, (x, bw, bh) -> tower(x, bw, bh, Color(0xFF34297F), Color(0xFFFFD86B).copy(alpha = 0.85f), Color(0xFF5546B8), i + 3) }
}
