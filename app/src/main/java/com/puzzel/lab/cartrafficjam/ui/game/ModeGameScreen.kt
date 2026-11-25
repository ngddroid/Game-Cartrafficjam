package com.puzzel.lab.cartrafficjam.ui.game

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
import com.puzzel.lab.cartrafficjam.App
import com.puzzel.lab.cartrafficjam.core.audio.Haptic
import com.puzzel.lab.cartrafficjam.core.audio.Music
import com.puzzel.lab.cartrafficjam.core.audio.Sfx
import com.puzzel.lab.cartrafficjam.core.data.TOTAL_LEVELS
import com.puzzel.lab.cartrafficjam.game.engine.GameFx
import com.puzzel.lab.cartrafficjam.game.model.Difficulty
import com.puzzel.lab.cartrafficjam.game.model.GameMode
import com.puzzel.lab.cartrafficjam.game.modes.ModeEngine
import com.puzzel.lab.cartrafficjam.game.modes.ModeEvent
import com.puzzel.lab.cartrafficjam.game.modes.ModePhase
import com.puzzel.lab.cartrafficjam.game.modes.PixelEngine
import com.puzzel.lab.cartrafficjam.game.modes.SandEngine
import com.puzzel.lab.cartrafficjam.game.render.Skin
import com.puzzel.lab.cartrafficjam.ui.components.CoinPill
import com.puzzel.lab.cartrafficjam.ui.components.GameIcon
import com.puzzel.lab.cartrafficjam.ui.components.GameToast
import com.puzzel.lab.cartrafficjam.ui.components.Icon
import com.puzzel.lab.cartrafficjam.ui.components.LocalAudio
import com.puzzel.lab.cartrafficjam.ui.components.OutlinedText
import com.puzzel.lab.cartrafficjam.ui.components.RoundButton
import com.puzzel.lab.cartrafficjam.ui.components.WatchAdDialog
import com.puzzel.lab.cartrafficjam.ui.theme.GameText
import com.puzzel.lab.cartrafficjam.ui.theme.TitleFont
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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

    val events = remember(engine) { Channel<ModeEvent>(Channel.UNLIMITED) }
    remember(engine) { engine.onEvent = { events.trySend(it) } }
    LaunchedEffect(engine) {
        for (e in events) {
            when (e) {
                is ModeEvent.Won -> {
                    stars = e.stars
                    progress.completeModeLevel(mode, levelNumber, reward)
                    audio.play(Sfx.Win)
                    audio.haptic(Haptic.Success)
                    delay(900)
                    overlay = ModeOverlay.WIN
                }
                ModeEvent.Stuck -> { delay(250); overlay = ModeOverlay.REVIVE }
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
                    .background(Color(0xCC0D2C6B), RoundedCornerShape(16.dp))
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
                    overlay = ModeOverlay.NONE
                    engine.revive()
                } else showToast("Not enough coins")
            },
            onAd = { ads.showRewarded("revive", onReward = { overlay = ModeOverlay.NONE; engine.revive() }) },
            onGiveUp = {
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
                    done()
                })
            },
            onNext = { overlay = ModeOverlay.NONE; onNext() },
            onHome = { overlay = ModeOverlay.NONE; onHome() },
        )

        WatchAdDialog(
            visible = overlay == ModeOverlay.SPOT,
            title = if (mode == GameMode.SAND) "Longer Belt" else "Extra Stop",
            message = if (mode == GameMode.SAND) "Watch a short ad to fit one more truck on the belt."
            else "Watch a short ad to open this bus stop for the rest of the level.",
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
