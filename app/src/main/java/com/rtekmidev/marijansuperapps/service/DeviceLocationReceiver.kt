package com.rtekmidev.marijansuperapps.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import com.rtekmidev.marijansuperapps.api.ApiClient
import com.rtekmidev.marijansuperapps.util.LocationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DeviceLocationReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_PERIODIC_UPDATE = "com.rtekmidev.marijansuperapps.ACTION_UPDATE_LOCATION_PERIODIC"
        private const val TAG = "DeviceLocationReceiver"

        /**
         * Fungsi reusable untuk membaca GPS dan mengupdate lokasi ke server
         */
        fun updateLocationToServer(context: Context, onComplete: ((Boolean) -> Unit)? = null) {
            val userPref = context.getSharedPreferences("USER_PREF", Context.MODE_PRIVATE)
            val token = userPref.getString("TOKEN", null) ?: ApiClient.authToken

            val loc = LocationHelper.getBestLastKnownLocation(context)
            val isMock = LocationHelper.isLocationMock(loc)

            // 🛡️ Jika fake GPS / mock location terdeteksi, tolak kirim koordinat
            if (isMock) {
                Log.w(TAG, "Fake GPS / Mock Location terdeteksi! Pengiriman lokasi dibatalkan.")
                onComplete?.invoke(false)
                return
            }

            if (loc == null || (loc.latitude == 0.0 && loc.longitude == 0.0)) {
                Log.w(TAG, "Lokasi tidak valid atau GPS belum aktif.")
                onComplete?.invoke(false)
                return
            }

            val deviceId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN"

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (!token.isNullOrEmpty()) {
                        ApiClient.authToken = token
                    }
                    val resp = ApiClient.instance.updateLocation(
                        deviceId = deviceId,
                        latitude = loc.latitude.toString(),
                        longitude = loc.longitude.toString(),
                        isMock = false
                    )
                    Log.d(TAG, "Lokasi berhasil diupdate ke server: isSuccessful=${resp.isSuccessful}")
                    onComplete?.invoke(resp.isSuccessful)
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal update lokasi: ${e.message}")
                    onComplete?.invoke(false)
                }
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_PERIODIC_UPDATE || intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Menerima event update lokasi: ${intent.action}")
            updateLocationToServer(context)
            // Jadwalkan ulang untuk memastikan interval 2 jam berikutnya tetap aktif
            DeviceLocationScheduler.jadwalkanPeriodicUpdate(context)
        }
    }
}
