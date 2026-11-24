package com.puzzel.lab.cartrafficjam.game.engine

import androidx.compose.ui.geometry.Offset
import kotlin.math.hypot

/**
 * A smooth polyline sampled at even spacing, so a point [s] pixels along it is one array
 * lookup. Crowd Loop lines its crowd up along one of these; past the end it carries straight on.
 */
class CrowdPath(raw: List<Offset>, private val step: Float) {
    private val xs: FloatArray
    private val ys: FloatArray
    private val tx: FloatArray
    private val ty: FloatArray
    val length: Float

    init {
        var pts = raw
        repeat(3) { pts = chaikin(pts) }
        val cum = FloatArray(pts.size)
        for (i in 1 until pts.size) cum[i] = cum[i - 1] + (pts[i] - pts[i - 1]).getDistance()
        length = cum.last()
        val n = (length / step).toInt() + 1
        xs = FloatArray(n)
        ys = FloatArray(n)
        tx = FloatArray(n)
        ty = FloatArray(n)
        var seg = 0
        for (k in 0 until n) {
            val s = k * step
            while (seg < pts.size - 2 && cum[seg + 1] < s) seg++
            val a = pts[seg]
            val b = pts[seg + 1]
            val len = (cum[seg + 1] - cum[seg]).coerceAtLeast(1e-3f)
            val f = ((s - cum[seg]) / len).coerceIn(0f, 1f)
            xs[k] = a.x + (b.x - a.x) * f
            ys[k] = a.y + (b.y - a.y) * f
            val d = hypot(b.x - a.x, b.y - a.y).coerceAtLeast(1e-3f)
            tx[k] = (b.x - a.x) / d
            ty[k] = (b.y - a.y) / d
        }
    }

    private fun index(s: Float) = (s / step).toInt().coerceIn(0, xs.size - 1)

    fun point(s: Float): Offset {
        if (s <= 0f) return Offset(xs[0] + tx[0] * s, ys[0] + ty[0] * s)
        if (s >= length) {
            val l = xs.size - 1
            return Offset(xs[l] + tx[l] * (s - length), ys[l] + ty[l] * (s - length))
        }
        val i = index(s)
        return Offset(xs[i], ys[i])
    }

    /** Point [s] along the path, pushed [side] pixels to the left of the walking direction. */
    fun point(s: Float, side: Float): Offset {
        val i = index(s)
        val p = point(s)
        return Offset(p.x - ty[i] * side, p.y + tx[i] * side)
    }

    /** Every [every]-th sample, for drawing the road under the crowd. */
    fun samples(every: Int): List<Offset> = (xs.indices step every).map { Offset(xs[it], ys[it]) } + Offset(xs.last(), ys.last())

    private fun chaikin(p: List<Offset>): List<Offset> {
        if (p.size < 3) return p
        val out = ArrayList<Offset>(p.size * 2)
        out += p.first()
        for (i in 0 until p.size - 1) {
            val a = p[i]
            val b = p[i + 1]
            out += a * 0.75f + b * 0.25f
            out += a * 0.25f + b * 0.75f
        }
        out += p.last()
        return out
    }
}
