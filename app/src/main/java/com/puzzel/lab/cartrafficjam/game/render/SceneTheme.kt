package com.puzzel.lab.cartrafficjam.game.render

import androidx.compose.ui.graphics.Color
import com.puzzel.lab.cartrafficjam.core.data.collectionOf

enum class Backdrop { STATION, ZOO, FAIR, PIER, DESERT, SNOW, NEON, FARM, JUNGLE, SPACE }

/** Colours and props for one world. Collections cycle through these ten in tiers I–V. */
data class SceneTheme(
    val name: String,
    val backdrop: Backdrop,
    val skyTop: Color,
    val skyBottom: Color,
    val platform: Color,
    val platformDark: Color,
    val slotBand: Color,
    val road: Color,
    val lot: Color,
    val lotEdge: Color,
    val outside: Color,
    val accent: Color,
    val dark: Boolean = false,
)

object Themes {
    val all = listOf(
        SceneTheme(
            "City Station", Backdrop.STATION,
            Color(0xFFBFE6FF), Color(0xFFE3F4FF), Color(0xFFEDEFF2), Color(0xFFC9CED6),
            Color(0xFF9EA3AB), Color(0xFF6E737B), Color(0xFFD4D6CF), Color(0xFFB9BBB2), Color(0xFF8FC25A),
            Color(0xFF2F7CF6),
        ),
        SceneTheme(
            "Sunny Zoo", Backdrop.ZOO,
            Color(0xFFA8E4FF), Color(0xFFE8F8D8), Color(0xFFE9D9A6), Color(0xFFC9B47A),
            Color(0xFF8DBE5A), Color(0xFF5E7C46), Color(0xFFE5E3B8), Color(0xFFC6C28C), Color(0xFF8CB55A),
            Color(0xFF7CB342),
        ),
        SceneTheme(
            "Candy Fair", Backdrop.FAIR,
            Color(0xFFFFD6EC), Color(0xFFFFF0F7), Color(0xFFFFE1EE), Color(0xFFF4B7D0),
            Color(0xFFE79AB8), Color(0xFFB0688A), Color(0xFFF7C9DA), Color(0xFFE6A7BF), Color(0xFFD47DA0),
            Color(0xFFEC407A),
        ),
        SceneTheme(
            "Ocean Pier", Backdrop.PIER,
            Color(0xFF7FD3FF), Color(0xFFBFEAFF), Color(0xFFD8B98A), Color(0xFFB08F5E),
            Color(0xFF6BA3C7), Color(0xFF4A6E8A), Color(0xFFCFE6EE), Color(0xFFA9CBD8), Color(0xFF3F8FC0),
            Color(0xFF00ACC1),
        ),
        SceneTheme(
            "Desert Dunes", Backdrop.DESERT,
            Color(0xFFFFD59A), Color(0xFFFFEFD2), Color(0xFFF1D29B), Color(0xFFD8B270),
            Color(0xFFD6A15E), Color(0xFF9E7448), Color(0xFFF2D7A8), Color(0xFFDDB97F), Color(0xFFC9955A),
            Color(0xFFFF8A1F),
        ),
        SceneTheme(
            "Snowy Peak", Backdrop.SNOW,
            Color(0xFFB6D8F2), Color(0xFFEAF5FF), Color(0xFFF7FBFF), Color(0xFFD5E3F0),
            Color(0xFFA9BFD3), Color(0xFF6F8399), Color(0xFFE9F1F8), Color(0xFFC7D6E5), Color(0xFF9DB4CA),
            Color(0xFF29B6F6),
        ),
        SceneTheme(
            "Neon Night", Backdrop.NEON,
            Color(0xFF1B1446), Color(0xFF3A2475), Color(0xFF3B3366), Color(0xFF29224F),
            Color(0xFF2E2859), Color(0xFF1C1838), Color(0xFF3A3468), Color(0xFF2A2552), Color(0xFF221C46),
            Color(0xFFE040FB), dark = true,
        ),
        SceneTheme(
            "Autumn Farm", Backdrop.FARM,
            Color(0xFFFFD8A8), Color(0xFFFFF1DC), Color(0xFFE8C48F), Color(0xFFC79A5E),
            Color(0xFFC58A4E), Color(0xFF8A5E35), Color(0xFFF0DDB8), Color(0xFFD9BE8E), Color(0xFFB98A4A),
            Color(0xFFE65100),
        ),
        SceneTheme(
            "Jungle Camp", Backdrop.JUNGLE,
            Color(0xFF9FE3C2), Color(0xFFDDF7E6), Color(0xFFCDBB8A), Color(0xFFA8955F),
            Color(0xFF5E9A5A), Color(0xFF3F6B3D), Color(0xFFD5E2B8), Color(0xFFB2C48C), Color(0xFF5E8F45),
            Color(0xFF2E7D32),
        ),
        SceneTheme(
            "Space Port", Backdrop.SPACE,
            Color(0xFF0B1030), Color(0xFF243063), Color(0xFF4A5378), Color(0xFF353D5E),
            Color(0xFF3A4266), Color(0xFF232945), Color(0xFF3F476B), Color(0xFF2F3656), Color(0xFF1B2140),
            Color(0xFF00E5FF), dark = true,
        ),
    )

    private val roman = listOf("I", "II", "III", "IV", "V")

    fun forCollection(c: Int) = all[(c - 1) % all.size]
    fun forLevel(level: Int) = forCollection(collectionOf(level))
    fun collectionName(c: Int) = "${forCollection(c).name} ${roman[((c - 1) / all.size).coerceIn(0, 4)]}"
}
