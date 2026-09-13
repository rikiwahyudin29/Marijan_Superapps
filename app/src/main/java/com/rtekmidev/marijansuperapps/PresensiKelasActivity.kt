package com.rtekmidev.marijansuperapps

import android.content.Context
import android.os.Bundle
import com.bumptech.glide.Glide
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.marijansuperapps.api.ApiClient
import com.rtekmidev.marijansuperapps.api.DataAbsenItem
import com.rtekmidev.marijansuperapps.api.SubmitAbsenJurnalRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@android.annotation.SuppressLint("SetTextI18n")
class PresensiKelasActivity : AppCompatActivity() {

    private lateinit var rvSiswa: RecyclerView
    private lateinit var adapter: PresensiSiswaAdapter
    private lateinit var progressBar: View
    
    // Header & Banner
    private lateinit var btnBack: ImageView
    private lateinit var tvKelasBadge: TextView
    private lateinit var tvMapelBadge: TextView
    private lateinit var tvJamKeBadge: TextView
    private lateinit var tvMateri: TextView
    private lateinit var tvTanggal: TextView
    private lateinit var tvTotalSiswa: TextView
    
    // Summaries
    private lateinit var tvSummaryHadir: TextView
    private lateinit var tvSummarySakit: TextView
    private lateinit var tvSummaryIzin: TextView
    private lateinit var tvSummaryAlpa: TextView
    
    // Bulk Actions
    private lateinit var btnSetHadir: Button
    private lateinit var btnSetSakit: Button
    private lateinit var btnSetIzin: Button
    private lateinit var btnSetAlpa: Button
    
    // Search & Progress
    private lateinit var etSearchSiswa: EditText
    private lateinit var tvTitleDaftarSiswa: TextView
    private lateinit var tvProgressAbsen: TextView
    private lateinit var tvProgressBadge: TextView
    
    // Bottom Buttons
    private lateinit var btnBatal: Button
    private lateinit var btnSimpan: Button

    private var idJurnal: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        applyEnterTransition()
        
        // Transparent Status Bar
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode

        setContentView(R.layout.activity_presensi_kelas)

        // Handle insets for root view (samakan dengan RekapMengajar)
        val rootLayout = findViewById<View>(R.id.main)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val prefGuru = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val fotoProfilUrl = prefGuru.getString("foto_profil", null)
        val ivProfilPhoto = findViewById<ImageView>(R.id.ivProfilPhoto)
        if (!fotoProfilUrl.isNullOrEmpty() && ivProfilPhoto != null) {
            val fullUrl = if (fotoProfilUrl.startsWith("http")) fotoProfilUrl else "https://smkriyadhuljannahjalancagak.sch.id/uploads/guru/$fotoProfilUrl"
            try {
                Glide.with(this)
                    .load(fullUrl)
                    .placeholder(android.R.drawable.ic_menu_myplaces)
                    .error(android.R.drawable.ic_menu_myplaces)
                    .circleCrop()
                    .into(ivProfilPhoto)
            } catch (e: Exception) {
                // Ignore glide errors
            }
        }

        idJurnal = intent.getIntExtra("id_jurnal", 0)
        val namaKelas = intent.getStringExtra("nama_kelas") ?: "-"
        val namaMapel = intent.getStringExtra("nama_mapel") ?: "-"
        val jamKe = intent.getStringExtra("jam_ke") ?: "-"

        initViews()

        tvKelasBadge.text = namaKelas
        tvMapelBadge.text = namaMapel
        tvJamKeBadge.text = "Jam: $jamKe"
        val materi = intent.getStringExtra("materi")
        tvMateri.text = if (!materi.isNullOrEmpty()) materi else namaMapel
        
        val intentTanggal = intent.getStringExtra("tanggal")
        val dateToDisplay = if (!intentTanggal.isNullOrEmpty()) {
            try {
                SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(intentTanggal) ?: Date()
            } catch (e: Exception) {
                Date()
            }
        } else {
            Date()
        }
        val sdf = SimpleDateFormat("EEEE, d MMMM yyyy", Locale("id", "ID"))
        tvTanggal.text = sdf.format(dateToDisplay)

        adapter = PresensiSiswaAdapter(emptyList()) {
            updateSummary()
        }
        rvSiswa.layoutManager = LinearLayoutManager(this)
        rvSiswa.adapter = adapter

        btnBack.setOnClickListener { finish() }
        btnBatal.setOnClickListener { finish() }
        
        btnSetHadir.setOnClickListener { adapter.setSemuaStatus("H") }
        btnSetSakit.setOnClickListener { adapter.setSemuaStatus("S") }
        btnSetIzin.setOnClickListener { adapter.setSemuaStatus("I") }
        btnSetAlpa.setOnClickListener { adapter.setSemuaStatus("A") }

        etSearchSiswa.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                adapter.filter(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnSimpan.setOnClickListener {
            submitAbsen()
        }

        loadData()
    }
    
    private fun initViews() {
        rvSiswa = findViewById(R.id.rvSiswa)
        progressBar = findViewById(R.id.progressBar)
        btnBack = findViewById(R.id.btnBack)
        tvKelasBadge = findViewById(R.id.tvKelasBadge)
        tvMapelBadge = findViewById(R.id.tvMapelBadge)
        tvJamKeBadge = findViewById(R.id.tvJamKeBadge)
        tvMateri = findViewById(R.id.tvMateri)
        tvTanggal = findViewById(R.id.tvTanggal)
        tvTotalSiswa = findViewById(R.id.tvTotalSiswa)
        
        tvSummaryHadir = findViewById(R.id.tvSummaryHadir)
        tvSummarySakit = findViewById(R.id.tvSummarySakit)
        tvSummaryIzin = findViewById(R.id.tvSummaryIzin)
        tvSummaryAlpa = findViewById(R.id.tvSummaryAlpa)
        
        btnSetHadir = findViewById(R.id.btnSetHadir)
        btnSetSakit = findViewById(R.id.btnSetSakit)
        btnSetIzin = findViewById(R.id.btnSetIzin)
        btnSetAlpa = findViewById(R.id.btnSetAlpa)
        
        etSearchSiswa = findViewById(R.id.etSearchSiswa)
        tvTitleDaftarSiswa = findViewById(R.id.tvTitleDaftarSiswa)
        tvProgressAbsen = findViewById(R.id.tvProgressAbsen)
        tvProgressBadge = findViewById(R.id.tvProgressBadge)
        
        btnBatal = findViewById(R.id.btnBatal)
        btnSimpan = findViewById(R.id.btnSimpan)
    }

    private fun loadData() {
        progressBar.visibility = View.VISIBLE
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.getSiswaJurnal(idJurnal)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.status == true) {
                        val list = response.body()?.data ?: emptyList()
                        adapter.updateData(list)
                        tvTotalSiswa.text = list.size.toString()
                        tvTitleDaftarSiswa.text = "DAFTAR SISWA (${list.size})"
                        updateSummary()
                    } else {
                        Toast.makeText(this@PresensiKelasActivity, "Gagal memuat data", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@PresensiKelasActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateSummary() {
        val list = adapter.getData()
        if (list.isEmpty()) return
        
        var hadir = 0
        var sakit = 0
        var izin = 0
        var alpa = 0
        
        for (siswa in list) {
            when (siswa.status_absen?.uppercase(Locale.ROOT)) {
                "H" -> hadir++
                "S" -> sakit++
                "I" -> izin++
                "A" -> alpa++
            }
        }
        
        tvSummaryHadir.text = "$hadir Hadir"
        tvSummarySakit.text = "$sakit Sakit"
        tvSummaryIzin.text = "$izin Izin"
        tvSummaryAlpa.text = "$alpa Alpa"
        
        val totalDiabsen = hadir + sakit + izin + alpa
        val totalSiswa = list.size
        
        tvProgressAbsen.text = "$totalDiabsen dari $totalSiswa Siswa telah diabsen"
        
        val percentage = if (totalSiswa > 0) (totalDiabsen * 100) / totalSiswa else 0
        tvProgressBadge.text = "$percentage% Lengkap"
        
        if (percentage == 100) {
            tvProgressBadge.setBackgroundResource(R.drawable.bg_badge_green_dark)
            tvProgressBadge.setTextColor(android.graphics.Color.parseColor("#047857"))
        } else {
            tvProgressBadge.setBackgroundResource(R.drawable.bg_badge_yellow_outline)
            tvProgressBadge.setTextColor(android.graphics.Color.parseColor("#B45309"))
        }
    }

    private fun submitAbsen() {
        val list = adapter.getData()
        val absenData = list.map {
            DataAbsenItem(
                id_siswa = it.id,
                status = it.status_absen ?: "H" // Default Hadir jika kosong saat submit
            )
        }

        progressBar.visibility = View.VISIBLE
        btnSimpan.isEnabled = false

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val req = SubmitAbsenJurnalRequest(
                    id_jurnal = idJurnal,
                    data_absen = absenData
                )
                val response = ApiClient.instance.submitAbsenJurnal(req)
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnSimpan.isEnabled = true
                    if (response.isSuccessful && response.body()?.status == true) {
                        Toast.makeText(this@PresensiKelasActivity, "Presensi berhasil disimpan!", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this@PresensiKelasActivity, "Gagal: ${response.body()?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    btnSimpan.isEnabled = true
                    Toast.makeText(this@PresensiKelasActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        applyExitTransition()
    }
}
