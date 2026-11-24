package com.gefuhlskarte.citymood.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "travel_journal")
data class TravelJournalEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val placeName: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val description: String,
    val photoUri: String? = null,
    val facialEmotion: String? = null,
    val textEmotion: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val pageColor: String = "#FFFFF8E7",
    val textBackgroundColor: String = "#FFFFE8C2",
    val fontStyle: String = "SANS_SERIF",

    // true = față detectată; false = nicio față; null = neanalizată sau eroare
    val containsFace: Boolean? = null
)