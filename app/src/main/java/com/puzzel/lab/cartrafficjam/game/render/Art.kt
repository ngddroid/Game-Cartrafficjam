package com.puzzel.lab.cartrafficjam.game.render

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.LightingColorFilter
import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.puzzel.lab.cartrafficjam.game.model.JamColor
import com.puzzel.lab.cartrafficjam.game.model.VehicleType
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

fun Color.shade(f: Float) = Color((red * f).coerceIn(0f, 1f), (green * f).coerceIn(0f, 1f), (blue * f).coerceIn(0f, 1f), alpha)
fun Color.tint(f: Float) = lerp(this, Color.White, f)
fun Color.gray(f: Float) = lerp(this, Color(0xFF8A8F98), f)
fun jamColor(i: Int) = Color(JamColor.entries[i].hex)

val MysteryColor = Color(0xFF9BA3AE)

/** Draws [block] into a fresh software bitmap. */
fun renderBitmap(w: Int, h: Int, block: DrawScope.() -> Unit): ImageBitmap {
    val bmp = ImageBitmap(w.coerceAtLeast(1), h.coerceAtLeast(1))
    val canvas = androidx.compose.ui.graphics.Canvas(bmp)
    CanvasDrawScope().draw(Density(1f), LayoutDirection.Ltr, canvas, Size(w.toFloat(), h.toFloat()), block)
    return bmp
}

enum class Skin(val id: String, val title: String, val tagline: String, val price: Int) {
    CLASSIC("classic", "Classic", "Glossy factory paint", 0),
    CANDY("candy", "Candy Pop", "Pastel paint with sugar stripes", 300),
    RETRO("retro", "Retro Cruiser", "Two-tone, chrome and whitewalls", 500),
    TAXI("taxi", "City Cab", "Checker trim and roof light", 600),
    RACING("racing", "Speedway", "Race stripes, numbers and wings", 800),
    SAFARI("safari", "Safari", "Camo paint and a loaded roof rack", 900),
    CHROME("chrome", "Chrome Shine", "Mirror metal with dark glass", 1000),
    TOY("toy", "Toy Bricks", "Snap-together plastic studs", 1200),
    NEON("neon", "Neon Glow", "Glowing edges and underglow", 1500),
    GALAXY("galaxy", "Galaxy", "Deep-space paint full of stars", 1800),
    GOLDEN("golden", "Royal Gold", "Gold trim fit for a VIP", 2500);

    companion object {
        fun of(id: String) = entries.firstOrNull { it.id == id } ?: CLASSIC
    }
}

/**
 * Pseudo-3D vehicles built by sprite stacking: each vehicle is a pile of thin horizontal
 * slices (tyres, bumpers, body, light band, glass, roof) drawn one pixel above the other.
 * Rotating every slice by the heading before stacking gives a convincing toy-car look at any
 * angle. Finished stacks are cached per (type, colour, angle bucket).
 */
class VehicleArt(val unit: Float, val skin: Skin) {

    val pad = unit * 0.5f
    fun bodyW() = unit * 2 * 0.84f
    fun bodyL(type: VehicleType) = unit * type.length - unit * 0.32f

    private fun sliceW() = ceil(bodyW() * 1.2f + pad * 2).toInt()
    private fun sliceH(type: VehicleType) = ceil(bodyL(type) + pad * 2).toInt()

    /** Vertical pixels per unit of body width; tuned for a ~50° camera. */
    private val lift = 0.95f

    // --- Public API -------------------------------------------------------------------------

    class Composite(val bitmap: ImageBitmap, val groundX: Float, val groundY: Float, val roofZ: Float)

    private val stacks = HashMap<Int, List<Pair<Int, Bitmap>>>()
    private val composites = object : LinkedHashMap<Long, Composite>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Long, Composite>?) = size > 160
    }
    private val shadows = HashMap<Int, ImageBitmap>()

    /** The finished 3D sprite for [angle] degrees (clockwise from north). */
    fun composite(type: VehicleType, color: Int, mystery: Boolean, angle: Float): Composite {
        val bucket = (((angle % 360f) + 360f) % 360f / 2f).roundToInt() % 180
        val key = (stackKey(type, color, mystery).toLong() shl 9) or bucket.toLong()
        return composites.getOrPut(key) { buildComposite(type, color, mystery, bucket * 2f) }
    }

    /**
     * Draws the slice stack straight onto [scope] at any angle, without caching. Used for the
     * Garage turntable where the angle changes every frame.
     */
    fun drawLive(scope: DrawScope, type: VehicleType, color: Int, angle: Float, ground: Offset) {
        val slices = stack(type, color, false)
        val sw = sliceW() / 2f
        val sh = sliceH(type) / 2f
        scope.drawIntoCanvas { c ->
            val n = c.nativeCanvas
            for ((z, slice) in slices) {
                n.save()
                n.translate(ground.x, ground.y - z)
                n.rotate(angle)
                n.drawBitmap(slice, -sw, -sh, slicePaint)
                n.restore()
            }
        }
    }

    /** Residual rotation between the requested angle and the cached bucket. */
    fun residual(angle: Float): Float {
        val a = ((angle % 360f) + 360f) % 360f
        return a - (a / 2f).roundToInt() * 2f
    }

    fun shadow(type: VehicleType): ImageBitmap = shadows.getOrPut(type.ordinal) {
        val bw = bodyW()
        val bl = bodyL(type)
        renderBitmap(sliceW(), sliceH(type)) {
            drawIntoCanvas {
                val p = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    this.color = if (skin == Skin.NEON) android.graphics.Color.argb(150, 120, 220, 255) else android.graphics.Color.argb(95, 20, 25, 45)
                    maskFilter = BlurMaskFilter(unit * 0.38f, BlurMaskFilter.Blur.NORMAL)
                }
                val cx = size.width / 2
                val cy = size.height / 2
                it.nativeCanvas.drawRoundRect(cx - bw * 0.52f, cy - bl * 0.5f, cx + bw * 0.52f, cy + bl * 0.5f, bw * 0.3f, bw * 0.3f, p)
            }
        }
    }

    /** Roof rectangle in north-facing local coordinates (origin at footprint centre). */
    fun roofRect(type: VehicleType): Rect {
        val bw = bodyW()
        val bl = bodyL(type)
        val g = greenhouse(type)
        return Rect(-bw / 2 + bw * g.inset, -bl / 2 + bl * g.start, bw / 2 - bw * g.inset, -bl / 2 + bl * g.end)
    }

    fun roofZ(type: VehicleType): Float = heights(type).roofTop.toFloat()

    // --- Geometry ---------------------------------------------------------------------------

    private class Greenhouse(val start: Float, val end: Float, val inset: Float, val radius: Float)

    private fun greenhouse(type: VehicleType) = when (type) {
        VehicleType.CAR -> Greenhouse(0.26f, 0.84f, 0.11f, 0.26f)
        VehicleType.VAN -> Greenhouse(0.16f, 0.97f, 0.08f, 0.22f)
        VehicleType.BUS -> Greenhouse(0.03f, 0.985f, 0.05f, 0.14f)
    }

    private fun bodyRadius(type: VehicleType) = bodyW() * when (type) {
        VehicleType.CAR -> 0.3f
        VehicleType.VAN -> 0.22f
        VehicleType.BUS -> 0.14f
    }

    private class Heights(val tyre: Int, val bumper: Int, val lower: Int, val lights: Int, val upper: Int, val glass: Int, val roofTop: Int)

    private fun heights(type: VehicleType): Heights {
        val bw = bodyW() * lift
        fun px(f: Float) = max(1, (bw * f).roundToInt())
        val tyre = px(0.1f)
        val bumper = px(0.06f)
        val lower = px(if (type == VehicleType.CAR) 0.05f else 0.08f)
        val lights = px(0.07f)
        val upper = px(0.05f)
        val glass = px(
            when (type) {
                VehicleType.CAR -> 0.17f
                VehicleType.VAN -> 0.24f
                VehicleType.BUS -> 0.26f
            },
        )
        val roofTop = tyre / 2 + bumper + lower + lights + upper + glass
        return Heights(tyre, bumper, lower, lights, upper, glass, roofTop)
    }

    private fun wheelSpots(type: VehicleType) = when (type) {
        VehicleType.CAR -> listOf(0.19f, 0.78f)
        VehicleType.VAN -> listOf(0.15f, 0.8f)
        VehicleType.BUS -> listOf(0.13f, 0.32f, 0.82f)
    }

    // --- Palette per skin -------------------------------------------------------------------

    private class Palette(
        val body: Color,
        val roof: Color,
        val trim: Color,
        val glassTop: Color,
        val glassBottom: Color,
        val edge: Color?,
    )

    private fun palette(base: Color, mystery: Boolean): Palette {
        if (mystery) return Palette(base, base.tint(0.18f), Color(0xFF5A606B), Color(0xFF8DA2BF), Color(0xFF2E3A4F), null)
        return when (skin) {
            Skin.CLASSIC -> Palette(base, base.tint(0.14f), Color(0xFF3A3F48), Color(0xFF6E93C4), Color(0xFF1A2640), null)
            Skin.CANDY -> Palette(base.tint(0.38f), base.tint(0.55f), Color(0xFFFFFFFF), Color(0xFFC9E8FF), Color(0xFF6E8BB5), Color.White)
            Skin.RETRO -> Palette(base.gray(0.12f), Color(0xFFFFF3DA), Color(0xFFD5DBE2), Color(0xFF9EC3D9), Color(0xFF2F4A5C), Color(0xFFFFF3DA))
            Skin.TAXI -> Palette(base, base.tint(0.1f), Color(0xFF26282E), Color(0xFF8FB3E0), Color(0xFF223250), null)
            Skin.RACING -> Palette(base.shade(1.03f), base.tint(0.08f), Color(0xFF1E2128), Color(0xFF6E7F99), Color(0xFF151C2A), Color.White)
            Skin.SAFARI -> Palette(base.gray(0.28f), base.gray(0.2f).tint(0.08f), Color(0xFF3B3328), Color(0xFFA9C6A8), Color(0xFF2C3B2C), null)
            Skin.CHROME -> Palette(base.gray(0.25f).tint(0.15f), base.gray(0.2f).tint(0.35f), Color(0xFFF5F7FA), Color(0xFF3A414C), Color(0xFF07090C), Color(0xFFF5F7FA))
            Skin.TOY -> Palette(base, base, base.shade(0.82f), Color(0xFFBFE4FF), Color(0xFF5E88B8), null)
            Skin.NEON -> Palette(Color(0xFF1B1F2E), Color(0xFF242a3d), base, base.tint(0.4f), base.shade(0.5f), base.tint(0.25f))
            Skin.GALAXY -> Palette(lerp(base.shade(0.42f), Color(0xFF2A1B5C), 0.45f), lerp(base.shade(0.5f), Color(0xFF3A2A7A), 0.4f), Color(0xFFC9D2FF), Color(0xFFB39DFF), Color(0xFF1A1240), Color(0xFFC9D2FF))
            Skin.GOLDEN -> Palette(base, base.tint(0.12f), Color(0xFFFFC93C), Color(0xFF9FC4F0), Color(0xFF1F2E4A), Color(0xFFFFC93C))
        }
    }

    // --- Slices -----------------------------------------------------------------------------

    private fun stackKey(type: VehicleType, color: Int, mystery: Boolean) = type.ordinal * 16 + if (mystery) 15 else color

    /** (z, slice) pairs, bottom to top. */
    private fun stack(type: VehicleType, color: Int, mystery: Boolean): List<Pair<Int, Bitmap>> =
        stacks.getOrPut(stackKey(type, color, mystery)) { buildStack(type, if (mystery) MysteryColor else jamColor(color), mystery) }

    private fun buildStack(type: VehicleType, base: Color, mystery: Boolean): List<Pair<Int, Bitmap>> {
        val w = sliceW()
        val h = sliceH(type)
        val bw = bodyW()
        val bl = bodyL(type)
        val cx = w / 2f
        val cy = h / 2f
        val r = bodyRadius(type)
        val pal = palette(base, mystery)
        val hs = heights(type)
        val gh = greenhouse(type)
        val out = ArrayList<Pair<Int, Bitmap>>()
        val bodyRect = Rect(cx - bw / 2, cy - bl / 2, cx + bw / 2, cy + bl / 2)

        fun slice(block: DrawScope.() -> Unit) = renderBitmap(w, h, block).asAndroidBitmap()
        fun Bitmap.dim(f: Float) = darken(this, f)

        // Tyres: dark blocks poking out of the sides, hub caps show on the outer face.
        val tyre = slice {
            for (wy in wheelSpots(type)) {
                val y = bodyRect.top + bl * wy
                val tl = bl * 0.13f
                drawRoundRect(Color(0xFF1C1E22), Offset(bodyRect.left - bw * 0.05f, y), Size(bw * 1.1f, tl), CornerRadius(bw * 0.06f))
                val hub = when {
                    skin == Skin.RETRO && !mystery -> Color.White
                    skin == Skin.GOLDEN && !mystery -> Color(0xFFFFC93C)
                    else -> Color(0xFFB8C0CA)
                }
                drawRect(hub, Offset(bodyRect.left - bw * 0.05f, y + tl * 0.3f), Size(bw * 0.035f, tl * 0.4f))
                drawRect(hub, Offset(bodyRect.right + bw * 0.015f, y + tl * 0.3f), Size(bw * 0.035f, tl * 0.4f))
            }
        }
        var z = 0
        for (i in 0 until hs.tyre) out += z++ to tyre.dim(0.75f + 0.25f * i / hs.tyre)
        z = hs.tyre / 2

        val bodyPath = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(bodyRect, CornerRadius(r))) }

        // Wall slice: body silhouette whose outline pixels carry the side detailing.
        fun wall(kind: Int) = slice {
            drawPath(bodyPath, pal.body)
            clipPath(bodyPath) {
                val front = bl * 0.07f
                when (kind) {
                    0 -> { // bumpers
                        drawRect(pal.trim, Offset(bodyRect.left, bodyRect.top), Size(bw, front))
                        drawRect(pal.trim, Offset(bodyRect.left, bodyRect.bottom - front), Size(bw, front))
                        drawRect(pal.body.shade(0.8f), Offset(bodyRect.left, bodyRect.top + front), Size(bw, bl - 2 * front))
                    }
                    1 -> { // light band: headlights, grille, tail lights, side decoration
                        drawRect(Color(0xFFFFF7CF), Offset(bodyRect.left, bodyRect.top), Size(bw * 0.3f, front))
                        drawRect(Color(0xFFFFF7CF), Offset(bodyRect.right - bw * 0.3f, bodyRect.top), Size(bw * 0.3f, front))
                        drawRect(pal.trim.shade(0.8f), Offset(bodyRect.left + bw * 0.36f, bodyRect.top), Size(bw * 0.28f, front))
                        drawRect(Color(0xFFE5322B), Offset(bodyRect.left, bodyRect.bottom - front), Size(bw * 0.26f, front))
                        drawRect(Color(0xFFE5322B), Offset(bodyRect.right - bw * 0.26f, bodyRect.bottom - front), Size(bw * 0.26f, front))
                        sideDecor(this, bodyRect, pal, mystery)
                    }
                    else -> if (pal.edge != null && !mystery && skin != Skin.TAXI) {
                        drawPath(bodyPath, pal.edge, style = Stroke(max(1.5f, bw * 0.05f)))
                    }
                }
            }
        }
        val bumper = wall(0)
        val lower = wall(3)
        val lights = wall(1)
        val upper = wall(2)
        for (i in 0 until hs.bumper) out += z++ to bumper.dim(0.6f + 0.06f * i / hs.bumper)
        for (i in 0 until hs.lower) out += z++ to lower.dim(0.66f + 0.1f * i / hs.lower)
        for (i in 0 until hs.lights) out += z++ to lights.dim(0.8f + 0.08f * i / hs.lights)
        for (i in 0 until hs.upper - 1) out += z++ to upper.dim(0.88f + 0.06f * i / hs.upper)
        out += z++ to slice { hoodCap(type, bodyRect, bodyPath, pal, mystery) }

        // Glass band: cabin silhouette in glass colours with body-coloured pillars.
        val cab = Rect(
            bodyRect.left + bw * gh.inset, bodyRect.top + bl * gh.start,
            bodyRect.right - bw * gh.inset, bodyRect.top + bl * gh.end,
        )
        val cabPath = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(cab, CornerRadius(bw * gh.radius))) }
        val glass = slice {
            drawPath(cabPath, Brush.verticalGradient(listOf(pal.glassTop, pal.glassBottom), cab.top, cab.bottom))
            clipPath(cabPath) {
                val pillar = max(1.5f, bl * 0.022f)
                val pillars = when (type) {
                    VehicleType.CAR -> listOf(0.02f, 0.5f, 0.97f)
                    VehicleType.VAN -> listOf(0.02f, 0.2f, 0.45f, 0.7f, 0.97f)
                    VehicleType.BUS -> (0..9).map { 0.01f + it * 0.108f }
                }
                for (p in pillars) {
                    drawRect(pal.body, Offset(cab.left, cab.top + cab.height * p - pillar / 2), Size(cab.width, pillar))
                }
                // Reflection streak
                drawRect(Color.White.copy(alpha = 0.18f), Offset(cab.left, cab.top), Size(cab.width, cab.height * 0.08f))
                if (skin == Skin.NEON && !mystery) drawPath(cabPath, pal.edge!!, style = Stroke(max(1.5f, bw * 0.04f)))
            }
        }
        val glassTop = slice {
            drawPath(cabPath, pal.body.shade(0.92f))
        }
        for (i in 0 until hs.glass - 1) out += z++ to glass.dim(0.92f + 0.08f * i / hs.glass)
        out += z++ to glassTop.dim(0.92f)
        out += z to slice { roofCap(type, cab, cabPath, bodyRect, pal, mystery) }
        val roofZ = z

        // Extras on the roof.
        when {
            type == VehicleType.BUS && skin != Skin.TOY -> {
                val ac = Rect(cx - bw * 0.2f, cab.top + cab.height * 0.7f, cx + bw * 0.2f, cab.top + cab.height * 0.86f)
                val acSide = slice { drawRoundRect(Color(0xFFAEB7C2), ac.topLeft, ac.size, CornerRadius(bw * 0.05f)) }
                val acTop = slice {
                    drawRoundRect(Color(0xFFE3E8EE), ac.topLeft, ac.size, CornerRadius(bw * 0.05f))
                    for (k in 1..3) drawLine(Color(0xFFB8C1CC), Offset(ac.left + ac.width * k / 4, ac.top + 2f), Offset(ac.left + ac.width * k / 4, ac.bottom - 2f), 1f)
                }
                for (k in 1..max(1, hs.upper)) out += roofZ + k to acSide
                out += roofZ + hs.upper + 1 to acTop
            }
        }
        if (!mystery) when (skin) {
            Skin.TAXI -> {
                val sign = Rect(cx - bw * 0.2f, cab.top + cab.height * 0.04f, cx + bw * 0.2f, cab.top + cab.height * (if (type == VehicleType.CAR) 0.2f else 0.1f))
                val side = slice { drawRoundRect(Color(0xFF2A2A2A), sign.topLeft, sign.size, CornerRadius(bw * 0.04f)) }
                val top = slice {
                    drawRoundRect(Color(0xFFFFD84A), sign.topLeft, sign.size, CornerRadius(bw * 0.04f))
                    drawRect(Color(0xFF2A2A2A), Offset(sign.left + sign.width * 0.2f, sign.center.y - 1f), Size(sign.width * 0.6f, 2f))
                }
                val n = max(2, hs.upper + 1)
                for (k in 1..n) out += roofZ + k to side
                out += roofZ + n + 1 to top
            }
            Skin.RACING -> if (type == VehicleType.CAR) {
                val wing = Rect(bodyRect.left + bw * 0.02f, bodyRect.bottom - bl * 0.1f, bodyRect.right - bw * 0.02f, bodyRect.bottom - bl * 0.03f)
                val post = slice {
                    drawRect(Color(0xFF1E2128), Offset(cx - bw * 0.3f, wing.top), Size(bw * 0.06f, wing.height))
                    drawRect(Color(0xFF1E2128), Offset(cx + bw * 0.24f, wing.top), Size(bw * 0.06f, wing.height))
                }
                val top = slice {
                    drawRoundRect(Color(0xFF1E2128), wing.topLeft, wing.size, CornerRadius(bw * 0.03f))
                    drawRect(Color.White, Offset(wing.left, wing.center.y - 0.75f), Size(wing.width, 1.5f))
                }
                val base = hs.tyre / 2 + hs.bumper + hs.lower + hs.lights + hs.upper
                val n = max(2, hs.glass / 2)
                for (k in 1..n) out += base + k to post
                out += base + n + 1 to top
            } else Unit
            Skin.SAFARI -> {
                val rack = Rect(cab.left + bw * 0.08f, cab.top + cab.height * (if (type == VehicleType.CAR) 0.8f else 0.78f), cab.right - bw * 0.08f, cab.bottom - cab.height * 0.03f)
                val box = slice {
                    drawRoundRect(Color(0xFF8D6E4A), rack.topLeft, rack.size, CornerRadius(bw * 0.04f))
                }
                val top = slice {
                    drawRoundRect(Color(0xFFB08A5C), rack.topLeft, rack.size, CornerRadius(bw * 0.04f))
                    drawLine(Color(0xFF6D5235), Offset(rack.left, rack.center.y), Offset(rack.right, rack.center.y), 1.5f)
                    drawLine(Color(0xFF6D5235), Offset(rack.center.x, rack.top), Offset(rack.center.x, rack.bottom), 1.5f)
                }
                val n = max(2, hs.upper + 1)
                for (k in 1..n) out += roofZ + k to box
                out += roofZ + n + 1 to top
            }
            Skin.TOY -> {
                val studs = studPositions(type, cab, bw)
                val studSide = slice { for (s in studs) drawCircle(pal.body.shade(0.72f), bw * 0.1f, s) }
                val studTop = slice {
                    for (s in studs) {
                        drawCircle(pal.body.tint(0.14f), bw * 0.1f, s)
                        drawCircle(pal.body.shade(0.85f), bw * 0.1f, s, style = Stroke(1f))
                        drawCircle(Color.White.copy(alpha = 0.6f), bw * 0.035f, s - Offset(bw * 0.03f, bw * 0.03f))
                    }
                }
                out += roofZ + 1 to studSide
                out += roofZ + 2 to studSide
                out += roofZ + 3 to studTop
            }
            else -> Unit
        }
        return out.sortedBy { it.first }
    }

    private fun studPositions(type: VehicleType, cab: Rect, bw: Float): List<Offset> {
        val step = bw * 0.27f
        val rows = ((cab.height - step * 0.6f) / step).toInt().coerceAtLeast(2)
        val list = ArrayList<Offset>()
        for (k in 0 until rows) {
            val y = cab.top + step * 0.6f + k * step
            list += Offset(cab.left + bw * 0.13f, y)
            list += Offset(cab.right - bw * 0.13f, y)
        }
        return list
    }

    private fun sideDecor(scope: DrawScope, body: Rect, pal: Palette, mystery: Boolean) = with(scope) {
        if (mystery) return@with
        val bw = body.width
        val bl = body.height
        when (skin) {
            Skin.TAXI -> {
                val sq = max(2f, bl * 0.045f)
                var y = body.top + bl * 0.1f
                var k = 0
                while (y < body.bottom - bl * 0.1f) {
                    val c = if (k % 2 == 0) Color(0xFF1A1A1A) else Color.White
                    drawRect(c, Offset(body.left, y), Size(bw * 0.08f, sq))
                    drawRect(if (k % 2 == 0) Color.White else Color(0xFF1A1A1A), Offset(body.right - bw * 0.08f, y), Size(bw * 0.08f, sq))
                    y += sq; k++
                }
            }
            Skin.RACING -> {
                drawRect(Color.White, Offset(body.left, body.top + bl * 0.3f), Size(bw * 0.08f, bl * 0.2f))
                drawRect(Color.White, Offset(body.right - bw * 0.08f, body.top + bl * 0.3f), Size(bw * 0.08f, bl * 0.2f))
            }
            Skin.GALAXY -> {
                val rnd = Random(bl.toInt())
                repeat(10) { drawCircle(Color.White.copy(alpha = 0.9f), 0.9f, Offset(body.left + rnd.nextFloat() * bw, body.top + rnd.nextFloat() * bl)) }
            }
            else -> Unit
        }
    }

    // --- Caps (the visible top surfaces) ----------------------------------------------------

    private fun DrawScope.hoodCap(type: VehicleType, body: Rect, bodyPath: Path, pal: Palette, mystery: Boolean) {
        val bw = body.width
        val bl = body.height
        val neon = skin == Skin.NEON && !mystery
        drawPath(
            bodyPath,
            Brush.horizontalGradient(
                0f to pal.body.shade(0.86f), 0.25f to pal.body, 0.5f to pal.body.tint(0.12f), 0.75f to pal.body, 1f to pal.body.shade(0.86f),
                startX = body.left, endX = body.right,
            ),
        )
        clipPath(bodyPath) {
            paintwork(body, pal, mystery, hood = true)
            // Headlights and tail lights seen from above.
            val lw = bw * 0.2f
            val lh = bl * 0.035f
            drawRoundRect(Color(0xFFFFFBE0), Offset(body.left + bw * 0.08f, body.top + 1f), Size(lw, lh), CornerRadius(lh))
            drawRoundRect(Color(0xFFFFFBE0), Offset(body.right - bw * 0.08f - lw, body.top + 1f), Size(lw, lh), CornerRadius(lh))
            drawRoundRect(Color(0xFFE5322B), Offset(body.left + bw * 0.08f, body.bottom - lh - 1f), Size(lw, lh), CornerRadius(lh))
            drawRoundRect(Color(0xFFE5322B), Offset(body.right - bw * 0.08f - lw, body.bottom - lh - 1f), Size(lw, lh), CornerRadius(lh))
            if (type == VehicleType.CAR && !mystery) {
                // Hood panel line
                drawLine(pal.body.shade(0.82f), Offset(body.left + bw * 0.22f, body.top + bl * 0.06f), Offset(body.left + bw * 0.22f, body.top + bl * 0.27f), 1f)
                drawLine(pal.body.shade(0.82f), Offset(body.right - bw * 0.22f, body.top + bl * 0.06f), Offset(body.right - bw * 0.22f, body.top + bl * 0.27f), 1f)
            }
        }
        drawPath(bodyPath, if (neon) pal.edge!! else pal.body.shade(0.6f), style = Stroke(if (neon) bw * 0.05f else 1.2f))
        // mirrors
        val my = body.top + bl * when (type) {
            VehicleType.CAR -> 0.3f
            VehicleType.VAN -> 0.17f
            VehicleType.BUS -> 0.04f
        }
        drawOval(pal.body.shade(0.75f), Offset(body.left - bw * 0.08f, my), Size(bw * 0.13f, bl * 0.045f))
        drawOval(pal.body.shade(0.75f), Offset(body.right - bw * 0.05f, my), Size(bw * 0.13f, bl * 0.045f))
    }

    private fun DrawScope.roofCap(type: VehicleType, cab: Rect, cabPath: Path, body: Rect, pal: Palette, mystery: Boolean) {
        val bw = body.width
        val neon = skin == Skin.NEON && !mystery
        drawPath(
            cabPath,
            Brush.linearGradient(
                listOf(pal.roof.tint(0.18f), pal.roof, pal.roof.shade(0.9f)),
                start = Offset(cab.left, cab.top), end = Offset(cab.right, cab.bottom),
            ),
        )
        clipPath(cabPath) {
            paintwork(cab, pal, mystery, hood = false)
            // Glossy highlight along the left edge of the roof
            drawRoundRect(
                Color.White.copy(alpha = if (skin == Skin.CHROME) 0.45f else 0.22f),
                Offset(cab.left + cab.width * 0.07f, cab.top + cab.height * 0.08f), Size(cab.width * 0.12f, cab.height * 0.6f), CornerRadius(cab.width * 0.06f),
            )
        }
        drawPath(cabPath, if (neon) pal.edge!! else pal.roof.shade(0.72f), style = Stroke(if (neon) bw * 0.045f else 1.2f))

        // Arrow
        val la = min(bw * 1.02f, cab.height * 0.9f)
        val wa = bw * 0.62f
        val acx = cab.center.x
        val acy = cab.center.y + if (type == VehicleType.BUS) -cab.height * 0.08f else 0f
        val shadowCol = if (neon) Color.Black.copy(alpha = 0.6f) else pal.roof.shade(0.45f).copy(alpha = 0.55f)
        drawPath(arrowPath(acx + bw * 0.03f, acy + bw * 0.06f, wa, la), shadowCol)
        val arrow = arrowPath(acx, acy, wa, la)
        drawPath(arrow, if (neon) pal.edge!! else Color.White)
        if (!neon) drawPath(arrow, pal.roof.shade(0.55f).copy(alpha = 0.7f), style = Stroke(max(1f, bw * 0.025f)))
        drawPath(arrow, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0f), Color(0x22000000)), acy - la / 2, acy + la / 2))
        if (skin == Skin.GOLDEN && !mystery) drawPath(arrow, Color(0xFFFFC93C), style = Stroke(max(1f, bw * 0.03f)))

    }

    /** Skin-specific paint applied to hood and roof caps (clipped to the surface). */
    private fun DrawScope.paintwork(area: Rect, pal: Palette, mystery: Boolean, hood: Boolean) {
        if (mystery) return
        val bw = bodyW()
        when (skin) {
            Skin.CANDY -> {
                val sw = bw * 0.08f
                drawRect(Color.White.copy(alpha = 0.92f), Offset(area.center.x - sw * 2.2f, area.top), Size(sw, area.height))
                drawRect(Color.White.copy(alpha = 0.92f), Offset(area.center.x + sw * 1.2f, area.top), Size(sw, area.height))
            }
            Skin.RACING -> {
                val sw = bw * 0.14f
                drawRect(Color.White, Offset(area.center.x - sw * 1.25f, area.top), Size(sw, area.height))
                drawRect(Color.White, Offset(area.center.x + sw * 0.25f, area.top), Size(sw, area.height))
                if (hood) {
                    val c = Offset(area.center.x, area.top + area.height * 0.15f)
                    drawCircle(Color.White, bw * 0.17f, c)
                    drawCircle(Color(0xFF1E2128), bw * 0.17f, c, style = Stroke(1.2f))
                }
            }
            Skin.SAFARI -> {
                val rnd = Random((area.width * 31 + area.height).toInt())
                val n = (area.height / bw * 9).toInt().coerceIn(8, 40)
                repeat(n) {
                    val c = Offset(area.left - bw * 0.1f + rnd.nextFloat() * area.width, area.top + rnd.nextFloat() * area.height)
                    val col = when (it % 3) {
                        0 -> pal.body.shade(0.6f)
                        1 -> Color(0xFF5D4A2E).copy(alpha = 0.75f)
                        else -> pal.body.tint(0.3f)
                    }
                    drawOval(col, c, Size(bw * (0.22f + rnd.nextFloat() * 0.22f), bw * (0.14f + rnd.nextFloat() * 0.16f)))
                }
            }
            Skin.CHROME -> {
                drawRect(
                    Brush.linearGradient(
                        0f to Color.Black.copy(alpha = 0.18f), 0.3f to Color.White.copy(alpha = 0.8f), 0.38f to Color.Transparent,
                        0.62f to Color.Black.copy(alpha = 0.12f), 0.72f to Color.White.copy(alpha = 0.55f), 1f to Color.Transparent,
                        start = Offset(area.left, area.top), end = Offset(area.right, area.top + area.width * 1.2f),
                    ),
                    area.topLeft, area.size,
                )
            }
            Skin.RETRO -> if (!hood) {
                drawRect(Color.White.copy(alpha = 0.25f), area.topLeft, area.size)
            } else {
                drawRect(pal.trim, Offset(area.left, area.top + area.height * 0.5f - 1f), Size(area.width, 2f))
            }
            Skin.GALAXY -> {
                val rnd = Random((area.height * 7).toInt())
                drawRect(Brush.radialGradient(listOf(Color(0x557C4DFF), Color.Transparent), area.center, area.maxDimension * 0.6f), area.topLeft, area.size)
                repeat((area.width * area.height / (bw * bw) * 14).toInt().coerceIn(6, 60)) {
                    drawCircle(Color.White.copy(alpha = 0.5f + rnd.nextFloat() * 0.5f), 0.5f + rnd.nextFloat() * 1.1f, Offset(area.left + rnd.nextFloat() * area.width, area.top + rnd.nextFloat() * area.height))
                }
            }
            Skin.GOLDEN -> {
                drawRect(pal.trim, Offset(area.left, area.top), Size(area.width, max(1.5f, bw * 0.04f)))
                if (!hood) drawCircle(Color(0xFFFFC93C).copy(alpha = 0.35f), bw * 0.4f, area.center)
            }
            else -> Unit
        }
    }

    // --- Composite --------------------------------------------------------------------------

    private val slicePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val darkCache = HashMap<Pair<Bitmap, Int>, Bitmap>()

    /** A copy of [src] with RGB scaled by [f], shared between identical wall slices. */
    private fun darken(src: Bitmap, f: Float): Bitmap {
        val q = (f.coerceIn(0f, 1f) * 50).roundToInt()
        if (q >= 50) return src
        return darkCache.getOrPut(src to q) {
            val m = (q / 50f * 255).roundToInt()
            val out = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
            android.graphics.Canvas(out).drawBitmap(
                src, 0f, 0f,
                Paint().apply { colorFilter = LightingColorFilter(android.graphics.Color.rgb(m, m, m), 0) },
            )
            out
        }
    }

    private fun buildComposite(type: VehicleType, color: Int, mystery: Boolean, angle: Float): Composite {
        val slices = stack(type, color, mystery)
        val sw = sliceW()
        val sh = sliceH(type)
        val diag = sqrt((sw * sw + sh * sh).toFloat())
        val maxZ = slices.maxOf { it.first } + 1
        val w = ceil(diag).toInt() + 2
        val h = ceil(diag).toInt() + maxZ + 2
        val gx = w / 2f
        val gy = maxZ + diag / 2f + 1
        val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bmp)
        for ((z, slice) in slices) {
            canvas.save()
            canvas.translate(gx, gy - z)
            canvas.rotate(angle)
            canvas.drawBitmap(slice, -sw / 2f, -sh / 2f, slicePaint)
            canvas.restore()
        }
        return Composite(bmp.asImageBitmap(), gx, gy, heights(type).roofTop.toFloat())
    }

    companion object {
        fun arrowPath(cx: Float, cy: Float, w: Float, len: Float): Path = Path().apply {
            val head = len * 0.5f
            val shaft = w * 0.42f
            moveTo(cx, cy - len / 2)
            lineTo(cx + w / 2, cy - len / 2 + head)
            lineTo(cx + shaft / 2, cy - len / 2 + head)
            lineTo(cx + shaft / 2, cy + len / 2)
            lineTo(cx - shaft / 2, cy + len / 2)
            lineTo(cx - shaft / 2, cy - len / 2 + head)
            lineTo(cx - w / 2, cy - len / 2 + head)
            close()
        }
    }
}

/**
 * Glossy jelly people with little faces. Frames 0–3 are a walk cycle, frame 4 is standing.
 */
class PassengerArt(val w: Float, val h: Float) {
    private val cache = HashMap<Int, ImageBitmap>()

    fun get(color: Int, frame: Int = 4): ImageBitmap = cache.getOrPut(color * 8 + frame) { build(color, frame) }

    private fun build(color: Int, frame: Int): ImageBitmap {
        val c = jamColor(color)
        val walking = frame < 4
        val phase = frame * PI.toFloat() / 2
        val swing = if (walking) sin(phase) else 0f
        val bob = if (walking) kotlin.math.abs(cos(phase)) * h * 0.025f else 0f
        return renderBitmap(ceil(w).toInt(), ceil(h).toInt()) {
            val dark = c.shade(0.62f)
            val legTop = h * 0.66f - bob
            val legW = w * 0.2f
            fun leg(x: Float, s: Float, front: Boolean) {
                val dx = s * w * 0.1f
                val lift = if (s > 0.3f) h * 0.03f else 0f
                drawRoundRect(
                    if (front) c.shade(0.78f) else c.shade(0.62f),
                    Offset(x - legW / 2 + dx, legTop), Size(legW, h * 0.98f - legTop - lift), CornerRadius(legW / 2),
                )
            }
            fun arm(x: Float, s: Float, front: Boolean) {
                val dy = s * h * 0.03f
                drawRoundRect(
                    if (front) c.shade(0.85f) else c.shade(0.65f),
                    Offset(x - w * 0.09f, h * 0.42f - bob + dy), Size(w * 0.18f, h * 0.24f), CornerRadius(w * 0.09f),
                )
            }
            leg(w * 0.37f, -swing, false)
            arm(w * 0.14f, swing, false)
            leg(w * 0.63f, swing, true)
            // torso
            val tTop = h * 0.36f - bob
            drawRoundRect(dark, Offset(w * 0.17f - 0.8f, tTop - 0.8f), Size(w * 0.66f + 1.6f, h * 0.38f + 1.6f), CornerRadius(w * 0.3f))
            drawRoundRect(
                Brush.horizontalGradient(listOf(c.tint(0.3f), c, c.shade(0.72f)), w * 0.17f, w * 0.83f),
                Offset(w * 0.17f, tTop), Size(w * 0.66f, h * 0.38f), CornerRadius(w * 0.3f),
            )
            drawOval(Color.White.copy(alpha = 0.35f), Offset(w * 0.27f, tTop + h * 0.04f), Size(w * 0.16f, h * 0.12f))
            arm(w * 0.86f, -swing, true)
            // head
            val hc = Offset(w * 0.5f, h * 0.21f - bob)
            val hr = w * 0.33f
            drawCircle(dark, hr + 0.9f, hc)
            drawCircle(
                Brush.radialGradient(listOf(c.tint(0.5f), c, c.shade(0.78f)), center = hc - Offset(hr * 0.4f, hr * 0.45f), radius = hr * 1.5f),
                hr, hc,
            )
            // face
            val eyeY = hc.y + hr * 0.05f
            drawOval(Color(0xFF1F2430), Offset(hc.x - hr * 0.42f, eyeY - hr * 0.16f), Size(hr * 0.2f, hr * 0.3f))
            drawOval(Color(0xFF1F2430), Offset(hc.x + hr * 0.22f, eyeY - hr * 0.16f), Size(hr * 0.2f, hr * 0.3f))
            drawCircle(Color.White, hr * 0.06f, Offset(hc.x - hr * 0.3f, eyeY - hr * 0.08f))
            drawCircle(Color.White, hr * 0.06f, Offset(hc.x + hr * 0.34f, eyeY - hr * 0.08f))
            drawCircle(Color(0x55FF5A7A), hr * 0.13f, Offset(hc.x - hr * 0.58f, eyeY + hr * 0.3f))
            drawCircle(Color(0x55FF5A7A), hr * 0.13f, Offset(hc.x + hr * 0.58f, eyeY + hr * 0.3f))
            drawCircle(Color.White.copy(alpha = 0.7f), hr * 0.2f, hc - Offset(hr * 0.42f, hr * 0.48f))
        }
    }
}
