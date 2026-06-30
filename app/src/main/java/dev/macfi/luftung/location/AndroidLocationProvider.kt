package dev.macfi.luftung.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import dev.macfi.luftung.data.Coordinates

class AndroidLocationProvider(private val context: Context) {
    fun currentCoordinates(): Coordinates? {
        if (!hasLocationPermission()) return null

        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val bestLocation = locationManager.getProviders(true)
            .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
            .maxByOrNull(Location::getTime)

        return bestLocation?.let {
            Coordinates(latitude = it.latitude, longitude = it.longitude)
        }
    }

    fun hasLocationPermission(): Boolean {
        val coarse = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val fine = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        return coarse || fine
    }
}
