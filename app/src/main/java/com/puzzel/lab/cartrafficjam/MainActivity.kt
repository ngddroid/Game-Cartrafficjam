package com.puzzel.lab.cartrafficjam

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.puzzel.lab.cartrafficjam.ui.components.AdOverlay
import com.puzzel.lab.cartrafficjam.ui.components.LocalAudio
import com.puzzel.lab.cartrafficjam.core.data.TOTAL_LEVELS
import com.puzzel.lab.cartrafficjam.game.model.GameMode
import com.puzzel.lab.cartrafficjam.ui.game.GameScreen
import com.puzzel.lab.cartrafficjam.ui.game.ModeGameScreen
import com.puzzel.lab.cartrafficjam.ui.intro.StudioIntroScreen
import com.puzzel.lab.cartrafficjam.ui.main.MainScreen
import com.puzzel.lab.cartrafficjam.ui.main.Tab
import com.puzzel.lab.cartrafficjam.ui.onboarding.OnboardingScreen
import com.puzzel.lab.cartrafficjam.ui.splash.SplashScreen
import com.puzzel.lab.cartrafficjam.ui.theme.CarTrafficJamTheme
import com.puzzel.lab.cartrafficjam.ui.web.WebScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        hideNavigationBar()
        val container = App.container
        if (BuildConfig.DEBUG) {
            com.puzzel.lab.cartrafficjam.game.modes.DebugFlags.autoplay = intent.getBooleanExtra("debug_autoplay", false)
            com.puzzel.lab.cartrafficjam.game.modes.DebugFlags.perf = intent.getBooleanExtra("debug_perf", false)
        }
        setContent {
            CarTrafficJamTheme {
                CompositionLocalProvider(LocalAudio provides container.audio) {
                    Box(Modifier.fillMaxSize()) {
                        AppNavHost(
                            debugLevel = if (BuildConfig.DEBUG) intent.getIntExtra("debug_level", 0) else 0,
                            debugMode = if (BuildConfig.DEBUG) intent.getStringExtra("debug_mode") else null,
                            // Debug builds can open a main tab directly: --es debug_tab GARAGE
                            debugTab = if (BuildConfig.DEBUG) intent.getStringExtra("debug_tab") else null,
                        )
                        AdOverlay(container.ads)
                    }
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Dialogs, the share sheet or a swipe can bring the bar back; hide it again on return.
        if (hasFocus) hideNavigationBar()
    }

    /** Immersive play: the bottom navigation bar stays hidden and only peeks in on a swipe. */
    private fun hideNavigationBar() {
        WindowCompat.getInsetsController(window, window.decorView).apply {
            systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}

@androidx.compose.runtime.Composable
private fun AppNavHost(debugLevel: Int, debugMode: String? = null, debugTab: String? = null) {
    val nav = rememberNavController()
    // Debug builds can jump straight into a level: adb shell am start ... --ei debug_level N
    androidx.compose.runtime.LaunchedEffect(debugLevel) {
        if (debugLevel > 0) {
            kotlinx.coroutines.delay(300)
            if (debugMode != null) nav.navigate("mode/$debugMode/$debugLevel") else nav.navigate("game/$debugLevel")
        }
    }
    NavHost(
        nav,
        // The NG Studio ident plays first on every launch, then the game's own splash.
        // Debug jumps skip the ident and splash so nothing navigates over the level.
        startDestination = if (debugLevel > 0) "main" else "intro",
        enterTransition = { fadeIn(tween(260)) + scaleIn(tween(260), initialScale = 0.96f) },
        exitTransition = { fadeOut(tween(200)) },
        popEnterTransition = { fadeIn(tween(260)) },
        popExitTransition = { fadeOut(tween(200)) },
    ) {
        composable("intro", exitTransition = { fadeOut(tween(600)) }) {
            StudioIntroScreen {
                nav.navigate("splash") { popUpTo("intro") { inclusive = true } }
            }
        }
        composable(
            "splash",
            // Same 600 ms crossfade out of the ident as the reference game.
            enterTransition = { if (initialState.destination.route == "intro") fadeIn(tween(600)) else fadeIn(tween(260)) },
        ) {
            SplashScreen { onboarded ->
                nav.navigate(if (onboarded) "main" else "onboarding") { popUpTo("splash") { inclusive = true } }
            }
        }
        composable("onboarding") {
            OnboardingScreen {
                nav.navigate("main") { popUpTo("onboarding") { inclusive = true } }
            }
        }
        composable("main") {
            MainScreen(
                initialTab = debugTab?.let { name -> Tab.entries.firstOrNull { it.name == name } } ?: Tab.HOME,
                onPlay = { level -> nav.navigate("game/$level") { launchSingleTop = true } },
                onPlayMode = { mode, level -> nav.navigate("mode/${mode.name}/$level") { launchSingleTop = true } },
                onOpenDoc = { doc -> nav.navigate("web/$doc") },
            )
        }
        composable("game/{level}", arguments = listOf(navArgument("level") { type = NavType.IntType })) { entry ->
            val level = entry.arguments?.getInt("level") ?: 1
            GameScreen(
                levelNumber = level,
                onHome = { nav.popBackStack("main", inclusive = false) },
                onPlayLevel = { next ->
                    nav.navigate("game/$next") { popUpTo("main") }
                },
            )
        }
        composable(
            "mode/{mode}/{level}",
            arguments = listOf(navArgument("mode") { type = NavType.StringType }, navArgument("level") { type = NavType.IntType }),
        ) { entry ->
            val mode = GameMode.of(entry.arguments?.getString("mode"))
            val level = (entry.arguments?.getInt("level") ?: 1).coerceIn(1, TOTAL_LEVELS)
            val onHome = { nav.popBackStack("main", inclusive = false); Unit }
            val onNext = { next: Int -> nav.navigate("mode/${mode.name}/$next") { popUpTo("main") } }
            if (mode == GameMode.LOOP) {
                GameScreen(levelNumber = level, mode = mode, onHome = onHome, onPlayLevel = onNext)
            } else {
                ModeGameScreen(mode = mode, levelNumber = level, onHome = onHome, onPlayLevel = onNext)
            }
        }
        composable("web/{doc}") { entry ->
            WebScreen(entry.arguments?.getString("doc") ?: "privacy") { nav.popBackStack() }
        }
    }
}
