package com.gefuhlskarte.citymood.context

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

data class GeoLocation(val latitude: Double, val longitude: Double)

/** Rezultatul unei citiri de locație: fie coordonatele, fie motivul pentru care lipsesc. */
sealed class LocationResult {
    data class Success(val location: GeoLocation) : LocationResult()
    object NoPermission : LocationResult()
    object LocationDisabled : LocationResult()
    object Unavailable : LocationResult()

    fun locationOrNull(): GeoLocation? = (this as? Success)?.location

    /** Text scurt pentru mesajul afișat utilizatorului. */
    fun reason(): String? = when (this) {
        is Success -> null
        NoPermission -> "location permission not granted"
        LocationDisabled -> "location is turned off on the device"
        Unavailable -> "no GPS fix yet"
    }
}

/**
 * Preia locația curentă a telefonului (o singură citire, nu urmărire continuă).
 * Încearcă pe rând: Google Fused Location (citire nouă), ultima locație Fused,
 * apoi ultima locație cunoscută de Android (GPS / rețea), ca să meargă și pe
 * emulatoare sau telefoane fără Google Play Services.
 */
class LocationProvider(context: Context) {

    private val appContext = context.applicationContext
    private val fusedClient = LocationServices.getFusedLocationProviderClient(appContext)
    private val locationManager =
        appContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(
            appContext, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(
                    appContext, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

    fun isLocationEnabled(): Boolean =
        LocationManagerCompat.isLocationEnabled(locationManager)

    suspend fun getCurrentLocation(timeoutMs: Long = 15_000): LocationResult {
        if (!hasPermission()) return LocationResult.NoPermission
        if (!isLocationEnabled()) return LocationResult.LocationDisabled

        val location = withTimeoutOrNull(timeoutMs) { fusedCurrentLocation() }
            ?: fusedLastLocation()
            ?: systemLastKnownLocation()

        return location
            ?.let { LocationResult.Success(GeoLocation(it.latitude, it.longitude)) }
            ?: LocationResult.Unavailable
    }

    @SuppressLint("MissingPermission")
    private suspend fun fusedCurrentLocation(): Location? =
        suspendCancellableCoroutine { continuation ->
            val cancellationToken = CancellationTokenSource()
            try {
                fusedClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    cancellationToken.token
                )
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resume(null) }
            } catch (e: Exception) {
                continuation.resume(null)
            }
            continuation.invokeOnCancellation { cancellationToken.cancel() }
        }

    @SuppressLint("MissingPermission")
    private suspend fun fusedLastLocation(): Location? =
        withTimeoutOrNull(3_000) {
            suspendCancellableCoroutine { continuation ->
                try {
                    fusedClient.lastLocation
                        .addOnSuccessListener { continuation.resume(it) }
                        .addOnFailureListener { continuation.resume(null) }
                } catch (e: Exception) {
                    continuation.resume(null)
                }
            }
        }

    /** Varianta fără Google Play Services: cea mai recentă locație știută de Android. */
    @SuppressLint("MissingPermission")
    private fun systemLastKnownLocation(): Location? =
        try {
            locationManager.getProviders(true)
                .mapNotNull { provider -> locationManager.getLastKnownLocation(provider) }
                .maxByOrNull { it.time }
        } catch (e: Exception) {
            null
        }
}
