package com.gefuhlskarte.citymood.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gefuhlskarte.citymood.data.TravelJournalEntry
import com.gefuhlskarte.citymood.repository.TravelJournalRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class TravelJournalViewModel(
    private val repository: TravelJournalRepository
) : ViewModel() {

    val entries = repository.allEntries.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun saveEntry(
        placeName: String,
        description: String,
        photoUri: String?,
        pageColor: String = "#FFFFF8E7",
        textBackgroundColor: String = "#FFFFE8C2",
        fontStyle: String = "SANS_SERIF",
        onResult: (String) -> Unit
    ) {
        if (placeName.isBlank()) {
            onResult("Introdu numele locației.")
            return
        }

        if (description.isBlank()) {
            onResult("Introdu o descriere a experienței.")
            return
        }

        viewModelScope.launch {
            try {
                val entry = TravelJournalEntry(
                    placeName = placeName.trim(),
                    description = description.trim(),
                    photoUri = photoUri,
                    pageColor = pageColor,
                    textBackgroundColor = textBackgroundColor,
                    fontStyle = fontStyle
                )

                repository.saveEntry(entry)

                onResult("Experiența a fost salvată!")
            } catch (exception: Exception) {
                onResult(
                    "Eroare la salvare: ${
                        exception.localizedMessage
                            ?: "Eroare necunoscută"
                    }"
                )
            }
        }
    }

    fun updatePageStyle(
        entry: TravelJournalEntry,
        pageColor: String,
        textBackgroundColor: String,
        fontStyle: String,
        onResult: (String) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                repository.updateEntry(
                    entry.copy(
                        pageColor = pageColor,
                        textBackgroundColor = textBackgroundColor,
                        fontStyle = fontStyle
                    )
                )

                onResult("Personalizarea a fost salvată!")
            } catch (exception: Exception) {
                onResult(
                    "Nu am putut salva personalizarea: ${
                        exception.localizedMessage
                            ?: "Eroare necunoscută"
                    }"
                )
            }
        }
    }

    fun deleteEntry(entry: TravelJournalEntry) {
        viewModelScope.launch {
            try {
                repository.deleteEntry(entry)
            } catch (_: Exception) {
                // Păstrăm aplicația funcțională dacă ștergerea eșuează.
            }
        }
    }
}