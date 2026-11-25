package com.gefuhlskarte.citymood.context

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/** Vremea din momentul înregistrării stării. */
data class WeatherSnapshot(
    val temperature: Double?,          // °C
    val apparentTemperature: Double?,  // °C, temperatura resimțită
    val humidity: Int?,                // %
    val precipitation: Double?,        // mm în ultima oră
    val cloudCover: Int?,              // %
    val windSpeed: Double?,            // km/h
    val weatherCode: Int?,             // cod WMO
    val weatherDescription: String?,   // ex. "rain", "clear"
    val isDay: Boolean?
)

/**
 * Preia vremea curentă de la Open-Meteo (https://open-meteo.com).
 * Nu are nevoie de cheie API și e gratuit pentru uz necomercial / cercetare.
 * Folosește doar HttpURLConnection + org.json, deci nu adaugă dependențe noi.
 */
object WeatherService {

    private const val BASE_URL = "https://api.open-meteo.com/v1/forecast"

    private val CURRENT_FIELDS = listOf(
        "temperature_2m",
        "apparent_temperature",
        "relative_humidity_2m",
        "precipitation",
        "cloud_cover",
        "wind_speed_10m",
        "weather_code",
        "is_day"
    ).joinToString(",")

    /** Întoarce null dacă nu e internet sau cererea eșuează — salvarea stării nu se blochează. */
    suspend fun fetchCurrent(latitude: Double, longitude: Double): WeatherSnapshot? =
        withContext(Dispatchers.IO) {
            var connection: HttpURLConnection? = null
            try {
                val url = String.format(
                    Locale.US,
                    "%s?latitude=%.4f&longitude=%.4f&current=%s&timezone=auto",
                    BASE_URL, latitude, longitude, CURRENT_FIELDS
                )

                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 8_000
                    readTimeout = 8_000
                }

                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext null
                }

                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val current = JSONObject(body).getJSONObject("current")

                val code = current.optIntOrNull("weather_code")

                WeatherSnapshot(
                    temperature = current.optDoubleOrNull("temperature_2m"),
                    apparentTemperature = current.optDoubleOrNull("apparent_temperature"),
                    humidity = current.optIntOrNull("relative_humidity_2m"),
                    precipitation = current.optDoubleOrNull("precipitation"),
                    cloudCover = current.optIntOrNull("cloud_cover"),
                    windSpeed = current.optDoubleOrNull("wind_speed_10m"),
                    weatherCode = code,
                    weatherDescription = code?.let { describeWeatherCode(it) },
                    isDay = current.optIntOrNull("is_day")?.let { it == 1 }
                )
            } catch (e: Exception) {
                null
            } finally {
                connection?.disconnect()
            }
        }

    /** Grupează codurile WMO în categorii ușor de folosit în analiză. */
    fun describeWeatherCode(code: Int): String = when (code) {
        0 -> "clear"
        1, 2 -> "partly_cloudy"
        3 -> "overcast"
        45, 48 -> "fog"
        in 51..57 -> "drizzle"
        in 61..67, in 80..82 -> "rain"
        in 71..77, 85, 86 -> "snow"
        in 95..99 -> "thunderstorm"
        else -> "unknown"
    }

    fun emojiFor(description: String?): String = when (description) {
        "clear" -> "☀️"
        "partly_cloudy" -> "⛅"
        "overcast" -> "☁️"
        "fog" -> "🌫️"
        "drizzle" -> "🌦️"
        "rain" -> "🌧️"
        "snow" -> "❄️"
        "thunderstorm" -> "⛈️"
        else -> "🌡️"
    }

    private fun JSONObject.optDoubleOrNull(name: String): Double? =
        if (has(name) && !isNull(name)) optDouble(name).takeUnless { it.isNaN() } else null

    private fun JSONObject.optIntOrNull(name: String): Int? =
        if (has(name) && !isNull(name)) optInt(name) else null
}
