package com.ngstudio.trafficjam.core.analytics

import android.app.Application
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.ngstudio.trafficjam.BuildConfig
import com.ngstudio.trafficjam.game.model.GameMode

/**
 * One place for everything the game reports to Firebase.
 *
 * Analytics gets Firebase's standard game events (level_start, level_end, earn/spend virtual
 * currency, unlock_achievement, tutorial_complete, screen_view) plus a few custom ones, so the
 * built-in game reports in the console work without extra setup. Crashlytics gets breadcrumbs
 * and custom keys for the current screen, mode and level, so every crash report shows where the
 * player was. No personal data is sent.
 */
object Analytics {

    private var fa: FirebaseAnalytics? = null
    private val crash: FirebaseCrashlytics? get() = runCatching { FirebaseCrashlytics.getInstance() }.getOrNull()

    fun init(app: Application) {
        fa = FirebaseAnalytics.getInstance(app)
        crash?.apply {
            setCustomKey("build_type", if (BuildConfig.DEBUG) "debug" else "release")
            setCustomKey("version", BuildConfig.VERSION_NAME)
        }
    }

    // --- Screens ----------------------------------------------------------------------------

    private var lastScreen: String? = null

    fun screen(name: String) {
        if (name == lastScreen) return
        lastScreen = name
        log(FirebaseAnalytics.Event.SCREEN_VIEW) {
            putString(FirebaseAnalytics.Param.SCREEN_NAME, name)
            putString(FirebaseAnalytics.Param.SCREEN_CLASS, name)
        }
        crash?.setCustomKey("screen", name)
        breadcrumb("screen $name")
    }

    // --- Levels -----------------------------------------------------------------------------

    private fun levelName(mode: GameMode, level: Int) = "${mode.name.lowercase()}_$level"

    fun levelStart(mode: GameMode, level: Int, difficulty: String) {
        log(FirebaseAnalytics.Event.LEVEL_START) {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, levelName(mode, level))
            putLong(FirebaseAnalytics.Param.LEVEL, level.toLong())
            putString("mode", mode.name.lowercase())
            putString("difficulty", difficulty)
        }
        crash?.apply {
            setCustomKey("mode", mode.name)
            setCustomKey("level", level)
            setCustomKey("difficulty", difficulty)
        }
        breadcrumb("level start ${levelName(mode, level)} ($difficulty)")
    }

    fun levelWin(mode: GameMode, level: Int, stars: Int, coins: Int, seconds: Int) {
        log(FirebaseAnalytics.Event.LEVEL_END) {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, levelName(mode, level))
            putLong(FirebaseAnalytics.Param.LEVEL, level.toLong())
            putString(FirebaseAnalytics.Param.SUCCESS, "true")
            putString("mode", mode.name.lowercase())
            putLong("stars", stars.toLong())
            putLong("seconds", seconds.toLong())
        }
        earnCoins("level_win", coins)
        breadcrumb("level win ${levelName(mode, level)} stars=$stars")
    }

    /** The player gave up after getting stuck (the fail screen opened). */
    fun levelFail(mode: GameMode, level: Int, seconds: Int) {
        log(FirebaseAnalytics.Event.LEVEL_END) {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, levelName(mode, level))
            putLong(FirebaseAnalytics.Param.LEVEL, level.toLong())
            putString(FirebaseAnalytics.Param.SUCCESS, "false")
            putString("mode", mode.name.lowercase())
            putLong("seconds", seconds.toLong())
        }
        breadcrumb("level fail ${levelName(mode, level)}")
    }

    /** Left the level before winning or failing it (home, back or retry). */
    fun levelQuit(mode: GameMode, level: Int, seconds: Int) {
        log("level_quit") {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, levelName(mode, level))
            putString("mode", mode.name.lowercase())
            putLong("seconds", seconds.toLong())
        }
        breadcrumb("level quit ${levelName(mode, level)}")
    }

    fun levelStuck(mode: GameMode, level: Int) {
        log("level_stuck") {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, levelName(mode, level))
            putString("mode", mode.name.lowercase())
        }
    }

    fun levelSkip(mode: GameMode, level: Int) {
        log("level_skip") {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, levelName(mode, level))
            putString("mode", mode.name.lowercase())
        }
    }

    /** [method] is "coins" or "ad". */
    fun revive(mode: GameMode, level: Int, method: String) {
        log("revive") {
            putString(FirebaseAnalytics.Param.LEVEL_NAME, levelName(mode, level))
            putString("mode", mode.name.lowercase())
            putString("method", method)
        }
    }

    /** A new world (collection of Classic levels) opened. */
    fun worldUnlocked(name: String) {
        log(FirebaseAnalytics.Event.UNLOCK_ACHIEVEMENT) {
            putString(FirebaseAnalytics.Param.ACHIEVEMENT_ID, "world_$name")
        }
    }

    // --- Economy ----------------------------------------------------------------------------

    fun earnCoins(source: String, amount: Int) {
        if (amount <= 0) return
        log(FirebaseAnalytics.Event.EARN_VIRTUAL_CURRENCY) {
            putString(FirebaseAnalytics.Param.VIRTUAL_CURRENCY_NAME, "coins")
            putLong(FirebaseAnalytics.Param.VALUE, amount.toLong())
            putString("source", source)
        }
    }

    fun spendCoins(item: String, amount: Int) {
        if (amount <= 0) return
        log(FirebaseAnalytics.Event.SPEND_VIRTUAL_CURRENCY) {
            putString(FirebaseAnalytics.Param.VIRTUAL_CURRENCY_NAME, "coins")
            putLong(FirebaseAnalytics.Param.VALUE, amount.toLong())
            putString(FirebaseAnalytics.Param.ITEM_NAME, item)
        }
    }

    fun dailyReward(day: Int, coins: Int, doubled: Boolean) {
        log("daily_reward") {
            putLong("day", day.toLong())
            putLong("coins", coins.toLong())
            putString("doubled", doubled.toString())
        }
        earnCoins(if (doubled) "daily_x2" else "daily", coins)
    }

    /** [via] is "coins" or "ad". */
    fun skinUnlocked(id: String, via: String) {
        log("skin_unlock") {
            putString(FirebaseAnalytics.Param.ITEM_ID, id)
            putString("via", via)
        }
    }

    // --- Ads --------------------------------------------------------------------------------

    fun rewardedAd(placement: String, rewarded: Boolean) {
        log(if (rewarded) "rewarded_ad_complete" else "rewarded_ad_skip") {
            putString("placement", placement)
        }
        breadcrumb("rewarded ad $placement rewarded=$rewarded")
    }

    // --- Notifications and updates ----------------------------------------------------------

    /** [source] is "push", "reminder_morning" or "reminder_evening". */
    fun notificationOpen(source: String) {
        log("notification_open") { putString("source", source) }
        breadcrumb("opened from notification $source")
    }

    fun notificationPermission(granted: Boolean) {
        log("notification_permission") { putString("granted", granted.toString()) }
    }

    fun updateOffered(versionCode: Int, type: String) {
        log("app_update_offered") {
            putLong("version_code", versionCode.toLong())
            putString("type", type)
        }
    }

    fun updateFlowResult(accepted: Boolean) {
        log("app_update_response") { putString("accepted", accepted.toString()) }
    }

    // --- Misc -------------------------------------------------------------------------------

    fun tutorialComplete() = log(FirebaseAnalytics.Event.TUTORIAL_COMPLETE) {}

    fun setting(name: String, on: Boolean) {
        log("setting_change") {
            putString("setting", name)
            putString("value", if (on) "on" else "off")
        }
    }

    fun progressReset() = log("progress_reset") {}

    /** Highest levels reached, as user properties so audiences can be split by progress. */
    private val sentProps = HashMap<String, String>()

    fun progress(classicLevel: Int, modeLevels: Map<GameMode, Int>) {
        val a = fa ?: return
        fun put(name: String, level: Int) {
            val v = bucket(level)
            if (sentProps[name] == v) return
            sentProps[name] = v
            a.setUserProperty(name, v)
        }
        put("classic_level", classicLevel)
        for ((m, l) in modeLevels) put("${m.name.lowercase()}_level", l)
    }

    /** User property values are limited to 36 characters; buckets also keep them readable. */
    private fun bucket(level: Int): String = when {
        level <= 10 -> level.toString()
        level <= 50 -> "${(level - 1) / 10 * 10 + 1}-${(level - 1) / 10 * 10 + 10}"
        level <= 500 -> "${(level - 1) / 50 * 50 + 1}-${(level - 1) / 50 * 50 + 50}"
        else -> "${(level - 1) / 500 * 500 + 1}-${(level - 1) / 500 * 500 + 500}"
    }

    // --- Plumbing ---------------------------------------------------------------------------

    private inline fun log(event: String, params: Bundle.() -> Unit) {
        val a = fa ?: return
        runCatching { a.logEvent(event, Bundle().apply(params)) }
    }

    fun breadcrumb(message: String) {
        crash?.log(message)
    }

    /** Reports a caught exception that the game recovered from. */
    fun nonFatal(t: Throwable) {
        crash?.recordException(t)
    }
}
