package com.gefuhlskarte.citymood.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val placeName: String,
    val moodLabel: String,
    val emoji: String,

    // Normalized valence: -1.0 (unpleasant) to +1.0 (pleasant)
    val valence: Float,

    // Arousal: 0.0 (calm) to 1.0 (highly activated)
    val arousal: Float,

    val createdAt: Long = System.currentTimeMillis(),

    // ---------- Metadate de context (versiunea 5 a bazei de date) ----------
    // Toate sunt nullable: dacă lipsește permisiunea de locație sau internetul,
    // starea se salvează oricum, iar valoarea lipsă rămâne null (nu 0!),
    // ca să nu strice analiza statistică.

    // Locație GPS
    val latitude: Double? = null,
    val longitude: Double? = null,

    // Vreme (Open-Meteo)
    val temperature: Double? = null,
    val apparentTemperature: Double? = null,
    val humidity: Int? = null,
    val precipitation: Double? = null,
    val cloudCover: Int? = null,
    val windSpeed: Double? = null,
    val weatherCode: Int? = null,
    val weatherDescription: String? = null,
    val isDay: Boolean? = null,

    // Timp (calculat local)
    val hourOfDay: Int? = null,
    val timeOfDay: String? = null,
    val dayOfWeek: Int? = null,
    val isWeekend: Boolean? = null,
    val season: String? = null,

    // Cu cine este utilizatorul (vezi SocialContext.key)
    val socialContext: String? = null
)
