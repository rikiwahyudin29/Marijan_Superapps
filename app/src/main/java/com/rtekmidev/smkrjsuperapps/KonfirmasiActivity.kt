package com.rtekmidev.smkrjsuperapps

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import android.view.View
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.google.gson.Gson
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class KonfirmasiActivity : AppCompatActivity() {

    private lateinit var dbHelper: DatabaseHelper
    private var idJadwal: String = ""
    private var tokenServer: String = ""
    private var settingToken: String = ""
    private var nisnSiswa: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_konfirmasi)

        // Inisialisasi Database SQLite
        dbHelper = DatabaseHelper(this)

        // 1. Ambil data yang dilempar dari CbtFragment / JadwalActivity
        idJadwal = intent.getStringExtra("ID_JADWAL") ?: ""
        val mapel = intent.getStringExtra("MAPEL") ?: "Mata Pelajaran"
        val durasi = intent.getStringExtra("DURASI") ?: "90"
        tokenServer = intent.getStringExtra("TOKEN_SERVER") ?: ""
        settingToken = intent.getStringExtra("SETTING_TOKEN") ?: "0"
        val jenisUjian = intent.getStringExtra("JENIS_UJIAN") ?: "PENILAIAN SUMATIF"
        val waktuMulai = intent.getStringExtra("WAKTU_MULAI") ?: ""
        val totalSoal = intent.getStringExtra("TOTAL_SOAL") ?: "40"
        val serverNode = intent.getStringExtra("SERVER_NODE") ?: "RTEKMI-Node01 • High-Speed Online"

        // 2. Ambil sesi Siswa dari SharedPreferences
        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisnSiswa = sharedPref.getString("nisn", "") ?: ""
        val localNama = sharedPref.getString("nama_siswa", null) ?: sharedPref.getString("nama", "Siswa")
        val localKelas = sharedPref.getString("kelas", "12 TKJT 1")
        val localNis = sharedPref.getString("nis", null) ?: nisnSiswa
        val localSem = sharedPref.getString("semester", "Semester Ganjil")

        // 3. Bind Top Bar Views
        val btnBack = findViewById<View>(R.id.btnBack)
        btnBack.setOnClickListener { finish() }

        // 4. Bind Hero Student Views
        val tvStudentName = findViewById<TextView>(R.id.tvStudentName)
        val tvStudentInfo = findViewById<TextView>(R.id.tvStudentInfo)
        val tvStudentSemester = findViewById<TextView>(R.id.tvStudentSemester)

        tvStudentName.text = localNama
        tvStudentInfo.text = "NIS: $localNis  •  $localKelas"
        tvStudentSemester.text = if (localSem?.startsWith("Semester") == true) localSem else "Semester $localSem"

        // 5. Bind Card Detail Mapel
        val tvBadgeJenisUjian = findViewById<TextView>(R.id.tvBadgeJenisUjian)
        val tvDetailMapel = findViewById<TextView>(R.id.tvDetailMapel)
        val tvDetailKelas = findViewById<TextView>(R.id.tvDetailKelas)

        tvBadgeJenisUjian.text = jenisUjian.uppercase(Locale.getDefault())
        tvDetailMapel.text = mapel
        tvDetailKelas.text = "Kelas $localKelas"

        // 6. Bind Parameter Ujian (4 Kartu)
        val tvWaktuMulai = findViewById<TextView>(R.id.tvWaktuMulai)
        val tvWaktuMulaiJam = findViewById<TextView>(R.id.tvWaktuMulaiJam)
        val tvDetailDurasi = findViewById<TextView>(R.id.tvDetailDurasi)
        val tvTotalSoal = findViewById<TextView>(R.id.tvTotalSoal)
        val tvServerNodeJudul = findViewById<TextView>(R.id.tvServerNodeJudul)
        val tvServerNodeSub = findViewById<TextView>(R.id.tvServerNodeSub)

        val (tglFormatted, jamFormatted) = formatTanggalWaktu(waktuMulai)
        tvWaktuMulai.text = tglFormatted
        tvWaktuMulaiJam.text = jamFormatted

        tvDetailDurasi.text = "$durasi Menit"
        tvTotalSoal.text = "$totalSoal Butir Soal"

        if (serverNode.contains("•")) {
            val parts = serverNode.split("•")
            tvServerNodeJudul.text = parts[0].trim()
            tvServerNodeSub.text = if (parts.size > 1) parts[1].trim() else "High-Speed Online"
        } else {
            tvServerNodeJudul.text = "RTEKMI-Node01"
            tvServerNodeSub.text = "High-Speed Online"
        }

        // 7. Bind Card Masukkan Token Ujian (KONDISIONAL SESUAI PERMINTAAN USER)
        val layoutInputTokenCard = findViewById<CardView>(R.id.layoutInputTokenCard)
        val etToken = findViewById<EditText>(R.id.etTokenUjian)
        etToken.filters = arrayOf(InputFilter.AllCaps())

        // 🔥 LOGIKA KONDISIONAL TOKEN:
        // Jika settingToken == "1", wajib token -> Tampilkan kolom token
        // Jika settingToken == "0" atau selain "1" -> Sembunyikan kolom token
        val isWajibToken = (settingToken == "1")
        if (isWajibToken) {
            layoutInputTokenCard.visibility = View.VISIBLE
        } else {
            layoutInputTokenCard.visibility = View.GONE
        }

        // 8. Bind Bottom Action Bar
        val btnBatal = findViewById<View>(R.id.btnBatal)
        val btnMulai = findViewById<View>(R.id.btnMulaiUjianCard)
        val layoutBtnMulaiContent = findViewById<View>(R.id.layoutBtnMulaiContent)
        val tvBtnMulaiText = findViewById<TextView>(R.id.tvBtnMulaiText)
        val pbLoading = findViewById<ProgressBar>(R.id.pbMulaiUjian)

        btnBatal.setOnClickListener { finish() }

        btnMulai.setOnClickListener {
            val inputToken = etToken.text.toString().trim().uppercase()

            // Validasi Token hanya jika ujian ini di-set wajib token
            if (isWajibToken) {
                if (inputToken.isEmpty()) {
                    Toast.makeText(this, "Mohon masukkan Token Ujian terlebih dahulu!", Toast.LENGTH_SHORT).show()
                    etToken.requestFocus()
                    return@setOnClickListener
                }
                if (tokenServer.isNotBlank() && inputToken != tokenServer.trim().uppercase()) {
                    Toast.makeText(this, "Token Salah! Silakan cek kembali pengawas ruangan.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // Mulai Proses Download Soal & Masuk Ujian
            pbLoading.visibility = View.VISIBLE
            layoutBtnMulaiContent.visibility = View.INVISIBLE
            btnMulai.isEnabled = false
            btnBatal.isEnabled = false
            etToken.isEnabled = false

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val requestPayload = mapOf(
                        "nisn" to nisnSiswa,
                        "id_jadwal" to idJadwal,
                        "token" to (if (isWajibToken) inputToken else "")
                    )

                    val response = ApiClient.instance.downloadSoal(requestPayload)

                    withContext(Dispatchers.Main) {
                        pbLoading.visibility = View.GONE
                        layoutBtnMulaiContent.visibility = View.VISIBLE
                        btnMulai.isEnabled = true
                        btnBatal.isEnabled = true
                        etToken.isEnabled = true

                        if (response.isSuccessful && response.body()?.status == true) {
                            val downloadData = response.body()

                            if (downloadData != null && downloadData.data_soal.isNotEmpty()) {
                                // Bersihkan sesi lokal lama
                                dbHelper.clearSemuaData()

                                // Simpan soal ke SQLite offline
                                val jsonSoalString = Gson().toJson(downloadData.data_soal)
                                dbHelper.simpanSesiSoal(
                                    idUjian = downloadData.id_ujian_siswa,
                                    durasi = downloadData.durasi,
                                    minFinish = downloadData.min_finish ?: "0",
                                    jsonSoal = jsonSoalString
                                )

                                Toast.makeText(this@KonfirmasiActivity, "Soal berhasil diunduh! Memasuki ruang ujian.", Toast.LENGTH_SHORT).show()

                                val intentUjian = Intent(this@KonfirmasiActivity, UjianActivity::class.java).apply {
                                    putExtra("MAPEL", mapel)
                                    putExtra("JENIS_UJIAN", jenisUjian)
                                }
                                sharedPref.edit()
                                    .putString("CURRENT_MAPEL_UJIAN", mapel)
                                    .putString("CURRENT_JENIS_UJIAN", jenisUjian)
                                    .apply()
                                startActivity(intentUjian)
                                finish()

                            } else {
                                Toast.makeText(this@KonfirmasiActivity, "Soal belum tersedia di server!", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            val errorMsg = response.body()?.message ?: "Gagal mendownload soal ujian"
                            Toast.makeText(this@KonfirmasiActivity, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        pbLoading.visibility = View.GONE
                        layoutBtnMulaiContent.visibility = View.VISIBLE
                        btnMulai.isEnabled = true
                        btnBatal.isEnabled = true
                        etToken.isEnabled = true
                        Toast.makeText(this@KonfirmasiActivity, "Error Koneksi: ${e.localizedMessage ?: "Cek koneksi internet"}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun formatTanggalWaktu(waktuStr: String?): Pair<String, String> {
        if (waktuStr.isNullOrBlank() || waktuStr == "-") {
            return Pair("Hari Ini", "08:00 WIB")
        }
        val formats = arrayOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                val d = sdf.parse(waktuStr)
                if (d != null) {
                    val localeId = Locale.forLanguageTag("id-ID")
                    val dateFmt = SimpleDateFormat("dd MMM yyyy", localeId)
                    val timeFmt = SimpleDateFormat("HH:mm", localeId)
                    return Pair(dateFmt.format(d), "${timeFmt.format(d)} WIB")
                }
            } catch (_: Exception) {}
        }
        val datePart = if (waktuStr.length >= 10) waktuStr.take(10) else waktuStr
        val timePart = if (waktuStr.length >= 16) waktuStr.substring(11, 16) else "08:00"
        return Pair(datePart, "$timePart WIB")
    }
}
