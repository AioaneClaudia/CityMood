package com.gefuhlskarte.citymood.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gefuhlskarte.citymood.data.MoodEntry
import com.gefuhlskarte.citymood.repository.MoodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MoodViewModel(
    private val repository: MoodRepository
) : ViewModel() {

    val moods = repository.allMoods.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun saveMood(
        placeName: String,
        moodLabel: String,
        emoji: String,
        valence: Float,
        arousal: Float,
        onResult: (String) -> Unit
    ) {
        if (placeName.isBlank()) {
            onResult("Please enter a place name.")
            return
        }

        viewModelScope.launch {
            try {
                repository.saveMood(
                    MoodEntry(
                        placeName = placeName.trim(),
                        moodLabel = moodLabel,
                        emoji = emoji,
                        valence = valence.coerceIn(-1f, 1f),
                        arousal = arousal.coerceIn(0f, 1f)
                    )
                )
                onResult("Mood saved successfully!")
            } catch (exception: Exception) {
                onResult("Could not save mood. Please try again.")
            }
        }
    }
}

class MoodViewModelFactory(
    private val repository: MoodRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(MoodViewModel::class.java)) {
            return MoodViewModel(repository) as T
        }
        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
