package com.puzzel.lab.cartrafficjam.game.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import com.puzzel.lab.cartrafficjam.game.model.Level
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

const val SLOT_COUNT = 7
const val VIP_SLOT = SLOT_COUNT

/**
 * Pixel geometry of the game screen, derived from the canvas size. Everything scales with [u],
 * one 360th of the screen width.
 */
class GameLayout(
    val width: Float,
    val height: Float,
    val topInset: Float,
    val bottomInset: Float,
    level: Level,
) {
    val u = width / 360f

    // Scene (station + queue)
    val sceneBottom = topInset + 168 * u
    val queueY = sceneBottom - 16 * u
    val queueColumnTop = topInset + 70 * u
    val queueStartX = 22 * u
    val queueEndX = width - 20 * u
    val passengerSpacing = 15.5f * u
    val passengerW = 15 * u
    val passengerH = 27 * u

    // Parking slots
    val slotsTop = sceneBottom
    val slotH = 68 * u
    val slotsBottom = slotsTop + slotH + 8 * u
    private val slotW = 38 * u
    private val slotGap = 3.5f * u
    private val slotsStart = width - (SLOT_COUNT * slotW + (SLOT_COUNT - 1) * slotGap) - 8 * u

    fun slotRect(i: Int): Rect = if (i == VIP_SLOT) {
        Rect(7 * u, slotsTop + 4 * u, slotsStart - 6 * u, slotsTop + 4 * u + slotH)
    } else {
        val x = slotsStart + i * (slotW + slotGap)
        Rect(x, slotsTop + 4 * u, x + slotW, slotsTop + 4 * u + slotH)
    }

    /** Unit size vehicles are drawn at once parked. */
    val slotUnit = min((slotH - 6 * u) / 5f, slotW / 2.3f)

    // Road
    val roadTop = slotsBottom
    val roadBottom = roadTop + 36 * u
    val roadY = (roadTop + roadBottom) / 2

    // Lot
    val boosterBarH = 104 * u
    val lotRect = Rect(10 * u, roadBottom + 8 * u, width - 10 * u, height - bottomInset - boosterBarH)

    /** The loop vehicles follow from the lot edge up to the road. Its top edge is the road. */
    val track = Rect(lotRect.left + 16 * u, roadY, lotRect.right - 16 * u, lotRect.bottom - 14 * u)

    // Board transform
    val cols = level.cols
    val rows = level.rows
    val rotationDeg = level.rotation
    private val rad = Math.toRadians(level.rotation.toDouble())
    val cosR = cos(rad).toFloat()
    val sinR = sin(rad).toFloat()
    val unit: Float
    val boardCenter: Offset

    init {
        val bbW = cols * abs(cosR) + rows * abs(sinR)
        val bbH = cols * abs(sinR) + rows * abs(cosR)
        val availW = lotRect.width - 2 * 18 * u
        val availH = lotRect.height - 2 * 16 * u
        unit = min(min(availW / bbW, availH / bbH), 19 * u)
        boardCenter = Offset(lotRect.center.x, lotRect.center.y + 4 * u)
    }

    fun boardToScreen(gx: Float, gy: Float): Offset {
        val lx = (gx - cols / 2f) * unit
        val ly = (gy - rows / 2f) * unit
        return Offset(boardCenter.x + lx * cosR - ly * sinR, boardCenter.y + lx * sinR + ly * cosR)
    }

    fun screenToBoard(p: Offset): Offset {
        val dx = p.x - boardCenter.x
        val dy = p.y - boardCenter.y
        val lx = dx * cosR + dy * sinR
        val ly = -dx * sinR + dy * cosR
        return Offset(lx / unit + cols / 2f, ly / unit + rows / 2f)
    }

    /** Point along the queue snake, [s] pixels from the front. */
    fun queuePoint(s: Float): Offset {
        val row = queueEndX - queueStartX
        return if (s <= row) {
            Offset(queueStartX + s, queueY)
        } else {
            Offset(queueEndX, queueY - (s - row))
        }
    }

    val queueVisibleLength = (queueEndX - queueStartX) + (queueY - queueColumnTop)

    // HUD anchors (pixels)
    val queueSign = Offset(70 * u, topInset + 88 * u)
}
