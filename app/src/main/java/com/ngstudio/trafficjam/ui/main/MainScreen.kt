package com.ngstudio.trafficjam.ui.main

import com.ngstudio.trafficjam.core.ads.AdConfig
import com.ngstudio.trafficjam.core.analytics.Analytics
import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.ngstudio.trafficjam.App
import com.ngstudio.trafficjam.core.audio.Music
import com.ngstudio.trafficjam.core.audio.Sfx
import com.ngstudio.trafficjam.game.model.GameMode
import com.ngstudio.trafficjam.ui.components.ConfirmDialog
import com.ngstudio.trafficjam.ui.components.GameIcon
import com.ngstudio.trafficjam.ui.components.Icon
import com.ngstudio.trafficjam.ui.components.LocalAudio
import com.ngstudio.trafficjam.ui.components.OutlinedText
import com.ngstudio.trafficjam.ui.components.TownBackground
import com.ngstudio.trafficjam.ui.components.WatchAdDialog
import com.ngstudio.trafficjam.ui.theme.GameText
import com.ngstudio.trafficjam.ui.theme.LocalTabPalette
import com.ngstudio.trafficjam.ui.theme.Sunset
import com.ngstudio.trafficjam.ui.theme.TabPalette
import com.ngstudio.trafficjam.ui.theme.TabPalettes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

enum class Tab(val label: String, val icon: Icon, val palette: TabPalette) {
    LEVELS("Levels", Icon.MAP, TabPalettes.Levels),
    MODES("Modes", Icon.GAMEPAD, TabPalettes.Modes),
    HOME("Home", Icon.HOME, TabPalettes.Home),
    GARAGE("Garage", Icon.GARAGE, TabPalettes.Garage),
    SETTINGS("Settings", Icon.GEAR, TabPalettes.Settings),
}

@Composable
fun MainScreen(
    onPlay: (Int) -> Unit,
    onPlayMode: (GameMode, Int) -> Unit,
    onOpenDoc: (String) -> Unit,
    initialTab: Tab = Tab.HOME,
) {
    val container = App.container
    val audio = LocalAudio.current
    val progress = container.progress
    val player by progress.state.collectAsState()
    var tab by rememberSaveable { mutableStateOf(initialTab) }
    var scrollToCollection by remember { mutableIntStateOf(0) }
    var lockedLevel by remember { mutableStateOf<Int?>(null) }
    var confirmExit by remember { mutableStateOf(false) }
    val activity = LocalContext.current as? Activity

    LaunchedEffect(Unit) { audio.playMusic(Music.Home) }
    LaunchedEffect(tab) { Analytics.screen("tab_${tab.name.lowercase()}") }

    // Android 13+ needs the player's OK before the daily reminders and push messages can show.
    // Asked once the player reaches the main screen (after onboarding), never in the middle of a level.
    val notifyPermission = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission(),
    ) { granted -> Analytics.notificationPermission(granted) }
    LaunchedEffect(Unit) {
        val ctx = activity ?: return@LaunchedEffect
        if (android.os.Build.VERSION.SDK_INT >= 33 && !com.ngstudio.trafficjam.core.notify.Notifications.canPost(ctx)) {
            kotlinx.coroutines.delay(1200)
            notifyPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler {
        if (tab != Tab.HOME) tab = Tab.HOME else confirmExit = true
    }

    // The background fades into each tab's own colors.
    val skyAnim = tween<Color>(450)
    val skyTop by animateColorAsState(tab.palette.skyTop, skyAnim, label = "skyTop")
    val skyMid by animateColorAsState(tab.palette.skyMid, skyAnim, label = "skyMid")
    val skyBottom by animateColorAsState(tab.palette.skyBottom, skyAnim, label = "skyBottom")
    // Home (and Levels, under its map) show the cartoon town; Modes, Garage and Settings are purple gradients.
    val scenery by animateFloatAsState(if (tab == Tab.HOME || tab == Tab.LEVELS || tab == Tab.MODES) 1f else 0f, tween(450), label = "scenery")
    TownBackground(top = skyTop, mid = skyMid, bottom = skyBottom, sceneryAlpha = scenery) {
        Column(Modifier.fillMaxSize()) {
            AnimatedContent(
                tab,
                modifier = Modifier.weight(1f),
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "tab",
            ) { t ->
                CompositionLocalProvider(LocalTabPalette provides t.palette) {
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
        message = if (AdConfig.enabled) "Level ${lockedLevel ?: 0} is locked.\nWatch a short ad to unlock it now!"
        else "Level ${lockedLevel ?: 0} is locked.\nClear the levels before it to unlock it.",
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

/** Navbar colors: a soft purple dock with a lighter lavender tile for the selected tab. */
private val NavDockTop = Color(0xFFFFFFFF)
private val NavDockBottom = Color(0xFFEAF1FA)
private val NavRim = Color(0xFFD5E6F6)
private val NavTileTop = Color(0xFF5BB8F7)
private val NavTileBottom = Color(0xFF2E8BD8)
private val NavInk = Color(0xFF1C4B86)

@Composable
private fun BottomBar(selected: Tab, onSelect: (Tab) -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .drawBehind {
                // Purple dock with rounded top corners (the bottom ones run off the screen).
                val r = CornerRadius(28.dp.toPx())
                val tall = Size(size.width, size.height + 60.dp.toPx())
                drawRoundRect(Color.Black.copy(alpha = 0.25f), Offset(0f, -6.dp.toPx()), tall, r)
                drawRoundRect(NavInk, Offset.Zero, tall, r)
                drawRoundRect(NavRim, Offset(0f, 2.dp.toPx()), tall, r)
                drawRoundRect(Brush.verticalGradient(listOf(NavDockTop, NavDockBottom), 0f, size.height), Offset(0f, 5.dp.toPx()), tall, r)
                drawRoundRect(
                    Color.White.copy(alpha = 0.7f), Offset(30.dp.toPx(), 7.dp.toPx()),
                    Size(size.width - 60.dp.toPx(), 3.dp.toPx()), CornerRadius(1.5f.dp.toPx()),
                )
            }
            .navigationBarsPadding()
            .height(80.dp),
    ) {
        Row(Modifier.fillMaxSize()) {
            Tab.entries.forEachIndexed { index, t ->
                val sel = t == selected
                val weight by animateFloatAsState(if (sel) 1.5f else 1f, spring(dampingRatio = 0.7f), label = "w")
                val on by animateFloatAsState(if (sel) 1f else 0f, spring(dampingRatio = 0.6f, stiffness = 450f), label = "on")
                Box(
                    Modifier
                        .weight(weight)
                        .fillMaxHeight()
                        .clickable(remember { MutableInteractionSource() }, null) { onSelect(t) }
                        .drawBehind {
                            if (on > 0.02f) {
                                // Lighter lavender tile with a white border and a soft inner glow.
                                val inset = 6.dp.toPx()
                                val tl = Offset(inset, 10.dp.toPx() - 4.dp.toPx() * on)
                                val sz = Size(size.width - 2 * inset, size.height - 14.dp.toPx() + 4.dp.toPx() * on)
                                val rr = CornerRadius(18.dp.toPx())
                                drawRoundRect(NavInk.copy(alpha = 0.3f * on), tl + Offset(0f, 3.dp.toPx()), sz, rr)
                                drawRoundRect(Color.White.copy(alpha = on), tl, sz, rr)
                                drawRoundRect(
                                    Brush.verticalGradient(listOf(NavTileTop, NavTileBottom), tl.y, tl.y + sz.height),
                                    tl + Offset(2.5.dp.toPx(), 2.5.dp.toPx()), Size(sz.width - 5.dp.toPx(), sz.height - 5.dp.toPx()),
                                    CornerRadius(16.dp.toPx()), alpha = on,
                                )
                                drawRoundRect(
                                    Color.White.copy(alpha = 0.35f * on), tl + Offset(10.dp.toPx(), 5.dp.toPx()),
                                    Size(sz.width - 20.dp.toPx(), 6.dp.toPx()), CornerRadius(3.dp.toPx()),
                                )
                            }

                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier
                            .offset(y = (-2).dp - 9.dp * on)
                            .graphicsLayer { val k = 1f + 0.12f * on; scaleX = k; scaleY = k },
                    ) {
                        GameIcon(t.icon, 42.dp)
                    }
                    if (on > 0.5f) {
                        OutlinedText(
                            t.label,
                            Modifier.align(Alignment.BottomCenter).padding(bottom = 7.dp).graphicsLayer { alpha = (on - 0.5f) * 2f },
                            style = GameText.Button, fontSize = 15.sp, strokeWidth = 6f, outline = NavInk, depth = 0.07f,
                        )
                    }
                }
            }
        }
    }
}
