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

    val createdAt: Long = System.currentTimeMillis()
)
