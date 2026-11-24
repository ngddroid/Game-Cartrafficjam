package com.puzzel.lab.cartrafficjam.core.data

import com.puzzel.lab.cartrafficjam.game.model.BoosterType
import com.puzzel.lab.cartrafficjam.game.model.GameMode

const val TOTAL_LEVELS = 5000
const val LEVELS_PER_COLLECTION = 100
const val TOTAL_COLLECTIONS = TOTAL_LEVELS / LEVELS_PER_COLLECTION

fun collectionOf(level: Int) = (level - 1) / LEVELS_PER_COLLECTION + 1
fun firstLevelOf(collection: Int) = (collection - 1) * LEVELS_PER_COLLECTION + 1

/**
 * Immutable snapshot of everything the player owns. Arrays are copied on every change so
 * StateFlow sees a new value; index 0 is unused so level/collection numbers index directly.
 */
data class PlayerState(
    val coins: Int = 0,
    val boosters: Map<BoosterType, Int> = BoosterType.entries.associateWith { 2 },
    val unlockedLevels: BooleanArray = BooleanArray(TOTAL_LEVELS + 1).also { it[1] = true },
    val stars: ByteArray = ByteArray(TOTAL_LEVELS + 1),
    val unlockedCollections: BooleanArray = BooleanArray(TOTAL_COLLECTIONS + 1).also { it[1] = true },
    val ownedSkins: Set<String> = setOf("classic"),
    val skin: String = "classic",
    val sound: Boolean = true,
    val music: Boolean = true,
    val vibration: Boolean = true,
    val onboardingDone: Boolean = false,
    val dailyStreak: Int = 0,
    val lastDailyDay: Long = -1,
    /** Next level to play in each non-classic mode (1-based). */
    val modeLevels: Map<GameMode, Int> = emptyMap(),
    val loaded: Boolean = false,
) {
    fun modeLevel(mode: GameMode): Int = if (mode == GameMode.CLASSIC) nextLevel else (modeLevels[mode] ?: 1)

    fun isLevelUnlocked(level: Int) = level in 1..TOTAL_LEVELS && unlockedLevels[level]
    fun starsOf(level: Int): Int = if (level in 1..TOTAL_LEVELS) stars[level].toInt() else 0
    fun isCompleted(level: Int) = starsOf(level) > 0
    fun isCollectionUnlocked(c: Int) = c in 1..TOTAL_COLLECTIONS && unlockedCollections[c]
    fun boosterCount(type: BoosterType) = boosters[type] ?: 0

    /** The level the big Play button starts: lowest unlocked level not yet beaten. */
    val nextLevel: Int
        get() {
            for (l in 1..TOTAL_LEVELS) if (unlockedLevels[l] && stars[l].toInt() == 0) return l
            return TOTAL_LEVELS
        }

    val completedCount: Int get() = stars.count { it > 0 }

    fun completedIn(collection: Int): Int {
        val start = firstLevelOf(collection)
        var n = 0
        for (l in start until start + LEVELS_PER_COLLECTION) if (stars[l] > 0) n++
        return n
    }

    fun starsIn(collection: Int): Int {
        val start = firstLevelOf(collection)
        var n = 0
        for (l in start until start + LEVELS_PER_COLLECTION) n += stars[l]
        return n
    }

    override fun equals(other: Any?) = this === other
    override fun hashCode() = System.identityHashCode(this)
}
