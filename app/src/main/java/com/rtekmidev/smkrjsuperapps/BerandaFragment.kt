package com.rtekmidev.smkrjsuperapps

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.DashboardData
import com.rtekmidev.smkrjsuperapps.util.AvatarHelper
import com.rtekmidev.smkrjsuperapps.util.LoadingDialogHelper
import com.rtekmidev.smkrjsuperapps.util.RefreshableFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BerandaFragment : Fragment(), RefreshableFragment {

    private var loadingDialog: Dialog? = null

    override fun onDestroyView() {
        super.onDestroyView()
        LoadingDialogHelper.dismiss(loadingDialog)
        loadingDialog = null
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_beranda, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val nisn = sharedPref.getString("nisn", null)
        val userName = sharedPref.getString("nama_lengkap", "Siswa") ?: "Siswa"
        val initialKelas = sharedPref.getString("kelas", "-") ?: "-"

        // Profile image from global header
        val ivProfilPhoto = requireActivity().findViewById<ImageView>(R.id.ivProfilPhoto)

        // Bind Section 1: Hero Card
        val tvHeroTa = view.findViewById<TextView>(R.id.tvHeroTa)
        val tvHeroDate = view.findViewById<TextView>(R.id.tvHeroDate)
        val tvNamaDashboard = view.findViewById<TextView>(R.id.tvNamaDashboard)
        val tvHeroNisKelas = view.findViewById<TextView>(R.id.tvHeroNisKelas)
        val tvHeroJurusan = view.findViewById<TextView>(R.id.tvHeroJurusan)
        val tvHeroStatus = view.findViewById<TextView>(R.id.tvHeroStatus)
        val tvHeroWaliKelas = view.findViewById<TextView>(R.id.tvHeroWaliKelas)
        val tvHeroPoinDisiplin = view.findViewById<TextView>(R.id.tvHeroPoinDisiplin)
        val pbHeroDisiplin = view.findViewById<ProgressBar>(R.id.pbHeroDisiplin)

        // Bind Section 2: 4 Stat Cards
        val cardStatPresensi = view.findViewById<View>(R.id.cardStatPresensi)
        val tvStatPresensiVal = view.findViewById<TextView>(R.id.tvStatPresensiVal)
        val tvStatPresensiSub = view.findViewById<TextView>(R.id.tvStatPresensiSub)

        val cardStatTugas = view.findViewById<View>(R.id.cardStatTugas)
        val tvStatTugasVal = view.findViewById<TextView>(R.id.tvStatTugasVal)
        val tvStatTugasSub = view.findViewById<TextView>(R.id.tvStatTugasSub)

        val cardStatUjian = view.findViewById<View>(R.id.cardStatUjian)
        val tvStatUjianVal = view.findViewById<TextView>(R.id.tvStatUjianVal)
        val tvStatUjianSub = view.findViewById<TextView>(R.id.tvStatUjianSub)

        val cardStatTagihan = view.findViewById<View>(R.id.cardStatTagihan)
        val tvStatTagihanVal = view.findViewById<TextView>(R.id.tvStatTagihanVal)
        val tvStatTagihanSub = view.findViewById<TextView>(R.id.tvStatTagihanSub)

        // Bind Section 3: Ujian Hari Ini
        val layoutUjianHariIniWrapper = view.findViewById<View>(R.id.layoutUjianHariIniWrapper)
        val tvUjianBadge = view.findViewById<TextView>(R.id.tvUjianBadge)
        val tvUjianWaktu = view.findViewById<TextView>(R.id.tvUjianWaktu)
        val tvUjianJudul = view.findViewById<TextView>(R.id.tvUjianJudul)
        val tvUjianRuang = view.findViewById<TextView>(R.id.tvUjianRuang)
        val btnMulaiUjian = view.findViewById<Button>(R.id.btnMulaiUjian)

        // Bind Section 4: Mata Pelajaran Hari Ini
        val tvMapelHariPill = view.findViewById<TextView>(R.id.tvMapelHariPill)
        val cardActiveKbm = view.findViewById<View>(R.id.cardActiveKbm)
        val tvKbmStatusBadge = view.findViewById<TextView>(R.id.tvKbmStatusBadge)
        val tvKbmWaktuRuang = view.findViewById<TextView>(R.id.tvKbmWaktuRuang)
        val tvKbmMapel = view.findViewById<TextView>(R.id.tvKbmMapel)
        val tvKbmGuru = view.findViewById<TextView>(R.id.tvKbmGuru)
        val btnKbmMateri = view.findViewById<Button>(R.id.btnKbmMateri)
        val containerOtherSubjects = view.findViewById<LinearLayout>(R.id.containerOtherSubjects)

        // Bind Section 5: Presensi Kehadiran Sekolah
        val tvPresensiRadiusPill = view.findViewById<TextView>(R.id.tvPresensiRadiusPill)
        val tvPresensiLokasiSub = view.findViewById<TextView>(R.id.tvPresensiLokasiSub)
        val tvPresensiJamMasuk = view.findViewById<TextView>(R.id.tvPresensiJamMasuk)
        val tvPresensiStatusMasuk = view.findViewById<TextView>(R.id.tvPresensiStatusMasuk)
        val tvPresensiJamPulang = view.findViewById<TextView>(R.id.tvPresensiJamPulang)
        val tvPresensiStatusPulang = view.findViewById<TextView>(R.id.tvPresensiStatusPulang)
        val btnPresensiScan = view.findViewById<Button>(R.id.btnPresensiScan)
        val btnPresensiIzin = view.findViewById<Button>(R.id.btnPresensiIzin)

        // Bind Section 6: Shortcuts
        val menuShortcutJadwal = view.findViewById<View>(R.id.menuShortcutJadwal)
        val menuShortcutTugas = view.findViewById<View>(R.id.menuShortcutTugas)
        val menuShortcutMateri = view.findViewById<View>(R.id.menuShortcutMateri)
        val menuShortcutNilai = view.findViewById<View>(R.id.menuShortcutNilai)
        val menuShortcutPresensi = view.findViewById<View>(R.id.menuShortcutPresensi)
        val menuShortcutCbt = view.findViewById<View>(R.id.menuShortcutCbt)
        val menuShortcutSpp = view.findViewById<View>(R.id.menuShortcutSpp)
        val menuShortcutBantuan = view.findViewById<View>(R.id.menuShortcutBantuan)

        // Bind Section 7: Tugas
        val btnLihatSemuaTugas = view.findViewById<View>(R.id.btnLihatSemuaTugas)
        val containerTugasPreview = view.findViewById<LinearLayout>(R.id.containerTugasPreview)

        // Bind Section 8: Tagihan
        val btnRincianTagihan = view.findViewById<View>(R.id.btnRincianTagihan)
        val tvTagihanTotalBesar = view.findViewById<TextView>(R.id.tvTagihanTotalBesar)
        val btnBayarSpp = view.findViewById<Button>(R.id.btnBayarSpp)
        val containerTagihanPreview = view.findViewById<LinearLayout>(R.id.containerTagihanPreview)

        // Initial default texts
        tvNamaDashboard?.text = "Selamat Datang, $userName!"
        tvHeroNisKelas?.text = "NISN: - • Kelas $initialKelas"

        // Auto-Hide Bottom Nav on Scroll
        val scrollView = view.findViewById<androidx.core.widget.NestedScrollView>(R.id.scrollView)
        scrollView?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 12) {
                (activity as? DashboardActivity)?.hideBottomNav()
            } else if (dy < -12 || scrollY <= 10) {
                (activity as? DashboardActivity)?.showBottomNav()
            }
        }

        // Wire Click Listeners
        cardStatPresensi?.setOnClickListener {
            requireActivity().findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 3
        }

        cardStatTugas?.setOnClickListener {
            startActivity(Intent(requireContext(), DaftarTugasActivity::class.java))
        }

        cardStatUjian?.setOnClickListener {
            requireActivity().findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 2
        }

        cardStatTagihan?.setOnClickListener {
            requireActivity().findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 4
        }

        btnMulaiUjian?.setOnClickListener {
            requireActivity().findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 2
        }

        btnKbmMateri?.setOnClickListener {
            startActivity(Intent(requireContext(), MateriBelajarActivity::class.java))
        }

        btnPresensiScan?.setOnClickListener {
            (activity as? DashboardActivity)?.cekRadiusDanScan()
        }

        btnPresensiIzin?.setOnClickListener {
            startActivity(Intent(requireContext(), IzinActivity::class.java))
        }

        // Shortcut clicks
        menuShortcutJadwal?.setOnClickListener {
            startActivity(Intent(requireContext(), JadwalPelajaranActivity::class.java))
        }

        menuShortcutTugas?.setOnClickListener {
            startActivity(Intent(requireContext(), DaftarTugasActivity::class.java))
        }

        menuShortcutMateri?.setOnClickListener {
            startActivity(Intent(requireContext(), MateriBelajarActivity::class.java))
        }

        menuShortcutNilai?.setOnClickListener {
            startActivity(Intent(requireContext(), NilaiRaportActivity::class.java))
        }

        menuShortcutPresensi?.setOnClickListener {
            requireActivity().findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 3
        }

        menuShortcutCbt?.setOnClickListener {
            requireActivity().findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 2
        }

        menuShortcutSpp?.setOnClickListener {
            requireActivity().findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 4
        }

        menuShortcutBantuan?.setOnClickListener {
            try {
                val phone = "6285155232366"
                val text = Uri.encode("Halo Admin SMKS Riyadhul Jannah Jalancagak, saya butuh bantuan terkait aplikasi.")
                val url = "https://api.whatsapp.com/send?phone=$phone&text=$text"
                val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(i)
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Aplikasi WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
            }
        }

        btnLihatSemuaTugas?.setOnClickListener {
            startActivity(Intent(requireContext(), DaftarTugasActivity::class.java))
        }

        btnRincianTagihan?.setOnClickListener {
            requireActivity().findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 4
        }

        btnBayarSpp?.setOnClickListener {
            requireActivity().findViewById<ViewPager2>(R.id.viewPager)?.currentItem = 4
        }

        // Fetch Dashboard Data from API
        fetchBerandaData(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { fetchBerandaData(it) }
    }

    override fun refreshData() {
        view?.let { fetchBerandaData(it) }
    }

    private fun fetchBerandaData(view: View) {
        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val nisn = sharedPref.getString("nisn", null) ?: return
        val userName = sharedPref.getString("nama_lengkap", "Siswa") ?: "Siswa"
        val ivProfilPhoto = requireActivity().findViewById<ImageView>(R.id.ivProfilPhoto)

        loadDashboardData(
            nisn = nisn,
            userName = userName,
            ivProfilPhoto = ivProfilPhoto,
            tvHeroTa = view.findViewById(R.id.tvHeroTa),
            tvHeroDate = view.findViewById(R.id.tvHeroDate),
            tvNamaDashboard = view.findViewById(R.id.tvNamaDashboard),
            tvHeroNisKelas = view.findViewById(R.id.tvHeroNisKelas),
            tvHeroJurusan = view.findViewById(R.id.tvHeroJurusan),
            tvHeroStatus = view.findViewById(R.id.tvHeroStatus),
            tvHeroWaliKelas = view.findViewById(R.id.tvHeroWaliKelas),
            tvHeroPoinDisiplin = view.findViewById(R.id.tvHeroPoinDisiplin),
            pbHeroDisiplin = view.findViewById(R.id.pbHeroDisiplin),
            tvStatPresensiVal = view.findViewById(R.id.tvStatPresensiVal),
            tvStatPresensiSub = view.findViewById(R.id.tvStatPresensiSub),
            tvStatTugasVal = view.findViewById(R.id.tvStatTugasVal),
            tvStatTugasSub = view.findViewById(R.id.tvStatTugasSub),
            tvStatUjianVal = view.findViewById(R.id.tvStatUjianVal),
            tvStatUjianSub = view.findViewById(R.id.tvStatUjianSub),
            tvStatTagihanVal = view.findViewById(R.id.tvStatTagihanVal),
            tvStatTagihanSub = view.findViewById(R.id.tvStatTagihanSub),
            layoutUjianHariIniWrapper = view.findViewById(R.id.layoutUjianHariIniWrapper),
            tvUjianBadge = view.findViewById(R.id.tvUjianBadge),
            tvUjianWaktu = view.findViewById(R.id.tvUjianWaktu),
            tvUjianJudul = view.findViewById(R.id.tvUjianJudul),
            tvUjianRuang = view.findViewById(R.id.tvUjianRuang),
            tvMapelHariPill = view.findViewById(R.id.tvMapelHariPill),
            cardActiveKbm = view.findViewById(R.id.cardActiveKbm),
            tvKbmStatusBadge = view.findViewById(R.id.tvKbmStatusBadge),
            tvKbmWaktuRuang = view.findViewById(R.id.tvKbmWaktuRuang),
            tvKbmMapel = view.findViewById(R.id.tvKbmMapel),
            tvKbmGuru = view.findViewById(R.id.tvKbmGuru),
            containerOtherSubjects = view.findViewById(R.id.containerOtherSubjects),
            tvPresensiRadiusPill = view.findViewById(R.id.tvPresensiRadiusPill),
            tvPresensiLokasiSub = view.findViewById(R.id.tvPresensiLokasiSub),
            tvPresensiJamMasuk = view.findViewById(R.id.tvPresensiJamMasuk),
            tvPresensiStatusMasuk = view.findViewById(R.id.tvPresensiStatusMasuk),
            tvPresensiJamPulang = view.findViewById(R.id.tvPresensiJamPulang),
            tvPresensiStatusPulang = view.findViewById(R.id.tvPresensiStatusPulang),
            containerTugasPreview = view.findViewById(R.id.containerTugasPreview),
            tvTagihanTotalBesar = view.findViewById(R.id.tvTagihanTotalBesar),
            containerTagihanPreview = view.findViewById(R.id.containerTagihanPreview)
        )
    }

    private fun loadDashboardData(
        nisn: String,
        userName: String,
        ivProfilPhoto: ImageView?,
        tvHeroTa: TextView?,
        tvHeroDate: TextView?,
        tvNamaDashboard: TextView?,
        tvHeroNisKelas: TextView?,
        tvHeroJurusan: TextView?,
        tvHeroStatus: TextView?,
        tvHeroWaliKelas: TextView?,
        tvHeroPoinDisiplin: TextView?,
        pbHeroDisiplin: ProgressBar?,
        tvStatPresensiVal: TextView?,
        tvStatPresensiSub: TextView?,
        tvStatTugasVal: TextView?,
        tvStatTugasSub: TextView?,
        tvStatUjianVal: TextView?,
        tvStatUjianSub: TextView?,
        tvStatTagihanVal: TextView?,
        tvStatTagihanSub: TextView?,
        layoutUjianHariIniWrapper: View?,
        tvUjianBadge: TextView?,
        tvUjianWaktu: TextView?,
        tvUjianJudul: TextView?,
        tvUjianRuang: TextView?,
        tvMapelHariPill: TextView?,
        cardActiveKbm: View?,
        tvKbmStatusBadge: TextView?,
        tvKbmWaktuRuang: TextView?,
        tvKbmMapel: TextView?,
        tvKbmGuru: TextView?,
        containerOtherSubjects: LinearLayout?,
        tvPresensiRadiusPill: TextView?,
        tvPresensiLokasiSub: TextView?,
        tvPresensiJamMasuk: TextView?,
        tvPresensiStatusMasuk: TextView?,
        tvPresensiJamPulang: TextView?,
        tvPresensiStatusPulang: TextView?,
        containerTugasPreview: LinearLayout?,
        tvTagihanTotalBesar: TextView?,
        containerTagihanPreview: LinearLayout?
    ) {
        LoadingDialogHelper.dismiss(loadingDialog)
        loadingDialog = LoadingDialogHelper.show(context, "Memuat dashboard siswa...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getDashboard(nisn)
                withContext(Dispatchers.Main) {
                    LoadingDialogHelper.dismiss(loadingDialog)
                    loadingDialog = null
                    if (!isAdded) return@withContext

                    if (response.isSuccessful && response.body()?.status == true && response.body()?.data != null) {
                        val data: DashboardData = response.body()?.data!!
                        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)

                        // 1. Hero Card
                        val namaFinal = data.nama ?: userName
                        tvNamaDashboard?.text = "Selamat Datang, $namaFinal!"

                        val ta = data.tahun_ajaran ?: "2024/2025"
                        val sem = data.semester ?: "Ganjil"
                        tvHeroTa?.text = "● TA $ta • $sem"

                        tvHeroDate?.text = data.tanggal_hari_ini ?: "Senin, 08 Sep 2026"

                        val nisText = data.nisn ?: data.nis ?: nisn ?: "-"
                        val kelasText = data.kelas ?: "-"
                        tvHeroNisKelas?.text = "NISN: $nisText • Kelas $kelasText"

                        tvHeroJurusan?.text = data.nama_jurusan ?: "Semua Jurusan"
                        tvHeroStatus?.text = "● ${data.status_siswa ?: "Siswa Aktif"}"
                        tvHeroWaliKelas?.text = "Wali Kelas : ${data.wali_kelas ?: "Belum Ditentukan"}"

                        val poin = data.poin_disiplin ?: 100
                        val predikat = data.predikat_disiplin ?: "Sangat Baik"
                        tvHeroPoinDisiplin?.text = "$poin / 100 ($predikat)"
                        pbHeroDisiplin?.progress = poin

                        // Save cache
                        sharedPref.edit()
                            .putString("kelas", kelasText)
                            .putString("nama_siswa", namaFinal)
                            .apply()

                        // Load Profile Photo / Initials in Activity Header
                        activity?.let { act ->
                            val tvProfilInisial = act.findViewById<TextView>(R.id.tvProfilInisial)
                            val cvProfilPic = act.findViewById<CardView>(R.id.cvProfilPic)
                            val rawFoto = data.foto_profil
                            val fullFotoUrl = if (!rawFoto.isNullOrBlank()) {
                                if (rawFoto.startsWith("http")) rawFoto else ApiClient.BASE_URL.trimEnd('/') + "/" + rawFoto.trimStart('/')
                            } else null
                            AvatarHelper.setAvatar(act, namaFinal, fullFotoUrl, ivProfilPhoto, tvProfilInisial, cvProfilPic)
                        }

                        // 2. 4 Stat Cards
                        val persenHadir = data.kehadiran_persen ?: 100.0
                        tvStatPresensiVal?.text = "$persenHadir%"
                        tvStatPresensiSub?.text = data.kehadiran_sub ?: "Hadir 0 hari • Izin 0 hari"

                        val countTugas = data.tugas_tertunda_count ?: data.tugas_aktif ?: 0
                        tvStatTugasVal?.text = "$countTugas Tugas"
                        tvStatTugasSub?.text = data.tugas_tertunda_sub ?: (if (countTugas > 0) "$countTugas Perlu Dikerjakan" else "Semua tugas selesai")

                        val countUjian = data.ujian_cbt_count ?: 0
                        tvStatUjianVal?.text = "$countUjian Ujian"
                        tvStatUjianSub?.text = data.ujian_cbt_sub ?: "Tidak ada ujian aktif"

                        tvStatTagihanVal?.text = data.total_tagihan_formatted ?: "Rp 0"
                        tvStatTagihanSub?.text = data.tagihan_sub ?: "Semua Tagihan Lunas"

                        // 3. Section Ujian Hari Ini
                        if (data.has_active_exam == true && data.active_exam != null) {
                            layoutUjianHariIniWrapper?.visibility = View.VISIBLE
                            tvUjianBadge?.text = "PENILAIAN CBT ONLINE"
                            tvUjianWaktu?.text = data.active_exam.waktu ?: "08:00 - 10:00 WIB"
                            tvUjianJudul?.text = data.active_exam.nama_mapel ?: "Ujian Online"
                            tvUjianRuang?.text = data.active_exam.ruang ?: "Ruang CBT Online"
                        } else {
                            layoutUjianHariIniWrapper?.visibility = View.GONE
                        }

                        // 4. Section Mata Pelajaran Hari Ini
                        tvMapelHariPill?.text = data.hari_ini_nama ?: "Hari Ini"
                        val isKbmAktif = (data.has_active_kbm == true && data.active_kbm != null && data.active_kbm.is_active == true)
                        if (isKbmAktif) {
                            cardActiveKbm?.visibility = View.VISIBLE
                            val kbm = data.active_kbm!!
                            val jamKe = kbm.jam_ke ?: 1
                            tvKbmStatusBadge?.text = "● Sedang Berlangsung (Jam Ke $jamKe)"
                            tvKbmWaktuRuang?.text = "${kbm.waktu ?: "-"} • ${kbm.ruang ?: "Ruang Kelas"}"
                            tvKbmMapel?.text = kbm.nama_mapel ?: "Mata Pelajaran"
                            tvKbmGuru?.text = "Guru: ${kbm.guru ?: "-"}"
                        } else {
                            cardActiveKbm?.visibility = View.GONE
                        }

                        // Populate other subjects
                        containerOtherSubjects?.removeAllViews()
                        if (data.jadwal_hari_ini != null && data.jadwal_hari_ini.isNotEmpty()) {
                            containerOtherSubjects?.visibility = View.VISIBLE
                            for (jadwal in data.jadwal_hari_ini) {
                                // Exclude the active subject from the other subjects preview only when active card is showing
                                if (isKbmAktif && data.active_kbm?.nama_mapel == jadwal.nama_mapel) {
                                    continue
                                }
                                val itemView = layoutInflater.inflate(R.layout.item_jadwal_preview_line, containerOtherSubjects, false)
                                itemView.findViewById<TextView>(R.id.tvWaktuItem).text = jadwal.waktu ?: "${jadwal.jam_mulai} - ${jadwal.jam_selesai} WIB"
                                itemView.findViewById<TextView>(R.id.tvMapelGuruItem).text = "${jadwal.nama_mapel} (${jadwal.guru ?: "-"})"
                                containerOtherSubjects?.addView(itemView)
                            }
                        } else {
                            containerOtherSubjects?.visibility = View.GONE
                        }

                        // 5. Presensi Kehadiran Sekolah
                        if (data.presensi_sekolah != null) {
                            val ps = data.presensi_sekolah
                            tvPresensiRadiusPill?.text = "● ${ps.radius_info ?: "Radius 100m Aktif"}"
                            tvPresensiLokasiSub?.text = "Lokasi: ${ps.lokasi_sekolah ?: "SMKS Riyadhul Jannah Jalancagak"} • ${ps.radius_info ?: "Radius Aktif"}"
                            tvPresensiJamMasuk?.text = ps.jam_masuk ?: "--:-- WIB"
                            tvPresensiStatusMasuk?.text = "● ${ps.jam_masuk_status ?: "Belum Presensi"}"
                            tvPresensiJamPulang?.text = ps.jam_pulang ?: "--:-- WIB"
                            tvPresensiStatusPulang?.text = "● ${ps.jam_pulang_status ?: "Menunggu Waktu"}"
                        }

                        // 7. Preview Tugas Sekolah (Hanya tugas yang belum selesai)
                        containerTugasPreview?.removeAllViews()
                        val pendingTasks = data.tugas_preview?.filter { it.is_selesai != true }
                        if (pendingTasks != null && pendingTasks.isNotEmpty()) {
                            for (tugas in pendingTasks) {
                                val itemTugas = layoutInflater.inflate(R.layout.item_tugas_preview, containerTugasPreview, false)
                                itemTugas.findViewById<TextView>(R.id.tvMapelTugas).text = tugas.mapel ?: "Mata Pelajaran"
                                itemTugas.findViewById<TextView>(R.id.tvDeadlineTugas).text = "Tenggat: ${tugas.deadline ?: "-"}"
                                itemTugas.findViewById<TextView>(R.id.tvJudulTugas).text = tugas.judul ?: "-"
                                itemTugas.findViewById<TextView>(R.id.tvGuruTugas).text = "Guru: ${tugas.guru ?: "-"}"
                                
                                val tvStatus = itemTugas.findViewById<TextView>(R.id.tvStatusTugas)
                                tvStatus.text = "● ${tugas.status ?: "Belum Dikumpulkan"}"

                                itemTugas.findViewById<View>(R.id.btnAksiTugas).setOnClickListener {
                                    startActivity(Intent(requireContext(), DaftarTugasActivity::class.java))
                                }

                                containerTugasPreview?.addView(itemTugas)
                            }
                        } else {
                            val emptyText = TextView(requireContext()).apply {
                                text = "Tidak ada tugas tertunda saat ini."
                                textSize = 12f
                                setTextColor(android.graphics.Color.parseColor("#94A3B8"))
                                setPadding(0, 16, 0, 16)
                            }
                            containerTugasPreview?.addView(emptyText)
                        }

                        // 8. Preview Tagihan Biaya Sekolah
                        tvTagihanTotalBesar?.text = data.total_tagihan_formatted ?: "Rp 0"
                        containerTagihanPreview?.removeAllViews()
                        if (data.tagihan_preview != null && data.tagihan_preview.isNotEmpty()) {
                            for (tagihan in data.tagihan_preview) {
                                val itemTagihan = layoutInflater.inflate(R.layout.item_tagihan_preview, containerTagihanPreview, false)
                                itemTagihan.findViewById<TextView>(R.id.tvNamaTagihanItem).text = tagihan.nama_pos ?: "Tagihan Sekolah"
                                itemTagihan.findViewById<TextView>(R.id.tvNominalTagihanItem).text = tagihan.nominal_formatted ?: "Rp 0"
                                itemTagihan.findViewById<TextView>(R.id.tvStatusTagihanItem).text = tagihan.status ?: "Belum Bayar"

                                containerTagihanPreview?.addView(itemTagihan)
                            }
                        } else {
                            val emptyTagihan = TextView(requireContext()).apply {
                                text = "Semua kewajiban administrasi sekolah lunas."
                                textSize = 12f
                                setTextColor(android.graphics.Color.parseColor("#10B981"))
                                setPadding(0, 16, 0, 16)
                            }
                            containerTagihanPreview?.addView(emptyTagihan)
                        }

                    } else {
                        Toast.makeText(requireContext(), "Gagal memperbarui dashboard", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingDialogHelper.dismiss(loadingDialog)
                    loadingDialog = null
                    if (isAdded) {
                        Toast.makeText(requireContext(), "Koneksi Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
