package com.rtekmidev.smkrjsuperapps

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import kotlinx.coroutines.*

class MateriTugasActivity : AppCompatActivity() {

    private var nisnSiswa = ""
    private var jenisKonten = "" // "MATERI" atau "TUGAS"

    // Kita buat class bantuan agar mudah dirender ke UI
    data class DisplayItem(
        val id: String, val judul: String, val mapel: String,
        val guru: String, val waktu: String, val urlFile: String?,
        val isSelesai: Boolean, val nilai: String?,
        val komentarGuru: String?, val fileJawaban: String?,
        val rawStatus: String?,
        val deskripsi: String? = null,
        val jenisFile: String? = null,
        val linkYoutube: String? = null,
        val ukuranFile: String? = null
    )

    private var listDataAsli = listOf<DisplayItem>()
    private var filterAktif = "Semua"
    private var searchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_materi_tugas)

        nisnSiswa = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE).getString("nisn", "") ?: ""
        jenisKonten = intent.getStringExtra("KONTEN") ?: "TUGAS"

        val tvJudul = findViewById<TextView>(R.id.tvJudulHalaman)
        val tvSubJudul = findViewById<TextView>(R.id.tvSubJudulHalaman)

        if (jenisKonten == "MATERI") {
            tvJudul.text = "Materi Pelajaran"
            tvSubJudul.text = "Akses dan unduh bahan ajar guru."
            val etSearch = findViewById<EditText>(R.id.etSearchMateri)
            etSearch.visibility = View.VISIBLE
            etSearch.addTextChangedListener(object : android.text.TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    searchQuery = s.toString()
                    renderData()
                }
                override fun afterTextChanged(s: android.text.Editable?) {}
            })
        } else {
            tvJudul.text = "Tugas Aktif"
            tvSubJudul.text = "Kerjakan dan kumpulkan tugasmu."
            findViewById<EditText>(R.id.etSearchMateri).visibility = View.GONE
        }

        findViewById<ImageView>(R.id.btnBackAkademik).setOnClickListener { finish() }

        muatDataAkademik()
    }

    private fun muatDataAkademik() {
        val loading = findViewById<LinearLayout>(R.id.loadingAkademik)
        loading.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (jenisKonten == "MATERI") {
                    val resp = ApiClient.instance.getMateri(nisnSiswa)
                    withContext(Dispatchers.Main) {
                        loading.visibility = View.GONE
                        if (resp.isSuccessful && resp.body()?.status == true) {
                            // Konversi respon ke format seragam
                            val rawData = resp.body()?.data
                            if (rawData != null && rawData.isJsonArray) {
                                val arr = rawData.asJsonArray
                                val extractedList = mutableListOf<DisplayItem>()
                                for (groupItem in arr) {
                                    val groupObj = groupItem.asJsonObject
                                    val mapel = groupObj.get("mapel")?.asString ?: "UMUM"
                                    val materiArr = groupObj.get("materi")?.asJsonArray
                                    if (materiArr != null) {
                                        for (materiItem in materiArr) {
                                            val obj = materiItem.asJsonObject
                                            fun getStr(key: String): String? {
                                                val el = obj.get(key)
                                                return if (el != null && !el.isJsonNull) el.asString else null
                                            }
                                            val id = getStr("id_materi") ?: getStr("id") ?: ""
                                            val judul = getStr("judul") ?: "Tanpa Judul"
                                            val guru = getStr("guru") ?: getStr("nama_guru") ?: "-"
                                            val tanggal = getStr("tanggal") ?: "-"
                                            val file = getStr("file_materi") ?: getStr("file")
                                            val deskripsi = getStr("deskripsi")
                                            val jenisFile = getStr("jenis_file")
                                            val linkYoutube = getStr("link_youtube")
                                            val ukuranFile = getStr("ukuran_file") ?: getStr("ukuran")
                                            
                                            extractedList.add(
                                                DisplayItem(
                                                    id, judul, mapel, guru, tanggal, file, false, null, null, null, null,
                                                    deskripsi, jenisFile, linkYoutube, ukuranFile
                                                )
                                            )
                                        }
                                    }
                                }
                                listDataAsli = extractedList
                            } else {
                                listDataAsli = emptyList()
                            }
                            buatFilterChips()
                            renderData()
                        } else {
                            Toast.makeText(this@MateriTugasActivity, "Gagal memuat materi", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    val resp = ApiClient.instance.getTugas(nisnSiswa)
                    withContext(Dispatchers.Main) {
                        loading.visibility = View.GONE
                        if (resp.isSuccessful && resp.body()?.status == true) {
                            // Konversi respon ke format seragam
                            val tugasData = resp.body()?.data
                            
                            val listBerlangsung = tugasData?.berlangsung?.map {
                                // Apapun statusnya selain "Belum Selesai", kita anggap sudah dikerjakan
                                val isDone = !(it.status?.equals("Belum Selesai", ignoreCase = true) == true)
                                DisplayItem(
                                    it.id_tugas?.toString() ?: "", it.judul ?: "Tanpa Judul",
                                    it.mapel ?: "UMUM", "-", it.deadline ?: "-", null,
                                    isDone, it.nilai, it.komentar_guru, it.file_jawaban,
                                    it.status
                                )
                            } ?: emptyList()

                            val listSelesai = tugasData?.selesai?.map {
                                val isDone = true // Asumsikan array selesai sudah pasti selesai
                                DisplayItem(
                                    it.id_tugas?.toString() ?: "", it.judul ?: "Tanpa Judul",
                                    it.mapel ?: "UMUM", "-", it.deadline ?: "-", null,
                                    isDone, it.nilai, it.komentar_guru, it.file_jawaban,
                                    it.status
                                )
                            } ?: emptyList()
                            
                            listDataAsli = listBerlangsung + listSelesai
                            buatFilterChips()
                            renderData()
                        } else {
                            Toast.makeText(this@MateriTugasActivity, "Gagal memuat tugas", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE
                    Toast.makeText(this@MateriTugasActivity, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun buatFilterChips() {
        val wadahFilter = findViewById<LinearLayout>(R.id.wadahFilterMapel)
        wadahFilter.removeAllViews()

        val setMapel = mutableSetOf("Semua")
        listDataAsli.forEach { setMapel.add(it.mapel) }

        setMapel.forEach { mapel ->
            val chip = Button(this).apply {
                text = mapel
                isAllCaps = false
                textSize = 13f
                setTextColor(if (filterAktif == mapel) Color.WHITE else Color.parseColor("#4B5563"))
                setBackgroundResource(android.R.color.transparent)

                if (filterAktif == mapel) {
                    setBackgroundColor(Color.parseColor("#111827"))
                } else {
                    setBackgroundColor(Color.parseColor("#E5E7EB"))
                }

                setPadding(30, 0, 30, 0)
                setOnClickListener {
                    filterAktif = mapel
                    buatFilterChips()
                    renderData()
                }
            }

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                100
            ).apply { setMargins(0, 0, 20, 0) }

            wadahFilter.addView(chip, params)
        }
    }

    private fun renderData() {
        val wadah = findViewById<LinearLayout>(R.id.wadahDataAkademik)
        wadah.removeAllViews()

        val listFiltered = if (filterAktif == "Semua") listDataAsli
        else listDataAsli.filter { it.mapel == filterAktif }

        val listFinal = if (searchQuery.isNotEmpty()) {
            listFiltered.filter { it.judul.contains(searchQuery, true) || it.mapel.contains(searchQuery, true) }
        } else listFiltered

        if (listFinal.isEmpty()) {
            val tvKosong = TextView(this)
            tvKosong.text = if (jenisKonten == "MATERI") "Materi tidak ditemukan." else "Yeay! Semua tugas sudah selesai."
            tvKosong.setTextColor(Color.parseColor("#6B7280"))
            wadah.addView(tvKosong)
        } else {
            if (jenisKonten == "MATERI") {
                val grouped = listFinal.groupBy { it.mapel }
                grouped.forEach { (mapel, items) ->
                    val header = layoutInflater.inflate(R.layout.item_header_mapel, null)
                    header.findViewById<TextView>(R.id.tvHeaderMapel).text = mapel
                    wadah.addView(header)

                    items.forEach { item ->
                        val card = layoutInflater.inflate(R.layout.item_materi_modern, null) as CardView
                        setCardDataMateriModern(card, item)
                        wadah.addView(card)
                    }
                }
            } else {
                listFinal.forEach { item ->
                    val card = layoutInflater.inflate(R.layout.item_akademik, null) as CardView
                    setCardData(card, item)
                    wadah.addView(card)
                }
            }
        }
    }

    private fun setCardDataMateriModern(card: CardView, item: DisplayItem) {
        card.findViewById<TextView>(R.id.tvJudulMateri).text = item.judul
        card.findViewById<TextView>(R.id.tvDeskripsi).text = item.deskripsi ?: "Tanpa deskripsi"
        card.findViewById<TextView>(R.id.tvTanggalMateri).text = item.waktu

        val tvBadge = card.findViewById<TextView>(R.id.tvBadgeType)
        val tvAction = card.findViewById<TextView>(R.id.tvAction)
        val border = card.findViewById<View>(R.id.viewBorderType)
        val tvUkuran = card.findViewById<TextView>(R.id.tvUkuran)

        val jenis = item.jenisFile?.lowercase() ?: ""
        tvBadge.text = item.jenisFile ?: "File"

        if (jenis == "youtube" || jenis == "video") {
            border.setBackgroundColor(Color.parseColor("#F97316")) // Orange
            tvBadge.setTextColor(Color.parseColor("#EA580C"))
            tvBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFEDD5"))
            tvAction.text = "Tonton →"
            tvAction.setTextColor(Color.parseColor("#EA580C"))
        } else {
            border.setBackgroundColor(Color.parseColor("#06B6D4")) // Cyan
            tvBadge.setTextColor(Color.parseColor("#0284C7"))
            tvBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#E0F2FE"))
            tvAction.text = "Unduh →"
            tvAction.setTextColor(Color.parseColor("#06B6D4"))
        }

        tvUkuran.text = item.ukuranFile ?: ""
        if (tvUkuran.text.isEmpty()) tvUkuran.visibility = View.GONE

        card.setOnClickListener {
            if (jenis == "youtube" && !item.linkYoutube.isNullOrEmpty()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(item.linkYoutube))
                startActivity(intent)
            } else {
                val urlLengkap = "https://smkriyadhuljannahjalancagak.sch.id/uploads/materi/" + item.urlFile
                val viewerIntent = Intent(this, FileViewerActivity::class.java)
                viewerIntent.putExtra("FILE_URL", urlLengkap)
                viewerIntent.putExtra("TITLE", item.judul)
                startActivity(viewerIntent)
            }
        }
    }

    private fun setCardData(card: CardView, item: DisplayItem) {
        card.findViewById<TextView>(R.id.tvMapel).text = item.mapel
        card.findViewById<TextView>(R.id.tvJudulAkademik).text = item.judul

        val labelWaktu = if (jenisKonten == "TUGAS") "Tenggat" else "Diunggah"
        card.findViewById<TextView>(R.id.tvGuruInfo).text = "Oleh: ${item.guru}  |  $labelWaktu: ${item.waktu}"

        val btnAksi = card.findViewById<Button>(R.id.btnAksiAkademik)
        val btnPerbarui = card.findViewById<Button>(R.id.btnPerbaruiJawaban)
        val badge = card.findViewById<TextView>(R.id.tvBadge)
        val layNilaiKomentar = card.findViewById<LinearLayout>(R.id.layNilaiKomentar)
        val tvNilai = card.findViewById<TextView>(R.id.tvNilai)
        val tvKomentarGuru = card.findViewById<TextView>(R.id.tvKomentarGuru)

        if (jenisKonten == "TUGAS") {
            if (item.isSelesai) {
                badge.text = "SUDAH SELESAI"
                badge.setTextColor(Color.parseColor("#059669"))
                badge.setBackgroundColor(Color.parseColor("#D1FAE5"))
                
                layNilaiKomentar.visibility = View.VISIBLE
                tvNilai.text = if (item.nilai != null && item.nilai.isNotEmpty()) "Nilai: ${item.nilai}" else "Nilai: Belum Dinilai"
                tvKomentarGuru.text = if (item.komentarGuru != null && item.komentarGuru.isNotEmpty()) "Komentar: ${item.komentarGuru}" else "Komentar: Tidak ada catatan."

                btnAksi.text = "Lihat Jawaban"
                btnAksi.setBackgroundColor(Color.parseColor("#F3F4F6"))
                btnAksi.setTextColor(Color.parseColor("#111827"))
                
                btnPerbarui.visibility = View.VISIBLE
            } else {
                badge.text = "API NYA NGIRIM STATUS: '${item.rawStatus}'" // Buat debug
                badge.setTextColor(Color.parseColor("#DC2626"))
                badge.setBackgroundColor(Color.parseColor("#FEE2E2"))
                
                layNilaiKomentar.visibility = View.GONE
                btnPerbarui.visibility = View.GONE
                
                btnAksi.text = "Kumpulkan Tugas"
                btnAksi.setBackgroundColor(Color.parseColor("#1F2937"))
                btnAksi.setTextColor(Color.parseColor("#FFFFFF"))
            }
        } else {
            badge.visibility = View.GONE
            layNilaiKomentar.visibility = View.GONE
            btnPerbarui.visibility = View.GONE
            btnAksi.text = "Buka Materi"
            btnAksi.setBackgroundColor(Color.parseColor("#10B981"))
            btnAksi.setTextColor(Color.parseColor("#FFFFFF"))
        }

        btnAksi.setOnClickListener {
            if (jenisKonten == "MATERI") {
                val urlLengkap = "https://mariyadhuljannahsubang.sch.id/uploads/materi/" + item.urlFile
                val viewerIntent = Intent(this, FileViewerActivity::class.java)
                viewerIntent.putExtra("FILE_URL", urlLengkap)
                viewerIntent.putExtra("TITLE", item.judul)
                startActivity(viewerIntent)
            } else {
                if (item.isSelesai) {
                    if (item.fileJawaban != null && item.fileJawaban.isNotEmpty()) {
                        val viewerIntent = Intent(this, FileViewerActivity::class.java)
                        viewerIntent.putExtra("FILE_URL", item.fileJawaban)
                        viewerIntent.putExtra("TITLE", "File Jawaban")
                        startActivity(viewerIntent)
                    } else {
                        Toast.makeText(this, "URL File jawaban tidak tersedia dari server", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    val kumpulIntent = Intent(this, KumpulTugasActivity::class.java)
                    kumpulIntent.putExtra("ID_TUGAS", item.id)
                    kumpulIntent.putExtra("MAPEL", item.mapel)
                    kumpulIntent.putExtra("JUDUL", item.judul)
                    startActivity(kumpulIntent)
                }
            }
        }
        
        btnPerbarui.setOnClickListener {
            val kumpulIntent = Intent(this, KumpulTugasActivity::class.java)
            kumpulIntent.putExtra("ID_TUGAS", item.id)
            kumpulIntent.putExtra("MAPEL", item.mapel)
            kumpulIntent.putExtra("JUDUL", item.judul)
            startActivity(kumpulIntent)
        }
    }
}
