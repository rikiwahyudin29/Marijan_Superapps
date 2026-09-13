package com.rtekmidev.marijansuperapps

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.lifecycle.lifecycleScope
import com.rtekmidev.marijansuperapps.api.ApiClient
import com.rtekmidev.marijansuperapps.api.JadwalPelajaranItem
import com.rtekmidev.marijansuperapps.util.AvatarHelper
import com.rtekmidev.marijansuperapps.util.StatusBarHelper
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

    data class MapelTheme(
        val primaryColor: Int,
        val lightBgColor: Int,
        val strokeColor: Int,
        val darkTextColor: Int,
        val defaultIconRes: Int
    )

    private val themeIndigo = MapelTheme(
        primaryColor = Color.parseColor("#4F46E5"),
        lightBgColor = Color.parseColor("#EEF2FF"),
        strokeColor = Color.parseColor("#C7D2FE"),
        darkTextColor = Color.parseColor("#3730A3"),
        defaultIconRes = R.drawable.ic_subject_tech
    )

    private val themeEmerald = MapelTheme(
        primaryColor = Color.parseColor("#059669"),
        lightBgColor = Color.parseColor("#ECFDF5"),
        strokeColor = Color.parseColor("#A7F3D0"),
        darkTextColor = Color.parseColor("#065F46"),
        defaultIconRes = R.drawable.ic_subject_islamic
    )

    private val themeRose = MapelTheme(
        primaryColor = Color.parseColor("#E11D48"),
        lightBgColor = Color.parseColor("#FFF1F2"),
        strokeColor = Color.parseColor("#FECDD3"),
        darkTextColor = Color.parseColor("#9F1239"),
        defaultIconRes = R.drawable.ic_subject_sport
    )

    private val themeAmber = MapelTheme(
        primaryColor = Color.parseColor("#D97706"),
        lightBgColor = Color.parseColor("#FEF3C7"),
        strokeColor = Color.parseColor("#FDE68A"),
        darkTextColor = Color.parseColor("#92400E"),
        defaultIconRes = R.drawable.ic_subject_book
    )

    private val themePurple = MapelTheme(
        primaryColor = Color.parseColor("#7C3AED"),
        lightBgColor = Color.parseColor("#F5F3FF"),
        strokeColor = Color.parseColor("#DDD6FE"),
        darkTextColor = Color.parseColor("#5B21B6"),
        defaultIconRes = R.drawable.ic_modern_akademik
    )

    private val themeSky = MapelTheme(
        primaryColor = Color.parseColor("#0284C7"),
        lightBgColor = Color.parseColor("#F0F9FF"),
        strokeColor = Color.parseColor("#BAE6FD"),
        darkTextColor = Color.parseColor("#075985"),
        defaultIconRes = R.drawable.ic_subject_science
    )

    private val themeTeal = MapelTheme(
        primaryColor = Color.parseColor("#0D9488"),
        lightBgColor = Color.parseColor("#F0FDFA"),
        strokeColor = Color.parseColor("#99F6E4"),
        darkTextColor = Color.parseColor("#115E59"),
        defaultIconRes = R.drawable.ic_subject_language
    )

    private val themePink = MapelTheme(
        primaryColor = Color.parseColor("#DB2777"),
        lightBgColor = Color.parseColor("#FDF2F8"),
        strokeColor = Color.parseColor("#FBCFE8"),
        darkTextColor = Color.parseColor("#9D174D"),
        defaultIconRes = R.drawable.ic_subject_book
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_jadwal_pelajaran)

        // Setup Edge-to-Edge Status Bar Transparan & Insets
        StatusBarHelper.setupTranslucentBar(this, findViewById(R.id.main))

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
        findViewById<TextView>(R.id.tvHeaderCategory)?.text = "AKADEMIK SISWA"

        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val namaSiswa = sharedPref.getString("nama_siswa", "Siswa")
        val fotoProfil = sharedPref.getString("foto_profil", "")
        val ivProfilPhoto = findViewById<ImageView>(R.id.ivProfilPhoto)
        val tvProfilInisial = findViewById<TextView>(R.id.tvProfilInisial)
        val cvProfilPic = findViewById<CardView>(R.id.cvProfilPic)
        AvatarHelper.setAvatar(this, namaSiswa, fotoProfil, ivProfilPhoto, tvProfilInisial, cvProfilPic)

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
            if (hari.equals(activeHari, ignoreCase = true)) {
                chip.setBackgroundResource(R.drawable.bg_chip_day_active)
                chip.backgroundTintList = null
                chip.setTextColor(Color.WHITE)
            } else {
                chip.setBackgroundResource(R.drawable.bg_chip_day_inactive)
                chip.backgroundTintList = null
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

    private fun getThemeForMapel(namaMapel: String): MapelTheme {
        val lower = namaMapel.lowercase()
        return when {
            lower.contains("islam") || lower.contains("pai") || lower.contains("qur'an") || lower.contains("fiqih") || lower.contains("akidah") || lower.contains("agama") -> themeEmerald
            lower.contains("inggris") || lower.contains("jepang") || lower.contains("arab") || lower.contains("sunda") || lower.contains("bahasa") -> themeTeal
            lower.contains("matematika") || lower.contains("mtk") || lower.contains("fisika") || lower.contains("kimia") || lower.contains("ipa") -> themeSky
            lower.contains("komputer") || lower.contains("rpl") || lower.contains("tkjt") || lower.contains("jaringan") || lower.contains("pbo") || lower.contains("web") || lower.contains("basis data") || lower.contains("kejuruan") || lower.contains("produktif") || lower.contains("desain") || lower.contains("dkv") -> themeIndigo
            lower.contains("olahraga") || lower.contains("penjas") || lower.contains("pjok") || lower.contains("pkn") || lower.contains("ppkn") -> themeRose
            lower.contains("sejarah") || lower.contains("seni") || lower.contains("budaya") || lower.contains("ips") || lower.contains("pkwu") || lower.contains("kewirausahaan") -> themeAmber
            lower.contains("bimbingan") || lower.contains("bk") || lower.contains("konseling") || lower.contains("literasi") -> themePurple
            else -> {
                val list = listOf(themeIndigo, themeEmerald, themeRose, themeAmber, themePurple, themeSky, themeTeal, themePink)
                val index = kotlin.math.abs(namaMapel.hashCode()) % list.size
                list[index]
            }
        }
    }

    private fun getKategoriText(namaMapel: String?): String {
        val lower = (namaMapel ?: "").lowercase()
        return when {
            lower.contains("islam") || lower.contains("pai") || lower.contains("pkn") || lower.contains("ppkn") || lower.contains("sejarah") || lower.contains("seni") || lower.contains("olahraga") || lower.contains("penjas") || lower.contains("pjok") -> "Muatan Nasional"
            lower.contains("inggris") || lower.contains("bahasa") || lower.contains("matematika") || lower.contains("ipa") || lower.contains("fisika") || lower.contains("kimia") -> "Akademik Umum"
            lower.contains("rpl") || lower.contains("tkjt") || lower.contains("komputer") || lower.contains("jaringan") || lower.contains("pbo") || lower.contains("web") || lower.contains("basis data") || lower.contains("kejuruan") || lower.contains("produktif") || lower.contains("dkv") -> "Konsentrasi Kejuruan"
            lower.contains("sunda") || lower.contains("jawa") || lower.contains("arab") -> "Muatan Lokal"
            lower.contains("pkwu") || lower.contains("kewirausahaan") -> "Kewirausahaan"
            lower.contains("bk") || lower.contains("konseling") -> "Bimbingan Konseling"
            else -> "Mata Pelajaran"
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
            for ((index, item) in list.withIndex()) {
                val cardView = inflater.inflate(R.layout.item_jadwal_siswa_card, containerJadwalHari, false)

                val viewColorAccent = cardView.findViewById<View>(R.id.viewColorAccent)
                val layTimeBadge = cardView.findViewById<LinearLayout>(R.id.layTimeBadge)
                val ivTimeIcon = cardView.findViewById<ImageView>(R.id.ivTimeIcon)
                val tvWaktu = cardView.findViewById<TextView>(R.id.tvJadwalWaktu)
                val tvStatus = cardView.findViewById<TextView>(R.id.tvJadwalStatus)
                val tvJamKeTag = cardView.findViewById<TextView>(R.id.tvJamKeTag)
                val cvIconBox = cardView.findViewById<CardView>(R.id.cvIconBox)
                val ivMapelIcon = cardView.findViewById<ImageView>(R.id.ivMapelIcon)
                val tvMapel = cardView.findViewById<TextView>(R.id.tvJadwalMapel)
                val tvGuru = cardView.findViewById<TextView>(R.id.tvJadwalGuru)
                val tvRuang = cardView.findViewById<TextView>(R.id.tvJadwalRuang)
                val tvKategoriBadge = cardView.findViewById<TextView>(R.id.tvKategoriBadge)

                val mapelName = item.nama_mapel ?: "Mata Pelajaran"
                val theme = getThemeForMapel(mapelName)

                // 1. Accent Bar on the left
                viewColorAccent.setBackgroundColor(theme.primaryColor)

                // 2. Icon Box (Rounded squircle with lightBg and themed icon)
                cvIconBox.setCardBackgroundColor(theme.lightBgColor)
                ivMapelIcon.setImageResource(theme.defaultIconRes)
                ivMapelIcon.imageTintList = ColorStateList.valueOf(theme.primaryColor)

                // 3. Time Pill (Curved with lightBg, subtle border and matching primary text)
                val timePillBg = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = 24f
                    setColor(theme.lightBgColor)
                    setStroke(2, theme.strokeColor)
                }
                layTimeBadge.background = timePillBg
                ivTimeIcon.imageTintList = ColorStateList.valueOf(theme.primaryColor)
                tvWaktu.text = item.waktu ?: "${item.jam_mulai} - ${item.jam_selesai} WIB"
                tvWaktu.setTextColor(theme.primaryColor)

                // 4. Jam Ke Tag
                tvJamKeTag.text = "Jam Ke-${index + 1}"

                // 5. Subject title, teacher & room
                tvMapel.text = mapelName
                tvGuru.text = if (!item.nama_guru.isNullOrBlank() && item.nama_guru != "-") "Guru: ${item.nama_guru}" else "Guru Pengampu"
                tvRuang.text = if (!item.ruang.isNullOrBlank()) "Ruang: ${item.ruang}" else "Ruang Kelas"
                tvKategoriBadge.text = getKategoriText(mapelName)

                // 6. Active indicator
                if (item.is_active == true) {
                    tvStatus.visibility = View.VISIBLE
                    tvStatus.text = "● Sedang Berlangsung"
                    tvStatus.setBackgroundResource(R.drawable.bg_badge_green_soft)
                    tvStatus.setTextColor(Color.parseColor("#15803D"))
                } else {
                    tvStatus.visibility = View.GONE
                }

                containerJadwalHari.addView(cardView)
            }
        }
    }
}
