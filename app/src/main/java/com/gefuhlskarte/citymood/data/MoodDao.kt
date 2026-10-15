package com.gefuhlskarte.citymood.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {

    @Insert
    suspend fun insertMood(mood: MoodEntry)

    @Query("""
        SELECT * FROM mood_entries
        ORDER BY createdAt DESC
    """)
    fun getAllMoods(): Flow<List<MoodEntry>>

    @Query("DELETE FROM mood_entries WHERE id = :moodId")
    suspend fun deleteMood(moodId: Long)
}
