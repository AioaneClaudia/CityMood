package com.gefuhlskarte.citymood.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TravelJournalDao {

    @Query(
        "SELECT * FROM travel_journal ORDER BY createdAt DESC"
    )
    fun getAllEntries(): Flow<List<TravelJournalEntry>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEntry(entry: TravelJournalEntry)

    @Update
    suspend fun updateEntry(entry: TravelJournalEntry)

    @Delete
    suspend fun deleteEntry(entry: TravelJournalEntry)

    @Query("DELETE FROM travel_journal WHERE id = :entryId")
    suspend fun deleteEntryById(entryId: Long)
}