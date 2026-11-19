package com.emma_zara.accessbilityproject.color

enum class Temperature(val label: String) { WARM("Warm"), COOL("Cool"), NEUTRAL("Neutral") }
enum class Lightness(val label: String) { LIGHT("Light"), MEDIUM("Medium"), DARK("Dark") }
/** How colorful it is (saturation). */
enum class Saturation(val label: String) { VIVID("Vivid"), MUTED("Muted"), GRAYISH("Grayish") }

/** Everything the app says about one color. */
data class ColorAnalysis(
    val color: RgbColor,
    val name: String,
    val hsv: Hsv,
    val lab: Lab,
    val cmyk: Cmyk,
    val temperature: Temperature,
    val lightness: Lightness,
    val saturation: Saturation,
    val complementary: RgbColor,
    val analogous: List<RgbColor>,
    val triadic: List<RgbColor>,
    val description: String,
)

/** Rotates the hue around the color wheel, keeping saturation and brightness. */
fun RgbColor.rotateHue(degrees: Float): RgbColor {
    val hsv = toHsv()
    return RgbColor.fromHsv(hsv.h + degrees, hsv.s, hsv.v)
}

fun analyzeColor(color: RgbColor): ColorAnalysis {
    val hsv = color.toHsv()
    val lab = color.toLab()

    // Very gray or very dark colors don't really have a hue, so they are "neutral".
    val temperature = when {
        hsv.s < 0.15f || hsv.v < 0.15f -> Temperature.NEUTRAL
        hsv.h < 75f || hsv.h >= 300f -> Temperature.WARM   // reds, oranges, yellows, magentas
        else -> Temperature.COOL                             // greens, blues, purples
    }

    // Lab lightness matches what our eyes see (yellow looks lighter than blue at the same "value").
    val lightness = when {
        lab.l >= 70 -> Lightness.LIGHT
        lab.l <= 35 -> Lightness.DARK
        else -> Lightness.MEDIUM
    }

    val saturation = when {
        hsv.s < 0.15f -> Saturation.GRAYISH
        hsv.s < 0.5f -> Saturation.MUTED
        else -> Saturation.VIVID
    }

    val name = ColorNames.nearest(color).name
    val complementary = color.rotateHue(180f)

    val description = buildString {
        append("$name is a ${lightness.label.lowercase()}, ${saturation.label.lowercase()}, ")
        append("${temperature.label.lowercase()} color. ")
        if (temperature == Temperature.NEUTRAL) {
            append("Neutral colors match almost anything, so they are easy to wear with bright pieces.")
        } else {
            append("Its complementary (opposite) color is ${ColorNames.nearest(complementary).name}, ")
            append("which makes it stand out. For a calm look, pair it with similar colors next to it on the wheel.")
        }
    }

    return ColorAnalysis(
        color = color,
        name = name,
        hsv = hsv,
        lab = lab,
        cmyk = color.toCmyk(),
        temperature = temperature,
        lightness = lightness,
        saturation = saturation,
        complementary = complementary,
        analogous = listOf(color.rotateHue(-30f), color.rotateHue(30f)),
        triadic = listOf(color.rotateHue(120f), color.rotateHue(240f)),
        description = description,
    )
}
