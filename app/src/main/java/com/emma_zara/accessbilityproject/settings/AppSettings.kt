package com.emma_zara.accessbilityproject.settings

import android.content.Context

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
)

/** Saves settings on the phone (SharedPreferences) so they are kept after the app closes. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    fun load(): AppSettings = AppSettings(
        theme = enumOrDefault(prefs.getString("theme", null), AppTheme.LIGHT),
        fontScale = prefs.getFloat("fontScale", 1f),
        bold = prefs.getBoolean("bold", false),
        casing = enumOrDefault(prefs.getString("casing", null), TextCasing.NORMAL),
    )

    fun save(settings: AppSettings) {
        prefs.edit()
            .putString("theme", settings.theme.name)
            .putFloat("fontScale", settings.fontScale)
            .putBoolean("bold", settings.bold)
            .putString("casing", settings.casing.name)
            .apply()
    }

    private inline fun <reified T : Enum<T>> enumOrDefault(name: String?, default: T): T =
        enumValues<T>().firstOrNull { it.name == name } ?: default
}
