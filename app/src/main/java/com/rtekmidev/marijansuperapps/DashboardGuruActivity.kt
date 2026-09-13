package com.rtekmidev.marijansuperapps

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
import com.rtekmidev.marijansuperapps.api.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardGuruActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private val PERMISSION_REQUEST_CODE = 1001
    private val NOTIF_PERMISSION_REQUEST_CODE = 1011
    private var dialogWajibNotif: android.app.Dialog? = null
    private var isBottomNavHidden = false

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

        // Cek dan wajibkan izin notifikasi aktif agar aplikasi bisa digunakan
        cekDanTampilkanWajibNotifikasi()

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
            sinkronisasiFcmToken()
            com.rtekmidev.marijansuperapps.service.DeviceLocationScheduler.jadwalkanPeriodicUpdate(this)
        }

        // 🔔 Minta izin notifikasi runtime di Android 13+ (API 33+)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1013)
            }
        }

        viewPager = findViewById(R.id.viewPager)
        viewPager.isUserInputEnabled = true
        viewPager.offscreenPageLimit = 3
        val pagerAdapter = DashboardGuruPagerAdapter(this)
        viewPager.adapter = pagerAdapter

        val navBeranda = findViewById<LinearLayout>(R.id.navBeranda)
        val navPresensi = findViewById<LinearLayout>(R.id.navPresensi)
        val navAkademik = findViewById<LinearLayout>(R.id.navAkademik)
        val navProfil = findViewById<LinearLayout>(R.id.navProfil)

        val ivBeranda = findViewById<ImageView>(R.id.ivNavBeranda)
        val ivPresensi = findViewById<ImageView>(R.id.ivNavPresensi)
        val ivAkademik = findViewById<ImageView>(R.id.ivNavAkademik)
        val ivProfil = findViewById<ImageView>(R.id.ivNavProfil)

        val tvBeranda = findViewById<TextView>(R.id.tvNavBeranda)
        val tvPresensi = findViewById<TextView>(R.id.tvNavPresensi)
        val tvAkademik = findViewById<TextView>(R.id.tvNavAkademik)
        val tvProfil = findViewById<TextView>(R.id.tvNavProfil)

        val headerTitleColor = if (isNightMode) android.graphics.Color.parseColor("#FFFFFF") else android.graphics.Color.parseColor("#1A1B41")
        val headerSubtextColor = if (isNightMode) android.graphics.Color.parseColor("#D1D5DB") else android.graphics.Color.parseColor("#6B7280")
        
        val btnRefreshTop = findViewById<View>(R.id.btnRefreshTop)
        val ivRefreshTop = findViewById<ImageView>(R.id.ivRefreshTop)

        btnRefreshTop?.setOnClickListener {
            ivRefreshTop?.animate()?.rotationBy(360f)?.setDuration(600)?.start()
            Toast.makeText(this, "Memperbarui data...", Toast.LENGTH_SHORT).show()

            val currentFrag = (viewPager.adapter as? DashboardGuruPagerAdapter)?.getFragment(viewPager.currentItem)
                ?: supportFragmentManager.findFragmentByTag("f" + viewPager.currentItem)

            if (currentFrag is com.rtekmidev.marijansuperapps.util.RefreshableFragment) {
                currentFrag.refreshData()
            }

            // Perbarui avatar / inisial header guru
            val ivProfil = findViewById<ImageView>(R.id.ivProfilPhoto)
            val tvInisial = findViewById<TextView>(R.id.tvProfilInisial)
            val cvProfil = findViewById<androidx.cardview.widget.CardView>(R.id.cvProfilPic)
            val sharedPref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
            val nama = sharedPref.getString("nama", "Guru")
            val foto = sharedPref.getString("foto_profil", null)
            val fullUrl = if (!foto.isNullOrEmpty()) {
                if (foto.startsWith("http")) foto else "https://mariyadhuljannahsubang.sch.id/uploads/guru/$foto"
            } else null
            com.rtekmidev.marijansuperapps.util.AvatarHelper.setAvatar(this, nama, fullUrl, ivProfil, tvInisial, cvProfil)
        }

        findViewById<View>(R.id.cvProfilPic)?.setOnClickListener {
            viewPager.setCurrentItem(3, false) // Profil tab
            showBottomNav()
        }

        val density = resources.displayMetrics.density
        fun dp(v: Int): Int = (v * density).toInt()

        val bottomNavContainer = findViewById<ViewGroup>(R.id.bottomNavigation)
        val cardBottomNav = findViewById<androidx.cardview.widget.CardView>(R.id.cardBottomNav)
        if (isNightMode) {
            cardBottomNav?.setCardBackgroundColor(android.graphics.Color.parseColor("#1E293B"))
        } else {
            cardBottomNav?.setCardBackgroundColor(android.graphics.Color.parseColor("#FFFFFF"))
        }

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
                Triple(navPresensi, ivPresensi, tvPresensi),
                Triple(navAkademik, ivAkademik, tvAkademik),
                Triple(navProfil, ivProfil, tvProfil)
            )

            val inactiveColor = if (isNightMode) android.graphics.Color.parseColor("#64748B") else android.graphics.Color.parseColor("#94A3B8")

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
                    iv?.setColorFilter(inactiveColor)
                    tv?.visibility = View.GONE
                }
            }
        }

        val initialPos = intent.getIntExtra("NAV_POSITION", 0)
        if (initialPos in 0..3) {
            viewPager.currentItem = initialPos
            updateNavSelection(initialPos, animate = false)
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
        navPresensi.setOnClickListener { viewPager.setCurrentItem(1, false); showBottomNav() }
        navAkademik.setOnClickListener { viewPager.setCurrentItem(2, false); showBottomNav() }
        navProfil.setOnClickListener { viewPager.setCurrentItem(3, false); showBottomNav() }

        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (viewPager.currentItem != 0) {
                    viewPager.currentItem = 0
                    showBottomNav()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val targetPos = intent.getIntExtra("NAV_POSITION", -1)
        if (targetPos in 0..3) {
            viewPager.currentItem = targetPos
        }
    }

    override fun onResume() {
        super.onResume()
        // Validasi status izin notifikasi setiap kali kembali ke aplikasi
        cekDanTampilkanWajibNotifikasi()
    }

    override fun onDestroy() {
        super.onDestroy()
        dialogWajibNotif?.dismiss()
        dialogWajibNotif = null
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == NOTIF_PERMISSION_REQUEST_CODE) {
            val isGranted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            if (isGranted) {
                dialogWajibNotif?.dismiss()
                dialogWajibNotif = null
                Toast.makeText(this, "Izin notifikasi aktif. Pengingat KBM siap!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Izin notifikasi wajib aktif untuk menggunakan aplikasi.", Toast.LENGTH_LONG).show()
                PengingatMengajarManager.bukaPengaturanNotifikasiAplikasi(this)
            }
        }
    }

    private fun cekDanTampilkanWajibNotifikasi() {
        val isGranted = PengingatMengajarManager.apakahNotifikasiDiizinkan(this)
        if (!isGranted) {
            if (dialogWajibNotif?.isShowing == true) return

            val dialog = android.app.Dialog(this).apply {
                requestWindowFeature(android.view.Window.FEATURE_NO_TITLE)
                setContentView(R.layout.dialog_wajib_izin_notifikasi)
                setCancelable(false)
                setCanceledOnTouchOutside(false)
                window?.setBackgroundDrawable(android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT))
                window?.setLayout(
                    (resources.displayMetrics.widthPixels * 0.90).toInt(),
                    android.view.ViewGroup.LayoutParams.WRAP_CONTENT
                )

                setOnKeyListener { _, keyCode, event ->
                    if (keyCode == android.view.KeyEvent.KEYCODE_BACK && event.action == android.view.KeyEvent.ACTION_UP) {
                        finishAffinity()
                        true
                    } else false
                }

                findViewById<View>(R.id.btnAktifkanIzinNotif)?.setOnClickListener {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                        PengingatMengajarManager.mintaIzinNotifikasiRuntime(
                            this@DashboardGuruActivity,
                            NOTIF_PERMISSION_REQUEST_CODE
                        )
                    } else {
                        PengingatMengajarManager.bukaPengaturanNotifikasiAplikasi(this@DashboardGuruActivity)
                    }
                }

                findViewById<View>(R.id.btnBukaPengaturanHp)?.setOnClickListener {
                    PengingatMengajarManager.bukaPengaturanNotifikasiAplikasi(this@DashboardGuruActivity)
                }

                findViewById<View>(R.id.btnKeluarAplikasi)?.setOnClickListener {
                    dismiss()
                    finishAffinity()
                }
            }
            dialogWajibNotif = dialog
            dialog.show()
        } else {
            dialogWajibNotif?.dismiss()
            dialogWajibNotif = null
        }
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

    fun hideBottomNav() {
        val cardBottomNav = findViewById<View>(R.id.cardBottomNav) ?: return
        if (!isBottomNavHidden) {
            isBottomNavHidden = true
            val height = if (cardBottomNav.height > 0) cardBottomNav.height.toFloat() else (72 * resources.displayMetrics.density)
            cardBottomNav.animate()
                .translationY(height + (32 * resources.displayMetrics.density))
                .setDuration(250)
                .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                .start()
        }
    }

    fun showBottomNav() {
        val cardBottomNav = findViewById<View>(R.id.cardBottomNav) ?: return
        if (isBottomNavHidden) {
            isBottomNavHidden = false
            cardBottomNav.animate()
                .translationY(0f)
                .setDuration(250)
                .setInterpolator(android.view.animation.AccelerateDecelerateInterpolator())
                .start()
        }
    }

    private inner class DashboardGuruPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        private val fragmentMap = mutableMapOf<Int, Fragment>()

        override fun getItemCount(): Int = 4

        override fun createFragment(position: Int): Fragment {
            val frag = when (position) {
                0 -> BerandaGuruFragment()
                1 -> PresensiGuruFragment()
                2 -> AkademikGuruFragment()
                3 -> ProfilGuruFragment()
                else -> BerandaGuruFragment()
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
                        if (androidx.core.content.ContextCompat.checkSelfPermission(this@DashboardGuruActivity, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED ||
                            androidx.core.content.ContextCompat.checkSelfPermission(this@DashboardGuruActivity, android.Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
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
                            android.util.Log.e("DashboardGuru", "Sync FCM Error: ${e.message}")
                        }
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("DashboardGuru", "FCM getInstance Error: ${e.message}")
        }
    }
}