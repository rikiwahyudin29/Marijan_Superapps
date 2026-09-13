package com.rtekmidev.marijansuperapps

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.rtekmidev.marijansuperapps.api.ApiClient
import com.rtekmidev.marijansuperapps.api.JadwalUjian
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class JadwalActivity : AppCompatActivity() {

    private var nisnSiswa: String = ""
    private var namaSiswa: String = ""
    private var fullJadwalList: List<JadwalUjian> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jadwal)

        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisnSiswa = sharedPref.getString("nisn", "") ?: ""
        namaSiswa = sharedPref.getString("nama", "") ?: ""

        findViewById<TextView>(R.id.tvNamaSiswa).text = "Halo, $namaSiswa!"

        findViewById<CardView>(R.id.btnRefreshJadwal).setOnClickListener {
            Toast.makeText(this, "Menyegarkan jadwal...", Toast.LENGTH_SHORT).show()
            fetchJadwal()
        }

        // 🔥 FIXED: Ubah dari Button ke CardView sesuai XML baru 🔥
        findViewById<CardView>(R.id.cvFilterSemua).setOnClickListener { ubahFilter("SEMUA", it) }
        findViewById<CardView>(R.id.cvFilterAktif).setOnClickListener { ubahFilter("AKTIF", it) }
        findViewById<CardView>(R.id.cvFilterMendatang).setOnClickListener { ubahFilter("MENDATANG", it) }
        findViewById<CardView>(R.id.cvFilterSelesai).setOnClickListener { ubahFilter("SELESAI", it) }

        fetchJadwal()
    }

    private fun fetchJadwal() {
        val pbLoading = findViewById<View>(R.id.pbLoading)
        val containerJadwal = findViewById<LinearLayout>(R.id.containerJadwal)
        val tvInfoJadwal = findViewById<TextView>(R.id.tvInfoJadwal)

        pbLoading.visibility = View.VISIBLE
        containerJadwal.removeAllViews()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.getJadwal(nisnSiswa)

                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.status == true) {
                        fullJadwalList = response.body()?.data ?: emptyList()

                        if (fullJadwalList.isEmpty()) {
                            tvInfoJadwal.text = "Menampilkan 0 jadwal"
                            Toast.makeText(this@JadwalActivity, "Belum ada jadwal aktif untuk Anda", Toast.LENGTH_LONG).show()
                        } else {
                            // Tampilkan semua secara default
                            renderJadwal(fullJadwalList)
                            tvInfoJadwal.text = "Menampilkan ${fullJadwalList.size} jadwal"

                            // Set Filter "Semua" aktif sebagai default (Kuning)
                            ubahFilter("SEMUA", findViewById<CardView>(R.id.cvFilterSemua))
                        }
                    } else {
                        Toast.makeText(this@JadwalActivity, "Gagal mengambil jadwal", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    Toast.makeText(this@JadwalActivity, "Error Koneksi. Cari sinyal lalu refresh.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun ubahFilter(tipe: String, view: View) {
        // 1. Reset warna semua Card filter ke Putih dengan teks Hijau
        val cardIds = listOf(R.id.cvFilterSemua, R.id.cvFilterAktif, R.id.cvFilterMendatang, R.id.cvFilterSelesai)
        val textIds = listOf(R.id.tvFilterSemua, R.id.tvFilterAktif, R.id.tvFilterMendatang, R.id.tvFilterSelesai)

        for (i in cardIds.indices) {
            val card = findViewById<CardView>(cardIds[i])
            val text = findViewById<TextView>(textIds[i])
            card.setCardBackgroundColor(Color.WHITE)
            text.setTextColor(Color.parseColor("#059669"))
        }

        // 2. 🔥 Set Card Aktif: Kuning dengan Tulisan Putih 🔥
        val activeCard = view as CardView
        activeCard.setCardBackgroundColor(Color.parseColor("#FFB300")) // Kuning Elegan
        val activeText = activeCard.getChildAt(0) as TextView
        activeText.setTextColor(Color.WHITE)

        // 3. Filter list berdasarkan waktu dan status
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val sekarang = Calendar.getInstance().time

        val filteredList = when (tipe) {
            "AKTIF" -> fullJadwalList.filter {
                val waktuMulai = it.waktu_mulai?.let { str -> try { sdf.parse(str) } catch(e:Exception){null} }
                it.status_pengerjaan != "2" && waktuMulai != null && (waktuMulai.before(sekarang) || waktuMulai.time == sekarang.time)
            }
            "MENDATANG" -> fullJadwalList.filter {
                val waktuMulai = it.waktu_mulai?.let { str -> try { sdf.parse(str) } catch(e:Exception){null} }
                waktuMulai != null && waktuMulai.after(sekarang)
            }
            "SELESAI" -> fullJadwalList.filter { it.status_pengerjaan == "2" }
            else -> fullJadwalList // SEMUA
        }

        renderJadwal(filteredList)
        findViewById<TextView>(R.id.tvInfoJadwal).text = "Menampilkan ${filteredList.size} jadwal"
    }

    private fun renderJadwal(jadwalList: List<JadwalUjian>) {
        val containerJadwal = findViewById<LinearLayout>(R.id.containerJadwal)
        val inflater = LayoutInflater.from(this)

        containerJadwal.removeAllViews()

        // Siapkan alat baca waktu untuk mengecek status kedaluwarsa & belum mulai
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val waktuSekarang = Calendar.getInstance().time

        for (jadwal in jadwalList) {
            val itemView = inflater.inflate(R.layout.item_jadwal, containerJadwal, false)

            val viewAksen = itemView.findViewById<View>(R.id.viewAksen)
            val tvMapel = itemView.findViewById<TextView>(R.id.tvMapel)
            val tvJudulUjian = itemView.findViewById<TextView>(R.id.tvJudulUjian)
            val tvRentangWaktu = itemView.findViewById<TextView>(R.id.tvRentangWaktu)
            val tvDurasi = itemView.findViewById<TextView>(R.id.tvDurasi)
            val tvStatusBadge = itemView.findViewById<TextView>(R.id.tvStatusBadge)
            val btnKerjakan = itemView.findViewById<Button>(R.id.btnKerjakan)

            // Set Data Teks
            tvMapel.text = jadwal.nama_mapel ?: "Mata Pelajaran"
            tvJudulUjian.text = jadwal.judul_ujian ?: "Ujian Sekolah"

            // 🔥 FIX: Baca "waktu_selesai" dari API agar tidak Strip (-) 🔥
            val waktuMulaiStr = jadwal.waktu_mulai ?: "-"
            val waktuSelesaiStr = jadwal.waktu_selesai ?: jadwal.waktu_berakhir ?: "-"

            val mulaiFormat = if (waktuMulaiStr != "-") waktuMulaiStr.take(16) else "-"
            val akhirFormat = if (waktuSelesaiStr != "-") waktuSelesaiStr.takeLast(8).take(5) else "-"
            tvRentangWaktu.text = "$mulaiFormat s/d $akhirFormat"

            tvDurasi.text = "Durasi: ${jadwal.durasi ?: "90"} Menit"

            // 🔥 CEK WAKTU KEDALUWARSA DAN BELUM MULAI 🔥
            var isKedaluwarsa = false
            var isBelumMulai = false

            try {
                val waktuMulaiDb = if (waktuMulaiStr != "-") sdf.parse(waktuMulaiStr) else null
                val waktuSelesaiDb = if (waktuSelesaiStr != "-") sdf.parse(waktuSelesaiStr) else null

                if (waktuSelesaiDb != null && waktuSekarang.after(waktuSelesaiDb)) {
                    isKedaluwarsa = true
                }
                if (waktuMulaiDb != null && waktuSekarang.before(waktuMulaiDb)) {
                    isBelumMulai = true // Waktu sekarang masih di bawah waktu mulai
                }
            } catch (e: Exception) {}

            val status = jadwal.status_pengerjaan

            if (status == "2") {
                // SUDAH SELESAI
                tvStatusBadge.text = "Selesai"
                tvStatusBadge.setTextColor(Color.parseColor("#059669"))
                viewAksen.setBackgroundColor(Color.parseColor("#9CA3AF"))

                btnKerjakan.isEnabled = false
                btnKerjakan.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E5E7EB"))
                btnKerjakan.setTextColor(Color.parseColor("#9CA3AF"))

                val nilaiAkhir = (jadwal.nilai_pg?.toDoubleOrNull() ?: 0.0) + (jadwal.nilai_esai?.toDoubleOrNull() ?: 0.0)
                btnKerjakan.text = "NILAI ANDA: $nilaiAkhir"

            } else if (isKedaluwarsa) {
                // 🔥 JADWAL SUDAH LEWAT 🔥
                tvStatusBadge.text = "Terlewat"
                tvStatusBadge.setTextColor(Color.parseColor("#D32F2F")) // Merah
                viewAksen.setBackgroundColor(Color.parseColor("#D32F2F"))

                btnKerjakan.text = "WAKTU HABIS"
                btnKerjakan.isEnabled = false
                btnKerjakan.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFCDD2"))
                btnKerjakan.setTextColor(Color.parseColor("#D32F2F"))

            } else if (isBelumMulai) {
                // 🔥 JADWAL BELUM MULAI (CEGAH COLONG START) 🔥
                tvStatusBadge.text = "Belum Mulai"
                tvStatusBadge.setTextColor(Color.parseColor("#1976D2")) // Biru
                viewAksen.setBackgroundColor(Color.parseColor("#1976D2"))

                btnKerjakan.text = "BELUM WAKTUNYA"
                btnKerjakan.isEnabled = false // KUNCI TOMBOL
                btnKerjakan.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#BBDEFB")) // Biru Muda
                btnKerjakan.setTextColor(Color.parseColor("#1976D2"))

            } else if (status == "1") {
                // SEDANG DIKERJAKAN
                tvStatusBadge.text = "Sedang Dikerjakan"
                tvStatusBadge.setTextColor(Color.parseColor("#F59E0B"))
                viewAksen.setBackgroundColor(Color.parseColor("#F59E0B"))

                btnKerjakan.text = "LANJUTKAN UJIAN"
                btnKerjakan.isEnabled = true
                btnKerjakan.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F59E0B"))
                btnKerjakan.setTextColor(Color.WHITE)

                btnKerjakan.setOnClickListener { bukaKonfirmasi(jadwal) }

            } else {
                // BELUM MENGERJAKAN (TERSEDIA)
                tvStatusBadge.text = "Tersedia"
                tvStatusBadge.setTextColor(Color.parseColor("#374151"))
                viewAksen.setBackgroundColor(Color.parseColor("#059669"))

                btnKerjakan.text = "MULAI UJIAN"
                btnKerjakan.isEnabled = true
                btnKerjakan.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#059669"))
                btnKerjakan.setTextColor(Color.WHITE)

                btnKerjakan.setOnClickListener { bukaKonfirmasi(jadwal) }
            }

            containerJadwal.addView(itemView)
        }
    }

    private fun bukaKonfirmasi(jadwal: JadwalUjian) {
        val intent = Intent(this, KonfirmasiActivity::class.java)
        intent.putExtra("ID_JADWAL", jadwal.id_jadwal)
        intent.putExtra("MAPEL", jadwal.nama_mapel ?: jadwal.judul_ujian)
        intent.putExtra("DURASI", jadwal.durasi)
        intent.putExtra("TOKEN_SERVER", jadwal.token)
        intent.putExtra("SETTING_TOKEN", jadwal.setting_token)
        startActivity(intent)
    }
}
