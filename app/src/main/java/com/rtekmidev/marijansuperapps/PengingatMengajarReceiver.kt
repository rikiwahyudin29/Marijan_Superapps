package com.rtekmidev.marijansuperapps

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.app.NotificationCompat

class PengingatMengajarReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "channel_pengingat_mengajar_guru"
        const val CHANNEL_NAME = "Pengingat Jadwal Mengajar Guru"

        const val ACTION_MULAI_MENGAJAR = "com.rtekmidev.marijansuperapps.ACTION_MULAI_MENGAJAR"
        const val ACTION_SELESAI_MENGAJAR = "com.rtekmidev.marijansuperapps.ACTION_SELESAI_MENGAJAR"
        const val ACTION_TEST_PENGINGAT = "com.rtekmidev.marijansuperapps.ACTION_TEST_PENGINGAT"

        const val EXTRA_ID_JADWAL = "extra_id_jadwal"
        const val EXTRA_MAPEL = "extra_mapel"
        const val EXTRA_KELAS = "extra_kelas"
        const val EXTRA_JAM_KE = "extra_jam_ke"
        const val EXTRA_JAM_SELESAI = "extra_jam_selesai"

        // Pola getar panjang: jeda 0ms, getar 1200ms, jeda 400ms, getar 1500ms, jeda 400ms, getar 2000ms (~5.5 detik total)
        val POLA_GETAR_PANJANG = longArrayOf(0, 1200, 400, 1500, 400, 2000)
    }

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Jadwalkan ulang pengingat dari cache lokal jika HP baru dinyalakan
            PengingatMengajarManager.sinkronkanDariCache(context)
            return
        }

        val mapel = intent.getStringExtra(EXTRA_MAPEL) ?: "Pelajaran KBM"
        val kelas = intent.getStringExtra(EXTRA_KELAS) ?: "Kelas Binaan"
        val jamKe = intent.getStringExtra(EXTRA_JAM_KE) ?: ""
        val idJadwal = intent.getStringExtra(EXTRA_ID_JADWAL) ?: "0"

        val title: String
        val message: String
        val notifId = when (action) {
            ACTION_MULAI_MENGAJAR -> {
                title = "🔔 Waktunya Mengajar: $mapel"
                message = if (jamKe.isNotEmpty()) {
                    "$jamKe • Kelas $kelas sedang dimulai. Selamat mengajar!"
                } else {
                    "KBM kelas $kelas sedang dimulai. Selamat mengajar!"
                }
                (10000 + (kotlin.math.abs(idJadwal.hashCode()) % 10000))
            }
            ACTION_SELESAI_MENGAJAR -> {
                title = "⏰ Sesi Mengajar Berakhir: $mapel"
                message = "Jam pelajaran di kelas $kelas telah selesai. Jangan lupa isi Jurnal KBM!"
                (20000 + (kotlin.math.abs(idJadwal.hashCode()) % 10000))
            }
            ACTION_TEST_PENGINGAT -> {
                title = "🔔 Tes Pengingat Jadwal Mengajar"
                message = "Notifikasi dan getar panjang pengingat KBM berhasil aktif!"
                99999
            }
            else -> return
        }

        // 1. Picu Getar Panjang di Hardware
        getarkanHandphone(context)

        // 2. Tampilkan Push Notifikasi High Priority
        tampilkanNotifikasi(context, notifId, title, message)
    }

    private fun getarkanHandphone(context: Context) {
        try {
            val audioAttributes = android.media.AudioAttributes.Builder()
                .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                .build()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(
                    VibrationEffect.createWaveform(POLA_GETAR_PANJANG, -1),
                    audioAttributes
                )
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(POLA_GETAR_PANJANG, -1),
                    audioAttributes
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                vibrator?.vibrate(POLA_GETAR_PANJANG, -1)
            }
        } catch (e: Exception) {
            android.util.Log.e("PengingatMengajar", "Error getar: ${e.message}")
        }
    }

    private fun tampilkanNotifikasi(context: Context, notifId: Int, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val audioAttributes = android.media.AudioAttributes.Builder()
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
            .build()

        // Buat Notification Channel untuk Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat jam mulai dan jam akhir mengajar guru MA RJ"
                enableVibration(true)
                vibrationPattern = POLA_GETAR_PANJANG
                enableLights(true)
                lightColor = Color.parseColor("#4338CA")
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
                setSound(soundUri, audioAttributes)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent saat notifikasi diklik -> Buka DashboardGuruActivity tab Akademik (posisi 2)
        val clickIntent = Intent(context, DashboardGuruActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("NAV_POSITION", 2)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val largeIcon = try {
            BitmapFactory.decodeResource(context.resources, R.drawable.logo_marj)
        } catch (_: Exception) {
            null
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
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
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(POLA_GETAR_PANJANG)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notifId, builder.build())
    }
}
