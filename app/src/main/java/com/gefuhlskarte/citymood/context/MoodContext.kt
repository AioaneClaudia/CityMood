package com.gefuhlskarte.citymood.context

import java.util.Calendar
import java.util.TimeZone

/**
 * Cu cine este utilizatorul în momentul în care își înregistrează starea.
 * `key` este valoarea salvată în baze de date (stabilă, în engleză, ușor de analizat),
 * `label` și `emoji` sunt doar pentru interfață.
 */
enum class SocialContext(val key: String, val label: String, val emoji: String) {
    ALONE("alone", "Alone", "🧍"),
    PARTNER("partner", "Partner", "💑"),
    FRIENDS("friends", "Friends", "👯"),
    FAMILY("family", "Family", "👨‍👩‍👧"),
    COLLEAGUES("colleagues", "Colleagues", "💼"),
    STRANGERS("strangers", "Crowd / strangers", "👥");

    companion object {
        fun fromKey(key: String?): SocialContext? =
            entries.firstOrNull { it.key == key }
    }
}

/** Metadate de timp calculate local, fără niciun API. */
data class TimeContext(
    val hourOfDay: Int,       // 0..23
    val timeOfDay: String,    // night / morning / afternoon / evening
    val dayOfWeek: Int,       // 1 = luni ... 7 = duminică (ISO)
    val isWeekend: Boolean,
    val season: String        // winter / spring / summer / autumn
)

object TimeContextCalculator {

    /**
     * @param latitude dacă e negativă (emisfera sudică) anotimpurile se inversează.
     */
    fun from(
        timestamp: Long,
        latitude: Double? = null,
        timeZone: TimeZone = TimeZone.getDefault()
    ): TimeContext {
        val calendar = Calendar.getInstance(timeZone).apply {
            timeInMillis = timestamp
        }

        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val month = calendar.get(Calendar.MONTH) + 1 // 1..12

        // Calendar: SUNDAY = 1 ... SATURDAY = 7  ->  ISO: MONDAY = 1 ... SUNDAY = 7
        val calendarDay = calendar.get(Calendar.DAY_OF_WEEK)
        val isoDay = if (calendarDay == Calendar.SUNDAY) 7 else calendarDay - 1

        return TimeContext(
            hourOfDay = hour,
            timeOfDay = timeOfDay(hour),
            dayOfWeek = isoDay,
            isWeekend = isoDay >= 6,
            season = season(month, southernHemisphere = (latitude ?: 0.0) < 0)
        )
    }

    fun timeOfDay(hour: Int): String = when (hour) {
        in 5..11 -> "morning"
        in 12..16 -> "afternoon"
        in 17..21 -> "evening"
        else -> "night"
    }

    /** Anotimpuri meteorologice (pe luni întregi), standardul folosit în climatologie. */
    fun season(month: Int, southernHemisphere: Boolean = false): String {
        val northern = when (month) {
            12, 1, 2 -> "winter"
            3, 4, 5 -> "spring"
            6, 7, 8 -> "summer"
            else -> "autumn"
        }
        if (!southernHemisphere) return northern
        return when (northern) {
            "winter" -> "summer"
            "summer" -> "winter"
            "spring" -> "autumn"
            else -> "spring"
        }
    }
}
