@file:Suppress("DEPRECATION")
package com.rtekmidev.marijancbt

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.*

class MateriBelajarActivity : AppCompatActivity() {
    private var nisnSiswa = ""
    private var searchQuery = ""
    private var listDataAsli = listOf<DisplayItem>()

    data class DisplayItem(
        val id: String, val judul: String, val mapel: String,
        val guru: String, val waktu: String, val urlFile: String?,
        val deskripsi: String? = null,
        val jenisFile: String? = null,
        val linkYoutube: String? = null,
        val ukuranFile: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_materi_belajar)
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
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
        }

        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisnSiswa = sharedPref.getString("nisn", "") ?: ""
        val namaSiswa = sharedPref.getString("nama_siswa", "Siswa")
        val kelasLengkap = sharedPref.getString("kelas_lengkap", "Kelas -")
        val fotoProfil = sharedPref.getString("foto_profil", "")
        
        findViewById<TextView>(R.id.tvNamaDashboard)?.text = "Halo, $namaSiswa"
        findViewById<TextView>(R.id.tvKelas)?.text = kelasLengkap

        val ivProfilPhoto = findViewById<ImageView>(R.id.ivProfilPhoto)
        if (ivProfilPhoto != null && !fotoProfil.isNullOrEmpty()) {
            Glide.with(this)
                .load(fotoProfil)
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .error(android.R.drawable.ic_menu_myplaces)
                .circleCrop()
                .into(ivProfilPhoto)
        }

        val etSearch = findViewById<EditText>(R.id.etSearch)
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString()
                renderData()
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        muatDataMateri()
    }

    private fun muatDataMateri() {
        val wadah = findViewById<LinearLayout>(R.id.wadahDataMateri)
        wadah.removeAllViews()
        val loadingView = android.widget.ProgressBar(this)
        wadah.addView(loadingView)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.getMateri(nisnSiswa)
                withContext(Dispatchers.Main) {
                    wadah.removeView(loadingView)
                    if (resp.isSuccessful && resp.body()?.status == true) {
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
                                        val file = getStr("file_materi") ?: getStr("file") ?: getStr("file_pendukung") ?: getStr("dokumen") ?: getStr("file_pdf")
                                        val deskripsi = getStr("deskripsi")
                                        val jenisFile = getStr("jenis_file")
                                        val linkYoutube = getStr("link_youtube")
                                        val ukuranFile = getStr("ukuran_file") ?: getStr("ukuran")

                                        extractedList.add(
                                            DisplayItem(
                                                id, judul, mapel, guru, tanggal, file, deskripsi, jenisFile, linkYoutube, ukuranFile
                                            )
                                        )
                                    }
                                }
                            }
                            listDataAsli = extractedList
                        } else {
                            listDataAsli = emptyList()
                        }
                        renderData()
                    } else {
                        Toast.makeText(this@MateriBelajarActivity, "Gagal memuat materi", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    wadah.removeView(loadingView)
                    Toast.makeText(this@MateriBelajarActivity, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun renderData() {
        val wadah = findViewById<LinearLayout>(R.id.wadahDataMateri)
        wadah.removeAllViews()

        val listFinal = if (searchQuery.isNotEmpty()) {
            listDataAsli.filter { it.judul.contains(searchQuery, true) || it.mapel.contains(searchQuery, true) }
        } else listDataAsli

        if (listFinal.isEmpty()) {
            val tvKosong = TextView(this)
            tvKosong.text = "Materi tidak ditemukan."
            tvKosong.setTextColor(Color.parseColor("#6B7280"))
            tvKosong.setPadding(0, 40, 0, 0)
            wadah.addView(tvKosong)
        } else {
            val grouped = listFinal.groupBy { it.mapel }
            grouped.forEach { (mapel, items) ->
                val header = layoutInflater.inflate(R.layout.item_header_mapel, wadah, false)
                header.findViewById<TextView>(R.id.tvHeaderMapel).text = mapel
                wadah.addView(header)

                items.forEach { item ->
                    val card = layoutInflater.inflate(R.layout.item_materi_modern, wadah, false) as CardView
                    setCardDataMateriModern(card, item)
                    wadah.addView(card)
                }
            }
        }
    }

    private fun setCardDataMateriModern(card: CardView, item: DisplayItem) {
        card.findViewById<TextView>(R.id.tvJudulMateri).text = item.judul
        val tvDesc = card.findViewById<TextView>(R.id.tvDeskripsi)
        tvDesc.text = item.deskripsi ?: "Tanpa deskripsi"
        tvDesc.maxLines = 20
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
                val fileUrl = item.urlFile ?: ""
                val urlLengkap = if (fileUrl.startsWith("http")) {
                    fileUrl
                } else {
                    "https://smkriyadhuljannahjalancagak.sch.id/uploads/materi/" + fileUrl
                }

                val viewerIntent = Intent(this, FileViewerActivity::class.java)
                viewerIntent.putExtra("FILE_URL", urlLengkap)
                viewerIntent.putExtra("TITLE", item.judul)
                startActivity(viewerIntent)
            }
        }
    }
}
