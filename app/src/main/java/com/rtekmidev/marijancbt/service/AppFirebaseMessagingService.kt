package com.rtekmidev.marijancbt.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rtekmidev.marijancbt.DashboardActivity
import com.rtekmidev.marijancbt.DashboardGuruActivity
import com.rtekmidev.marijancbt.R
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AppFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "AppFCM"
        const val CHANNEL_ID = "channel_smk_rj_broadcast"
        const val CHANNEL_NAME = "Pengumuman & Notifikasi Sekolah"
        val POLA_GETAR = longArrayOf(0, 600, 250, 600)
    }

    /**
     * Dipanggil saat Firebase menghasilkan token perangkat baru / refresh token
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Token FCM Baru Diterima: $token")

        // 1. Simpan token ke SharedPreferences lokal
        val pref = getSharedPreferences("APP_SETTINGS", Context.MODE_PRIVATE)
        pref.edit().putString("FCM_TOKEN", token).apply()

        // 2. Jika user sedang aktif login, sinkronkan token ini ke server Laravel
        val userPref = getSharedPreferences("USER_PREF", Context.MODE_PRIVATE)
        val authToken = userPref.getString("TOKEN", null) ?: ApiClient.authToken

        if (!authToken.isNullOrEmpty()) {
            val deviceId = Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID) ?: "android_device"
            val deviceName = "${Build.MANUFACTURER} ${Build.MODEL}"

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    ApiClient.authToken = authToken
                    val response = ApiClient.instance.registerFcmToken(
                        fcmToken = token,
                        deviceId = deviceId,
                        deviceName = deviceName
                    )
                    Log.d(TAG, "Sinkronisasi token ke Laravel: isSuccessful=${response.isSuccessful}")
                } catch (e: Exception) {
                    Log.e(TAG, "Gagal mengirim token baru ke Laravel: ${e.message}")
                }
            }
        }
    }

    /**
     * Dipanggil saat ada pesan Push Notification masuk dari Laravel Firebase
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.d(TAG, "Pesan Masuk dari: ${remoteMessage.from}")

        // Ekstrak Judul dan Isi Notifikasi
        val title = remoteMessage.notification?.title 
            ?: remoteMessage.data["title"] 
            ?: "Pengumuman SMK RJ"

        val body = remoteMessage.notification?.body 
            ?: remoteMessage.data["body"] 
            ?: "Ada pesan pengumuman baru dari sekolah."

        // 1. Getarkan HP
        getarkanHandphone()

        // 2. Tampilkan Heads-Up Banner Notification
        tampilkanNotifikasi(title, body, remoteMessage.data)
    }

    private fun getarkanHandphone() {
        try {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(POLA_GETAR, -1),
                    audioAttributes
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(POLA_GETAR, -1),
                    audioAttributes
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(POLA_GETAR, -1)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrasi: ${e.message}")
        }
    }

    private fun tampilkanNotifikasi(title: String, message: String, data: Map<String, String>) {
        try {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

            // 1. Buat / Pastikan Notification Channel terdaftar (Android 8.0+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifikasi siaran pengumuman dan berita sistem SMK Riyadhul Jannah"
                    enableVibration(true)
                    vibrationPattern = POLA_GETAR
                    enableLights(true)
                    lightColor = Color.parseColor("#4338CA")
                    setShowBadge(true)
                    lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                    val audioAttributes = android.media.AudioAttributes.Builder()
                        .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                        .build()
                    setSound(soundUri, audioAttributes)
                }
                notificationManager.createNotificationChannel(channel)
            }

            // 2. Tentukan Target Activity saat Notifikasi Diklik
            val userPref = getSharedPreferences("USER_PREF", Context.MODE_PRIVATE)
            val role = userPref.getString("ROLE", "siswa")
            val targetClass = if (role.equals("guru", ignoreCase = true) || role.equals("kepsek", ignoreCase = true)) {
                DashboardGuruActivity::class.java
            } else {
                DashboardActivity::class.java
            }

            val clickIntent = Intent(this, targetClass).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("FROM_PUSH_NOTIF", true)
                data.forEach { (key, value) ->
                    putExtra(key, value)
                }
            }

            val notifId = (System.currentTimeMillis() % 100000).toInt()

            val pendingIntent = PendingIntent.getActivity(
                this,
                notifId,
                clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Decode Large Icon secara hemat memori
            val largeIcon = try {
                val options = BitmapFactory.Options().apply {
                    inSampleSize = 4 // Perkecil skala agar tidak makan banyak memori
                }
                BitmapFactory.decodeResource(resources, R.drawable.logo_marj, options)
            } catch (_: Throwable) {
                null
            }

            // 3. Bangun Notifikasi dengan Small Icon Vector yang valid
            val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .apply {
                    if (largeIcon != null) {
                        setLargeIcon(largeIcon)
                    }
                }
                .setColor(Color.parseColor("#4338CA"))
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setSound(soundUri)
                .setVibrate(POLA_GETAR)
                .setContentIntent(pendingIntent)

            val managerCompat = androidx.core.app.NotificationManagerCompat.from(this)
            managerCompat.notify(notifId, builder.build())
            Log.d(TAG, "Notifikasi berhasil diposting ke NotificationManager dengan notifId=$notifId")
        } catch (e: Throwable) {
            Log.e(TAG, "Error saat menampilkan notifikasi: ${e.message}", e)
        }
    }
}
