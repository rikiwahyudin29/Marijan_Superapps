package com.rtekmidev.smkrjsuperapps

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
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
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.PortalPresensiSiswaResponse
import com.rtekmidev.smkrjsuperapps.api.PortalRiwayatItem
import com.rtekmidev.smkrjsuperapps.util.LoadingDialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.rtekmidev.smkrjsuperapps.util.RefreshableFragment

class PresensiFragment : Fragment(), LocationListener, RefreshableFragment {

    // Hero Banner
    private lateinit var tvPortalTahunAjaran: TextView
    private lateinit var tvPortalTanggal: TextView
    private lateinit var tvPortalNamaSiswa: TextView
    private lateinit var tvPortalNisnKelas: TextView
    private lateinit var chipSiswaAktif: LinearLayout
    private lateinit var chipJurusan: LinearLayout
    private lateinit var tvChipJurusanText: TextView
    private lateinit var tvPortalShiftNama: TextView
    private lateinit var tvPortalWaktuSistem: TextView

    // Presensi Hari Ini
    private lateinit var badgePortalRadius: LinearLayout
    private lateinit var dotPortalRadius: View
    private lateinit var tvPortalRadiusText: TextView
    private lateinit var tvPortalMasukBadge: TextView
    private lateinit var tvPortalJamMasuk: TextView
    private lateinit var ivPortalMasukIcon: ImageView
    private lateinit var tvPortalMasukSub: TextView
    private lateinit var tvPortalPulangBadge: TextView
    private lateinit var tvPortalJamPulang: TextView
    private lateinit var ivPortalPulangIcon: ImageView
    private lateinit var tvPortalPulangSub: TextView
    private lateinit var btnPortalAbsenUtama: LinearLayout
    private lateinit var ivPortalAbsenUtamaIcon: ImageView
    private lateinit var tvPortalAbsenUtamaText: TextView

    // Geofencing & Titik Lokasi
    private lateinit var tvPortalGpsAkuratBadge: TextView
    private lateinit var mapPortalWebView: WebView
    private lateinit var tvPortalMapSchoolBadge: TextView
    private lateinit var tvPortalNamaSekolah: TextView
    private lateinit var tvPortalAlamatJarak: TextView
    private lateinit var btnPortalRefreshGps: LinearLayout

    // Statistik Kehadiran Siswa
    private lateinit var tvPortalStatPeriode: TextView
    private lateinit var tvPortalStatPerformaBadge: TextView
    private lateinit var tvPortalStatPersen: TextView
    private lateinit var tvPortalStatHariKerja: TextView
    private lateinit var pbPortalStat: ProgressBar
    private lateinit var tvPortalStatHadir: TextView
    private lateinit var tvPortalStatTerlambat: TextView
    private lateinit var tvPortalStatIzinSakit: TextView
    private lateinit var tvPortalStatAlfa: TextView

    // Riwayat Presensi Terakhir
    private lateinit var btnPortalLihatKalender: TextView
    private lateinit var layoutPortalRiwayatContainer: LinearLayout
    private lateinit var tvPortalRiwayatEmpty: TextView

    // 2 Pintasan Bawah
    private lateinit var btnPortalPintasanKalender: CardView
    private lateinit var btnPortalPintasanIzin: CardView

    // Variabel Data & State
    private var clockJob: Job? = null
    private var schoolLat: Double = -6.5714
    private var schoolLng: Double = 107.7587
    private var schoolRadius: Int = 200
    private var isHariLibur: Boolean = false
    private var keteranganLiburHariIni: String = ""
    private var isCurrentlyInRadius: Boolean = false
    private var currentDistanceMeters: Int = 0
    private var userLat: Double = 0.0
    private var userLng: Double = 0.0
    private var isMapLoaded: Boolean = false
    private var isSubmitting: Boolean = false
    private var nisn: String = ""
    private var loadingDialog: Dialog? = null

    private var locationManager: LocationManager? = null

    private val barcodeLauncher = registerForActivityResult(ScanContract()) { result ->
        if (result.contents != null) {
            prosesAbsenQR(result.contents)
        }
    }

    private val permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (fineGranted || coarseGranted) {
            startLocationUpdates()
        } else {
            tvPortalGpsAkuratBadge.text = "GPS Izin Ditolak"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_presensi_siswa, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Cek hari libur akhir pekan lokal (Sabtu/Minggu) saat awal
        val cal = Calendar.getInstance()
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        if (dayOfWeek == Calendar.SUNDAY || dayOfWeek == Calendar.SATURDAY) {
            isHariLibur = true
            keteranganLiburHariIni = if (dayOfWeek == Calendar.SUNDAY) "Hari Minggu (Libur Akhir Pekan)" else "Hari Sabtu (Libur Akhir Pekan)"
        }

        initViews(view)
        setupMapWebView()
        setupListeners()
        loadLocalProfile()
        updateRadiusUI()
        startRealtimeClock()
        checkLocationPermissionAndStart()
        fetchPortalData()
    }

    override fun onResume() {
        super.onResume()
        if (nisn.isNotEmpty()) {
            fetchPortalData()
        }
    }

    override fun refreshData() {
        loadLocalProfile()
        fetchPortalData()
        checkLocationPermissionAndStart()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clockJob?.cancel()
        LoadingDialogHelper.dismiss(loadingDialog)
        loadingDialog = null
        try {
            locationManager?.removeUpdates(this)
        } catch (_: Exception) {}
    }

    private fun initViews(view: View) {
        // Hero
        tvPortalTahunAjaran = view.findViewById(R.id.tvPortalTahunAjaran)
        tvPortalTanggal = view.findViewById(R.id.tvPortalTanggal)
        tvPortalNamaSiswa = view.findViewById(R.id.tvPortalNamaSiswa)
        tvPortalNisnKelas = view.findViewById(R.id.tvPortalNisnKelas)
        chipSiswaAktif = view.findViewById(R.id.chipSiswaAktif)
        chipJurusan = view.findViewById(R.id.chipJurusan)
        tvChipJurusanText = view.findViewById(R.id.tvChipJurusanText)
        tvPortalShiftNama = view.findViewById(R.id.tvPortalShiftNama)
        tvPortalWaktuSistem = view.findViewById(R.id.tvPortalWaktuSistem)

        // Presensi Hari Ini
        badgePortalRadius = view.findViewById(R.id.badgePortalRadius)
        dotPortalRadius = view.findViewById(R.id.dotPortalRadius)
        tvPortalRadiusText = view.findViewById(R.id.tvPortalRadiusText)
        tvPortalMasukBadge = view.findViewById(R.id.tvPortalMasukBadge)
        tvPortalJamMasuk = view.findViewById(R.id.tvPortalJamMasuk)
        ivPortalMasukIcon = view.findViewById(R.id.ivPortalMasukIcon)
        tvPortalMasukSub = view.findViewById(R.id.tvPortalMasukSub)
        tvPortalPulangBadge = view.findViewById(R.id.tvPortalPulangBadge)
        tvPortalJamPulang = view.findViewById(R.id.tvPortalJamPulang)
        ivPortalPulangIcon = view.findViewById(R.id.ivPortalPulangIcon)
        tvPortalPulangSub = view.findViewById(R.id.tvPortalPulangSub)
        btnPortalAbsenUtama = view.findViewById(R.id.btnPortalAbsenUtama)
        ivPortalAbsenUtamaIcon = view.findViewById(R.id.ivPortalAbsenUtamaIcon)
        tvPortalAbsenUtamaText = view.findViewById(R.id.tvPortalAbsenUtamaText)

        // Geofencing
        tvPortalGpsAkuratBadge = view.findViewById(R.id.tvPortalGpsAkuratBadge)
        mapPortalWebView = view.findViewById(R.id.mapPortalWebView)
        tvPortalMapSchoolBadge = view.findViewById(R.id.tvPortalMapSchoolBadge)
        tvPortalNamaSekolah = view.findViewById(R.id.tvPortalNamaSekolah)
        tvPortalAlamatJarak = view.findViewById(R.id.tvPortalAlamatJarak)
        btnPortalRefreshGps = view.findViewById(R.id.btnPortalRefreshGps)

        // Statistik
        tvPortalStatPeriode = view.findViewById(R.id.tvPortalStatPeriode)
        tvPortalStatPerformaBadge = view.findViewById(R.id.tvPortalStatPerformaBadge)
        tvPortalStatPersen = view.findViewById(R.id.tvPortalStatPersen)
        tvPortalStatHariKerja = view.findViewById(R.id.tvPortalStatHariKerja)
        pbPortalStat = view.findViewById(R.id.pbPortalStat)
        tvPortalStatHadir = view.findViewById(R.id.tvPortalStatHadir)
        tvPortalStatTerlambat = view.findViewById(R.id.tvPortalStatTerlambat)
        tvPortalStatIzinSakit = view.findViewById(R.id.tvPortalStatIzinSakit)
        tvPortalStatAlfa = view.findViewById(R.id.tvPortalStatAlfa)

        // Riwayat
        btnPortalLihatKalender = view.findViewById(R.id.btnPortalLihatKalender)
        layoutPortalRiwayatContainer = view.findViewById(R.id.layoutPortalRiwayatContainer)
        tvPortalRiwayatEmpty = view.findViewById(R.id.tvPortalRiwayatEmpty)

        // Pintasan
        btnPortalPintasanKalender = view.findViewById(R.id.btnPortalPintasanKalender)
        btnPortalPintasanIzin = view.findViewById(R.id.btnPortalPintasanIzin)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun setupMapWebView() {
        mapPortalWebView.settings.javaScriptEnabled = true
        mapPortalWebView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                isMapLoaded = true
                updateMapLocation()
            }
        }
        mapPortalWebView.loadUrl("file:///android_asset/leaflet_map.html")
    }

    private fun loadLocalProfile() {
        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisn = sharedPref.getString("nisn", "") ?: ""
        val namaSiswa = sharedPref.getString("nama", "Siswa") ?: "Siswa"
        val namaKelas = sharedPref.getString("nama_kelas", "") ?: ""
        val namaJurusan = sharedPref.getString("jurusan", "") ?: ""

        tvPortalNamaSiswa.text = namaSiswa
        tvPortalNisnKelas.text = if (nisn.isNotEmpty()) "NISN: $nisn • ${namaKelas.ifEmpty { "Siswa SMK RJ" }}" else "Peserta Didik SMK Riyadhul Jannah"
        if (namaJurusan.isNotEmpty()) {
            tvChipJurusanText.text = namaJurusan
        }

        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.Builder().setLanguage("id").setRegion("ID").build())
        tvPortalTanggal.text = sdf.format(Date())

        if (isHariLibur) {
            tvPortalMasukBadge.text = "Hari Libur"
            tvPortalMasukSub.text = keteranganLiburHariIni
            tvPortalPulangBadge.text = "Hari Libur"
            tvPortalPulangSub.text = "Tidak Ada Presensi"
        }
    }

    private fun startRealtimeClock() {
        clockJob?.cancel()
        clockJob = viewLifecycleOwner.lifecycleScope.launch {
            val sdf = SimpleDateFormat("HH:mm:ss 'WIB'", Locale.getDefault())
            while (isActive) {
                try {
                    tvPortalWaktuSistem.text = sdf.format(Date())
                } catch (_: Exception) {}
                delay(1000)
            }
        }
    }

    private fun setupListeners() {
        // Tombol Absen Utama
        btnPortalAbsenUtama.setOnClickListener {
            if (isSubmitting) return@setOnClickListener

            if (isHariLibur) {
                Toast.makeText(requireContext(), "Hari ini libur sekolah ($keteranganLiburHariIni)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isCurrentlyInRadius) {
                val intent = Intent(requireContext(), IzinActivity::class.java)
                intent.putExtra("ROLE", "SISWA")
                startActivity(intent)
                requireActivity().applyEnterTransition()
                return@setOnClickListener
            }
            bukaScannerQR()
        }

        // Tombol Refresh GPS
        btnPortalRefreshGps.setOnClickListener {
            Toast.makeText(requireContext(), "Memperbarui lokasi GPS...", Toast.LENGTH_SHORT).show()
            checkLocationPermissionAndStart()
        }

        // Link Lihat Kalender Presensi & Pintasan Kalender
        val openKalender = View.OnClickListener {
            val intent = Intent(requireContext(), RekapActivity::class.java)
            intent.putExtra("ROLE", "SISWA")
            startActivity(intent)
            requireActivity().applyEnterTransition()
        }
        btnPortalLihatKalender.setOnClickListener(openKalender)
        btnPortalPintasanKalender.setOnClickListener(openKalender)

        // Pintasan Ajukan Izin
        btnPortalPintasanIzin.setOnClickListener {
            val intent = Intent(requireContext(), IzinActivity::class.java)
            intent.putExtra("ROLE", "SISWA")
            startActivity(intent)
            requireActivity().applyEnterTransition()
        }

        // Auto-Hide Bottom Nav on Scroll
        (view as? NestedScrollView)?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 12) {
                (activity as? DashboardActivity)?.hideBottomNav()
            } else if (dy < -12 || scrollY <= 10) {
                (activity as? DashboardActivity)?.showBottomNav()
            }
        }
    }

    private fun checkLocationPermissionAndStart() {
        val fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            startLocationUpdates()
        } else {
            permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.CAMERA))
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        try {
            locationManager = requireActivity().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val locNet = locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val locGps = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val bestLoc = locGps ?: locNet

            if (bestLoc != null) {
                onLocationChanged(bestLoc)
            }

            locationManager?.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 3000L, 2f, this)
            locationManager?.requestLocationUpdates(LocationManager.GPS_PROVIDER, 3000L, 2f, this)
        } catch (_: Exception) {}
    }

    override fun onLocationChanged(location: Location) {
        val isMock = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) location.isMock else {
            @Suppress("DEPRECATION") location.isFromMockProvider
        }

        if (isMock) {
            tvPortalGpsAkuratBadge.text = "FAKE GPS TERDETEKSI!"
            tvPortalRadiusText.text = "Lokasi Palsu (Fake GPS Dilarang!)"
            tvPortalRadiusText.setTextColor(Color.parseColor("#DC2626"))
            dotPortalRadius.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_circle_red)
            badgePortalRadius.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_red_outline)
            badgePortalRadius.backgroundTintList = null

            tvPortalAbsenUtamaText.text = "Gagal (Fake GPS)"
            btnPortalAbsenUtama.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#991B1B"))
            isCurrentlyInRadius = false
            return
        }

        userLat = location.latitude
        userLng = location.longitude

        val latFormatted = String.format(Locale.US, "%.4f", userLat)
        val lngFormatted = String.format(Locale.US, "%.4f", userLng)
        tvPortalGpsAkuratBadge.text = "GPS Akurat ($latFormatted, $lngFormatted)"

        val distArr = FloatArray(1)
        Location.distanceBetween(userLat, userLng, schoolLat, schoolLng, distArr)
        currentDistanceMeters = distArr[0].toInt()

        tvPortalAlamatJarak.text = "Jl. Raya Prapatan Bandung Jalancagak Subang • Jarak $currentDistanceMeters m"

        isCurrentlyInRadius = currentDistanceMeters <= schoolRadius
        updateRadiusUI()
        updateMapLocation()
    }

    override fun onProviderEnabled(provider: String) {}
    override fun onProviderDisabled(provider: String) {}
    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    private fun updateRadiusUI() {
        if (!isAdded) return

        if (isHariLibur) {
            tvPortalRadiusText.text = "Hari Libur Sekolah"
            tvPortalRadiusText.setTextColor(Color.parseColor("#64748B"))
            dotPortalRadius.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_circle_green)
            dotPortalRadius.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
            badgePortalRadius.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_rounded_border)
            badgePortalRadius.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))

            tvPortalAbsenUtamaText.text = "Hari Libur"
            tvPortalAbsenUtamaText.setTextColor(Color.parseColor("#64748B"))
            ivPortalAbsenUtamaIcon.setImageResource(R.drawable.ic_hourglass_16)
            ivPortalAbsenUtamaIcon.setColorFilter(Color.parseColor("#94A3B8"))
            btnPortalAbsenUtama.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
        } else if (isCurrentlyInRadius) {
            tvPortalRadiusText.text = "Dalam Radius Sekolah ($currentDistanceMeters m / Maks ${schoolRadius} m)"
            tvPortalRadiusText.setTextColor(Color.parseColor("#15803D"))
            dotPortalRadius.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_circle_green)
            dotPortalRadius.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#16A34A"))
            badgePortalRadius.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_radius_in)
            badgePortalRadius.backgroundTintList = null

            tvPortalAbsenUtamaText.text = "Presensi Sekarang"
            tvPortalAbsenUtamaText.setTextColor(Color.WHITE)
            ivPortalAbsenUtamaIcon.setImageResource(R.drawable.ic_modern_scan)
            ivPortalAbsenUtamaIcon.setColorFilter(Color.WHITE)
            btnPortalAbsenUtama.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E1B4B"))
        } else {
            tvPortalRadiusText.text = "Luar Radius Sekolah ($currentDistanceMeters m / Maks ${schoolRadius} m)"
            tvPortalRadiusText.setTextColor(Color.parseColor("#DC2626"))
            dotPortalRadius.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_circle_red)
            dotPortalRadius.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DC2626"))
            badgePortalRadius.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_red_outline)
            badgePortalRadius.backgroundTintList = null

            tvPortalAbsenUtamaText.text = "Ajukan Izin"
            tvPortalAbsenUtamaText.setTextColor(Color.WHITE)
            ivPortalAbsenUtamaIcon.setImageResource(R.drawable.ic_modern_mail)
            ivPortalAbsenUtamaIcon.setColorFilter(Color.WHITE)
            btnPortalAbsenUtama.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
        }
    }

    private fun updateMapLocation() {
        if (!isMapLoaded || userLat == 0.0 || userLng == 0.0) return
        activity?.runOnUiThread {
            try {
                mapPortalWebView.loadUrl("javascript:updateLocation($userLat, $userLng, $schoolLat, $schoolLng, $schoolRadius)")
            } catch (_: Exception) {}
        }
    }

    private fun fetchPortalData() {
        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisn = sharedPref.getString("nisn", "") ?: ""
        val token = sharedPref.getString("token", "") ?: ""
        if (token.isNotEmpty()) {
            ApiClient.authToken = token
        }

        if (nisn.isEmpty()) {
            android.util.Log.e("PresensiFragment", "NISN kosong di SharedPreferences SesiUjian!")
            return
        }

        LoadingDialogHelper.dismiss(loadingDialog)
        loadingDialog = LoadingDialogHelper.show(context, "Memuat presensi siswa...")

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getPresensiSiswaPortalDashboard(nisn)
                withContext(Dispatchers.Main) {
                    LoadingDialogHelper.dismiss(loadingDialog)
                    loadingDialog = null
                    if (response.isSuccessful && response.body()?.status == true) {
                        val body = response.body()!!
                        populatePortalUI(body)
                    } else {
                        val err = response.errorBody()?.string() ?: "Response code: ${response.code()}"
                        android.util.Log.e("PresensiFragment", "Gagal memuat portal presensi siswa: $err")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingDialogHelper.dismiss(loadingDialog)
                    loadingDialog = null
                }
                android.util.Log.e("PresensiFragment", "Exception fetchPortalData: ${e.message}", e)
            }
        }
    }

    private fun populatePortalUI(data: PortalPresensiSiswaResponse) {
        if (!isAdded) return

        // 1. SISWA & PROFIL
        val s = data.siswa_info
        if (s != null) {
            if (!s.nama.isNullOrEmpty()) {
                tvPortalNamaSiswa.text = s.nama
                requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
                    .edit().putString("nama", s.nama).apply()
            }
            val kelasTxt = s.nama_kelas ?: "Kelas Siswa"
            tvPortalNisnKelas.text = if (!s.nisn.isNullOrEmpty()) "NISN: ${s.nisn} • $kelasTxt" else kelasTxt

            if (!s.jurusan.isNullOrEmpty()) {
                tvChipJurusanText.text = s.jurusan
            }
            if (!s.tahun_ajaran.isNullOrEmpty()) {
                tvPortalTahunAjaran.text = s.tahun_ajaran
            }
            if (!s.tanggal_hari_ini.isNullOrEmpty()) {
                tvPortalTanggal.text = s.tanggal_hari_ini
            }
        }

        // 2. SHIFT & SEKOLAH
        val sh = data.shift_info
        if (sh != null && !sh.jam_kerja.isNullOrEmpty()) {
            tvPortalShiftNama.text = "${sh.nama ?: "Shift Belajar Reguler"} (${sh.jam_kerja})"
        }

        val lok = data.lokasi_sekolah
        if (lok != null) {
            schoolLat = lok.latitude
            schoolLng = lok.longitude
            if (lok.radius > 0) {
                schoolRadius = lok.radius
            }
            if (!lok.nama_sekolah.isNullOrEmpty()) {
                tvPortalNamaSekolah.text = lok.nama_sekolah
                tvPortalMapSchoolBadge.text = "● ${lok.nama_sekolah} (Radius ${schoolRadius}m)"
            }
            if (!lok.alamat.isNullOrEmpty()) {
                tvPortalAlamatJarak.text = "${lok.alamat} • Jarak $currentDistanceMeters m"
            }
            updateRadiusUI()
            updateMapLocation()
        }

        // 3. PRESENSI HARI INI
        val phi = data.presensi_hari_ini
        if (phi != null) {
            isHariLibur = phi.is_libur
            if (isHariLibur) {
                keteranganLiburHariIni = phi.keterangan_libur ?: "Libur Sekolah"
            }

            tvPortalJamMasuk.text = phi.jam_masuk ?: "-- : -- WIB"
            tvPortalMasukBadge.text = phi.status_masuk_badge ?: (if (isHariLibur) "Hari Libur" else "Belum Absen")
            tvPortalMasukSub.text = phi.status_masuk_sub ?: (if (isHariLibur) keteranganLiburHariIni else "Belum Presensi Masuk")

            if (phi.sudah_masuk) {
                tvPortalJamMasuk.setTextColor(Color.parseColor("#0F172A"))
                tvPortalMasukBadge.setTextColor(Color.parseColor("#15803D"))
                tvPortalMasukBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                ivPortalMasukIcon.setImageResource(R.drawable.ic_check_circle_24)
                ivPortalMasukIcon.setColorFilter(Color.parseColor("#15803D"))
            } else if (!isHariLibur) {
                tvPortalJamMasuk.setTextColor(Color.parseColor("#64748B"))
                tvPortalMasukBadge.setTextColor(Color.parseColor("#B45309"))
                tvPortalMasukBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                ivPortalMasukIcon.setImageResource(R.drawable.ic_hourglass_16)
                ivPortalMasukIcon.setColorFilter(Color.parseColor("#D97706"))
            }

            tvPortalJamPulang.text = phi.jam_pulang ?: "-- : -- WIB"
            tvPortalPulangBadge.text = phi.status_pulang_badge ?: (if (isHariLibur) "Hari Libur" else "Mulai 14:45")
            tvPortalPulangSub.text = phi.status_pulang_sub ?: (if (isHariLibur) "Tidak Ada Presensi" else "Menunggu Jam Pulang")

            if (phi.sudah_pulang) {
                tvPortalJamPulang.setTextColor(Color.parseColor("#0F172A"))
                tvPortalPulangBadge.setTextColor(Color.parseColor("#15803D"))
                tvPortalPulangBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                ivPortalPulangIcon.setImageResource(R.drawable.ic_check_circle_24)
                ivPortalPulangIcon.setColorFilter(Color.parseColor("#15803D"))
            } else if (!isHariLibur) {
                tvPortalJamPulang.setTextColor(Color.parseColor("#64748B"))
                tvPortalPulangBadge.setTextColor(Color.parseColor("#475569"))
                tvPortalPulangBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
                ivPortalPulangIcon.setImageResource(R.drawable.ic_hourglass_16)
                ivPortalPulangIcon.setColorFilter(Color.parseColor("#94A3B8"))
            }

            updateRadiusUI()
        }

        // 4. STATISTIK KEHADIRAN
        val stat = data.statistik_kehadiran
        if (stat != null) {
            tvPortalStatPeriode.text = stat.periode_text ?: "Bulan Ini • Semester Ganjil"
            tvPortalStatPerformaBadge.text = stat.label_performa ?: "Sangat Baik"
            tvPortalStatPersen.text = "${stat.persentase}%"
            tvPortalStatHariKerja.text = stat.hari_kerja_text ?: "(${stat.hadir}/${stat.total_hari_kerja} Hari Belajar)"
            pbPortalStat.progress = stat.persentase

            tvPortalStatHadir.text = stat.hadir.toString()
            tvPortalStatTerlambat.text = stat.terlambat.toString()
            tvPortalStatIzinSakit.text = stat.izin_sakit.toString()
            tvPortalStatAlfa.text = stat.alfa.toString()
        }

        // 5. RIWAYAT PRESENSI TERAKHIR (5 Hari Aktif Terkini)
        val riwayat = data.riwayat_terakhir
        layoutPortalRiwayatContainer.removeAllViews()
        if (!riwayat.isNullOrEmpty()) {
            tvPortalRiwayatEmpty.visibility = View.GONE
            val inflater = LayoutInflater.from(requireContext())

            for (item in riwayat) {
                val itemView = inflater.inflate(R.layout.item_portal_riwayat_guru, layoutPortalRiwayatContainer, false)
                val ivIcon = itemView.findViewById<ImageView>(R.id.ivRiwayatIcon)
                val circle = itemView.findViewById<LinearLayout>(R.id.layoutRiwayatIconCircle)
                val tvTgl = itemView.findViewById<TextView>(R.id.tvRiwayatTanggal)
                val tvJam = itemView.findViewById<TextView>(R.id.tvRiwayatJam)
                val tvBadge = itemView.findViewById<TextView>(R.id.tvRiwayatStatusBadge)

                tvTgl.text = item.hari_tanggal ?: item.tanggal ?: "-"
                tvJam.text = item.jam_text ?: "-"
                tvBadge.text = item.status_badge ?: "Hadir Tepat Waktu"

                when (item.status_tipe?.lowercase()) {
                    "terlambat" -> {
                        ivIcon.setImageResource(R.drawable.ic_hourglass_16)
                        ivIcon.setColorFilter(Color.parseColor("#D97706"))
                        circle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                        tvBadge.setTextColor(Color.parseColor("#B45309"))
                        tvBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEF3C7"))
                    }
                    "izin", "sakit" -> {
                        ivIcon.setImageResource(R.drawable.ic_modern_mail)
                        ivIcon.setColorFilter(Color.parseColor("#0284C7"))
                        circle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0F2FE"))
                        tvBadge.setTextColor(Color.parseColor("#0369A1"))
                        tvBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0F2FE"))
                    }
                    "alfa" -> {
                        ivIcon.setImageResource(R.drawable.ic_cancel_circle_24)
                        ivIcon.setColorFilter(Color.parseColor("#DC2626"))
                        circle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                        tvBadge.setTextColor(Color.parseColor("#991B1B"))
                        tvBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                    }
                    else -> { // Hadir
                        ivIcon.setImageResource(R.drawable.ic_check_circle_24)
                        ivIcon.setColorFilter(Color.parseColor("#059669"))
                        circle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                        tvBadge.setTextColor(Color.parseColor("#15803D"))
                        tvBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                    }
                }
                layoutPortalRiwayatContainer.addView(itemView)
            }
        } else {
            tvPortalRiwayatEmpty.visibility = View.VISIBLE
            tvPortalRiwayatEmpty.text = "Belum ada catatan riwayat presensi."
            layoutPortalRiwayatContainer.addView(tvPortalRiwayatEmpty)
        }
    }

    private fun bukaScannerQR() {
        val options = ScanOptions()
        options.setDesiredBarcodeFormats(ScanOptions.QR_CODE)
        options.setPrompt("Arahkan ke QR Code Presensi")
        options.setBeepEnabled(true)
        options.setCaptureActivity(CustomScannerActivity::class.java)
        options.setOrientationLocked(true)
        barcodeLauncher.launch(options)
    }

    @SuppressLint("MissingPermission")
    private fun prosesAbsenQR(qrToken: String) {
        if (nisn.isEmpty()) {
            Toast.makeText(requireContext(), "Gagal: Sesi NISN kosong!", Toast.LENGTH_SHORT).show()
            return
        }

        isSubmitting = true
        val location = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)

        val lat = location?.latitude?.toString() ?: userLat.toString()
        val lng = location?.longitude?.toString() ?: userLng.toString()

        Toast.makeText(requireContext(), "Mengirim Data Presensi...", Toast.LENGTH_SHORT).show()

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.submitAbsen(nisn, lat, lng, qrToken)
                withContext(Dispatchers.Main) {
                    isSubmitting = false
                    if (response.isSuccessful) {
                        val msg = response.body()?.message ?: "Presensi Berhasil!"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                        fetchPortalData()
                    } else {
                        var errorMsg = response.body()?.message
                        if (errorMsg.isNullOrEmpty()) {
                            try {
                                val errorStr = response.errorBody()?.string()
                                if (!errorStr.isNullOrEmpty() && errorStr.startsWith("{")) {
                                    val jsonObject = org.json.JSONObject(errorStr)
                                    errorMsg = jsonObject.optString("message", "")
                                }
                            } catch (_: Exception) {}
                        }
                        if (errorMsg.isNullOrEmpty()) errorMsg = "Presensi Gagal"
                        Toast.makeText(requireContext(), errorMsg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isSubmitting = false
                    Toast.makeText(requireContext(), "Gagal koneksi ke server: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
