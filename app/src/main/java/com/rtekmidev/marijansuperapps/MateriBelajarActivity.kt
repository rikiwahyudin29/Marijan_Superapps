package com.rtekmidev.marijansuperapps

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.marijansuperapps.api.ApiClient
import com.rtekmidev.marijansuperapps.util.AvatarHelper
import com.rtekmidev.marijansuperapps.util.StatusBarHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MateriBelajarActivity : AppCompatActivity() {

    private var nisnSiswa = ""
    private var searchQuery = ""
    private var selectedMapel = "Semua"

    private var listDataAsli = listOf<DisplayItem>()

    private lateinit var rvMateri: RecyclerView
    private lateinit var materiAdapter: MateriAdapter
    private lateinit var layLoading: LinearLayout
    private lateinit var layEmpty: LinearLayout
    private lateinit var tvEmptySubtitle: TextView
    private lateinit var etSearch: EditText
    private lateinit var btnClearSearch: ImageView
    private lateinit var layMapelChips: LinearLayout

    data class DisplayItem(
        val id: String,
        val judul: String,
        val mapel: String,
        val guru: String,
        val waktu: String,
        val urlFile: String?,
        val deskripsi: String? = null,
        val jenisFile: String? = null,
        val linkYoutube: String? = null,
        val ukuranFile: String? = null
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_materi_belajar)

        // Setup Edge-to-Edge Status Bar Transparan & Insets
        StatusBarHelper.setupTranslucentBar(this, findViewById(R.id.main))

        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisnSiswa = sharedPref.getString("nisn", "") ?: ""
        val namaSiswa = sharedPref.getString("nama_siswa", "Siswa") ?: "Siswa"
        val fotoProfil = sharedPref.getString("foto_profil", "") ?: ""

        setupHeader(namaSiswa, fotoProfil)
        initViews()
        setupSearch()
        muatDataMateri()
    }

    private fun setupHeader(namaSiswa: String, fotoProfil: String) {
        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Materi Belajar"
        findViewById<TextView>(R.id.tvHeaderCategory)?.text = "E-LEARNING SISWA"
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }

        val ivProfilPhoto = findViewById<ImageView>(R.id.ivProfilPhoto)
        val tvProfilInisial = findViewById<TextView>(R.id.tvProfilInisial)
        val cvProfilPic = findViewById<CardView>(R.id.cvProfilPic)
        AvatarHelper.setAvatar(this, namaSiswa, fotoProfil, ivProfilPhoto, tvProfilInisial, cvProfilPic)
    }

    private fun initViews() {
        rvMateri = findViewById(R.id.rvMateri)
        layLoading = findViewById(R.id.layLoading)
        layEmpty = findViewById(R.id.layEmpty)
        tvEmptySubtitle = findViewById(R.id.tvEmptySubtitle)
        etSearch = findViewById(R.id.etSearch)
        btnClearSearch = findViewById(R.id.btnClearSearch)
        layMapelChips = findViewById(R.id.layMapelChips)

        rvMateri.layoutManager = LinearLayoutManager(this)
        materiAdapter = MateriAdapter(
            items = emptyList(),
            onVideoClick = { videoUrl ->
                bukaVideo(videoUrl)
            },
            onFileClick = { item ->
                bukaBerkas(item)
            }
        )
        rvMateri.adapter = materiAdapter
    }

    private fun setupSearch() {
        etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                searchQuery = s.toString().trim()
                btnClearSearch.visibility = if (searchQuery.isNotEmpty()) View.VISIBLE else View.GONE
                applyFilter()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearch.text.clear()
            searchQuery = ""
            btnClearSearch.visibility = View.GONE
            applyFilter()
        }
    }

    private fun muatDataMateri() {
        layLoading.visibility = View.VISIBLE
        layEmpty.visibility = View.GONE
        rvMateri.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.getMateri(nisnSiswa)
                withContext(Dispatchers.Main) {
                    layLoading.visibility = View.GONE
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
                        renderMapelChips()
                        applyFilter()
                    } else {
                        Toast.makeText(this@MateriBelajarActivity, "Gagal memuat materi", Toast.LENGTH_SHORT).show()
                        applyFilter()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    layLoading.visibility = View.GONE
                    Toast.makeText(this@MateriBelajarActivity, "Koneksi ke server gagal", Toast.LENGTH_SHORT).show()
                    applyFilter()
                }
            }
        }
    }

    private fun renderMapelChips() {
        layMapelChips.removeAllViews()

        val mapelSet = mutableListOf("Semua")
        val uniqueMapel = listDataAsli.map { it.mapel.trim() }.distinct().sorted()
        mapelSet.addAll(uniqueMapel)

        for (mapel in mapelSet) {
            val chip = TextView(this)
            chip.text = mapel
            chip.textSize = 11f
            chip.setPadding(32, 14, 32, 14)
            chip.isClickable = true
            chip.isFocusable = true

            val isSelected = (mapel == selectedMapel)
            updateChipStyle(chip, isSelected)

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            params.setMargins(0, 0, 16, 0)
            chip.layoutParams = params

            chip.setOnClickListener {
                selectedMapel = mapel
                for (i in 0 until layMapelChips.childCount) {
                    val child = layMapelChips.getChildAt(i) as? TextView
                    if (child != null) {
                        val active = (child.text == selectedMapel)
                        updateChipStyle(child, active)
                    }
                }
                applyFilter()
            }

            layMapelChips.addView(chip)
        }
    }

    private fun updateChipStyle(chip: TextView, isSelected: Boolean) {
        if (isSelected) {
            chip.setBackgroundResource(R.drawable.bg_button_dark_rounded)
            chip.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1A1B41"))
            chip.setTextColor(Color.WHITE)
            chip.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            chip.setBackgroundResource(R.drawable.bg_tag_mapel)
            chip.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
            chip.setTextColor(Color.parseColor("#64748B"))
            chip.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }

    private fun applyFilter() {
        var filtered = listDataAsli

        // Filter Mapel
        if (selectedMapel != "Semua") {
            filtered = filtered.filter { it.mapel.equals(selectedMapel, ignoreCase = true) }
        }

        // Filter Search Query
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.judul.contains(searchQuery, ignoreCase = true) ||
                it.mapel.contains(searchQuery, ignoreCase = true) ||
                it.guru.contains(searchQuery, ignoreCase = true) ||
                (it.deskripsi?.contains(searchQuery, ignoreCase = true) == true)
            }
        }

        materiAdapter.updateData(filtered)

        if (filtered.isEmpty()) {
            rvMateri.visibility = View.GONE
            layEmpty.visibility = View.VISIBLE
            if (searchQuery.isNotEmpty()) {
                tvEmptySubtitle.text = "Tidak ada materi yang sesuai dengan kata kunci \"$searchQuery\"."
            } else if (selectedMapel != "Semua") {
                tvEmptySubtitle.text = "Belum ada materi pelajaran untuk mata pelajaran $selectedMapel."
            } else {
                tvEmptySubtitle.text = "Belum ada materi pelajaran yang diunggah oleh guru."
            }
        } else {
            rvMateri.visibility = View.VISIBLE
            layEmpty.visibility = View.GONE
        }
    }

    private fun bukaVideo(videoUrl: String) {
        if (videoUrl.isBlank()) {
            Toast.makeText(this, "Tautan video tidak tersedia.", Toast.LENGTH_SHORT).show()
            return
        }
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(videoUrl))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak dapat membuka tautan video.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun bukaBerkas(item: DisplayItem) {
        val fileUrl = item.urlFile ?: ""
        if (fileUrl.isBlank()) {
            Toast.makeText(this, "Berkas materi tidak tersedia.", Toast.LENGTH_SHORT).show()
            return
        }

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
