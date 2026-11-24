package com.emma_zara.accessbilityproject.weather

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.math.roundToLong

/** A place to get the weather for. */
data class Place(val name: String, val latitude: Double, val longitude: Double)

/**
 * Gets the current weather from Open-Meteo (free, no account or key needed) for a place the user chose.
 *
 * Privacy: only the place's coordinates, rounded to about 1 km, are sent. The chosen place is saved
 * on the phone so the user doesn't have to choose it every time.
 */
class WeatherRepository(context: Context) {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("weather", Context.MODE_PRIVATE)

    var place: Place? by mutableStateOf(loadPlace())
        private set
    var weather: Weather? by mutableStateOf(null)
        private set
    var loading by mutableStateOf(false)
        private set
    var error: String? by mutableStateOf(null)
        private set
    private var fetchedAt = 0L

    fun choosePlace(newPlace: Place?) {
        place = newPlace
        weather = null
        error = null
        fetchedAt = 0
        val edit = prefs.edit()
        if (newPlace == null) {
            edit.clear()
        } else {
            edit.putString("name", newPlace.name)
                .putFloat("lat", newPlace.latitude.toFloat())
                .putFloat("lon", newPlace.longitude.toFloat())
        }
        edit.apply()
    }

    private fun loadPlace(): Place? {
        val name = prefs.getString("name", null) ?: return null
        return Place(name, prefs.getFloat("lat", 0f).toDouble(), prefs.getFloat("lon", 0f).toDouble())
    }

    /** Gets the weather for the chosen place, unless it was fetched in the last 15 minutes (or [force]). */
    suspend fun refresh(force: Boolean = false) {
        val p = place ?: return
        if (!force && weather != null && System.currentTimeMillis() - fetchedAt < 15 * 60_000) return
        loading = true
        error = null
        try {
            weather = withContext(Dispatchers.IO) { fetchWeather(p) }
            fetchedAt = System.currentTimeMillis()
        } catch (e: Exception) {
            Log.w("WeatherRepository", "Couldn't get the weather", e)
            error = "Couldn't get the weather. Check the internet connection and try again."
        } finally {
            loading = false
        }
    }

    /** Finds places by name, like "Toronto". */
    suspend fun searchPlaces(query: String): List<Place> = withContext(Dispatchers.IO) {
        val url = "https://geocoding-api.open-meteo.com/v1/search?count=6&language=en&format=json&name=" +
            URLEncoder.encode(query.trim(), "UTF-8")
        val results = JSONObject(get(url)).optJSONArray("results") ?: return@withContext emptyList()
        (0 until results.length()).map { i ->
            val o = results.getJSONObject(i)
            // Like "Toronto, Ontario, Canada"
            val name = listOf(o.optString("name"), o.optString("admin1"), o.optString("country"))
                .filter { it.isNotBlank() }.distinct().joinToString(", ")
            Place(name, o.getDouble("latitude"), o.getDouble("longitude"))
        }
    }

    /** True if the user allowed the app to know the phone's rough location. */
    fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.ACCESS_COARSE_LOCATION) ==
            android.content.pm.PackageManager.PERMISSION_GRANTED

    /** The phone's rough location, or null if it can't be found (location turned off, no signal...). */
    @SuppressLint("MissingPermission") // checked with hasLocationPermission() first
    suspend fun currentLocation(): Place? {
        if (!hasLocationPermission()) return null
        val manager = appContext.getSystemService(LocationManager::class.java) ?: return null
        val providers = listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            .filter { runCatching { manager.isProviderEnabled(it) }.getOrDefault(false) }
        // A location from the last hour is close enough for the weather
        val recent = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .filter { System.currentTimeMillis() - it.time < 60 * 60_000 }
            .maxByOrNull { it.time }
        val location = recent ?: providers.firstOrNull { it != LocationManager.PASSIVE_PROVIDER }?.let { provider ->
            withTimeoutOrNull(20_000) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val signal = androidx.core.os.CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    LocationManagerCompat.getCurrentLocation(manager, provider, signal, ContextCompat.getMainExecutor(appContext)) {
                        if (cont.isActive) cont.resume(it)
                    }
                }
            }
        } ?: providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }.maxByOrNull { it.time }
        return location?.let { Place("Your location", it.latitude, it.longitude) }
    }

    private fun fetchWeather(p: Place): Weather {
        val url = String.format(
            Locale.US,
            "https://api.open-meteo.com/v1/forecast?latitude=%.2f&longitude=%.2f&timezone=auto" +
                "&current=temperature_2m,apparent_temperature,weather_code,precipitation,wind_speed_10m,is_day",
            round2(p.latitude), round2(p.longitude),
        )
        val current = JSONObject(get(url)).getJSONObject("current")
        return Weather(
            temperature = current.getDouble("temperature_2m"),
            feelsLike = current.getDouble("apparent_temperature"),
            code = current.getInt("weather_code"),
            precipitation = current.optDouble("precipitation", 0.0),
            windSpeed = current.optDouble("wind_speed_10m", 0.0),
            isDay = current.optInt("is_day", 1) == 1,
        )
    }

    /** Rounds to 2 decimals (about 1 km), so the exact location is never sent. */
    private fun round2(x: Double) = (x * 100).roundToLong() / 100.0

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        try {
            c.connectTimeout = 10_000
            c.readTimeout = 10_000
            if (c.responseCode !in 200..299) throw IOException("HTTP ${c.responseCode}")
            return c.inputStream.bufferedReader().use { it.readText() }
        } finally {
            c.disconnect()
        }
    }
}
