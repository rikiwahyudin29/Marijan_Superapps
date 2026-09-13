package com.rtekmidev.marijansuperapps

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.text.HtmlCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.rtekmidev.marijansuperapps.util.StatusBarHelper

class LihatJawabanActivity : AppCompatActivity() {

    private lateinit var tvNilaiBesar: TextView
    private lateinit var tvNilaiScale: TextView
    private lateinit var tvBadgeStatusPenilaian: TextView
    private lateinit var tvHeroTitle: TextView
    private lateinit var tvHeroSub: TextView
    private lateinit var tvKomentarGuru: TextView
    private lateinit var badgeScoreCircle: FrameLayout

    private lateinit var tvMapel: TextView
    private lateinit var tvStatusKumpulBadge: TextView
    private lateinit var tvJudulTugas: TextView
    private lateinit var tvWaktuKumpul: TextView
    private lateinit var tvDeadlineTugas: TextView
    private lateinit var layDeskripsiTugas: LinearLayout
    private lateinit var tvDeskripsiTugas: TextView

    private lateinit var tvCatatanSiswa: TextView

    private lateinit var layFileAda: LinearLayout
    private lateinit var layFileKosong: LinearLayout
    private lateinit var ivFileIcon: ImageView
    private lateinit var tvNamaFile: TextView
    private lateinit var tvStatusFile: TextView
    private lateinit var btnBukaFile: Button
    private lateinit var btnUnduhFile: Button

    private lateinit var btnPerbaruiJawaban: Button

    private var idTugas: String = ""
    private var mapel: String = ""
    private var judul: String = ""
    private var deadline: String = ""
    private var deskripsi: String = ""
    private var filePendukung: String = ""
    private var fileJawaban: String = ""
    private var nilai: String = ""
    private var komentarGuru: String = ""
    private var catatanSiswa: String = ""
    private var tglKumpul: String = ""
    private var statusKumpul: String = ""

    private var currentDownloadId: Long = -1L
    private var isReceiverRegistered: Boolean = false
    private var currentDownloadingFileName: String = ""

    private val downloadReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == DownloadManager.ACTION_DOWNLOAD_COMPLETE) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id != -1L && id == currentDownloadId) {
                    val dm = getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
                    val query = DownloadManager.Query().setFilterById(id)
                    val cursor = dm?.query(query)
                    var isSuccess = false
                    var downloadedTitle = currentDownloadingFileName

                    if (cursor != null && cursor.moveToFirst()) {
                        val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                        if (statusIndex != -1) {
                            val status = cursor.getInt(statusIndex)
                            isSuccess = (status == DownloadManager.STATUS_SUCCESSFUL)
                        }
                        val titleIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TITLE)
                        if (titleIndex != -1) {
                            val t = cursor.getString(titleIndex)
                            if (!t.isNullOrBlank()) downloadedTitle = t
                        }
                        cursor.close()
                    }

                    if (isSuccess) {
                        Toast.makeText(
                            this@LihatJawabanActivity,
                            "✅ Berkas $downloadedTitle sudah selesai diunduh!\nTersimpan di folder Download.",
                            Toast.LENGTH_LONG
                        ).show()
                    } else {
                        Toast.makeText(
                            this@LihatJawabanActivity,
                            "❌ Unduhan berkas $downloadedTitle gagal atau dibatalkan.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lihat_jawaban)

        // Terapkan Status Bar Transparan & Insets Safe Padding
        StatusBarHelper.setupTranslucentBar(this)

        // Tangani navigasi bawah agar tombol perbarui tidak tertutup nav bar HP
        findViewById<View>(R.id.bottomBar)?.let { bar ->
            ViewCompat.setOnApplyWindowInsetsListener(bar) { view, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom)
                insets
            }
        }

        initViews()
        setupHeader()
        loadDataFromIntent()
        registerDownloadReceiverSafe()
    }

    private fun initViews() {
        tvNilaiBesar = findViewById(R.id.tvNilaiBesar)
        tvNilaiScale = findViewById(R.id.tvNilaiScale)
        tvBadgeStatusPenilaian = findViewById(R.id.tvBadgeStatusPenilaian)
        tvHeroTitle = findViewById(R.id.tvHeroTitle)
        tvHeroSub = findViewById(R.id.tvHeroSub)
        tvKomentarGuru = findViewById(R.id.tvKomentarGuru)
        badgeScoreCircle = findViewById(R.id.badgeScoreCircle)

        tvMapel = findViewById(R.id.tvMapel)
        tvStatusKumpulBadge = findViewById(R.id.tvStatusKumpulBadge)
        tvJudulTugas = findViewById(R.id.tvJudulTugas)
        tvWaktuKumpul = findViewById(R.id.tvWaktuKumpul)
        tvDeadlineTugas = findViewById(R.id.tvDeadlineTugas)
        layDeskripsiTugas = findViewById(R.id.layDeskripsiTugas)
        tvDeskripsiTugas = findViewById(R.id.tvDeskripsiTugas)

        tvCatatanSiswa = findViewById(R.id.tvCatatanSiswa)

        layFileAda = findViewById(R.id.layFileAda)
        layFileKosong = findViewById(R.id.layFileKosong)
        ivFileIcon = findViewById(R.id.ivFileIcon)
        tvNamaFile = findViewById(R.id.tvNamaFile)
        tvStatusFile = findViewById(R.id.tvStatusFile)
        btnBukaFile = findViewById(R.id.btnBukaFile)
        btnUnduhFile = findViewById(R.id.btnUnduhFile)

        btnPerbaruiJawaban = findViewById(R.id.btnPerbaruiJawaban)
    }

    private fun setupHeader() {
        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val namaSiswa = sharedPref.getString("nama_siswa", "Siswa")
        val fotoProfil = sharedPref.getString("foto_profil", "")

        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Detail Jawaban"
        findViewById<TextView>(R.id.tvHeaderCategory)?.text = "E-LEARNING SISWA"
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }

        val ivProfil = findViewById<ImageView>(R.id.ivProfilPhoto)
        val tvProfilInisial = findViewById<TextView>(R.id.tvProfilInisial)
        val cvProfilPic = findViewById<CardView>(R.id.cvProfilPic)

        if (!fotoProfil.isNullOrEmpty() && ivProfil != null) {
            ivProfil.visibility = View.VISIBLE
            tvProfilInisial?.visibility = View.GONE
            Glide.with(this)
                .load(fotoProfil)
                .circleCrop()
                .into(ivProfil)
        } else {
            ivProfil?.visibility = View.GONE
            tvProfilInisial?.visibility = View.VISIBLE
            val inisial = if (!namaSiswa.isNullOrEmpty()) {
                val parts = namaSiswa.trim().split(" ")
                if (parts.size > 1) "${parts[0].first()}${parts[1].first()}".uppercase()
                else parts[0].take(2).uppercase()
            } else "SW"
            tvProfilInisial?.text = inisial
        }
    }

    private fun loadDataFromIntent() {
        idTugas = intent.getStringExtra("ID_TUGAS") ?: ""
        mapel = intent.getStringExtra("MAPEL") ?: "Mata Pelajaran"
        judul = intent.getStringExtra("JUDUL") ?: "Tugas"
        deadline = intent.getStringExtra("DEADLINE") ?: "-"
        deskripsi = intent.getStringExtra("DESKRIPSI") ?: ""
        filePendukung = intent.getStringExtra("FILE_PENDUKUNG") ?: ""
        fileJawaban = intent.getStringExtra("FILE_JAWABAN") ?: ""
        nilai = intent.getStringExtra("NILAI") ?: ""
        komentarGuru = intent.getStringExtra("KOMENTAR_GURU") ?: ""
        catatanSiswa = intent.getStringExtra("CATATAN_SISWA") ?: ""
        tglKumpul = intent.getStringExtra("TGL_KUMPUL") ?: ""
        statusKumpul = intent.getStringExtra("STATUS_KUMPUL") ?: "Tepat Waktu"

        renderUi()
    }

    private fun renderUi() {
        // --- 1. HERO NILAI & REVIEW ---
        val isGraded = nilai.isNotEmpty() && nilai != "null" && nilai != "-"
        if (isGraded) {
            tvNilaiBesar.text = nilai
            tvNilaiScale.visibility = View.VISIBLE
            tvBadgeStatusPenilaian.text = "SUDAH DINILAI"
            tvBadgeStatusPenilaian.setBackgroundResource(R.drawable.bg_badge_green_soft)
            tvBadgeStatusPenilaian.setTextColor(Color.parseColor("#059669"))
            tvHeroTitle.text = "Nilai: $nilai / 100"
            tvHeroSub.text = "Tugas telah dikoreksi oleh Guru Mapel"

            if (komentarGuru.isNotEmpty() && komentarGuru != "null") {
                tvKomentarGuru.text = "“$komentarGuru”"
                tvKomentarGuru.setTextColor(Color.parseColor("#1E293B"))
            } else {
                tvKomentarGuru.text = "Guru tidak menyertakan catatan tambahan."
                tvKomentarGuru.setTextColor(Color.parseColor("#64748B"))
            }
        } else {
            tvNilaiBesar.text = "⏳"
            tvNilaiScale.visibility = View.GONE
            tvBadgeStatusPenilaian.text = "MENUNGGU PENILAIAN"
            tvBadgeStatusPenilaian.setBackgroundResource(R.drawable.bg_badge_amber_soft)
            tvBadgeStatusPenilaian.setTextColor(Color.parseColor("#D97706"))
            tvHeroTitle.text = "Menunggu Nilai"
            tvHeroSub.text = "Tugas sudah tersimpan & dalam antrean koreksi"

            tvKomentarGuru.text = "Guru mata pelajaran belum memberikan nilai atau catatan evaluasi."
            tvKomentarGuru.setTextColor(Color.parseColor("#64748B"))
        }

        // --- 2. RINGKASAN TUGAS ---
        tvMapel.text = mapel
        tvJudulTugas.text = judul

        if (statusKumpul.contains("Terlambat", ignoreCase = true)) {
            tvStatusKumpulBadge.text = "Terlambat"
            tvStatusKumpulBadge.setBackgroundResource(R.drawable.bg_badge_red_soft)
            tvStatusKumpulBadge.setTextColor(Color.parseColor("#E11D48"))
        } else {
            tvStatusKumpulBadge.text = "Tepat Waktu"
            tvStatusKumpulBadge.setBackgroundResource(R.drawable.bg_badge_green_soft)
            tvStatusKumpulBadge.setTextColor(Color.parseColor("#059669"))
        }

        tvWaktuKumpul.text = if (tglKumpul.isNotEmpty()) "Dikumpulkan: $tglKumpul WIB" else "Status: Sudah Dikumpulkan"
        tvDeadlineTugas.text = "Batas Waktu: $deadline"

        if (deskripsi.isNotEmpty()) {
            layDeskripsiTugas.visibility = View.VISIBLE
            val cleanDesc = HtmlCompat.fromHtml(deskripsi, HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
            tvDeskripsiTugas.text = cleanDesc
        } else {
            layDeskripsiTugas.visibility = View.GONE
        }

        // --- 3. CATATAN SISWA ---
        if (catatanSiswa.isNotEmpty() && catatanSiswa != "null") {
            tvCatatanSiswa.text = catatanSiswa
            tvCatatanSiswa.setTextColor(Color.parseColor("#1E293B"))
        } else {
            tvCatatanSiswa.text = "Tidak ada catatan tertulis saat mengumpulkan tugas."
            tvCatatanSiswa.setTextColor(Color.parseColor("#64748B"))
        }

        // --- 4. BERKAS LAMPIRAN JAWABAN ---
        if (fileJawaban.isNotEmpty() && fileJawaban != "null") {
            layFileAda.visibility = View.VISIBLE
            layFileKosong.visibility = View.GONE

            val rawFileName = fileJawaban.substringAfterLast("/")
            val cleanFileName = if (rawFileName.contains("_")) rawFileName.substringAfter("_") else rawFileName
            tvNamaFile.text = cleanFileName

            val lower = cleanFileName.lowercase()
            when {
                lower.endsWith(".pdf") -> {
                    ivFileIcon.setImageResource(R.drawable.ic_pdf_document)
                    tvStatusFile.text = "Dokumen PDF • Tersimpan di server"
                }
                lower.endsWith(".doc") || lower.endsWith(".docx") -> {
                    ivFileIcon.setImageResource(R.drawable.ic_word_document)
                    tvStatusFile.text = "Dokumen Word • Tersimpan di server"
                }
                lower.endsWith(".jpg") || lower.endsWith(".jpeg") || lower.endsWith(".png") -> {
                    ivFileIcon.setImageResource(R.drawable.ic_camera_edit)
                    tvStatusFile.text = "Gambar Berkas • Tersimpan di server"
                }
                else -> {
                    ivFileIcon.setImageResource(R.drawable.ic_pdf_document)
                    tvStatusFile.text = "Berkas Tugas • Tersimpan di server"
                }
            }

            // Aksi Buka File
            btnBukaFile.setOnClickListener {
                val intent = Intent(this, FileViewerActivity::class.java)
                intent.putExtra("FILE_URL", fileJawaban)
                intent.putExtra("TITLE", "Jawaban: $judul")
                startActivity(intent)
            }

            // Aksi Unduh File
            btnUnduhFile.setOnClickListener {
                downloadFile(fileJawaban, cleanFileName)
            }
        } else {
            layFileAda.visibility = View.GONE
            layFileKosong.visibility = View.VISIBLE
        }

        // --- 5. TOMBOL PERBARUI JAWABAN ---
        btnPerbaruiJawaban.setOnClickListener {
            val intent = Intent(this, KumpulTugasActivity::class.java)
            intent.putExtra("ID_TUGAS", idTugas)
            intent.putExtra("MAPEL", mapel)
            intent.putExtra("JUDUL", judul)
            intent.putExtra("DEADLINE", deadline)
            intent.putExtra("DESKRIPSI", deskripsi)
            intent.putExtra("FILE_PENDUKUNG", filePendukung)
            intent.putExtra("IS_PERBARUI", true)
            startActivity(intent)
            finish()
        }
    }

    private fun downloadFile(url: String, fileName: String) {
        try {
            val dm = getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            val uri = Uri.parse(url)
            val request = DownloadManager.Request(uri)
                .setTitle(fileName)
                .setDescription("Mengunduh berkas jawaban tugas...")
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(true)

            currentDownloadingFileName = fileName
            currentDownloadId = dm.enqueue(request)
            Toast.makeText(this, "Mulai mengunduh: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Gagal mengunduh: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun registerDownloadReceiverSafe() {
        if (!isReceiverRegistered) {
            val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(downloadReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
            } else {
                registerReceiver(downloadReceiver, filter)
            }
            isReceiverRegistered = true
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isReceiverRegistered) {
            try {
                unregisterReceiver(downloadReceiver)
                isReceiverRegistered = false
            } catch (_: Exception) {}
        }
    }
}
