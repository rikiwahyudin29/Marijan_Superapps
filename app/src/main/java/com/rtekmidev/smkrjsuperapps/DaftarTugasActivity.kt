package com.rtekmidev.smkrjsuperapps

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
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.DataTugas
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
        com.rtekmidev.smkrjsuperapps.util.StatusBarHelper.setupTranslucentBar(this)

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
                val cleanDesc = androidx.core.text.HtmlCompat.fromHtml(task.deskripsi ?: "", androidx.core.text.HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
                intent.putExtra("DESKRIPSI", cleanDesc)
                intent.putExtra("FILE_PENDUKUNG", task.file_pendukung ?: "")
                intent.putExtra("IS_PERBARUI", aksi == "Perbarui")
                startActivity(intent)
            } else if (aksi == "LihatJawaban") {
                if (!task.file_jawaban.isNullOrEmpty()) {
                    val intent = Intent(this, FileViewerActivity::class.java)
                    intent.putExtra("FILE_URL", task.file_jawaban)
                    intent.putExtra("TITLE", "Jawaban: ${task.judul}")
                    startActivity(intent)
                } else {
                    Toast.makeText(this, "Tidak ada file jawaban", Toast.LENGTH_SHORT).show()
                }
            }
        }
        rvTugas.adapter = tugasAdapter

        // Fetch Data Awal
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

    override fun onResume() {
        super.onResume()
        // Muat ulang daftar tugas setiap kali kembali ke halaman ini
        fetchTugas()
    }

    private fun setupHeader() {
        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val namaSiswa = sharedPref.getString("nama_siswa", "Siswa")
        val fotoProfil = sharedPref.getString("foto_profil", "")

        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Daftar Tugas"
        findViewById<ImageView>(R.id.btnBack)?.setOnClickListener { finish() }

        val ivProfil = findViewById<ImageView>(R.id.ivProfilPhoto)
        val tvProfilInisial = findViewById<TextView>(R.id.tvProfilInisial)
        val cvProfilPic = findViewById<androidx.cardview.widget.CardView>(R.id.cvProfilPic)
        com.rtekmidev.smkrjsuperapps.util.AvatarHelper.setAvatar(this, namaSiswa, fotoProfil, ivProfil, tvProfilInisial, cvProfilPic)
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

                        // Jika tab aktif kosong tapi ada tugas yang sudah selesai, otomatis beralih ke tab selesai
                        if (isTabAktif && activeTasks.isEmpty() && completedTasks.isNotEmpty()) {
                            isTabAktif = false
                            updateTabStyling()
                        }

                        // Load data
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
