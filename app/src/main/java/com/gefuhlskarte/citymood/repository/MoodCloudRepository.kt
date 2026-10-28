package com.gefuhlskarte.citymood.repository

import com.gefuhlskarte.citymood.data.cloud.CloudMood
import com.google.firebase.auth.FirebaseAuth
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

    fun saveMood(
        placeName: String,
        moodLabel: String,
        emoji: String,
        valence: Float,
        arousal: Float,
        onSuccess: (String) -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onError("Trebuie să fii autentificat.")
            return
        }

        if (placeName.isBlank()) {
            onError("Introdu numele locului.")
            return
        }

        val mood = hashMapOf(
            "userId" to user.uid,
            "placeName" to placeName.trim(),
            "moodLabel" to moodLabel,
            "emoji" to emoji,
            "valence" to valence.coerceIn(-1f, 1f),
            "arousal" to arousal.coerceIn(0f, 1f),
            "createdAt" to System.currentTimeMillis()
        )

        moodsCollection()
            .add(mood)
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

                val moods = snapshot?.documents?.mapNotNull { document ->
                    val placeName = document.getString("placeName")
                        ?: return@mapNotNull null

                    CloudMood(
                        id = document.id,
                        userId = document.getString("userId") ?: "",
                        placeName = placeName,
                        moodLabel = document.getString("moodLabel") ?: "",
                        emoji = document.getString("emoji") ?: "",
                        valence = (document.get("valence") as? Number)
                            ?.toFloat() ?: 0f,
                        arousal = (document.get("arousal") as? Number)
                            ?.toFloat() ?: 0f,
                        createdAt = (document.get("createdAt") as? Number)
                            ?.toLong() ?: 0L
                    )
                } ?: emptyList()

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
}