package com.puzzel.lab.cartrafficjam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.puzzel.lab.cartrafficjam.R

/**
 * Sunset Orange: the menu theme. Backgrounds, tab headers, the bottom navbar, buttons and the
 * dark outline under titles all come from here, so the whole look can be retuned in one place.
 * (Gameplay boards keep their own per-world palettes.)
 */
object Sunset {
    // Menu background, top to bottom.
    val SkyTop = Color(0xFFFFB347)
    val SkyMid = Color(0xFFFF7A59)
    val SkyBottom = Color(0xFFE94F6A)
    // Tab header banners.
    val HeaderTop = Color(0xFFFF8A3D)
    val HeaderBottom = Color(0xFFF25C54)
    // Bottom navbar: deep plum dock with a gold pill for the selected tab.
    val NavTop = Color(0xFF4A1942)
    val NavBottom = Color(0xFF2E0F2B)
    val NavRim = Color(0xFFFFB347)
    val NavShadow = Color(0xFF1A0718)
    val PillTop = Color(0xFFFFD166)
    val PillBottom = Color(0xFFFF9F1C)
    val PillGlow = Color(0xFFFFC14D)
    // Dark outline for titles and button lips (replaces the old navy).
    val Outline = Color(0xFF3D1033)
    // Round icon buttons and toasts: plum, so white icons stand out on the warm sky.
    val Button = Color(0xFF6B2459)
    val Toast = Color(0xE63D1033)
    // Dialog frames.
    val Frame = Color(0xFFFF7A45)
}

/**
 * Each main tab has its own color family so moving around the game feels varied: the
 * background, the header banner, the navbar pill and the dark outline all take the tab's
 * colors. Home keeps the Sunset look.
 */
@Immutable
data class TabPalette(
    val skyTop: Color,
    val skyMid: Color,
    val skyBottom: Color,
    val headerTop: Color,
    val headerBottom: Color,
    val pillTop: Color,
    val pillBottom: Color,
    /** Dark outline for titles and card lips on this tab. */
    val outline: Color,
)

object TabPalettes {
    /** Home: warm sunset orange. */
    val Home = TabPalette(
        Sunset.SkyTop, Sunset.SkyMid, Sunset.SkyBottom,
        Sunset.HeaderTop, Sunset.HeaderBottom,
        Sunset.PillTop, Sunset.PillBottom, Sunset.Outline,
    )
    /** Level map: fresh lime-to-jade green, like roads through parks. */
    val Levels = TabPalette(
        Color(0xFFB6EB6E), Color(0xFF5FCB78), Color(0xFF1FA27A),
        Color(0xFF3CBF6E), Color(0xFF179A6A),
        Color(0xFFC9F78A), Color(0xFF55C86A), Color(0xFF0B3D2C),
    )
    /** Game modes: lavender-to-indigo purple, matching the arcade game picker design. */
    val Modes = TabPalette(
        Color(0xFF8F6CF3), Color(0xFF5B44CC), Color(0xFF34298A),
        Color(0xFF9B7BFF), Color(0xFF6A4FE0),
        Color(0xFFC7B5FF), Color(0xFF9C82F7), Color(0xFF2A1A6E),
    )
    /** Garage: purple workshop, matching the garage showroom design. */
    val Garage = TabPalette(
        Color(0xFF6E52D6), Color(0xFF5440B8), Color(0xFF3A2C8E),
        Color(0xFF9B7BFF), Color(0xFF6A4FE0),
        Color(0xFFC7B5FF), Color(0xFF9C82F7), Color(0xFF2A1A6E),
    )
    /** Settings: deep indigo-purple night, matching the settings design. */
    val Settings = TabPalette(
        Color(0xFF3F3196), Color(0xFF30267C), Color(0xFF221A5E),
        Color(0xFF9B7BFF), Color(0xFF6A4FE0),
        Color(0xFFC7B5FF), Color(0xFF9C82F7), Color(0xFF2B1C6E),
    )
}

/** The palette of the tab being shown; screens outside the tabs get Home's. */
val LocalTabPalette = staticCompositionLocalOf { TabPalettes.Home }

object GameColors {
    val SkyTop = Sunset.SkyTop
    val SkyBottom = Sunset.SkyBottom
    val SkyDeep = Sunset.HeaderBottom
    val Navy = Sunset.Outline
    val NavyDark = Sunset.NavBottom
    val Cream = Color(0xFFFFF8E7)
    val CreamDark = Color(0xFFF3DFB8)
    val Yellow = Color(0xFFFFC107)
    val YellowLight = Color(0xFFFFE57F)
    val Orange = Color(0xFFFF9800)
    val OrangeDark = Color(0xFFE65100)
    val Green = Color(0xFF7CB342)
    val GreenLight = Color(0xFFAEEA00)
    val GreenDark = Color(0xFF33691E)
    val Red = Color(0xFFE53935)
    val RedDark = Color(0xFF8E1B1B)
    val Pink = Color(0xFFEC407A)
    val Purple = Color(0xFF7E57C2)
    val PurpleDark = Color(0xFF4527A0)
    val Blue = Color(0xFF1E88E5)
    val BlueDark = Color(0xFF0D47A1)
    val Brown = Color(0xFF6D4C41)
    val TextDark = Color(0xFF3E2723)
    val Shade = Color(0x99000000)
}

object Brushes {
    val Sky = Brush.verticalGradient(listOf(Sunset.SkyTop, Sunset.SkyMid, Sunset.SkyBottom))
    val Panel = Brush.verticalGradient(listOf(Color(0xFFFFFDF5), GameColors.Cream, Color(0xFFFFEFC9)))
    val Night = Brush.verticalGradient(listOf(Color(0xFF1A237E), Color(0xFF0D1B4A)))
}

@OptIn(ExperimentalTextApi::class)
private fun fredoka(weight: Int) = Font(
    R.font.fredoka,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight)),
)

val TitleFont = FontFamily(Font(R.font.lilita_one))
val BodyFont = FontFamily(fredoka(400), fredoka(500), fredoka(600), fredoka(700))

object GameText {
    private val outline = Shadow(Color(0x66000000), Offset(0f, 4f), 2f)
    val Display = TextStyle(fontFamily = TitleFont, fontSize = 44.sp, color = Color.White, shadow = outline)
    val Title = TextStyle(fontFamily = TitleFont, fontSize = 30.sp, color = Color.White, shadow = outline)
    val Button = TextStyle(fontFamily = TitleFont, fontSize = 22.sp, color = Color.White, shadow = Shadow(Color(0x55000000), Offset(0f, 3f), 1f))
    val Label = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = GameColors.TextDark)
    val Body = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = GameColors.TextDark)
    val Small = TextStyle(fontFamily = BodyFont, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = GameColors.TextDark)
}

@Composable
fun CarTrafficJamTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = GameColors.Orange,
            secondary = Sunset.NavTop,
            background = GameColors.SkyBottom,
            surface = GameColors.Cream,
        ),
        content = content,
    )
}
