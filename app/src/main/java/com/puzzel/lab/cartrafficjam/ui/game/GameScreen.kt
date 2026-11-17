package com.puzzel.lab.cartrafficjam.ui.game

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.cartrafficjam.App
import com.puzzel.lab.cartrafficjam.core.audio.Haptic
import com.puzzel.lab.cartrafficjam.core.audio.Music
import com.puzzel.lab.cartrafficjam.core.audio.Sfx
import com.puzzel.lab.cartrafficjam.core.data.TOTAL_LEVELS
import com.puzzel.lab.cartrafficjam.core.data.collectionOf
import com.puzzel.lab.cartrafficjam.game.engine.GameEngine
import com.puzzel.lab.cartrafficjam.game.engine.GameEvent
import com.puzzel.lab.cartrafficjam.game.engine.GameFx
import com.puzzel.lab.cartrafficjam.game.gen.LevelGenerator
import com.puzzel.lab.cartrafficjam.game.model.BoosterType
import com.puzzel.lab.cartrafficjam.game.model.Difficulty
import com.puzzel.lab.cartrafficjam.game.render.GameRenderer.drawGame
import com.puzzel.lab.cartrafficjam.game.render.Skin
import com.puzzel.lab.cartrafficjam.game.render.Themes
import com.puzzel.lab.cartrafficjam.ui.components.ButtonColor
import com.puzzel.lab.cartrafficjam.ui.components.CoinPill
import com.puzzel.lab.cartrafficjam.ui.components.GameButton
import com.puzzel.lab.cartrafficjam.ui.components.GameDialog
import com.puzzel.lab.cartrafficjam.ui.components.GameIcon
import com.puzzel.lab.cartrafficjam.ui.components.GamePanel
import com.puzzel.lab.cartrafficjam.ui.components.GameToast
import com.puzzel.lab.cartrafficjam.ui.components.GameToggle
import com.puzzel.lab.cartrafficjam.ui.components.Icon
import com.puzzel.lab.cartrafficjam.ui.components.LocalAudio
import com.puzzel.lab.cartrafficjam.ui.components.OutlinedText
import com.puzzel.lab.cartrafficjam.ui.components.RibbonBanner
import com.puzzel.lab.cartrafficjam.ui.components.RoundButton
import com.puzzel.lab.cartrafficjam.ui.components.StarRow
import com.puzzel.lab.cartrafficjam.ui.components.WatchAdDialog
import com.puzzel.lab.cartrafficjam.ui.theme.GameText
import com.puzzel.lab.cartrafficjam.ui.theme.TitleFont
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val REVIVE_PRICE = 120

private fun rewardFor(level: Int) = when (LevelGenerator.get(level).difficulty) {
    Difficulty.NORMAL -> 10
    Difficulty.HARD -> 20
    Difficulty.SUPER_HARD -> 30
}

@Composable
fun GameScreen(levelNumber: Int, onHome: () -> Unit, onPlayLevel: (Int) -> Unit) {
    val container = App.container
    val audio = LocalAudio.current
    val progress = container.progress
    val player by progress.state.collectAsState()
    var attempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) { audio.playMusic(Music.Game) }

    key(levelNumber, attempt) {
        GameSession(
            levelNumber = levelNumber,
            skin = Skin.of(player.skin),
            onHome = onHome,
            onRetry = { attempt++ },
            onNext = { onPlayLevel((levelNumber + 1).coerceAtMost(TOTAL_LEVELS)) },
        )
    }
}

private enum class Overlay { NONE, PAUSE, PROP, REVIVE, FAIL, WIN, SLOT }

@Composable
private fun GameSession(
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

    var overlay by remember { mutableStateOf(Overlay.NONE) }
    var prop by remember { mutableStateOf(BoosterType.SHUFFLE) }
    var slotIndex by remember { mutableIntStateOf(-1) }
    var stars by remember { mutableIntStateOf(0) }
    var toast by remember { mutableStateOf<String?>(null) }
    var toastId by remember { mutableIntStateOf(0) }
    var newWorld by remember { mutableStateOf<String?>(null) }

    fun showToast(msg: String) {
        toast = msg
        val id = ++toastId
        scope.launch { delay(1800); if (toastId == id) toast = null }
    }

    val level = remember(levelNumber) { LevelGenerator.get(levelNumber) }
    val theme = remember(levelNumber) { Themes.forLevel(levelNumber) }
    val reward = remember(levelNumber) { rewardFor(levelNumber) }

    val engine = remember(levelNumber) {
        GameEngine(
            level, theme, skin,
            fx = object : GameFx {
                override fun sfx(s: Sfx, rate: Float) = audio.play(s, rate)
                override fun haptic(h: Haptic) = audio.haptic(h)
            },
        )
    }
    // Engine callbacks fire mid-frame; queue them and handle them in order in a coroutine.
    val events = remember(engine) { Channel<GameEvent>(Channel.UNLIMITED) }
    remember(engine) { engine.onEvent = { events.trySend(it) } }

    LaunchedEffect(engine) {
        for (e in events) {
            when (e) {
                is GameEvent.Won -> {
                    stars = e.stars
                    val opened = progress.completeLevel(levelNumber, e.stars, reward)
                    if (opened) newWorld = Themes.collectionName(collectionOf(levelNumber + 1))
                    audio.play(Sfx.Win)
                    audio.haptic(Haptic.Success)
                    delay(900)
                    overlay = Overlay.WIN
                }
                GameEvent.Stuck -> { delay(250); overlay = Overlay.REVIVE }
                is GameEvent.LockedSlot -> { slotIndex = e.index; overlay = Overlay.SLOT }
                GameEvent.NoSpace -> showToast("All parking spots are full!")
                GameEvent.VipBusy -> showToast("The VIP spot is taken right now")
            }
        }
    }

    // Frame loop
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(engine) {
        var last = 0L
        while (true) {
            withFrameNanos { now ->
                val dt = if (last == 0L) 0f else (now - last) / 1e9f
                last = now
                if (overlay != Overlay.PAUSE) {
                    engine.update(dt)
                    time += dt
                }
            }
        }
    }

    BackHandler(overlay == Overlay.NONE) { overlay = Overlay.PAUSE }

    val topInset = WindowInsets.statusBars.getTop(density).toFloat()
    val bottomInset = WindowInsets.navigationBars.getBottom(density).toFloat()

    // The navigation bar can hide after the first layout; re-fit the board when insets change.
    LaunchedEffect(topInset, bottomInset) {
        if (engine.ready) engine.setSize(engine.layout.width, engine.layout.height, topInset, bottomInset)
    }

    Box(Modifier.fillMaxSize().background(theme.outside)) {
        Canvas(
            Modifier
                .fillMaxSize()
                .onSizeChanged { engine.setSize(it.width.toFloat(), it.height.toFloat(), topInset, bottomInset) }
                .pointerInput(engine) { detectTapGestures(onPress = { engine.onTap(it) }) },
        ) {
            engine.frame // observe
            drawGame(engine, time)
        }

        if (engine.ready) {
            Hud(engine, levelNumber, level.difficulty, player.coins, onPause = { overlay = Overlay.PAUSE })
            QueueSign(engine)
            TutorialLayer(engine)
            BoosterBar(
                engine = engine,
                counts = player.boosters,
                modifier = Modifier.align(Alignment.BottomCenter),
                onBooster = { type ->
                    if (engine.phase != com.puzzel.lab.cartrafficjam.game.engine.Phase.PLAYING) return@BoosterBar
                    if (player.boosterCount(type) > 0) {
                        if (useBooster(engine, type)) progress.addBooster(type, -1)
                    } else {
                        prop = type
                        overlay = Overlay.PROP
                    }
                },
            )
        }

        if (level.difficulty != Difficulty.NORMAL) DifficultyBanner(level.difficulty)

        GameToast(toast, Modifier.align(Alignment.Center).offset(y = (-40).dp))

        AnimatedVisibility(engine.craneArmed, Modifier.align(Alignment.Center), enter = fadeIn(), exit = fadeOut()) {
            Row(
                Modifier
                    .background(Color(0xE61A3A6B), RoundedCornerShape(22.dp))
                    .border(2.dp, Color(0xFFFFD54F), RoundedCornerShape(22.dp))
                    .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GameIcon(Icon.CRANE, 34.dp)
                Spacer(Modifier.width(8.dp))
                Text("Tap any vehicle to lift it!", style = GameText.Label.copy(color = Color.White))
                Spacer(Modifier.width(8.dp))
                Box(
                    Modifier.size(32.dp).background(Color(0xFFF2453D), CircleShape).clickable {
                        engine.craneArmed = false
                        progress.addBooster(BoosterType.CRANE, 1)
                    },
                    contentAlignment = Alignment.Center,
                ) { Text("✕", style = GameText.Label.copy(color = Color.White)) }
            }
        }

        // --- Dialogs ---

        PauseDialog(
            visible = overlay == Overlay.PAUSE,
            sound = player.sound, music = player.music, vibration = player.vibration,
            onSound = progress::setSound, onMusic = progress::setMusic, onVibration = progress::setVibration,
            onResume = { overlay = Overlay.NONE },
            onRestart = { overlay = Overlay.NONE; onRetry() },
            onHome = { overlay = Overlay.NONE; onHome() },
        )

        PropDialog(
            visible = overlay == Overlay.PROP,
            type = prop,
            coins = player.coins,
            onDismiss = { overlay = Overlay.NONE },
            onBuy = {
                if (progress.spendCoins(prop.price)) {
                    overlay = Overlay.NONE
                    audio.play(Sfx.Coin)
                    if (!useBooster(engine, prop)) progress.addBooster(prop, 1)
                } else {
                    showToast("Not enough coins")
                }
            },
            onAd = {
                ads.showRewarded("booster_${prop.name}", onReward = {
                    overlay = Overlay.NONE
                    if (!useBooster(engine, prop)) progress.addBooster(prop, 1)
                })
            },
        )

        ReviveDialog(
            visible = overlay == Overlay.REVIVE,
            coins = player.coins,
            onBuy = {
                if (progress.spendCoins(REVIVE_PRICE)) {
                    overlay = Overlay.NONE
                    engine.revive()
                } else showToast("Not enough coins")
            },
            onAd = { ads.showRewarded("revive", onReward = { overlay = Overlay.NONE; engine.revive() }) },
            onGiveUp = {
                audio.play(Sfx.Fail)
                audio.haptic(Haptic.Fail)
                overlay = Overlay.FAIL
            },
        )

        FailDialog(
            visible = overlay == Overlay.FAIL,
            onRetry = { overlay = Overlay.NONE; onRetry() },
            onSkip = {
                ads.showRewarded("skip_level", onReward = {
                    progress.completeLevel(levelNumber, 1, 0)
                    overlay = Overlay.NONE
                    onNext()
                })
            },
            onHome = { overlay = Overlay.NONE; onHome() },
        )

        WinDialog(
            visible = overlay == Overlay.WIN,
            levelNumber = levelNumber,
            stars = stars,
            reward = reward,
            newWorld = newWorld,
            onDouble = { done ->
                ads.showRewarded("double_coins", onReward = {
                    progress.addCoins(reward)
                    done()
                })
            },
            onNext = { overlay = Overlay.NONE; onNext() },
            onHome = { overlay = Overlay.NONE; onHome() },
        )

        WatchAdDialog(
            visible = overlay == Overlay.SLOT,
            title = "Extra Spot",
            message = "Watch a short ad to open this parking spot for the rest of the level.",
            onWatch = {
                ads.showRewarded("slot", onReward = {
                    overlay = Overlay.NONE
                    if (slotIndex >= 0) engine.unlockSlot(slotIndex)
                })
            },
            onDismiss = { overlay = Overlay.NONE },
        )
    }
}

private fun useBooster(engine: GameEngine, type: BoosterType): Boolean = when (type) {
    BoosterType.SHUFFLE -> { engine.shuffle(); true }
    BoosterType.CRANE -> engine.armCrane()
    BoosterType.SORT -> { engine.useSort(); true }
}

// --- HUD --------------------------------------------------------------------------------------

@Composable
private fun Hud(engine: GameEngine, level: Int, difficulty: Difficulty, coins: Int, onPause: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundButton(onPause, size = 44.dp) { GameIcon(Icon.GEAR, 30.dp) }
        Spacer(Modifier.weight(1f))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier
                    .background(Color(0x66000000), RoundedCornerShape(18.dp))
                    .padding(horizontal = 22.dp, vertical = 5.dp),
            ) {
                OutlinedText("Level $level", style = GameText.Title, fontSize = 22.sp, strokeWidth = 6f)
            }
            if (difficulty != Difficulty.NORMAL) {
                Text(
                    difficulty.label ?: "",
                    style = GameText.Small.copy(color = Color.White, fontFamily = TitleFont, fontSize = 11.sp),
                    modifier = Modifier
                        .offset(y = (-3).dp)
                        .background(if (difficulty == Difficulty.HARD) Color(0xFFFF6D00) else Color(0xFFD500F9), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 1.dp),
                )
            }
        }
        Spacer(Modifier.weight(1f))
        CoinPill(coins)
    }
}

@Composable
private fun QueueSign(engine: GameEngine) {
    val lay = engine.layout
    val density = LocalDensity.current
    val remaining = engine.remaining
    val bump = remember { Animatable(1f) }
    LaunchedEffect(remaining) {
        bump.snapTo(1.18f)
        bump.animateTo(1f, spring(dampingRatio = 0.4f, stiffness = 600f))
    }
    val pos = with(density) { IntOffset((lay.queueSign.x - 42.dp.toPx()).roundToInt(), (lay.queueSign.y - 26.dp.toPx()).roundToInt()) }
    Column(
        Modifier.offset { pos },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(
            Modifier
                .width(84.dp)
                .scale(bump.value)
                .background(Color(0xFF0D47A1), RoundedCornerShape(10.dp))
                .padding(3.dp)
                .background(Brush.verticalGradient(listOf(Color(0xFF64B5F6), Color(0xFF1E88E5))), RoundedCornerShape(8.dp))
                .padding(vertical = 2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedText("$remaining", style = GameText.Title, fontSize = 22.sp, strokeWidth = 5f, outline = Color(0xFF0D2C6B))
            Text("Queue", style = GameText.Small.copy(color = Color(0xFFFFE082), fontFamily = TitleFont, fontSize = 12.sp), modifier = Modifier.offset(y = (-4).dp))
        }
        Box(Modifier.size(6.dp, 14.dp).background(Color(0xFF546E7A)))
    }
}

@Composable
private fun TutorialLayer(engine: GameEngine) {
    val density = LocalDensity.current
    val text = engine.tutorialText
    val lay = engine.layout
    if (text != null) {
        val y = with(density) { (lay.lotRect.top + 6.dp.toPx()).toDp() }
        Box(
            Modifier
                .fillMaxWidth()
                .offset(y = y)
                .background(Color(0x99000000))
                .padding(vertical = 10.dp, horizontal = 12.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = GameText.Label.copy(color = Color.White, fontSize = 15.sp, textAlign = TextAlign.Center))
        }
    }
    val p = engine.tutorialPoint ?: return
    val anim = rememberInfiniteTransition(label = "hand")
    val bob by anim.animateFloat(-8f, 8f, infiniteRepeatable(tween(520), RepeatMode.Reverse), label = "bob")
    val ring by anim.animateFloat(1f, 2f, infiniteRepeatable(tween(700, easing = androidx.compose.animation.core.LinearEasing)), label = "ring")
    with(density) {
        Box(
            Modifier
                .offset { IntOffset((p.x - 30.dp.toPx()).roundToInt(), (p.y - 30.dp.toPx()).roundToInt()) }
                .size(60.dp)
                .drawBehind {
                    drawCircle(Color.White.copy(alpha = (2f - ring) * 0.7f), size.minDimension / 2 * ring * 0.7f, style = Stroke(4.dp.toPx()))
                },
        )
        Box(
            Modifier.offset {
                IntOffset((p.x - 14.dp.toPx() + bob).roundToInt(), (p.y - 2.dp.toPx() + bob).roundToInt())
            },
        ) { GameIcon(Icon.HAND, 64.dp) }
    }
}

@Composable
private fun BoosterBar(
    engine: GameEngine,
    counts: Map<BoosterType, Int>,
    modifier: Modifier,
    onBooster: (BoosterType) -> Unit,
) {
    val density = LocalDensity.current
    val h = with(density) { (engine.layout.boosterBarH).toDp() }
    Row(
        modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(h)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BoosterButton("Shuffle", Icon.SHUFFLE, counts[BoosterType.SHUFFLE] ?: 0) { onBooster(BoosterType.SHUFFLE) }
        BoosterButton("Crane", Icon.CRANE, counts[BoosterType.CRANE] ?: 0) { onBooster(BoosterType.CRANE) }
        BoosterButton("Sort", Icon.SORT, counts[BoosterType.SORT] ?: 0) { onBooster(BoosterType.SORT) }
    }
}

@Composable
private fun BoosterButton(label: String, icon: Icon, count: Int, onClick: () -> Unit) {
    val audio = LocalAudio.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(Modifier.size(84.dp, 86.dp)) {
        Column(
            Modifier
                .padding(top = 6.dp, end = 6.dp)
                .size(78.dp, 80.dp)
                .scale(if (pressed) 0.92f else 1f)
                .clickable(interaction, null) { audio.play(Sfx.Click); onClick() }
                .drawBehind {
                    val r = CornerRadius(18.dp.toPx())
                    drawRoundRect(Color(0xFF1E5FD0), Offset(0f, 4.dp.toPx()), size, r)
                    drawRoundRect(Color.White, Offset.Zero, Size(size.width, size.height - 2.dp.toPx()), r)
                    drawRoundRect(
                        Brush.verticalGradient(listOf(Color(0xFFE9F4FF), Color(0xFFBFDDFF))),
                        Offset(3.dp.toPx(), 3.dp.toPx()), Size(size.width - 6.dp.toPx(), size.height - 8.dp.toPx()), CornerRadius(15.dp.toPx()),
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(5.dp))
            GameIcon(icon, 48.dp)
            OutlinedText(label, style = GameText.Button, fontSize = 15.sp, strokeWidth = 5f, outline = Color(0xFF1E4FA8))
        }
        Box(
            Modifier
                .align(Alignment.TopEnd)
                .size(26.dp)
                .background(if (count > 0) Color(0xFFF2453D) else Color(0xFF5FC12F), CircleShape)
                .border(2.dp, Color.White, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(if (count > 0) "$count" else "+", style = GameText.Button.copy(fontSize = 14.sp))
        }
    }
}

@Composable
private fun DifficultyBanner(difficulty: Difficulty) {
    var show by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(1900)
        show = false
    }
    val audio = LocalAudio.current
    LaunchedEffect(Unit) { delay(150); audio.play(Sfx.Whoosh); delay(120); audio.play(Sfx.Warning) }
    AnimatedVisibility(
        show,
        Modifier.fillMaxSize(),
        enter = fadeIn(tween(200)) + slideInVertically { it / 6 },
        exit = fadeOut(tween(300)) + slideOutVertically { -it / 6 },
    ) {
        Box(Modifier.fillMaxSize().background(Color(0x66000000)), contentAlignment = Alignment.Center) {
            val pulse = rememberInfiniteTransition(label = "hard")
            val s by pulse.animateFloat(0.96f, 1.06f, infiniteRepeatable(tween(380), RepeatMode.Reverse), label = "s")
            val col = if (difficulty == Difficulty.HARD) Color(0xFFFF6D00) else Color(0xFFD500F9)
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .background(Brush.horizontalGradient(listOf(Color.Transparent, col.copy(alpha = 0.9f), col, col.copy(alpha = 0.9f), Color.Transparent))),
                contentAlignment = Alignment.Center,
            ) {
                OutlinedText(
                    difficulty.label ?: "", Modifier.scale(s), style = GameText.Display, fontSize = 42.sp,
                    color = Color(0xFFFFF59D), outline = Color(0xFF4A1500), strokeWidth = 10f,
                )
            }
        }
    }
}

// --- Dialogs ----------------------------------------------------------------------------------

@Composable
private fun PauseDialog(
    visible: Boolean,
    sound: Boolean,
    music: Boolean,
    vibration: Boolean,
    onSound: (Boolean) -> Unit,
    onMusic: (Boolean) -> Unit,
    onVibration: (Boolean) -> Unit,
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onHome: () -> Unit,
) {
    GameDialog(visible, onResume) {
        GamePanel("Paused", onClose = onResume) {
            ToggleRow("Sound", sound, onSound)
            ToggleRow("Music", music, onMusic)
            ToggleRow("Vibration", vibration, onVibration)
            Spacer(Modifier.height(14.dp))
            GameButton("Resume", onResume, Modifier.fillMaxWidth(), color = ButtonColor.GREEN)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GameButton("Restart", onRestart, Modifier.weight(1f), color = ButtonColor.ORANGE, fontSize = 20.sp, height = 54.dp)
                GameButton("Home", onHome, Modifier.weight(1f), color = ButtonColor.BLUE, fontSize = 20.sp, height = 54.dp)
            }
        }
    }
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = GameText.Label.copy(fontSize = 19.sp), modifier = Modifier.weight(1f))
        GameToggle(value, onChange)
    }
}

@Composable
private fun PropDialog(
    visible: Boolean,
    type: BoosterType,
    coins: Int,
    onDismiss: () -> Unit,
    onBuy: () -> Unit,
    onAd: () -> Unit,
) {
    GameDialog(visible, onDismiss) {
        GamePanel(type.title, onClose = onDismiss) {
            Box(
                Modifier
                    .size(124.dp)
                    .background(Color(0xFFFFE6C7), RoundedCornerShape(26.dp))
                    .border(3.dp, Color(0xFFF3C98B), RoundedCornerShape(26.dp)),
                contentAlignment = Alignment.Center,
            ) {
                GameIcon(
                    when (type) {
                        BoosterType.SHUFFLE -> Icon.SHUFFLE
                        BoosterType.CRANE -> Icon.CRANE
                        BoosterType.SORT -> Icon.SORT
                    },
                    92.dp,
                )
            }
            Spacer(Modifier.height(14.dp))
            Text(type.description, style = GameText.Body.copy(fontSize = 17.sp, textAlign = TextAlign.Center, color = Color(0xFF8A4B14)))
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GameButton(
                    "${type.price}", onBuy, Modifier.weight(1f), color = ButtonColor.GREEN, fontSize = 22.sp, enabled = coins >= type.price,
                    leading = { GameIcon(Icon.COIN, 30.dp) },
                )
                GameButton("Free", onAd, Modifier.weight(1f), color = ButtonColor.ORANGE, fontSize = 22.sp, leading = { GameIcon(Icon.AD, 30.dp) })
            }
        }
    }
}

@Composable
private fun ReviveDialog(visible: Boolean, coins: Int, onBuy: () -> Unit, onAd: () -> Unit, onGiveUp: () -> Unit) {
    GameDialog(visible, onGiveUp) {
        GamePanel("Out of Space!", frame = Color(0xFFF2453D), onClose = onGiveUp) {
            val beat = rememberInfiniteTransition(label = "beat")
            val s by beat.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(420, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "s")
            GameIcon(Icon.HEART, 120.dp, Modifier.scale(s))
            Spacer(Modifier.height(8.dp))
            Text(
                "Every parking spot is taken.\nKeep going with an extra spot!",
                style = GameText.Body.copy(fontSize = 17.sp, textAlign = TextAlign.Center, color = Color(0xFF8A4B14)),
            )
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GameButton(
                    "$REVIVE_PRICE", onBuy, Modifier.weight(1f), color = ButtonColor.GREEN, fontSize = 22.sp, enabled = coins >= REVIVE_PRICE,
                    leading = { GameIcon(Icon.COIN, 30.dp) },
                )
                GameButton("Revive", onAd, Modifier.weight(1f), color = ButtonColor.ORANGE, fontSize = 22.sp, leading = { GameIcon(Icon.AD, 30.dp) })
            }
        }
    }
}

@Composable
private fun FailDialog(visible: Boolean, onRetry: () -> Unit, onSkip: () -> Unit, onHome: () -> Unit) {
    GameDialog(visible, onHome) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            RibbonBanner("Level Failed", Color(0xFF7A8BA8), Modifier.fillMaxWidth(0.9f))
            Spacer(Modifier.height(16.dp))
            val wob = rememberInfiniteTransition(label = "sad")
            val r by wob.animateFloat(-6f, 6f, infiniteRepeatable(tween(600), RepeatMode.Reverse), label = "r")
            GameIcon(Icon.SAD, 150.dp, Modifier.graphicsLayer { rotationZ = r })
            Spacer(Modifier.height(10.dp))
            Text("So close! Want to try again?", style = GameText.Label.copy(color = Color.White, fontSize = 18.sp))
            Spacer(Modifier.height(28.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth(0.92f)) {
                GameButton("Retry", onRetry, Modifier.weight(1f), color = ButtonColor.GREEN)
                GameButton("Skip", onSkip, Modifier.weight(1f), color = ButtonColor.PURPLE, leading = { GameIcon(Icon.AD, 30.dp) })
            }
            Spacer(Modifier.height(12.dp))
            GameButton("Home", onHome, Modifier.fillMaxWidth(0.5f), color = ButtonColor.BLUE, height = 50.dp, fontSize = 19.sp)
        }
    }
}

@Composable
private fun WinDialog(
    visible: Boolean,
    levelNumber: Int,
    stars: Int,
    reward: Int,
    newWorld: String?,
    onDouble: (done: () -> Unit) -> Unit,
    onNext: () -> Unit,
    onHome: () -> Unit,
) {
    val audio = LocalAudio.current
    var doubled by remember { mutableStateOf(false) }
    var flyKey by remember { mutableIntStateOf(0) }
    GameDialog(visible, null) {
        Box(Modifier.fillMaxSize()) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                val glow = rememberInfiniteTransition(label = "glow")
                val rot by glow.animateFloat(0f, 360f, infiniteRepeatable(tween(9000, easing = androidx.compose.animation.core.LinearEasing)), label = "rot")
                Box(contentAlignment = Alignment.Center) {
                    Canvas(Modifier.size(300.dp).graphicsLayer { rotationZ = rot }) {
                        val c = center
                        for (i in 0 until 12) {
                            val a = Math.toRadians(i * 30.0)
                            val a2 = Math.toRadians(i * 30.0 + 12)
                            val p = androidx.compose.ui.graphics.Path().apply {
                                moveTo(c.x, c.y)
                                lineTo(c.x + (size.width * kotlin.math.cos(a)).toFloat(), c.y + (size.width * kotlin.math.sin(a)).toFloat())
                                lineTo(c.x + (size.width * kotlin.math.cos(a2)).toFloat(), c.y + (size.width * kotlin.math.sin(a2)).toFloat())
                                close()
                            }
                            drawPath(p, Brush.radialGradient(listOf(Color(0x66FFF59D), Color.Transparent), c, size.width / 2))
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        RibbonBanner("Level Complete!", Color(0xFFFFA000), Modifier.width(320.dp), fontSize = 28.sp)
                        Spacer(Modifier.height(14.dp))
                        StarRow(stars, 72.dp, animate = visible, onStar = { i ->
                            audio.play(listOf(Sfx.Star1, Sfx.Star2, Sfx.Star3)[i])
                        })
                    }
                }
                Text("Level $levelNumber cleared", style = GameText.Label.copy(color = Color.White, fontSize = 17.sp))
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .background(Color(0x66000000), RoundedCornerShape(24.dp))
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    GameIcon(Icon.COIN, 40.dp)
                    Spacer(Modifier.width(8.dp))
                    OutlinedText("+${if (doubled) reward * 2 else reward}", style = GameText.Title, fontSize = 30.sp, color = Color(0xFFFFE082))
                }
                if (newWorld != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "New world unlocked: $newWorld!",
                        style = GameText.Label.copy(color = Color(0xFFB9F6CA), fontSize = 16.sp),
                    )
                }
                Spacer(Modifier.height(26.dp))
                if (!doubled) {
                    GameButton(
                        "Claim x2", { onDouble { doubled = true; flyKey++; audio.play(Sfx.CoinBurst) } },
                        Modifier.width(260.dp), color = ButtonColor.GREEN, leading = { GameIcon(Icon.AD, 32.dp) },
                    )
                    Spacer(Modifier.height(12.dp))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.width(300.dp)) {
                    GameButton("Home", onHome, Modifier.weight(1f), color = ButtonColor.BLUE, fontSize = 20.sp, height = 56.dp)
                    GameButton("Next", onNext, Modifier.weight(1.4f), color = ButtonColor.ORANGE, fontSize = 24.sp, height = 56.dp)
                }
            }
            if (visible) CoinFly(flyKey)
        }
    }
    LaunchedEffect(visible) { if (visible) { delay(250); audio.play(Sfx.CoinBurst); flyKey++ } }
}

/** A burst of coins that arcs from the middle of the screen up to the coin counter. */
@Composable
private fun CoinFly(key: Int) {
    if (key == 0) return
    val audio = LocalAudio.current
    var w by remember { mutableFloatStateOf(0f) }
    var h by remember { mutableFloatStateOf(0f) }
    val coinPx = with(LocalDensity.current) { 17.dp.toPx() }
    key(key) {
        Box(Modifier.fillMaxSize().onSizeChanged { w = it.width.toFloat(); h = it.height.toFloat() }) {
            for (i in 0 until 10) {
                val p = remember { Animatable(0f) }
                LaunchedEffect(Unit) {
                    delay(i * 55L)
                    p.animateTo(1f, tween(650, easing = FastOutSlowInEasing))
                    audio.play(Sfx.Coin, 1f + i * 0.03f)
                }
                val t = p.value
                if (t in 0.001f..0.999f) {
                    val startX = 0.5f + ((i % 5) - 2) * 0.06f
                    val x = startX + (0.86f - startX) * t
                    val y = 0.56f + (0.05f - 0.56f) * t - kotlin.math.sin(t * Math.PI).toFloat() * 0.12f
                    Box(
                        Modifier
                            .offset { IntOffset((x * w - coinPx).roundToInt(), (y * h - coinPx).roundToInt()) }
                            .scale(1f - t * 0.4f),
                    ) { GameIcon(Icon.COIN, 34.dp) }
                }
            }
        }
    }
}
