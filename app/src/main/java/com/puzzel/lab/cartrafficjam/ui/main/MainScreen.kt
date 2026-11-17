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
import androidx.compose.ui.draw.drawBehind
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
import com.puzzel.lab.cartrafficjam.game.render.Themes
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
    COLLECTIONS("Worlds", Icon.COLLECTION),
    HOME("Home", Icon.HOME),
    GARAGE("Garage", Icon.GARAGE),
    SETTINGS("Settings", Icon.GEAR),
}

private sealed interface LockRequest {
    data class Level(val n: Int) : LockRequest
    data class Collection(val c: Int) : LockRequest
}

@Composable
fun MainScreen(onPlay: (Int) -> Unit, onOpenDoc: (String) -> Unit) {
    val container = App.container
    val audio = LocalAudio.current
    val progress = container.progress
    val player by progress.state.collectAsState()
    var tab by rememberSaveable { mutableStateOf(Tab.HOME) }
    var scrollToCollection by remember { mutableIntStateOf(0) }
    var lock by remember { mutableStateOf<LockRequest?>(null) }
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
                            if (player.isLevelUnlocked(n)) onPlay(n) else lock = LockRequest.Level(n)
                        },
                    )
                    Tab.COLLECTIONS -> CollectionsTab(
                        player = player,
                        onOpen = { c ->
                            if (player.isCollectionUnlocked(c)) {
                                scrollToCollection = c
                                tab = Tab.LEVELS
                            } else {
                                lock = LockRequest.Collection(c)
                            }
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

    val req = lock
    WatchAdDialog(
        visible = req != null,
        title = if (req is LockRequest.Collection) "Locked World" else "Locked Level",
        message = when (req) {
            is LockRequest.Level -> "Level ${req.n} is locked.\nWatch a short ad to unlock it now!"
            is LockRequest.Collection -> "${Themes.collectionName(req.c)} is locked.\nWatch a short ad to open this world!"
            null -> ""
        },
        onWatch = {
            val r = lock
            container.ads.showRewarded("unlock", onReward = {
                when (r) {
                    is LockRequest.Level -> progress.unlockLevel(r.n)
                    is LockRequest.Collection -> {
                        progress.unlockCollection(r.c)
                        scrollToCollection = r.c
                    }
                    null -> Unit
                }
                lock = null
            })
        },
        onDismiss = { lock = null },
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
    Box(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                drawRect(Color(0xFF0E3A8C), Offset(0f, 0f), size)
                drawRect(Brush.verticalGradient(listOf(Color(0xFF3D8BFF), Color(0xFF1C5BD0))), Offset(0f, 4.dp.toPx()), Size(size.width, size.height - 4.dp.toPx()))
                drawRect(Color(0xFFFFF3D6), Offset(0f, 0f), Size(size.width, 4.dp.toPx()))
            }
            .navigationBarsPadding()
            .height(74.dp),
    ) {
        Row(Modifier.fillMaxSize()) {
            Tab.entries.forEach { t ->
                val sel = t == selected
                val weight by animateFloatAsState(if (sel) 1.45f else 1f, spring(dampingRatio = 0.7f), label = "w")
                val lift by animateDpAsState(if (sel) (-16).dp else 0.dp, spring(dampingRatio = 0.5f, stiffness = 500f), label = "lift")
                val scale by animateFloatAsState(if (sel) 1.25f else 1f, spring(dampingRatio = 0.5f), label = "s")
                Box(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, null) { onSelect(t) }
                        .drawBehind {
                            if (sel) {
                                drawRoundRect(
                                    Brush.verticalGradient(listOf(Color(0xFF7FB8FF), Color(0xFF3C82F0))),
                                    Offset(2.dp.toPx(), 4.dp.toPx()), Size(size.width - 4.dp.toPx(), size.height - 4.dp.toPx()),
                                    CornerRadius(14.dp.toPx(), 14.dp.toPx()),
                                )
                            }
                            drawRect(Color(0x33000000), Offset(size.width - 1.dp.toPx(), 10.dp.toPx()), Size(1.dp.toPx(), size.height - 20.dp.toPx()))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(Modifier.offset(y = lift).graphicsLayer { scaleX = scale; scaleY = scale }) {
                        GameIcon(t.icon, 42.dp)
                    }
                    if (sel) {
                        OutlinedText(
                            t.label, Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp),
                            style = GameText.Button, fontSize = 15.sp, strokeWidth = 5f, outline = Color(0xFF0E3A8C),
                        )
                    }
                }
            }
        }
    }
}
