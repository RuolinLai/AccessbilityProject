package com.emma_zara.accessbilityproject.color

/** A color with a human-friendly name. */
data class NamedColor(val name: String, val color: RgbColor) {
    val lab: Lab = color.toLab()
}

/**
 * Color names people actually use for clothes.
 * To add a color, add a line here: the app automatically picks the closest one.
 */
object ColorNames {
    val all: List<NamedColor> = listOf(
        NamedColor("Black", RgbColor(0, 0, 0)),
        NamedColor("Charcoal", RgbColor(54, 69, 79)),
        NamedColor("Dark Gray", RgbColor(90, 90, 90)),
        NamedColor("Gray", RgbColor(128, 128, 128)),
        NamedColor("Light Gray", RgbColor(200, 200, 200)),
        NamedColor("White", RgbColor(255, 255, 255)),
        NamedColor("Ivory", RgbColor(255, 255, 240)),
        NamedColor("Cream", RgbColor(255, 253, 208)),
        NamedColor("Beige", RgbColor(222, 204, 170)),
        NamedColor("Tan", RgbColor(210, 180, 140)),
        NamedColor("Camel", RgbColor(193, 154, 107)),
        NamedColor("Khaki", RgbColor(195, 176, 145)),
        NamedColor("Stone", RgbColor(170, 162, 150)),
        NamedColor("Taupe", RgbColor(120, 104, 90)),
        NamedColor("Mocha", RgbColor(112, 78, 58)),
        NamedColor("Brown", RgbColor(139, 69, 19)),
        NamedColor("Chocolate", RgbColor(92, 51, 23)),
        NamedColor("Rosewood", RgbColor(112, 70, 72)),
        NamedColor("Maroon", RgbColor(128, 0, 0)),
        NamedColor("Burgundy", RgbColor(128, 0, 32)),
        NamedColor("Dark Red", RgbColor(170, 20, 20)),
        NamedColor("Red", RgbColor(255, 0, 0)),
        NamedColor("Crimson", RgbColor(220, 20, 60)),
        NamedColor("Coral", RgbColor(255, 127, 80)),
        NamedColor("Salmon", RgbColor(250, 128, 114)),
        NamedColor("Peach", RgbColor(255, 203, 164)),
        NamedColor("Dusty Rose", RgbColor(196, 136, 140)),
        NamedColor("Blush", RgbColor(242, 205, 205)),
        NamedColor("Light Pink", RgbColor(255, 182, 193)),
        NamedColor("Pink", RgbColor(255, 140, 180)),
        NamedColor("Hot Pink", RgbColor(255, 20, 147)),
        NamedColor("Magenta", RgbColor(255, 0, 255)),
        NamedColor("Plum", RgbColor(142, 69, 133)),
        NamedColor("Purple", RgbColor(128, 0, 128)),
        NamedColor("Violet", RgbColor(143, 0, 255)),
        NamedColor("Mauve", RgbColor(176, 138, 168)),
        NamedColor("Lavender", RgbColor(200, 180, 230)),
        NamedColor("Eggplant", RgbColor(92, 70, 112)),
        NamedColor("Dusty Purple", RgbColor(130, 105, 150)),
        NamedColor("Indigo", RgbColor(75, 0, 130)),
        NamedColor("Navy", RgbColor(0, 0, 128)),
        NamedColor("Dark Blue", RgbColor(0, 0, 180)),
        NamedColor("Blue", RgbColor(0, 0, 255)),
        NamedColor("Royal Blue", RgbColor(65, 105, 225)),
        NamedColor("Denim", RgbColor(21, 96, 189)),
        NamedColor("Slate", RgbColor(84, 98, 114)),
        NamedColor("Slate Gray", RgbColor(112, 128, 144)),
        NamedColor("Steel Blue", RgbColor(70, 130, 180)),
        NamedColor("Powder Blue", RgbColor(176, 210, 230)),
        NamedColor("Sky Blue", RgbColor(135, 206, 235)),
        NamedColor("Light Blue", RgbColor(173, 216, 230)),
        NamedColor("Turquoise", RgbColor(64, 224, 208)),
        NamedColor("Cyan", RgbColor(0, 255, 255)),
        NamedColor("Teal", RgbColor(0, 128, 128)),
        NamedColor("Mint", RgbColor(170, 240, 200)),
        NamedColor("Lime", RgbColor(50, 205, 50)),
        NamedColor("Green", RgbColor(0, 160, 0)),
        NamedColor("Dark Green", RgbColor(0, 100, 0)),
        NamedColor("Hunter Green", RgbColor(60, 105, 85)),
        NamedColor("Moss", RgbColor(108, 108, 72)),
        NamedColor("Sage", RgbColor(156, 175, 136)),
        NamedColor("Olive Green", RgbColor(85, 107, 47)),
        NamedColor("Olive", RgbColor(128, 128, 0)),
        NamedColor("Mustard", RgbColor(225, 173, 1)),
        NamedColor("Yellow", RgbColor(255, 255, 0)),
        NamedColor("Light Yellow", RgbColor(255, 250, 160)),
        NamedColor("Gold", RgbColor(255, 200, 0)),
        NamedColor("Orange", RgbColor(255, 140, 0)),
        NamedColor("Rust", RgbColor(183, 65, 14)),
    )

    /** Finds the name whose color looks most similar (smallest Delta E). */
    fun nearest(color: RgbColor): NamedColor {
        val lab = color.toLab()
        return all.minBy { deltaE(lab, it.lab) }
    }
}
