package com.rtekmidev.smkrjsuperapps

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.HariJadwalData
import com.rtekmidev.smkrjsuperapps.api.JadwalItemModel
import com.rtekmidev.smkrjsuperapps.api.JadwalMengajarData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SuppressLint("SetTextI18n")
class JadwalMengajarActivity : AppCompatActivity() {

    // Toolbar
    private lateinit var btnBack: ImageView
    private lateinit var ivProfilPhoto: ImageView

    // Banner Guru
    private lateinit var ivAvatarGuru: ImageView
    private lateinit var tvInisialGuru: TextView
    private lateinit var tvNamaGuru: TextView
    private lateinit var tvJabatanGuru: TextView
    private lateinit var tvNipGuru: TextView
    private lateinit var tvSemesterBadge: TextView
    private lateinit var tvNamaSekolahBadge: TextView

    // Stats
    private lateinit var tvTotalJamStat: TextView
    private lateinit var tvTotalSiswaStat: TextView
    private lateinit var tvTotalRombelBadge: TextView
    private lateinit var tvKelasStat: TextView
    private lateinit var tvTotalMapelStat: TextView

    // Tab Switcher
    private lateinit var btnTabPerHari: TextView
    private lateinit var btnTabMatriks: TextView
    private lateinit var layoutTampilanPerHari: View
    private lateinit var layoutMatriksMingguan: View

    // Tampilan Per Hari
    private lateinit var tvSelectedHariBadge: TextView
    private lateinit var containerSelectorHari: LinearLayout
    private lateinit var tvTitleRincianHari: TextView
    private lateinit var tvBadgeStatusHari: TextView
    private lateinit var pbLoadingJadwal: View
    private lateinit var containerJadwalHari: LinearLayout
    private lateinit var cvEmptyHari: View
    private lateinit var tvEmptyHariTitle: TextView
    private lateinit var tvEmptyHariSub: TextView

    // Matriks
    private lateinit var tvFilterKelasText: TextView
    private lateinit var btnFilterKelasMatriks: View
    private lateinit var btnIconFilterMatriks: View
    private lateinit var containerLegendaMatriks: LinearLayout
    private lateinit var containerMatriksTableRows: LinearLayout
    private lateinit var tvTotalJamTatapMukaFooter: TextView

    private var jadwalData: JadwalMengajarData? = null
    private var selectedHariIndex: Int = 0
    private var selectedFilterKelas: String = "Semua Kelas"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEnterTransition()

        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode

        setContentView(R.layout.activity_jadwal_mengajar)

        val rootLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
        switchTab(perHari = true)
        loadProfilPicHeader()
        fetchJadwalMengajar()
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        ivProfilPhoto = findViewById(R.id.ivProfilPhoto)

        ivAvatarGuru = findViewById(R.id.ivAvatarGuru)
        tvInisialGuru = findViewById(R.id.tvInisialGuru)
        tvNamaGuru = findViewById(R.id.tvNamaGuru)
        tvJabatanGuru = findViewById(R.id.tvJabatanGuru)
        tvNipGuru = findViewById(R.id.tvNipGuru)
        tvSemesterBadge = findViewById(R.id.tvSemesterBadge)
        tvNamaSekolahBadge = findViewById(R.id.tvNamaSekolahBadge)

        tvTotalJamStat = findViewById(R.id.tvTotalJamStat)
        tvTotalSiswaStat = findViewById(R.id.tvTotalSiswaStat)
        tvTotalRombelBadge = findViewById(R.id.tvTotalRombelBadge)
        tvKelasStat = findViewById(R.id.tvKelasStat)
        tvTotalMapelStat = findViewById(R.id.tvTotalMapelStat)

        btnTabPerHari = findViewById(R.id.btnTabPerHari)
        btnTabMatriks = findViewById(R.id.btnTabMatriks)
        layoutTampilanPerHari = findViewById(R.id.layoutTampilanPerHari)
        layoutMatriksMingguan = findViewById(R.id.layoutMatriksMingguan)

        tvSelectedHariBadge = findViewById(R.id.tvSelectedHariBadge)
        containerSelectorHari = findViewById(R.id.containerSelectorHari)
        tvTitleRincianHari = findViewById(R.id.tvTitleRincianHari)
        tvBadgeStatusHari = findViewById(R.id.tvBadgeStatusHari)
        pbLoadingJadwal = findViewById(R.id.pbLoadingJadwal)
        containerJadwalHari = findViewById(R.id.containerJadwalHari)
        cvEmptyHari = findViewById(R.id.cvEmptyHari)
        tvEmptyHariTitle = findViewById(R.id.tvEmptyHariTitle)
        tvEmptyHariSub = findViewById(R.id.tvEmptyHariSub)

        tvFilterKelasText = findViewById(R.id.tvFilterKelasText)
        btnFilterKelasMatriks = findViewById(R.id.btnFilterKelasMatriks)
        btnIconFilterMatriks = findViewById(R.id.btnIconFilterMatriks)
        containerLegendaMatriks = findViewById(R.id.containerLegendaMatriks)
        containerMatriksTableRows = findViewById(R.id.containerMatriksTableRows)
        tvTotalJamTatapMukaFooter = findViewById(R.id.tvTotalJamTatapMukaFooter)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
            applyExitTransition()
        }

        btnTabPerHari.setOnClickListener {
            switchTab(perHari = true)
        }

        btnTabMatriks.setOnClickListener {
            switchTab(perHari = false)
        }

        btnFilterKelasMatriks.setOnClickListener {
            jadwalData?.let { showFilterKelasDialog(it) }
        }

        btnIconFilterMatriks.setOnClickListener {
            jadwalData?.let { showFilterKelasDialog(it) }
        }
    }

    private fun getActiveTabDrawable(): android.graphics.drawable.GradientDrawable {
        return android.graphics.drawable.GradientDrawable().apply {
            cornerRadius = 20 * resources.displayMetrics.density
            setColor(Color.parseColor("#1E1B4B"))
        }
    }

    private fun switchTab(perHari: Boolean) {
        if (perHari) {
            btnTabPerHari.background = getActiveTabDrawable()
            btnTabPerHari.setTextColor(Color.WHITE)
            btnTabPerHari.setTypeface(null, android.graphics.Typeface.BOLD)

            btnTabMatriks.background = null
            btnTabMatriks.setTextColor(Color.parseColor("#64748B"))
            btnTabMatriks.setTypeface(null, android.graphics.Typeface.NORMAL)

            layoutTampilanPerHari.visibility = View.VISIBLE
            layoutMatriksMingguan.visibility = View.GONE
        } else {
            btnTabMatriks.background = getActiveTabDrawable()
            btnTabMatriks.setTextColor(Color.WHITE)
            btnTabMatriks.setTypeface(null, android.graphics.Typeface.BOLD)

            btnTabPerHari.background = null
            btnTabPerHari.setTextColor(Color.parseColor("#64748B"))
            btnTabPerHari.setTypeface(null, android.graphics.Typeface.NORMAL)

            layoutTampilanPerHari.visibility = View.GONE
            layoutMatriksMingguan.visibility = View.VISIBLE
        }
    }

    private fun loadProfilPicHeader() {
        val prefGuru = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val fotoProfilUrl = prefGuru.getString("foto_profil", null)
        val namaLengkap = prefGuru.getString("nama_lengkap", "Guru")

        if (!fotoProfilUrl.isNullOrEmpty()) {
            val fullUrl = if (fotoProfilUrl.startsWith("http")) fotoProfilUrl else "https://smkriyadhuljannahjalancagak.sch.id/uploads/guru/$fotoProfilUrl"
            try {
                Glide.with(this)
                    .load(fullUrl)
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .circleCrop()
                    .into(ivProfilPhoto)
            } catch (_: Exception) {}
        }
    }

    private fun fetchJadwalMengajar() {
        pbLoadingJadwal.visibility = View.VISIBLE

        val prefGuru = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val idUser = prefGuru.getString("id_user", "") ?: ""

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getJadwalMengajar(idUser)
                withContext(Dispatchers.Main) {
                    pbLoadingJadwal.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.status == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            jadwalData = data
                            renderDashboard(data)
                        }
                    } else {
                        Toast.makeText(this@JadwalMengajarActivity, "Gagal memuat jadwal mengajar", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbLoadingJadwal.visibility = View.GONE
                    Toast.makeText(this@JadwalMengajarActivity, "Koneksi bermasalah: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun renderDashboard(data: JadwalMengajarData) {
        // 1. Profil Guru
        val guru = data.guru
        if (guru != null) {
            tvNamaGuru.text = guru.nama_lengkap ?: "Guru"

            val roleDariApi = guru.jabatan?.takeIf { it.isNotBlank() }
                ?: guru.role?.takeIf { it.isNotBlank() }
            val cleanJabatan = if (!roleDariApi.isNullOrBlank()) {
                roleDariApi.replace("tkjt", "", ignoreCase = true)
                    .replace("tjkt", "", ignoreCase = true)
                    .trim()
            } else {
                val prefGuru = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
                val roleUser = prefGuru.getString("role", "guru") ?: "guru"
                val roleMap = mapOf(
                    "guru" to "Guru Pengajar",
                    "kepsek" to "Kepala Sekolah",
                    "admin" to "Administrator",
                    "superadmin" to "Super Administrator",
                    "kurikulum" to "Wakasek Kurikulum",
                    "kesiswaan" to "Wakasek Kesiswaan",
                    "humas" to "Wakasek Humas",
                    "sarpras" to "Wakasek Sarpras",
                    "staff" to "Staf Tata Usaha"
                )
                roleMap[roleUser.lowercase()] ?: roleUser.replaceFirstChar { it.uppercase() }
            }
            tvJabatanGuru.text = if (cleanJabatan.isNotBlank()) cleanJabatan else "Guru Pengajar"

            val nikVal = guru.nik?.takeIf { it.isNotBlank() } ?: guru.nip?.takeIf { it.isNotBlank() } ?: "-"
            tvNipGuru.text = "NIK: $nikVal"
            tvSemesterBadge.text = guru.semester_info ?: "Semester Ganjil"
            tvNamaSekolahBadge.text = guru.nama_sekolah ?: "SMKS Riyadhul Jannah"

            // Avatar / inisial
            val nama = guru.nama_lengkap ?: "G"
            val inisial = nama.split(" ")
                .filter { it.isNotEmpty() }
                .take(2)
                .map { it[0] }
                .joinToString("")
                .uppercase()
            tvInisialGuru.text = if (inisial.isNotEmpty()) inisial else "RW"

            if (!guru.foto.isNullOrEmpty()) {
                ivAvatarGuru.visibility = View.VISIBLE
                tvInisialGuru.visibility = View.GONE
                try {
                    Glide.with(this)
                        .load(guru.foto)
                        .circleCrop()
                        .into(ivAvatarGuru)
                } catch (_: Exception) {}
            }
        }

        // 2. Stats
        val stats = data.stats
        if (stats != null) {
            tvTotalJamStat.text = (stats.total_jam_minggu ?: 14).toString()
            tvTotalSiswaStat.text = (stats.total_siswa ?: 0).toString()
            tvTotalRombelBadge.text = "${stats.total_rombel ?: 0} Rombel"
            tvKelasStat.text = stats.kelas_ringkasan ?: "-"
            tvTotalMapelStat.text = "${stats.total_mapel ?: 0} Mata Pelajaran"
        }

        // 3. Tentukan hari default yang dipilih
        val hariList = data.hari_list ?: emptyList()
        if (hariList.isNotEmpty()) {
            // Prioritas: hari ini (jika bukan libur) -> jika hari ini libur, cari hari terdekat yang ada jamnya
            val todayIdx = hariList.indexOfFirst { it.is_hari_ini == true && (it.total_jam ?: 0) > 0 }
            selectedHariIndex = if (todayIdx != -1) {
                todayIdx
            } else {
                val firstActiveIdx = hariList.indexOfFirst { (it.total_jam ?: 0) > 0 }
                if (firstActiveIdx != -1) firstActiveIdx else 0
            }

            renderSelectorHari(hariList)
            renderRincianHari(hariList[selectedHariIndex])
            renderMatriksMingguan(data)
        }
    }

    private fun renderSelectorHari(hariList: List<HariJadwalData>) {
        containerSelectorHari.removeAllViews()
        val inflater = LayoutInflater.from(this)

        hariList.forEachIndexed { index, hari ->
            val chipView = inflater.inflate(R.layout.item_hari_kbm_chip, containerSelectorHari, false)

            val tvSingkatan = chipView.findViewById<TextView>(R.id.tvSingkatanHari)
            val tvNama = chipView.findViewById<TextView>(R.id.tvNamaHari)
            val tvBadgeJam = chipView.findViewById<TextView>(R.id.tvBadgeJamHari)
            val cvChip = chipView.findViewById<CardView>(R.id.cvHariChip)
            val llContent = chipView.findViewById<LinearLayout>(R.id.llHariChipContent)

            tvSingkatan.text = hari.singkatan ?: ""
            tvNama.text = hari.nama_hari ?: ""

            val totalJam = hari.total_jam ?: 0
            if (hari.is_libur == true || totalJam == 0) {
                tvBadgeJam.text = "Libur"
            } else {
                tvBadgeJam.text = "$totalJam Jam"
            }

            val isSelected = index == selectedHariIndex
            if (isSelected) {
                cvChip.setCardBackgroundColor(Color.parseColor("#1E1B4B"))
                llContent.setBackgroundColor(Color.parseColor("#1E1B4B"))
                tvSingkatan.setTextColor(Color.parseColor("#94A3B8"))
                tvNama.setTextColor(Color.WHITE)
                tvBadgeJam.setBackgroundColor(Color.parseColor("#312E81"))
                tvBadgeJam.setTextColor(Color.parseColor("#C7D2FE"))
            } else {
                cvChip.setCardBackgroundColor(Color.WHITE)
                llContent.setBackgroundResource(R.drawable.bg_rounded_border_outline)
                tvSingkatan.setTextColor(Color.parseColor("#94A3B8"))
                tvNama.setTextColor(Color.parseColor("#1E293B"))
                if (hari.is_libur == true || totalJam == 0) {
                    tvBadgeJam.setBackgroundColor(Color.parseColor("#F1F5F9"))
                    tvBadgeJam.setTextColor(Color.parseColor("#64748B"))
                } else {
                    tvBadgeJam.setBackgroundColor(Color.parseColor("#EDE9FE"))
                    tvBadgeJam.setTextColor(Color.parseColor("#6D28D9"))
                }
            }

            chipView.setOnClickListener {
                if (selectedHariIndex != index) {
                    selectedHariIndex = index
                    renderSelectorHari(hariList)
                    renderRincianHari(hariList[index])
                }
            }

            containerSelectorHari.addView(chipView)
        }
    }

    private fun renderRincianHari(hari: HariJadwalData) {
        val namaHari = hari.nama_hari ?: "Hari"
        val totalJam = hari.total_jam ?: 0

        tvTitleRincianHari.text = "RINCIAN JAM MENGAJAR (${namaHari.uppercase()})"
        tvSelectedHariBadge.text = if (hari.is_libur == true || totalJam == 0) {
            "$namaHari (Libur)"
        } else {
            "$namaHari terpilih ($totalJam Jam)"
        }

        if (hari.is_hari_ini == true) {
            tvBadgeStatusHari.visibility = View.VISIBLE
            tvBadgeStatusHari.text = "● Hari Ini"
        } else {
            tvBadgeStatusHari.visibility = View.GONE
        }

        containerJadwalHari.removeAllViews()

        val items = hari.items ?: emptyList()
        if (items.isEmpty() || hari.is_libur == true) {
            cvEmptyHari.visibility = View.VISIBLE
            tvEmptyHariTitle.text = if (hari.is_libur == true) "Hari Libur ($namaHari)" else "Tidak Ada Jadwal Mengajar"
            tvEmptyHariSub.text = "Tidak ada agenda kegiatan belajar mengajar pada hari $namaHari."
        } else {
            cvEmptyHari.visibility = View.GONE
            val inflater = LayoutInflater.from(this)

            for (item in items) {
                if (item.type == "istirahat") {
                    // Card Istirahat
                    val viewIstirahat = inflater.inflate(R.layout.item_jadwal_istirahat_card, containerJadwalHari, false)
                    viewIstirahat.findViewById<TextView>(R.id.tvNamaIstirahat).text = item.nama_mapel ?: "Jam Istirahat"

                    var jmMulai = item.jam_mulai ?: "11:20"
                    var jmSelesai = item.jam_selesai ?: "12:45"
                    if (jmSelesai.startsWith("00:") && (jmMulai.take(2).toIntOrNull() ?: 0) >= 10) {
                        jmSelesai = "12:" + jmSelesai.substring(3)
                    }

                    var durasiStr = item.ruang ?: ""
                    if (durasiStr.isBlank() || (durasiStr.contains("15") && jmMulai.startsWith("11") && jmSelesai.startsWith("12"))) {
                        durasiStr = "85 Menit"
                    } else if (!durasiStr.endsWith("Menit", ignoreCase = true)) {
                        durasiStr = "$durasiStr Menit"
                    }

                    viewIstirahat.findViewById<TextView>(R.id.tvWaktuIstirahat).text = "$jmMulai - $jmSelesai WIB ($durasiStr)"
                    viewIstirahat.findViewById<TextView>(R.id.tvBadgeIstirahat).text = item.kategori ?: "Jeda Istirahat"
                    containerJadwalHari.addView(viewIstirahat)
                } else {
                    // Card Pelajaran (View Only)
                    val viewPelajaran = inflater.inflate(R.layout.item_jadwal_pelajaran_card, containerJadwalHari, false)

                    viewPelajaran.findViewById<TextView>(R.id.tvJamKeBadge).text = item.jam_ke ?: "Jam Ke"
                    viewPelajaran.findViewById<TextView>(R.id.tvWaktuPelajaran).text = "🕒 ${item.jam_mulai} - ${item.jam_selesai} WIB"
                    viewPelajaran.findViewById<TextView>(R.id.tvKelasPelajaranBadge).text = item.nama_kelas ?: "-"
                    viewPelajaran.findViewById<TextView>(R.id.tvNamaMapelPelajaran).text = item.nama_mapel ?: "-"

                    val namaKelas = item.nama_kelas?.trim().orEmpty()
                    val ruangNama = when {
                        item.ruang?.startsWith("Ruang", ignoreCase = true) == true && !item.ruang.contains("Lab Komputer", ignoreCase = true) -> item.ruang
                        namaKelas.isNotEmpty() -> "Ruang $namaKelas"
                        else -> "Ruang Kelas"
                    }
                    viewPelajaran.findViewById<TextView>(R.id.tvRuangKategori).text = "🏢 $ruangNama • Kategori: ${item.kategori ?: "Teori & Praktik"}"

                    containerJadwalHari.addView(viewPelajaran)
                }
            }
        }
    }

    private fun showFilterKelasDialog(data: JadwalMengajarData) {
        val kelasSet = linkedSetOf<String>()
        data.matriks_rows?.forEach { row ->
            row.cells?.values?.forEach { cell ->
                val kls = cell.nama_kelas?.trim()
                if (!kls.isNullOrBlank()) {
                    kelasSet.add(kls)
                }
            }
        }
        data.hari_list?.forEach { hari ->
            hari.items?.forEach { item ->
                val kls = item.nama_kelas?.trim()
                if (!kls.isNullOrBlank()) {
                    kelasSet.add(kls)
                }
            }
        }

        val options = mutableListOf("Semua Kelas")
        options.addAll(kelasSet.sorted())

        val checkedItem = options.indexOf(selectedFilterKelas).takeIf { it != -1 } ?: 0

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Filter Jadwal Berdasarkan Kelas")
            .setSingleChoiceItems(options.toTypedArray(), checkedItem) { dialog, which ->
                val chosen = options[which]
                selectedFilterKelas = chosen
                renderMatriksMingguan(data)
                dialog.dismiss()
                Toast.makeText(this, "Menampilkan filter: $chosen", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Tutup") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun renderMatriksMingguan(data: JadwalMengajarData) {
        containerLegendaMatriks.removeAllViews()
        containerMatriksTableRows.removeAllViews()

        val density = resources.displayMetrics.density

        // 1. Filter Dropdown
        val stats = data.stats
        if (selectedFilterKelas == "Semua Kelas") {
            tvFilterKelasText.text = "Semua Kelas (${stats?.kelas_ringkasan ?: "-"})"
        } else {
            tvFilterKelasText.text = "Kelas: $selectedFilterKelas"
        }

        // 2. Legenda Mata Pelajaran & Jadwal
        val legendaList = data.legenda_list ?: emptyList()
        for (item in legendaList) {
            if (selectedFilterKelas != "Semua Kelas" && item.type == "mapel") {
                val match = item.title?.contains(selectedFilterKelas, ignoreCase = true) == true
                if (!match) continue
            }

            val legendCard = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    bottomMargin = (8 * density).toInt()
                }
                setPadding((12 * density).toInt(), (10 * density).toInt(), (12 * density).toInt(), (10 * density).toInt())

                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 12 * density
                    setColor(Color.parseColor(item.bg_hex ?: "#F8FAFC"))
                    setStroke((1 * density).toInt(), Color.parseColor(item.border_hex ?: "#E2E8F0"))
                }
            }

            // Dot solid
            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams((12 * density).toInt(), (12 * density).toInt()).apply {
                    marginEnd = (10 * density).toInt()
                }
                background = android.graphics.drawable.GradientDrawable().apply {
                    shape = android.graphics.drawable.GradientDrawable.OVAL
                    setColor(Color.parseColor(item.color_hex ?: "#8B5CF6"))
                }
            }

            // Title
            val tvTitle = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                text = item.title ?: ""
                textSize = 11f
                setTextColor(Color.parseColor("#1E293B"))
                setTypeface(null, android.graphics.Typeface.BOLD)
            }

            // Badge
            val tvBadge = TextView(this).apply {
                text = item.badge ?: ""
                textSize = 10f
                setTextColor(Color.parseColor(item.text_hex ?: "#6D28D9"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setPadding((8 * density).toInt(), (4 * density).toInt(), (8 * density).toInt(), (4 * density).toInt())
                background = android.graphics.drawable.GradientDrawable().apply {
                    cornerRadius = 8 * density
                    setColor(Color.WHITE)
                    setStroke((1 * density).toInt(), Color.parseColor(item.border_hex ?: "#E2E8F0"))
                }
            }

            legendCard.addView(dot)
            legendCard.addView(tvTitle)
            legendCard.addView(tvBadge)
            containerLegendaMatriks.addView(legendCard)
        }

        // 3. Matriks Rows Grid (Senin s/d Sabtu)
        val days = listOf("Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu")
        val matriksRows = data.matriks_rows ?: emptyList()
        var totalJpFiltered = 0

        for (row in matriksRows) {
            val isIstirahat = row.is_istirahat == true
            val rowHeight = if (isIstirahat) (36 * density).toInt() else (48 * density).toInt()

            val rowView = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = android.view.Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    rowHeight
                )
                setBackgroundColor(if (isIstirahat) Color.parseColor("#FEF3C7") else Color.WHITE)
            }

            // Col 1: Jam (45dp)
            val tvJam = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams((45 * density).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
                gravity = android.view.Gravity.CENTER
                text = (row.urutan ?: "").toString()
                textSize = 11f
                setTextColor(if (isIstirahat) Color.parseColor("#92400E") else Color.parseColor("#1E293B"))
                setTypeface(null, android.graphics.Typeface.BOLD)
                setBackgroundColor(if (isIstirahat) Color.parseColor("#FEF3C7") else Color.WHITE)
            }
            rowView.addView(tvJam)

            // Col 2: Waktu (85dp)
            val tvWaktu = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams((85 * density).toInt(), LinearLayout.LayoutParams.MATCH_PARENT)
                gravity = android.view.Gravity.CENTER
                var waktuClean = row.waktu ?: ""
                if (waktuClean.contains("00:")) {
                    waktuClean = waktuClean.replace("00:", "12:")
                }
                text = waktuClean
                textSize = 9.5f
                setTextColor(if (isIstirahat) Color.parseColor("#B45309") else Color.parseColor("#64748B"))
                setBackgroundColor(if (isIstirahat) Color.parseColor("#FEF3C7") else Color.WHITE)
            }
            rowView.addView(tvWaktu)

            // Col 3..8: Days (Senin..Sabtu, 115dp each)
            for (hari in days) {
                val cell = row.cells?.get(hari)
                val cellContainer = FrameLayout(this).apply {
                    layoutParams = LinearLayout.LayoutParams((115 * density).toInt(), rowHeight)
                    setBackgroundColor(if (isIstirahat) Color.parseColor("#FEF3C7") else Color.WHITE)
                }

                val isMatchKelas = if (selectedFilterKelas == "Semua Kelas") {
                    true
                } else {
                    cell?.nama_kelas?.trim().equals(selectedFilterKelas.trim(), ignoreCase = true)
                }

                if (isIstirahat) {
                    val tvIst = TextView(this).apply {
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                        gravity = android.view.Gravity.CENTER
                        text = "🍽️ ISTIRAHAT"
                        textSize = 9f
                        setTextColor(Color.parseColor("#92400E"))
                        setTypeface(null, android.graphics.Typeface.BOLD)
                    }
                    cellContainer.addView(tvIst)
                } else if (cell != null && cell.type == "pelajaran" && isMatchKelas) {
                    val pos = cell.position ?: if (cell.is_start == true) "start" else "end"
                    if (cell.is_start == true || pos == "start" || pos == "single") {
                        totalJpFiltered += (cell.jp ?: 1)
                    }

                    val cardPel = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        val r = 10f * density
                        background = android.graphics.drawable.GradientDrawable().apply {
                            when (pos) {
                                "start" -> cornerRadii = floatArrayOf(r, r, r, r, 0f, 0f, 0f, 0f)
                                "middle" -> cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f)
                                "end" -> cornerRadii = floatArrayOf(0f, 0f, 0f, 0f, r, r, r, r)
                                else -> cornerRadius = r // "single"
                            }
                            setColor(Color.parseColor(cell.bg_hex ?: "#F5F3FF"))
                            setStroke((1 * density).toInt(), Color.parseColor(cell.border_hex ?: "#DDD6FE"))
                        }
                    }

                    when (pos) {
                        "start" -> {
                            cardPel.layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            ).apply {
                                setMargins((3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt(), 0)
                            }
                            cardPel.setPadding((6 * density).toInt(), (5 * density).toInt(), (6 * density).toInt(), (2 * density).toInt())

                            val tvMapel = TextView(this).apply {
                                text = cell.nama_mapel ?: ""
                                textSize = 9.5f
                                setTextColor(Color.parseColor(cell.text_hex ?: "#6D28D9"))
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                maxLines = 2
                                ellipsize = android.text.TextUtils.TruncateAt.END
                            }
                            cardPel.addView(tvMapel)
                        }
                        "middle" -> {
                            cardPel.layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            ).apply {
                                setMargins((3 * density).toInt(), 0, (3 * density).toInt(), 0)
                            }
                            // Kosong
                        }
                        "end" -> {
                            cardPel.layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            ).apply {
                                setMargins((3 * density).toInt(), 0, (3 * density).toInt(), (3 * density).toInt())
                            }
                            cardPel.gravity = android.view.Gravity.BOTTOM
                            cardPel.setPadding((6 * density).toInt(), (2 * density).toInt(), (6 * density).toInt(), (5 * density).toInt())

                            val bottomRow = LinearLayout(this).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = android.view.Gravity.CENTER_VERTICAL
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                )
                            }

                            val tvKelas = TextView(this).apply {
                                text = cell.nama_kelas ?: ""
                                textSize = 7.5f
                                setTextColor(Color.parseColor(cell.text_hex ?: "#6D28D9"))
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                setPadding((4 * density).toInt(), (1 * density).toInt(), (4 * density).toInt(), (1 * density).toInt())
                                background = android.graphics.drawable.GradientDrawable().apply {
                                    cornerRadius = 3 * density
                                    setColor(Color.WHITE)
                                }
                            }

                            val spacer = View(this).apply {
                                layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                            }

                            val tvJp = TextView(this).apply {
                                text = "${cell.jp ?: 2} JP"
                                textSize = 8.5f
                                setTextColor(Color.parseColor(cell.text_hex ?: "#6D28D9"))
                                setTypeface(null, android.graphics.Typeface.BOLD)
                            }

                            bottomRow.addView(tvKelas)
                            bottomRow.addView(spacer)
                            bottomRow.addView(tvJp)
                            cardPel.addView(bottomRow)
                        }
                        else -> { // single
                            cardPel.layoutParams = FrameLayout.LayoutParams(
                                FrameLayout.LayoutParams.MATCH_PARENT,
                                FrameLayout.LayoutParams.MATCH_PARENT
                            ).apply {
                                setMargins((3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt(), (3 * density).toInt())
                            }
                            cardPel.setPadding((6 * density).toInt(), (4 * density).toInt(), (6 * density).toInt(), (4 * density).toInt())

                            val tvMapel = TextView(this).apply {
                                text = cell.nama_mapel ?: ""
                                textSize = 9f
                                setTextColor(Color.parseColor(cell.text_hex ?: "#6D28D9"))
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                maxLines = 1
                                ellipsize = android.text.TextUtils.TruncateAt.END
                            }

                            val bottomRow = LinearLayout(this).apply {
                                orientation = LinearLayout.HORIZONTAL
                                gravity = android.view.Gravity.CENTER_VERTICAL
                                layoutParams = LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                                ).apply {
                                    topMargin = (2 * density).toInt()
                                }
                            }

                            val tvKelas = TextView(this).apply {
                                text = cell.nama_kelas ?: ""
                                textSize = 8f
                                setTextColor(Color.parseColor(cell.text_hex ?: "#6D28D9"))
                                setTypeface(null, android.graphics.Typeface.BOLD)
                                setPadding((4 * density).toInt(), (1 * density).toInt(), (4 * density).toInt(), (1 * density).toInt())
                                background = android.graphics.drawable.GradientDrawable().apply {
                                    cornerRadius = 4 * density
                                    setColor(Color.WHITE)
                                }
                            }

                            val spacer = View(this).apply {
                                layoutParams = LinearLayout.LayoutParams(0, 0, 1f)
                            }

                            val tvJp = TextView(this).apply {
                                text = "${cell.jp ?: 1} JP"
                                textSize = 8f
                                setTextColor(Color.parseColor(cell.text_hex ?: "#6D28D9"))
                                setTypeface(null, android.graphics.Typeface.BOLD)
                            }

                            bottomRow.addView(tvKelas)
                            bottomRow.addView(spacer)
                            bottomRow.addView(tvJp)
                            cardPel.addView(tvMapel)
                            cardPel.addView(bottomRow)
                        }
                    }

                    cellContainer.addView(cardPel)
                } else {
                    val tvEmpty = TextView(this).apply {
                        layoutParams = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
                        gravity = android.view.Gravity.CENTER
                        text = "-"
                        textSize = 12f
                        setTextColor(Color.parseColor("#CBD5E1"))
                    }
                    cellContainer.addView(tvEmpty)
                }

                val rightDivider = View(this).apply {
                    layoutParams = FrameLayout.LayoutParams((1 * density).toInt(), FrameLayout.LayoutParams.MATCH_PARENT).apply {
                        gravity = android.view.Gravity.END
                    }
                    setBackgroundColor(if (isIstirahat) Color.parseColor("#FDE68A") else Color.parseColor("#F1F5F9"))
                }
                cellContainer.addView(rightDivider)

                rowView.addView(cellContainer)
            }

            val bottomBorder = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    (1 * density).toInt()
                )
                setBackgroundColor(if (isIstirahat) Color.parseColor("#FDE68A") else Color.parseColor("#F1F5F9"))
            }

            val rowWrapper = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
            }
            rowWrapper.addView(rowView)
            rowWrapper.addView(bottomBorder)

            containerMatriksTableRows.addView(rowWrapper)
        }

        // 4. Update Footer Total Jam Tatap Muka
        val finalTotalJp = if (selectedFilterKelas == "Semua Kelas") {
            stats?.total_jam_minggu ?: 0
        } else {
            totalJpFiltered
        }
        tvTotalJamTatapMukaFooter.text = if (selectedFilterKelas == "Semua Kelas") {
            "ℹ️ Total Jam Tatap Muka: $finalTotalJp Jam Pelajaran (JP)"
        } else {
            "ℹ️ Total Jam Tatap Muka ($selectedFilterKelas): $finalTotalJp Jam Pelajaran (JP)"
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        applyExitTransition()
    }
}
