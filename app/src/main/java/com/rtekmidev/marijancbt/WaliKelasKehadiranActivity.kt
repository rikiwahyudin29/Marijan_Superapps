package com.rtekmidev.marijancbt

import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rtekmidev.marijancbt.api.ApiClient
import com.rtekmidev.marijancbt.api.RekapKehadiranDateItem
import com.rtekmidev.marijancbt.api.RekapKehadiranSiswaItem
import com.rtekmidev.marijancbt.api.WaliKelasRekapKehadiranResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@SuppressLint("SetTextI18n")
class WaliKelasKehadiranActivity : AppCompatActivity() {

    private lateinit var ivProfilPhoto: ImageView
    private lateinit var tvKelasBadge: TextView
    private lateinit var tvTahunAjaranBadge: TextView
    private lateinit var tvWaliKelasNama: TextView

    private lateinit var btnPrevMonth: ImageView
    private lateinit var tvSelectedMonth: TextView
    private lateinit var btnNextMonth: ImageView

    private lateinit var tvMetricHadirPersen: TextView
    private lateinit var tvMetricHadirTotal: TextView
    private lateinit var tvMetricSakit: TextView
    private lateinit var tvMetricIzin: TextView
    private lateinit var tvMetricAlpha: TextView

    private lateinit var btnCetakMatrix: View
    private lateinit var btnExportExcel: View

    // Tab Switcher
    private lateinit var btnTabDaftarSiswa: TextView
    private lateinit var btnTabMatriks: TextView
    private lateinit var layoutTabDaftarSiswa: LinearLayout
    private lateinit var layoutTabMatriks: LinearLayout

    private lateinit var cvInfoLibur: View
    private lateinit var tvInfoLiburHeader: TextView
    private lateinit var tvTotalLiburBadge: TextView
    private lateinit var containerInfoLiburItems: LinearLayout

    private lateinit var cvMatriksPresensi: View
    private lateinit var containerMatriksFixed: TableLayout
    private lateinit var containerMatriksScrollable: TableLayout

    private val monthlyDataCache = HashMap<String, WaliKelasRekapKehadiranResponse>()

    private lateinit var etCariSiswa: EditText
    private lateinit var tvDaftarSiswaHeader: TextView
    private lateinit var btnUrutkanSiswa: TextView

    private lateinit var chipSemua: TextView
    private lateinit var chipAlpha: TextView
    private lateinit var chipHadir100: TextView
    private lateinit var chipSakitIzin: TextView

    private lateinit var pbLoading: View
    private lateinit var tvEmptyState: TextView
    private lateinit var rvDaftarSiswa: RecyclerView

    private var adapter: RekapSiswaBinaanAdapter? = null
    private var allSiswaList: List<RekapKehadiranSiswaItem> = emptyList()
    private var currentCalendar: Calendar = Calendar.getInstance()
    private var currentFilterType: String = "Semua"
    private var isSortByNomorAbsen: Boolean = true
    private var loadJob: Job? = null

    private var isTabDaftarActive: Boolean = true
    private var isMatrixRendered: Boolean = false
    private var cachedDates: List<RekapKehadiranDateItem> = emptyList()
    private var cachedSiswaList: List<RekapKehadiranSiswaItem> = emptyList()

    private var cetakMatrixUrl: String? = null
    private var exportExcelUrl: String? = null
    private var cetakSiswaBaseUrl: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEnterTransition()

        // Edge to Edge Transparent Status Bar (Dark Icons)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode

        setContentView(R.layout.activity_wali_kelas_kehadiran)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
        loadProfilPhoto()

        val namaKelasIntent = intent.getStringExtra("nama_kelas")
        if (!namaKelasIntent.isNullOrEmpty()) {
            tvKelasBadge.text = namaKelasIntent
        }

        loadData()
    }

    private fun initViews() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        ivProfilPhoto = findViewById(R.id.ivProfilPhoto)
        tvKelasBadge = findViewById(R.id.tvKelasBadge)
        tvTahunAjaranBadge = findViewById(R.id.tvTahunAjaranBadge)
        tvWaliKelasNama = findViewById(R.id.tvWaliKelasNama)

        btnPrevMonth = findViewById(R.id.btnPrevMonth)
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth)
        btnNextMonth = findViewById(R.id.btnNextMonth)

        tvMetricHadirPersen = findViewById(R.id.tvMetricHadirPersen)
        tvMetricHadirTotal = findViewById(R.id.tvMetricHadirTotal)
        tvMetricSakit = findViewById(R.id.tvMetricSakit)
        tvMetricIzin = findViewById(R.id.tvMetricIzin)
        tvMetricAlpha = findViewById(R.id.tvMetricAlpha)

        btnCetakMatrix = findViewById(R.id.btnCetakMatrix)
        btnExportExcel = findViewById(R.id.btnExportExcel)

        btnTabDaftarSiswa = findViewById(R.id.btnTabDaftarSiswa)
        btnTabMatriks = findViewById(R.id.btnTabMatriks)
        layoutTabDaftarSiswa = findViewById(R.id.layoutTabDaftarSiswa)
        layoutTabMatriks = findViewById(R.id.layoutTabMatriks)

        cvInfoLibur = findViewById(R.id.cvInfoLibur)
        tvInfoLiburHeader = findViewById(R.id.tvInfoLiburHeader)
        tvTotalLiburBadge = findViewById(R.id.tvTotalLiburBadge)
        containerInfoLiburItems = findViewById(R.id.containerInfoLiburItems)

        cvMatriksPresensi = findViewById(R.id.cvMatriksPresensi)
        containerMatriksFixed = findViewById(R.id.containerMatriksFixed)
        containerMatriksScrollable = findViewById(R.id.containerMatriksScrollable)

        etCariSiswa = findViewById(R.id.etCariSiswa)
        tvDaftarSiswaHeader = findViewById(R.id.tvDaftarSiswaHeader)
        btnUrutkanSiswa = findViewById(R.id.btnUrutkanSiswa)

        chipSemua = findViewById(R.id.chipSemua)
        chipAlpha = findViewById(R.id.chipAlpha)
        chipHadir100 = findViewById(R.id.chipHadir100)
        chipSakitIzin = findViewById(R.id.chipSakitIzin)

        pbLoading = findViewById(R.id.pbLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)
        rvDaftarSiswa = findViewById(R.id.rvDaftarSiswa)

        rvDaftarSiswa.layoutManager = LinearLayoutManager(this)
        rvDaftarSiswa.setHasFixedSize(false)

        updateMonthLabel()
    }

    private fun setupListeners() {
        btnPrevMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateMonthLabel()
            loadData()
        }

        btnNextMonth.setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateMonthLabel()
            loadData()
        }

        tvSelectedMonth.setOnClickListener {
            showMonthPicker()
        }

        // Tab Switcher
        btnTabDaftarSiswa.setOnClickListener { switchTab(isDaftar = true) }
        btnTabMatriks.setOnClickListener { switchTab(isDaftar = false) }

        // Direct Download Matrix PDF
        btnCetakMatrix.setOnClickListener {
            val bulanIso = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCalendar.time)
            val cleanKelas = tvKelasBadge.text.toString().replace("[^a-zA-Z0-9]".toRegex(), "_")
            val ts = System.currentTimeMillis() % 100000
            val fileName = "Matriks_Presensi_${cleanKelas}_${bulanIso}_$ts.pdf"

            val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
            val idUser = sharedPref.getString("id_user", "") ?: ""

            val url = if (!cetakMatrixUrl.isNullOrEmpty() && cetakMatrixUrl!!.contains("download-matrix")) {
                cetakMatrixUrl!!
            } else {
                "https://smkriyadhuljannahjalancagak.sch.id/api/walikelas/download-matrix?id_user=$idUser&bulan=$bulanIso"
            }
            downloadFileDirect(url, fileName, "application/pdf")
        }

        // Direct Download Excel Rekap
        btnExportExcel.setOnClickListener {
            val bulanIso = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCalendar.time)
            val cleanKelas = tvKelasBadge.text.toString().replace("[^a-zA-Z0-9]".toRegex(), "_")
            val ts = System.currentTimeMillis() % 100000
            val fileName = "Rekap_Kehadiran_${cleanKelas}_${bulanIso}_$ts.xlsx"

            val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
            val idUser = sharedPref.getString("id_user", "") ?: ""

            val url = if (!exportExcelUrl.isNullOrEmpty() && exportExcelUrl!!.contains("download-excel")) {
                exportExcelUrl!!
            } else {
                "https://smkriyadhuljannahjalancagak.sch.id/api/walikelas/download-excel?id_user=$idUser&bulan=$bulanIso"
            }
            downloadFileDirect(url, fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
        }

        // Live Search
        etCariSiswa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter?.filterBySearch(s.toString())
                val size = adapter?.itemCount ?: 0
                tvEmptyState.visibility = if (size == 0 && allSiswaList.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Sort By Nomor Absen vs Nama A-Z
        btnUrutkanSiswa.setOnClickListener {
            isSortByNomorAbsen = !isSortByNomorAbsen
            btnUrutkanSiswa.text = if (isSortByNomorAbsen) "Urut Nomor Absen ⌄" else "Urut Nama A-Z ⌄"
            adapter?.sortBy(isSortByNomorAbsen)
        }

        // Chips Filter
        chipSemua.setOnClickListener { setFilter("Semua") }
        chipAlpha.setOnClickListener { setFilter("Alpha > 1") }
        chipHadir100.setOnClickListener { setFilter("100% Hadir") }
        chipSakitIzin.setOnClickListener { setFilter("Sakit/Izin") }
    }

    private fun switchTab(isDaftar: Boolean) {
        isTabDaftarActive = isDaftar

        val activeBg = R.drawable.bg_rounded_border
        val activeBgTint = Color.parseColor("#1E1B4B")
        val inactiveTextColor = Color.parseColor("#64748B")

        if (isDaftar) {
            btnTabDaftarSiswa.setBackgroundResource(activeBg)
            btnTabDaftarSiswa.backgroundTintList = ColorStateList.valueOf(activeBgTint)
            btnTabDaftarSiswa.setTextColor(Color.WHITE)
            btnTabDaftarSiswa.setTypeface(null, Typeface.BOLD)

            btnTabMatriks.background = null
            btnTabMatriks.backgroundTintList = null
            btnTabMatriks.setTextColor(inactiveTextColor)
            btnTabMatriks.setTypeface(null, Typeface.NORMAL)

            layoutTabDaftarSiswa.visibility = View.VISIBLE
            layoutTabMatriks.visibility = View.GONE
        } else {
            btnTabMatriks.setBackgroundResource(activeBg)
            btnTabMatriks.backgroundTintList = ColorStateList.valueOf(activeBgTint)
            btnTabMatriks.setTextColor(Color.WHITE)
            btnTabMatriks.setTypeface(null, Typeface.BOLD)

            btnTabDaftarSiswa.background = null
            btnTabDaftarSiswa.backgroundTintList = null
            btnTabDaftarSiswa.setTextColor(inactiveTextColor)
            btnTabDaftarSiswa.setTypeface(null, Typeface.NORMAL)

            layoutTabDaftarSiswa.visibility = View.GONE
            layoutTabMatriks.visibility = View.VISIBLE

            // Lazy rendering matrix jika belum pernah di-render untuk bulan ini
            if (!isMatrixRendered && cachedDates.isNotEmpty() && cachedSiswaList.isNotEmpty()) {
                renderMatrixTable(cachedDates, cachedSiswaList)
                isMatrixRendered = true
            }
        }
    }

    private fun downloadFileDirect(downloadUrl: String, fileName: String, mimeType: String) {
        try {
            val secureUrl = downloadUrl.replace("http://", "https://")
            val uri = Uri.parse(secureUrl)
            val manager = getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
            if (manager != null) {
                val request = DownloadManager.Request(uri).apply {
                    setTitle(fileName)
                    setDescription("Mengunduh berkas $fileName...")
                    setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                    try {
                        setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                    } catch (_: Exception) {}
                    setMimeType(mimeType)
                    setAllowedOverMetered(true)
                    setAllowedOverRoaming(true)
                }
                manager.enqueue(request)
                Toast.makeText(this, "📥 Mengunduh $fileName\nCek bar notifikasi dan folder Download HP Anda.", Toast.LENGTH_LONG).show()
            } else {
                val intent = Intent(Intent.ACTION_VIEW, uri)
                startActivity(intent)
            }
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl.replace("http://", "https://")))
                startActivity(intent)
            } catch (_: Exception) {
                Toast.makeText(this, "Gagal mengunduh: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun updateMonthLabel() {
        val format = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        tvSelectedMonth.text = "📅 " + format.format(currentCalendar.time)
    }

    private fun showMonthPicker() {
        val dpd = DatePickerDialog(
            this,
            { _, year, month, _ ->
                currentCalendar.set(Calendar.YEAR, year)
                currentCalendar.set(Calendar.MONTH, month)
                currentCalendar.set(Calendar.DAY_OF_MONTH, 1)
                updateMonthLabel()
                loadData()
            },
            currentCalendar.get(Calendar.YEAR),
            currentCalendar.get(Calendar.MONTH),
            currentCalendar.get(Calendar.DAY_OF_MONTH)
        )
        dpd.show()
    }

    private fun setFilter(filterType: String) {
        currentFilterType = filterType

        val activeBg = R.drawable.bg_rounded_border
        val inactiveBg = R.drawable.bg_rounded_border_outline

        fun updateChip(tv: TextView, isActive: Boolean, activeColor: Int, activeBgTint: Int, inactiveColor: Int) {
            tv.setBackgroundResource(if (isActive) activeBg else inactiveBg)
            tv.setTextColor(if (isActive) activeColor else inactiveColor)
            tv.backgroundTintList = if (isActive) ColorStateList.valueOf(activeBgTint) else null
        }

        updateChip(chipSemua, filterType == "Semua", Color.WHITE, Color.parseColor("#1E1B4B"), Color.parseColor("#64748B"))
        updateChip(chipAlpha, filterType == "Alpha > 1", Color.parseColor("#DC2626"), Color.parseColor("#FEE2E2"), Color.parseColor("#DC2626"))
        updateChip(chipHadir100, filterType == "100% Hadir", Color.WHITE, Color.parseColor("#059669"), Color.parseColor("#059669"))
        updateChip(chipSakitIzin, filterType == "Sakit/Izin", Color.WHITE, Color.parseColor("#D97706"), Color.parseColor("#64748B"))

        adapter?.filterByCategory(filterType)
        val size = adapter?.itemCount ?: 0
        tvEmptyState.visibility = if (size == 0 && allSiswaList.isNotEmpty()) View.VISIBLE else View.GONE
    }

    private fun loadProfilPhoto() {
        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val fotoProfilUrl = sharedPref.getString("foto_profil", null)
        if (!fotoProfilUrl.isNullOrEmpty() && ::ivProfilPhoto.isInitialized) {
            val fullUrl = if (fotoProfilUrl.startsWith("http")) fotoProfilUrl else "https://smkriyadhuljannahjalancagak.sch.id/uploads/guru/$fotoProfilUrl"
            try {
                Glide.with(this)
                    .load(fullUrl)
                    .circleCrop()
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .into(ivProfilPhoto)
            } catch (_: Exception) {}
        }
    }

    private fun loadData() {
        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val identifier = sharedPref.getString("id_user", "") ?: ""

        if (identifier.isEmpty()) {
            Toast.makeText(this, "Sesi login tidak valid", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val bulanIso = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(currentCalendar.time)

        // Super Fast Cache: Jika data bulan ini sudah pernah dimuat, render seketika (0 ms)
        if (monthlyDataCache.containsKey(bulanIso)) {
            val cached = monthlyDataCache[bulanIso]!!
            renderUi(cached, bulanIso)
            pbLoading.visibility = View.GONE
        } else {
            pbLoading.visibility = View.VISIBLE
            rvDaftarSiswa.visibility = View.GONE
            tvEmptyState.visibility = View.GONE
        }

        containerMatriksFixed.removeAllViews()
        containerMatriksScrollable.removeAllViews()
        isMatrixRendered = false
        cachedDates = emptyList()
        cachedSiswaList = emptyList()

        loadJob?.cancel()
        loadJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getWaliKelasRekapKehadiran(identifier, bulanIso)
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.status == true) {
                        val body = response.body()
                        if (body != null) {
                            monthlyDataCache[bulanIso] = body
                            renderUi(body, bulanIso)
                        }
                    } else {
                        if (!monthlyDataCache.containsKey(bulanIso)) {
                            val errMsg = response.body()?.message ?: "Gagal memuat rekap kehadiran (${response.code()})"
                            tvEmptyState.visibility = View.VISIBLE
                            tvEmptyState.text = errMsg
                            Toast.makeText(this@WaliKelasKehadiranActivity, errMsg, Toast.LENGTH_LONG).show()
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    if (!monthlyDataCache.containsKey(bulanIso)) {
                        tvEmptyState.visibility = View.VISIBLE
                        tvEmptyState.text = "Gagal terhubung ke server: ${e.localizedMessage}"
                        Toast.makeText(this@WaliKelasKehadiranActivity, "Koneksi bermasalah: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun renderUi(res: WaliKelasRekapKehadiranResponse, bulanIso: String) {
        // 1. Header Info
        if (!res.kelas.isNullOrEmpty()) tvKelasBadge.text = res.kelas
        if (!res.tahun_ajaran.isNullOrEmpty()) tvTahunAjaranBadge.text = res.tahun_ajaran
        if (!res.wali_kelas.isNullOrEmpty()) tvWaliKelasNama.text = "Wali Kelas: ${res.wali_kelas}"

        cetakMatrixUrl = res.cetak_matrix_url
        exportExcelUrl = res.export_excel_url
        cetakSiswaBaseUrl = res.cetak_siswa_base_url

        // 2. Summary Metrics
        val sum = res.summary
        if (sum != null) {
            tvMetricHadirPersen.text = "${sum.persentase_hadir}%"
            tvMetricHadirTotal.text = "${sum.total_hadir} Total"
            tvMetricSakit.text = sum.total_sakit.toString()
            tvMetricIzin.text = sum.total_izin.toString()
            tvMetricAlpha.text = sum.total_alpha.toString()
        }

        // 3. Info Libur Card
        val infoList = res.info_libur ?: emptyList()
        val totalLibur = res.total_libur_hari ?: infoList.size
        tvTotalLiburBadge.text = "$totalLibur Hari"
        tvInfoLiburHeader.text = "Info Libur & Keterangan: ${res.bulan_formatted ?: bulanIso}"

        containerInfoLiburItems.removeAllViews()
        if (infoList.isNotEmpty()) {
            cvInfoLibur.visibility = View.VISIBLE
            val density = resources.displayMetrics.density
            for (line in infoList) {
                val tv = TextView(this).apply {
                    text = "• $line"
                    textSize = 11.5f
                    setTextColor(Color.parseColor("#1E40AF"))
                    setPadding(0, (2 * density).toInt(), 0, (2 * density).toInt())
                }
                containerInfoLiburItems.addView(tv)
            }
        } else {
            cvInfoLibur.visibility = View.GONE
        }

        // 4. Matrix Table Data Caching & Lazy Render (Ultra Fast Initial Load)
        cachedDates = res.dates ?: emptyList()
        cachedSiswaList = res.data ?: emptyList()
        val siswaList = cachedSiswaList
        allSiswaList = cachedSiswaList
        isMatrixRendered = false

        if (!isTabDaftarActive) {
            renderMatrixTable(cachedDates, cachedSiswaList)
            isMatrixRendered = true
        }

        // 5. Update Chips Counter
        val total = siswaList.size
        val alphaCount = siswaList.count { it.alpha > 1 }
        val hadir100Count = siswaList.count { it.alpha == 0 && it.sakit == 0 && it.izin == 0 && it.hadir > 0 }
        val sakitIzinCount = siswaList.count { it.sakit > 0 || it.izin > 0 }

        tvDaftarSiswaHeader.text = "DAFTAR REKAP SISWA ($total)"
        chipSemua.text = "Semua ($total)"
        chipAlpha.text = "Alpha > 1 ($alphaCount)"
        chipHadir100.text = "100% Hadir ($hadir100Count)"
        chipSakitIzin.text = "Sakit/Izin ($sakitIzinCount)"

        // 6. RecyclerView Setup (Cetak Per Siswa Direct Download)
        adapter = RekapSiswaBinaanAdapter(siswaList) { student ->
            val cleanNama = student.nama_siswa.replace("[^a-zA-Z0-9]".toRegex(), "_")
            val ts = System.currentTimeMillis() % 100000
            val fileName = "Rekap_Kehadiran_${cleanNama}_${bulanIso}_$ts.pdf"

            val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
            val idUser = sharedPref.getString("id_user", "") ?: ""

            val downloadUrl = if (!cetakSiswaBaseUrl.isNullOrEmpty() && cetakSiswaBaseUrl!!.contains("download-siswa-pdf")) {
                "$cetakSiswaBaseUrl${student.siswa_id}"
            } else {
                "https://smkriyadhuljannahjalancagak.sch.id/api/walikelas/download-siswa-pdf?id_user=$idUser&bulan=$bulanIso&id_siswa=${student.siswa_id}"
            }
            downloadFileDirect(downloadUrl, fileName, "application/pdf")
        }
        rvDaftarSiswa.adapter = adapter
        rvDaftarSiswa.visibility = if (siswaList.isNotEmpty()) View.VISIBLE else View.GONE
        tvEmptyState.visibility = if (siswaList.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun renderMatrixTable(dates: List<RekapKehadiranDateItem>, siswaList: List<RekapKehadiranSiswaItem>) {
        containerMatriksFixed.removeAllViews()
        containerMatriksScrollable.removeAllViews()

        if (dates.isEmpty() || siswaList.isEmpty()) {
            cvMatriksPresensi.visibility = View.GONE
            return
        }
        cvMatriksPresensi.visibility = View.VISIBLE

        val density = resources.displayMetrics.density
        val headerHeight = (42 * density).toInt()
        val rowHeight = (36 * density).toInt()
        val dividerHeight = (1 * density).toInt()

        // ==========================================
        // 1. HEADER ROW (SINKRON TINGGI: headerHeight)
        // ==========================================

        // --- Sisi Kiri (Fixed): Header Nama Siswa ---
        val leftHeaderRow = TableRow(this).apply {
            layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, headerHeight)
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
        val tvHeaderNama = TextView(this).apply {
            text = "Nama Siswa"
            textSize = 11.5f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#475569"))
            gravity = Gravity.CENTER_VERTICAL
            setPadding((10 * density).toInt(), 0, (8 * density).toInt(), 0)
            layoutParams = TableRow.LayoutParams((140 * density).toInt(), headerHeight)
        }
        leftHeaderRow.addView(tvHeaderNama)
        containerMatriksFixed.addView(leftHeaderRow)

        containerMatriksFixed.addView(View(this).apply {
            layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, dividerHeight)
            setBackgroundColor(Color.parseColor("#CBD5E1"))
        })

        // --- Sisi Kanan (Scrollable): Header Tanggal + Summary ---
        val rightHeaderRow = TableRow(this).apply {
            layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, headerHeight)
            setBackgroundColor(Color.parseColor("#F8FAFC"))
        }
        for (d in dates) {
            val tvDateCol = TextView(this).apply {
                text = "${d.hari_short}\n${d.tgl_num}"
                textSize = 9f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(if (d.is_libur) Color.parseColor("#DC2626") else Color.parseColor("#1E293B"))
                setBackgroundColor(if (d.is_libur) Color.parseColor("#FEE2E2") else Color.parseColor("#F8FAFC"))
                layoutParams = TableRow.LayoutParams((34 * density).toInt(), headerHeight)
            }
            rightHeaderRow.addView(tvDateCol)
        }

        fun addSummaryHeader(title: String, colorHex: String) {
            val tv = TextView(this).apply {
                text = title
                textSize = 10f
                gravity = Gravity.CENTER
                setTypeface(null, Typeface.BOLD)
                setTextColor(Color.parseColor(colorHex))
                setBackgroundColor(Color.parseColor("#F1F5F9"))
                layoutParams = TableRow.LayoutParams((28 * density).toInt(), headerHeight)
            }
            rightHeaderRow.addView(tv)
        }
        addSummaryHeader("H", "#059669")
        addSummaryHeader("S", "#2563EB")
        addSummaryHeader("I", "#D97706")
        addSummaryHeader("A", "#DC2626")
        containerMatriksScrollable.addView(rightHeaderRow)

        containerMatriksScrollable.addView(View(this).apply {
            layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, dividerHeight)
            setBackgroundColor(Color.parseColor("#CBD5E1"))
        })

        // ==========================================
        // 2. DATA ROWS (SINKRON TINGGI: rowHeight)
        // ==========================================
        for ((idx, s) in siswaList.withIndex()) {
            val rowBg = if (idx % 2 == 0) Color.WHITE else Color.parseColor("#F8FAFC")

            // --- Sisi Kiri (Fixed): No & Nama Siswa ---
            val leftRow = TableRow(this).apply {
                layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, rowHeight)
                setBackgroundColor(rowBg)
            }
            val namaPendek = if (s.nama_siswa.length > 18) s.nama_siswa.substring(0, 16) + ".." else s.nama_siswa
            val tvNama = TextView(this).apply {
                text = "${s.nomor_absen.takeIf { it > 0 } ?: (idx + 1)}. $namaPendek"
                textSize = 11f
                setTextColor(Color.parseColor("#1E293B"))
                gravity = Gravity.CENTER_VERTICAL
                setPadding((10 * density).toInt(), 0, (6 * density).toInt(), 0)
                layoutParams = TableRow.LayoutParams((140 * density).toInt(), rowHeight)
                maxLines = 1
            }
            leftRow.addView(tvNama)
            containerMatriksFixed.addView(leftRow)

            containerMatriksFixed.addView(View(this).apply {
                layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, dividerHeight)
                setBackgroundColor(Color.parseColor("#F1F5F9"))
            })

            // --- Sisi Kanan (Scrollable): Tanggal & Totals ---
            val rightRow = TableRow(this).apply {
                layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.WRAP_CONTENT, rowHeight)
                setBackgroundColor(rowBg)
            }
            val harian = s.harian ?: emptyMap()
            for (d in dates) {
                val status = harian[d.tanggal] ?: "-"
                val tvCell = TextView(this).apply {
                    textSize = 10.5f
                    gravity = Gravity.CENTER
                    layoutParams = TableRow.LayoutParams((34 * density).toInt(), rowHeight)

                    when (status) {
                        "H" -> {
                            text = "●"
                            setTextColor(Color.parseColor("#059669"))
                            setTypeface(null, Typeface.BOLD)
                        }
                        "S" -> {
                            text = "S"
                            setTextColor(Color.parseColor("#2563EB"))
                            setTypeface(null, Typeface.BOLD)
                        }
                        "I" -> {
                            text = "I"
                            setTextColor(Color.parseColor("#D97706"))
                            setTypeface(null, Typeface.BOLD)
                        }
                        "A" -> {
                            text = "A"
                            setTextColor(Color.parseColor("#DC2626"))
                            setTypeface(null, Typeface.BOLD)
                        }
                        else -> {
                            text = "-"
                            setTextColor(Color.parseColor("#CBD5E1"))
                        }
                    }
                }
                rightRow.addView(tvCell)
            }

            fun addSummaryCell(value: Int, colorHex: String) {
                val tv = TextView(this).apply {
                    text = value.toString()
                    textSize = 10.5f
                    gravity = Gravity.CENTER
                    setTypeface(null, Typeface.BOLD)
                    setTextColor(Color.parseColor(colorHex))
                    layoutParams = TableRow.LayoutParams((28 * density).toInt(), rowHeight)
                }
                rightRow.addView(tv)
            }
            addSummaryCell(s.hadir, "#059669")
            addSummaryCell(s.sakit, "#2563EB")
            addSummaryCell(s.izin, "#D97706")
            addSummaryCell(s.alpha, "#DC2626")

            containerMatriksScrollable.addView(rightRow)

            containerMatriksScrollable.addView(View(this).apply {
                layoutParams = TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, dividerHeight)
                setBackgroundColor(Color.parseColor("#F1F5F9"))
            })
        }
    }

    override fun finish() {
        super.finish()
        applyExitTransition()
    }
}
