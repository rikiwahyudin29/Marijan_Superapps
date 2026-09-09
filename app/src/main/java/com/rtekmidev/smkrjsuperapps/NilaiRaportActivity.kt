@file:Suppress("DEPRECATION")
package com.rtekmidev.smkrjsuperapps

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
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.NilaiMapel
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
    private lateinit var progressBarNilai: View
    private lateinit var btnDownloadPdf: LinearLayout
    private var downloadUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nilai_raport)

        val headerLayout = findViewById<View>(R.id.headerLayout)
        if (headerLayout != null) {
            
            // Set header text & back button
            val tvHeaderTitle = headerLayout.findViewById<TextView>(R.id.tvHeaderTitle)
            if (tvHeaderTitle != null) tvHeaderTitle.text = "Nilai Raport"

            headerLayout.findViewById<ImageView>(R.id.btnBack)?.setOnClickListener {
                finish()
            }

            val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
            val namaSiswa = sharedPref.getString("nama_siswa", "Siswa")
            val fotoProfil = sharedPref.getString("foto_profil", "")

            val ivProfilPhoto = headerLayout.findViewById<ImageView>(R.id.ivProfilPhoto)
            val tvProfilInisial = headerLayout.findViewById<TextView>(R.id.tvProfilInisial)
            val cvProfilPic = headerLayout.findViewById<androidx.cardview.widget.CardView>(R.id.cvProfilPic)
            com.rtekmidev.smkrjsuperapps.util.AvatarHelper.setAvatar(this, namaSiswa, fotoProfil, ivProfilPhoto, tvProfilInisial, cvProfilPic)
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
                            val rataRataStr = r?.rata_rata_nilai?.toString() ?: "0"
                            tvRataRataNilai.text = rataRataStr
                            tvTrendNilai.text = r?.trend_nilai ?: ""
                            
                            val avgVal = rataRataStr.toDoubleOrNull() ?: 0.0
                            progressRataRata.progress = avgVal.toInt()

                            val hadirStr = r?.kehadiran_persen?.toString() ?: "0"
                            val hadir = hadirStr.toIntOrNull() ?: 0
                            tvPersentaseKehadiran.text = "${hadir}%"
                            tvStatusKehadiran.text = r?.kehadiran_status ?: "-"
                            progressKehadiran.progress = hadir

                            val rankStr = r?.peringkat_kelas?.toString() ?: "-"
                            tvPeringkat.text = if (rankStr == "0" || rankStr == "-" || rankStr.isEmpty()) "-" else "#$rankStr"
                            val totalStr = r?.total_siswa?.toString() ?: "-"
                            tvTotalSiswa.text = "dari $totalStr Siswa"
                            val rankParalelStr = r?.peringkat_paralel?.toString() ?: "-"
                            tvPeringkatParalel.text = "Peringkat Paralel: ${if (rankParalelStr == "0" || rankParalelStr == "-" || rankParalelStr.isEmpty()) "-" else "#$rankParalelStr"}"
                            
                            downloadUrl = r?.url_download_pdf

                            // Daftar Nilai
                            renderDaftarNilai(data.daftar_nilai ?: emptyList())
                        }
                    } else {
                        Toast.makeText(this@NilaiRaportActivity, response.body()?.pesan ?: "Gagal memuat raport", Toast.LENGTH_SHORT).show()
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

        if (list.isEmpty()) {
            val emptyTv = TextView(this).apply {
                text = "Belum ada data nilai raport untuk semester ini."
                textSize = 13f
                setTextColor(Color.parseColor("#94A3B8"))
                gravity = android.view.Gravity.CENTER
                setPadding(0, 40, 0, 40)
            }
            wadahDaftarNilai.addView(emptyTv)
            return
        }

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
            tvNilaiAkhir.text = item.nilai_akhir?.toString() ?: "-"
            tvKKM.text = item.kkm?.toString() ?: "-"
            tvFormatif.text = item.formatif?.toString() ?: "-"
            tvSumatif.text = item.sumatif?.toString() ?: "-"
            
            if (item.deskripsi.isNullOrEmpty()) {
                tvDeskripsi.visibility = View.GONE
            } else {
                tvDeskripsi.text = item.deskripsi
            }

            wadahDaftarNilai.addView(itemView)
        }
    }
}
