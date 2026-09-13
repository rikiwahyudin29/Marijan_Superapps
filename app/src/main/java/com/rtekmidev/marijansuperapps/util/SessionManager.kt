package com.rtekmidev.marijansuperapps.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AlertDialog
import com.rtekmidev.marijansuperapps.LoginActivity
import com.rtekmidev.marijansuperapps.api.ApiClient
import com.rtekmidev.marijansuperapps.service.DeviceLocationScheduler

object SessionManager {

    /**
     * Tampilkan dialog konfirmasi keluar akun yang kompatibel dengan Theme AppCompat
     */
    fun konfirmasiLogout(
        activity: Activity,
        pesan: String = "Apakah Anda yakin ingin keluar dari akun aplikasi MA Riyadhul Jannah?"
    ) {
        AlertDialog.Builder(activity)
            .setTitle("Konfirmasi Keluar")
            .setMessage(pesan)
            .setPositiveButton("Ya, Keluar") { _, _ ->
                logout(activity)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    /**
     * Bersihkan seluruh sesi login (Guru, Siswa, Admin, Superadmin, Token, Lokasi)
     * dan arahkan ke halaman Login
     */
    fun logout(context: Context) {
        try {
            // Hentikan pelacakan lokasi latar belakang
            DeviceLocationScheduler.batalkanPeriodicUpdate(context)
        } catch (_: Exception) {}

        // 1. Bersihkan SharedPreferences SesiGuru (Guru, Kepsek, Admin, Superadmin)
        val prefGuru = context.getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        prefGuru.edit().clear().apply()

        // 2. Bersihkan SharedPreferences SesiUjian (Siswa)
        val prefSiswa = context.getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        prefSiswa.edit().clear().apply()

        // 3. Bersihkan Token & Data Global di USER_PREF
        val userPref = context.getSharedPreferences("USER_PREF", Context.MODE_PRIVATE)
        userPref.edit().clear().apply()

        // 4. Reset ApiClient Token
        ApiClient.authToken = ""

        // 5. Kembali ke LoginActivity dan bersihkan seluruh backstack task
        val intent = Intent(context, LoginActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        context.startActivity(intent)

        if (context is Activity) {
            context.finishAffinity()
        }
    }
}
