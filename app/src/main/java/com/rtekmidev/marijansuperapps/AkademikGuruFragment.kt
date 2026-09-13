package com.rtekmidev.marijansuperapps

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rtekmidev.marijansuperapps.api.ApiClient
import com.rtekmidev.marijansuperapps.api.DashboardGuruData
import com.rtekmidev.marijansuperapps.api.KbmAktifInfo
import com.rtekmidev.marijansuperapps.util.LoadingDialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale

@SuppressLint("SetTextI18n")
class AkademikGuruFragment : Fragment(), com.rtekmidev.marijansuperapps.util.RefreshableFragment {

    private var cachedData: DashboardGuruData? = null
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
        return inflater.inflate(R.layout.fragment_akademik_guru, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initStaticClickListeners(view)
        loadDashboardData(view)
    }

    override fun onResume() {
        super.onResume()
        view?.let { loadDashboardData(it) }
    }

    override fun refreshData() {
        view?.let { loadDashboardData(it) }
    }

    private fun initStaticClickListeners(view: View) {
        // Quick Action 2: Rekap Mengajar
        view.findViewById<View>(R.id.btnAksiRekapMengajar)?.setOnClickListener {
            val intent = Intent(requireContext(), RekapMengajarActivity::class.java)
            startActivity(intent)
            requireActivity().applyEnterTransition()
        }

        // Quick Action 3: Jadwal Mengajar
        view.findViewById<View>(R.id.btnAksiJadwalMengajar)?.setOnClickListener {
            val intent = Intent(requireContext(), JadwalMengajarActivity::class.java)
            startActivity(intent)
            requireActivity().applyEnterTransition()
        }

        // Auto-Hide Bottom Nav on Scroll
        (view as? androidx.core.widget.NestedScrollView)?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 12) {
                (activity as? DashboardGuruActivity)?.hideBottomNav()
            } else if (dy < -12 || scrollY <= 10) {
                (activity as? DashboardGuruActivity)?.showBottomNav()
            }
        }
    }

    private fun loadDashboardData(view: View) {
        val sharedPref = requireActivity().getSharedPreferences("SesiGuru", Context.MODE_PRIVATE)
        val idUser = sharedPref.getString("id_user", null)

        if (idUser.isNullOrEmpty()) return

        LoadingDialogHelper.dismiss(loadingDialog)
        loadingDialog = LoadingDialogHelper.show(context, "Memuat data akademik guru...")

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getAkademikGuruDashboard(idUser)
                withContext(Dispatchers.Main) {
                    LoadingDialogHelper.dismiss(loadingDialog)
                    loadingDialog = null
                    if (isAdded && response.isSuccessful && response.body()?.status == true) {
                        val data = response.body()?.data
                        if (data != null) {
                            cachedData = data
                            renderDashboard(view, data)
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
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingDialogHelper.dismiss(loadingDialog)
                    loadingDialog = null
                }
            }
        }
    }

    private fun renderDashboard(view: View, data: DashboardGuruData) {
        // 1. HERO BANNER
        view.findViewById<TextView>(R.id.tvHeroTahunAjaran)?.text =
            data.tahun_ajaran_aktif ?: "● TA 2026/2027 • Ganjil"

        view.findViewById<TextView>(R.id.tvHeroTanggal)?.text =
            data.tanggal_hari_ini_formatted ?: "Hari Ini"

        val namaGuru = data.guru_info?.nama ?: "Bapak/Ibu Guru"
        view.findViewById<TextView>(R.id.tvHeroNamaGuru)?.text = namaGuru

        val nipStr = data.guru_info?.nip ?: "-"
        val mapelUtama = data.guru_info?.mapel_utama ?: "Guru Pengajar"
        view.findViewById<TextView>(R.id.tvHeroNipMapel)?.text = "NIP: $nipStr • $mapelUtama"

        val badgeHeroWali = view.findViewById<View>(R.id.badgeHeroWaliKelas)
        val tvHeroWaliText = view.findViewById<TextView>(R.id.tvHeroWaliKelasText)
        if (data.wali_kelas_info != null && !data.wali_kelas_info.nama_kelas.isNullOrEmpty()) {
            badgeHeroWali?.visibility = View.VISIBLE
            val kls = data.wali_kelas_info.nama_kelas
            val jml = data.wali_kelas_info.total_siswa ?: 0
            tvHeroWaliText?.text = "Wali Kelas $kls ($jml Siswa)"
        } else {
            badgeHeroWali?.visibility = View.GONE
        }

        val statusKbm = data.status_kbm ?: "Aktif Mengajar"
        view.findViewById<TextView>(R.id.tvHeroStatusKbm)?.text = "● Status KBM: $statusKbm"

        val mingguKe = data.minggu_efektif_ke ?: 7
        val totalMinggu = data.total_minggu_efektif ?: 18
        view.findViewById<TextView>(R.id.tvHeroMingguEfektif)?.text =
            "Minggu Efektif ke-$mingguKe dari $totalMinggu"

        val pbMinggu = view.findViewById<ProgressBar>(R.id.pbMingguEfektif)
        val progressPct = data.progress_minggu ?: ((mingguKe.toFloat() / totalMinggu.toFloat()) * 100).toInt()
        pbMinggu?.progress = progressPct


        // 2. STATISTIK BEBAN MENGAJAR
        view.findViewById<TextView>(R.id.tvBadgeJjm)?.text = data.badge_jjm ?: "Sesuai JJM"
        view.findViewById<TextView>(R.id.tvPersenTargetJam)?.text = "${data.persen_target_jam ?: 100}% Target"
        view.findViewById<TextView>(R.id.tvTotalJamMingguIni)?.text = data.total_jam_minggu_ini.toString()

        val sesiTerisi = data.progres_jurnal_bulan_ini?.sesi_terisi ?: 0
        val totalSesi = data.progres_jurnal_bulan_ini?.total_sesi ?: 24
        val persenJurnal = data.progres_jurnal_bulan_ini?.persentase ?: 0
        view.findViewById<TextView>(R.id.tvRasioSesiJurnal)?.text = "$sesiTerisi/$totalSesi Sesi"
        view.findViewById<TextView>(R.id.tvPersenJurnalTerisi)?.text = "$persenJurnal%"

        view.findViewById<TextView>(R.id.tvTotalRombel)?.text = "${data.total_rombel ?: 3} Kelas"
        view.findViewById<TextView>(R.id.tvRombelSummary)?.text = data.rombel_summary ?: "Semua Rombel"

        view.findViewById<TextView>(R.id.tvTotalSiswaDiajar)?.text = "${data.total_siswa_diajar ?: 0} Siswa"
        view.findViewById<TextView>(R.id.tvStatusSiswaDiajar)?.text = data.status_siswa_diajar ?: "Semua Aktif"


        // 3. KBM SEDANG BERLANGSUNG (LIVE SESSION)
        renderKbmAktif(view, data.kbm_aktif, data)


        // 4. MATA PELAJARAN DIAMPU
        view.findViewById<TextView>(R.id.tvTotalJamMapelHeader)?.text =
            "Total: ${data.total_jam_minggu_ini} Jam/Mg"

        view.findViewById<TextView>(R.id.tvKurikulumText)?.text =
            data.kurikulum_text ?: "Kurikulum Merdeka SMK PK"

        val containerMapel = view.findViewById<LinearLayout>(R.id.containerMapelDiampu)
        containerMapel?.removeAllViews()

        val listMapel = data.mata_pelajaran_diampu
        if (!listMapel.isNullOrEmpty()) {
            val inflater = LayoutInflater.from(requireContext())
            val iconBgs = listOf("#EDE9FE", "#CCFBF1", "#E0F2FE")
            val iconTints = listOf("#6D28D9", "#0D9488", "#0284C7")
            val progressDrawables = listOf(R.drawable.bg_progress_blue, R.drawable.bg_progress_blue, R.drawable.bg_progress_blue)

            listMapel.forEachIndexed { index, mapel ->
                val item = inflater.inflate(R.layout.item_mapel_diampu, containerMapel, false)

                item.findViewById<TextView>(R.id.tvNamaMapel)?.text = mapel.nama_mapel ?: "-"
                item.findViewById<TextView>(R.id.tvNamaKelas)?.text = mapel.nama_kelas ?: "-"
                item.findViewById<TextView>(R.id.tvTotalJam)?.text = (mapel.jp_per_minggu ?: 0).toString()

                // Vary icon color themes
                val colorIdx = index % iconBgs.size
                val flIcon = item.findViewById<FrameLayout>(R.id.flMapelIconBg)
                val ivIcon = item.findViewById<ImageView>(R.id.ivMapelIcon)
                flIcon?.backgroundTintList = ColorStateList.valueOf(Color.parseColor(iconBgs[colorIdx]))
                ivIcon?.imageTintList = ColorStateList.valueOf(Color.parseColor(iconTints[colorIdx]))

                containerMapel?.addView(item)
            }
        }


        // 5. AKSI CEPAT (QUICK ACTIONS)
        view.findViewById<View>(R.id.btnAksiIsiJurnal)?.setOnClickListener {
            handleIsiJurnalAction(data.kbm_aktif, data)
        }


        // 6. MENU WALI KELAS & MONITORING SPP
        val sectionWaliKelas = view.findViewById<View>(R.id.sectionWaliKelas)
        if (data.wali_kelas_info != null && !data.wali_kelas_info.nama_kelas.isNullOrEmpty()) {
            sectionWaliKelas?.visibility = View.VISIBLE
            val namaKelas = data.wali_kelas_info.nama_kelas ?: "-"
            val totalSiswa = data.wali_kelas_info.total_siswa ?: 0

            view.findViewById<TextView>(R.id.tvWaliKelasBadge)?.text = "$namaKelas ($totalSiswa Siswa)"

            view.findViewById<View>(R.id.btnWaliAbsenHarian)?.setOnClickListener {
                val intent = Intent(requireContext(), WaliKelasAbsenHarianActivity::class.java)
                intent.putExtra("nama_kelas", namaKelas)
                startActivity(intent)
                requireActivity().applyEnterTransition()
            }

            view.findViewById<View>(R.id.btnWaliRekapKehadiran)?.setOnClickListener {
                val intent = Intent(requireContext(), WaliKelasKehadiranActivity::class.java)
                intent.putExtra("nama_kelas", namaKelas)
                intent.putExtra("mode", "rekap")
                startActivity(intent)
                requireActivity().applyEnterTransition()
            }

            view.findViewById<View>(R.id.btnWaliKeuanganKelas)?.setOnClickListener {
                val intent = Intent(requireContext(), WaliKelasKeuanganActivity::class.java)
                intent.putExtra("nama_kelas", namaKelas)
                startActivity(intent)
                requireActivity().applyEnterTransition()
            }

            // Banner Monitoring SPP Kelas
            view.findViewById<TextView>(R.id.tvMonitoringSppTitle)?.text = "Monitoring SPP Kelas $namaKelas"

            val tagihan = data.keuangan_kelas_summary?.total_tagihan ?: 0L
            val terbayar = data.keuangan_kelas_summary?.total_terbayar ?: 0L
            val localeId = Locale.forLanguageTag("id-ID")
            val nf = NumberFormat.getCurrencyInstance(localeId)
            val strTerbayar = nf.format(terbayar).replace(",00", "")
            val strTagihan = nf.format(tagihan).replace(",00", "")
            view.findViewById<TextView>(R.id.tvMonitoringSppNominal)?.text = "$strTerbayar / $strTagihan"

            view.findViewById<View>(R.id.cardMonitoringSpp)?.setOnClickListener {
                val intent = Intent(requireContext(), WaliKelasKeuanganActivity::class.java)
                intent.putExtra("nama_kelas", namaKelas)
                startActivity(intent)
                requireActivity().applyEnterTransition()
            }
        } else {
            sectionWaliKelas?.visibility = View.GONE
        }
    }

    private fun renderKbmAktif(view: View, kbm: KbmAktifInfo?, fullData: DashboardGuruData) {
        val tvHeaderTitle = view.findViewById<TextView>(R.id.tvHeaderKbmTitle)
        val tvLiveIndicator = view.findViewById<TextView>(R.id.tvLiveIndicator)
        val tvStatus = view.findViewById<TextView>(R.id.tvKbmStatusBadge)
        val tvJamKe = view.findViewById<TextView>(R.id.tvKbmJamKe)
        val tvNamaMapel = view.findViewById<TextView>(R.id.tvKbmNamaMapel)
        val tvKelas = view.findViewById<TextView>(R.id.tvKbmKelas)
        val tvRuang = view.findViewById<TextView>(R.id.tvKbmRuang)
        val btnIsiJurnal = view.findViewById<View>(R.id.btnKbmIsiJurnal)
        val tvBtnIsiText = view.findViewById<TextView>(R.id.tvBtnKbmIsiJurnalText)

        val isLibur = fullData.is_libur == true || fullData.wali_kelas_info?.is_libur == true

        if (isLibur) {
            // Kondisi Hari Libur Sekolah / Akhir Pekan
            tvHeaderTitle?.text = "● Status KBM (Hari Libur)"
            tvHeaderTitle?.setTextColor(Color.parseColor("#64748B"))
            tvLiveIndicator?.visibility = View.GONE

            val ketLibur = fullData.keterangan_libur ?: fullData.wali_kelas_info?.keterangan_libur ?: "Libur Sekolah"
            tvStatus?.text = "● $ketLibur"
            tvStatus?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
            tvStatus?.setTextColor(Color.parseColor("#64748B"))

            tvJamKe?.text = fullData.tanggal_hari_ini_formatted ?: "-"
            tvNamaMapel?.text = "Hari Libur Sekolah"
            tvKelas?.text = fullData.keterangan_libur ?: "Tidak Ada Kegiatan KBM"
            tvRuang?.text = "Sekolah Libur"

            tvBtnIsiText?.text = "Hari Libur"
            btnIsiJurnal?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#94A3B8"))
            btnIsiJurnal?.setOnClickListener {
                Toast.makeText(requireContext(), "Hari ini libur sekolah, tidak ada kegiatan KBM.", Toast.LENGTH_SHORT).show()
            }
        } else if (kbm != null) {
            // Sesi KBM sedang berlangsung atau terjadwal hari ini
            tvHeaderTitle?.text = if (kbm.is_active) "● KBM Sedang Berlangsung" else "● Jadwal KBM Hari Ini"
            tvHeaderTitle?.setTextColor(if (kbm.is_active) Color.parseColor("#0D9488") else Color.parseColor("#475569"))
            tvLiveIndicator?.visibility = if (kbm.is_active) View.VISIBLE else View.GONE

            tvStatus?.text = "● ${kbm.status_badge ?: if (kbm.is_active) "Sedang Berlangsung" else "Jadwal Hari Ini"}"
            if (kbm.is_active) {
                tvStatus?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                tvStatus?.setTextColor(Color.parseColor("#166534"))
            } else {
                tvStatus?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E0F2FE"))
                tvStatus?.setTextColor(Color.parseColor("#0369A1"))
            }

            tvJamKe?.text = kbm.jam_ke ?: "-"
            tvNamaMapel?.text = kbm.nama_mapel ?: "-"
            tvKelas?.text = kbm.nama_kelas ?: "-"
            tvRuang?.text = kbm.ruang ?: "Lab Komputer"

            if (kbm.is_jurnal_filled) {
                tvBtnIsiText?.text = "Jurnal Terisi ✓"
                btnIsiJurnal?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#059669"))
            } else {
                tvBtnIsiText?.text = "Isi Jurnal KBM"
                btnIsiJurnal?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E1B4B"))
            }

            btnIsiJurnal?.setOnClickListener {
                bukaJurnalMengajar(kbm)
            }
        } else {
            // State ketika tidak ada jadwal aktif saat ini
            tvHeaderTitle?.text = "● Status KBM Hari Ini"
            tvHeaderTitle?.setTextColor(Color.parseColor("#64748B"))
            tvLiveIndicator?.visibility = View.GONE

            tvStatus?.text = "● Tidak Ada KBM Aktif"
            tvStatus?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
            tvStatus?.setTextColor(Color.parseColor("#64748B"))

            tvJamKe?.text = fullData.tanggal_hari_ini_formatted ?: "-"
            tvNamaMapel?.text = "Tidak Ada KBM Berlangsung Saat Ini"
            tvKelas?.text = "Cek Jadwal Mingguan"
            tvRuang?.text = "Ruang Kelas"

            tvBtnIsiText?.text = "Lihat Jadwal Mengajar"
            btnIsiJurnal?.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1E1B4B"))
            btnIsiJurnal?.setOnClickListener {
                startActivity(Intent(requireContext(), JadwalMengajarActivity::class.java))
                requireActivity().applyEnterTransition()
            }
        }
    }

    private fun handleIsiJurnalAction(kbm: KbmAktifInfo?, fullData: DashboardGuruData) {
        val isLibur = fullData.is_libur == true || fullData.wali_kelas_info?.is_libur == true
        if (isLibur) {
            Toast.makeText(requireContext(), "Hari ini libur sekolah, tidak ada pengisian jurnal KBM.", Toast.LENGTH_SHORT).show()
            return
        }

        if (kbm != null && !kbm.id_kelas.isNullOrEmpty()) {
            bukaJurnalMengajar(kbm)
        } else {
            val list = fullData.jadwal_hari_ini
            if (!list.isNullOrEmpty()) {
                val first = list.first()
                val intent = Intent(requireContext(), JurnalMengajarActivity::class.java)
                intent.putExtra("id_kelas", first.id_kelas ?: "")
                intent.putExtra("id_mapel", first.id_mapel ?: "")
                intent.putExtra("nama_kelas", first.nama_kelas ?: "")
                intent.putExtra("nama_mapel", first.nama_mapel ?: "")
                intent.putExtra("jam_ke", first.jam_ke ?: "")
                startActivity(intent)
                requireActivity().applyEnterTransition()
            } else {
                Toast.makeText(requireContext(), "Tidak ada jadwal mengajar aktif hari ini.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun bukaJurnalMengajar(kbm: KbmAktifInfo) {
        val intent = Intent(requireContext(), JurnalMengajarActivity::class.java)
        intent.putExtra("id_kelas", kbm.id_kelas ?: "")
        intent.putExtra("id_mapel", kbm.id_mapel ?: "")
        intent.putExtra("nama_kelas", kbm.nama_kelas?.replace("Kelas ", "") ?: "")
        intent.putExtra("nama_mapel", kbm.nama_mapel ?: "")
        intent.putExtra("jam_ke", kbm.jam_ke ?: "")
        startActivity(intent)
        requireActivity().applyEnterTransition()
    }
}
