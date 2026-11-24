package com.puzzel.lab.cartrafficjam.core.data

import android.content.Context
import android.util.Base64
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.puzzel.lab.cartrafficjam.game.model.BoosterType
import com.puzzel.lab.cartrafficjam.game.model.GameMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.TimeUnit

private val Context.store by preferencesDataStore(name = "player")

/**
 * Holds the player's progress in memory for instant UI reads and writes every change through
 * to DataStore in the background.
 */
class ProgressRepository(private val context: Context, private val scope: CoroutineScope) {

    private val _state = MutableStateFlow(PlayerState())
    val state: StateFlow<PlayerState> = _state.asStateFlow()
    val now: PlayerState get() = _state.value

    private val saveLock = Mutex()

    private object K {
        val coins = intPreferencesKey("coins")
        val levels = stringPreferencesKey("levels")
        val stars = stringPreferencesKey("stars")
        val collections = stringPreferencesKey("collections")
        val skins = stringSetPreferencesKey("skins")
        val skin = stringPreferencesKey("skin")
        val sound = booleanPreferencesKey("sound")
        val music = booleanPreferencesKey("music")
        val vibration = booleanPreferencesKey("vibration")
        val onboarding = booleanPreferencesKey("onboarding")
        val streak = intPreferencesKey("streak")
        val lastDaily = longPreferencesKey("last_daily")
        fun booster(t: BoosterType) = intPreferencesKey("booster_${t.name}")
        fun mode(m: GameMode) = intPreferencesKey("mode_level_${m.name}")
    }

    suspend fun load() {
        val p = context.store.data.first()
        val d = PlayerState()
        _state.value = PlayerState(
            coins = p[K.coins] ?: d.coins,
            boosters = BoosterType.entries.associateWith { p[K.booster(it)] ?: 2 },
            unlockedLevels = p[K.levels]?.let { decodeBits(it, TOTAL_LEVELS + 1) } ?: d.unlockedLevels,
            stars = p[K.stars]?.let { decodeBytes(it, TOTAL_LEVELS + 1) } ?: d.stars,
            unlockedCollections = p[K.collections]?.let { decodeBits(it, TOTAL_COLLECTIONS + 1) } ?: d.unlockedCollections,
            ownedSkins = p[K.skins] ?: d.ownedSkins,
            skin = p[K.skin] ?: d.skin,
            sound = p[K.sound] ?: true,
            music = p[K.music] ?: true,
            vibration = p[K.vibration] ?: true,
            onboardingDone = p[K.onboarding] ?: false,
            dailyStreak = p[K.streak] ?: 0,
            lastDailyDay = p[K.lastDaily] ?: -1,
            modeLevels = GameMode.entries.filter { it != GameMode.CLASSIC }.associateWith { p[K.mode(it)] ?: 1 },
            loaded = true,
        ).also {
            // Level 1 and collection 1 are always open.
            it.unlockedLevels[1] = true
            it.unlockedCollections[1] = true
        }
    }

    private fun mutate(block: (PlayerState) -> PlayerState) {
        _state.update(block)
        persist()
    }

    private fun persist() {
        val s = _state.value
        scope.launch {
            saveLock.withLock {
                context.store.edit { p -> write(p, s) }
            }
        }
    }

    private fun write(p: androidx.datastore.preferences.core.MutablePreferences, s: PlayerState) {
        p[K.coins] = s.coins
        BoosterType.entries.forEach { p[K.booster(it)] = s.boosterCount(it) }
        p[K.levels] = encodeBits(s.unlockedLevels)
        p[K.stars] = Base64.encodeToString(s.stars, Base64.NO_WRAP)
        p[K.collections] = encodeBits(s.unlockedCollections)
        p[K.skins] = s.ownedSkins
        p[K.skin] = s.skin
        p[K.sound] = s.sound
        p[K.music] = s.music
        p[K.vibration] = s.vibration
        p[K.onboarding] = s.onboardingDone
        p[K.streak] = s.dailyStreak
        p[K.lastDaily] = s.lastDailyDay
        GameMode.entries.filter { it != GameMode.CLASSIC }.forEach { p[K.mode(it)] = s.modeLevel(it) }
    }

    // --- Progress ---------------------------------------------------------------------------

    /** Records a win and opens what comes after it. Returns true if a new collection opened. */
    fun completeLevel(level: Int, stars: Int, coins: Int): Boolean {
        var openedCollection = false
        mutate { s ->
            val st = s.stars.copyOf().also { if (stars > it[level]) it[level] = stars.toByte() }
            val lv = s.unlockedLevels.copyOf()
            val col = s.unlockedCollections.copyOf()
            if (level < TOTAL_LEVELS) {
                lv[level + 1] = true
                val nextC = collectionOf(level + 1)
                if (!col[nextC]) {
                    col[nextC] = true
                    openedCollection = true
                }
            }
            s.copy(stars = st, unlockedLevels = lv, unlockedCollections = col, coins = s.coins + coins)
        }
        return openedCollection
    }

    /** Records a win in Crowd Loop, Pixel Crowd or Sand Sweep and moves that mode on one level. */
    fun completeModeLevel(mode: GameMode, level: Int, coins: Int) = mutate { s ->
        val next = maxOf(s.modeLevel(mode), level + 1)
        s.copy(modeLevels = s.modeLevels + (mode to next), coins = s.coins + coins)
    }

    /** Ad reward: opens one level and the collection it lives in. */
    fun unlockLevel(level: Int) = mutate { s ->
        val lv = s.unlockedLevels.copyOf().also { it[level] = true }
        val col = s.unlockedCollections.copyOf().also { it[collectionOf(level)] = true }
        s.copy(unlockedLevels = lv, unlockedCollections = col)
    }

    /** Ad reward: opens a collection and its first level. */
    fun unlockCollection(c: Int) = mutate { s ->
        val col = s.unlockedCollections.copyOf().also { it[c] = true }
        val lv = s.unlockedLevels.copyOf().also { it[firstLevelOf(c)] = true }
        s.copy(unlockedLevels = lv, unlockedCollections = col)
    }

    fun addCoins(n: Int) = mutate { it.copy(coins = (it.coins + n).coerceAtLeast(0)) }

    fun spendCoins(n: Int): Boolean {
        if (now.coins < n) return false
        addCoins(-n)
        return true
    }

    fun addBooster(t: BoosterType, n: Int) = mutate { s ->
        s.copy(boosters = s.boosters + (t to (s.boosterCount(t) + n).coerceAtLeast(0)))
    }

    fun buySkin(id: String) = mutate { it.copy(ownedSkins = it.ownedSkins + id, skin = id) }
    fun selectSkin(id: String) = mutate { it.copy(skin = id) }

    // --- Settings ---------------------------------------------------------------------------

    fun setSound(on: Boolean) = mutate { it.copy(sound = on) }
    fun setMusic(on: Boolean) = mutate { it.copy(music = on) }
    fun setVibration(on: Boolean) = mutate { it.copy(vibration = on) }
    fun finishOnboarding() = mutate { it.copy(onboardingDone = true) }

    fun resetProgress() = mutate { s ->
        PlayerState(sound = s.sound, music = s.music, vibration = s.vibration, onboardingDone = true, loaded = true)
    }

    // --- Daily reward -----------------------------------------------------------------------

    private fun today() = TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis())

    val dailyAvailable: Boolean get() = now.lastDailyDay != today()

    /** Day index 0..6 the next claim will pay out for. */
    val dailyIndex: Int
        get() {
            val s = now
            val consecutive = s.lastDailyDay == today() - 1 || s.lastDailyDay == today()
            return if (consecutive) s.dailyStreak % 7 else 0
        }

    fun claimDaily(coins: Int) = mutate { s ->
        val consecutive = s.lastDailyDay == today() - 1
        s.copy(
            coins = s.coins + coins,
            dailyStreak = if (consecutive) s.dailyStreak + 1 else 1,
            lastDailyDay = today(),
        )
    }

    // --- Encoding ---------------------------------------------------------------------------

    private fun encodeBits(b: BooleanArray): String {
        val bytes = ByteArray((b.size + 7) / 8)
        for (i in b.indices) if (b[i]) bytes[i / 8] = (bytes[i / 8].toInt() or (1 shl (i % 8))).toByte()
        return Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    private fun decodeBits(s: String, size: Int): BooleanArray {
        val bytes = Base64.decode(s, Base64.NO_WRAP)
        return BooleanArray(size) { i -> i / 8 < bytes.size && (bytes[i / 8].toInt() shr (i % 8)) and 1 == 1 }
    }

    private fun decodeBytes(s: String, size: Int): ByteArray {
        val bytes = Base64.decode(s, Base64.NO_WRAP)
        return ByteArray(size) { i -> if (i < bytes.size) bytes[i] else 0 }
    }
}
