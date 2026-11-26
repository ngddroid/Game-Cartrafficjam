package com.ngstudio.trafficjam.game.modes

import android.graphics.Bitmap
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.toArgb
import com.ngstudio.trafficjam.game.model.VehicleType
import com.ngstudio.trafficjam.game.render.jamColor
import com.ngstudio.trafficjam.game.render.renderBitmap
import com.ngstudio.trafficjam.game.render.shade
import com.ngstudio.trafficjam.game.render.tint
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

/**
 * The Sand Sweep belt vans, seen from the side and a little above, facing right. Each has an
 * open cargo bay on top: empty and dark at first, then a heap of sand grows in it until the bay
 * is full. The body is rendered once per type and color; only the sand is drawn each frame.
 */
internal class SandVan(private val bodyWidth: Float) {

    class Look(
        val body: ImageBitmap,
        /** Where the van's center sits inside [body]. */
        val origin: Offset,
        val length: Float,
        val height: Float,
        /** The open bay, relative to the van's center. */
        val bay: Rect,
        val sand: ShaderBrush,
        val sandDark: Color,
        val sandLight: Color,
    )

    private val cache = HashMap<Int, Look>()

    fun look(type: VehicleType, color: Int, length: Float): Look =
        cache.getOrPut(type.ordinal * 16 + color) { build(type, color, length) }

    /** Point in the middle of the bay mouth where poured sand lands, relative to the center. */
    fun mouth(l: Look) = Offset(l.bay.center.x, l.bay.top + l.bay.height * 0.35f)

    /**
     * Draws the van centered on [pos], tilted by [tilt] degrees, with the bay [fill] full (0..1).
     */
    fun DrawScope.draw(l: Look, pos: Offset, scale: Float, tilt: Float, fill: Float) {
        withTransform({
            translate(pos.x, pos.y)
            if (tilt != 0f) rotate(tilt, Offset.Zero)
            if (scale != 1f) scale(scale, scale, Offset.Zero)
        }) {
            drawImage(l.body, -l.origin)
            if (fill > 0f) {
                val b = l.bay
                clipRect(b.left, b.top, b.right, b.bottom) {
                    // The heap grows from the middle of the bay and spills to its walls.
                    val f = fill.coerceIn(0f, 1f)
                    val w = b.width * (0.34f + 1.05f * f)
                    val h = b.height * (0.55f + 1.1f * f)
                    val c = Offset(b.center.x, b.bottom - h * 0.42f + b.height * 0.12f)
                    drawOval(l.sandDark, Offset(c.x - w / 2, c.y - h / 2 + b.height * 0.08f), Size(w, h))
                    translate(b.left, b.top) {
                        drawOval(l.sand, Offset(c.x - w / 2 - b.left, c.y - h / 2 - b.top), Size(w, h))
                    }
                    drawOval(
                        l.sandLight.copy(alpha = 0.55f),
                        Offset(c.x - w * 0.28f, c.y - h * 0.36f), Size(w * 0.42f, h * 0.3f),
                    )
                }
            }
        }
    }

    private fun build(type: VehicleType, color: Int, length: Float): Look {
        val l = length
        // Total height, wheels included: cars are nearly square, long vans about 2.3 : 1.
        val t = min(l * 0.66f, bodyWidth * 1.05f)
        val h = t
        val pad = ceil(t * 0.12f)
        val top = -t * 0.74f
        val bottom = t * 0.52f
        val origin = Offset(l / 2 + pad, -top + pad)
        val bw = ceil(l + pad * 2).toInt()
        val bh = ceil(bottom - top + pad * 2).toInt()

        val c = jamColor(color)
        val edge = c.shade(0.62f)
        val r = t * 0.135f
        val wheelCy = t * 0.33f
        val wheelX = l / 2 - max(l * 0.19f, r * 1.7f)
        val nose = max(l * 0.07f, t * 0.12f)
        val cab = max(l * 0.24f, t * 0.42f)
        // One rounded toy body: the side face below, the lighter top face above it.
        val body = Rect(-l / 2, -t * 0.5f, l / 2, t * 0.36f)
        val roof = Rect(-l / 2 + t * 0.03f, -t * 0.55f, l / 2 - nose, -t * 0.06f)
        val bay = Rect(roof.left + t * 0.07f, roof.top + t * 0.07f, roof.right - cab, roof.bottom - t * 0.07f)

        val img = renderBitmap(bw, bh) {
            translate(origin.x, origin.y) {
                // Soft shadow on the road.
                drawOval(Color(0x38000000), Offset(-l * 0.52f, t * 0.3f), Size(l * 1.04f, t * 0.2f))
                // Tires peeking out under the body.
                for (x in floatArrayOf(-wheelX, wheelX)) {
                    drawCircle(Color(0xFF34373F), r, Offset(x, wheelCy))
                }
                // Side face, rounded all round, a touch darker toward the sill.
                val br = CornerRadius(t * 0.2f)
                drawRoundRect(edge, body.topLeft + Offset(0f, t * 0.02f), body.size, br)
                drawRoundRect(
                    Brush.verticalGradient(listOf(c, c.shade(0.86f)), body.top, body.bottom),
                    body.topLeft, Size(body.width, body.height - t * 0.02f), br,
                )
                // Wheel arches and tires with light hubs.
                for (x in floatArrayOf(-wheelX, wheelX)) {
                    drawCircle(c.shade(0.6f), r * 1.18f, Offset(x, wheelCy - r * 0.05f))
                    drawCircle(Color(0xFF34373F), r, Offset(x, wheelCy))
                    drawCircle(Color(0xFFBFC5CF), r * 0.52f, Offset(x, wheelCy))
                    drawCircle(Color(0xFF9AA1AD), r * 0.22f, Offset(x, wheelCy))
                }
                // Lighter sloped front face at the nose.
                val nr = CornerRadius(t * 0.16f)
                drawRoundRect(
                    Brush.horizontalGradient(listOf(c.tint(0.1f), c.tint(0.28f)), body.right - nose * 1.6f, body.right),
                    Offset(body.right - nose * 1.6f, roof.top + t * 0.12f), Size(nose * 1.6f, body.bottom - roof.top - t * 0.2f), nr,
                )
                // Dark slot under the cab, and a tiny tail light.
                drawRoundRect(
                    c.shade(0.42f),
                    Offset(roof.right - cab * 0.95f, roof.bottom + t * 0.08f), Size(cab * 0.5f, t * 0.09f), CornerRadius(t * 0.03f),
                )
                drawRoundRect(c.shade(0.5f), Offset(roof.right - cab * 0.36f, roof.bottom + t * 0.06f), Size(t * 0.05f, t * 0.17f), CornerRadius(t * 0.025f))
                drawRoundRect(Color(0xFFD84343), Offset(body.left + t * 0.01f, -t * 0.02f), Size(t * 0.05f, t * 0.12f), CornerRadius(t * 0.025f))

                // Top face.
                val rr = CornerRadius(t * 0.16f)
                drawRoundRect(edge.copy(alpha = 0.55f), roof.topLeft + Offset(0f, t * 0.025f), roof.size, rr)
                drawRoundRect(
                    Brush.verticalGradient(listOf(c.tint(0.3f), c.tint(0.12f)), roof.top, roof.bottom),
                    roof.topLeft, roof.size, rr,
                )
                // Cab roof: a soft shine, nothing else.
                drawRoundRect(
                    Color.White.copy(alpha = 0.18f),
                    Offset(bay.right + t * 0.08f, roof.top + t * 0.06f), Size(roof.right - bay.right - t * 0.16f, t * 0.12f), CornerRadius(t * 0.06f),
                )

                // The open bay: a thick light rim, a mid-tone back wall and a dark floor.
                val ir = CornerRadius(t * 0.08f)
                drawRoundRect(c.shade(0.72f), bay.topLeft, bay.size, ir)
                drawRoundRect(
                    Brush.verticalGradient(listOf(c.shade(0.42f), c.shade(0.3f)), bay.top + bay.height * 0.3f, bay.bottom),
                    Offset(bay.left, bay.top + bay.height * 0.3f), Size(bay.width, bay.height * 0.7f), ir,
                )
                drawRoundRect(c.tint(0.42f), bay.topLeft - Offset(t * 0.012f, t * 0.012f), Size(bay.width + t * 0.024f, bay.height + t * 0.024f), ir, style = Stroke(t * 0.035f))
                // Antenna where the bay meets the cab.
                val ax = bay.right + t * 0.1f
                drawLine(Color(0xFF22252C), Offset(ax, roof.top + t * 0.05f), Offset(ax, roof.top - t * 0.15f), t * 0.04f, StrokeCap.Round)
            }
        }

        val tex = sandTexture(color, ceil(bay.width).toInt().coerceAtLeast(4), ceil(bay.height).toInt().coerceAtLeast(4), max(1, (h * 0.045f).toInt()))
        return Look(
            body = img,
            origin = origin,
            length = l,
            height = h,
            bay = bay,
            sand = ShaderBrush(ImageShader(tex, TileMode.Repeated, TileMode.Repeated)),
            sandDark = c.shade(0.62f),
            sandLight = c.tint(0.6f),
        )
    }

    /** Grainy sand in the van's color: specks of four shades at [cell]-pixel size. */
    private fun sandTexture(color: Int, w: Int, h: Int, cell: Int): ImageBitmap {
        val c = jamColor(color)
        val shades = intArrayOf(c.tint(0.32f).toArgb(), c.tint(0.14f).toArgb(), c.toArgb(), c.shade(0.86f).toArgb())
        val rng = Random(color * 77 + w)
        val px = IntArray(w * h)
        var y = 0
        while (y < h) {
            var x = 0
            while (x < w) {
                val k = rng.nextInt(10)
                val col = shades[if (k < 3) 0 else if (k < 6) 1 else if (k < 9) 2 else 3]
                for (yy in y until min(h, y + cell)) for (xx in x until min(w, x + cell)) px[yy * w + xx] = col
                x += cell
            }
            y += cell
        }
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        bmp.setPixels(px, 0, w, 0, 0, w, h)
        return bmp.asImageBitmap()
    }
}
