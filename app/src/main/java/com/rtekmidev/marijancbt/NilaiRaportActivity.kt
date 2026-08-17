@file:Suppress("DEPRECATION")
package com.rtekmidev.marijancbt

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.bumptech.glide.Glide
import com.rtekmidev.marijancbt.api.ApiClient
import com.rtekmidev.marijancbt.api.NilaiMapel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NilaiRaportActivity : AppCompatActivity() {

    private lateinit var tvSemesterTahun: TextView
    private lateinit var tvDeskripsiKelas: TextView
    private lateinit var tvRataRataNilai: TextView
    private lateinit var tvTrendNilai: TextView
    private lateinit var progressRataRata: ProgressBar
    private lateinit var tvPersentaseKehadiran: TextView
    private lateinit var tvStatusKehadiran: TextView
    private lateinit var progressKehadiran: ProgressBar
    private lateinit var tvPeringkat: TextView
    private lateinit var tvTotalSiswa: TextView
    private lateinit var tvPeringkatParalel: TextView
    private lateinit var wadahDaftarNilai: LinearLayout
    private lateinit var progressBarNilai: ProgressBar
    private lateinit var btnDownloadPdf: LinearLayout
    private var downloadUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nilai_raport)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = true

        val headerLayout = findViewById<View>(R.id.headerLayout)
        if (headerLayout != null) {
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(headerLayout) { v, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                val density = resources.displayMetrics.density
                val extraPadding = (20 * density).toInt()
                v.setPadding(v.paddingLeft, systemBars.top + extraPadding, v.paddingRight, v.paddingBottom)
                insets
            }
            
            // Set header text
            val tvAppTitle = headerLayout.findViewById<TextView>(R.id.tvAppTitle)
            if (tvAppTitle != null) tvAppTitle.text = "Nilai Raport"

            val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
            val namaSiswa = sharedPref.getString("nama_siswa", "Siswa")
            val kelasLengkap = sharedPref.getString("kelas_lengkap", "Kelas -")
            val fotoProfil = sharedPref.getString("foto_profil", "")
            
            headerLayout.findViewById<TextView>(R.id.tvNamaDashboard)?.text = "Halo, $namaSiswa"
            headerLayout.findViewById<TextView>(R.id.tvKelas)?.text = kelasLengkap

            val ivProfilPhoto = headerLayout.findViewById<ImageView>(R.id.ivProfilPhoto)
            if (ivProfilPhoto != null && !fotoProfil.isNullOrEmpty()) {
                Glide.with(this)
                    .load(fotoProfil)
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .circleCrop()
                    .into(ivProfilPhoto)
            }
            
            headerLayout.setOnClickListener {
                finish()
            }
        }

        initViews()
        loadDataRaport()

        btnDownloadPdf.setOnClickListener {
            if (downloadUrl.isNullOrEmpty()) {
                Toast.makeText(this, "URL PDF belum tersedia", Toast.LENGTH_SHORT).show()
            } else {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                startActivity(intent)
            }
        }
    }

    private fun initViews() {
        tvSemesterTahun = findViewById(R.id.tvSemesterTahun)
        tvDeskripsiKelas = findViewById(R.id.tvDeskripsiKelas)
        tvRataRataNilai = findViewById(R.id.tvRataRataNilai)
        tvTrendNilai = findViewById(R.id.tvTrendNilai)
        progressRataRata = findViewById(R.id.progressRataRata)
        tvPersentaseKehadiran = findViewById(R.id.tvPersentaseKehadiran)
        tvStatusKehadiran = findViewById(R.id.tvStatusKehadiran)
        progressKehadiran = findViewById(R.id.progressKehadiran)
        tvPeringkat = findViewById(R.id.tvPeringkat)
        tvTotalSiswa = findViewById(R.id.tvTotalSiswa)
        tvPeringkatParalel = findViewById(R.id.tvPeringkatParalel)
        wadahDaftarNilai = findViewById(R.id.wadahDaftarNilai)
        progressBarNilai = findViewById(R.id.progressBarNilai)
        btnDownloadPdf = findViewById(R.id.btnDownloadPdf)
    }

    private fun loadDataRaport() {
        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val nisnSiswa = sharedPref.getString("nisn", "") ?: ""

        progressBarNilai.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.getNilaiRaport(nisnSiswa)
                withContext(Dispatchers.Main) {
                    progressBarNilai.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.status == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            // Info Akademik
                            tvSemesterTahun.text = data.info_akademik?.semester_tahun ?: "-"
                            tvDeskripsiKelas.text = data.info_akademik?.deskripsi_kelas ?: "-"

                            // Ringkasan
                            val r = data.ringkasan
                            tvRataRataNilai.text = r?.rata_rata_nilai ?: "0"
                            tvTrendNilai.text = r?.trend_nilai ?: ""
                            
                            val avgVal = r?.rata_rata_nilai?.toDoubleOrNull() ?: 0.0
                            progressRataRata.progress = avgVal.toInt()

                            val hadir = r?.kehadiran_persen ?: 0
                            tvPersentaseKehadiran.text = "${hadir}%"
                            tvStatusKehadiran.text = r?.kehadiran_status ?: "-"
                            progressKehadiran.progress = hadir

                            tvPeringkat.text = "#${r?.peringkat_kelas ?: "-"}"
                            tvTotalSiswa.text = "dari ${r?.total_siswa ?: "-"} Siswa"
                            tvPeringkatParalel.text = "Peringkat Paralel: #${r?.peringkat_paralel ?: "-"}"
                            
                            downloadUrl = r?.url_download_pdf

                            // Daftar Nilai
                            renderDaftarNilai(data.daftar_nilai ?: emptyList())
                        }
                    } else {
                        Toast.makeText(this@NilaiRaportActivity, "Gagal memuat raport", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBarNilai.visibility = View.GONE
                    Toast.makeText(this@NilaiRaportActivity, "Error koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun renderDaftarNilai(list: List<NilaiMapel>) {
        // Hapus child lain kecuali ProgressBar yang sudah disembunyikan
        wadahDaftarNilai.removeAllViews()

        for (item in list) {
            val itemView = layoutInflater.inflate(R.layout.item_nilai_mapel_merdeka, wadahDaftarNilai, false)
            
            val tvMapel = itemView.findViewById<TextView>(R.id.tvMapel)
            val tvGuru = itemView.findViewById<TextView>(R.id.tvGuru)
            val tvNilaiAkhir = itemView.findViewById<TextView>(R.id.tvNilaiAkhir)
            val tvKKM = itemView.findViewById<TextView>(R.id.tvKKM)
            val tvFormatif = itemView.findViewById<TextView>(R.id.tvFormatif)
            val tvSumatif = itemView.findViewById<TextView>(R.id.tvSumatif)
            val tvDeskripsi = itemView.findViewById<TextView>(R.id.tvDeskripsi)

            tvMapel.text = item.mapel ?: "-"
            tvGuru.text = item.guru ?: "-"
            tvNilaiAkhir.text = item.nilai_akhir ?: "-"
            tvKKM.text = item.kkm ?: "-"
            tvFormatif.text = item.formatif ?: "-"
            tvSumatif.text = item.sumatif ?: "-"
            
            if (item.deskripsi.isNullOrEmpty()) {
                tvDeskripsi.visibility = View.GONE
            } else {
                tvDeskripsi.text = item.deskripsi
            }

            wadahDaftarNilai.addView(itemView)
        }
    }
}
