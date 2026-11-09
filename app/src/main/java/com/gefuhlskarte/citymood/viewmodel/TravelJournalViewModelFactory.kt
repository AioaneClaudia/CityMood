
package com.gefuhlskarte.citymood.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.gefuhlskarte.citymood.repository.TravelJournalRepository

class TravelJournalViewModelFactory(
    private val repository: TravelJournalRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(TravelJournalViewModel::class.java)) {
            return TravelJournalViewModel(repository) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
