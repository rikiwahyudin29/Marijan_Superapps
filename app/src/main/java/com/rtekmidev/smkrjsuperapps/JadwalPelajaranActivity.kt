package com.rtekmidev.smkrjsuperapps

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.JadwalPelajaranItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class JadwalPelajaranActivity : AppCompatActivity() {

    private lateinit var btnBack: ImageView
    private lateinit var tvBadgeKelas: TextView
    private lateinit var tvWaliKelasJadwal: TextView
    private lateinit var pbLoading: ProgressBar
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var containerJadwalHari: LinearLayout

    private val dayChips = mutableMapOf<String, TextView>()
    private var selectedDay: String = "Senin"
    private var jadwalPerHari: Map<String, List<JadwalPelajaranItem>> = emptyMap()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jadwal_pelajaran)

        initViews()
        setupDayChips()
        determineToday()

        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val nisn = sharedPref.getString("nisn", "") ?: ""

        if (nisn.isNotEmpty()) {
            loadJadwal(nisn)
        } else {
            Toast.makeText(this, "Sesi siswa tidak valid", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun initViews() {
        btnBack = findViewById(R.id.btnBack)
        tvBadgeKelas = findViewById(R.id.tvBadgeKelas)
        tvWaliKelasJadwal = findViewById(R.id.tvWaliKelasJadwal)
        pbLoading = findViewById(R.id.pbLoading)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        containerJadwalHari = findViewById(R.id.containerJadwalHari)

        findViewById<TextView>(R.id.tvHeaderTitle)?.text = "Jadwal Pelajaran"
        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val namaSiswa = sharedPref.getString("nama_siswa", "Siswa")
        val fotoProfil = sharedPref.getString("foto_profil", "")
        val ivProfilPhoto = findViewById<ImageView>(R.id.ivProfilPhoto)
        val tvProfilInisial = findViewById<TextView>(R.id.tvProfilInisial)
        val cvProfilPic = findViewById<androidx.cardview.widget.CardView>(R.id.cvProfilPic)
        com.rtekmidev.smkrjsuperapps.util.AvatarHelper.setAvatar(this, namaSiswa, fotoProfil, ivProfilPhoto, tvProfilInisial, cvProfilPic)

        btnBack.setOnClickListener { finish() }

        dayChips["Senin"] = findViewById(R.id.chipSenin)
        dayChips["Selasa"] = findViewById(R.id.chipSelasa)
        dayChips["Rabu"] = findViewById(R.id.chipRabu)
        dayChips["Kamis"] = findViewById(R.id.chipKamis)
        dayChips["Jumat"] = findViewById(R.id.chipJumat)
        dayChips["Sabtu"] = findViewById(R.id.chipSabtu)
    }

    private fun setupDayChips() {
        for ((hari, chip) in dayChips) {
            chip.setOnClickListener {
                selectDay(hari)
            }
        }
    }

    private fun determineToday() {
        val calendar = Calendar.getInstance()
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)
        selectedDay = when (dayOfWeek) {
            Calendar.MONDAY -> "Senin"
            Calendar.TUESDAY -> "Selasa"
            Calendar.WEDNESDAY -> "Rabu"
            Calendar.THURSDAY -> "Kamis"
            Calendar.FRIDAY -> "Jumat"
            Calendar.SATURDAY -> "Sabtu"
            else -> "Senin"
        }
        highlightDayChip(selectedDay)
    }

    private fun selectDay(hari: String) {
        selectedDay = hari
        highlightDayChip(hari)
        renderJadwalHari(hari)
    }

    private fun highlightDayChip(activeHari: String) {
        for ((hari, chip) in dayChips) {
            if (hari == activeHari) {
                chip.setBackgroundResource(R.drawable.bg_rounded_border)
                chip.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#2563EB"))
                chip.setTextColor(Color.parseColor("#FFFFFF"))
            } else {
                chip.setBackgroundResource(R.drawable.bg_rounded_border)
                chip.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
                chip.setTextColor(Color.parseColor("#64748B"))
            }
        }
    }

    private fun loadJadwal(nisn: String) {
        pbLoading.visibility = View.VISIBLE
        containerJadwalHari.visibility = View.GONE
        layoutEmptyState.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getJadwalPelajaran(nisn)
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    if (response.isSuccessful && response.body()?.status == true && response.body()?.data != null) {
                        val data = response.body()?.data!!
                        tvBadgeKelas.text = "● Kelas ${data.kelas ?: "-"}"
                        val wali = if (!data.wali_kelas.isNullOrEmpty()) "Wali Kelas : ${data.wali_kelas}" else "Wali Kelas : Belum Ditentukan"
                        tvWaliKelasJadwal.text = wali

                        if (!data.hari_ini.isNullOrEmpty() && dayChips.containsKey(data.hari_ini)) {
                            selectedDay = data.hari_ini
                            highlightDayChip(selectedDay)
                        }

                        jadwalPerHari = data.jadwal ?: emptyMap()
                        renderJadwalHari(selectedDay)
                    } else {
                        Toast.makeText(this@JadwalPelajaranActivity, "Gagal memuat jadwal pelajaran", Toast.LENGTH_SHORT).show()
                        renderJadwalHari(selectedDay)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    Toast.makeText(this@JadwalPelajaranActivity, "Koneksi Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    renderJadwalHari(selectedDay)
                }
            }
        }
    }

    private fun renderJadwalHari(hari: String) {
        containerJadwalHari.removeAllViews()
        val list = jadwalPerHari[hari] ?: emptyList()

        if (list.isEmpty()) {
            containerJadwalHari.visibility = View.GONE
            layoutEmptyState.visibility = View.VISIBLE
        } else {
            layoutEmptyState.visibility = View.GONE
            containerJadwalHari.visibility = View.VISIBLE

            val inflater = LayoutInflater.from(this)
            for (item in list) {
                val cardView = inflater.inflate(R.layout.item_jadwal_siswa_card, containerJadwalHari, false)

                val tvWaktu = cardView.findViewById<TextView>(R.id.tvJadwalWaktu)
                val tvStatus = cardView.findViewById<TextView>(R.id.tvJadwalStatus)
                val tvMapel = cardView.findViewById<TextView>(R.id.tvJadwalMapel)
                val tvGuru = cardView.findViewById<TextView>(R.id.tvJadwalGuru)
                val tvRuang = cardView.findViewById<TextView>(R.id.tvJadwalRuang)

                tvWaktu.text = item.waktu ?: "${item.jam_mulai} - ${item.jam_selesai} WIB"
                tvMapel.text = item.nama_mapel ?: "Mata Pelajaran"
                tvGuru.text = "Guru: ${item.nama_guru ?: "-"}"
                tvRuang.text = "Ruang: ${item.ruang ?: "Ruang Kelas"}"

                if (item.is_active == true) {
                    tvStatus.visibility = View.VISIBLE
                    tvStatus.text = "● Sedang Berlangsung"
                    tvStatus.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                    tvStatus.setTextColor(Color.parseColor("#15803D"))
                } else {
                    tvStatus.visibility = View.GONE
                }

                containerJadwalHari.addView(cardView)
            }
        }
    }
}
