package com.gefuhlskarte.citymood.data.cloud

data class CloudMood(
    val id: String = "",
    val userId: String = "",
    val placeName: String = "",
    val moodLabel: String = "",
    val emoji: String = "",
    val valence: Float = 0f,
    val arousal: Float = 0f,
    val createdAt: Long = 0L
)