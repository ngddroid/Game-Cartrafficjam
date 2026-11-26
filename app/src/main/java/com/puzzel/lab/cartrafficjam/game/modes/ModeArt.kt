package com.puzzel.lab.cartrafficjam.game.modes

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import com.puzzel.lab.cartrafficjam.game.model.VehicleType
import com.puzzel.lab.cartrafficjam.game.render.VehicleArt
import com.puzzel.lab.cartrafficjam.game.render.jamColor
import com.puzzel.lab.cartrafficjam.game.render.renderBitmap
import com.puzzel.lab.cartrafficjam.game.render.shade
import com.puzzel.lab.cartrafficjam.game.render.tint

/** Sprites shared by the mode engines. */
object ModeArt {

    /**
     * Draws a 3D vehicle sprite centred on [pos] facing [heading] degrees, shadow included.
     * Returns the roof height in pixels so callers can place labels on top.
     *
     * Pass [live] while the vehicle is turning: the slice stack is then drawn straight to the
     * screen instead of building (and allocating) a new cached sprite for every 2° of heading,
     * which is what made driving and merging stutter.
     */
    fun DrawScope.vehicleSprite(
        art: VehicleArt,
        type: VehicleType,
        color: Int,
        pos: Offset,
        heading: Float,
        scale: Float = 1f,
        squashX: Float = 1f,
        live: Boolean = false,
    ): Float {
        val unit = art.unit
        val sh = art.shadow(type)
        val sc = pos + Offset(unit * 0.22f, unit * 0.36f) * scale
        withTransform({
            translate(sc.x, sc.y)
            rotate(heading, Offset.Zero)
            scale(scale * squashX, scale, Offset.Zero)
        }) { drawImage(sh, Offset(-sh.width / 2f, -sh.height / 2f)) }
        if (live) {
            withTransform({ scale(scale * squashX, scale, pos) }) { art.drawLive(this, type, color, heading, pos) }
            return art.roofZ(type) * scale
        }
        val comp = art.composite(type, color, false, heading)
        withTransform({
            translate(pos.x, pos.y)
            rotate(art.residual(heading), Offset.Zero)
            scale(scale * squashX, scale, Offset.Zero)
        }) { drawImage(comp.bitmap, Offset(-comp.groundX, -comp.groundY)) }
        return comp.roofZ * scale
    }

    /**
     * A rider as a chunky block, seen from above and in front: a darker front face hangs below
     * the top face, so a whole picture of them reads as a 3D mosaic. The bitmap is [px] wide and
     * about 1.24 × [px] tall; the top face fills the first [px] rows.
     */
    fun riderBlock(color: Int, px: Int): ImageBitmap = renderBitmap(px, (px * 1.24f).toInt()) {
        val s = px.toFloat()
        val c = jamColor(color)
        val r = CornerRadius(s * 0.2f)
        val inset = s * 0.05f
        // front face
        drawRoundRect(c.shade(0.42f), Offset(inset, s * 0.3f), Size(s - inset * 2, s * 0.92f), r)
        drawRoundRect(
            Brush.verticalGradient(listOf(c.shade(0.72f), c.shade(0.55f)), s * 0.5f, s * 1.2f),
            Offset(inset, s * 0.3f), Size(s - inset * 2, s * 0.88f), r,
        )
        // top face
        drawRoundRect(
            Brush.linearGradient(listOf(c.tint(0.32f), c, c.shade(0.88f)), Offset(0f, 0f), Offset(s, s)),
            Offset(inset, inset), Size(s - inset * 2, s - inset * 2), r,
        )
        drawRoundRect(Color.White.copy(alpha = 0.28f), Offset(s * 0.16f, s * 0.12f), Size(s * 0.5f, s * 0.14f), CornerRadius(s * 0.07f))
        // face, on the top so every rider in the mosaic shows one
        val eyeY = s * 0.58f
        drawOval(Color(0xFF1F2430), Offset(s * 0.28f, eyeY - s * 0.07f), Size(s * 0.11f, s * 0.15f))
        drawOval(Color(0xFF1F2430), Offset(s * 0.61f, eyeY - s * 0.07f), Size(s * 0.11f, s * 0.15f))
        drawCircle(Color.White, s * 0.025f, Offset(s * 0.32f, eyeY - s * 0.03f))
        drawCircle(Color.White, s * 0.025f, Offset(s * 0.65f, eyeY - s * 0.03f))
        drawCircle(Color(0x66FF5A7A), s * 0.05f, Offset(s * 0.2f, eyeY + s * 0.08f))
        drawCircle(Color(0x66FF5A7A), s * 0.05f, Offset(s * 0.8f, eyeY + s * 0.08f))
    }

    /** A rider seen from above: shoulders, a round head and two dot eyes. */
    fun riderTile(color: Int, px: Int): ImageBitmap = renderBitmap(px, px) {
        val s = px.toFloat()
        val c = jamColor(color)
        val r = CornerRadius(s * 0.24f)
        drawRoundRect(c.shade(0.55f), Offset(s * 0.07f, s * 0.2f), Size(s * 0.86f, s * 0.76f), r)
        drawRoundRect(
            Brush.verticalGradient(listOf(c.tint(0.18f), c.shade(0.86f)), s * 0.12f, s * 0.86f),
            Offset(s * 0.07f, s * 0.12f), Size(s * 0.86f, s * 0.74f), r,
        )
        val hc = Offset(s * 0.5f, s * 0.4f)
        val hr = s * 0.25f
        drawCircle(c.shade(0.6f), hr + s * 0.02f, hc + Offset(0f, s * 0.03f))
        drawCircle(Brush.radialGradient(listOf(c.tint(0.5f), c), hc - Offset(hr * 0.4f, hr * 0.5f), hr * 1.6f), hr, hc)
        drawOval(Color(0xFF1F2430), Offset(hc.x - hr * 0.5f, hc.y - hr * 0.05f), Size(hr * 0.24f, hr * 0.34f))
        drawOval(Color(0xFF1F2430), Offset(hc.x + hr * 0.26f, hc.y - hr * 0.05f), Size(hr * 0.24f, hr * 0.34f))
        drawCircle(Color.White.copy(alpha = 0.7f), hr * 0.22f, hc - Offset(hr * 0.42f, hr * 0.5f))
    }
}
