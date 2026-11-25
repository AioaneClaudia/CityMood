package com.gefuhlskarte.citymood.data.cloud

data class CloudMood(
    val id: String = "",
    val userId: String = "",
    val placeName: String = "",
    val moodLabel: String = "",
    val emoji: String = "",
    val valence: Float = 0f,
    val arousal: Float = 0f,
    val createdAt: Long = 0L,

    // Metadate de context
    val latitude: Double? = null,
    val longitude: Double? = null,
    val temperature: Double? = null,
    val apparentTemperature: Double? = null,
    val humidity: Int? = null,
    val precipitation: Double? = null,
    val cloudCover: Int? = null,
    val windSpeed: Double? = null,
    val weatherCode: Int? = null,
    val weatherDescription: String? = null,
    val isDay: Boolean? = null,
    val hourOfDay: Int? = null,
    val timeOfDay: String? = null,
    val dayOfWeek: Int? = null,
    val isWeekend: Boolean? = null,
    val season: String? = null,
    val socialContext: String? = null
)
