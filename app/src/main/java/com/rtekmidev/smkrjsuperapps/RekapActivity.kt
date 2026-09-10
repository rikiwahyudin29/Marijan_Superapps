package com.rtekmidev.smkrjsuperapps

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.DataRekap
import com.rtekmidev.smkrjsuperapps.util.AvatarHelper
import kotlinx.coroutines.*
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class RekapActivity : AppCompatActivity() {

    private var userRole = ""
    private var identifier = ""

    private var currentMonthCalendar = Calendar.getInstance()
    private var listKehadiran = listOf<DataRekap>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Override transition (masuk mulus)
        applyEnterTransition()
        
        setContentView(R.layout.activity_rekap)

        // Status bar transparan
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(window, false)

        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)?.isAppearanceLightStatusBars = !isNightMode

        val rootLayout = findViewById<View>(R.id.rootRekap)
        androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootLayout) { view, insets ->
            val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val prefGuru = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val prefSiswa = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)

        var namaUser = ""
        var fotoProfilUrl = ""
        if (prefGuru.getBoolean("isLoggedIn", false)) {
            userRole = "GURU"
            identifier = prefGuru.getString("id_user", "") ?: ""
            namaUser = prefGuru.getString("nama", "Guru") ?: "Guru"
            fotoProfilUrl = prefGuru.getString("foto_profil", "") ?: ""
        } else if (prefSiswa.getBoolean("isLoggedIn", false)) {
            userRole = "SISWA"
            identifier = prefSiswa.getString("nisn", "") ?: ""
            namaUser = prefSiswa.getString("nama", "Siswa") ?: "Siswa"
            fotoProfilUrl = prefSiswa.getString("foto_profil", "") ?: ""
        } else {
            Toast.makeText(this, "Sesi tidak valid!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.btnBackRekap).setOnClickListener { finish() }

        val ivProfilPhoto = findViewById<ImageView>(R.id.ivProfilPhoto)
        val tvProfilInisial = findViewById<TextView>(R.id.tvProfilInisial)
        val cvProfilPic = findViewById<CardView>(R.id.cvProfilPic)

        val finalUrl = if (fotoProfilUrl.isNotEmpty()) {
            if (fotoProfilUrl.startsWith("http")) {
                fotoProfilUrl
            } else if (userRole == "GURU") {
                "https://smkriyadhuljannahjalancagak.sch.id/uploads/guru/$fotoProfilUrl"
            } else {
                "https://smkriyadhuljannahjalancagak.sch.id/uploads/siswa/$fotoProfilUrl"
            }
        } else {
            null
        }

        AvatarHelper.setAvatar(
            context = this,
            name = namaUser,
            fotoUrl = finalUrl,
            ivPhoto = ivProfilPhoto,
            tvInitial = tvProfilInisial,
            cardContainer = cvProfilPic
        )
        cvProfilPic?.setOnClickListener { finish() }

        findViewById<ImageView>(R.id.btnPrevMonth).setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, -1)
            muatDataRekap()
        }

        findViewById<ImageView>(R.id.btnNextMonth).setOnClickListener {
            currentMonthCalendar.add(Calendar.MONTH, 1)
            muatDataRekap()
        }

        muatDataRekap()
    }

    private fun updateMonthText() {
        val sdf = SimpleDateFormat("MMMM yyyy", Locale("id", "ID"))
        findViewById<TextView>(R.id.tvMonthYear).text = sdf.format(currentMonthCalendar.time)
    }



    private fun goToDashboard(tabIndex: Int) {
        val intent = Intent(this, DashboardActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        // Passing intent extra might require DashboardActivity to handle it, but standard Android behavior is to just let user navigate back
        startActivity(intent)
        finish()
    }

    private fun muatDataRekap() {
        if (identifier.isEmpty()) return

        updateMonthText()

        val yearMonthSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val selectedMonthStr = yearMonthSdf.format(currentMonthCalendar.time)

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = if (userRole == "GURU") {
                    ApiClient.instance.getRekapGuru(identifier, selectedMonthStr)
                } else {
                    ApiClient.instance.getRekapAbsen(identifier, selectedMonthStr)
                }

                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body()?.status == true) {
                        val body = resp.body()!!
                        
                        findViewById<TextView>(R.id.tvTotalHadir).text = body.summary?.hadir?.toString() ?: "0"
                        findViewById<TextView>(R.id.tvTotalIzin).text = body.summary?.izin?.toString() ?: "0"
                        findViewById<TextView>(R.id.tvTotalSakit).text = body.summary?.sakit?.toString() ?: "0"
                        val countAlfa = body.summary?.alfa ?: body.summary?.alpha ?: 0
                        findViewById<TextView>(R.id.tvTotalAlpha).text = countAlfa.toString()
                        findViewById<TextView>(R.id.tvTotalTerlambat).text = "${body.summary?.terlambat ?: 0}m"
                        findViewById<TextView>(R.id.tvTotalCuti).text = body.summary?.cuti?.toString() ?: "0"
                        findViewById<TextView>(R.id.tvTotalDinasLuar).text = body.summary?.dinas_luar?.toString() ?: "0"

                        listKehadiran = body.data ?: emptyList()
                        buildCalendar()
                        
                    } else {
                        Toast.makeText(this@RekapActivity, "Gagal memuat data rekap", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@RekapActivity, "Koneksi API Error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun buildCalendar() {
        val daysList = mutableListOf<CalendarDay>()
        
        val cal = currentMonthCalendar.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) // Sunday=1, Monday=2
        
        // Android Calendar: 1=Sunday, 2=Monday. Our UI: 0=Min, 1=Sen, 2=Sel, 3=Rab, 4=Kam, 5=Jum, 6=Sab
        val offset = firstDayOfWeek - 1
        
        for (i in 0 until offset) {
            daysList.add(CalendarDay(0, false, false, "")) // Empty cells
        }
        
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val todayCal = Calendar.getInstance()
        val isCurrentMonth = todayCal.get(Calendar.YEAR) == cal.get(Calendar.YEAR) && 
                             todayCal.get(Calendar.MONTH) == cal.get(Calendar.MONTH)
        val todayDate = todayCal.get(Calendar.DAY_OF_MONTH)

        val sdfFullDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

        for (day in 1..maxDays) {
            cal.set(Calendar.DAY_OF_MONTH, day)
            val dateStr = sdfFullDate.format(cal.time)
            
            // Find status from API response
            val dataForDay = listKehadiran.find { it.tanggal == dateStr }
            val status = dataForDay?.status_kehadiran
            
            val isToday = isCurrentMonth && day == todayDate
            val isSelected = isToday // default select today, or day 1 if not current month
            
            daysList.add(CalendarDay(day, isToday, false, dateStr, status))
        }

        // If today is in list, select it. Else select day 1
        var selectedIdx = daysList.indexOfFirst { it.isToday }
        if (selectedIdx == -1) {
            selectedIdx = daysList.indexOfFirst { it.dayNumber == 1 }
        }
        if (selectedIdx != -1) {
            daysList[selectedIdx].isSelected = true
            updateDetailCard(daysList[selectedIdx])
        }

        val rv = findViewById<RecyclerView>(R.id.rvCalendar)
        rv.layoutManager = GridLayoutManager(this, 7)
        rv.adapter = CalendarAdapter(daysList) { day ->
            updateDetailCard(day)
        }
    }

    private fun updateDetailCard(day: CalendarDay) {
        val sdfParse = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val sdfFormat = SimpleDateFormat("dd MMM yyyy", Locale("id", "ID"))
        
        var dateText = day.dateString
        try {
            val d = sdfParse.parse(day.dateString)
            if (d != null) dateText = sdfFormat.format(d)
        } catch (e: Exception) { }

        findViewById<TextView>(R.id.tvDetailTitle).text = "Detail Hari: $dateText"
        
        val dataForDay = listKehadiran.find { it.tanggal == day.dateString }
        
        val tvMasuk = findViewById<TextView>(R.id.tvDetailMasuk)
        val tvPulang = findViewById<TextView>(R.id.tvDetailPulang)
        val tvBadge = findViewById<TextView>(R.id.tvDetailStatusBadge)

        if (dataForDay != null) {
            tvMasuk.text = dataForDay.jam_masuk ?: "--:--"
            tvPulang.text = dataForDay.jam_pulang ?: "--:--"
            
            tvBadge.visibility = View.VISIBLE
            tvBadge.text = dataForDay.status_kehadiran.uppercase()
            
            val status = dataForDay.status_kehadiran.lowercase()
            if (status == "hadir" || status == "tepat waktu") {
                tvBadge.setBackgroundColor(Color.parseColor("#1E3A8A"))
                tvBadge.setTextColor(Color.WHITE)
            } else if (status == "terlambat") {
                tvBadge.setBackgroundColor(Color.parseColor("#FEF3C7"))
                tvBadge.setTextColor(Color.parseColor("#B45309"))
            } else if (status == "izin" || status == "izin pulang" || status == "udzur syar'i" || status == "dinas luar" || status == "cuti") {
                tvBadge.setBackgroundColor(Color.parseColor("#00D2D3"))
                tvBadge.setTextColor(Color.WHITE)
            } else if (status == "sakit") {
                tvBadge.setBackgroundColor(Color.parseColor("#E5E7EB"))
                tvBadge.setTextColor(Color.parseColor("#4B5563"))
            } else if (status == "alfa" || status == "alpha" || status == "alpa" || status == "a") {
                tvBadge.setBackgroundColor(Color.parseColor("#FEE2E2"))
                tvBadge.setTextColor(Color.parseColor("#991B1B"))
            } else {
                tvBadge.setBackgroundColor(Color.parseColor("#1E3A8A"))
                tvBadge.setTextColor(Color.WHITE)
            }
        } else {
            tvMasuk.text = "--:--"
            tvPulang.text = "--:--"
            tvBadge.visibility = View.GONE
        }
    }

    override fun finish() {
        super.finish()
        // Override transition (keluar mulus)
        applyExitTransition()
    }
}
