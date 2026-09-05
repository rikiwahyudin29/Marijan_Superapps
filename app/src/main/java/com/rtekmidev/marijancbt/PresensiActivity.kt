package com.rtekmidev.marijancbt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanIntentResult
import com.journeyapps.barcodescanner.ScanOptions
import com.rtekmidev.marijancbt.api.ApiClient
import android.content.Intent
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PresensiActivity : AppCompatActivity() {

    // 🔥 Variabel Penentu Hybrid (Guru / Siswa)
    private var identifier = "" // Bisa berisi NISN atau ID_USER
    private var userRole = "SISWA"

    private var isSubmitting = false

    // Data Lokasi Sekolah
    private var schoolLat = 0.0
    private var schoolLng = 0.0
    private var schoolRadius = 0
    private var isWithinRadius = false
    private var currentJarak = 0

    private lateinit var locationManager: LocationManager
    private lateinit var mapWebView: WebView

    // 1. SCANNER QR
    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result: ScanIntentResult ->
        if (result.contents != null) {
            prosesAbsenQR(result.contents)
        }
    }

    // 2. PERMISSION LAUNCHER
    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val locGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locGranted) {
            mulaiMonitoringLokasi()
        } else {
            updateUIOffline("Izin GPS Ditolak!")
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_presensi)

        // Set Date
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.Builder().setLanguage("id").setRegion("ID").build())
        findViewById<TextView>(R.id.tvDate).text = sdf.format(Date())

        // Setup WebView Leaflet
        mapWebView = findViewById(R.id.mapWebView)
        mapWebView.settings.javaScriptEnabled = true
        mapWebView.webViewClient = WebViewClient()
        mapWebView.loadUrl("file:///android_asset/leaflet_map.html")

        // 🔥 LOGIKA HYBRID BACA SESI 🔥
        userRole = intent.getStringExtra("ROLE") ?: "SISWA"
        if (userRole == "GURU") {
            val pref = getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
            identifier = pref.getString("id_user", "") ?: ""
        } else {
            val pref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
            identifier = pref.getString("nisn", "") ?: ""
        }

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager



        // Ambil Setting Sekolah dari API
        muatSettingSekolah()

        // Tombol Main Action
        findViewById<CardView>(R.id.btnMulaiAbsen).setOnClickListener {
            if (isSubmitting) return@setOnClickListener

            if (schoolLat == 0.0) {
                Toast.makeText(this, "Setting sekolah belum berhasil dimuat. Tunggu sebentar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isWithinRadius) {
                val intent = Intent(this, IzinActivity::class.java)
                intent.putExtra("ROLE", userRole)
                startActivity(intent)
                applyEnterTransition()
                return@setOnClickListener
            }

            bukaScannerQR()
        }
        
        findViewById<CardView>(R.id.btnAjukanIzin)?.setOnClickListener {
            val intent = Intent(this, IzinActivity::class.java)
            intent.putExtra("ROLE", userRole)
            startActivity(intent)
            applyEnterTransition()
        }
        
        findViewById<CardView>(R.id.btnLihatRekap)?.setOnClickListener {
            val intent = Intent(this, RekapActivity::class.java)
            startActivity(intent)
            applyEnterTransition()
        }
    }

    private fun muatSettingSekolah() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.getSettingPresensi()
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && ((resp.body()?.status?.isJsonPrimitive == true && resp.body()?.status?.asBoolean == true) || resp.body()?.status?.asString == "success" || resp.body()?.status?.asString == "true")) {
                        val d = resp.body()?.data
                        schoolLat = d?.latitude?.toDouble() ?: 0.0
                        schoolLng = d?.longitude?.toDouble() ?: 0.0
                        schoolRadius = d?.radius ?: 100
                    } else {
                        Toast.makeText(this@PresensiActivity, "Gagal memuat koordinat sekolah", Toast.LENGTH_SHORT).show()
                    }
                    cekIzinDanMonitor()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PresensiActivity, "Koneksi ke server gagal. Pastikan API jalan.", Toast.LENGTH_SHORT).show()
                    cekIzinDanMonitor()
                }
            }
        }
    }

    private fun cekIzinDanMonitor() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mulaiMonitoringLokasi()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA))
        }
    }

    @SuppressLint("MissingPermission")
    private fun mulaiMonitoringLokasi() {
        val tvStatus = findViewById<TextView>(R.id.tvStatusRadius)

        val lastLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)

        if (lastLoc != null) {
            updateVisualRadius(lastLoc)
        } else {
            tvStatus.text = "Mengunci Sinyal GPS..."
        }

        val locationListener = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                updateVisualRadius(location)
            }
            override fun onProviderDisabled(provider: String) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        }

        if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000L, 0f, locationListener)
        }
        if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000L, 0f, locationListener)
        }

        if (!locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) && !locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            updateUIOffline("GPS Anda Dimatikan!")
        }
    }

    private fun updateVisualRadius(loc: Location) {
        val tvStatus = findViewById<TextView>(R.id.tvStatusRadius)
        val dot = findViewById<View>(R.id.indicatorDot)
        val btnActionText = findViewById<TextView>(R.id.tvBtnActionText)

        // Update Map Marker via JavaScript
        if (schoolLat != 0.0) {
            val script = "updateLocation(${loc.latitude}, ${loc.longitude}, $schoolLat, $schoolLng, $schoolRadius);"
            mapWebView.evaluateJavascript(script, null)
        }

        if (schoolLat == 0.0) {
            tvStatus.text = "Menunggu data sekolah..."
            tvStatus.setTextColor(Color.parseColor("#F59E0B"))
            setDotColor(dot, "#F59E0B")
            isWithinRadius = false
            return
        }

        val results = FloatArray(1)
        Location.distanceBetween(loc.latitude, loc.longitude, schoolLat, schoolLng, results)
        currentJarak = results[0].toInt()
        val jarak = currentJarak

        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) loc.isMock else {
            @Suppress("DEPRECATION") loc.isFromMockProvider
        }

        if (isMock) {
            tvStatus.text = "TERDETEKSI FAKE GPS!"
            tvStatus.setTextColor(Color.RED)
            setDotColor(dot, "#D32F2F")
            isWithinRadius = false
            btnActionText.text = "Gagal (Fake GPS)"
            return
        }

        if (jarak <= schoolRadius) {
            isWithinRadius = true
            tvStatus.text = "Dalam Radius Kehadiran ( m)"
            tvStatus.setTextColor(Color.parseColor("#1DA748"))
            setDotColor(dot, "#1DA748")
            btnActionText.text = "Presensi Sekarang"
        } else {
            isWithinRadius = false
            tvStatus.text = "Luar Radius Kehadiran ( m / Maks:  m)"
            tvStatus.setTextColor(Color.RED)
            setDotColor(dot, "#D32F2F")
            btnActionText.text = "Ajukan Izin"
        }
    }

    private fun setDotColor(v: View, colorHex: String) {
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.OVAL
        shape.setColor(Color.parseColor(colorHex))
        v.background = shape
    }

    private fun updateUIOffline(msg: String) {
        val tvStatus = findViewById<TextView>(R.id.tvStatusRadius)
        tvStatus.text = msg
        tvStatus.setTextColor(Color.RED)
        setDotColor(findViewById(R.id.indicatorDot), "#D32F2F")
        isWithinRadius = false
    }

    private fun bukaScannerQR() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Arahkan ke QR Code Guru Piket")
        options.setBeepEnabled(true)
        options.setCaptureActivity(CustomScannerActivity::class.java)
        options.setOrientationLocked(true)
        barcodeLauncher.launch(options)
    }

    @SuppressLint("MissingPermission")
    private fun prosesAbsenQR(qrToken: String) {
        if (identifier.isEmpty()) {
            Toast.makeText(this, "Gagal: Data Sesi Tidak Lengkap ()", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        val lat = location?.latitude?.toString() ?: "0.0"
        val lng = location?.longitude?.toString() ?: "0.0"

        Toast.makeText(this, "Mengirim Data Absensi...", Toast.LENGTH_SHORT).show()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = if (userRole == "GURU") {
                    ApiClient.instance.submitAbsenGuru(identifier, lat, lng, qrToken)
                } else {
                    ApiClient.instance.submitAbsen(identifier, lat, lng, qrToken)
                }

                withContext(Dispatchers.Main) {
                    isSubmitting = false

                    if (response.isSuccessful) {
                        val msg = response.body()?.message ?: "Absen Berhasil!"
                        Toast.makeText(this@PresensiActivity, msg, Toast.LENGTH_LONG).show()
                        finish()

                    } else {
                        var errorMsg = "Gagal memproses absensi"
                        try {
                            val errorJson = response.errorBody()?.string()
                            if (errorJson != null) {
                                val jsonObject = org.json.JSONObject(errorJson)
                                errorMsg = jsonObject.getString("message")
                            }
                        } catch (e: Exception) {
                            errorMsg = "Gagal membaca pesan error dari server."
                        }

                        Toast.makeText(this@PresensiActivity, "Gagal: $errorMsg", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSubmitting = false
                    Toast.makeText(this@PresensiActivity, "Gagal koneksi ke server", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun finish() {
        super.finish()
        applyExitTransition()
    }
}

