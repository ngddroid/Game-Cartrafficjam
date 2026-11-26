package com.ngstudio.trafficjam.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ngstudio.trafficjam.game.model.SPOT_PRICE
import com.ngstudio.trafficjam.core.ads.AdConfig
import com.ngstudio.trafficjam.App
import com.ngstudio.trafficjam.core.analytics.Analytics
import com.ngstudio.trafficjam.core.audio.Haptic
import com.ngstudio.trafficjam.core.audio.Music
import com.ngstudio.trafficjam.core.audio.Sfx
import com.ngstudio.trafficjam.core.data.TOTAL_LEVELS
import com.ngstudio.trafficjam.game.engine.GameFx
import com.ngstudio.trafficjam.game.model.Difficulty
import com.ngstudio.trafficjam.game.model.GameMode
import com.ngstudio.trafficjam.game.modes.ModeEngine
import com.ngstudio.trafficjam.game.modes.ModeEvent
import com.ngstudio.trafficjam.game.modes.ModePhase
import com.ngstudio.trafficjam.game.modes.PixelEngine
import com.ngstudio.trafficjam.game.modes.SandEngine
import com.ngstudio.trafficjam.game.render.Skin
import com.ngstudio.trafficjam.ui.components.CoinPill
import com.ngstudio.trafficjam.ui.components.GameIcon
import com.ngstudio.trafficjam.ui.components.GameToast
import com.ngstudio.trafficjam.ui.components.Icon
import com.ngstudio.trafficjam.ui.components.LocalAudio
import com.ngstudio.trafficjam.ui.components.OutlinedText
import com.ngstudio.trafficjam.ui.components.RoundButton
import com.ngstudio.trafficjam.ui.components.WatchAdDialog
import com.ngstudio.trafficjam.ui.theme.GameText
import com.ngstudio.trafficjam.ui.theme.TitleFont
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.ngstudio.trafficjam.ui.theme.Sunset

/** Plays a Pixel Crowd or Sand Sweep level. Crowd Loop runs on the classic [GameScreen]. */
@Composable
fun ModeGameScreen(mode: GameMode, levelNumber: Int, onHome: () -> Unit, onPlayLevel: (Int) -> Unit) {
    val audio = LocalAudio.current
    val player by App.container.progress.state.collectAsState()
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { audio.playMusic(Music.Game) }
    key(levelNumber, attempt) {
        ModeSession(
            mode = mode,
            levelNumber = levelNumber,
            skin = Skin.of(player.skin),
            onHome = onHome,
            onRetry = { attempt++ },
            onNext = { onPlayLevel((levelNumber + 1).coerceAtMost(TOTAL_LEVELS)) },
        )
    }
}

private enum class ModeOverlay { NONE, PAUSE, REVIVE, FAIL, WIN, SPOT }

@Composable
private fun ModeSession(
    mode: GameMode,
    levelNumber: Int,
    skin: Skin,
    onHome: () -> Unit,
    onRetry: () -> Unit,
    onNext: () -> Unit,
) {
    val container = App.container
    val audio = LocalAudio.current
    val progress = container.progress
    val ads = container.ads
    val player by progress.state.collectAsState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    var overlay by remember { mutableStateOf(ModeOverlay.NONE) }
    var spot by remember { mutableIntStateOf(-1) }
    var stars by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }
    var toastId by remember { mutableIntStateOf(0) }
    var hint by remember { mutableStateOf(levelNumber <= 2) }

    fun showToast(msg: String) {
        toast = msg
        val id = ++toastId
        scope.launch { delay(1800); if (toastId == id) toast = null }
    }

    val engine: ModeEngine = remember(levelNumber) {
        val fx = object : GameFx {
            override fun sfx(s: Sfx, rate: Float) = audio.play(s, rate)
            override fun haptic(h: Haptic) = audio.haptic(h)
        }
        when (mode) {
            GameMode.SAND -> SandEngine(levelNumber, skin, fx)
            else -> PixelEngine(levelNumber, skin, fx)
        }
    }
    val reward = remember(engine) { rewardFor(engine.difficulty) }
    // Analytics: one level_start per attempt, and a quit if the player leaves before it ends.
    val startedAt = remember(levelNumber) { android.os.SystemClock.elapsedRealtime() }
    val ended = remember(levelNumber) { booleanArrayOf(false) }
    fun playSeconds() = ((android.os.SystemClock.elapsedRealtime() - startedAt) / 1000).toInt()
    androidx.compose.runtime.DisposableEffect(levelNumber) {
        Analytics.levelStart(mode, levelNumber, engine.difficulty.name.lowercase())
        onDispose { if (!ended[0]) Analytics.levelQuit(mode, levelNumber, playSeconds()) }
    }

    val events = remember(engine) { Channel<ModeEvent>(Channel.UNLIMITED) }
    remember(engine) { engine.onEvent = { events.trySend(it) } }
    LaunchedEffect(engine) {
        for (e in events) {
            when (e) {
                is ModeEvent.Won -> {
                    stars = e.stars
                    ended[0] = true
                    Analytics.levelWin(mode, levelNumber, e.stars, reward, playSeconds())
                    progress.completeModeLevel(mode, levelNumber, reward)
                    audio.play(Sfx.Win)
                    audio.haptic(Haptic.Success)
                    delay(900)
                    overlay = ModeOverlay.WIN
                }
                ModeEvent.Stuck -> {
                    Analytics.levelStuck(mode, levelNumber)
                    delay(250)
                    overlay = ModeOverlay.REVIVE
                }
                is ModeEvent.Toast -> showToast(e.message)
                is ModeEvent.LockedSpot -> { spot = e.index; overlay = ModeOverlay.SPOT }
            }
        }
    }

    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(engine) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1e9f
                last = now
                if (overlay != ModeOverlay.PAUSE) {
                    engine.update(dt)
                    time += dt
                }
            }
        }
    }
    LaunchedEffect(hint) { if (hint) { delay(6000); hint = false } }

    BackHandler(overlay == ModeOverlay.NONE) { overlay = ModeOverlay.PAUSE }

    val topInset = WindowInsets.statusBars.getTop(density).toFloat()
    val bottomInset = WindowInsets.navigationBars.getBottom(density).toFloat()
    LaunchedEffect(topInset, bottomInset) {
        if (engine.ready) engine.setSize(engine.width, engine.height, topInset, bottomInset)
    }

    Box(Modifier.fillMaxSize().background(Color(0xFFC9DBFA))) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { engine.setSize(it.width.toFloat(), it.height.toFloat(), topInset, bottomInset) }
                .pointerInput(engine) {
                    detectTapGestures(onPress = {
                        hint = false
                        engine.onTap(it)
                    })
                },
        ) {
            engine.frame // observe
            engine.draw(this, time)
        }

        if (engine.ready) {
            ModeHud(
                title = "${mode.title} $levelNumber",
                counter = engine.hudCounter.coerceAtLeast(0),
                counterLabel = engine.counterLabel,
                accent = Color(mode.accent),
                coins = player.coins,
                onPause = { overlay = ModeOverlay.PAUSE },
            )
        }

        AnimatedVisibility(hint && engine.phase == ModePhase.PLAYING, Modifier.align(Alignment.BottomCenter), enter = fadeIn(), exit = fadeOut()) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 18.dp)
                    .background(Sunset.Outline.copy(alpha = 0.8f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    when (mode) {
                        GameMode.SAND -> "Tap a free truck. It rides the belt and scoops sand of its own color from the bottom of the picture!"
                        else -> "Tap a garage to send its bus to a stop. Riders on the edge of the picture hop on when the color matches!"
                    },
                    style = GameText.Label.copy(color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center),
                )
            }
        }

        if (engine.difficulty != Difficulty.NORMAL) DifficultyBanner(engine.difficulty)

        GameToast(toast, Modifier.align(Alignment.Center).offset(y = (-40).dp))

        PauseDialog(
            visible = overlay == ModeOverlay.PAUSE,
            sound = player.sound, music = player.music, vibration = player.vibration,
            onSound = progress::setSound, onMusic = progress::setMusic, onVibration = progress::setVibration,
            onResume = { overlay = ModeOverlay.NONE },
            onRestart = { overlay = ModeOverlay.NONE; onRetry() },
            onHome = { overlay = ModeOverlay.NONE; onHome() },
        )

        ReviveDialog(
            visible = overlay == ModeOverlay.REVIVE,
            coins = player.coins,
            title = engine.reviveTitle,
            message = engine.reviveMessage,
            onBuy = {
                if (progress.spendCoins(REVIVE_PRICE)) {
                    Analytics.spendCoins("revive", REVIVE_PRICE)
                    Analytics.revive(mode, levelNumber, "coins")
                    overlay = ModeOverlay.NONE
                    engine.revive()
                } else showToast("Not enough coins")
            },
            onAd = {
                ads.showRewarded("revive", onReward = {
                    Analytics.revive(mode, levelNumber, "ad")
                    overlay = ModeOverlay.NONE
                    engine.revive()
                })
            },
            onGiveUp = {
                ended[0] = true
                Analytics.levelFail(mode, levelNumber, playSeconds())
                audio.play(Sfx.Fail)
                audio.haptic(Haptic.Fail)
                overlay = ModeOverlay.FAIL
            },
        )

        FailDialog(
            visible = overlay == ModeOverlay.FAIL,
            onRetry = { overlay = ModeOverlay.NONE; onRetry() },
            onSkip = {
                ads.showRewarded("skip_level", onReward = {
                    Analytics.levelSkip(mode, levelNumber)
                    progress.completeModeLevel(mode, levelNumber, 0)
                    overlay = ModeOverlay.NONE
                    onNext()
                })
            },
            onHome = { overlay = ModeOverlay.NONE; onHome() },
        )

        WinDialog(
            visible = overlay == ModeOverlay.WIN,
            levelNumber = levelNumber,
            stars = stars,
            reward = reward,
            newWorld = null,
            onDouble = { done ->
                ads.showRewarded("double_coins", onReward = {
                    progress.addCoins(reward)
                    Analytics.earnCoins("double_coins", reward)
                    done()
                })
            },
            onNext = { overlay = ModeOverlay.NONE; onNext() },
            onHome = { overlay = ModeOverlay.NONE; onHome() },
        )

        WatchAdDialog(
            visible = overlay == ModeOverlay.SPOT,
            title = if (mode == GameMode.SAND) "Longer Belt" else "Extra Stop",
            message = if (mode == GameMode.SAND) "Make room for one more truck on the belt."
            else "Open this bus stop for the rest of the level.",
            price = SPOT_PRICE,
            coins = player.coins,
            onBuy = {
                if (progress.spendCoins(SPOT_PRICE)) {
                    Analytics.spendCoins(if (mode == GameMode.SAND) "belt_slot" else "bus_stop", SPOT_PRICE)
                    audio.play(Sfx.Coin)
                    overlay = ModeOverlay.NONE
                    if (spot >= 0) engine.unlockSpot(spot)
                } else showToast("Not enough coins")
            },
            onWatch = {
                ads.showRewarded("slot", onReward = {
                    overlay = ModeOverlay.NONE
                    if (spot >= 0) engine.unlockSpot(spot)
                })
            },
            onDismiss = { overlay = ModeOverlay.NONE },
        )
    }
}

@Composable
private fun ModeHud(title: String, counter: Int, counterLabel: String, accent: Color, coins: Int, onPause: () -> Unit) {
    val bump = remember { Animatable(1f) }
    LaunchedEffect(counter) {
        bump.snapTo(1.12f)
        bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
    }
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton(onPause, size = 44.dp) { GameIcon(Icon.GEAR, 30.dp) }
        Spacer(Modifier.weight(1f))
        Column(
            Modifier
                .background(Color(0x88000000), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedText(title, style = GameText.Title, fontSize = 19.sp, strokeWidth = 6f)
            Row(
                Modifier
                    .offset(y = (-2).dp)
                    .scale(bump.value)
                    .background(Brush.verticalGradient(listOf(accent, accent.copy(alpha = 0.85f))), RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedText("$counter", style = GameText.Title, fontSize = 14.sp, strokeWidth = 4f)
                Spacer(Modifier.width(4.dp))
                Text(counterLabel, style = GameText.Small.copy(color = Color.White, fontFamily = TitleFont, fontSize = 11.sp))
            }
        }
        Spacer(Modifier.weight(1f))
        CoinPill(coins)
    }
}
