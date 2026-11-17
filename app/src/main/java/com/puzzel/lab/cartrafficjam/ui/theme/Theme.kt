package com.puzzel.lab.cartrafficjam.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
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

object GameColors {
    val SkyTop = Color(0xFF4FC3F7)
    val SkyBottom = Color(0xFF1E88E5)
    val SkyDeep = Color(0xFF1565C0)
    val Navy = Color(0xFF1A3A6B)
    val NavyDark = Color(0xFF0F2547)
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
    val Sky = Brush.verticalGradient(listOf(GameColors.SkyTop, GameColors.SkyBottom))
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
            secondary = GameColors.Blue,
            background = GameColors.SkyBottom,
            surface = GameColors.Cream,
        ),
        content = content,
    )
}
