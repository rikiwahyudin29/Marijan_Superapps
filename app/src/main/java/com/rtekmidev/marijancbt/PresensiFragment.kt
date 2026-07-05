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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        userRole = requireActivity().intent.getStringExtra("ROLE") ?: "SISWA"
        if (userRole == "GURU") {
            val pref = requireActivity().getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
            identifier = pref.getString("id_user", "") ?: ""
        } else {
            val pref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
            identifier = pref.getString("nisn", "") ?: ""
        }

        locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager

        view.findViewById<ImageView>(R.id.btnBack).setOnClickListener { 
            // In a fragment, back could mean switching to Beranda
        }

        muatSettingSekolah()

        view.findViewById<CardView>(R.id.btnMulaiAbsen).setOnClickListener {
            if (isSubmitting) return@setOnClickListener

            if (schoolLat == 0.0) {
                Toast.makeText(requireContext(), "Setting sekolah belum berhasil dimuat. Tunggu sebentar.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!isWithinRadius) {
                val selisih = currentJarak - schoolRadius
                if (selisih > 0) {
                    Toast.makeText(requireContext(), "Gagal: Anda berada $selisih meter di luar jangkauan (Jarak Anda: $currentJarak m, Maks: $schoolRadius m).", Toast.LENGTH_LONG).show()
                } else {
                    Toast.makeText(requireContext(), "Gagal: Anda masih di luar radius sekolah!", Toast.LENGTH_LONG).show()
                }
                return@setOnClickListener
            }
            bukaScannerQR()
        }

        view.findViewById<CardView>(R.id.btnAjukanIzin).setOnClickListener {
            val intent = Intent(requireContext(), IzinActivity::class.java)
            intent.putExtra("ROLE", userRole)
            startActivity(intent)
        }

        view.findViewById<CardView>(R.id.btnLihatRekap).setOnClickListener {
            val intent = Intent(requireContext(), RekapActivity::class.java)
            startActivity(intent)
        }
    }

    private fun muatSettingSekolah() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.getSettingPresensi()
                withContext(Dispatchers.Main) {
                    if (resp.isSuccessful && resp.body()?.status == true) {
                        val d = resp.body()?.data
                        schoolLat = d?.latitude?.toDouble() ?: 0.0
                        schoolLng = d?.longitude?.toDouble() ?: 0.0
                        schoolRadius = d?.radius ?: 100
                    } else {
                        Toast.makeText(requireContext(), "Gagal memuat koordinat sekolah", Toast.LENGTH_SHORT).show()
                    }
                    cekIzinDanMonitor()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Koneksi ke server gagal. Pastikan API jalan.", Toast.LENGTH_SHORT).show()
                    cekIzinDanMonitor()
                }
            }
        }
    }

    private fun cekIzinDanMonitor() {
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
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

        if (schoolLat == 0.0) {
            tvStatus.text = "Menunggu data sekolah..."
            tvStatus.setTextColor(Color.parseColor("#F57C00"))
            setDotColor(dot, "#F57C00")
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
            return
        }

        if (jarak <= schoolRadius) {
            isWithinRadius = true
            tvStatus.text = "Dalam Radius ($jarak m)"
            tvStatus.setTextColor(Color.parseColor("#1DA748"))
            setDotColor(dot, "#1DA748")
        } else {
            isWithinRadius = false
            tvStatus.text = "Luar Radius ($jarak m / Maks: $schoolRadius m)"
            tvStatus.setTextColor(Color.RED)
            setDotColor(dot, "#D32F2F")
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
            Toast.makeText(requireContext(), "Gagal: Data Sesi Tidak Lengkap ($userRole)", Toast.LENGTH_SHORT).show()
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

                        Toast.makeText(requireContext(), "Gagal: $errorMsg", Toast.LENGTH_LONG).show()
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
