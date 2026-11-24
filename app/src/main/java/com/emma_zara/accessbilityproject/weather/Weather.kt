package com.emma_zara.accessbilityproject.weather

import kotlin.math.roundToInt

/** How the temperature feels, for choosing clothes. */
enum class TempFeel(val label: String) {
    FREEZING("Freezing"),
    COLD("Cold"),
    COOL("Cool"),
    MILD("Mild"),
    HOT("Hot"),
}

/**
 * The weather right now, from Open-Meteo.
 * Temperatures are in °C, [precipitation] in mm, [windSpeed] in km/h.
 * [code] is the WMO weather code (0 = clear sky, 61 = rain, 71 = snow, ...).
 */
data class Weather(
    val temperature: Double,
    val feelsLike: Double,
    val code: Int,
    val precipitation: Double,
    val windSpeed: Double,
    val isDay: Boolean,
) {
    /** Based on the "feels like" temperature, since wind and humidity change what you need to wear. */
    val feel: TempFeel
        get() = when {
            feelsLike < 0 -> TempFeel.FREEZING
            feelsLike < 10 -> TempFeel.COLD
            feelsLike < 17 -> TempFeel.COOL
            feelsLike < 25 -> TempFeel.MILD
            else -> TempFeel.HOT
        }

    val rainy: Boolean get() = code in 51..67 || code in 80..82 || code in 95..99 || (precipitation > 0.2 && !snowy)
    val snowy: Boolean get() = code in 71..77 || code in 85..86
    val windy: Boolean get() = windSpeed >= 30
    val sunny: Boolean get() = isDay && code <= 1

    /** In words, like "Light rain". */
    val description: String
        get() = when (code) {
            0 -> if (isDay) "Clear sky" else "Clear night"
            1 -> "Mostly clear"
            2 -> "Partly cloudy"
            3 -> "Cloudy"
            45, 48 -> "Fog"
            51, 53, 55 -> "Drizzle"
            56, 57 -> "Freezing drizzle"
            61 -> "Light rain"
            63 -> "Rain"
            65 -> "Heavy rain"
            66, 67 -> "Freezing rain"
            71 -> "Light snow"
            73 -> "Snow"
            75 -> "Heavy snow"
            77 -> "Snow grains"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Unknown weather"
        }

    /** One sentence to show or read aloud, like "3°C, feels like -1°C. Light rain." */
    fun summary(): String {
        val t = temperature.roundToInt()
        val f = feelsLike.roundToInt()
        val feels = if (f != t) ", feels like $f°C" else ""
        return "$t°C$feels. $description."
    }

    /** What to wear for this weather, in plain words. */
    fun tips(): List<String> = buildList {
        add(
            when (feel) {
                TempFeel.FREEZING -> "Freezing: wear your warmest coat, plus a hat, scarf and gloves."
                TempFeel.COLD -> "Cold: wear warm pieces and a jacket or coat."
                TempFeel.COOL -> "Cool: bring a light jacket or sweater."
                TempFeel.MILD -> "Mild: comfortable for most clothes, no jacket needed."
                TempFeel.HOT -> "Hot: wear light, thin clothes. Light colors stay cooler than dark ones in the sun."
            },
        )
        if (rainy) add("Rain: take an umbrella or a waterproof jacket, and shoes that can get wet.")
        if (snowy) add("Snow: wear boots with good grip.")
        if (windy) add("Windy: a jacket that closes helps, and loose pieces may blow around.")
        if (sunny && feel >= TempFeel.MILD) add("Sunny: sunglasses and a hat help.")
    }
}
