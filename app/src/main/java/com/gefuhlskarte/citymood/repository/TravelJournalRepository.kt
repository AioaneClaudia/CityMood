package com.gefuhlskarte.citymood.repository

import com.gefuhlskarte.citymood.data.TravelJournalDao
import com.gefuhlskarte.citymood.data.TravelJournalEntry
import kotlinx.coroutines.flow.Flow

class TravelJournalRepository(
    private val dao: TravelJournalDao
) {

    val allEntries: Flow<List<TravelJournalEntry>> =
        dao.getAllEntries()

    suspend fun saveEntry(entry: TravelJournalEntry) {
        dao.insertEntry(entry)
    }

    suspend fun updateEntry(entry: TravelJournalEntry) {
        dao.updateEntry(entry)
    }

    suspend fun deleteEntry(entry: TravelJournalEntry) {
        dao.deleteEntry(entry)
    }
}