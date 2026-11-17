package com.puzzel.lab.cartrafficjam.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.puzzel.lab.cartrafficjam.core.data.LEVELS_PER_COLLECTION
import com.puzzel.lab.cartrafficjam.core.data.PlayerState
import com.puzzel.lab.cartrafficjam.core.data.TOTAL_COLLECTIONS
import com.puzzel.lab.cartrafficjam.game.render.Themes
import com.puzzel.lab.cartrafficjam.ui.components.GameIcon
import com.puzzel.lab.cartrafficjam.ui.components.Icon
import com.puzzel.lab.cartrafficjam.ui.components.OutlinedText
import com.puzzel.lab.cartrafficjam.ui.components.WorldArt
import com.puzzel.lab.cartrafficjam.ui.theme.GameText
import com.puzzel.lab.cartrafficjam.ui.theme.TitleFont

@Composable
fun CollectionsTab(player: PlayerState, onOpen: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TabHeader("Worlds", "${(1..TOTAL_COLLECTIONS).count { player.isCollectionUnlocked(it) }} of $TOTAL_COLLECTIONS open")
        LazyVerticalGrid(
            GridCells.Fixed(2),
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items((1..TOTAL_COLLECTIONS).toList(), key = { it }) { c -> WorldCard(c, player, onOpen) }
        }
    }
}

@Composable
fun TabHeader(title: String, subtitle: String? = null) {
    Box(
        Modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(Color(0xFF1C5BD0), Color(0xFF2F7CF6))))
            .statusBarsPadding()
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            OutlinedText(title, style = GameText.Title, fontSize = 28.sp, strokeWidth = 7f, outline = Color(0xFF0D2C6B))
            if (subtitle != null) Text(subtitle, style = GameText.Small.copy(color = Color.White, fontSize = 13.sp))
        }
    }
}

@Composable
private fun WorldCard(c: Int, player: PlayerState, onOpen: (Int) -> Unit) {
    val theme = Themes.forCollection(c)
    val unlocked = player.isCollectionUnlocked(c)
    val done = player.completedIn(c)
    Column(
        Modifier
            .drawBehind {
                drawRoundRect(Color(0xFF0D2C6B), Offset(0f, 5.dp.toPx()), size, CornerRadius(20.dp.toPx()))
                drawRoundRect(Color.White, Offset.Zero, size, CornerRadius(20.dp.toPx()))
            }
            .padding(4.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onOpen(c) },
    ) {
        Box(Modifier.fillMaxWidth().height(112.dp)) {
            WorldArt(theme, Modifier.fillMaxSize())
            Text(
                "$c",
                style = GameText.Label.copy(color = Color.White, fontFamily = TitleFont, fontSize = 15.sp),
                modifier = Modifier.padding(6.dp).background(Color(0xAA0D2C6B), CircleShape).padding(horizontal = 9.dp, vertical = 1.dp),
            )
            if (!unlocked) {
                Box(Modifier.fillMaxSize().background(Color(0x99101828)), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        GameIcon(Icon.LOCK, 44.dp)
                        Row(
                            Modifier.background(Color(0xFF5FC12F), RoundedCornerShape(10.dp)).padding(horizontal = 8.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            GameIcon(Icon.AD, 18.dp)
                            Text(" Unlock", style = GameText.Small.copy(color = Color.White, fontFamily = TitleFont))
                        }
                    }
                }
            }
        }
        Column(Modifier.fillMaxWidth().background(Color(0xFFFFF8E7)).padding(horizontal = 10.dp, vertical = 8.dp)) {
            Text(Themes.collectionName(c), style = GameText.Label.copy(fontFamily = TitleFont, fontSize = 16.sp, color = Color(0xFF3E2723)), maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Box(Modifier.fillMaxWidth().height(10.dp).background(Color(0xFFE6D3AE), CircleShape)) {
                Box(
                    Modifier
                        .fillMaxWidth(done / LEVELS_PER_COLLECTION.toFloat())
                        .height(10.dp)
                        .background(Brush.horizontalGradient(listOf(Color(0xFF9CCC65), Color(0xFF43A047))), CircleShape),
                )
            }
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$done/$LEVELS_PER_COLLECTION", style = GameText.Small.copy(color = Color(0xFF6D4C41)))
                Spacer(Modifier.weight(1f))
                GameIcon(Icon.STAR, 14.dp)
                Text(" ${player.starsIn(c)}", style = GameText.Small.copy(color = Color(0xFF6D4C41)))
            }
        }
    }
}
