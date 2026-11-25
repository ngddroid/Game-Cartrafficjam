package com.puzzel.lab.cartrafficjam.ui.main

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.cartrafficjam.App
import com.puzzel.lab.cartrafficjam.core.audio.Music
import com.puzzel.lab.cartrafficjam.core.audio.Sfx
import com.puzzel.lab.cartrafficjam.game.model.GameMode
import com.puzzel.lab.cartrafficjam.ui.components.ConfirmDialog
import com.puzzel.lab.cartrafficjam.ui.components.GameIcon
import com.puzzel.lab.cartrafficjam.ui.components.Icon
import com.puzzel.lab.cartrafficjam.ui.components.LocalAudio
import com.puzzel.lab.cartrafficjam.ui.components.OutlinedText
import com.puzzel.lab.cartrafficjam.ui.components.SkyBackground
import com.puzzel.lab.cartrafficjam.ui.components.WatchAdDialog
import com.puzzel.lab.cartrafficjam.ui.theme.GameText

enum class Tab(val label: String, val icon: Icon) {
    LEVELS("Levels", Icon.MAP),
    MODES("Modes", Icon.MODES),
    HOME("Home", Icon.HOME),
    GARAGE("Garage", Icon.GARAGE),
    SETTINGS("Settings", Icon.GEAR),
}

@Composable
fun MainScreen(onPlay: (Int) -> Unit, onPlayMode: (GameMode, Int) -> Unit, onOpenDoc: (String) -> Unit) {
    val container = App.container
    val audio = LocalAudio.current
    val progress = container.progress
    val player by progress.state.collectAsState()
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var scrollToCollection by remember { mutableIntStateOf(0) }
    var lockedLevel by remember { mutableStateOf<Int?>(null) }
    var confirmExit by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    LaunchedEffect(Unit) { audio.playMusic(Music.Home) }

    BackHandler {
        if (tab != Tab.HOME) tab = Tab.HOME else confirmExit = true
    }

    SkyBackground {
        Column(Modifier.fillMaxSize()) {
            AnimatedContent(
                tab,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "tab",
            ) { t ->
                when (t) {
                    Tab.HOME -> HomeTab(
                        player = player,
                        onPlay = { onPlay(player.nextLevel) },
                        onSettings = { tab = Tab.SETTINGS },
                        onWorld = { c -> scrollToCollection = c; tab = Tab.LEVELS },
                    )
                    Tab.LEVELS -> LevelsTab(
                        player = player,
                        scrollToCollection = scrollToCollection,
                        onScrolled = { scrollToCollection = 0 },
                        onLevel = { n ->
                            if (player.isLevelUnlocked(n)) onPlay(n) else lockedLevel = n
                        },
                    )
                    Tab.MODES -> ModesTab(
                        player = player,
                        onPlay = { m ->
                            if (m == GameMode.CLASSIC) onPlay(player.nextLevel) else onPlayMode(m, player.modeLevel(m))
                        },
                    )
                    Tab.GARAGE -> GarageTab(player)
                    Tab.SETTINGS -> SettingsTab(player, onOpenDoc)
                }
            }
            BottomBar(tab) {
                if (it != tab) audio.play(Sfx.Click)
                tab = it
            }
        }
    }

    WatchAdDialog(
        visible = lockedLevel != null,
        title = "Locked Level",
        message = "Level ${lockedLevel ?: 0} is locked.\nWatch a short ad to unlock it now!",
        onWatch = {
            val n = lockedLevel
            container.ads.showRewarded("unlock", onReward = {
                if (n != null) progress.unlockLevel(n)
                lockedLevel = null
            })
        },
        onDismiss = { lockedLevel = null },
    )

    ConfirmDialog(
        visible = confirmExit,
        title = "Leaving?",
        message = "The traffic will wait for you. Come back soon!",
        confirm = "Quit",
        cancel = "Stay",
        onConfirm = { activity?.finish() },
        onDismiss = { confirmExit = false },
    )

}

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    val barShape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    Box(
        Modifier
            .fillMaxWidth()
            .shadow(20.dp, barShape, clip = false, ambientColor = Color(0xFF05122E), spotColor = Color(0xFF05122E))
            .clip(barShape)
            .drawBehind {
                // Deep navy dock — contrasts against the light sky background.
                drawRect(Brush.verticalGradient(listOf(Color(0xFF16356E), Color(0xFF0A2048))), Offset(0f, 3.dp.toPx()), size)
                // Bright top rim so the dock reads as a raised surface.
                drawRect(Color(0xFF6FA8FF), Offset(0f, 0f), Size(size.width, 3.dp.toPx()))
                drawRect(Color(0x33FFFFFF), Offset(0f, 3.dp.toPx()), Size(size.width, 1.dp.toPx()))
            }
            .navigationBarsPadding()
            .height(76.dp),
    ) {
        Row(Modifier.fillMaxSize()) {
            Tab.entries.forEachIndexed { index, t ->
                val sel = t == selected
                val weight by animateFloatAsState(if (sel) 1.45f else 1f, spring(dampingRatio = 0.7f), label = "w")
                val lift by animateDpAsState(if (sel) (-16).dp else 0.dp, spring(dampingRatio = 0.5f, stiffness = 500f), label = "lift")
                val scale by animateFloatAsState(if (sel) 1.25f else 1f, spring(dampingRatio = 0.5f), label = "s")
                val glow by animateFloatAsState(if (sel) 1f else 0f, spring(dampingRatio = 0.6f), label = "g")
                Box(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, null) { onSelect(t) }
                        .drawBehind {
                            if (glow > 0f) {
                                val inset = 5.dp.toPx()
                                // Soft glow halo behind the active pill.
                                drawRoundRect(
                                    Color(0xFF4F9BFF).copy(alpha = 0.35f * glow),
                                    Offset(inset - 3.dp.toPx(), 5.dp.toPx()),
                                    Size(size.width - 2 * inset + 6.dp.toPx(), size.height - 10.dp.toPx()),
                                    CornerRadius(20.dp.toPx(), 20.dp.toPx()),
                                )
                                // Bright raised pill that pops against the dark dock.
                                drawRoundRect(
                                    Brush.verticalGradient(listOf(Color(0xFF8FC2FF), Color(0xFF3B82F6))),
                                    Offset(inset, 7.dp.toPx()),
                                    Size(size.width - 2 * inset, size.height - 14.dp.toPx()),
                                    CornerRadius(16.dp.toPx(), 16.dp.toPx()),
                                )
                            }
                            if (index < Tab.entries.lastIndex) {
                                drawRect(Color(0x1AFFFFFF), Offset(size.width - 1.dp.toPx(), 16.dp.toPx()), Size(1.dp.toPx(), size.height - 32.dp.toPx()))
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.offset(y = lift).graphicsLayer { scaleX = scale; scaleY = scale }) {
                        GameIcon(t.icon, 42.dp)
                    }
                    if (sel) {
                        OutlinedText(
                            t.label, Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp),
                            style = GameText.Button, fontSize = 15.sp, strokeWidth = 5f, outline = Color(0xFF0A2048),
                        )
                    }
                }
            }
        }
    }
}
