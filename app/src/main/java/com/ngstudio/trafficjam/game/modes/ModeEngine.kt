package com.ngstudio.trafficjam.game.modes

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import com.ngstudio.trafficjam.game.engine.GameFx
import com.ngstudio.trafficjam.game.engine.Particles
import com.ngstudio.trafficjam.game.model.Difficulty
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

enum class ModePhase { PLAYING, STUCK, WON }

sealed interface ModeEvent {
    data class Won(val stars: Int) : ModeEvent
    data object Stuck : ModeEvent
    data class Toast(val message: String) : ModeEvent
    /** The player tapped a spot that opens after a rewarded ad. */
    data class LockedSpot(val index: Int) : ModeEvent
}

/**
 * Shared frame loop and plumbing for the Pixel Crowd and Sand Sweep engines. Subclasses own
 * their state, lay it out in [layout], advance it in [step] and paint it in [draw].
 */
abstract class ModeEngine(val levelNumber: Int, protected val fx: GameFx) {
    var frame by mutableLongStateOf(0L)
        private set
    var phase by mutableStateOf(ModePhase.PLAYING)
        protected set
    var ready by mutableStateOf(false)
        private set
    /** [counter] mirrored into Compose state so the HUD recomposes when it changes. */
    var hudCounter by mutableIntStateOf(-1)
        private set
    var onEvent: (ModeEvent) -> Unit = {}

    var width = 0f
        private set
    var height = 0f
        private set
    var topInset = 0f
        private set
    var bottomInset = 0f
        private set
    /** One 360th of the screen width; every size scales with it. */
    var u = 1f
        private set
    lateinit var particles: Particles
        private set

    protected var time = 0f
    protected var revives = 0
    private var shake = 0f

    abstract val difficulty: Difficulty

    /** Big number in the HUD counter and the word under it. */
    abstract val counter: Int
    abstract val counterLabel: String

    abstract val reviveTitle: String
    abstract val reviveMessage: String

    val screenShake: Offset
        get() = if (shake <= 0f) Offset.Zero else Offset(sin(time * 90f) * 3f * u * shake, cos(time * 77f) * 2f * u * shake)

    fun setSize(w: Float, h: Float, top: Float, bottom: Float) {
        if (ready && w == width && h == height && top == topInset && bottom == bottomInset) return
        width = w
        height = h
        topInset = top
        bottomInset = bottom
        u = w / 360f
        if (!::particles.isInitialized) particles = Particles(u)
        layout()
        ready = true
    }

    fun update(dtRaw: Float) {
        if (!ready) return
        val dt = min(dtRaw, 1 / 30f)
        time += dt
        shake = (shake - dt * 4f).coerceAtLeast(0f)
        particles.update(dt)
        step(dt)
        val c = counter
        if (c != hudCounter) hudCounter = c
        frame++
    }

    protected fun shakeScreen(amount: Float = 0.5f) { shake = amount }

    protected fun win() {
        if (phase == ModePhase.WON) return
        phase = ModePhase.WON
        particles.confetti(width, topInset, 140)
        onEvent(ModeEvent.Won(starsFor(revives)))
    }

    protected fun stuck() {
        if (phase != ModePhase.PLAYING) return
        phase = ModePhase.STUCK
        fx.sfx(com.ngstudio.trafficjam.core.audio.Sfx.Warning)
        onEvent(ModeEvent.Stuck)
    }

    /** Called after the player pays for a second chance. */
    fun revive() {
        revives++
        onRevive()
        if (phase == ModePhase.STUCK) phase = ModePhase.PLAYING
    }

    /** Called after the rewarded ad for a locked spot. */
    open fun unlockSpot(index: Int) = Unit

    protected abstract fun layout()
    protected abstract fun step(dt: Float)
    protected abstract fun onRevive()
    abstract fun onTap(p: Offset)
    abstract fun draw(scope: DrawScope, t: Float)

    companion object {
        fun starsFor(revives: Int) = when (revives) {
            0 -> 3
            1 -> 2
            else -> 1
        }
    }
}

/** Small drawing helpers shared by the mode renderers. */
object ModeDraw {
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
    }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        typeface = Typeface.DEFAULT_BOLD
        textAlign = Paint.Align.CENTER
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
    }

    /** Centered bold text with an optional outline, baseline-corrected around [c]. */
    fun DrawScope.label(text: String, c: Offset, size: Float, color: Color = Color.White, outline: Color? = null, outlineW: Float = size * 0.22f) {
        val y = c.y + size * 0.36f
        if (outline != null) {
            stroke.textSize = size
            stroke.strokeWidth = outlineW
            stroke.color = outline.toArgb()
            drawContext.canvas.nativeCanvas.drawText(text, c.x, y, stroke)
        }
        fill.textSize = size
        fill.color = color.toArgb()
        drawContext.canvas.nativeCanvas.drawText(text, c.x, y, fill)
    }

    /** A path through [pts] with each corner rounded off by up to [radius]. */
    fun roundedPath(pts: List<Offset>, radius: Float): Path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size - 1) {
            val a = pts[i - 1]
            val b = pts[i]
            val c = pts[i + 1]
            val inLen = (b - a).getDistance().coerceAtLeast(1e-3f)
            val outLen = (c - b).getDistance().coerceAtLeast(1e-3f)
            val r = min(radius, min(inLen, outLen) / 2)
            val p1 = b - (b - a) / inLen * r
            val p2 = b + (c - b) / outLen * r
            lineTo(p1.x, p1.y)
            quadraticTo(b.x, b.y, p2.x, p2.y)
        }
        lineTo(pts.last().x, pts.last().y)
    }

    fun headingVec(deg: Float): Offset {
        val r = deg * PI.toFloat() / 180f
        return Offset(sin(r), -cos(r))
    }

    fun easeOutBack(t: Float): Float {
        val c1 = 1.70158f
        val c3 = c1 + 1
        val x = t - 1
        return 1 + c3 * x * x * x + c1 * x * x
    }

    fun shortestAngle(from: Float, to: Float): Float {
        var d = (to - from) % 360f
        if (d > 180f) d -= 360f
        if (d < -180f) d += 360f
        return d
    }
}
