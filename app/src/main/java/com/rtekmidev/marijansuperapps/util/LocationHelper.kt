package com.rtekmidev.marijansuperapps.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import androidx.core.content.ContextCompat

object LocationHelper {

    /**
     * Deteksi apakah koordinat GPS berasal dari Fake GPS / Mock Location
     */
    fun isLocationMock(location: Location?): Boolean {
        if (location == null) return false

        // 1. Android 12+ (API 31+) menggunakan properti isMock
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (location.isMock) return true
        } else {
            // 2. Android 11 ke bawah menggunakan isFromMockProvider
            @Suppress("DEPRECATION")
            if (location.isFromMockProvider) return true
        }

        // 3. Fallback pemeriksaan extras bundle
        val extras = location.extras
        if (extras != null && extras.getBoolean("mockLocation", false)) {
            return true
        }

        return false
    }

    /**
     * Cek apakah izin akses lokasi sudah diberikan oleh pengguna
     */
    fun hasLocationPermission(context: Context): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    /**
     * Ambil lokasi terbaik saat ini
     */
    @SuppressLint("MissingPermission")
    fun getBestLastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null

        val gpsLoc = try { lm.getLastKnownLocation(LocationManager.GPS_PROVIDER) } catch (_: Exception) { null }
        val netLoc = try { lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { null }

        return when {
            gpsLoc != null && netLoc != null -> {
                if (gpsLoc.time >= netLoc.time) gpsLoc else netLoc
            }
            gpsLoc != null -> gpsLoc
            else -> netLoc
        }
    }
}
