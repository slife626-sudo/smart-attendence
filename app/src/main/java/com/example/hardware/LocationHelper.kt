package com.example.hardware

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracyMeters: Float
)

object LocationHelper {
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(context: Context): Result<LocationPoint> {
        return try {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            val fresh: Location? = withTimeoutOrNull(12_000L) {
                try {
                    client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token).await()
                } catch (_: Exception) {
                    null
                }
            }
            val fallback: Location? = fresh ?: try {
                client.lastLocation.await()
            } catch (_: Exception) {
                null
            } ?: getBestLastKnownFromManager(context)

            if (fallback != null) {
                Result.success(
                    LocationPoint(
                        latitude = fallback.latitude,
                        longitude = fallback.longitude,
                        accuracyMeters = fallback.accuracy
                    )
                )
            } else {
                Result.failure(
                    IllegalStateException("লোকেশন পাওয়া যায়নি — ফোনের Location চালু করে Allow দিন")
                )
            }
        } catch (e: Exception) {
            Result.failure(
                IllegalStateException("লোকেশন পাওয়া যায়নি — ফোনের Location চালু করে Allow দিন")
            )
        }
    }

    @SuppressLint("MissingPermission")
    private fun getBestLastKnownFromManager(context: Context): Location? {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        return providers.mapNotNull { provider ->
            try {
                lm.getLastKnownLocation(provider)
            } catch (_: Exception) {
                null
            }
        }.minByOrNull { it.accuracy }
    }
}
