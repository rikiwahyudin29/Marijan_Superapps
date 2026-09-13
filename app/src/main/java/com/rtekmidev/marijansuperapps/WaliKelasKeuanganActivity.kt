package com.rtekmidev.marijansuperapps

import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import com.airbnb.lottie.LottieAnimationView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rtekmidev.marijansuperapps.api.ApiClient
import com.rtekmidev.marijansuperapps.api.KeuanganPosItem
import com.rtekmidev.marijansuperapps.api.KeuanganSiswaItem
import com.rtekmidev.marijansuperapps.api.KeuanganSummary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.text.NumberFormat
import java.util.Locale

class WaliKelasKeuanganActivity : AppCompatActivity() {

    private lateinit var tvHeaderTitle: TextView
    private lateinit var ivProfilPhoto: ImageView

    // Hero Views
    private lateinit var tvKelasBadge: TextView
    private lateinit var tvTahunAjaranBadge: TextView
    private lateinit var tvWaliKelasNama: TextView
    private lateinit var tvStatTotalTagihan: TextView
    private lateinit var tvStatTotalSiswa: TextView
    private lateinit var tvStatTerbayar: TextView
    private lateinit var tvStatPersenTerbayar: TextView
    private lateinit var tvStatSisa: TextView
    private lateinit var tvStatSiswaBelumLunas: TextView

    // Action Buttons
    private lateinit var btnCetakRekapPdf: LinearLayout
    private lateinit var btnExportExcel: LinearLayout

    // Filter Pos & Search
    private lateinit var spFilterPos: Spinner
    private lateinit var etSearchSiswa: EditText
    private lateinit var btnClearSearch: ImageView

    // Section Header & Sort
    private lateinit var tvDaftarCountBadge: TextView
    private lateinit var btnSortOrder: LinearLayout
    private lateinit var tvSortLabel: TextView

    // Status Chips
    private lateinit var chipSemua: LinearLayout
    private lateinit var tvChipSemua: TextView
    private lateinit var chipBelumLunas: LinearLayout
    private lateinit var tvChipBelumLunas: TextView
    private lateinit var chipLunas: LinearLayout
    private lateinit var tvChipLunas: TextView

    // Recycler & Loading
    private lateinit var layoutLoadingContainer: LinearLayout
    private lateinit var progressBarLoading: LottieAnimationView
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var rvKeuanganSiswa: RecyclerView

    private lateinit var adapter: KeuanganSiswaBinaanAdapter

    private var identifier: String = ""
    private var namaKelas: String = "Kelas Binaan"
    private var namaWaliKelas: String = "-"
    private var selectedPosId: Int? = null
    private var currentSortMode: String = "ABSEN"
    private var currentFilterStatus: String = "ALL"

    private var cetakRekapUrl: String? = null
    private var exportExcelUrl: String? = null
    private var cetakTagihanBaseUrl: String? = null

    private var posList: MutableList<KeuanganPosItem> = mutableListOf()
    private var isSpinnerInitialized = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEnterTransition()

        // Edge-to-edge status bar configuration
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode

        setContentView(R.layout.activity_wali_kelas_keuangan)

        val rootLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        identifier = sharedPref.getString("id_user", "") ?: ""
        val fotoProfilUrl = sharedPref.getString("foto_profil", null)
        namaKelas = intent.getStringExtra("nama_kelas") ?: "Kelas Binaan"

        initViews()
        loadProfilePic(fotoProfilUrl)
        setupRecyclerView()
        setupListeners()

        // Fetch Data
        loadData(null)
    }

    private fun initViews() {
        findViewById<View>(R.id.btnBack)?.setOnClickListener { finish() }

        tvHeaderTitle = findViewById(R.id.tvHeaderTitle)
        ivProfilPhoto = findViewById(R.id.ivProfilPhoto)

        // Hero
        tvKelasBadge = findViewById(R.id.tvKelasBadge)
        tvTahunAjaranBadge = findViewById(R.id.tvTahunAjaranBadge)
        tvWaliKelasNama = findViewById(R.id.tvWaliKelasNama)
        tvStatTotalTagihan = findViewById(R.id.tvStatTotalTagihan)
        tvStatTotalSiswa = findViewById(R.id.tvStatTotalSiswa)
        tvStatTerbayar = findViewById(R.id.tvStatTerbayar)
        tvStatPersenTerbayar = findViewById(R.id.tvStatPersenTerbayar)
        tvStatSisa = findViewById(R.id.tvStatSisa)
        tvStatSiswaBelumLunas = findViewById(R.id.tvStatSiswaBelumLunas)

        // Actions
        btnCetakRekapPdf = findViewById(R.id.btnCetakRekapPdf)
        btnExportExcel = findViewById(R.id.btnExportExcel)

        // Filters & Search
        spFilterPos = findViewById(R.id.spFilterPos)
        etSearchSiswa = findViewById(R.id.etSearchSiswa)
        btnClearSearch = findViewById(R.id.btnClearSearch)

        // Section & Sort
        tvDaftarCountBadge = findViewById(R.id.tvDaftarCountBadge)
        btnSortOrder = findViewById(R.id.btnSortOrder)
        tvSortLabel = findViewById(R.id.tvSortLabel)

        // Chips
        chipSemua = findViewById(R.id.chipSemua)
        tvChipSemua = findViewById(R.id.tvChipSemua)
        chipBelumLunas = findViewById(R.id.chipBelumLunas)
        tvChipBelumLunas = findViewById(R.id.tvChipBelumLunas)
        chipLunas = findViewById(R.id.chipLunas)
        tvChipLunas = findViewById(R.id.tvChipLunas)

        // Recycler & Loading
        layoutLoadingContainer = findViewById(R.id.layoutLoadingContainer)
        progressBarLoading = findViewById(R.id.progressBarLoading)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        rvKeuanganSiswa = findViewById(R.id.rvKeuanganSiswa)

        tvKelasBadge.text = namaKelas
    }

    private fun loadProfilePic(fotoUrl: String?) {
        if (!fotoUrl.isNullOrEmpty()) {
            val fullUrl = if (fotoUrl.startsWith("http")) fotoUrl else "https://smkriyadhuljannahjalancagak.sch.id/uploads/guru/$fotoUrl"
            try {
                Glide.with(this)
                    .load(fullUrl)
                    .circleCrop()
                    .into(ivProfilPhoto)
            } catch (_: Exception) {}
        }
    }

    private fun setupRecyclerView() {
        adapter = KeuanganSiswaBinaanAdapter(
            onCetakTagihanClick = { siswa ->
                downloadTagihanSiswaPdf(siswa)
            },
            onWaOrtuClick = { siswa ->
                kirimPesanWaOrtu(siswa)
            }
        )

        rvKeuanganSiswa.layoutManager = LinearLayoutManager(this)
        rvKeuanganSiswa.adapter = adapter

        // Data Observer untuk Empty State dan Badge Counter
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onChanged() {
                super.onChanged()
                updateListCounters()
            }
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                updateListCounters()
            }
            override fun onItemRangeRemoved(positionStart: Int, itemCount: Int) {
                super.onItemRangeRemoved(positionStart, itemCount)
                updateListCounters()
            }
        })
    }

    private fun updateListCounters() {
        val filteredCount = adapter.getFilteredItemCount()
        tvDaftarCountBadge.text = filteredCount.toString()
        if (::layoutLoadingContainer.isInitialized && layoutLoadingContainer.visibility == View.VISIBLE) {
            rvKeuanganSiswa.visibility = View.GONE
            layoutEmptyState.visibility = View.GONE
        } else if (filteredCount == 0) {
            layoutEmptyState.visibility = View.VISIBLE
            rvKeuanganSiswa.visibility = View.GONE
        } else {
            layoutEmptyState.visibility = View.GONE
            rvKeuanganSiswa.visibility = View.VISIBLE
        }
    }

    private fun setupListeners() {
        // Cetak Rekap PDF Kelas
        btnCetakRekapPdf.setOnClickListener {
            downloadRekapPdf()
        }

        // Export Excel Rekap Kelas
        btnExportExcel.setOnClickListener {
            downloadRekapExcel()
        }

        // Search Listener
        etSearchSiswa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s?.toString() ?: ""
                btnClearSearch.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
                adapter.setSearchQuery(query)
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnClearSearch.setOnClickListener {
            etSearchSiswa.setText("")
        }

        // Sort Listener
        btnSortOrder.setOnClickListener {
            showSortDialog()
        }

        // Status Chips Listener
        chipSemua.setOnClickListener {
            setActiveChip("ALL")
        }
        chipBelumLunas.setOnClickListener {
            setActiveChip("BELUM_LUNAS")
        }
        chipLunas.setOnClickListener {
            setActiveChip("LUNAS")
        }
    }

    private fun setActiveChip(status: String) {
        currentFilterStatus = status
        adapter.setFilterStatus(status)

        // Reset Styles
        chipSemua.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
        tvChipSemua.setTextColor(Color.parseColor("#64748B"))

        chipBelumLunas.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
        tvChipBelumLunas.setTextColor(Color.parseColor("#DC2626"))

        chipLunas.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
        tvChipLunas.setTextColor(Color.parseColor("#059669"))

        // Highlight Active Chip
        when (status) {
            "ALL" -> {
                chipSemua.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E1B4B"))
                tvChipSemua.setTextColor(Color.parseColor("#FFFFFF"))
            }
            "BELUM_LUNAS" -> {
                chipBelumLunas.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DC2626"))
                tvChipBelumLunas.setTextColor(Color.parseColor("#FFFFFF"))
            }
            "LUNAS" -> {
                chipLunas.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#059669"))
                tvChipLunas.setTextColor(Color.parseColor("#FFFFFF"))
            }
        }
    }

    private fun showSortDialog() {
        val options = arrayOf("Nomor Absen Siswa", "Nama Siswa (A - Z)", "Tunggakan Terbesar")
        val currentSelected = when (currentSortMode) {
            "NAMA_ASC" -> 1
            "TUNGGAKAN_DESC" -> 2
            else -> 0
        }

        AlertDialog.Builder(this)
            .setTitle("Urutkan Berdasarkan")
            .setSingleChoiceItems(options, currentSelected) { dialog, which ->
                when (which) {
                    0 -> {
                        currentSortMode = "ABSEN"
                        tvSortLabel.text = "Urut Absen"
                    }
                    1 -> {
                        currentSortMode = "NAMA_ASC"
                        tvSortLabel.text = "Nama A-Z"
                    }
                    2 -> {
                        currentSortMode = "TUNGGAKAN_DESC"
                        tvSortLabel.text = "Tunggakan"
                    }
                }
                adapter.setSortMode(currentSortMode)
                dialog.dismiss()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun loadData(posId: Int?) {
        layoutLoadingContainer.visibility = View.VISIBLE
        progressBarLoading.playAnimation()
        rvKeuanganSiswa.visibility = View.GONE
        layoutEmptyState.visibility = View.GONE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.getWaliKelasRekapTagihan(identifier, posId)
                withContext(Dispatchers.Main) {
                    layoutLoadingContainer.visibility = View.GONE
                    progressBarLoading.pauseAnimation()
                    if (response.isSuccessful && response.body() != null) {
                        val body = response.body()!!
                        val summary = body.summary ?: KeuanganSummary()

                        cetakRekapUrl = body.cetak_rekap_url
                        exportExcelUrl = body.export_excel_url
                        cetakTagihanBaseUrl = body.cetak_tagihan_base_url

                        bindSummaryData(summary, body.kelas, body.tahun_ajaran, body.wali_kelas)
                        namaWaliKelas = body.wali_kelas ?: "-"
                        namaKelas = body.kelas ?: namaKelas

                        if (!isSpinnerInitialized && body.pos_list != null) {
                            setupSpinnerPos(body.pos_list)
                            isSpinnerInitialized = true
                        }

                        val siswaList = body.data ?: emptyList()
                        adapter.setMasterData(siswaList)

                        // Update Chips Count
                        val (total, belum, lunas) = adapter.getCounts()
                        tvChipSemua.text = "Semua ($total)"
                        tvChipBelumLunas.text = "Belum Lunas ($belum)"
                        tvChipLunas.text = "Lunas ($lunas)"

                        updateListCounters()
                    } else {
                        Toast.makeText(this@WaliKelasKeuanganActivity, "Gagal memuat data keuangan kelas", Toast.LENGTH_SHORT).show()
                        updateListCounters()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    layoutLoadingContainer.visibility = View.GONE
                    progressBarLoading.pauseAnimation()
                    Toast.makeText(this@WaliKelasKeuanganActivity, "Koneksi terputus: ${e.message}", Toast.LENGTH_SHORT).show()
                    updateListCounters()
                }
            }
        }
    }

    private fun bindSummaryData(summary: KeuanganSummary, kelas: String?, ta: String?, wali: String?) {
        if (!kelas.isNullOrEmpty()) tvKelasBadge.text = kelas
        if (!ta.isNullOrEmpty()) tvTahunAjaranBadge.text = ta
        if (!wali.isNullOrEmpty()) tvWaliKelasNama.text = "Wali Kelas: $wali"

        tvStatTotalTagihan.text = formatRupiah(summary.total_tagihan)
        tvStatTotalSiswa.text = "${summary.total_siswa} Siswa"

        tvStatTerbayar.text = formatRupiah(summary.total_terbayar)
        tvStatPersenTerbayar.text = "${summary.persentase_selesai} Terkumpul"

        tvStatSisa.text = formatRupiah(summary.sisa_tunggakan)
        tvStatSiswaBelumLunas.text = "${summary.total_belum_lunas} Siswa"
    }

    private fun setupSpinnerPos(list: List<KeuanganPosItem>) {
        posList.clear()
        // Item default: Semua Pos
        posList.add(KeuanganPosItem(id = 0, nama_pos = "-- Semua Pos Bayar --"))
        posList.addAll(list)

        val spinnerItems = posList.map { it.nama_pos }
        val spinnerAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, spinnerItems)
        spFilterPos.adapter = spinnerAdapter

        spFilterPos.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val selected = posList[position]
                val newPosId = if (selected.id == 0) null else selected.id
                if (newPosId != selectedPosId) {
                    selectedPosId = newPosId
                    loadData(selectedPosId)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun showLoadingDialog(message: String): AlertDialog {
        val dialogView = layoutInflater.inflate(R.layout.dialog_loading_lottie, null)
        dialogView.findViewById<TextView>(R.id.tvDialogMessage)?.text = message
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .setCancelable(false)
            .create()
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        try {
            dialog.show()
        } catch (_: Exception) {}
        return dialog
    }

    private fun downloadRekapPdf() {
        val dialog = showLoadingDialog("Menyiapkan Rekap PDF...")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try { dialog.dismiss() } catch (_: Exception) {}
        }, 1200)

        val cleanKelas = namaKelas.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val ts = System.currentTimeMillis() % 100000
        val fileName = "Rekap_Keuangan_${cleanKelas}_$ts.pdf"

        val posParam = if (selectedPosId != null && selectedPosId != 0) "&pos=$selectedPosId" else ""
        val downloadUrl = if (!cetakRekapUrl.isNullOrEmpty() && cetakRekapUrl!!.contains("download-keuangan-pdf")) {
            cetakRekapUrl!!
        } else {
            "https://smkriyadhuljannahjalancagak.sch.id/api/walikelas/download-keuangan-pdf?id_user=${identifier}$posParam"
        }

        downloadFileDirect(downloadUrl, fileName, "application/pdf")
    }

    private fun downloadRekapExcel() {
        val dialog = showLoadingDialog("Menyiapkan Berkas Excel...")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try { dialog.dismiss() } catch (_: Exception) {}
        }, 1200)

        val cleanKelas = namaKelas.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val ts = System.currentTimeMillis() % 100000
        val fileName = "Rekap_Keuangan_${cleanKelas}_$ts.xlsx"

        val posParam = if (selectedPosId != null && selectedPosId != 0) "&pos=$selectedPosId" else ""
        val downloadUrl = if (!exportExcelUrl.isNullOrEmpty() && exportExcelUrl!!.contains("download-keuangan-excel")) {
            exportExcelUrl!!
        } else {
            "https://smkriyadhuljannahjalancagak.sch.id/api/walikelas/download-keuangan-excel?id_user=${identifier}$posParam"
        }

        downloadFileDirect(downloadUrl, fileName, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    }

    private fun downloadTagihanSiswaPdf(siswa: KeuanganSiswaItem) {
        val dialog = showLoadingDialog("Menyiapkan Tagihan Siswa...")
        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            try { dialog.dismiss() } catch (_: Exception) {}
        }, 1200)

        val cleanNama = siswa.nama_siswa.replace("[^a-zA-Z0-9]".toRegex(), "_")
        val ts = System.currentTimeMillis() % 100000
        val fileName = "Tagihan_${cleanNama}_$ts.pdf"

        val downloadUrl = if (!cetakTagihanBaseUrl.isNullOrEmpty() && cetakTagihanBaseUrl!!.contains("download-tagihan-siswa-pdf")) {
            "${cetakTagihanBaseUrl}${siswa.siswa_id}"
        } else {
            "https://smkriyadhuljannahjalancagak.sch.id/api/walikelas/download-tagihan-siswa-pdf?id_user=${identifier}&id_siswa=${siswa.siswa_id}"
        }

        downloadFileDirect(downloadUrl, fileName, "application/pdf")
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

    private fun kirimPesanWaOrtu(siswa: KeuanganSiswaItem) {
        val rawPhone = siswa.no_hp_ortu
        if (rawPhone.isNullOrBlank()) {
            Toast.makeText(this, "Nomor WhatsApp Orang Tua ananda ${siswa.nama_siswa} belum terdaftar di sistem.", Toast.LENGTH_LONG).show()
            return
        }

        var cleanPhone = rawPhone.replace(Regex("[^0-9]"), "")
        if (cleanPhone.startsWith("0")) {
            cleanPhone = "62" + cleanPhone.substring(1)
        } else if (!cleanPhone.startsWith("62")) {
            cleanPhone = "62$cleanPhone"
        }

        // Susun daftar tagihan yang belum lunas (sertakan tahun ajaran)
        val rincianList = siswa.rincian ?: emptyList()
        val belumLunasList = rincianList.filter { it.sisa > 0 }
        val rincianBuilder = StringBuilder()
        for (pos in belumLunasList) {
            val taText = if (!pos.tahun_ajaran_lengkap.isNullOrBlank()) " [${pos.tahun_ajaran_lengkap}]" else ""
            rincianBuilder.append("• *${pos.nama_pos}$taText*: Sisa ${formatRupiah(pos.sisa)}\n")
        }

        val rincianText = if (rincianBuilder.isNotEmpty()) {
            rincianBuilder.toString()
        } else {
            "• *Total Tagihan Administrasi*: Sisa ${formatRupiah(siswa.sisa_tunggakan)}\n"
        }

        val pesan = """
Assalamu'alaikum Wr. Wb.

Yth. Bapak/Ibu Orang Tua / Wali dari ananda:
Nama: *${siswa.nama_siswa}*
Kelas: *${namaKelas}*

Berikut kami sampaikan rincian tagihan administrasi sekolah yang masih perlu diselesaikan:
$rincianText
*Total Tunggakan: ${formatRupiah(siswa.sisa_tunggakan)}*

Untuk proses pembayaran atau konfirmasi, Bapak/Ibu dapat mengunjungi loket Tata Usaha / Keuangan MA Riyadhul Jannah atau membalas pesan ini.

Terima kasih atas perhatian dan kerjasamanya.

Wassalamu'alaikum Wr. Wb.
_Wali Kelas: ${namaWaliKelas}_
_MA Riyadhul Jannah Jalancagak_
        """.trimIndent()

        try {
            val encodedMessage = URLEncoder.encode(pesan, "UTF-8")
            val waUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanPhone&text=$encodedMessage")
            val waIntent = Intent(Intent.ACTION_VIEW, waUri)
            startActivity(waIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak dapat membuka aplikasi WhatsApp: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatRupiah(amount: Long): String {
        return try {
            "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount)
        } catch (e: Exception) {
            "Rp $amount"
        }
    }
}
