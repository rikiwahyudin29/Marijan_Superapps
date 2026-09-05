package com.rtekmidev.marijancbt

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardGuruActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private val PERMISSION_REQUEST_CODE = 1001

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            prosesAbsenQR(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Pass "ROLE" intent to children fragments 
        intent.putExtra("ROLE", "GURU")

        // Transparent Status Bar
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Handle Status Bar Icon Colors
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode
        
        setContentView(R.layout.activity_dashboard_guru)

        // Terapkan bottom inset secara global agar konten tidak tertutup navigasi bawaan HP
        findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)?.let { rootView ->
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                // Jika view sudah punya padding top (dari header dsb), pertahankan. Hanya tambah bottom.
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom)
                insets
            }
        }


        val headerLayout = findViewById<LinearLayout>(R.id.headerLayout)
        ViewCompat.setOnApplyWindowInsetsListener(headerLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val topPaddingPx = (20 * resources.displayMetrics.density).toInt()
            view.setPadding(view.paddingLeft, systemBars.top + topPaddingPx, view.paddingRight, view.paddingBottom)
            insets
        }

        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val idUser = sharedPref.getString("id_user", null)
        
        if (idUser.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        val token = sharedPref.getString("token", "") ?: ""
        if (token.isNotEmpty()) {
            ApiClient.authToken = token
        }

        viewPager = findViewById(R.id.viewPager)
        viewPager.isUserInputEnabled = true
        val pagerAdapter = DashboardGuruPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        val navBeranda = findViewById<LinearLayout>(R.id.navBeranda)
        val navPresensi = findViewById<LinearLayout>(R.id.navPresensi)
        val navAkademik = findViewById<LinearLayout>(R.id.navAkademik)
        val navProfil = findViewById<LinearLayout>(R.id.navProfil)

        val headerTitleColor = if (isNightMode) android.graphics.Color.parseColor("#FFFFFF") else android.graphics.Color.parseColor("#1A1B41")
        val headerSubtextColor = if (isNightMode) android.graphics.Color.parseColor("#D1D5DB") else android.graphics.Color.parseColor("#6B7280")
        
        findViewById<TextView>(R.id.tvAppTitle)?.setTextColor(headerTitleColor)
        findViewById<View>(R.id.btnNotifikasi)?.setOnClickListener {
            Toast.makeText(this, "Tidak ada notifikasi baru.", Toast.LENGTH_SHORT).show()
        }

        fun updateNavSelection(position: Int) {
            val unselectedColor = if (isNightMode) {
                android.graphics.Color.parseColor("#B3FFFFFF")
            } else {
                android.graphics.Color.parseColor("#9CA3AF")
            }
            val selectedColor = if (isNightMode) {
                android.graphics.Color.parseColor("#FFFFFF")
            } else {
                android.graphics.Color.parseColor("#1E3A8A")
            }

            findViewById<ImageView>(R.id.ivNavBeranda).setColorFilter(unselectedColor)
            findViewById<TextView>(R.id.tvNavBeranda).setTextColor(unselectedColor)
            findViewById<TextView>(R.id.tvNavBeranda).typeface = android.graphics.Typeface.DEFAULT

            findViewById<ImageView>(R.id.ivNavPresensi).setColorFilter(unselectedColor)
            findViewById<TextView>(R.id.tvNavPresensi).setTextColor(unselectedColor)
            findViewById<TextView>(R.id.tvNavPresensi).typeface = android.graphics.Typeface.DEFAULT

            findViewById<ImageView>(R.id.ivNavAkademik).setColorFilter(unselectedColor)
            findViewById<TextView>(R.id.tvNavAkademik).setTextColor(unselectedColor)
            findViewById<TextView>(R.id.tvNavAkademik).typeface = android.graphics.Typeface.DEFAULT

            findViewById<ImageView>(R.id.ivNavProfil).setColorFilter(unselectedColor)
            findViewById<TextView>(R.id.tvNavProfil).setTextColor(unselectedColor)
            findViewById<TextView>(R.id.tvNavProfil).typeface = android.graphics.Typeface.DEFAULT

            when (position) {
                0 -> {
                    findViewById<ImageView>(R.id.ivNavBeranda).setColorFilter(selectedColor)
                    findViewById<TextView>(R.id.tvNavBeranda).setTextColor(selectedColor)
                    findViewById<TextView>(R.id.tvNavBeranda).typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                1 -> {
                    findViewById<ImageView>(R.id.ivNavPresensi).setColorFilter(selectedColor)
                    findViewById<TextView>(R.id.tvNavPresensi).setTextColor(selectedColor)
                    findViewById<TextView>(R.id.tvNavPresensi).typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                2 -> {
                    findViewById<ImageView>(R.id.ivNavAkademik).setColorFilter(selectedColor)
                    findViewById<TextView>(R.id.tvNavAkademik).setTextColor(selectedColor)
                    findViewById<TextView>(R.id.tvNavAkademik).typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                3 -> {
                    findViewById<ImageView>(R.id.ivNavProfil).setColorFilter(selectedColor)
                    findViewById<TextView>(R.id.tvNavProfil).setTextColor(selectedColor)
                    findViewById<TextView>(R.id.tvNavProfil).typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
            }
        }

        updateNavSelection(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateNavSelection(position)
            }
        })
        
        navBeranda.setOnClickListener { viewPager.currentItem = 0 }
        navPresensi.setOnClickListener { viewPager.currentItem = 1 }
        navAkademik.setOnClickListener { viewPager.currentItem = 2 }
        navProfil.setOnClickListener { viewPager.currentItem = 3 }

        findViewById<FloatingActionButton>(R.id.fabScanner).setOnClickListener {
            cekRadiusDanScan()
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewPager.currentItem != 0) {
                    viewPager.currentItem = 0
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    fun cekRadiusDanScan() {
        val cal = java.util.Calendar.getInstance()
        val dow = cal.get(java.util.Calendar.DAY_OF_WEEK)
        if (dow == java.util.Calendar.SUNDAY || dow == java.util.Calendar.SATURDAY) {
            val namaHari = if (dow == java.util.Calendar.SUNDAY) "Hari Minggu" else "Hari Sabtu"
            Toast.makeText(this, "Hari ini libur sekolah ($namaHari), presensi tidak aktif.", Toast.LENGTH_SHORT).show()
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.CAMERA), PERMISSION_REQUEST_CODE)
            return
        }

        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val lastLoc = locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
            ?: locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
            
        if (lastLoc == null) {
            Toast.makeText(this, "Gagal mendapatkan lokasi GPS. Pastikan GPS aktif.", Toast.LENGTH_SHORT).show()
            return
        }

        Toast.makeText(this, "Memeriksa lokasi...", Toast.LENGTH_SHORT).show()
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.getSettingPresensi()
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && ((resp.body()?.status?.isJsonPrimitive == true && resp.body()?.status?.asBoolean == true) || resp.body()?.status?.asString == "success" || resp.body()?.status?.asString == "true")) {
                        val d = resp.body()?.data
                        val schoolLat = d?.latitude?.toDouble() ?: 0.0
                        val schoolLng = d?.longitude?.toDouble() ?: 0.0
                        val schoolRadius = d?.radius ?: 200
                        
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(lastLoc.latitude, lastLoc.longitude, schoolLat, schoolLng, results)
                        val jarak = results[0].toInt()
                        
                        if (jarak <= schoolRadius) {
                            bukaScannerQR()
                        } else {
                            val selisih = jarak - schoolRadius
                            Toast.makeText(this@DashboardGuruActivity, "Gagal: Anda berada $selisih meter di luar jangkauan (Jarak: $jarak m, Maks: $schoolRadius m).", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@DashboardGuruActivity, "Gagal memuat pengaturan sekolah", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DashboardGuruActivity, "Koneksi Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun bukaScannerQR() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Arahkan ke QR Code Kehadiran")
        options.setBeepEnabled(true)
        options.setCaptureActivity(CustomScannerActivity::class.java)
        options.setOrientationLocked(true)
        barcodeLauncher.launch(options)
    }

    private fun prosesAbsenQR(qrToken: String) {
        val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val identifier = sharedPref.getString("id_user", "") ?: ""
        
        if (identifier.isEmpty()) {
            Toast.makeText(this, "Sesi tidak valid", Toast.LENGTH_SHORT).show()
            return
        }
        
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val lastLoc = if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                ?: locationManager.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
        } else null
        
        val lat = lastLoc?.latitude ?: 0.0
        val lng = lastLoc?.longitude ?: 0.0

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = ApiClient.instance.submitAbsenGuru(identifier, lat.toString(), lng.toString(), qrToken)
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == true) {
                        Toast.makeText(this@DashboardGuruActivity, "Presensi Berhasil!", Toast.LENGTH_LONG).show()
                    } else {
                        var errorMsg = response.body()?.message
                        if (errorMsg.isNullOrEmpty()) errorMsg = "Presensi Gagal"
                        Toast.makeText(this@DashboardGuruActivity, errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DashboardGuruActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private inner class DashboardGuruPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> BerandaGuruFragment()
                1 -> PresensiGuruFragment()
                2 -> AkademikGuruFragment()
                3 -> ProfilFragment()
                else -> BerandaGuruFragment()
            }
        }
    }
}