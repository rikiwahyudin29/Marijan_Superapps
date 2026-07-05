package com.rtekmidev.marijancbt

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RekapActivity : AppCompatActivity() {

    private var userRole = ""
    private var identifier = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rekap)

        // 🔥 KUNCI UTAMA: BACA ROLE LANGSUNG DARI SESI (ANTI-NYASAR) 🔥
        val prefGuru = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val prefSiswa = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)

        if (prefGuru.getBoolean("isLoggedIn", false)) {
            userRole = "GURU"
            identifier = prefGuru.getString("id_user", "") ?: ""
        } else if (prefSiswa.getBoolean("isLoggedIn", false)) {
            userRole = "SISWA"
            identifier = prefSiswa.getString("nisn", "") ?: ""
        } else {
            Toast.makeText(this, "Sesi tidak valid!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.btnBackRekap).setOnClickListener { finish() }

        muatDataRekap()
    }

    private fun muatDataRekap() {
        val wadah = findViewById<LinearLayout>(R.id.wadahListRekap)
        if (identifier.isEmpty()) return

        // Ambil format bulan saat ini persis seperti Web (Contoh: 2026-05)
        val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Tembak API yang PASTI BENAR sesuai Role
                val resp = if (userRole == "GURU") {
                    ApiClient.instance.getRekapGuru(identifier, currentMonth)
                } else {
                    ApiClient.instance.getRekapAbsen(identifier, currentMonth)
                }

                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body()?.status == true) {
                        val body = resp.body()!!

                        findViewById<TextView>(R.id.tvTotalHadir).text = body.summary?.hadir?.toString() ?: "0"
                        findViewById<TextView>(R.id.tvTotalIzin).text = ((body.summary?.sakit ?: 0) + (body.summary?.izin ?: 0)).toString()
                        findViewById<TextView>(R.id.tvTotalAlpha).text = body.summary?.alpha?.toString() ?: "0"

                        wadah.removeAllViews()

                        val listData = body.data ?: emptyList()
                        if (listData.isEmpty()) {
                            Toast.makeText(this@RekapActivity, "Belum ada riwayat bulan ini.", Toast.LENGTH_SHORT).show()
                        }

                        listData.forEach { item ->
                            val card = CardView(this@RekapActivity).apply {
                                radius = 24f
                                cardElevation = 0f
                                useCompatPadding = true
                                setCardBackgroundColor(Color.WHITE)
                                setContentPadding(40, 30, 40, 30)
                            }

                            val layoutIn = LinearLayout(this@RekapActivity).apply {
                                orientation = LinearLayout.VERTICAL
                            }

                            val headerRow = RelativeLayout(this@RekapActivity)

                            val txtTgl = TextView(this@RekapActivity).apply {
                                text = item.tanggal
                                textSize = 14f
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                setTextColor(Color.parseColor("#111827"))
                            }

                            val badgeStatus = TextView(this@RekapActivity).apply {
                                text = item.status_kehadiran.uppercase()
                                textSize = 10f
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                setPadding(20, 8, 20, 8)

                                val status = item.status_kehadiran.lowercase()
                                if (status == "hadir") {
                                    setTextColor(Color.parseColor("#059669"))
                                    setBackgroundColor(Color.parseColor("#D1FAE5"))
                                } else if (status == "sakit" || status == "izin" || status == "dinas luar") {
                                    setTextColor(Color.parseColor("#D97706"))
                                    setBackgroundColor(Color.parseColor("#FEF3C7"))
                                } else {
                                    setTextColor(Color.parseColor("#DC2626"))
                                    setBackgroundColor(Color.parseColor("#FEE2E2"))
                                }
                            }

                            val paramsBadge = RelativeLayout.LayoutParams(
                                RelativeLayout.LayoutParams.WRAP_CONTENT,
                                RelativeLayout.LayoutParams.WRAP_CONTENT
                            ).apply { addRule(RelativeLayout.ALIGN_PARENT_END) }

                            headerRow.addView(txtTgl)
                            headerRow.addView(badgeStatus, paramsBadge)

                            val txtJam = TextView(this@RekapActivity).apply {
                                text = "Masuk: ${item.jam_masuk ?: "--"}  |  Pulang: ${item.jam_pulang ?: "--"}"
                                textSize = 13f
                                setTextColor(Color.parseColor("#6B7280"))
                                setPadding(0, 16, 0, 0)
                            }

                            layoutIn.addView(headerRow)
                            layoutIn.addView(txtJam)
                            card.addView(layoutIn)
                            wadah.addView(card)
                        }
                    } else {
                        Toast.makeText(this@RekapActivity, "Gagal memuat data rekap", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RekapActivity, "Koneksi API Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}