package com.rtekmidev.marijancbt

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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.marijancbt.api.ApiClient
import com.rtekmidev.marijancbt.api.DataRekap
import kotlinx.coroutines.*
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
        setContentView(R.layout.activity_rekap)

        val prefGuru = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val prefSiswa = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)

        if (prefGuru.getBoolean("isLoggedIn", false)) {
            userRole = "GURU"
            identifier = prefGuru.getString("id_user", "") ?: ""
        } else if (prefSiswa.getBoolean("isLoggedIn", false)) {
            userRole = "SISWA"
            identifier = prefSiswa.getString("nisn", "") ?: ""
        } else {
            Toast.makeText(this, "Sesi tidak valid!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        findViewById<ImageView>(R.id.btnBackRekap).setOnClickListener { finish() }

        findViewById<ImageView>(R.id.btnBackRekap).setOnClickListener { finish() }

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
                        findViewById<TextView>(R.id.tvTotalAlpha).text = body.summary?.alpha?.toString() ?: "0"

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
            } else if (status == "izin" || status == "izin pulang" || status == "udzur syar'i") {
                tvBadge.setBackgroundColor(Color.parseColor("#00D2D3"))
                tvBadge.setTextColor(Color.WHITE)
            } else if (status == "sakit") {
                tvBadge.setBackgroundColor(Color.parseColor("#E5E7EB"))
                tvBadge.setTextColor(Color.parseColor("#4B5563"))
            } else if (status == "alfa" || status == "alpha") {
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
}
