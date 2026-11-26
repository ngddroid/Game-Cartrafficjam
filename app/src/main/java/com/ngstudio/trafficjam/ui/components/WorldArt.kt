package com.ngstudio.trafficjam.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import com.ngstudio.trafficjam.game.render.SceneArt
import com.ngstudio.trafficjam.game.render.SceneTheme

/** A small postcard of a world's scenery, painted with the same art as the game scene. */
@Composable
fun WorldArt(theme: SceneTheme, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier) {
        val w = constraints.maxWidth.coerceIn(32, 1600)
        val h = constraints.maxHeight.coerceIn(32, 1600)
        val bmp = remember(theme, w, h) { SceneArt.postcard(theme, w, h) }
        Image(bmp, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds)
    }
}
