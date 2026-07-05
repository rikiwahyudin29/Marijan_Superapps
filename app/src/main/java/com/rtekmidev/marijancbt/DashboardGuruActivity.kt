package com.rtekmidev.marijancbt

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class DashboardGuruActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard_guru)

        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val namaGuru = sharedPref.getString("nama", "Bapak/Ibu Guru")

        findViewById<TextView>(R.id.tvNamaGuru).text = namaGuru

        // --- TOMBOL LOGOUT ---
        findViewById<ImageView>(R.id.btnLogoutGuru).setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Logout Guru")
                .setMessage("Apakah Anda yakin ingin keluar?")
                .setPositiveButton("Ya, Keluar") { _, _ ->
                    sharedPref.edit().clear().apply()
                    startActivity(Intent(this, LoginActivity::class.java))
                    finish()
                }
                .setNegativeButton("Batal", null)
                .show()
        }

        // --- MENU PRESENSI (Pakai UI Siswa, tapi role GURU) ---
        findViewById<CardView>(R.id.menuAbsenGuru).setOnClickListener {
            val intent = Intent(this, PresensiActivity::class.java)
            intent.putExtra("ROLE", "GURU") // 🔥 INI KUNCI AGAR UI TAHU INI GURU
            startActivity(intent)
        }

        // Menu Rekap & Izin (Tinggal diarahkan nanti)
        findViewById<CardView>(R.id.menuRekapGuru).setOnClickListener {
            Toast.makeText(this, "Modul Rekap Guru segera hadir", Toast.LENGTH_SHORT).show()
        }

        findViewById<CardView>(R.id.menuIzinGuru).setOnClickListener {
            Toast.makeText(this, "Modul Izin Guru segera hadir", Toast.LENGTH_SHORT).show()
        }
        // --- MENU PRESENSI GURU ---
        findViewById<CardView>(R.id.menuAbsenGuru).setOnClickListener {
            val intent = Intent(this, PresensiActivity::class.java)
            intent.putExtra("ROLE", "GURU")
            startActivity(intent)
        }

        // --- MENU REKAP GURU (AKTIF) ---
        findViewById<CardView>(R.id.menuRekapGuru).setOnClickListener {
            val intent = Intent(this, RekapActivity::class.java)
            intent.putExtra("ROLE", "GURU") // Kirim flag guru
            startActivity(intent)
        }

        // --- MENU IZIN GURU (AKTIF) ---
        findViewById<CardView>(R.id.menuIzinGuru).setOnClickListener {
            val intent = Intent(this, IzinActivity::class.java)
            intent.putExtra("ROLE", "GURU") // Kirim flag guru
            startActivity(intent)
        }
    }
}