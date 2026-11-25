package com.gefuhlskarte.citymood.repository

import com.gefuhlskarte.citymood.data.MoodEntry
import com.gefuhlskarte.citymood.data.cloud.CloudMood
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class MoodCloudRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    private fun moodsCollection() =
        db.collection("users")
            .document(auth.currentUser?.uid
                ?: throw IllegalStateException(
                    "Trebuie să fii autentificat pentru a accesa emoțiile."
                )
            )
            .collection("moods")

    /**
     * Salvează în Firestore aceeași intrare care a fost salvată în Room,
     * inclusiv metadatele de context (locație, vreme, timp, context social).
     * Câmpurile null nu sunt trimise, ca documentele să rămână curate.
     */
    fun saveMood(
        mood: MoodEntry,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onError("Trebuie să fii autentificat.")
            return
        }

        if (mood.placeName.isBlank()) {
            onError("Introdu numele locului.")
            return
        }

        val data = mutableMapOf<String, Any?>(
            "userId" to user.uid,
            "placeName" to mood.placeName.trim(),
            "moodLabel" to mood.moodLabel,
            "emoji" to mood.emoji,
            "valence" to mood.valence.coerceIn(-1f, 1f),
            "arousal" to mood.arousal.coerceIn(0f, 1f),
            "createdAt" to mood.createdAt,

            "latitude" to mood.latitude,
            "longitude" to mood.longitude,
            "temperature" to mood.temperature,
            "apparentTemperature" to mood.apparentTemperature,
            "humidity" to mood.humidity,
            "precipitation" to mood.precipitation,
            "cloudCover" to mood.cloudCover,
            "windSpeed" to mood.windSpeed,
            "weatherCode" to mood.weatherCode,
            "weatherDescription" to mood.weatherDescription,
            "isDay" to mood.isDay,
            "hourOfDay" to mood.hourOfDay,
            "timeOfDay" to mood.timeOfDay,
            "dayOfWeek" to mood.dayOfWeek,
            "isWeekend" to mood.isWeekend,
            "season" to mood.season,
            "socialContext" to mood.socialContext
        ).filterValues { it != null }

        moodsCollection()
            .add(data)
            .addOnSuccessListener { document ->
                onSuccess(document.id)
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.localizedMessage
                        ?: "Nu am putut salva emoția în cloud."
                )
            }
    }

    fun observeMoods(
        onUpdate: (List<CloudMood>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {

        if (auth.currentUser == null) {
            onError("Trebuie să fii autentificat.")
            return null
        }

        return moodsCollection()
            .orderBy("createdAt",
                com.google.firebase.firestore.Query.Direction.DESCENDING
            )
            .addSnapshotListener { snapshot, error ->

                if (error != null) {
                    onError(
                        error.localizedMessage
                            ?: "Nu am putut încărca emoțiile."
                    )
                    return@addSnapshotListener
                }

                val moods = snapshot?.documents?.mapNotNull { it.toCloudMood() }
                    ?: emptyList()

                onUpdate(moods)
            }
    }

    fun deleteMood(
        moodId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (auth.currentUser == null) {
            onError("Trebuie să fii autentificat.")
            return
        }

        moodsCollection()
            .document(moodId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener { exception ->
                onError(
                    exception.localizedMessage
                        ?: "Nu am putut șterge emoția."
                )
            }
    }

    /**
     * Ascultă stările TUTUROR utilizatorilor (toate subcolecțiile "moods"),
     * pentru calculul stării generale a fiecărui loc de pe hartă.
     * Necesită regula Firestore pentru collection group (vezi README / mesajul de instalare).
     */
    fun observeAllUsersMoods(
        onUpdate: (List<CloudMood>) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {

        if (auth.currentUser == null) {
            onError("Trebuie să fii autentificat.")
            return null
        }

        return db.collectionGroup("moods")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onError(
                        error.localizedMessage
                            ?: "Nu am putut încărca stările comunității."
                    )
                    return@addSnapshotListener
                }

                onUpdate(
                    snapshot?.documents?.mapNotNull { it.toCloudMood() } ?: emptyList()
                )
            }
    }

    private fun DocumentSnapshot.toCloudMood(): CloudMood? {
        val placeName = getString("placeName") ?: return null

        return CloudMood(
            id = id,
            userId = getString("userId") ?: "",
            placeName = placeName,
            moodLabel = getString("moodLabel") ?: "",
            emoji = getString("emoji") ?: "",
            valence = number("valence")?.toFloat() ?: 0f,
            arousal = number("arousal")?.toFloat() ?: 0f,
            createdAt = number("createdAt")?.toLong() ?: 0L,

            latitude = number("latitude")?.toDouble(),
            longitude = number("longitude")?.toDouble(),
            temperature = number("temperature")?.toDouble(),
            apparentTemperature = number("apparentTemperature")?.toDouble(),
            humidity = number("humidity")?.toInt(),
            precipitation = number("precipitation")?.toDouble(),
            cloudCover = number("cloudCover")?.toInt(),
            windSpeed = number("windSpeed")?.toDouble(),
            weatherCode = number("weatherCode")?.toInt(),
            weatherDescription = getString("weatherDescription"),
            isDay = getBoolean("isDay"),
            hourOfDay = number("hourOfDay")?.toInt(),
            timeOfDay = getString("timeOfDay"),
            dayOfWeek = number("dayOfWeek")?.toInt(),
            isWeekend = getBoolean("isWeekend"),
            season = getString("season"),
            socialContext = getString("socialContext")
        )
    }

    private fun DocumentSnapshot.number(field: String): Number? =
        get(field) as? Number
}
