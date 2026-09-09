package com.rtekmidev.smkrjsuperapps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class KonfirmasiActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var idJadwal: String = ""
    private var tokenServer: String = ""
    private var settingToken: String = ""
    private var nisnSiswa: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_konfirmasi)

        // Inisialisasi Database SQLite kita
        dbHelper = DatabaseHelper(this)

        // 1. Ambil data yang dilempar dari JadwalActivity
        idJadwal = intent.getStringExtra("ID_JADWAL") ?: ""
        val mapel = intent.getStringExtra("MAPEL") ?: ""
        val durasi = intent.getStringExtra("DURASI") ?: ""
        tokenServer = intent.getStringExtra("TOKEN_SERVER") ?: ""
        settingToken = intent.getStringExtra("SETTING_TOKEN") ?: "0"

        // 2. Ambil NISN dari sesi HP
        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisnSiswa = sharedPref.getString("nisn", "") ?: ""

        // 🔥 FIXED: Sesuaikan ID dengan XML Desain Elegan Terbaru 🔥
        val tvMapel = findViewById<TextView>(R.id.tvDetailMapel)
        val tvDurasi = findViewById<TextView>(R.id.tvDetailDurasi)
        val layoutToken = findViewById<LinearLayout>(R.id.layoutInputToken)
        val etToken = findViewById<EditText>(R.id.etTokenUjian)
        // Paksa semua teks jadi huruf besar tanpa merusak kursor
        etToken.filters = arrayOf(android.text.InputFilter.AllCaps())
        val btnMulai = findViewById<Button>(R.id.btnMulaiUjian)
        val pbLoading = findViewById<View>(R.id.pbMulaiUjian)

        // Pasang Teks
        tvMapel.text = mapel
        tvDurasi.text = "$durasi Menit"

        // Sembunyikan kolom token jika Admin setting tidak wajib token (0)
        if (settingToken != "1") {
            layoutToken.visibility = View.GONE
        }

        // 3. Logika Tombol Download & Mulai
        btnMulai.setOnClickListener {
            val inputToken = etToken.text.toString().trim().uppercase()

            // Validasi Token Lokal (Biar tidak usah capek nembak API kalau sudah pasti salah)
            if (settingToken == "1") {
                if (inputToken.isEmpty()) {
                    Toast.makeText(this, "Mohon masukkan Token Ujian!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                if (inputToken != tokenServer.uppercase()) {
                    Toast.makeText(this, "Token Salah! Silakan cek kembali.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Mulai Proses Loading
            pbLoading.visibility = View.VISIBLE
            btnMulai.isEnabled = false
            etToken.isEnabled = false
            btnMulai.text = "MENDOWNLOAD SOAL..." // Biar kelihatan progresnya oleh siswa

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Siapkan Data JSON untuk ditembak ke API
                    val requestPayload = mapOf(
                        "nisn" to nisnSiswa,
                        "id_jadwal" to idJadwal,
                        "token" to inputToken
                    )

                    // Tembak API Download Soal
                    val response = ApiClient.instance.downloadSoal(requestPayload)

                    withContext(Dispatchers.Main) {
                        pbLoading.visibility = View.GONE
                        btnMulai.isEnabled = true
                        etToken.isEnabled = true
                        btnMulai.text = "MULAI KERJAKAN UJIAN" // Kembalikan teks asli

                        if (response.isSuccessful && response.body()?.status == true) {
                            val downloadData = response.body()

                            if (downloadData != null && downloadData.data_soal.isNotEmpty()) {

                                // 🔥 FASE OFFLINE DIMULAI 🔥
                                // 1. Bersihkan tabel database dari soal ujian mapel sebelumnya
                                dbHelper.clearSemuaData()

                                // 2. Ubah Array Soal dari API kembali menjadi JSON String murni menggunakan Gson
                                val jsonSoalString = Gson().toJson(downloadData.data_soal)

                                // 3. Simpan JSON utuh tersebut ke Brankas SQLite
                                dbHelper.simpanSesiSoal(
                                    idUjian = downloadData.id_ujian_siswa,
                                    durasi = downloadData.durasi,
                                    minFinish = downloadData.min_finish ?: "0",
                                    jsonSoal = jsonSoalString
                                )

                                Toast.makeText(this@KonfirmasiActivity, "Soal tersimpan! Memasuki Mode Offline.", Toast.LENGTH_SHORT).show()

                                // 4. Pindah ke Halaman Pengerjaan Ujian
                                val intentUjian = Intent(this@KonfirmasiActivity, UjianActivity::class.java)
                                startActivity(intentUjian)
                                finish() // Tutup konfirmasi agar tidak bisa di-back

                            } else {
                                Toast.makeText(this@KonfirmasiActivity, "Soal kosong dari server!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorMsg = response.body()?.message ?: "Gagal mendownload soal"
                            Toast.makeText(this@KonfirmasiActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        pbLoading.visibility = View.GONE
                        btnMulai.isEnabled = true
                        etToken.isEnabled = true
                        btnMulai.text = "MULAI KERJAKAN UJIAN" // Kembalikan teks asli
                        Toast.makeText(this@KonfirmasiActivity, "Error Koneksi: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}
