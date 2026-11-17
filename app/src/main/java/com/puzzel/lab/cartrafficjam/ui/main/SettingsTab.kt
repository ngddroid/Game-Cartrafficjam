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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
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

@Composable
fun SettingsTab(player: PlayerState, onOpenDoc: (String) -> Unit) {
    val progress = App.container.progress
    val audio = LocalAudio.current
    val context = LocalContext.current
    var confirmReset by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TabHeader("Settings")
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            Card {
                ToggleItem(Icons.AutoMirrored.Filled.VolumeUp, Color(0xFF2F8CF6), "Sound Effects", player.sound, progress::setSound)
                Divider()
                ToggleItem(Icons.Filled.MusicNote, Color(0xFF8E5CF0), "Music", player.music, progress::setMusic)
                Divider()
                ToggleItem(Icons.Filled.Vibration, Color(0xFFFF8A1F), "Vibration", player.vibration) {
                    progress.setVibration(it)
                    if (it) audio.haptic(com.puzzel.lab.cartrafficjam.core.audio.Haptic.Bump)
                }
            }
            Spacer(Modifier.height(16.dp))
            Card {
                MenuItem(Icons.Filled.Star, Color(0xFFFFB300), "Rate Us") {
                    val pkg = context.packageName
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$pkg".toUri()))
                    } catch (_: ActivityNotFoundException) {
                        context.startActivity(Intent(Intent.ACTION_VIEW, "https://play.google.com/store/apps/details?id=$pkg".toUri()))
                    }
                }
                Divider()
                MenuItem(Icons.Filled.Share, Color(0xFF5FC12F), "Share with Friends") {
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, "Can you clear the jam? Play Traffic Jam Rush: https://play.google.com/store/apps/details?id=${context.packageName}")
                    }
                    context.startActivity(Intent.createChooser(send, "Share"))
                }
                Divider()
                MenuItem(Icons.Filled.Shield, Color(0xFF26A69A), "Privacy Policy") { onOpenDoc("privacy") }
                Divider()
                MenuItem(Icons.AutoMirrored.Filled.Article, Color(0xFF5C6BC0), "Terms of Service") { onOpenDoc("terms") }
            }
            Spacer(Modifier.height(16.dp))
            Card {
                MenuItem(Icons.Filled.RestartAlt, Color(0xFFF2453D), "Reset Progress") { confirmReset = true }
            }
            Spacer(Modifier.height(22.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Mascot(64.dp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text("Traffic Jam Rush", style = GameText.Label.copy(color = Color.White, fontFamily = TitleFont, fontSize = 18.sp))
                    Text("Version ${BuildConfig.VERSION_NAME}", style = GameText.Small.copy(color = Color.White.copy(alpha = 0.85f)))
                }
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

@Composable
private fun Card(content: @Composable () -> Unit) {
    Column(
        Modifier
            .fillMaxWidth()
            .shadow(8.dp, RoundedCornerShape(24.dp))
            .drawBehind {
                drawRoundRect(Color(0xFF0D2C6B), Offset(0f, 5.dp.toPx()), size, CornerRadius(24.dp.toPx()))
                drawRoundRect(Color.White, Offset.Zero, Size(size.width, size.height - 2.dp.toPx()), CornerRadius(24.dp.toPx()))
            }
            .padding(4.dp)
            .background(Brush.verticalGradient(listOf(Color(0xFFFFFDF5), Color(0xFFFFEFCB))), RoundedCornerShape(20.dp))
            .padding(horizontal = 14.dp, vertical = 4.dp),
    ) { content() }
}

@Composable
private fun Divider() {
    Box(Modifier.fillMaxWidth().height(1.5.dp).background(Color(0xFFEAD7B0)))
}

@Composable
private fun Badge(icon: ImageVector, color: Color) {
    Box(
        Modifier
            .size(42.dp)
            .drawBehind {
                drawCircle(color.shade(0.65f), size.minDimension / 2, center + Offset(0f, 3.dp.toPx()))
                drawCircle(Brush.verticalGradient(listOf(color.tint(0.3f), color)), size.minDimension / 2)
            },
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
}

@Composable
private fun ToggleItem(icon: ImageVector, color: Color, label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Badge(icon, color)
        Spacer(Modifier.width(14.dp))
        Text(label, style = GameText.Label.copy(fontSize = 18.sp), modifier = Modifier.weight(1f))
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
            .fillMaxWidth()
            .scale(if (pressed) 0.97f else 1f)
            .clickable(interaction, null) { audio.play(Sfx.Click); onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Badge(icon, color)
        Spacer(Modifier.width(14.dp))
        Text(label, style = GameText.Label.copy(fontSize = 18.sp), modifier = Modifier.weight(1f))
        Box(
            Modifier.size(30.dp).background(Color(0xFFFFE0A3), CircleShape),
            contentAlignment = Alignment.Center,
        ) { Text("›", style = GameText.Label.copy(fontSize = 22.sp, color = Color(0xFF8A4B14))) }
    }
}
