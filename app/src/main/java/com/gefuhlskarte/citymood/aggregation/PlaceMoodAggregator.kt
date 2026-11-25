package com.gefuhlskarte.citymood.aggregation

import com.gefuhlskarte.citymood.data.cloud.CloudMood
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Starea generală a unui loc, calculată din stările tuturor utilizatorilor. */
data class PlaceMoodStats(
    val count: Int,
    val averageValence: Float,              // -1..+1
    val averageArousal: Float,              // 0..1
    val dominantMood: String,               // eticheta cea mai frecventă, ex. "Happy"
    val dominantEmoji: String,
    val moodCounts: List<Pair<String, Int>> // ex. [("Happy", 5), ("Calm", 3)], descrescător
)

object PlaceMoodAggregator {

    /** Stările salvate la cel mult această distanță de un loc contează pentru acel loc. */
    const val DEFAULT_RADIUS_METERS = 100.0

    /**
     * Calculează media stărilor salvate în jurul unui punct.
     * Întoarce null dacă nu există nicio stare cu GPS în rază.
     * Stările fără coordonate (salvate înainte de actualizare sau fără permisiune) sunt ignorate.
     */
    fun statsFor(
        latitude: Double,
        longitude: Double,
        moods: List<CloudMood>,
        radiusMeters: Double = DEFAULT_RADIUS_METERS
    ): PlaceMoodStats? {
        val nearby = moods.filter { mood ->
            val lat = mood.latitude
            val lon = mood.longitude
            lat != null && lon != null &&
                    distanceMeters(latitude, longitude, lat, lon) <= radiusMeters
        }

        return statsOf(nearby)
    }

    /** Media unui grup de stări. Întoarce null pentru o listă goală. */
    fun statsOf(nearby: List<CloudMood>): PlaceMoodStats? {
        if (nearby.isEmpty()) return null

        val counts = nearby
            .groupBy { it.moodLabel }
            .map { (label, list) -> label to list }
            // la egalitate câștigă starea înregistrată cel mai recent
            .sortedWith(
                compareByDescending<Pair<String, List<CloudMood>>> { it.second.size }
                    .thenByDescending { pair -> pair.second.maxOf { it.createdAt } }
            )

        val dominant = counts.first()

        return PlaceMoodStats(
            count = nearby.size,
            averageValence = nearby.map { it.valence }.average().toFloat(),
            averageArousal = nearby.map { it.arousal }.average().toFloat(),
            dominantMood = dominant.first,
            dominantEmoji = dominant.second.first().emoji,
            moodCounts = counts.map { it.first to it.second.size }
        )
    }

    /** Un loc nou, apărut din stările utilizatorilor, care nu e pe lista de locuri a hărții. */
    data class MoodCluster(
        val latitude: Double,
        val longitude: Double,
        val name: String,
        val moods: List<CloudMood>
    )

    /**
     * Grupează stările care NU sunt în raza niciunui loc deja afișat pe hartă.
     * Stările aflate la cel mult [radiusMeters] una de alta formează un singur loc,
     * iar numele lui este numele scris cel mai des de utilizatori.
     */
    fun clusterUnmatched(
        moods: List<CloudMood>,
        knownPlaces: List<Pair<Double, Double>>,
        radiusMeters: Double = DEFAULT_RADIUS_METERS
    ): List<MoodCluster> {
        val unmatched = moods.filter { mood ->
            val lat = mood.latitude ?: return@filter false
            val lon = mood.longitude ?: return@filter false
            knownPlaces.none { (pLat, pLon) ->
                distanceMeters(pLat, pLon, lat, lon) <= radiusMeters
            }
        }.sortedBy { it.createdAt }

        // Grupare simplă: fiecare stare intră în primul grup aflat în rază,
        // altfel pornește un grup nou. Centrul grupului e media coordonatelor.
        val groups = mutableListOf<MutableList<CloudMood>>()
        for (mood in unmatched) {
            val lat = mood.latitude!!
            val lon = mood.longitude!!
            val group = groups.firstOrNull { g ->
                distanceMeters(
                    g.map { it.latitude!! }.average(),
                    g.map { it.longitude!! }.average(),
                    lat, lon
                ) <= radiusMeters
            }
            if (group != null) group += mood else groups += mutableListOf(mood)
        }

        return groups.map { group ->
            val name = group
                .map { it.placeName.trim() }
                .filter { it.isNotEmpty() }
                .groupBy { it.lowercase() }
                .maxByOrNull { it.value.size }
                ?.value?.first()
                ?: "Loc fără nume"

            MoodCluster(
                latitude = group.map { it.latitude!! }.average(),
                longitude = group.map { it.longitude!! }.average(),
                name = name,
                moods = group
            )
        }
    }

    /** Distanța pe suprafața Pământului (formula haversine), în metri. */
    fun distanceMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)
        return earthRadius * 2 * atan2(sqrt(a), sqrt(1 - a))
    }
}
