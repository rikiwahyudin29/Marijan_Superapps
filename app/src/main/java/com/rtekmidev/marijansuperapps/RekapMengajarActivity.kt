package com.rtekmidev.marijansuperapps

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.marijansuperapps.api.ApiClient
import com.rtekmidev.marijansuperapps.api.RekapJurnalItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class RekapMengajarActivity : AppCompatActivity() {

    private lateinit var rvRekapJurnal: RecyclerView
    private lateinit var adapter: RekapMengajarAdapter
    private lateinit var pbLoading: View
    private lateinit var tvEmpty: TextView
    private lateinit var tvSemesterInfo: TextView
    private lateinit var tvSelectedMonth: TextView
    private lateinit var tvJurnalTerisi: TextView
    private lateinit var tvJurnalTotal1: TextView
    private lateinit var pbJurnalTerisi: ProgressBar
    private lateinit var tvPerluDiisi: TextView
    private lateinit var pbPerluDiisi: ProgressBar
    private lateinit var tvRataPresensi: TextView
    private lateinit var tvRataPresensiStatus: TextView
    
    private lateinit var chipSemua: TextView
    private lateinit var chipSudahDiisi: TextView
    private lateinit var chipBelumDiisi: TextView
    
    private var currentCalendar = Calendar.getInstance()
    private var allItems = listOf<RekapJurnalItem>()
    private var currentFilter = "Semua"
    private var isDataNeedsRefresh = false
    private var loadJob: kotlinx.coroutines.Job? = null

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

        setContentView(R.layout.activity_rekap_mengajar)
        
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        initViews()
        setupListeners()
        setupRecyclerView()
        loadData()
    }
    
    override fun onResume() {
        super.onResume()
        if (isDataNeedsRefresh) {
            isDataNeedsRefresh = false
            loadData()
        }
    }

    private fun initViews() {
        rvRekapJurnal = findViewById(R.id.rvRekapJurnal)
        pbLoading = findViewById(R.id.pbLoading)
        tvEmpty = findViewById(R.id.tvEmpty)
        tvSemesterInfo = findViewById(R.id.tvSemesterInfo)
        tvSelectedMonth = findViewById(R.id.tvSelectedMonth)
        tvJurnalTerisi = findViewById(R.id.tvJurnalTerisi)
        tvJurnalTotal1 = findViewById(R.id.tvJurnalTotal1)
        pbJurnalTerisi = findViewById(R.id.pbJurnalTerisi)
        tvPerluDiisi = findViewById(R.id.tvPerluDiisi)
        pbPerluDiisi = findViewById(R.id.pbPerluDiisi)
        tvRataPresensi = findViewById(R.id.tvRataPresensi)
        tvRataPresensiStatus = findViewById(R.id.tvRataPresensiStatus)
        chipSemua = findViewById(R.id.chipSemua)
        chipSudahDiisi = findViewById(R.id.chipSudahDiisi)
        chipBelumDiisi = findViewById(R.id.chipBelumDiisi)
        
        updateMonthText()
        loadProfilePhoto()
    }

    private fun loadProfilePhoto() {
        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val fotoProfilUrl = sharedPref.getString("foto_profil", null)
        val ivProfilPhoto = findViewById<ImageView>(R.id.ivProfilPhoto)
        if (!fotoProfilUrl.isNullOrEmpty() && ivProfilPhoto != null) {
            val fullUrl = if (fotoProfilUrl.startsWith("http")) fotoProfilUrl else "https://mariyadhuljannahsubang.sch.id/uploads/guru/$fotoProfilUrl"
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
    }

    private fun setupListeners() {
        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }
        
        findViewById<ImageView>(R.id.btnPrevMonth).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, -1)
            updateMonthText()
            loadData()
        }
        
        findViewById<ImageView>(R.id.btnNextMonth).setOnClickListener {
            currentCalendar.add(Calendar.MONTH, 1)
            updateMonthText()
            loadData()
        }
        
        chipSemua.setOnClickListener { setFilter("Semua") }
        chipSudahDiisi.setOnClickListener { setFilter("Sudah Diisi") }
        chipBelumDiisi.setOnClickListener { setFilter("Belum Diisi") }
    }

    private fun setupRecyclerView() {
        adapter = RekapMengajarAdapter(this, emptyList()) { item ->
            if (item.status == "Belum Diisi") {
                val intent = Intent(this, JurnalMengajarActivity::class.java)
                intent.putExtra("id_kelas", item.id_kelas ?: "")
                intent.putExtra("id_mapel", item.id_mapel ?: "")
                intent.putExtra("nama_kelas", item.nama_kelas ?: "")
                intent.putExtra("nama_mapel", item.nama_mapel ?: "")

                val jamKeVal = if (!item.jam_ke.isNullOrEmpty()) {
                    item.jam_ke
                } else {
                    val jmMulai = item.jam_mulai?.substring(0, 5) ?: ""
                    val jmSelesai = item.jam_selesai?.substring(0, 5) ?: ""
                    if (jmMulai.isNotEmpty() && jmSelesai.isNotEmpty()) "$jmMulai - $jmSelesai WIB" else ""
                }
                intent.putExtra("jam_ke", jamKeVal)
                intent.putExtra("tanggal", item.tanggal ?: "")

                // Fallback for uppercase keys
                intent.putExtra("ID_KELAS", item.id_kelas ?: "")
                intent.putExtra("ID_MAPEL", item.id_mapel ?: "")
                intent.putExtra("JAM_KE", jamKeVal)
                intent.putExtra("TANGGAL", item.tanggal ?: "")
                isDataNeedsRefresh = true
                startActivity(intent)
                applyEnterTransition()
            } else if (item.status == "Terisi") {
                Toast.makeText(this, "Jurnal untuk jadwal ini sudah diisi.", Toast.LENGTH_SHORT).show()
            }
        }
        rvRekapJurnal.layoutManager = LinearLayoutManager(this)
        rvRekapJurnal.setHasFixedSize(false)
        rvRekapJurnal.adapter = adapter
    }

    private fun updateMonthText() {
        val format = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        tvSelectedMonth.text = format.format(currentCalendar.time)
    }

    private fun setFilter(filter: String) {
        currentFilter = filter
        
        // Update chip UI
        val activeBg = R.drawable.bg_rounded_border
        val inactiveBg = R.drawable.bg_rounded_border_outline
        val activeColor = Color.parseColor("#FFFFFF")
        val activeBgTint = android.content.res.ColorStateList.valueOf(Color.parseColor("#1E1B4B"))
        val inactiveColor = Color.parseColor("#64748B")
        val inactiveBgTint = null

        chipSemua.apply {
            setBackgroundResource(if (filter == "Semua") activeBg else inactiveBg)
            setTextColor(if (filter == "Semua") activeColor else inactiveColor)
            backgroundTintList = if (filter == "Semua") activeBgTint else inactiveBgTint
        }
        chipSudahDiisi.apply {
            setBackgroundResource(if (filter == "Sudah Diisi") activeBg else inactiveBg)
            setTextColor(if (filter == "Sudah Diisi") activeColor else inactiveColor)
            backgroundTintList = if (filter == "Sudah Diisi") activeBgTint else inactiveBgTint
        }
        chipBelumDiisi.apply {
            setBackgroundResource(if (filter == "Belum Diisi") activeBg else inactiveBg)
            setTextColor(if (filter == "Belum Diisi") activeColor else inactiveColor)
            backgroundTintList = if (filter == "Belum Diisi") activeBgTint else inactiveBgTint
        }

        applyFilter()
    }

    private fun applyFilter() {
        val filteredList = when (currentFilter) {
            "Sudah Diisi" -> allItems.filter { it.status == "Terisi" }
            "Belum Diisi" -> allItems.filter { it.status == "Belum Diisi" }
            else -> allItems
        }
        
        adapter.updateData(filteredList)
        
        if (filteredList.isEmpty()) {
            tvEmpty.visibility = View.VISIBLE
            rvRekapJurnal.visibility = View.GONE
        } else {
            tvEmpty.visibility = View.GONE
            rvRekapJurnal.visibility = View.VISIBLE
        }
    }

    private fun loadData() {
        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val token = sharedPref.getString("token", "") ?: ""
        ApiClient.authToken = token
        
        val formatAPI = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val bulanStr = formatAPI.format(currentCalendar.time)

        pbLoading.visibility = View.VISIBLE
        rvRekapJurnal.visibility = View.GONE
        tvEmpty.visibility = View.GONE

        loadJob?.cancel()
        loadJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getRekapJurnal(bulanStr)
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.status == true && body.data != null) {
                            val data = body.data
                            tvSemesterInfo.text = data.semester_info ?: "Semester Info"
                            
                            val sum = data.summary
                            if (sum != null) {
                                tvJurnalTerisi.text = sum.terisi.toString()
                                tvJurnalTotal1.text = " / ${sum.total_sesi} Sesi"
                                pbJurnalTerisi.max = sum.total_sesi
                                pbJurnalTerisi.progress = sum.terisi
                                
                                tvPerluDiisi.text = sum.perlu_diisi.toString()
                                pbPerluDiisi.max = sum.total_sesi
                                pbPerluDiisi.progress = sum.perlu_diisi
                                
                                val presensiFormatted = if (sum.rata_presensi % 1.0 == 0.0) {
                                    sum.rata_presensi.toInt().toString()
                                } else {
                                    String.format(Locale.US, "%.1f", sum.rata_presensi)
                                }
                                tvRataPresensi.text = "$presensiFormatted% Hadir"
                                
                                if (sum.terisi == 0 && sum.rata_presensi == 0.0) {
                                    tvRataPresensiStatus.text = "Belum Ada Data"
                                    tvRataPresensiStatus.setTextColor(Color.parseColor("#64748B"))
                                    tvRataPresensiStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
                                } else if (sum.rata_presensi >= 90.0) {
                                    tvRataPresensiStatus.text = "Sangat Baik"
                                    tvRataPresensiStatus.setTextColor(Color.parseColor("#0d9488"))
                                    tvRataPresensiStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#ccfbf1"))
                                } else if (sum.rata_presensi >= 75.0) {
                                    tvRataPresensiStatus.text = "Baik"
                                    tvRataPresensiStatus.setTextColor(Color.parseColor("#b45309"))
                                    tvRataPresensiStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#fef3c7"))
                                } else {
                                    tvRataPresensiStatus.text = "Perlu Perhatian"
                                    tvRataPresensiStatus.setTextColor(Color.parseColor("#ef4444"))
                                    tvRataPresensiStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#fee2e2"))
                                }
                            }
                            
                            allItems = data.list ?: emptyList()
                            
                            // Update chips count
                            val semua = allItems.size
                            val terisi = allItems.count { it.status == "Terisi" }
                            val belum = allItems.count { it.status == "Belum Diisi" }
                            
                            chipSemua.text = "Semua ($semua)"
                            chipSudahDiisi.text = "Sudah Diisi ($terisi)"
                            chipBelumDiisi.text = "Belum Diisi ($belum)"
                            
                            applyFilter()
                            return@withContext
                        }
                    }
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Gagal memuat data rekap. Error: ${response.code()} ${response.message()}"
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    tvEmpty.visibility = View.VISIBLE
                    tvEmpty.text = "Terjadi kesalahan koneksi. ${e.localizedMessage}"
                }
            }
        }
    }
    
    override fun onDestroy() {
        loadJob?.cancel()
        super.onDestroy()
    }

    override fun finish() {
        super.finish()
        applyExitTransition()
    }
}
