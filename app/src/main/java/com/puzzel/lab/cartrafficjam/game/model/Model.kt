package com.puzzel.lab.cartrafficjam.game.model

enum class Dir(val dx: Int, val dy: Int, val degrees: Float) {
    N(0, -1, 0f), E(1, 0, 90f), S(0, 1, 180f), W(-1, 0, 270f);

    val horizontal get() = dx != 0
}

/** Sizes are in grid units; every vehicle is [WIDTH] units wide. */
enum class VehicleType(val length: Int, val seats: Int) {
    CAR(3, 4), VAN(4, 6), BUS(5, 10);

    companion object {
        const val WIDTH = 2
    }
}

enum class BoosterType(val title: String, val description: String, val price: Int) {
    SHUFFLE("Shuffle", "Repaint the vehicles in the lot to match who's waiting next.", 80),
    CRANE("Crane", "Lift any vehicle, even a blocked one, straight to the VIP spot.", 60),
    SORT("Sort", "Line up the passengers so they match the parked vehicles.", 80),
}

enum class Difficulty(val label: String?) { NORMAL(null), HARD("HARD LEVEL"), SUPER_HARD("SUPER HARD") }

/** Passenger and vehicle colors; [hex] is the base body color. */
enum class JamColor(val hex: Long) {
    RED(0xFFF2453D), BLUE(0xFF2F7CF6), GREEN(0xFF3DBE4B), YELLOW(0xFFFFC21A),
    PINK(0xFFFF6FB5), PURPLE(0xFF9B5CF0), ORANGE(0xFFFF8A1F), CYAN(0xFF23C9D9),
}

/**
 * A vehicle on the grid. (x, y) is its top-left unit; it covers [VehicleType.length] units along
 * its heading axis and [VehicleType.WIDTH] across it.
 */
data class VehicleSpec(
    val id: Int,
    val type: VehicleType,
    val color: Int,
    val dir: Dir,
    val x: Int,
    val y: Int,
    val mystery: Boolean = false,
) {
    val w get() = if (dir.horizontal) type.length else VehicleType.WIDTH
    val h get() = if (dir.horizontal) VehicleType.WIDTH else type.length

    inline fun forEachCell(block: (Int, Int) -> Unit) {
        for (yy in y until y + h) for (xx in x until x + w) block(xx, yy)
    }

    /** Centre of the footprint in grid units. */
    val centerX get() = x + w / 2f
    val centerY get() = y + h / 2f
}

data class Level(
    val number: Int,
    val cols: Int,
    val rows: Int,
    val mask: BooleanArray,
    val vehicles: List<VehicleSpec>,
    val queue: IntArray,
    val rotation: Float,
    val difficulty: Difficulty,
    val colorCount: Int,
    val openSlots: Int = 4,
    val lockedSlots: Int = 3,
) {
    fun inMask(x: Int, y: Int) = x in 0 until cols && y in 0 until rows && mask[y * cols + x]
    override fun equals(other: Any?) = this === other
    override fun hashCode() = number
}
