package com.rtekmidev.marijansuperapps.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

object DeviceLocationScheduler {

    private const val REQUEST_CODE = 9921
    private const val INTERVAL_2_HOURS_MS = 2 * 60 * 60 * 1000L // 2 Jam

    fun jadwalkanPeriodicUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DeviceLocationReceiver::class.java).apply {
            action = DeviceLocationReceiver.ACTION_PERIODIC_UPDATE
        }

        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)

        // Alarm inexact repeating 2 jam (sangat hemat baterai dan ramah doze mode)
        val triggerAtMillis = SystemClock.elapsedRealtime() + INTERVAL_2_HOURS_MS
        alarmManager.setInexactRepeating(
            AlarmManager.ELAPSED_REALTIME_WAKEUP,
            triggerAtMillis,
            INTERVAL_2_HOURS_MS,
            pendingIntent
        )
        Log.d("DeviceLocationScheduler", "Pembaruan lokasi periodik 2 jam berhasil dijadwalkan.")
    }

    fun batalkanPeriodicUpdate(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, DeviceLocationReceiver::class.java).apply {
            action = DeviceLocationReceiver.ACTION_PERIODIC_UPDATE
        }
        val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val pendingIntent = PendingIntent.getBroadcast(context, REQUEST_CODE, intent, flags)
        alarmManager.cancel(pendingIntent)
        Log.d("DeviceLocationScheduler", "Pembaruan lokasi periodik berhasil dibatalkan.")
    }
}
