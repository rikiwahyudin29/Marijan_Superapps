package com.rtekmidev.marijancbt

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.rtekmidev.marijancbt.api.ApiClient
import com.rtekmidev.marijancbt.api.DashboardGuruData
import com.rtekmidev.marijancbt.util.AvatarHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class BerandaGuruFragment : Fragment() {

    // Hero Views
    private lateinit var tvHeroTahunAjaran: TextView
    private lateinit var tvHeroTanggal: TextView
    private lateinit var tvHeroNamaGuru: TextView
    private lateinit var tvHeroNipMapel: TextView
    private lateinit var badgeWaliKelas: LinearLayout
    private lateinit var tvBadgeWaliKelasText: TextView

    // Presensi Guru Hari Ini
    private lateinit var sectionPresensiGuru: LinearLayout
    private lateinit var badgeRadiusGuru: LinearLayout
    private lateinit var dotRadiusGuru: View
    private lateinit var ivPinRadiusGuru: ImageView
    private lateinit var tvRadiusGuruText: TextView
    private lateinit var tvShiftGuruNama: TextView
    private lateinit var tvShiftGuruJam: TextView
    private lateinit var tvWaktuRealtimeGuru: TextView
    private lateinit var tvStatusMasukBadge: TextView
    private lateinit var tvJamMasukGuru: TextView
    private lateinit var ivStatusMasukIcon: ImageView
    private lateinit var tvStatusMasukSub: TextView
    private lateinit var tvStatusPulangBadge: TextView
    private lateinit var tvJamPulangGuru: TextView
    private lateinit var ivStatusPulangIcon: ImageView
    private lateinit var tvStatusPulangSub: TextView
    private lateinit var btnAbsenMandiriGuru: LinearLayout
    private lateinit var ivBtnAbsenMandiriIcon: ImageView
    private lateinit var tvBtnAbsenMandiriText: TextView
    private lateinit var tvStatHadirGuru: TextView
    private lateinit var tvStatTerlambatGuru: TextView
    private lateinit var tvStatIzinSakitGuru: TextView
    private lateinit var tvStatKehadiranGuru: TextView

    private var isHariLibur: Boolean = false
    private var keteranganLiburHariIni: String = "Libur Sekolah"
    private var isCurrentlyInRadius: Boolean = true

    private var clockJob: Job? = null
    private var schoolLat: Double = 0.0
    private var schoolLng: Double = 0.0
    private var schoolRadius: Int = 100

    // Presensi Siswa Binaan
    private lateinit var sectionPresensiBinaan: LinearLayout
    private lateinit var tvPresensiBinaanSub: TextView
    private lateinit var btnAbsenManual: TextView
    private lateinit var tvPresensiHadirCount: TextView
    private lateinit var tvPresensiSakitCount: TextView
    private lateinit var tvPresensiIzinCount: TextView
    private lateinit var tvPresensiAlphaCount: TextView
    private lateinit var btnPresensiInfoBanner: LinearLayout
    private lateinit var tvPresensiInfoText: TextView

    // KBM Hari Ini
    private lateinit var sectionKbmHariIni: LinearLayout
    private lateinit var tvSesiTersisaBadge: TextView
    private lateinit var cvKbmAktif: CardView
    private lateinit var tvKbmStatusBadge: TextView
    private lateinit var tvKbmJamKeBadge: TextView
    private lateinit var tvKbmWaktu: TextView
    private lateinit var tvKbmNamaMapel: TextView
    private lateinit var tvKbmKelasChip: TextView
    private lateinit var tvKbmRuangChip: TextView
    private lateinit var btnKbmIsiJurnal: LinearLayout

    // Progres Jurnal
    private lateinit var cvProgresJurnal: CardView
    private lateinit var tvProgresJurnalPersen: TextView
    private lateinit var pbProgresJurnal: ProgressBar
    private lateinit var tvProgresJurnalTerisi: TextView
    private lateinit var tvProgresJurnalMenanti: TextView

    // 5 Menu Pintasan
    private lateinit var menuJadwalMengajar: LinearLayout
    private lateinit var menuJurnalGuru: LinearLayout
    private lateinit var menuAbsenBinaan: LinearLayout
    private lateinit var menuRekapAbsen: LinearLayout
    private lateinit var menuKeuanganKelas: LinearLayout

    // Keuangan Kelas
    private lateinit var cvKeuanganKelasBinaan: CardView
    private lateinit var tvKeuanganKelasTitle: TextView
    private lateinit var tvKeuanganTotalTagihan: TextView
    private lateinit var tvKeuanganSiswaBelumLunas: TextView
    private lateinit var btnBukaRekapKeuangan: LinearLayout

    private var namaKelasWali: String = ""
    private var isGuruWaliKelas: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_beranda_guru, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        loadLocalProfile()
        setupListeners()
        startRealtimeClock()
        fetchDashboardData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        clockJob?.cancel()
    }

    private fun startRealtimeClock() {
        clockJob?.cancel()
        clockJob = viewLifecycleOwner.lifecycleScope.launch {
            val sdf = SimpleDateFormat("HH:mm:ss 'WIB'", Locale.getDefault())
            while (isActive) {
                try {
                    tvWaktuRealtimeGuru.text = sdf.format(Date())
                } catch (_: Exception) {}
                delay(1000)
            }
        }
    }

    private fun initViews(view: View) {
        // Hero
        tvHeroTahunAjaran = view.findViewById(R.id.tvHeroTahunAjaran)
        tvHeroTanggal = view.findViewById(R.id.tvHeroTanggal)
        tvHeroNamaGuru = view.findViewById(R.id.tvHeroNamaGuru)
        tvHeroNipMapel = view.findViewById(R.id.tvHeroNipMapel)
        badgeWaliKelas = view.findViewById(R.id.badgeWaliKelas)
        tvBadgeWaliKelasText = view.findViewById(R.id.tvBadgeWaliKelasText)

        // Presensi Guru
        sectionPresensiGuru = view.findViewById(R.id.sectionPresensiGuru)
        badgeRadiusGuru = view.findViewById(R.id.badgeRadiusGuru)
        dotRadiusGuru = view.findViewById(R.id.dotRadiusGuru)
        ivPinRadiusGuru = view.findViewById(R.id.ivPinRadiusGuru)
        tvRadiusGuruText = view.findViewById(R.id.tvRadiusGuruText)
        tvShiftGuruNama = view.findViewById(R.id.tvShiftGuruNama)
        tvShiftGuruJam = view.findViewById(R.id.tvShiftGuruJam)
        tvWaktuRealtimeGuru = view.findViewById(R.id.tvWaktuRealtimeGuru)
        tvStatusMasukBadge = view.findViewById(R.id.tvStatusMasukBadge)
        tvJamMasukGuru = view.findViewById(R.id.tvJamMasukGuru)
        ivStatusMasukIcon = view.findViewById(R.id.ivStatusMasukIcon)
        tvStatusMasukSub = view.findViewById(R.id.tvStatusMasukSub)
        tvStatusPulangBadge = view.findViewById(R.id.tvStatusPulangBadge)
        tvJamPulangGuru = view.findViewById(R.id.tvJamPulangGuru)
        ivStatusPulangIcon = view.findViewById(R.id.ivStatusPulangIcon)
        tvStatusPulangSub = view.findViewById(R.id.tvStatusPulangSub)
        btnAbsenMandiriGuru = view.findViewById(R.id.btnAbsenMandiriGuru)
        ivBtnAbsenMandiriIcon = view.findViewById(R.id.ivBtnAbsenMandiriIcon)
        tvBtnAbsenMandiriText = view.findViewById(R.id.tvBtnAbsenMandiriText)
        tvStatHadirGuru = view.findViewById(R.id.tvStatHadirGuru)
        tvStatTerlambatGuru = view.findViewById(R.id.tvStatTerlambatGuru)
        tvStatIzinSakitGuru = view.findViewById(R.id.tvStatIzinSakitGuru)
        tvStatKehadiranGuru = view.findViewById(R.id.tvStatKehadiranGuru)

        // Presensi Binaan
        sectionPresensiBinaan = view.findViewById(R.id.sectionPresensiBinaan)
        tvPresensiBinaanSub = view.findViewById(R.id.tvPresensiBinaanSub)
        btnAbsenManual = view.findViewById(R.id.btnAbsenManual)
        tvPresensiHadirCount = view.findViewById(R.id.tvPresensiHadirCount)
        tvPresensiSakitCount = view.findViewById(R.id.tvPresensiSakitCount)
        tvPresensiIzinCount = view.findViewById(R.id.tvPresensiIzinCount)
        tvPresensiAlphaCount = view.findViewById(R.id.tvPresensiAlphaCount)
        btnPresensiInfoBanner = view.findViewById(R.id.btnPresensiInfoBanner)
        tvPresensiInfoText = view.findViewById(R.id.tvPresensiInfoText)

        // KBM Hari Ini
        sectionKbmHariIni = view.findViewById(R.id.sectionKbmHariIni)
        tvSesiTersisaBadge = view.findViewById(R.id.tvSesiTersisaBadge)
        cvKbmAktif = view.findViewById(R.id.cvKbmAktif)
        tvKbmStatusBadge = view.findViewById(R.id.tvKbmStatusBadge)
        tvKbmJamKeBadge = view.findViewById(R.id.tvKbmJamKeBadge)
        tvKbmWaktu = view.findViewById(R.id.tvKbmWaktu)
        tvKbmNamaMapel = view.findViewById(R.id.tvKbmNamaMapel)
        tvKbmKelasChip = view.findViewById(R.id.tvKbmKelasChip)
        tvKbmRuangChip = view.findViewById(R.id.tvKbmRuangChip)
        btnKbmIsiJurnal = view.findViewById(R.id.btnKbmIsiJurnal)

        // Progres Jurnal
        cvProgresJurnal = view.findViewById(R.id.cvProgresJurnal)
        tvProgresJurnalPersen = view.findViewById(R.id.tvProgresJurnalPersen)
        pbProgresJurnal = view.findViewById(R.id.pbProgresJurnal)
        tvProgresJurnalTerisi = view.findViewById(R.id.tvProgresJurnalTerisi)
        tvProgresJurnalMenanti = view.findViewById(R.id.tvProgresJurnalMenanti)

        // 5 Menu
        menuJadwalMengajar = view.findViewById(R.id.menuJadwalMengajar)
        menuJurnalGuru = view.findViewById(R.id.menuJurnalGuru)
        menuAbsenBinaan = view.findViewById(R.id.menuAbsenBinaan)
        menuRekapAbsen = view.findViewById(R.id.menuRekapAbsen)
        menuKeuanganKelas = view.findViewById(R.id.menuKeuanganKelas)

        // Keuangan Kelas
        cvKeuanganKelasBinaan = view.findViewById(R.id.cvKeuanganKelasBinaan)
        tvKeuanganKelasTitle = view.findViewById(R.id.tvKeuanganKelasTitle)
        tvKeuanganTotalTagihan = view.findViewById(R.id.tvKeuanganTotalTagihan)
        tvKeuanganSiswaBelumLunas = view.findViewById(R.id.tvKeuanganSiswaBelumLunas)
        btnBukaRekapKeuangan = view.findViewById(R.id.btnBukaRekapKeuangan)
    }

    private fun loadLocalProfile() {
        val sharedPref = requireActivity().getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val namaGuru = sharedPref.getString("nama", "Guru") ?: "Guru"
        val nipGuru = sharedPref.getString("nip", null)

        tvHeroNamaGuru.text = namaGuru
        tvHeroNipMapel.text = if (!nipGuru.isNullOrEmpty()) "NIP: $nipGuru • Guru Pengajar" else "Tenaga Pendidik SMK Riyadhul Jannah"

        // Default Date
        val sdf = SimpleDateFormat("EEEE, dd MMM yyyy", Locale("id", "ID"))
        tvHeroTanggal.text = sdf.format(Calendar.getInstance().time)

        // Load Avatar / Initials in Top Bar
        val ivProfilPhoto = requireActivity().findViewById<ImageView>(R.id.ivProfilPhoto)
        val tvProfilInisial = requireActivity().findViewById<TextView>(R.id.tvProfilInisial)
        val cvProfilPic = requireActivity().findViewById<CardView>(R.id.cvProfilPic)
        val fotoProfilUrl = sharedPref.getString("foto_profil", null)
        val fullUrl = if (!fotoProfilUrl.isNullOrEmpty()) {
            if (fotoProfilUrl.startsWith("http")) fotoProfilUrl else "https://smkriyadhuljannahjalancagak.sch.id/uploads/guru/$fotoProfilUrl"
        } else null
        
        try {
            AvatarHelper.setAvatar(requireActivity(), namaGuru, fullUrl, ivProfilPhoto, tvProfilInisial, cvProfilPic)
        } catch (_: Exception) {}
    }

    private fun setupListeners() {
        // Presensi Guru: Arahkan ke scanner / form izin / info libur
        val aksiPresensiGuru = View.OnClickListener {
            if (isHariLibur) {
                Toast.makeText(requireContext(), "Hari ini libur sekolah ($keteranganLiburHariIni)", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            if (!isCurrentlyInRadius) {
                val intent = Intent(requireContext(), IzinActivity::class.java)
                startActivity(intent)
                requireActivity().applyEnterTransition()
                return@OnClickListener
            }
            val act = activity
            if (act is DashboardGuruActivity) {
                act.cekRadiusDanScan()
            } else {
                val viewPager = requireActivity().findViewById<ViewPager2>(R.id.viewPager)
                if (viewPager != null) {
                    viewPager.currentItem = 1
                }
            }
        }
        btnAbsenMandiriGuru.setOnClickListener(aksiPresensiGuru)
        badgeRadiusGuru.setOnClickListener(aksiPresensiGuru)

        // 1. Jadwal Mengajar
        menuJadwalMengajar.setOnClickListener {
            val intent = Intent(requireContext(), JadwalMengajarActivity::class.java)
            startActivity(intent)
            requireActivity().applyEnterTransition()
        }

        // 2. Jurnal Guru
        menuJurnalGuru.setOnClickListener {
            val intent = Intent(requireContext(), RekapMengajarActivity::class.java)
            startActivity(intent)
            requireActivity().applyEnterTransition()
        }

        // 3. Absen Binaan (Wali Kelas)
        val openAbsenHarian = View.OnClickListener {
            if (isGuruWaliKelas && namaKelasWali.isNotEmpty()) {
                val intent = Intent(requireContext(), WaliKelasAbsenHarianActivity::class.java)
                intent.putExtra("nama_kelas", namaKelasWali)
                startActivity(intent)
                requireActivity().applyEnterTransition()
            } else {
                Toast.makeText(requireContext(), "Menu ini khusus untuk Wali Kelas.", Toast.LENGTH_SHORT).show()
            }
        }
        menuAbsenBinaan.setOnClickListener(openAbsenHarian)
        btnAbsenManual.setOnClickListener(openAbsenHarian)
        btnPresensiInfoBanner.setOnClickListener(openAbsenHarian)

        // 4. Rekap Absen (Wali Kelas)
        menuRekapAbsen.setOnClickListener {
            if (isGuruWaliKelas && namaKelasWali.isNotEmpty()) {
                val intent = Intent(requireContext(), WaliKelasKehadiranActivity::class.java)
                intent.putExtra("nama_kelas", namaKelasWali)
                intent.putExtra("mode", "rekap")
                startActivity(intent)
                requireActivity().applyEnterTransition()
            } else {
                Toast.makeText(requireContext(), "Menu ini khusus untuk Wali Kelas.", Toast.LENGTH_SHORT).show()
            }
        }

        // 5. Keuangan Kelas (Wali Kelas)
        val openKeuangan = View.OnClickListener {
            if (isGuruWaliKelas && namaKelasWali.isNotEmpty()) {
                val intent = Intent(requireContext(), WaliKelasKeuanganActivity::class.java)
                intent.putExtra("nama_kelas", namaKelasWali)
                startActivity(intent)
                requireActivity().applyEnterTransition()
            } else {
                Toast.makeText(requireContext(), "Menu ini khusus untuk Wali Kelas.", Toast.LENGTH_SHORT).show()
            }
        }
        menuKeuanganKelas.setOnClickListener(openKeuangan)
        btnBukaRekapKeuangan.setOnClickListener(openKeuangan)

        // Tombol Aksi KBM: Hanya Isi Jurnal KBM
        btnKbmIsiJurnal.setOnClickListener {
            val intent = Intent(requireContext(), RekapMengajarActivity::class.java)
            startActivity(intent)
            requireActivity().applyEnterTransition()
        }

        // Auto-Hide Bottom Nav on Scroll
        val scrollView = view?.findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)
        scrollView?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 12) {
                (activity as? DashboardGuruActivity)?.hideBottomNav()
            } else if (dy < -12 || scrollY <= 10) {
                (activity as? DashboardGuruActivity)?.showBottomNav()
            }
        }
    }

    private fun fetchDashboardData() {
        val sharedPref = requireActivity().getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val idUser = sharedPref.getString("id_user", null) ?: return

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getAkademikGuruDashboard(idUser)
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful && response.body()?.status == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            renderDashboard(data)
                            context?.let { ctx ->
                                PengingatMengajarManager.sinkronkanJadwalHariIni(
                                    ctx,
                                    data.jadwal_hari_ini,
                                    data.is_libur == true
                                )
                            }
                        }
                    }
                }
            } catch (_: Exception) {
                // Keep default loaded views
            }
        }
    }

    private fun renderDashboard(data: DashboardGuruData) {
        // 1. Hero Card
        if (!data.tahun_ajaran_aktif.isNullOrEmpty()) {
            tvHeroTahunAjaran.text = "● ${data.tahun_ajaran_aktif}"
        }
        if (!data.tanggal_hari_ini_formatted.isNullOrEmpty()) {
            tvHeroTanggal.text = data.tanggal_hari_ini_formatted
        }

        val guruInfo = data.guru_info
        if (guruInfo != null) {
            if (!guruInfo.nama.isNullOrEmpty()) {
                tvHeroNamaGuru.text = guruInfo.nama
                try {
                    val sharedPref = requireActivity().getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
                    sharedPref.edit().putString("nama", guruInfo.nama).apply()
                } catch (_: Exception) {}
            }
            val nipStr = if (!guruInfo.nip.isNullOrEmpty() && guruInfo.nip != "-") "NIP: ${guruInfo.nip} • " else ""
            val mapelStr = guruInfo.mapel_utama ?: "Guru Pengajar"
            tvHeroNipMapel.text = "$nipStr$mapelStr"

            if (guruInfo.is_wali_kelas == true && !guruInfo.kelas_wali.isNullOrEmpty()) {
                isGuruWaliKelas = true
                namaKelasWali = guruInfo.kelas_wali
                badgeWaliKelas.visibility = View.VISIBLE
                tvBadgeWaliKelasText.text = "Wali Kelas ${guruInfo.kelas_wali}"
            } else {
                badgeWaliKelas.visibility = View.GONE
            }
        }

        // 2. Presensi Guru Hari Ini
        val pg = data.presensi_guru_hari_ini
        if (pg != null) {
            val isLiburHariIni = pg.is_libur || data.is_libur == true
            val ketLibur = pg.keterangan_libur ?: (data.keterangan_libur ?: "Libur Sekolah")
            isHariLibur = isLiburHariIni
            keteranganLiburHariIni = ketLibur

            if (isLiburHariIni) {
                tvShiftGuruNama.text = "${pg.shift_nama ?: "Shift Reguler Pagi"} (Libur)"
                tvShiftGuruJam.text = ketLibur
            } else {
                tvShiftGuruNama.text = pg.shift_nama ?: "Shift Reguler Pagi"
                tvShiftGuruJam.text = pg.shift_jam ?: "06:30 - 14:45 WIB"
            }

            // Masuk
            if (pg.is_sudah_masuk) {
                tvJamMasukGuru.text = pg.jam_masuk ?: "-- : -- WIB"
                tvStatusMasukBadge.text = pg.status_masuk_badge ?: "Tepat Waktu"
                tvStatusMasukBadge.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_green_solid)
                tvStatusMasukBadge.setTextColor(Color.WHITE)
                ivStatusMasukIcon.setImageResource(R.drawable.ic_check_circle_24)
                ivStatusMasukIcon.setColorFilter(Color.parseColor("#059669"))
                tvStatusMasukSub.text = pg.status_masuk_sub ?: "Terverifikasi Face/GPS"
                tvStatusMasukSub.setTextColor(Color.parseColor("#059669"))
            } else {
                tvJamMasukGuru.text = "-- : -- WIB"
                if (isLiburHariIni) {
                    tvStatusMasukBadge.text = "Hari Libur"
                    tvStatusMasukBadge.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_grey_solid)
                    tvStatusMasukBadge.setTextColor(Color.parseColor("#475569"))
                    ivStatusMasukIcon.setImageResource(R.drawable.ic_hourglass_16)
                    ivStatusMasukIcon.setColorFilter(Color.parseColor("#64748B"))
                    tvStatusMasukSub.text = ketLibur
                    tvStatusMasukSub.setTextColor(Color.parseColor("#64748B"))
                } else {
                    tvStatusMasukBadge.text = pg.status_masuk_badge ?: "Belum Absen"
                    tvStatusMasukBadge.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_grey_solid)
                    tvStatusMasukBadge.setTextColor(Color.parseColor("#475569"))
                    ivStatusMasukIcon.setImageResource(R.drawable.ic_hourglass_16)
                    ivStatusMasukIcon.setColorFilter(Color.parseColor("#64748B"))
                    tvStatusMasukSub.text = pg.status_masuk_sub ?: "Belum Absen Masuk"
                    tvStatusMasukSub.setTextColor(Color.parseColor("#64748B"))
                }
            }

            // Pulang
            if (pg.is_sudah_pulang) {
                tvJamPulangGuru.text = pg.jam_pulang ?: "-- : -- WIB"
                tvStatusPulangBadge.text = pg.status_pulang_badge ?: "Selesai"
                tvStatusPulangBadge.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_green_solid)
                tvStatusPulangBadge.setTextColor(Color.WHITE)
                ivStatusPulangIcon.setImageResource(R.drawable.ic_check_circle_24)
                ivStatusPulangIcon.setColorFilter(Color.parseColor("#059669"))
                tvStatusPulangSub.text = pg.status_pulang_sub ?: "Terverifikasi Face/GPS"
                tvStatusPulangSub.setTextColor(Color.parseColor("#059669"))
            } else {
                tvJamPulangGuru.text = "-- : -- WIB"
                if (isLiburHariIni) {
                    tvStatusPulangBadge.text = "Hari Libur"
                    tvStatusPulangBadge.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_grey_solid)
                    tvStatusPulangBadge.setTextColor(Color.parseColor("#475569"))
                    ivStatusPulangIcon.setImageResource(R.drawable.ic_hourglass_16)
                    ivStatusPulangIcon.setColorFilter(Color.parseColor("#94A3B8"))
                    tvStatusPulangSub.text = "Tidak Ada Presensi"
                    tvStatusPulangSub.setTextColor(Color.parseColor("#64748B"))
                } else {
                    tvStatusPulangBadge.text = pg.status_pulang_badge ?: (pg.jam_pulang_mulai_badge ?: "Mulai 14:45")
                    tvStatusPulangBadge.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_grey_solid)
                    tvStatusPulangBadge.setTextColor(Color.parseColor("#475569"))
                    ivStatusPulangIcon.setImageResource(R.drawable.ic_hourglass_16)
                    ivStatusPulangIcon.setColorFilter(Color.parseColor("#94A3B8"))
                    tvStatusPulangSub.text = pg.status_pulang_sub ?: "Belum Jam Kepulangan"
                    tvStatusPulangSub.setTextColor(Color.parseColor("#64748B"))
                }
            }

            // Stat Bulanan
            tvStatHadirGuru.text = pg.stat_hadir ?: "0 Hari"
            tvStatTerlambatGuru.text = pg.stat_terlambat ?: "0 Kali"
            tvStatIzinSakitGuru.text = pg.stat_izin_sakit ?: "0 Hari"
            tvStatKehadiranGuru.text = pg.stat_kehadiran ?: "100%"

            // Koordinat radius sekolah
            schoolLat = pg.school_lat
            schoolLng = pg.school_lng
            schoolRadius = pg.school_radius
            checkRadiusStatus()
            updateTombolPresensiState()
        }

        // 3. Presensi Siswa Binaan
        val waliInfo = data.wali_kelas_info
        if (waliInfo != null && !waliInfo.nama_kelas.isNullOrEmpty()) {
            isGuruWaliKelas = true
            namaKelasWali = waliInfo.nama_kelas
            sectionPresensiBinaan.visibility = View.VISIBLE

            val ps = data.presensi_binaan_summary
            val isLiburBinaan = waliInfo.is_libur == true || ps?.is_libur == true || data.is_libur == true
            val ketLiburBinaan = waliInfo.keterangan_libur ?: (ps?.keterangan_libur ?: (data.keterangan_libur ?: "Libur Sekolah"))

            if (isLiburBinaan) {
                tvPresensiBinaanSub.text = "Kelas ${waliInfo.nama_kelas} • Hari Libur"
                btnAbsenManual.text = "Hari Libur ›"
            } else {
                tvPresensiBinaanSub.text = "Kelas ${waliInfo.nama_kelas} (Real-time)"
                btnAbsenManual.text = "Absen Manual ›"
            }

            val totalSiswa = waliInfo.total_siswa ?: (ps?.total_siswa ?: 0)
            tvPresensiHadirCount.text = (ps?.hadir ?: 0).toString()
            tvPresensiSakitCount.text = (ps?.sakit ?: 0).toString()
            tvPresensiIzinCount.text = (ps?.izin ?: 0).toString()
            tvPresensiAlphaCount.text = (ps?.alpha ?: 0).toString()

            if (isLiburBinaan) {
                tvPresensiInfoText.text = "$totalSiswa Siswa • Hari Libur ($ketLiburBinaan)"
            } else {
                val catatan = ps?.catatan_izin ?: "Catatan Izin Masuk"
                tvPresensiInfoText.text = "$totalSiswa Siswa Total • $catatan"
            }
        } else if (!isGuruWaliKelas) {
            sectionPresensiBinaan.visibility = View.GONE
        }

        // 4. KBM & Pembelajaran Hari Ini
        val isLiburKbm = isHariLibur || data.is_libur == true
        val jadwalList = data.jadwal_hari_ini

        if (isLiburKbm) {
            sectionKbmHariIni.visibility = View.VISIBLE
            tvSesiTersisaBadge.text = "Libur"
            tvKbmStatusBadge.text = "● Hari Libur"
            tvKbmStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
            tvKbmStatusBadge.setTextColor(Color.parseColor("#64748B"))
            tvKbmJamKeBadge.text = "Tidak Ada KBM"
            tvKbmWaktu.text = "Hari ini libur sekolah, kegiatan belajar mengajar ditiadakan."
            val ket = data.keterangan_libur ?: keteranganLiburHariIni
            tvKbmNamaMapel.text = if (!ket.isNullOrEmpty()) ket else "Libur Sekolah"
            tvKbmKelasChip.visibility = View.GONE
            tvKbmRuangChip.visibility = View.GONE
            btnKbmIsiJurnal.visibility = View.GONE
        } else if (!jadwalList.isNullOrEmpty()) {
            sectionKbmHariIni.visibility = View.VISIBLE
            btnKbmIsiJurnal.visibility = View.VISIBLE
            tvKbmKelasChip.visibility = View.VISIBLE
            tvKbmRuangChip.visibility = View.VISIBLE

            val sesiTersisa = jadwalList.count { it.is_jurnal_filled != true }
            tvSesiTersisaBadge.text = if (sesiTersisa > 0) "$sesiTersisa Sesi Tersisa" else "Semua Selesai"

            val active = jadwalList.firstOrNull { it.is_jurnal_filled != true } ?: jadwalList.first()
            tvKbmStatusBadge.text = if (active.is_jurnal_filled == true) "● Selesai KBM" else "● Sedang Berlangsung"
            tvKbmStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor(if (active.is_jurnal_filled == true) "#DCFCE7" else "#EDE9FE"))
            tvKbmStatusBadge.setTextColor(Color.parseColor(if (active.is_jurnal_filled == true) "#15803D" else "#6D28D9"))

            tvKbmJamKeBadge.text = "Jam Ke ${active.jam_ke ?: "1"}"
            tvKbmWaktu.text = "🕒 ${active.jam_mulai ?: "08:00"} - ${active.jam_selesai ?: "09:30"} WIB"
            tvKbmNamaMapel.text = active.nama_mapel ?: "Mata Pelajaran"
            tvKbmKelasChip.text = "Kelas ${active.nama_kelas ?: "-"}"
            tvKbmRuangChip.text = "Ruang KBM"
        } else {
            sectionKbmHariIni.visibility = View.VISIBLE
            tvSesiTersisaBadge.text = "0 Sesi"
            tvKbmStatusBadge.text = "● Bebas Mengajar"
            tvKbmStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
            tvKbmStatusBadge.setTextColor(Color.parseColor("#64748B"))
            tvKbmJamKeBadge.text = "Hari Ini"
            tvKbmWaktu.text = "Tidak ada jadwal KBM tatap muka hari ini."
            tvKbmNamaMapel.text = "Jadwal Mengajar Kosong"
            tvKbmKelasChip.visibility = View.GONE
            tvKbmRuangChip.visibility = View.GONE
            btnKbmIsiJurnal.visibility = View.GONE
        }

        // Progres Jurnal
        val pj = data.progres_jurnal_bulan_ini
        if (pj != null) {
            tvProgresJurnalPersen.text = "${pj.persentase}%"
            pbProgresJurnal.progress = pj.persentase
            tvProgresJurnalTerisi.text = "${pj.sesi_terisi} dari ${pj.total_sesi} Sesi Terisi"
            tvProgresJurnalMenanti.text = "${pj.sesi_menanti} Sesi Menanti"
        }

        // 5. Keuangan Kelas Binaan
        val ks = data.keuangan_kelas_summary
        if (isGuruWaliKelas && namaKelasWali.isNotEmpty()) {
            cvKeuanganKelasBinaan.visibility = View.VISIBLE
            tvKeuanganKelasTitle.text = "Keuangan Kelas $namaKelasWali"
            if (ks != null) {
                tvKeuanganTotalTagihan.text = formatRupiah(ks.total_tagihan)
                tvKeuanganSiswaBelumLunas.text = "${ks.siswa_belum_lunas} Siswa"
            }
        } else {
            cvKeuanganKelasBinaan.visibility = View.GONE
        }
    }

    private fun checkRadiusStatus() {
        if (!isAdded || context == null) return
        if (schoolLat == 0.0 || schoolLng == 0.0) {
            setRadiusBadge(true)
            return
        }

        try {
            val lm = requireContext().getSystemService(Context.LOCATION_SERVICE) as? LocationManager
            val fineGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            val coarseGranted = ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
            
            if (fineGranted || coarseGranted) {
                val loc = lm?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    ?: lm?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                if (loc != null) {
                    val res = FloatArray(1)
                    Location.distanceBetween(loc.latitude, loc.longitude, schoolLat, schoolLng, res)
                    val dist = res[0].toInt()
                    setRadiusBadge(dist <= schoolRadius)
                    return
                }
            }
            setRadiusBadge(true)
        } catch (_: Exception) {
            setRadiusBadge(true)
        }
    }

    private fun setRadiusBadge(isInRadius: Boolean) {
        if (!isAdded) return
        isCurrentlyInRadius = isInRadius
        if (isInRadius) {
            tvRadiusGuruText.text = "Dalam Radius"
            tvRadiusGuruText.setTextColor(Color.parseColor("#16A34A"))
            dotRadiusGuru.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_circle_green)
            ivPinRadiusGuru.setColorFilter(Color.parseColor("#16A34A"))
            badgeRadiusGuru.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_pill_radius_in)
        } else {
            tvRadiusGuruText.text = "Luar Radius"
            tvRadiusGuruText.setTextColor(Color.parseColor("#E11D48"))
            dotRadiusGuru.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_circle_red)
            ivPinRadiusGuru.setColorFilter(Color.parseColor("#E11D48"))
            badgeRadiusGuru.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_red_outline)
        }
        updateTombolPresensiState()
    }

    private fun updateTombolPresensiState() {
        if (!isAdded || context == null) return
        if (isHariLibur) {
            tvBtnAbsenMandiriText.text = "Hari Libur"
            tvBtnAbsenMandiriText.setTextColor(Color.parseColor("#64748B"))
            ivBtnAbsenMandiriIcon.setImageResource(R.drawable.ic_hourglass_16)
            ivBtnAbsenMandiriIcon.setColorFilter(Color.parseColor("#94A3B8"))
            btnAbsenMandiriGuru.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
        } else if (!isCurrentlyInRadius) {
            tvBtnAbsenMandiriText.text = "Ajukan Izin"
            tvBtnAbsenMandiriText.setTextColor(Color.WHITE)
            ivBtnAbsenMandiriIcon.setImageResource(R.drawable.ic_modern_mail)
            ivBtnAbsenMandiriIcon.setColorFilter(Color.WHITE)
            btnAbsenMandiriGuru.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
        } else {
            tvBtnAbsenMandiriText.text = "Absen Mandiri (Scan / GPS)"
            tvBtnAbsenMandiriText.setTextColor(Color.WHITE)
            ivBtnAbsenMandiriIcon.setImageResource(R.drawable.ic_modern_scan)
            ivBtnAbsenMandiriIcon.setColorFilter(Color.WHITE)
            btnAbsenMandiriGuru.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E1B4B"))
        }
    }

    private fun formatRupiah(amount: Long): String {
        return try {
            "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount)
        } catch (_: Exception) {
            "Rp $amount"
        }
    }
}
