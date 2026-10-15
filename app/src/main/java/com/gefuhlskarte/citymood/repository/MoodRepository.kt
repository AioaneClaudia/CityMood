package com.gefuhlskarte.citymood.repository

import com.gefuhlskarte.citymood.data.MoodDao
import com.gefuhlskarte.citymood.data.MoodEntry
import kotlinx.coroutines.flow.Flow

class MoodRepository(
    private val moodDao: MoodDao
) {
    val allMoods: Flow<List<MoodEntry>> =
        moodDao.getAllMoods()

    suspend fun saveMood(mood: MoodEntry) {
        moodDao.insertMood(mood)
    }

    suspend fun deleteMood(id: Long) {
        moodDao.deleteMood(id)
    }
}
