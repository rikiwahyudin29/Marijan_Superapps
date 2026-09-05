package com.rtekmidev.marijancbt

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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

        const val ACTION_MULAI_MENGAJAR = "com.rtekmidev.marijancbt.ACTION_MULAI_MENGAJAR"
        const val ACTION_SELESAI_MENGAJAR = "com.rtekmidev.marijancbt.ACTION_SELESAI_MENGAJAR"
        const val ACTION_TEST_PENGINGAT = "com.rtekmidev.marijancbt.ACTION_TEST_PENGINGAT"

        const val EXTRA_ID_JADWAL = "extra_id_jadwal"
        const val EXTRA_MAPEL = "extra_mapel"
        const val EXTRA_KELAS = "extra_kelas"
        const val EXTRA_JAM_KE = "extra_jam_ke"
        const val EXTRA_JAM_SELESAI = "extra_jam_selesai"

        // Pola getar panjang: jeda 0ms, getar 1000ms, jeda 300ms, getar 1200ms, jeda 300ms, getar 1500ms
        val POLA_GETAR_PANJANG = longArrayOf(0, 1000, 300, 1200, 300, 1500)
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
                (10000 + (idJadwal.hashCode() % 10000))
            }
            ACTION_SELESAI_MENGAJAR -> {
                title = "⏰ Sesi Mengajar Berakhir: $mapel"
                message = "Jam pelajaran di kelas $kelas telah selesai. Jangan lupa isi Jurnal KBM!"
                (20000 + (idJadwal.hashCode() % 10000))
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
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            vibrator?.let { v ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(POLA_GETAR_PANJANG, -1))
                } else {
                    @Suppress("DEPRECATION")
                    v.vibrate(POLA_GETAR_PANJANG, -1)
                }
            }
        } catch (_: Exception) {}
    }

    private fun tampilkanNotifikasi(context: Context, notifId: Int, title: String, message: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Buat Notification Channel untuk Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat jam mulai dan jam akhir mengajar guru SMK RJ"
                enableVibration(true)
                vibrationPattern = POLA_GETAR_PANJANG
                enableLights(true)
                lightColor = Color.parseColor("#4338CA")
                setSound(soundUri, null)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Intent saat notifikasi diklik -> Buka DashboardGuruActivity tab Akademik (posisi 2)
        val clickIntent = Intent(context, DashboardGuruActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("NAV_POSITION", 2)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notifId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setSound(soundUri)
            .setVibrate(POLA_GETAR_PANJANG)
            .setContentIntent(pendingIntent)

        notificationManager.notify(notifId, builder.build())
    }
}
