package com.rtekmidev.smkrjsuperapps

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
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
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.util.AvatarHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private var isBottomNavHidden = false
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
        sinkronisasiFcmToken()
        com.rtekmidev.smkrjsuperapps.service.DeviceLocationScheduler.jadwalkanPeriodicUpdate(this)

        // 🔔 Minta izin notifikasi runtime di Android 13+ (API 33+) agar notifikasi muncul di layar
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1012)
            }
        }

        viewPager = findViewById(R.id.viewPager)
        viewPager.isUserInputEnabled = true // Enable swipe navigation
        viewPager.offscreenPageLimit = 5
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

        val btnRefreshTop = findViewById<View>(R.id.btnRefreshTop)
        val ivRefreshTop = findViewById<ImageView>(R.id.ivRefreshTop)

        btnRefreshTop?.setOnClickListener {
            ivRefreshTop?.animate()?.rotationBy(360f)?.setDuration(600)?.start()
            Toast.makeText(this, "Memperbarui data...", Toast.LENGTH_SHORT).show()

            val currentFrag = (viewPager.adapter as? DashboardPagerAdapter)?.getFragment(viewPager.currentItem)
                ?: supportFragmentManager.findFragmentByTag("f" + viewPager.currentItem)

            if (currentFrag is com.rtekmidev.smkrjsuperapps.util.RefreshableFragment) {
                currentFrag.refreshData()
            }

            // Perbarui avatar / inisial header
            val ivProfil = findViewById<ImageView>(R.id.ivProfilPhoto)
            val tvInisial = findViewById<TextView>(R.id.tvProfilInisial)
            val cvProfil = findViewById<CardView>(R.id.cvProfilPic)
            val nama = sharedPref.getString("nama_siswa", null) ?: sharedPref.getString("nama", null)
            val foto = sharedPref.getString("foto_profil", null)
            AvatarHelper.setAvatar(this, nama, foto, ivProfil, tvInisial, cvProfil)
        }

        // Initialize Avatar / Initials in Top Bar
        val ivProfilPhoto = findViewById<ImageView>(R.id.ivProfilPhoto)
        val tvProfilInisial = findViewById<TextView>(R.id.tvProfilInisial)
        val cvProfilPic = findViewById<CardView>(R.id.cvProfilPic)
        val namaSiswa = sharedPref.getString("nama_siswa", null) ?: sharedPref.getString("nama", null)
        val fotoSiswa = sharedPref.getString("foto_profil", null)
        AvatarHelper.setAvatar(this, namaSiswa, fotoSiswa, ivProfilPhoto, tvProfilInisial, cvProfilPic)

        cvProfilPic?.setOnClickListener {
            viewPager.setCurrentItem(5, false) // Tab Profil
            showBottomNav()
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

        val bottomNavContainer = findViewById<ViewGroup>(R.id.bottomNavigation)

        fun updateNavSelection(position: Int, animate: Boolean = true) {
            if (animate && bottomNavContainer != null) {
                val transition = androidx.transition.AutoTransition().apply {
                    duration = 180
                    interpolator = android.view.animation.DecelerateInterpolator(1.5f)
                }
                androidx.transition.TransitionManager.beginDelayedTransition(bottomNavContainer, transition)
            }

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
                    layout?.setBackgroundResource(R.drawable.bg_nav_capsule_inactive)
                    layout?.setPadding(dp(6), dp(8), dp(6), dp(8))
                    iv?.setColorFilter(android.graphics.Color.parseColor("#94A3B8"))
                    tv?.visibility = View.GONE
                }
            }
        }
        
        // Apply initial tab selection (support TARGET_TAB e.g. from CBT exam finish)
        val initialTab = intent.getIntExtra("TARGET_TAB", 0)
        if (initialTab in 0..5) {
            viewPager.setCurrentItem(initialTab, false)
            updateNavSelection(initialTab, animate = false)
        } else {
            updateNavSelection(0, animate = false)
        }

        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateNavSelection(position, animate = true)
                showBottomNav()
            }
        })
        
        navBeranda.setOnClickListener { viewPager.setCurrentItem(0, false); showBottomNav() }
        navAkademik.setOnClickListener { viewPager.setCurrentItem(1, false); showBottomNav() }
        navCbt.setOnClickListener { viewPager.setCurrentItem(2, false); showBottomNav() }
        navPresensi.setOnClickListener { viewPager.setCurrentItem(3, false); showBottomNav() }
        navKeuangan.setOnClickListener { viewPager.setCurrentItem(4, false); showBottomNav() }
        navProfil.setOnClickListener { viewPager.setCurrentItem(5, false); showBottomNav() }

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val targetTab = intent.getIntExtra("TARGET_TAB", -1)
        if (targetTab in 0..5) {
            viewPager.setCurrentItem(targetTab, false)
            showBottomNav()
        }
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
    

    fun hideBottomNav() {
        val cardBottomNav = findViewById<View>(R.id.cardBottomNav) ?: return
        if (!isBottomNavHidden) {
            isBottomNavHidden = true
            val height = if (cardBottomNav.height > 0) cardBottomNav.height.toFloat() else (72 * resources.displayMetrics.density)
            val distance = height + (32 * resources.displayMetrics.density)
            cardBottomNav.animate()
                .translationY(distance)
                .setDuration(280)
                .setInterpolator(android.view.animation.AccelerateInterpolator())
                .start()
        }
    }

    fun showBottomNav() {
        val cardBottomNav = findViewById<View>(R.id.cardBottomNav) ?: return
        if (isBottomNavHidden) {
            isBottomNavHidden = false
            cardBottomNav.animate()
                .translationY(0f)
                .setDuration(280)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }
    }

    private inner class DashboardPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        private val fragmentMap = mutableMapOf<Int, Fragment>()

        override fun getItemCount(): Int = 6 // 6 Nav Items

        override fun createFragment(position: Int): Fragment {
            val frag = when (position) {
                0 -> BerandaFragment()
                1 -> AkademikFragment()
                2 -> CbtFragment()
                3 -> PresensiFragment()
                4 -> KeuanganFragment()
                5 -> ProfilFragment()
                else -> BerandaFragment()
            }
            fragmentMap[position] = frag
            return frag
        }

        fun getFragment(position: Int): Fragment? {
            return fragmentMap[position] ?: supportFragmentManager.findFragmentByTag("f$position")
        }
    }

    private fun sinkronisasiFcmToken() {
        try {
            com.google.firebase.messaging.FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful && !task.result.isNullOrEmpty()) {
                    val fcmToken = task.result
                    val deviceId = android.provider.Settings.Secure.getString(contentResolver, android.provider.Settings.Secure.ANDROID_ID) ?: "UNKNOWN"
                    val deviceName = "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}"

                    var lat: String? = null
                    var lon: String? = null
                    try {
                        if (ContextCompat.checkSelfPermission(this@DashboardActivity, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(this@DashboardActivity, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            val lm = getSystemService(Context.LOCATION_SERVICE) as? android.location.LocationManager
                            val loc = lm?.getLastKnownLocation(android.location.LocationManager.NETWORK_PROVIDER)
                                ?: lm?.getLastKnownLocation(android.location.LocationManager.GPS_PROVIDER)
                            if (loc != null && loc.latitude != 0.0 && loc.longitude != 0.0) {
                                lat = loc.latitude.toString()
                                lon = loc.longitude.toString()
                            }
                        }
                    } catch (_: Exception) {}

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            ApiClient.instance.registerFcmToken(fcmToken, deviceId, deviceName, lat, lon)
                        } catch (e: Exception) {
                            android.util.Log.e("Dashboard", "Sync FCM Error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("Dashboard", "FCM getInstance Error: ${e.message}")
        }
    }
}

