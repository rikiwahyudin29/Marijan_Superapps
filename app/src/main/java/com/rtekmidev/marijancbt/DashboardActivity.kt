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

        // Terapkan bottom inset secara global agar konten tidak tertutup navigasi bawaan HP
        findViewById<android.view.ViewGroup>(android.R.id.content).getChildAt(0)?.let { rootView ->
            androidx.core.view.ViewCompat.setOnApplyWindowInsetsListener(rootView) { view, insets ->
                val systemBars = insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars())
                // Jika view sudah punya padding top (dari header dsb), pertahankan. Hanya tambah bottom.
                view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, systemBars.bottom)
                insets
            }
        }


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

        // Set token ke ApiClient
        ApiClient.authToken = sharedPref.getString("token", "") ?: ""

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
        val headerTitleColor = if (isNightMode) android.graphics.Color.parseColor("#FFFFFF") else android.graphics.Color.parseColor("#1E1B4B")
        val headerSubtextColor = if (isNightMode) android.graphics.Color.parseColor("#D1D5DB") else android.graphics.Color.parseColor("#64748B")
        
        findViewById<TextView>(R.id.tvAppTitle)?.setTextColor(headerTitleColor)
        findViewById<TextView>(R.id.tvAppSubtitle)?.setTextColor(headerSubtextColor)
        findViewById<TextView>(R.id.tvNamaDashboard)?.setTextColor(headerTitleColor)
        findViewById<TextView>(R.id.tvKelas)?.setTextColor(headerSubtextColor)

        findViewById<View>(R.id.btnNotifikasi)?.setOnClickListener {
            Toast.makeText(this, "Belum ada notifikasi baru", Toast.LENGTH_SHORT).show()
        }

        findViewById<View>(R.id.cvProfilPic)?.setOnClickListener {
            viewPager.currentItem = 5 // Tab Profil
        }


        val ivBeranda = findViewById<ImageView>(R.id.ivNavBeranda)
        val ivAkademik = findViewById<ImageView>(R.id.ivNavAkademik)
        val ivCbt = findViewById<ImageView>(R.id.ivNavCbt)
        val ivPresensi = findViewById<ImageView>(R.id.ivNavPresensi)
        val ivKeuangan = findViewById<ImageView>(R.id.ivNavKeuangan)
        val ivProfil = findViewById<ImageView>(R.id.ivNavProfil)

        val tvBeranda = findViewById<TextView>(R.id.tvNavBeranda)
        val tvAkademik = findViewById<TextView>(R.id.tvNavAkademik)
        val tvCbt = findViewById<TextView>(R.id.tvNavCbt)
        val tvPresensi = findViewById<TextView>(R.id.tvNavPresensi)
        val tvKeuangan = findViewById<TextView>(R.id.tvNavKeuangan)
        val tvProfil = findViewById<TextView>(R.id.tvNavProfil)

        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        fun updateNavSelection(position: Int) {
            val navItems = listOf(
                Triple(navBeranda, ivBeranda, tvBeranda),
                Triple(navAkademik, ivAkademik, tvAkademik),
                Triple(navCbt, ivCbt, tvCbt),
                Triple(navPresensi, ivPresensi, tvPresensi),
                Triple(navKeuangan, ivKeuangan, tvKeuangan),
                Triple(navProfil, ivProfil, tvProfil)
            )

            navItems.forEachIndexed { index, (layout, iv, tv) ->
                val isSelected = (index == position)
                val params = layout?.layoutParams as? LinearLayout.LayoutParams
                if (isSelected) {
                    params?.width = LinearLayout.LayoutParams.WRAP_CONTENT
                    params?.weight = 0f
                    layout?.layoutParams = params
                    layout?.setBackgroundResource(R.drawable.bg_nav_capsule_active)
                    layout?.setPadding(dp(14), dp(8), dp(14), dp(8))
                    iv?.setColorFilter(android.graphics.Color.parseColor("#FFFFFF"))
                    tv?.visibility = View.VISIBLE
                    tv?.setTextColor(android.graphics.Color.parseColor("#FFFFFF"))
                } else {
                    params?.width = 0
                    params?.weight = 1f
                    layout?.layoutParams = params
                    layout?.background = null
                    layout?.setPadding(dp(6), dp(8), dp(6), dp(8))
                    iv?.setColorFilter(android.graphics.Color.parseColor("#94A3B8"))
                    tv?.visibility = View.GONE
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

        findViewById<View>(R.id.fabScanner)?.setOnClickListener {
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

    fun cekRadiusDanScan() {
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
                        var errorMsg = response.body()?.message
                        if (errorMsg.isNullOrEmpty() && !response.isSuccessful) {
                            try {
                                val errorStr = response.errorBody()?.string()
                                if (!errorStr.isNullOrEmpty()) {
                                    if (errorStr.startsWith("{")) {
                                        val jsonObject = org.json.JSONObject(errorStr)
                                        errorMsg = jsonObject.optString("message", "")
                                        if (errorMsg.isNullOrEmpty()) errorMsg = errorStr.take(100)
                                    } else {
                                        errorMsg = "Server Error: " + errorStr.take(100)
                                    }
                                }
                            } catch (e: Exception) {
                                errorMsg = "Gagal memproses error dari server."
                            }
                        }
                        if (errorMsg.isNullOrEmpty()) errorMsg = "Presensi Gagal (Unknown Error)"
                        Toast.makeText(this@DashboardActivity, errorMsg, Toast.LENGTH_LONG).show()
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

