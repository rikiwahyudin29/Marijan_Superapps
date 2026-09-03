package com.rtekmidev.marijancbt

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PresensiFragment : Fragment() {

    private var identifier = ""
    private var userRole = "SISWA"
    private var isSubmitting = false

    private var schoolLat = 0.0
    private var schoolLng = 0.0
    private var schoolRadius = 0
    private var isWithinRadius = false
    private var currentJarak = 0

    private lateinit var locationManager: LocationManager
    private lateinit var rootView: View
    private lateinit var mapWebView: WebView
    private var isMapLoaded = false

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            prosesAbsenQR(result.contents)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val locGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (locGranted) {
            mulaiMonitoringLokasi()
        } else {
            updateUIOffline("Izin GPS Ditolak!")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        rootView = inflater.inflate(R.layout.activity_presensi, container, false)
        return rootView
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Set Date
        val sdf = SimpleDateFormat("EEEE, dd MMMM yyyy", java.util.Locale.Builder().setLanguage("id").setRegion("ID").build())
        view.findViewById<TextView>(R.id.tvDate).text = sdf.format(Date())

        // Setup WebView Leaflet
        mapWebView = view.findViewById(R.id.mapWebView)
        mapWebView.settings.javaScriptEnabled = true
                mapWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapLoaded = true
                // Force update location once map is loaded (if permission is already granted)
                context?.let { ctx ->
                    if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        try {
                            val lastLoc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                                ?: locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                            if (lastLoc != null) {
                                updateVisualRadius(lastLoc)
                            }
                        } catch (e: Exception) {
                            // Ignore
                        }
                    }
                }
            }
        }
        mapWebView.loadUrl("file:///android_asset/leaflet_map.html")

        userRole = requireActivity().intent.getStringExtra("ROLE") ?: "SISWA"
        if (userRole == "GURU") {
            val pref = requireActivity().getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
            identifier = pref.getString("id_user", "") ?: ""
            if (identifier.isEmpty()) {
                identifier = pref.getString("username", "") ?: ""
            }
        } else {
            val pref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
            identifier = pref.getString("nisn", "") ?: ""
        }

        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager



        muatSettingSekolah()
        muatStatistikPresensi()

        view.findViewById<CardView>(R.id.btnMulaiAbsen).setOnClickListener {
            if (isSubmitting) return@setOnClickListener

            if (schoolLat == 0.0) {
                Toast.makeText(requireContext(), "Setting sekolah belum berhasil dimuat. Tunggu sebentar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isWithinRadius) {
                val intent = Intent(requireContext(), IzinActivity::class.java)
                intent.putExtra("ROLE", userRole)
                startActivity(intent)
                requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                return@setOnClickListener
            }
            bukaScannerQR()
        }

        view.findViewById<CardView>(R.id.btnAjukanIzin).setOnClickListener {
            val intent = Intent(requireContext(), IzinActivity::class.java)
            intent.putExtra("ROLE", userRole)
            startActivity(intent)
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
        
        view.findViewById<CardView>(R.id.btnLihatRekap).setOnClickListener {
            val intent = Intent(requireContext(), RekapActivity::class.java)
            startActivity(intent)
            requireActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
        }
    }

    private fun muatStatistikPresensi() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                if (userRole == "GURU") {
                    val currentMonth = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                    val resp = ApiClient.instance.getRekapGuru(identifier, currentMonth)
                    withContext(Dispatchers.Main) {
                        if (resp.isSuccessful) {
                            val bodyObj = resp.body()
                            if (bodyObj != null) {
                                val sum = bodyObj.summary
                                val total = (sum?.hadir ?: 0) + (sum?.alpha ?: 0) + (sum?.sakit ?: 0) + (sum?.izin ?: 0)
                                val percentage = if (total > 0) (((sum?.hadir?.toFloat() ?: 0f) / total.toFloat()) * 100).toInt() else 0
                                rootView.findViewById<TextView>(R.id.tvTotalKehadiran).text = "${percentage}%"
                                rootView.findViewById<TextView>(R.id.tvStatHadir).text = (sum?.hadir ?: 0).toString()
                                rootView.findViewById<TextView>(R.id.tvStatAlfa).text = (sum?.alpha ?: 0).toString()
                                rootView.findViewById<TextView>(R.id.tvStatSakit).text = (sum?.sakit ?: 0).toString()
                                rootView.findViewById<TextView>(R.id.tvStatTerlambat).text = (sum?.terlambat ?: 0).toString()
                            }
                        } else {
                            context?.let { Toast.makeText(it, "API Stat Error: ${resp.code()}", Toast.LENGTH_SHORT).show() }
                        }
                    }
                } else {
                    val resp = ApiClient.instance.getRiwayatAbsen(identifier)
                    withContext(Dispatchers.Main) {
                        if (resp.isSuccessful) {
                            val jsonBody = resp.body()
                            if (jsonBody == null) {
                                context?.let { Toast.makeText(it, "Data stat null", Toast.LENGTH_SHORT).show() }
                            } else if (jsonBody.isJsonArray) {
                                // FALLBACK: If backend still returns Array instead of Object
                                val array = jsonBody.asJsonArray
                                var hadir = 0; var alfa = 0; var sakit = 0; var izin = 0; var terlambat = 0
                                for (i in 0 until array.size()) {
                                    val item = array[i].asJsonObject
                                    val statusKehadiran = item.get("status_kehadiran")?.asString?.lowercase() ?: ""
                                    when (statusKehadiran) {
                                        "hadir" -> hadir++
                                        "alfa" -> alfa++
                                        "sakit" -> sakit++
                                        "izin" -> izin++
                                        "terlambat" -> terlambat++
                                    }
                                }
                                val total = hadir + alfa + sakit + izin + terlambat
                                val percentage = if (total > 0) ((hadir.toFloat() / total.toFloat()) * 100).toInt() else 0
                                rootView.findViewById<TextView>(R.id.tvTotalKehadiran).text = "${percentage}%"
                                rootView.findViewById<TextView>(R.id.tvStatHadir).text = hadir.toString()
                                rootView.findViewById<TextView>(R.id.tvStatAlfa).text = alfa.toString()
                                rootView.findViewById<TextView>(R.id.tvStatSakit).text = sakit.toString()
                                rootView.findViewById<TextView>(R.id.tvStatTerlambat).text = terlambat.toString()
                            } else if (jsonBody.isJsonObject) {
                                val jsonObj = jsonBody.asJsonObject
                                val dataElement = jsonObj.get("data")
                                if (dataElement != null && dataElement.isJsonObject) {
                                    // NEW FORMAT: Object with pre-calculated stats
                                    val dataObj = dataElement.asJsonObject
                                    val tPercent = dataObj.get("total_percentage")?.asInt ?: 0
                                    val tHadir = dataObj.get("hadir")?.asInt ?: 0
                                    val tAlfa = dataObj.get("alfa")?.asInt ?: 0
                                    val tSakit = dataObj.get("sakit")?.asInt ?: 0
                                    val tTerlambat = dataObj.get("terlambat")?.asInt ?: 0
                                    rootView.findViewById<TextView>(R.id.tvTotalKehadiran).text = "${tPercent}%"
                                    rootView.findViewById<TextView>(R.id.tvStatHadir).text = tHadir.toString()
                                    rootView.findViewById<TextView>(R.id.tvStatAlfa).text = tAlfa.toString()
                                    rootView.findViewById<TextView>(R.id.tvStatSakit).text = tSakit.toString()
                                    rootView.findViewById<TextView>(R.id.tvStatTerlambat).text = tTerlambat.toString()
                                } else if (dataElement != null && dataElement.isJsonArray) {
                                    // OLD FORMAT: Array wrapped in "data"
                                    val array = dataElement.asJsonArray
                                    var hadir = 0; var alfa = 0; var sakit = 0; var izin = 0; var terlambat = 0
                                    for (i in 0 until array.size()) {
                                        val item = array[i].asJsonObject
                                        val statusKehadiran = (item.get("status_kehadiran") ?: item.get("status"))?.asString?.lowercase()?.trim() ?: ""
                                        when (statusKehadiran) {
                                            "hadir" -> hadir++
                                            "alfa", "alpha" -> alfa++
                                            "sakit" -> sakit++
                                            "izin" -> izin++
                                            "terlambat" -> terlambat++
                                        }
                                    }
                                    val total = hadir + alfa + sakit + izin + terlambat
                                    val percentage = if (total > 0) ((hadir.toFloat() / total.toFloat()) * 100).toInt() else 0
                                    rootView.findViewById<TextView>(R.id.tvTotalKehadiran).text = "${percentage}%"
                                    rootView.findViewById<TextView>(R.id.tvStatHadir).text = hadir.toString()
                                    rootView.findViewById<TextView>(R.id.tvStatAlfa).text = alfa.toString()
                                    rootView.findViewById<TextView>(R.id.tvStatSakit).text = sakit.toString()
                                    rootView.findViewById<TextView>(R.id.tvStatTerlambat).text = terlambat.toString()
                                } else {
                                    context?.let { Toast.makeText(it, "Data JSON tidak lengkap atau salah format", Toast.LENGTH_SHORT).show() }
                                }
                            }
                        } else {
                            context?.let { Toast.makeText(it, "API Stat Error: ${resp.code()}", Toast.LENGTH_SHORT).show() }
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    android.util.Log.e("PresensiStat", "Error muat stat", e)
                    context?.let { Toast.makeText(it, "Gagal muat stat: $e", Toast.LENGTH_SHORT).show() }
                }
            }
        }
    }

    private fun muatSettingSekolah() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.getSettingPresensi()
                withContext(Dispatchers.Main) {
                    val statusStr = resp.body()?.status?.asString; if (resp.isSuccessful && (statusStr == "success" || statusStr == "true")) {
                        val d = resp.body()?.data
                        schoolLat = d?.latitude?.toDouble() ?: 0.0
                        schoolLng = d?.longitude?.toDouble() ?: 0.0
                        schoolRadius = d?.radius ?: 100
                    } else {
                        context?.let { Toast.makeText(it, "Gagal memuat koordinat sekolah", Toast.LENGTH_SHORT).show() }
                    }
                    cekIzinDanMonitor()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    context?.let { Toast.makeText(it, "Koneksi ke server gagal. Pastikan API jalan.", Toast.LENGTH_SHORT).show() }
                    cekIzinDanMonitor()
                }
            }
        }
    }

    private fun cekIzinDanMonitor() {
        val ctx = context ?: return
        if (ActivityCompat.checkSelfPermission(ctx, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            mulaiMonitoringLokasi()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA))
        }
    }

    @SuppressLint("MissingPermission")
    private fun mulaiMonitoringLokasi() {
        val tvStatus = rootView.findViewById<TextView>(R.id.tvStatusRadius)

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
        val tvStatus = rootView.findViewById<TextView>(R.id.tvStatusRadius)
        val dot = rootView.findViewById<View>(R.id.indicatorDot)
        val btnActionText = rootView.findViewById<TextView>(R.id.tvBtnActionText)

        // Update Map Marker via JavaScript
        if (isMapLoaded) {
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
        val tvStatus = rootView.findViewById<TextView>(R.id.tvStatusRadius)
        tvStatus.text = msg
        tvStatus.setTextColor(Color.RED)
        setDotColor(rootView.findViewById(R.id.indicatorDot), "#D32F2F")
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
            Toast.makeText(requireContext(), "Gagal: Data Sesi Tidak Lengkap ()", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        val lat = location?.latitude?.toString() ?: "0.0"
        val lng = location?.longitude?.toString() ?: "0.0"

        Toast.makeText(requireContext(), "Mengirim Data Absensi...", Toast.LENGTH_SHORT).show()

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
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
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
                        context?.let { Toast.makeText(it, errorMsg, Toast.LENGTH_LONG).show() }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSubmitting = false
                    Toast.makeText(requireContext(), "Gagal koneksi ke server", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}














