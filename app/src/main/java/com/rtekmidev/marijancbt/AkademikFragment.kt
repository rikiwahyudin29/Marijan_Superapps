package com.rtekmidev.marijancbt

import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rtekmidev.marijancbt.api.ApiClient
import com.rtekmidev.marijancbt.api.DashboardData
import com.rtekmidev.marijancbt.api.MateriPreviewItem
import com.rtekmidev.marijancbt.api.TugasPreviewItem
import com.rtekmidev.marijancbt.util.AvatarHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AkademikFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_akademik, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        val nisn = sharedPref.getString("nisn", null)

        // Scroll listener to auto hide/show parent bottom nav
        view.findViewById<View>(R.id.scrollViewAkademik)?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 12) {
                (activity as? DashboardActivity)?.hideBottomNav()
            } else if (dy < -12 || scrollY <= 10) {
                (activity as? DashboardActivity)?.showBottomNav()
            }
        }

        // 1. Hero Card Views
        val tvTahunAjaranSemester = view.findViewById<TextView>(R.id.tvTahunAjaranSemester)
        val tvTanggalHariIni = view.findViewById<TextView>(R.id.tvTanggalHariIni)
        val tvTingkatCircle = view.findViewById<TextView>(R.id.tvTingkatCircle)
        val tvNamaSiswaHero = view.findViewById<TextView>(R.id.tvNamaSiswaHero)
        val tvKelasNisnHero = view.findViewById<TextView>(R.id.tvKelasNisnHero)
        val tvJurusanBadgeHero = view.findViewById<TextView>(R.id.tvJurusanBadgeHero)
        val tvMingguEfektifLabel = view.findViewById<TextView>(R.id.tvMingguEfektifLabel)
        val tvPersenSelesaiLabel = view.findViewById<TextView>(R.id.tvPersenSelesaiLabel)
        val pbMingguEfektif = view.findViewById<ProgressBar>(R.id.pbMingguEfektif)

        // 2. Ringkasan Akademik Views
        val tvRataRataNilai = view.findViewById<TextView>(R.id.tvRataRataNilai)
        val tvRataRataBadge = view.findViewById<TextView>(R.id.tvRataRataBadge)
        val tvTugasAktifCount = view.findViewById<TextView>(R.id.tvTugasAktifCount)
        val tvTugasUrgentBadge = view.findViewById<TextView>(R.id.tvTugasUrgentBadge)
        val tvTotalMateriCount = view.findViewById<TextView>(R.id.tvTotalMateriCount)
        val tvMateriBaruBadge = view.findViewById<TextView>(R.id.tvMateriBaruBadge)

        // 3. Menu Utama Views
        val tvSubMenuTugas = view.findViewById<TextView>(R.id.tvSubMenuTugas)
        val tvSubMenuMateri = view.findViewById<TextView>(R.id.tvSubMenuMateri)
        val tvSubMenuRaport = view.findViewById<TextView>(R.id.tvSubMenuRaport)

        // 4. Sedang Berlangsung (KBM) Views
        val tvKbmStatusBadge = view.findViewById<TextView>(R.id.tvKbmStatusBadge)
        val tvKbmWaktu = view.findViewById<TextView>(R.id.tvKbmWaktu)
        val tvKbmMapel = view.findViewById<TextView>(R.id.tvKbmMapel)
        val tvKbmGuruRuang = view.findViewById<TextView>(R.id.tvKbmGuruRuang)
        val btnBukaModulKbm = view.findViewById<TextView>(R.id.btnBukaModulKbm)

        // 5. Tugas Mendatang Container
        val tvCountTugasBadge = view.findViewById<TextView>(R.id.tvCountTugasBadge)
        val llTugasMendatang = view.findViewById<LinearLayout>(R.id.llTugasMendatang)
        val tvEmptyTugas = view.findViewById<TextView>(R.id.tvEmptyTugas)

        // 6. Aktivitas & Materi Terkini Container
        val llMateriTerkini = view.findViewById<LinearLayout>(R.id.llMateriTerkini)
        val tvEmptyMateri = view.findViewById<TextView>(R.id.tvEmptyMateri)

        // Load Real Data from Backend
        if (!nisn.isNullOrEmpty()) {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    val response = ApiClient.instance.getDashboard(nisn)
                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful && response.body()?.status == true && response.body()?.data != null) {
                            val data = response.body()?.data!!

                            // Update Top Bar Initials Avatar
                            activity?.let { act ->
                                val ivProfilPhoto = act.findViewById<ImageView>(R.id.ivProfilPhoto)
                                val tvProfilInisial = act.findViewById<TextView>(R.id.tvProfilInisial)
                                val cvProfilPic = act.findViewById<CardView>(R.id.cvProfilPic)
                                AvatarHelper.setAvatar(act, data.nama, data.foto_profil, ivProfilPhoto, tvProfilInisial, cvProfilPic)
                            }

                            // 1. Bind Hero Card
                            val ta = data.tahun_ajaran ?: "2026/2027"
                            val sem = data.semester?.uppercase() ?: "GANJIL"
                            tvTahunAjaranSemester?.text = "• TA $ta • $sem"
                            tvTanggalHariIni?.text = data.tanggal_hari_ini ?: ""
                            tvTingkatCircle?.text = data.tingkat ?: "12"
                            tvNamaSiswaHero?.text = data.nama ?: "Siswa SMK"
                            tvKelasNisnHero?.text = "${data.kelas ?: "-"} • NISN: ${data.nisn ?: "-"}"
                            tvJurusanBadgeHero?.text = data.jurusan_singkat ?: (data.nama_jurusan?.take(4)?.uppercase() ?: "SMK")

                            tvMingguEfektifLabel?.text = data.pekan_kbm_text ?: "Minggu Efektif KBM (Pekan 7/18)"
                            tvPersenSelesaiLabel?.text = data.pekan_kbm_persen_text ?: "${data.pekan_kbm_persen ?: 38}% Selesai"
                            pbMingguEfektif?.progress = data.pekan_kbm_persen ?: 38

                            // 2. Bind Ringkasan Akademik
                            val rataNilai = data.rata_rata_nilai ?: 0.0
                            tvRataRataNilai?.text = if (rataNilai > 0.0) String.format("%.1f", rataNilai) else "0.0"
                            tvRataRataBadge?.text = data.rata_rata_badge ?: "+2.4 pts"

                            val tugasAktif = data.tugas_aktif ?: 0
                            tvTugasAktifCount?.text = "$tugasAktif"
                            val urgentCount = data.tugas_urgent_count ?: 0
                            if (urgentCount > 0) {
                                tvTugasUrgentBadge?.text = "$urgentCount Urgent"
                                tvTugasUrgentBadge?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_red_soft)
                                tvTugasUrgentBadge?.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                            } else {
                                tvTugasUrgentBadge?.text = "Aktif"
                                tvTugasUrgentBadge?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_green_soft)
                                tvTugasUrgentBadge?.setTextColor(android.graphics.Color.parseColor("#059669"))
                            }

                            val totalMateri = data.total_materi ?: 0
                            tvTotalMateriCount?.text = "$totalMateri"
                            val materiBaru = data.materi_baru_count ?: 0
                            tvMateriBaruBadge?.text = if (materiBaru > 0) "$materiBaru Baru" else "Tersedia"

                            // 3. Bind Menu Utama Subtitles
                            tvSubMenuTugas?.text = if (tugasAktif > 0) "$tugasAktif Belum Selesai" else "Semua Selesai"
                            tvSubMenuMateri?.text = "$totalMateri Modul PDF"
                            tvSubMenuRaport?.text = "Transkrip Nilai"

                            // 4. Bind Sedang Berlangsung (KBM Card)
                            if (data.has_active_kbm == true && data.active_kbm != null) {
                                tvKbmStatusBadge?.text = "• SEDANG BERLANGSUNG"
                                tvKbmStatusBadge?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_red_soft)
                                tvKbmStatusBadge?.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                                tvKbmWaktu?.text = "Jam Ke ${data.active_kbm.jam_ke ?: 1} • ${data.active_kbm.waktu ?: "-"}"
                                tvKbmMapel?.text = data.active_kbm.nama_mapel ?: "Mata Pelajaran"
                                tvKbmGuruRuang?.text = "${data.active_kbm.guru ?: "Guru Mapel"} • ${data.active_kbm.ruang ?: "Ruang Kelas"}"
                            } else if (data.next_kbm != null) {
                                tvKbmStatusBadge?.text = "• JADWAL BERIKUTNYA"
                                tvKbmStatusBadge?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_blue_soft)
                                tvKbmStatusBadge?.setTextColor(android.graphics.Color.parseColor("#2563EB"))
                                tvKbmWaktu?.text = "Jam Ke ${data.next_kbm.jam_ke ?: 1} • ${data.next_kbm.waktu ?: "-"}"
                                tvKbmMapel?.text = data.next_kbm.nama_mapel ?: "Mata Pelajaran"
                                tvKbmGuruRuang?.text = "${data.next_kbm.guru ?: "Guru Mapel"} • ${data.next_kbm.ruang ?: "Ruang Kelas"}"
                            } else if (!data.jadwal_hari_ini.isNullOrEmpty()) {
                                // Hari ini ada jadwal pelajaran, jam KBM telah usai
                                val lastKbm = data.jadwal_hari_ini.last()
                                tvKbmStatusBadge?.text = "• KBM HARI INI SELESAI"
                                tvKbmStatusBadge?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_green_soft)
                                tvKbmStatusBadge?.setTextColor(android.graphics.Color.parseColor("#059669"))
                                tvKbmWaktu?.text = "Selesai Pukul ${lastKbm.jam_selesai ?: "-"} WIB (${data.jadwal_hari_ini.size} Mapel)"
                                tvKbmMapel?.text = lastKbm.nama_mapel ?: "KBM Hari Ini Selesai"
                                tvKbmGuruRuang?.text = "${lastKbm.guru ?: "Guru Mapel"} • ${lastKbm.ruang ?: "Ruang Kelas"}"
                            } else if (data.next_day_kbm != null) {
                                val hariLanjut = data.next_day_kbm.hari?.uppercase() ?: "SENIN"
                                tvKbmStatusBadge?.text = "• JADWAL $hariLanjut"
                                tvKbmStatusBadge?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_blue_soft)
                                tvKbmStatusBadge?.setTextColor(android.graphics.Color.parseColor("#2563EB"))
                                tvKbmWaktu?.text = "Jam Ke 1 • ${data.next_day_kbm.waktu ?: "-"}"
                                tvKbmMapel?.text = data.next_day_kbm.nama_mapel ?: "Mata Pelajaran"
                                tvKbmGuruRuang?.text = "${data.next_day_kbm.guru ?: "Guru Pengampu"} • ${data.next_day_kbm.ruang ?: "Ruang Kelas"}"
                            } else {
                                tvKbmStatusBadge?.text = "• JADWAL KBM"
                                tvKbmStatusBadge?.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_green_soft)
                                tvKbmStatusBadge?.setTextColor(android.graphics.Color.parseColor("#059669"))
                                tvKbmWaktu?.text = data.tanggal_hari_ini ?: "-"
                                tvKbmMapel?.text = "Jadwal Pelajaran Kelas ${data.kelas ?: "-"}"
                                tvKbmGuruRuang?.text = "Silakan klik Buka Modul KBM untuk materi pelajaran."
                            }

                            // 5. Populate Tugas Mendatang
                            populateTugasMendatang(llTugasMendatang, tvEmptyTugas, tvCountTugasBadge, data.tugas_preview)

                            // 6. Populate Aktivitas & Materi Terkini
                            populateMateriTerkini(llMateriTerkini, tvEmptyMateri, data.materi_preview)

                        } else {
                            Toast.makeText(requireContext(), "Gagal memuat data akademik terbaru", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "Error koneksi akademik", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Click Actions
        view.findViewById<CardView>(R.id.cardRataRata)?.setOnClickListener { openNilaiRaport() }
        view.findViewById<View>(R.id.btnRincianLengkap)?.setOnClickListener { openNilaiRaport() }
        view.findViewById<CardView>(R.id.btnMenuRaport)?.setOnClickListener { openNilaiRaport() }

        view.findViewById<CardView>(R.id.cardTugasAktif)?.setOnClickListener { openDaftarTugas() }
        view.findViewById<CardView>(R.id.btnMenuTugas)?.setOnClickListener { openDaftarTugas() }
        view.findViewById<View>(R.id.btnSemuaTugas)?.setOnClickListener { openDaftarTugas() }

        view.findViewById<CardView>(R.id.cardTotalMateri)?.setOnClickListener { openMateriBelajar() }
        view.findViewById<CardView>(R.id.btnMenuMateri)?.setOnClickListener { openMateriBelajar() }
        view.findViewById<View>(R.id.btnLihatSemuaMateri)?.setOnClickListener { openMateriBelajar() }
        btnBukaModulKbm?.setOnClickListener { openMateriBelajar() }
    }

    private fun populateTugasMendatang(
        container: LinearLayout?,
        tvEmpty: TextView?,
        tvBadgeCount: TextView?,
        list: List<TugasPreviewItem>?
    ) {
        if (container == null) return
        container.removeAllViews()

        val activeList = list?.filter { it.is_selesai != true } ?: emptyList()
        tvBadgeCount?.text = "${activeList.size}"

        if (activeList.isEmpty()) {
            tvEmpty?.visibility = View.VISIBLE
            container.addView(tvEmpty)
            return
        }

        tvEmpty?.visibility = View.GONE

        for (item in activeList.take(3)) {
            val itemView = layoutInflater.inflate(R.layout.item_tugas_mendatang, container, false)

            val tvBadgeJurusan = itemView.findViewById<TextView>(R.id.tvBadgeJurusan)
            val tvBadgeDeadline = itemView.findViewById<TextView>(R.id.tvBadgeDeadline)
            val tvJudulTugas = itemView.findViewById<TextView>(R.id.tvJudulTugas)
            val tvGuruTugas = itemView.findViewById<TextView>(R.id.tvGuruTugas)
            val btnKumpulkan = itemView.findViewById<TextView>(R.id.btnKumpulkanTugas)

            tvBadgeJurusan.text = item.jurusan_badge ?: item.mapel ?: "Kejuruan"
            tvBadgeDeadline.text = item.deadline_label ?: item.deadline ?: "-"

            if (item.is_urgent == true) {
                tvBadgeDeadline.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_red_soft)
                tvBadgeDeadline.setTextColor(android.graphics.Color.parseColor("#DC2626"))
            } else {
                tvBadgeDeadline.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_amber_soft)
                tvBadgeDeadline.setTextColor(android.graphics.Color.parseColor("#D97706"))
            }

            tvJudulTugas.text = item.judul ?: "Tugas"
            tvGuruTugas.text = item.guru ?: "Guru Mapel"

            btnKumpulkan.setOnClickListener {
                val intent = Intent(requireContext(), KumpulTugasActivity::class.java).apply {
                    putExtra("ID_TUGAS", item.id?.toString() ?: "")
                    putExtra("MAPEL", item.mapel ?: "")
                    putExtra("JUDUL", item.judul ?: "")
                    putExtra("DEADLINE", item.deadline ?: "")
                    putExtra("DESKRIPSI", "")
                    putExtra("FILE_PENDUKUNG", "")
                }
                startActivity(intent)
            }

            container.addView(itemView)
        }
    }

    private fun populateMateriTerkini(
        container: LinearLayout?,
        tvEmpty: TextView?,
        list: List<MateriPreviewItem>?
    ) {
        if (container == null) return
        container.removeAllViews()

        if (list.isNullOrEmpty()) {
            tvEmpty?.visibility = View.VISIBLE
            container.addView(tvEmpty)
            return
        }

        tvEmpty?.visibility = View.GONE

        for (item in list.take(3)) {
            val itemView = layoutInflater.inflate(R.layout.item_materi_terkini, container, false)

            val flBadgeFileType = itemView.findViewById<FrameLayout>(R.id.flBadgeFileType)
            val tvBadgeFileType = itemView.findViewById<TextView>(R.id.tvBadgeFileType)
            val tvJudulMateri = itemView.findViewById<TextView>(R.id.tvJudulMateri)
            val tvInfoMateri = itemView.findViewById<TextView>(R.id.tvInfoMateri)
            val btnDownload = itemView.findViewById<FrameLayout>(R.id.btnDownloadMateri)

            val fileType = item.file_type ?: "PDF"
            tvBadgeFileType.text = fileType
            if (fileType.equals("PDF", ignoreCase = true)) {
                flBadgeFileType.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_file_pdf)
                tvBadgeFileType.setTextColor(android.graphics.Color.parseColor("#DC2626"))
            } else {
                flBadgeFileType.background = ContextCompat.getDrawable(requireContext(), R.drawable.bg_badge_file_doc)
                tvBadgeFileType.setTextColor(android.graphics.Color.parseColor("#2563EB"))
            }

            tvJudulMateri.text = item.judul ?: "Materi Pembelajaran"
            tvInfoMateri.text = item.info_sub ?: "${item.file_size ?: ""} • Diunggah ${item.guru_singkat ?: "Guru"} • ${item.waktu ?: ""}"

            val downloadUrl = item.download_url
            btnDownload.setOnClickListener {
                if (!downloadUrl.isNullOrEmpty()) {
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(downloadUrl))
                        startActivity(browserIntent)
                    } catch (e: Exception) {
                        openMateriBelajar()
                    }
                } else {
                    openMateriBelajar()
                }
            }

            itemView.setOnClickListener {
                openMateriBelajar()
            }

            container.addView(itemView)
        }
    }

    private fun openNilaiRaport() {
        val intent = Intent(requireContext(), NilaiRaportActivity::class.java)
        startActivity(intent)
    }

    private fun openDaftarTugas() {
        val intent = Intent(requireContext(), DaftarTugasActivity::class.java)
        startActivity(intent)
    }

    private fun openMateriBelajar() {
        val intent = Intent(requireContext(), MateriBelajarActivity::class.java)
        startActivity(intent)
    }
}
