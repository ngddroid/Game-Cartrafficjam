package com.ngstudio.trafficjam.game.modes

import android.util.Log

/** Debug timing: averages and worst cases of a mode's simulate/update/draw, logged every 2 s. */
internal class PerfLog(private val tag: String) {
    private var n = 0
    private var sim = 0L
    private var upd = 0L
    private var drw = 0L
    private var simMax = 0L
    private var updMax = 0L
    private var drwMax = 0L
    private var frames = 0
    private var since = System.nanoTime()
    private var last = 0L
    private var gapMax = 0L

    fun step(simNs: Long, updNs: Long) {
        n++
        sim += simNs; upd += updNs
        if (simNs > simMax) simMax = simNs
        if (updNs > updMax) updMax = updNs
    }

    fun draw(drawNs: Long, extra: Int) {
        frames++
        drw += drawNs
        if (drawNs > drwMax) drwMax = drawNs
        val now = System.nanoTime()
        if (last != 0L && now - last > gapMax) gapMax = now - last
        last = now
        if (now - since < 2_000_000_000L) return
        val secs = (now - since) / 1e9
        fun ms(v: Long) = "%.2f".format(v / 1e6)
        Log.w(
            tag,
            "fps=${"%.1f".format(frames / secs)} belt=$extra sim=${ms(sim / maxOf(1, n))}/${ms(simMax)} " +
                "upd=${ms(upd / maxOf(1, n))}/${ms(updMax)} draw=${ms(drw / maxOf(1, frames))}/${ms(drwMax)} gapMax=${ms(gapMax)}",
        )
        n = 0; sim = 0; upd = 0; drw = 0; simMax = 0; updMax = 0; drwMax = 0; frames = 0; gapMax = 0; since = now
    }
}
