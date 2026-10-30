
package com.gefuhlskarte.citymood.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gefuhlskarte.citymood.data.MoodEntry
import com.gefuhlskarte.citymood.data.cloud.CloudMood
import com.gefuhlskarte.citymood.repository.MoodCloudRepository
import com.gefuhlskarte.citymood.repository.MoodRepository
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MoodViewModel(
    private val repository: MoodRepository
) : ViewModel() {

    private val cloudRepository = MoodCloudRepository()

    // Lista locală din Room
    val moods = repository.allMoods.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // Lista online din Firestore
    private val _cloudMoods = MutableStateFlow<List<CloudMood>>(emptyList())
    val cloudMoods: StateFlow<List<CloudMood>> = _cloudMoods

    // Mesaje pentru sincronizarea online
    private val _cloudStatus = MutableStateFlow("Cloud: not connected")
    val cloudStatus: StateFlow<String> = _cloudStatus

    private var cloudListener: ListenerRegistration? = null

    fun startObservingCloudMoods() {
        // Evită înregistrarea mai multor listeners
        if (cloudListener != null) return

        cloudListener = cloudRepository.observeMoods(
            onUpdate = { onlineMoods ->
                _cloudMoods.value = onlineMoods
                _cloudStatus.value = "Online moods updated."
            },
            onError = { error ->
                _cloudStatus.value = "Cloud error: $error"
            }
        )
    }

    fun stopObservingCloudMoods() {
        cloudListener?.remove()
        cloudListener = null
    }

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
                val cleanPlaceName = placeName.trim()
                val safeValence = valence.coerceIn(-1f, 1f)
                val safeArousal = arousal.coerceIn(0f, 1f)

                // 1. Salvăm mai întâi în Room.
                repository.saveMood(
                    MoodEntry(
                        placeName = cleanPlaceName,
                        moodLabel = moodLabel,
                        emoji = emoji,
                        valence = safeValence,
                        arousal = safeArousal
                    )
                )

                // Confirmăm salvarea locală fără a aștepta internetul.
                onResult("Mood saved locally. Syncing with cloud...")

                // 2. Încercăm salvarea în Firestore.
                cloudRepository.saveMood(
                    placeName = cleanPlaceName,
                    moodLabel = moodLabel,
                    emoji = emoji,
                    valence = safeValence,
                    arousal = safeArousal,
                    onSuccess = {
                        onResult("Mood saved locally and synced online.")
                    },
                    onError = { error ->
                        onResult(
                            "Mood saved locally, but cloud sync failed: $error"
                        )
                    }
                )
            } catch (exception: Exception) {
                onResult(
                    "Could not save mood locally: " +
                            (exception.localizedMessage ?: "Unknown error")
                )
            }
        }
    }

    override fun onCleared() {
        stopObservingCloudMoods()
        super.onCleared()
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
