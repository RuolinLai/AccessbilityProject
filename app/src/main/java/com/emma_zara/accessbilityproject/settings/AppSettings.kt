package com.emma_zara.accessbilityproject.settings

import android.content.Context
import com.emma_zara.accessbilityproject.color.ColorVision

enum class AppTheme(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    HIGH_CONTRAST("High contrast (yellow on black)"),
}

enum class TextCasing(val label: String) {
    NORMAL("Normal"),
    UPPER("UPPERCASE"),
    LOWER("lowercase");

    fun apply(text: String): String = when (this) {
        NORMAL -> text
        UPPER -> text.uppercase()
        LOWER -> text.lowercase()
    }
}

/** All the accessibility settings the user can change. */
data class AppSettings(
    val theme: AppTheme = AppTheme.LIGHT,
    val fontScale: Float = 1f,   // 1.0 = normal size, 2.0 = double size
    val bold: Boolean = false,
    val casing: TextCasing = TextCasing.NORMAL,
    val animations: Boolean = true,   // moving effects; can be turned off if motion bothers you
    /** How different (Lab color distance) a new color must be to play the paint splash. */
    val splashDifference: Int = DEFAULT_SPLASH_DIFFERENCE,
    /** The user's kind of color vision, so the app can warn about colors that may look alike to them. */
    val colorVision: ColorVision = ColorVision.TYPICAL,
)

const val DEFAULT_SPLASH_DIFFERENCE = 30
const val MIN_SPLASH_DIFFERENCE = 5
const val MAX_SPLASH_DIFFERENCE = 80

/** Saves settings on the phone (SharedPreferences) so they are kept after the app closes. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        theme = enumOrDefault(prefs.getString("theme", null), AppTheme.LIGHT),
        fontScale = prefs.getFloat("fontScale", 1f),
        bold = prefs.getBoolean("bold", false),
        casing = enumOrDefault(prefs.getString("casing", null), TextCasing.NORMAL),
        animations = prefs.getBoolean("animations", true),
        splashDifference = prefs.getInt("splashDifference", DEFAULT_SPLASH_DIFFERENCE)
            .coerceIn(MIN_SPLASH_DIFFERENCE, MAX_SPLASH_DIFFERENCE),
        colorVision = enumOrDefault(prefs.getString("colorVision", null), ColorVision.TYPICAL),
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString("theme", settings.theme.name)
            .putFloat("fontScale", settings.fontScale)
            .putBoolean("bold", settings.bold)
            .putString("casing", settings.casing.name)
            .putBoolean("animations", settings.animations)
            .putInt("splashDifference", settings.splashDifference)
            .putString("colorVision", settings.colorVision.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
