package com.rtekmidev.marijansuperapps

import android.app.DatePickerDialog
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rtekmidev.marijansuperapps.api.ApiClient
import com.rtekmidev.marijansuperapps.api.SimpanAbsenHarianRequest
import com.rtekmidev.marijansuperapps.api.SiswaAbsenItem
import com.rtekmidev.marijansuperapps.api.SiswaAbsenSubmitItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class WaliKelasAbsenHarianActivity : AppCompatActivity() {

    private lateinit var ivProfilPhoto: ImageView
    private lateinit var tvKelasBadge: TextView
    private lateinit var tvTahunAjaran: TextView
    private lateinit var tvJudulPresensi: TextView
    private lateinit var tvWaliKelasNama: TextView

    private lateinit var tvCountHadir: TextView
    private lateinit var tvCountSakit: TextView
    private lateinit var tvCountIzin: TextView
    private lateinit var tvCountAlpha: TextView

    private lateinit var tvTanggalDipilih: TextView
    private lateinit var btnUbahTanggal: TextView

    private lateinit var btnSetSemuaHadir: TextView
    private lateinit var btnSetSemuaSakit: TextView
    private lateinit var btnSetSemuaIzin: TextView
    private lateinit var btnSetSemuaAlpha: TextView

    private lateinit var etCariSiswa: EditText
    private lateinit var tvDaftarSiswaHeader: TextView
    private lateinit var btnUrutkanSiswa: TextView

    private lateinit var rvDaftarSiswa: RecyclerView
    private lateinit var pbLoading: View
    private lateinit var tvEmptyState: TextView

    private lateinit var tvProgressAbsen: TextView
    private lateinit var tvBadgeStatusLengkap: TextView
    private lateinit var btnBatal: AppCompatButton
    private lateinit var btnSimpanKehadiran: AppCompatButton

    // Holiday State Views
    private lateinit var llMetricBoxes: View
    private lateinit var llFastActionAndSearch: View
    private lateinit var cvStateHariLibur: View
    private lateinit var tvKeteranganLibur: TextView
    private lateinit var llHeaderDaftarSiswa: View
    private lateinit var bottomStickyBar: View
    private var isCurrentHariLibur: Boolean = false

    private var adapter: SiswaAbsenHarianAdapter? = null
    private var masterSiswaList: MutableList<SiswaAbsenItem> = mutableListOf()
    private var currentTanggalIso: String = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    private var namaKelasGlobal: String = ""
    private var isSortByNomorAbsen: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyEnterTransition()

        // Status Bar Transparan dengan Icon Gelap (Senada dengan Jadwal & Rekap Mengajar)
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode

        setContentView(R.layout.activity_wali_kelas_absen_harian)

        val rootLayout = findViewById<View>(R.id.main)
        ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
        loadProfilPicHeader()

        // Ambil nama kelas dari intent jika ada
        val namaKelasIntent = intent.getStringExtra("nama_kelas")
        if (!namaKelasIntent.isNullOrEmpty()) {
            namaKelasGlobal = namaKelasIntent
            tvKelasBadge.text = namaKelasIntent
            tvJudulPresensi.text = "Presensi Siswa $namaKelasIntent"
        }

        loadAbsenHarian(currentTanggalIso)
    }

    private fun initViews() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        ivProfilPhoto = findViewById(R.id.ivProfilPhoto)

        tvKelasBadge = findViewById(R.id.tvKelasBadge)
        tvTahunAjaran = findViewById(R.id.tvTahunAjaran)
        tvJudulPresensi = findViewById(R.id.tvJudulPresensi)
        tvWaliKelasNama = findViewById(R.id.tvWaliKelasNama)

        tvCountHadir = findViewById(R.id.tvCountHadir)
        tvCountSakit = findViewById(R.id.tvCountSakit)
        tvCountIzin = findViewById(R.id.tvCountIzin)
        tvCountAlpha = findViewById(R.id.tvCountAlpha)

        tvTanggalDipilih = findViewById(R.id.tvTanggalDipilih)
        btnUbahTanggal = findViewById(R.id.btnUbahTanggal)

        btnSetSemuaHadir = findViewById(R.id.btnSetSemuaHadir)
        btnSetSemuaSakit = findViewById(R.id.btnSetSemuaSakit)
        btnSetSemuaIzin = findViewById(R.id.btnSetSemuaIzin)
        btnSetSemuaAlpha = findViewById(R.id.btnSetSemuaAlpha)

        etCariSiswa = findViewById(R.id.etCariSiswa)
        tvDaftarSiswaHeader = findViewById(R.id.tvDaftarSiswaHeader)
        btnUrutkanSiswa = findViewById(R.id.btnUrutkanSiswa)

        rvDaftarSiswa = findViewById(R.id.rvDaftarSiswa)
        pbLoading = findViewById(R.id.pbLoading)
        tvEmptyState = findViewById(R.id.tvEmptyState)

        tvProgressAbsen = findViewById(R.id.tvProgressAbsen)
        tvBadgeStatusLengkap = findViewById(R.id.tvBadgeStatusLengkap)
        btnBatal = findViewById(R.id.btnBatal)
        btnSimpanKehadiran = findViewById(R.id.btnSimpanKehadiran)

        llMetricBoxes = findViewById(R.id.llMetricBoxes)
        llFastActionAndSearch = findViewById(R.id.llFastActionAndSearch)
        cvStateHariLibur = findViewById(R.id.cvStateHariLibur)
        tvKeteranganLibur = findViewById(R.id.tvKeteranganLibur)
        llHeaderDaftarSiswa = findViewById(R.id.llHeaderDaftarSiswa)
        bottomStickyBar = findViewById(R.id.bottomStickyBar)

        rvDaftarSiswa.layoutManager = LinearLayoutManager(this)
    }

    private fun setupListeners() {
        // Ubah Tanggal (DatePickerDialog)
        btnUbahTanggal.setOnClickListener {
            showDatePicker()
        }

        // Fast Actions: Set Semua
        btnSetSemuaHadir.setOnClickListener {
            adapter?.setAllStatus("Hadir")
            recalculateSummary()
            Toast.makeText(this, "Semua siswa diatur ke HADIR", Toast.LENGTH_SHORT).show()
        }

        btnSetSemuaSakit.setOnClickListener {
            adapter?.setAllStatus("Sakit")
            recalculateSummary()
            Toast.makeText(this, "Semua siswa diatur ke SAKIT", Toast.LENGTH_SHORT).show()
        }

        btnSetSemuaIzin.setOnClickListener {
            adapter?.setAllStatus("Izin")
            recalculateSummary()
            Toast.makeText(this, "Semua siswa diatur ke IZIN", Toast.LENGTH_SHORT).show()
        }

        btnSetSemuaAlpha.setOnClickListener {
            adapter?.setAllStatus("Alpha")
            recalculateSummary()
            Toast.makeText(this, "Semua siswa diatur ke ALPHA", Toast.LENGTH_SHORT).show()
        }

        // Live Search Filter
        etCariSiswa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter?.filter(s.toString())
                val count = adapter?.itemCount ?: 0
                tvEmptyState.visibility = if (count == 0 && masterSiswaList.isNotEmpty()) View.VISIBLE else View.GONE
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // Toggle Sort Nomor Absen vs Nama A-Z
        btnUrutkanSiswa.setOnClickListener {
            isSortByNomorAbsen = !isSortByNomorAbsen
            btnUrutkanSiswa.text = if (isSortByNomorAbsen) "Urut Nomor Absen ⇅" else "Urut Nama A-Z ⇅"
            adapter?.sortBy(isSortByNomorAbsen)
        }

        // Batal
        btnBatal.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Batal Absensi")
                .setMessage("Apakah Anda yakin ingin keluar? Perubahan yang belum disimpan akan hilang.")
                .setPositiveButton("Ya, Keluar") { _, _ -> finish() }
                .setNegativeButton("Tetap di Sini", null)
                .show()
        }

        // Simpan Kehadiran
        btnSimpanKehadiran.setOnClickListener {
            simpanAbsenHarian()
        }
    }

    private fun loadProfilPicHeader() {
        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val fotoProfilUrl = sharedPref.getString("foto_profil", null)
        if (!fotoProfilUrl.isNullOrEmpty() && ::ivProfilPhoto.isInitialized) {
            val fullUrl = if (fotoProfilUrl.startsWith("http")) fotoProfilUrl else "https://mariyadhuljannahsubang.sch.id/uploads/guru/$fotoProfilUrl"
            try {
                Glide.with(this)
                    .load(fullUrl)
                    .circleCrop()
                    .placeholder(R.drawable.logo_marj)
                    .error(R.drawable.logo_marj)
                    .into(ivProfilPhoto)
            } catch (e: Exception) {
                // Ignore Glide load errors
            }
        }
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(currentTanggalIso)
            if (date != null) calendar.time = date
        } catch (e: Exception) {
            // Ignore parse error
        }

        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val newCalendar = Calendar.getInstance()
                newCalendar.set(year, month, dayOfMonth)
                currentTanggalIso = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(newCalendar.time)
                loadAbsenHarian(currentTanggalIso)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePicker.show()
    }

    private fun loadAbsenHarian(tanggal: String) {
        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val idUser = sharedPref.getString("id_user", "") ?: ""

        if (idUser.isEmpty()) {
            Toast.makeText(this, "Sesi login tidak valid. Silakan login kembali.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        pbLoading.visibility = View.VISIBLE
        rvDaftarSiswa.visibility = View.GONE
        tvEmptyState.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getWaliKelasAbsenHarian(idUser, tanggal)
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.status == true) {
                        val result = response.body()?.data
                        if (result != null) {
                            // Update UI Header
                            val kelasNama = result.nama_kelas ?: namaKelasGlobal
                            namaKelasGlobal = kelasNama
                            tvKelasBadge.text = kelasNama
                            tvJudulPresensi.text = "Presensi Siswa $kelasNama"
                            tvTahunAjaran.text = "TA ${result.tahun_ajaran ?: "2024/2025"}"
                            tvWaliKelasNama.text = "Wali Kelas: ${result.wali_kelas ?: "-"}"
                            tvTanggalDipilih.text = result.tanggal_formatted ?: tanggal

                            val isLibur = result.is_libur == true
                            isCurrentHariLibur = isLibur

                            if (isLibur) {
                                // TAMPILAN KHUSUS HARI LIBUR:
                                // Data siswa tidak muncul, tombol simpan/bottom bar hilang total
                                cvStateHariLibur.visibility = View.VISIBLE
                                tvKeteranganLibur.text = result.keterangan_libur ?: "Libur"

                                llMetricBoxes.visibility = View.GONE
                                llFastActionAndSearch.visibility = View.GONE
                                llHeaderDaftarSiswa.visibility = View.GONE
                                rvDaftarSiswa.visibility = View.GONE
                                tvEmptyState.visibility = View.GONE
                                bottomStickyBar.visibility = View.GONE

                                masterSiswaList.clear()
                                adapter?.updateData(emptyList<SiswaAbsenItem>())
                            } else {
                                // TAMPILAN NORMAL HARI KERJA
                                cvStateHariLibur.visibility = View.GONE
                                llMetricBoxes.visibility = View.VISIBLE
                                llFastActionAndSearch.visibility = View.VISIBLE
                                llHeaderDaftarSiswa.visibility = View.VISIBLE
                                bottomStickyBar.visibility = View.VISIBLE

                                masterSiswaList = (result.siswa ?: emptyList()).toMutableList()

                                if (masterSiswaList.isEmpty()) {
                                    tvEmptyState.visibility = View.VISIBLE
                                    tvEmptyState.text = "Belum ada data siswa di kelas ini."
                                    rvDaftarSiswa.visibility = View.GONE
                                } else {
                                    rvDaftarSiswa.visibility = View.VISIBLE
                                    tvEmptyState.visibility = View.GONE

                                    adapter = SiswaAbsenHarianAdapter(masterSiswaList) { _, _ ->
                                        recalculateSummary()
                                    }
                                    rvDaftarSiswa.adapter = adapter
                                }

                                recalculateSummary()
                            }
                        }
                    } else {
                        val errMsg = response.body()?.message ?: "Gagal memuat data absensi (${response.code()})"
                        Toast.makeText(this@WaliKelasAbsenHarianActivity, errMsg, Toast.LENGTH_LONG).show()
                        tvEmptyState.visibility = View.VISIBLE
                        tvEmptyState.text = errMsg
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    tvEmptyState.visibility = View.VISIBLE
                    tvEmptyState.text = "Terjadi kesalahan: ${e.localizedMessage}"
                    Toast.makeText(this@WaliKelasAbsenHarianActivity, "Gagal koneksi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun recalculateSummary() {
        var hadir = 0
        var sakit = 0
        var izin = 0
        var alpha = 0

        for (item in masterSiswaList) {
            when (item.status_kehadiran) {
                "Hadir" -> hadir++
                "Sakit" -> sakit++
                "Izin" -> izin++
                "Alpha" -> alpha++
            }
        }

        tvCountHadir.text = hadir.toString()
        tvCountSakit.text = sakit.toString()
        tvCountIzin.text = izin.toString()
        tvCountAlpha.text = alpha.toString()

        val total = masterSiswaList.size
        tvDaftarSiswaHeader.text = "DAFTAR SISWA ($total)"

        tvProgressAbsen.text = "$total dari $total Siswa telah diabsen"
        tvBadgeStatusLengkap.text = if (total > 0) "100% Lengkap" else "0% Lengkap"
    }

    private fun simpanAbsenHarian() {
        if (isCurrentHariLibur) {
            Toast.makeText(this, "Hari ini libur, presensi tidak dapat disimpan.", Toast.LENGTH_SHORT).show()
            return
        }

        if (masterSiswaList.isEmpty()) {
            Toast.makeText(this, "Tidak ada data siswa untuk disimpan.", Toast.LENGTH_SHORT).show()
            return
        }

        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val idUser = sharedPref.getString("id_user", "") ?: ""

        btnSimpanKehadiran.isEnabled = false
        btnSimpanKehadiran.text = "Menyimpan..."

        val loadingDialog = AlertDialog.Builder(this)
            .setView(layoutInflater.inflate(R.layout.dialog_loading_lottie, null))
            .setCancelable(false)
            .create()
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        try {
            loadingDialog.show()
        } catch (e: Exception) {
            // Ignore dialog show exception
        }

        val itemsToSubmit = masterSiswaList.map {
            SiswaAbsenSubmitItem(
                siswa_id = it.id,
                status = it.status_kehadiran,
                keterangan = it.keterangan
            )
        }

        val request = SimpanAbsenHarianRequest(
            id_user = idUser,
            tanggal = currentTanggalIso,
            absensi = itemsToSubmit
        )

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.simpanWaliKelasAbsenHarian(request)
                withContext(Dispatchers.Main) {
                    try {
                        if (!isFinishing && !isDestroyed) loadingDialog.dismiss()
                    } catch (e: Exception) {}

                    btnSimpanKehadiran.isEnabled = true
                    btnSimpanKehadiran.text = "Simpan Kehadiran →"

                    if (response.isSuccessful && response.body()?.status == true) {
                        AlertDialog.Builder(this@WaliKelasAbsenHarianActivity)
                            .setTitle("✅ Presensi Tersimpan")
                            .setMessage("Data absensi harian kelas $namaKelasGlobal pada tanggal ${tvTanggalDipilih.text} berhasil disimpan ke database.")
                            .setPositiveButton("Selesai") { _, _ ->
                                finish()
                            }
                            .setCancelable(false)
                            .show()
                    } else {
                        val errMsg = response.body()?.message ?: "Gagal menyimpan presensi (${response.code()})"
                        Toast.makeText(this@WaliKelasAbsenHarianActivity, errMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    try {
                        if (!isFinishing && !isDestroyed) loadingDialog.dismiss()
                    } catch (e: Exception) {}

                    btnSimpanKehadiran.isEnabled = true
                    btnSimpanKehadiran.text = "Simpan Kehadiran →"
                    Toast.makeText(this@WaliKelasAbsenHarianActivity, "Gagal koneksi saat menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
