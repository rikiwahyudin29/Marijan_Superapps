package com.rtekmidev.marijancbt

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rtekmidev.marijancbt.api.ApiClient
import com.rtekmidev.marijancbt.api.DataTugas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DaftarTugasActivity : AppCompatActivity() {

    private lateinit var rvTugas: RecyclerView
    private lateinit var tabAktif: TextView
    private lateinit var tabSelesai: TextView
    private lateinit var tugasAdapter: TugasAdapter
    
    private var activeTasks: List<DataTugas> = emptyList()
    private var completedTasks: List<DataTugas> = emptyList()
    private var isTabAktif = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_daftar_tugas)
        
        // Setup Edge-to-Edge Transparent Status Bar
        @Suppress("DEPRECATION")
        window.statusBarColor = Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode
        
        val headerLayout = findViewById<View>(R.id.headerLayout)
        if (headerLayout != null) {
            ViewCompat.setOnApplyWindowInsetsListener(headerLayout) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(v.paddingLeft, systemBars.top, v.paddingRight, v.paddingBottom)
                insets
            }
        }

        // Setup Header with User Profile Data
        setupHeader()

        // Init Views
        rvTugas = findViewById(R.id.rvTugas)
        tabAktif = findViewById(R.id.tabAktif)
        tabSelesai = findViewById(R.id.tabSelesai)

        // Setup RecyclerView
        rvTugas.layoutManager = LinearLayoutManager(this)
        tugasAdapter = TugasAdapter(emptyList()) { task, aksi ->
            if (aksi == "Perbarui" || aksi == "Kerjakan") {
                val intent = Intent(this, KumpulTugasActivity::class.java)
                intent.putExtra("ID_TUGAS", task.id_tugas?.toString() ?: "")
                intent.putExtra("MAPEL", task.mapel ?: "")
                intent.putExtra("JUDUL", task.judul ?: "")
                intent.putExtra("DEADLINE", task.deadline ?: "")
                intent.putExtra("DESKRIPSI", task.deskripsi ?: "")
                intent.putExtra("FILE_PENDUKUNG", task.file_pendukung ?: "")
                startActivity(intent)
            } else if (aksi == "LihatJawaban") {
                if (!task.file_jawaban.isNullOrEmpty()) {
                    val intent = Intent(this, FileViewerActivity::class.java)
                    intent.putExtra("FILE_URL", task.file_jawaban)
                    intent.putExtra("NAMA_FILE", "Jawaban_Tugas_${task.id_tugas}")
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Tidak ada file jawaban", Toast.LENGTH_SHORT).show()
                }
            }
        }
        rvTugas.adapter = tugasAdapter

        // Fetch Data
        fetchTugas()

        // Tab Listeners
        tabAktif.setOnClickListener {
            isTabAktif = true
            updateTabStyling()
            tugasAdapter.updateData(activeTasks)
        }

        tabSelesai.setOnClickListener {
            isTabAktif = false
            updateTabStyling()
            tugasAdapter.updateData(completedTasks)
        }
    }

    private fun setupHeader() {
        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val namaSiswa = sharedPref.getString("nama_siswa", "Siswa")
        val kelasLengkap = sharedPref.getString("kelas_lengkap", "Memuat Kelas...")
        val fotoProfil = sharedPref.getString("foto_profil", "")

        val tvNama = findViewById<TextView>(R.id.tvNamaDashboard)
        val tvKelas = findViewById<TextView>(R.id.tvKelas)
        val ivProfil = findViewById<ImageView>(R.id.ivProfilPhoto)

        tvNama?.text = "Selamat Datang, $namaSiswa!"
        tvKelas?.text = kelasLengkap

        if (!fotoProfil.isNullOrEmpty() && ivProfil != null) {
            Glide.with(this)
                .load(fotoProfil)
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .error(android.R.drawable.ic_menu_myplaces)
                .circleCrop()
                .into(ivProfil)
        }
    }

    private fun fetchTugas() {
        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val nisn = sharedPref.getString("nisn", "") ?: ""

        if (nisn.isEmpty()) return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getTugas(nisn)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == true) {
                        val allData = response.body()?.data
                        
                        // Extract lists directly from API response
                        activeTasks = allData?.berlangsung ?: emptyList()
                        completedTasks = allData?.selesai ?: emptyList()

                        // Update Tab Titles
                        tabAktif.text = "Sedang Berlangsung (${activeTasks.size})"
                        tabSelesai.text = "Selesai (${completedTasks.size})"

                        // Load initial data
                        tugasAdapter.updateData(if (isTabAktif) activeTasks else completedTasks)
                    } else {
                        Toast.makeText(this@DaftarTugasActivity, "Gagal memuat tugas", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DaftarTugasActivity, "Koneksi Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun updateTabStyling() {
        val activeTextColor = ContextCompat.getColor(this, R.color.text_blue)
        val inactiveTextColor = ContextCompat.getColor(this, R.color.text_secondary)

        if (isTabAktif) {
            tabAktif.setBackgroundResource(R.drawable.bg_tab_selected)
            tabAktif.setTextColor(activeTextColor)
            tabAktif.setTypeface(null, android.graphics.Typeface.BOLD)
            
            tabSelesai.setBackgroundResource(android.R.color.transparent)
            tabSelesai.setTextColor(inactiveTextColor)
            tabSelesai.setTypeface(null, android.graphics.Typeface.NORMAL)
        } else {
            tabSelesai.setBackgroundResource(R.drawable.bg_tab_selected)
            tabSelesai.setTextColor(activeTextColor)
            tabSelesai.setTypeface(null, android.graphics.Typeface.BOLD)
            
            tabAktif.setBackgroundResource(android.R.color.transparent)
            tabAktif.setTextColor(inactiveTextColor)
            tabAktif.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
    }
}
