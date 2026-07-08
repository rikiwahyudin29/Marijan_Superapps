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

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private val PERMISSION_REQUEST_CODE = 1001

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            prosesAbsenQR(result.contents)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Transparent Status Bar
        @Suppress("DEPRECATION")
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        // Handle Status Bar Icon Colors (Black in Light Mode, White in Dark Mode)
        val currentNightMode = resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        val isNightMode = currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.isAppearanceLightStatusBars = !isNightMode
        
        setContentView(R.layout.activity_dashboard)

        // Add padding to headerLayout so it doesn't overlap the status bar icons
        val headerLayout = findViewById<LinearLayout>(R.id.headerLayout)
        ViewCompat.setOnApplyWindowInsetsListener(headerLayout) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val topPaddingPx = (20 * resources.displayMetrics.density).toInt()
            view.setPadding(view.paddingLeft, systemBars.top + topPaddingPx, view.paddingRight, view.paddingBottom)
            insets
        }

        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val nisn = sharedPref.getString("nisn", null)
        
        if (nisn.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        viewPager = findViewById(R.id.viewPager)
        viewPager.isUserInputEnabled = true // Enable swipe navigation
        val pagerAdapter = DashboardPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        val navBeranda = findViewById<LinearLayout>(R.id.navBeranda)
        val navAkademik = findViewById<LinearLayout>(R.id.navAkademik)
        val navCbt = findViewById<LinearLayout>(R.id.navCbt)
        val navPresensi = findViewById<LinearLayout>(R.id.navPresensi)
        val navKeuangan = findViewById<LinearLayout>(R.id.navKeuangan)
        val navProfil = findViewById<LinearLayout>(R.id.navProfil)
        
        // Update Header Text Colors based on theme
        val headerTitleColor = if (isNightMode) android.graphics.Color.parseColor("#FFFFFF") else android.graphics.Color.parseColor("#1A1B41")
        val headerSubtextColor = if (isNightMode) android.graphics.Color.parseColor("#D1D5DB") else android.graphics.Color.parseColor("#6B7280")
        
        findViewById<TextView>(R.id.tvAppTitle).setTextColor(headerTitleColor)
        findViewById<TextView>(R.id.tvNamaDashboard).setTextColor(headerTitleColor)
        findViewById<TextView>(R.id.tvKelas).setTextColor(headerSubtextColor)

        fun updateNavSelection(position: Int) {
            val unselectedColor = if (isNightMode) {
                android.graphics.Color.parseColor("#B3FFFFFF") // 70% White for Dark Mode
            } else {
                android.graphics.Color.parseColor("#9CA3AF") // Gray for Light Mode
            }
            
            val selectedColor = if (isNightMode) {
                android.graphics.Color.parseColor("#FFFFFF") // Solid White for Dark Mode
            } else {
                android.graphics.Color.parseColor("#1E3A8A") // Dark Blue for Light Mode
            }

            // Reset all
            findViewById<ImageView>(R.id.ivNavBeranda).setColorFilter(unselectedColor)
            findViewById<TextView>(R.id.tvNavBeranda).setTextColor(unselectedColor)
            findViewById<TextView>(R.id.tvNavBeranda).typeface = android.graphics.Typeface.DEFAULT

            findViewById<ImageView>(R.id.ivNavAkademik).setColorFilter(unselectedColor)
            findViewById<TextView>(R.id.tvNavAkademik).setTextColor(unselectedColor)
            findViewById<TextView>(R.id.tvNavAkademik).typeface = android.graphics.Typeface.DEFAULT

            findViewById<ImageView>(R.id.ivNavCbt).setColorFilter(unselectedColor)
            findViewById<TextView>(R.id.tvNavCbt).setTextColor(unselectedColor)
            findViewById<TextView>(R.id.tvNavCbt).typeface = android.graphics.Typeface.DEFAULT
            
            findViewById<ImageView>(R.id.ivNavPresensi).setColorFilter(unselectedColor)
            findViewById<TextView>(R.id.tvNavPresensi).setTextColor(unselectedColor)
            findViewById<TextView>(R.id.tvNavPresensi).typeface = android.graphics.Typeface.DEFAULT

            findViewById<ImageView>(R.id.ivNavKeuangan).setColorFilter(unselectedColor)
            findViewById<TextView>(R.id.tvNavKeuangan).setTextColor(unselectedColor)
            findViewById<TextView>(R.id.tvNavKeuangan).typeface = android.graphics.Typeface.DEFAULT

            findViewById<ImageView>(R.id.ivNavProfil).setColorFilter(unselectedColor)
            findViewById<TextView>(R.id.tvNavProfil).setTextColor(unselectedColor)
            findViewById<TextView>(R.id.tvNavProfil).typeface = android.graphics.Typeface.DEFAULT

            when (position) {
                0 -> { // Beranda
                    findViewById<ImageView>(R.id.ivNavBeranda).setColorFilter(selectedColor)
                    findViewById<TextView>(R.id.tvNavBeranda).setTextColor(selectedColor)
                    findViewById<TextView>(R.id.tvNavBeranda).typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                1 -> { // Akademik
                    findViewById<ImageView>(R.id.ivNavAkademik).setColorFilter(selectedColor)
                    findViewById<TextView>(R.id.tvNavAkademik).setTextColor(selectedColor)
                    findViewById<TextView>(R.id.tvNavAkademik).typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                2 -> { // CBT
                    findViewById<ImageView>(R.id.ivNavCbt).setColorFilter(selectedColor)
                    findViewById<TextView>(R.id.tvNavCbt).setTextColor(selectedColor)
                    findViewById<TextView>(R.id.tvNavCbt).typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                3 -> { // Presensi
                    findViewById<ImageView>(R.id.ivNavPresensi).setColorFilter(selectedColor)
                    findViewById<TextView>(R.id.tvNavPresensi).setTextColor(selectedColor)
                    findViewById<TextView>(R.id.tvNavPresensi).typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                4 -> { // Keuangan
                    findViewById<ImageView>(R.id.ivNavKeuangan).setColorFilter(selectedColor)
                    findViewById<TextView>(R.id.tvNavKeuangan).setTextColor(selectedColor)
                    findViewById<TextView>(R.id.tvNavKeuangan).typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
                5 -> { // Profil
                    findViewById<ImageView>(R.id.ivNavProfil).setColorFilter(selectedColor)
                    findViewById<TextView>(R.id.tvNavProfil).setTextColor(selectedColor)
                    findViewById<TextView>(R.id.tvNavProfil).typeface = android.graphics.Typeface.DEFAULT_BOLD
                }
            }
        }
        
        // Apply initial colors
        updateNavSelection(0)

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateNavSelection(position)
            }
        })
        
        navBeranda.setOnClickListener { viewPager.currentItem = 0 }
        navAkademik.setOnClickListener { viewPager.currentItem = 1 }
        navCbt.setOnClickListener { viewPager.currentItem = 2 }
        navPresensi.setOnClickListener { viewPager.currentItem = 3 }
        navKeuangan.setOnClickListener { viewPager.currentItem = 4 }
        navProfil.setOnClickListener { viewPager.currentItem = 5 }

        findViewById<FloatingActionButton>(R.id.fabScanner).setOnClickListener {
            cekRadiusDanScan()
        }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewPager.currentItem != 0) {
                    viewPager.currentItem = 0 // Return to Beranda
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed() // Exit app
                }
            }
        })
    }

    private fun cekRadiusDanScan() {
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
                        val schoolRadius = d?.radius ?: 100
                        
                        val results = FloatArray(1)
                        android.location.Location.distanceBetween(lastLoc.latitude, lastLoc.longitude, schoolLat, schoolLng, results)
                        val jarak = results[0].toInt()
                        
                        if (jarak <= schoolRadius) {
                            bukaScannerQR()
                        } else {
                            val selisih = jarak - schoolRadius
                            Toast.makeText(this@DashboardActivity, "Gagal: Anda berada $selisih meter di luar jangkauan (Jarak: $jarak m, Maks: $schoolRadius m).", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        Toast.makeText(this@DashboardActivity, "Gagal memuat pengaturan sekolah", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DashboardActivity, "Koneksi Error: ${e.message}", Toast.LENGTH_SHORT).show()
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
        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val identifier = sharedPref.getString("nisn", "") ?: ""
        val role = sharedPref.getString("role", "SISWA") ?: "SISWA"
        
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
                val response = if (role == "GURU") {
                    ApiClient.instance.submitAbsenGuru(identifier, lat.toString(), lng.toString(), qrToken)
                } else {
                    ApiClient.instance.submitAbsen(identifier, lat.toString(), lng.toString(), qrToken)
                }
                
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == true) {
                        Toast.makeText(this@DashboardActivity, "Presensi Berhasil!", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@DashboardActivity, response.body()?.message ?: "Presensi Gagal", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@DashboardActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    

    private inner class DashboardPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 6 // 6 Nav Items

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> BerandaFragment()
                1 -> AkademikFragment()
                2 -> CbtFragment()
                3 -> PresensiFragment()
                4 -> KeuanganFragment()
                5 -> ProfilFragment()
                else -> BerandaFragment()
            }
        }
    }
}

