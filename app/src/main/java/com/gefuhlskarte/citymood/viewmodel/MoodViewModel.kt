package com.gefuhlskarte.citymood.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.gefuhlskarte.citymood.context.LocationProvider
import com.gefuhlskarte.citymood.context.SocialContext
import com.gefuhlskarte.citymood.context.TimeContextCalculator
import com.gefuhlskarte.citymood.context.WeatherService
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
    private val repository: MoodRepository,
    private val locationProvider: LocationProvider
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

    // Stările tuturor utilizatorilor, pentru media fiecărui loc de pe hartă
    private val _communityMoods = MutableStateFlow<List<CloudMood>>(emptyList())
    val communityMoods: StateFlow<List<CloudMood>> = _communityMoods

    private val _communityStatus = MutableStateFlow("")
    val communityStatus: StateFlow<String> = _communityStatus

    private var communityListener: ListenerRegistration? = null

    fun startObservingCommunityMoods() {
        if (communityListener != null) return

        communityListener = cloudRepository.observeAllUsersMoods(
            onUpdate = { allMoods ->
                _communityMoods.value = allMoods
                _communityStatus.value = ""
            },
            onError = { error ->
                _communityStatus.value = "Could not load community moods: $error"
            }
        )
    }

    fun stopObservingCommunityMoods() {
        communityListener?.remove()
        communityListener = null
    }

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

    /**
     * Salvează starea împreună cu metadatele de context:
     * locație GPS, vreme (Open-Meteo), ora/anotimpul și cu cine e utilizatorul.
     * Dacă locația sau vremea nu sunt disponibile, starea se salvează oricum,
     * iar câmpurile respective rămân null.
     */
    fun saveMood(
        placeName: String,
        moodLabel: String,
        emoji: String,
        valence: Float,
        arousal: Float,
        socialContext: SocialContext?,
        onResult: (String) -> Unit
    ) {
        if (placeName.isBlank()) {
            onResult("Please enter a place name.")
            return
        }

        viewModelScope.launch {
            try {
                onResult("Collecting location and weather...")

                val createdAt = System.currentTimeMillis()

                // 1. Locația (sau motivul pentru care lipsește)
                val locationResult = locationProvider.getCurrentLocation()
                val location = locationResult.locationOrNull()

                // 2. Vremea (doar dacă avem locație)
                val weather = location?.let {
                    WeatherService.fetchCurrent(it.latitude, it.longitude)
                }

                // 3. Timpul și anotimpul, calculate local
                val time = TimeContextCalculator.from(createdAt, location?.latitude)

                val entry = MoodEntry(
                    placeName = placeName.trim(),
                    moodLabel = moodLabel,
                    emoji = emoji,
                    valence = valence.coerceIn(-1f, 1f),
                    arousal = arousal.coerceIn(0f, 1f),
                    createdAt = createdAt,

                    latitude = location?.latitude,
                    longitude = location?.longitude,

                    temperature = weather?.temperature,
                    apparentTemperature = weather?.apparentTemperature,
                    humidity = weather?.humidity,
                    precipitation = weather?.precipitation,
                    cloudCover = weather?.cloudCover,
                    windSpeed = weather?.windSpeed,
                    weatherCode = weather?.weatherCode,
                    weatherDescription = weather?.weatherDescription,
                    isDay = weather?.isDay,

                    hourOfDay = time.hourOfDay,
                    timeOfDay = time.timeOfDay,
                    dayOfWeek = time.dayOfWeek,
                    isWeekend = time.isWeekend,
                    season = time.season,

                    socialContext = socialContext?.key
                )

                // 4. Salvăm mai întâi în Room.
                repository.saveMood(entry)

                val note = when {
                    location == null ->
                        " (without location and weather: ${locationResult.reason()})"
                    weather == null ->
                        " (without weather: could not reach Open-Meteo)"
                    else -> ""
                }

                onResult("Mood saved locally$note. Syncing with cloud...")

                // 5. Apoi în Firestore.
                cloudRepository.saveMood(
                    mood = entry,
                    onSuccess = {
                        onResult("Mood saved locally and synced online$note.")
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
        stopObservingCommunityMoods()
        super.onCleared()
    }
}

class MoodViewModelFactory(
    private val repository: MoodRepository,
    private val locationProvider: LocationProvider
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(MoodViewModel::class.java)) {
            return MoodViewModel(repository, locationProvider) as T
        }

        throw IllegalArgumentException(
            "Unknown ViewModel class: ${modelClass.name}"
        )
    }
}
