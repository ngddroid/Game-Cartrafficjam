package com.puzzel.lab.cartrafficjam.game.modes

import com.puzzel.lab.cartrafficjam.game.model.JamColor

/**
 * A small pixel picture. [cells] holds a [JamColor] ordinal per cell, or -1 where the cell is
 * empty. [background] is the color Sand Sweep fills the empty cells with.
 */
class PixelPicture(val name: String, val w: Int, val h: Int, val cells: IntArray, val background: Int) {
    fun at(x: Int, y: Int) = cells[y * w + x]

    fun mirrored() = PixelPicture(name, w, h, IntArray(w * h) { i -> cells[(i / w) * w + (w - 1 - i % w)] }, background)

    /** The same picture with every color swapped through [map] (a permutation of 0..7). */
    fun recolored(map: IntArray) = PixelPicture(name, w, h, IntArray(w * h) { i -> cells[i].let { if (it < 0) -1 else map[it] } }, map[background])

    /** Every color that appears, background included when [withBackground]. */
    fun colors(withBackground: Boolean): List<Int> {
        val set = cells.filter { it >= 0 }.toMutableSet()
        if (withBackground) set += background
        return set.sorted()
    }
}

/**
 * Original pixel pictures for Pixel Crowd and Sand Sweep, drawn in the game's eight rider
 * colors. Letters: R red, B blue, G green, Y yellow, P pink, V purple, O orange, C cyan.
 */
object PixelPictures {

    private fun pic(name: String, bg: Char, vararg rows: String): PixelPicture {
        val w = rows.maxOf { it.length }
        val h = rows.size
        val cells = IntArray(w * h) { i ->
            val row = rows[i / w]
            val ch = if (i % w < row.length) row[i % w] else '.'
            code(ch)
        }
        return PixelPicture(name, w, h, cells, code(bg))
    }

    private fun code(c: Char) = when (c) {
        'R' -> JamColor.RED.ordinal
        'B' -> JamColor.BLUE.ordinal
        'G' -> JamColor.GREEN.ordinal
        'Y' -> JamColor.YELLOW.ordinal
        'P' -> JamColor.PINK.ordinal
        'V' -> JamColor.PURPLE.ordinal
        'O' -> JamColor.ORANGE.ordinal
        'C' -> JamColor.CYAN.ordinal
        else -> -1
    }

    val all: List<PixelPicture> = listOf(
        pic(
            "Heart", 'C',
            "..RRRR....RRRR..",
            ".RPPPPR..RPPPPR.",
            "RPPYYPPRRPPPPPPR",
            "RPYYPPPPPPPPPPPR",
            "RPYPPPPPPPPPPPPR",
            "RPPPPPPPPPPPPPPR",
            ".RPPPPPPPPPPPPR.",
            "..RPPPPPPPPPPR..",
            "...RPPPPPPPPR...",
            "....RPPPPPPR....",
            ".....RPPPPR.....",
            "......RPPR......",
            ".......RR.......",
        ),
        pic(
            "Apple", 'C',
            ".......GG.......",
            "......GG........",
            "....RRRGRRRR....",
            "..RRRRRRRRRRRR..",
            ".RRYRRRRRRRRRRR.",
            ".RYYRRRRRRRRRRR.",
            "RRYRRRRRRRRRRRRR",
            "RRRRRRRRRRRRRRRR",
            "RRRRRRRRRRRRRRRR",
            "RRRRRRRRRRRRRRRR",
            ".RRRRRRRRRRRRRR.",
            ".RRRRRRRRRRRRRR.",
            "..RRRRRRRRRRRR..",
            "...RRRRRRRRRR...",
            "....RRR..RRR....",
        ),
        pic(
            "Fish", 'Y',
            "........CCCCC.......",
            "......CCCCCCCCC...BB",
            "....CCCCCCCCCCCC.BBB",
            "...CCYBCCBCCBCCCBBBB",
            "..CCCBBCCBCCBCCCCBBB",
            "..CCCCCCCBCCBCCCCBBB",
            "...CCCCCCBCCBCCCBBBB",
            "....CCCCCCCCCCCC.BBB",
            "......CCCCCCCCC...BB",
            "........CCCCC.......",
        ),
        pic(
            "Balloon", 'C',
            "....RRRRRR....",
            "..RRRRRRRRRR..",
            ".RRYYRRRRRRRR.",
            ".RYYRRRRRRRRR.",
            "RRYRRRRRRRRRRR",
            "RRRRRRRRRRRRRR",
            "RRRRRRRRRRRRRR",
            ".RRRRRRRRRRRR.",
            ".RRRRRRRRRRRR.",
            "..RRRRRRRRRR..",
            "...RRRRRRRR...",
            ".....RRRR.....",
            "......OO......",
            "......B.......",
            ".......B......",
            "......B.......",
            ".....OOOO.....",
            ".....OOOO.....",
        ),
        pic(
            "Pine", 'C',
            "........Y........",
            ".......YYY.......",
            "......GGGGG......",
            ".....GGGRGGG.....",
            "....GGGGGGGGG....",
            "......GGGGG......",
            ".....GGGGRGGG....",
            "....GGGGGGGGGG...",
            "...GGRGGGGGGGGG..",
            ".....GGGGGGG.....",
            "....GGGGGGGRGG...",
            "...GGGGGGGGGGGG..",
            "..GGGGRGGGGGGGGG.",
            ".GGGGGGGGGGGGRGGG",
            ".......OOO.......",
            ".......OOO.......",
            "......OOOOO......",
        ),
        pic(
            "Melon", 'C',
            "GGGGGGGGGGGGGGGGGGGG",
            "GYYYYYYYYYYYYYYYYYYG",
            ".GYRRRRRRRRRRRRRRYG.",
            ".GYRRBRRRRRRBRRRRYG.",
            "..GYRRRRRBRRRRRRYG..",
            "..GYRRRRRRRRRRBRYG..",
            "...GYRRBRRRRRRRYG...",
            "....GYRRRRRBRRYG....",
            ".....GYRRRRRRYG.....",
            "......GYRRRRYG......",
            ".......GYRRYG.......",
            "........GYYG........",
            ".........GG.........",
        ),
        pic(
            "Sun", 'C',
            "........Y........",
            "...Y....Y....Y...",
            "....Y...Y...Y....",
            ".....OOOOOOO.....",
            "....OOOOOOOOO....",
            "...OOYYYYYYYOO...",
            "YY.OOYBYYYBYOO.YY",
            "...OOYYYYYYYOO...",
            "...OOYYRRRYYOO...",
            "...OOYYYRYYYOO...",
            "....OOOOOOOOO....",
            ".....OOOOOOO.....",
            "....Y...Y...Y....",
            "...Y....Y....Y...",
            "........Y........",
        ),
        pic(
            "Mushroom", 'C',
            "......RRRRRR......",
            "....RRYYRRRRRR....",
            "...RRYYYRRRRRRR...",
            "..RRRYYRRRRYYRRR..",
            "..RRRRRRRRRYYYRR..",
            ".RRYYRRRRRRRYYRRR.",
            ".RYYYYRRRRRRRRRRR.",
            ".RRYYRRRRRYYRRRRR.",
            "..RRRRRRRYYYYRRR..",
            "....OOOOOOOOOO....",
            ".....OOOOOOOOO....",
            ".....OOBOOOBOO....",
            ".....OOOOOOOOO....",
            ".....OOOPPOOOO....",
            "....GGOOOOOOOGG...",
            "..GGGGGGGGGGGGGG..",
        ),
        pic(
            "Cactus", 'Y',
            "......GG......",
            ".....GGGG.....",
            ".....GPGG.....",
            ".G...GGGG.....",
            "GGG..GGGG..G..",
            "GGG..GGPG.GGG.",
            "GPG..GGGG.GGG.",
            "GGGGGGGGG.GPG.",
            ".GGGGGGGGGGGG.",
            ".....GGGGGGG..",
            ".....GGGG.....",
            ".....GPGG.....",
            ".....GGGG.....",
            "...OOOOOOOO...",
            "...ORRRRRRO...",
            "....OOOOOO....",
            "....ORRRRO....",
            "....OOOOOO....",
        ),
        pic(
            "Flower", 'B',
            "......PPPPP......",
            "....PPPPPPPPP....",
            "...PPPPPPPPPPP...",
            "..PPPPPYYYPPPPP..",
            "..PPPPYYYYYPPPP..",
            ".PPPPYYOOOYYPPPP.",
            ".PPPPYYOOOYYPPPP.",
            ".PPPPYYOOOYYPPPP.",
            "..PPPPYYYYYPPPP..",
            "..PPPPPYYYPPPPP..",
            "...PPPPPGPPPPP...",
            "....PPPPGPPPP....",
            "........G........",
            "....GGGGG.GGG....",
            "...GGG..GGGG.....",
            "........GG.......",
            "........GG.......",
        ),
        pic(
            "Rocket", 'V',
            ".......R.......",
            "......RRR......",
            ".....RRRRR.....",
            ".....CCCCC.....",
            "....CCCCCCC....",
            "....CCBBBCC....",
            "....CBBYBBC....",
            "....CBYYYBC....",
            "....CCBBBCC....",
            "....CCCCCCC....",
            "....CCCCCCC....",
            "...RCCCCCCCR...",
            "..RRCCCCCCCRR..",
            ".RRRCCCCCCCRRR.",
            ".RR.CCCCCCC.RR.",
            ".R...OOOOO...R.",
            ".....OYYYO.....",
            "......YYY......",
            "......OYO......",
            ".......O.......",
        ),
        pic(
            "Ice Cream", 'C',
            "....PPPPPP....",
            "..PPPPPPPPPP..",
            ".PPVPPPPPVPPP.",
            ".PPPPPPVPPPPP.",
            "PPPPVPPPPPPVPP",
            "PPPPPPPPPPPPPP",
            ".YYYYYYYYYYYY.",
            "..OOYOOYOOYOO.",
            "..OYOOYOOYOO..",
            "...OOYOOYOOO..",
            "...OYOOYOOYO..",
            "....OOYOOYO...",
            ".....OYOOY....",
            ".....OOYOO....",
            "......OYO.....",
            "......OO......",
        ),
        pic(
            "Owl", 'C',
            "..O..........O..",
            "..OO........OO..",
            "..OOOOOOOOOOOO..",
            ".OOOOOOOOOOOOOO.",
            ".OYYYYOOOOYYYYO.",
            ".OYBBYOOOOYBBYO.",
            ".OYBBYOROOYBBYO.",
            ".OYYYYORROYYYYO.",
            ".OOOOOORROOOOOO.",
            ".OOPPPOOOOPPPOO.",
            ".OPPPPPPPPPPPPO.",
            ".OPPPPPPPPPPPPO.",
            ".OOPPPPPPPPPPOO.",
            "..OOPPPPPPPPOO..",
            "...OOOOOOOOOO...",
            "....YY....YY....",
        ),
        pic(
            "Butterfly", 'G',
            ".VVV.........VVV.",
            "VPPPV.......VPPPV",
            "VPYPPV.....VPPYPV",
            "VPPPPPV.B.VPPPPPV",
            ".VPPPPVBBBVPPPPV.",
            "..VVPPVBBBVPPVV..",
            "...VVVVBBBVVVV...",
            "..VCCCVBBBVCCCV..",
            ".VCCYCVBBBVCYCCV.",
            ".VCCCCV.B.VCCCCV.",
            "..VVVV.....VVVV..",
        ),
        pic(
            "Kitty", 'C',
            "..O............O..",
            ".OOO..........OOO.",
            ".OPOO........OOPO.",
            ".OPPOOOOOOOOOOPPO.",
            ".OOOOOOOOOOOOOOOO.",
            "OOOOOOOOOOOOOOOOOO",
            "OOOGGGOOOOOOGGGOOO",
            "OOOGBGOOOOOOGBGOOO",
            "OOOGGGOOOOOOGGGOOO",
            "OOOOOOOOPPOOOOOOOO",
            "YYOOOOOOPPOOOOOOYY",
            "OOOYYOOOOOOOOYYOOO",
            ".OOOOOOROOROOOOOO.",
            ".OOOOOOORROOOOOOO.",
            "..OOOOOOOOOOOOOO..",
            "....OOOOOOOOOO....",
        ),
        pic(
            "Rainbow", 'P',
            "......RRRRRRRR......",
            "....RROOOOOOOORR....",
            "...ROOYYYYYYYYOOR...",
            "..ROYYGGGGGGGGYYOR..",
            ".ROYGGCCCCCCCCGGYOR.",
            ".ROYGCCBBBBBBCCGYOR.",
            "ROYGCBBVVVVVVBBCGYOR",
            "ROYGCBV......VBCGYOR",
            "ROYGCBV......VBCGYOR",
            "ROYGCBV......VBCGYOR",
            "ROYGCBV......VBCGYOR",
        ),
    )

    /**
     * Pictures cycle with the level. The first lap is as drawn, the second mirrored, and every
     * lap after that also swaps the colors around, so a returning picture looks new.
     */
    fun forLevel(n: Int): PixelPicture {
        val i = (n - 1).coerceAtLeast(0)
        val lap = i / all.size
        var p = all[i % all.size]
        if (lap % 2 == 1) p = p.mirrored()
        if (lap >= 2) {
            val map = (0 until 8).shuffled(kotlin.random.Random(lap * 131 + i % all.size)).toIntArray()
            p = p.recolored(map)
        }
        return p
    }
}
