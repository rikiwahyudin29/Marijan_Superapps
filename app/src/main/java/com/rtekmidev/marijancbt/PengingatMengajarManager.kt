package com.rtekmidev.marijancbt

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.rtekmidev.marijancbt.api.JadwalGuruHariIni
import java.util.Calendar

object PengingatMengajarManager {

    private const val PREF_NAME = "PengingatMengajarPref"
    private const val KEY_JADWAL_CACHE = "key_jadwal_hari_ini_cache"
    private const val KEY_IS_ENABLED = "key_pengingat_enabled"
    private const val TAG = "PengingatMengajar"

    fun isPengingatAktif(context: Context): Boolean {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return pref.getBoolean(KEY_IS_ENABLED, true) // default aktif
    }

    fun setPengingatAktif(context: Context, aktif: Boolean) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        pref.edit().putBoolean(KEY_IS_ENABLED, aktif).apply()
        if (!aktif) {
            batalkanSemuaPengingat(context)
        } else {
            sinkronkanDariCache(context)
        }
    }

    /**
     * Sinkronkan jadwal mengajar hari ini untuk guru yang sedang login
     */
    fun sinkronkanJadwalHariIni(context: Context, jadwalList: List<JadwalGuruHariIni>?, isLibur: Boolean) {
        if (!isPengingatAktif(context)) return

        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

        if (isLibur || jadwalList.isNullOrEmpty()) {
            batalkanSemuaPengingat(context)
            pref.edit().remove(KEY_JADWAL_CACHE).apply()
            return
        }

        // Simpan cache jadwal hari ini
        val json = Gson().toJson(jadwalList)
        pref.edit().putString(KEY_JADWAL_CACHE, json).apply()

        // Pasang alarm untuk masing-masing jadwal
        jadwalkanAlarmList(context, jadwalList)
    }

    /**
     * Jadwalkan ulang dari cache saat HP baru reboot
     */
    fun sinkronkanDariCache(context: Context) {
        if (!isPengingatAktif(context)) return
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = pref.getString(KEY_JADWAL_CACHE, null) ?: return
        try {
            val type = object : TypeToken<List<JadwalGuruHariIni>>() {}.type
            val list: List<JadwalGuruHariIni> = Gson().fromJson(json, type)
            jadwalkanAlarmList(context, list)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membaca cache jadwal: ${e.message}")
        }
    }

    private fun jadwalkanAlarmList(context: Context, jadwalList: List<JadwalGuruHariIni>) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = System.currentTimeMillis()

        for (item in jadwalList) {
            val idBase = kotlin.math.abs(item.id.hashCode())

            // 1. Jadwal Jam Mulai Mengajar
            val calMulai = parseWaktuHariIni(item.jam_mulai)
            if (calMulai != null && calMulai.timeInMillis > now) {
                val intentMulai = Intent(context, PengingatMengajarReceiver::class.java).apply {
                    action = PengingatMengajarReceiver.ACTION_MULAI_MENGAJAR
                    putExtra(PengingatMengajarReceiver.EXTRA_ID_JADWAL, item.id)
                    putExtra(PengingatMengajarReceiver.EXTRA_MAPEL, item.nama_mapel ?: "Pelajaran KBM")
                    putExtra(PengingatMengajarReceiver.EXTRA_KELAS, item.nama_kelas ?: "Kelas")
                    putExtra(PengingatMengajarReceiver.EXTRA_JAM_KE, item.jam_ke ?: "")
                }
                val piMulai = PendingIntent.getBroadcast(
                    context,
                    (10000 + (idBase % 10000)),
                    intentMulai,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setExactAlarm(alarmManager, calMulai.timeInMillis, piMulai)
            }

            // 2. Jadwal Jam Selesai Mengajar
            val calSelesai = parseWaktuHariIni(item.jam_selesai)
            if (calSelesai != null && calSelesai.timeInMillis > now) {
                val intentSelesai = Intent(context, PengingatMengajarReceiver::class.java).apply {
                    action = PengingatMengajarReceiver.ACTION_SELESAI_MENGAJAR
                    putExtra(PengingatMengajarReceiver.EXTRA_ID_JADWAL, item.id)
                    putExtra(PengingatMengajarReceiver.EXTRA_MAPEL, item.nama_mapel ?: "Pelajaran KBM")
                    putExtra(PengingatMengajarReceiver.EXTRA_KELAS, item.nama_kelas ?: "Kelas")
                    putExtra(PengingatMengajarReceiver.EXTRA_JAM_KE, item.jam_ke ?: "")
                }
                val piSelesai = PendingIntent.getBroadcast(
                    context,
                    (20000 + (idBase % 10000)),
                    intentSelesai,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                setExactAlarm(alarmManager, calSelesai.timeInMillis, piSelesai)
            }
        }
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: Exception) {
            try {
                alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } catch (_: Exception) {}
        }
    }

    private fun parseWaktuHariIni(jamStr: String?): Calendar? {
        if (jamStr.isNullOrEmpty()) return null
        return try {
            val clean = jamStr.trim().replace(".", ":")
            val parts = clean.split(":")
            if (parts.size >= 2) {
                val jam = parts[0].toInt()
                val menit = parts[1].toInt()
                val cal = Calendar.getInstance()
                cal.set(Calendar.HOUR_OF_DAY, jam)
                cal.set(Calendar.MINUTE, menit)
                cal.set(Calendar.SECOND, 0)
                cal.set(Calendar.MILLISECOND, 0)
                cal
            } else null
        } catch (_: Exception) {
            null
        }
    }

    fun batalkanSemuaPengingat(context: Context) {
        val pref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = pref.getString(KEY_JADWAL_CACHE, null) ?: return
        try {
            val type = object : TypeToken<List<JadwalGuruHariIni>>() {}.type
            val list: List<JadwalGuruHariIni> = Gson().fromJson(json, type)
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            for (item in list) {
                val idBase = kotlin.math.abs(item.id.hashCode())
                val piMulai = PendingIntent.getBroadcast(
                    context,
                    (10000 + (idBase % 10000)),
                    Intent(context, PengingatMengajarReceiver::class.java),
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (piMulai != null) alarmManager.cancel(piMulai)

                val piSelesai = PendingIntent.getBroadcast(
                    context,
                    (20000 + (idBase % 10000)),
                    Intent(context, PengingatMengajarReceiver::class.java),
                    PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                )
                if (piSelesai != null) alarmManager.cancel(piSelesai)
            }
        } catch (_: Exception) {}
    }

    /**
     * Minta izin notifikasi runtime di Android 13+ (API 33+)
     */
    fun cekDanMintaIzinNotifikasi(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    1011
                )
            }
        }
    }

    /**
     * Trigger demo notifikasi + getar panjang seketika untuk pengetesan di handphone
     */
    fun testNotifikasiGetarPanjang(context: Context) {
        val intent = Intent(context, PengingatMengajarReceiver::class.java).apply {
            action = PengingatMengajarReceiver.ACTION_TEST_PENGINGAT
            putExtra(PengingatMengajarReceiver.EXTRA_MAPEL, "Administrasi Sistem Jaringan")
            putExtra(PengingatMengajarReceiver.EXTRA_KELAS, "12 TKJT 1")
            putExtra(PengingatMengajarReceiver.EXTRA_JAM_KE, "Jam Ke 1-2")
        }
        context.sendBroadcast(intent)
    }
}
