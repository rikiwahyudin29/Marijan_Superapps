package com.rtekmidev.smkrjsuperapps

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.JadwalUjian
import com.rtekmidev.smkrjsuperapps.api.RiwayatUjianItem
import com.rtekmidev.smkrjsuperapps.api.StudentCbtInfo
import com.rtekmidev.smkrjsuperapps.util.RefreshableFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CbtFragment : Fragment(), RefreshableFragment {

    private var nisnSiswa: String = ""
    private var fullJadwalList: List<JadwalUjian> = emptyList()
    private var riwayatList: List<RiwayatUjianItem> = emptyList()
    private var currentFilterStatus: String = "SEMUA" // "SEMUA", "BISA_UJIAN", "BELUM_MULAI", "SUDAH_UJIAN"

    // Views - Hero
    private var tvHeroStudentName: TextView? = null
    private var tvHeroStudentClassNis: TextView? = null
    private var tvHeroStudentSemester: TextView? = null
    private var tvStatTotalUjian: TextView? = null
    private var tvStatAktifUjian: TextView? = null
    private var tvStatSelesaiUjian: TextView? = null

    // Views - Filter Status Pelaksanaan
    private var btnFilterSemua: View? = null
    private var btnFilterBisaUjian: View? = null
    private var btnFilterBelumMulai: View? = null
    private var btnFilterSudahUjian: View? = null

    private var tvFilterSemuaLabel: TextView? = null
    private var tvFilterBisaUjianLabel: TextView? = null
    private var tvFilterBelumMulaiLabel: TextView? = null
    private var tvFilterSudahUjianLabel: TextView? = null

    private var tvFilterSemuaBadge: TextView? = null
    private var tvFilterBisaUjianBadge: TextView? = null
    private var tvFilterBelumMulaiBadge: TextView? = null
    private var tvFilterSudahUjianBadge: TextView? = null

    // Views - Status & Content
    private var tvInfoJadwal: TextView? = null
    private var ivRefreshJadwal: ImageView? = null
    private var pbLoading: View? = null
    private var layoutEmptyState: View? = null
    private var containerJadwal: LinearLayout? = null
    private var containerRiwayat: LinearLayout? = null
    private var btnLihatTranskrip: CardView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_cbt, container, false)

        val sharedPref = requireContext().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisnSiswa = sharedPref.getString("nisn", "") ?: ""

        // Bind Hero views
        tvHeroStudentName = root.findViewById(R.id.tvHeroStudentName)
        tvHeroStudentClassNis = root.findViewById(R.id.tvHeroStudentClassNis)
        tvHeroStudentSemester = root.findViewById(R.id.tvHeroStudentSemester)
        tvStatTotalUjian = root.findViewById(R.id.tvStatTotalUjian)
        tvStatAktifUjian = root.findViewById(R.id.tvStatAktifUjian)
        tvStatSelesaiUjian = root.findViewById(R.id.tvStatSelesaiUjian)

        // Seed initial student info from local cache
        val localNama = sharedPref.getString("nama_siswa", null) ?: sharedPref.getString("nama", "Siswa")
        val localKelas = sharedPref.getString("kelas", "12 TKJT 1")
        val localNis = sharedPref.getString("nis", null) ?: nisnSiswa
        val localSem = sharedPref.getString("semester", "Semester Ganjil")

        tvHeroStudentName?.text = localNama
        tvHeroStudentClassNis?.text = "$localKelas • NIS: $localNis"
        tvHeroStudentSemester?.text = if (localSem?.startsWith("Semester") == true) localSem else "Semester $localSem"

        // Bind Filter Status Buttons & Badges
        btnFilterSemua = root.findViewById(R.id.btnFilterSemua)
        btnFilterBisaUjian = root.findViewById(R.id.btnFilterBisaUjian)
        btnFilterBelumMulai = root.findViewById(R.id.btnFilterBelumMulai)
        btnFilterSudahUjian = root.findViewById(R.id.btnFilterSudahUjian)

        tvFilterSemuaLabel = root.findViewById(R.id.tvFilterSemuaLabel)
        tvFilterBisaUjianLabel = root.findViewById(R.id.tvFilterBisaUjianLabel)
        tvFilterBelumMulaiLabel = root.findViewById(R.id.tvFilterBelumMulaiLabel)
        tvFilterSudahUjianLabel = root.findViewById(R.id.tvFilterSudahUjianLabel)

        tvFilterSemuaBadge = root.findViewById(R.id.tvFilterSemuaBadge)
        tvFilterBisaUjianBadge = root.findViewById(R.id.tvFilterBisaUjianBadge)
        tvFilterBelumMulaiBadge = root.findViewById(R.id.tvFilterBelumMulaiBadge)
        tvFilterSudahUjianBadge = root.findViewById(R.id.tvFilterSudahUjianBadge)

        btnFilterSemua?.setOnClickListener { setFilterStatus("SEMUA") }
        btnFilterBisaUjian?.setOnClickListener { setFilterStatus("BISA_UJIAN") }
        btnFilterBelumMulai?.setOnClickListener { setFilterStatus("BELUM_MULAI") }
        btnFilterSudahUjian?.setOnClickListener { setFilterStatus("SUDAH_UJIAN") }

        // Bind Status & Content
        tvInfoJadwal = root.findViewById(R.id.tvInfoJadwal)
        ivRefreshJadwal = root.findViewById(R.id.ivRefreshJadwal)
        pbLoading = root.findViewById(R.id.pbLoading)
        layoutEmptyState = root.findViewById(R.id.layoutEmptyState)
        containerJadwal = root.findViewById(R.id.containerJadwal)
        containerRiwayat = root.findViewById(R.id.containerRiwayat)
        btnLihatTranskrip = root.findViewById(R.id.btnLihatTranskrip)

        root.findViewById<View>(R.id.btnRefreshJadwal)?.setOnClickListener {
            ivRefreshJadwal?.animate()?.rotationBy(360f)?.setDuration(500)?.start()
            fetchCbtDashboard(isSilent = false)
        }

        btnLihatTranskrip?.setOnClickListener {
            val intent = Intent(requireContext(), TranskripCbtActivity::class.java).apply {
                putExtra("NISN", nisnSiswa)
            }
            startActivity(intent)
        }

        fetchCbtDashboard(isSilent = false)

        return root
    }

    override fun onResume() {
        super.onResume()
        if (nisnSiswa.isNotEmpty()) {
            fetchCbtDashboard(isSilent = true)
        }
    }

    override fun refreshData() {
        fetchCbtDashboard(isSilent = false)
    }

    private fun fetchCbtDashboard(isSilent: Boolean = false) {
        if (!isAdded) return

        if (!isSilent) {
            pbLoading?.visibility = View.VISIBLE
            layoutEmptyState?.visibility = View.GONE
        }

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = ApiClient.instance.getJadwal(nisnSiswa)

                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    pbLoading?.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.status == true) {
                        val body = response.body()!!
                        fullJadwalList = body.data

                        // 1. Update Student Profile Strip
                        body.student_info?.let { info ->
                            updateStudentInfo(info)
                        }

                        // 2. Update Metrics & Badges
                        if (body.summary != null) {
                            tvStatTotalUjian?.text = body.summary.total_jadwal.toString()
                            tvStatAktifUjian?.text = body.summary.bisa_ujian.toString()
                            tvStatSelesaiUjian?.text = body.summary.sudah_ujian.toString()
                        } else {
                            calculateLocalStats()
                        }

                        // 3. Update Status Filter Badges & Render List Jadwal
                        updateFilterBadgesAndCounters()
                        updateFilterUI()
                        applyFilter()

                        // 4. Render Riwayat & Nilai Ujian CBT
                        riwayatList = body.riwayat_ujian ?: emptyList()
                        renderRiwayat(riwayatList)

                    } else {
                        if (!isSilent) {
                            Toast.makeText(
                                requireContext(),
                                response.body()?.message ?: "Gagal memuat jadwal CBT.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        if (fullJadwalList.isEmpty()) {
                            layoutEmptyState?.visibility = View.VISIBLE
                            tvInfoJadwal?.text = "Gagal memuat jadwal"
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (!isAdded) return@withContext
                    pbLoading?.visibility = View.GONE
                    if (!isSilent) {
                        Toast.makeText(
                            requireContext(),
                            "Koneksi CBT terganggu: ${e.localizedMessage ?: "Cek jaringan internet"}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    if (fullJadwalList.isEmpty()) {
                        layoutEmptyState?.visibility = View.VISIBLE
                        tvInfoJadwal?.text = "Koneksi terputus"
                    }
                }
            }
        }
    }

    private fun updateStudentInfo(info: StudentCbtInfo) {
        if (!info.nama.isNullOrBlank()) {
            tvHeroStudentName?.text = info.nama
        }
        val kelas = info.kelas ?: "12 TKJT 1"
        val nis = info.nis ?: "202412089"
        tvHeroStudentClassNis?.text = "$kelas • NIS: $nis"

        if (!info.semester.isNullOrBlank()) {
            tvHeroStudentSemester?.text = info.semester
        }
    }

    private fun calculateLocalStats() {
        val sekarang = Calendar.getInstance().time
        val total = fullJadwalList.size
        var bisaUjian = 0
        var selesai = 0

        for (item in fullJadwalList) {
            if (item.status_pengerjaan == "2") {
                selesai++
            } else {
                val waktuSelesai = parseDate(item.waktu_selesai ?: item.waktu_berakhir)
                val isExpired = (waktuSelesai != null && sekarang.after(waktuSelesai))
                if (!isExpired) {
                    bisaUjian++
                }
            }
        }

        tvStatTotalUjian?.text = "$total"
        tvStatAktifUjian?.text = "$bisaUjian"
        tvStatSelesaiUjian?.text = "$selesai"
    }

    private fun setFilterStatus(status: String) {
        currentFilterStatus = status
        updateFilterUI()
        applyFilter()
    }

    private fun updateFilterUI() {
        val buttons = listOf(btnFilterSemua, btnFilterBisaUjian, btnFilterBelumMulai, btnFilterSudahUjian)
        val labels = listOf(tvFilterSemuaLabel, tvFilterBisaUjianLabel, tvFilterBelumMulaiLabel, tvFilterSudahUjianLabel)
        val badges = listOf(tvFilterSemuaBadge, tvFilterBisaUjianBadge, tvFilterBelumMulaiBadge, tvFilterSudahUjianBadge)
        val statuses = listOf("SEMUA", "BISA_UJIAN", "BELUM_MULAI", "SUDAH_UJIAN")

        for (i in statuses.indices) {
            val isSelected = (statuses[i] == currentFilterStatus)
            if (isSelected) {
                // Aktif: Latar Navy Deep, Teks Putih, Badge Biru Pekat
                buttons[i]?.setBackgroundResource(R.drawable.bg_chip_filter_active)
                labels[i]?.setTextColor(Color.WHITE)
                badges[i]?.setBackgroundResource(R.drawable.bg_pill_filter_badge)
                badges[i]?.setTextColor(Color.WHITE)
            } else {
                // Inaktif: Latar Putih border abu-abu, Teks Slate gelap, Badge abu-abu muda
                buttons[i]?.setBackgroundResource(R.drawable.bg_chip_filter_inactive)
                labels[i]?.setTextColor(Color.parseColor("#334155"))
                badges[i]?.setBackgroundResource(R.drawable.bg_pill_filter_badge_inactive)
                badges[i]?.setTextColor(Color.parseColor("#64748B"))
            }
        }
    }

    private fun updateFilterBadgesAndCounters() {
        val sekarang = Calendar.getInstance().time
        val total = fullJadwalList.size
        var bisaUjian = 0
        var belumMulai = 0
        var sudahUjian = 0

        for (item in fullJadwalList) {
            if (item.status_pengerjaan == "2") {
                sudahUjian++
            } else {
                val waktuMulaiDb = parseDate(item.waktu_mulai)
                val waktuSelesaiDb = parseDate(item.waktu_selesai ?: item.waktu_berakhir)
                val isExpired = (waktuSelesaiDb != null && sekarang.after(waktuSelesaiDb))
                val isBelum = (waktuMulaiDb != null && sekarang.before(waktuMulaiDb))

                if (isBelum) {
                    belumMulai++
                } else if (!isExpired) {
                    bisaUjian++
                }
            }
        }

        tvFilterSemuaBadge?.text = "$total"
        tvFilterBisaUjianBadge?.text = "$bisaUjian"
        tvFilterBelumMulaiBadge?.text = "$belumMulai"
        tvFilterSudahUjianBadge?.text = "$sudahUjian"

        // Sinkronkan juga kotak metrik hero
        tvStatTotalUjian?.text = "$total"
        tvStatAktifUjian?.text = "$bisaUjian"
        tvStatSelesaiUjian?.text = "$sudahUjian"
    }

    private fun applyFilter() {
        val sekarang = Calendar.getInstance().time

        val filteredList = when (currentFilterStatus) {
            "BISA_UJIAN" -> fullJadwalList.filter {
                val waktuMulaiDb = parseDate(it.waktu_mulai)
                val waktuSelesaiDb = parseDate(it.waktu_selesai ?: it.waktu_berakhir)
                val isExpired = (waktuSelesaiDb != null && sekarang.after(waktuSelesaiDb))
                val isBelum = (waktuMulaiDb != null && sekarang.before(waktuMulaiDb))
                it.status_pengerjaan != "2" && !isExpired && !isBelum
            }
            "BELUM_MULAI" -> fullJadwalList.filter {
                val waktuMulaiDb = parseDate(it.waktu_mulai)
                val isBelum = (waktuMulaiDb != null && sekarang.before(waktuMulaiDb))
                it.status_pengerjaan != "2" && isBelum
            }
            "SUDAH_UJIAN" -> fullJadwalList.filter {
                it.status_pengerjaan == "2"
            }
            else -> fullJadwalList
        }

        renderJadwal(filteredList)

        val statusLabel = when (currentFilterStatus) {
            "BISA_UJIAN" -> "bisa ujian"
            "BELUM_MULAI" -> "belum mulai"
            "SUDAH_UJIAN" -> "sudah ujian"
            else -> "semua status"
        }
        tvInfoJadwal?.text = "Menampilkan ${filteredList.size} jadwal ($statusLabel)"
    }

    private fun renderJadwal(jadwalList: List<JadwalUjian>) {
        val container = containerJadwal ?: return
        container.removeAllViews()

        if (jadwalList.isEmpty()) {
            layoutEmptyState?.visibility = View.VISIBLE
            return
        }

        layoutEmptyState?.visibility = View.GONE
        val inflater = LayoutInflater.from(requireContext())
        val sekarang = Calendar.getInstance().time

        for (jadwal in jadwalList) {
            val itemView = inflater.inflate(R.layout.item_cbt_jadwal, container, false)

            val tvKategoriUjian = itemView.findViewById<TextView>(R.id.tvKategoriUjian)
            val tvStatusBadge = itemView.findViewById<TextView>(R.id.tvStatusBadge)
            val tvMapelJudul = itemView.findViewById<TextView>(R.id.tvMapelJudul)
            val tvDeskripsiUjian = itemView.findViewById<TextView>(R.id.tvDeskripsiUjian)

            val tvRentangWaktu = itemView.findViewById<TextView>(R.id.tvRentangWaktu)
            val tvSubRentangWaktu = itemView.findViewById<TextView>(R.id.tvSubRentangWaktu)
            val tvDurasiSoal = itemView.findViewById<TextView>(R.id.tvDurasiSoal)
            val tvTipeSoal = itemView.findViewById<TextView>(R.id.tvTipeSoal)
            val tvServerNode = itemView.findViewById<TextView>(R.id.tvServerNode)

            val btnMulaiUjianCard = itemView.findViewById<CardView>(R.id.btnMulaiUjianCard)
            val tvBtnMulaiText = itemView.findViewById<TextView>(R.id.tvBtnMulaiText)
            val ivBtnIconStart = itemView.findViewById<ImageView>(R.id.ivBtnIconStart)
            val ivBtnIconEnd = itemView.findViewById<ImageView>(R.id.ivBtnIconEnd)

            // Ekstraksi nama jenis ujian asli yang diselenggarakan untuk jadwal ini
            var jenisUjian = (jadwal.jenis_ujian ?: jadwal.kategori_ujian ?: "").trim()
            if (jenisUjian.isBlank() || jenisUjian.equals("UJIAN SEKOLAH", ignoreCase = true) || jenisUjian.equals("ASESMEN CBT", ignoreCase = true)) {
                val rawSource = jadwal.deskripsi ?: jadwal.judul_ujian ?: ""
                if (rawSource.contains("_")) {
                    val firstToken = rawSource.split("_").firstOrNull()?.trim()
                    if (!firstToken.isNullOrBlank()) {
                        jenisUjian = firstToken
                    }
                } else if (rawSource.isNotBlank()) {
                    jenisUjian = rawSource.trim()
                }
            }
            if (jenisUjian.isBlank()) {
                jenisUjian = "PENILAIAN SUMATIF"
            }
            tvKategoriUjian.text = jenisUjian.uppercase(Locale.getDefault())

            tvMapelJudul.text = jadwal.nama_mapel ?: jadwal.judul_ujian

            val deskripsiStr = jadwal.deskripsi ?: "Penilaian Sumatif Akhir Semester • ${jadwal.nama_mapel ?: "Mata Pelajaran"}"
            tvDeskripsiUjian.text = deskripsiStr

            // Timing
            val waktuMulaiStr = jadwal.waktu_mulai ?: "-"
            val waktuSelesaiStr = jadwal.waktu_selesai ?: jadwal.waktu_berakhir ?: "-"
            val mulaiFmt = if (waktuMulaiStr != "-") waktuMulaiStr.take(16) else "-"
            val selesaiFmt = if (waktuSelesaiStr != "-") {
                if (waktuSelesaiStr.length >= 16) waktuSelesaiStr.substring(11, 16) else waktuSelesaiStr
            } else "-"
            tvRentangWaktu.text = "$mulaiFmt s/d $selesaiFmt WIB"

            // Durasi & Soal
            val durasiStr = jadwal.durasi.ifEmpty { "90" }
            val totalSoalStr = (jadwal.total_soal ?: 40).toString()
            tvDurasiSoal.text = "Durasi: $durasiStr Menit • $totalSoalStr Butir Soal"
            tvTipeSoal.text = jadwal.tipe_soal ?: "Pilihan Ganda, PG Kompleks & Uraian"

            // Node Server
            tvServerNode.text = jadwal.server_node ?: "CBT Node 01 • RTEKMI High-Speed Dedicated Server"

            // Status Logic
            var isKedaluwarsa = false
            var isBelumMulai = false

            val waktuMulaiDb = parseDate(waktuMulaiStr)
            val waktuSelesaiDb = parseDate(waktuSelesaiStr)

            if (waktuSelesaiDb != null && sekarang.after(waktuSelesaiDb)) {
                isKedaluwarsa = true
            }
            if (waktuMulaiDb != null && sekarang.before(waktuMulaiDb)) {
                isBelumMulai = true
            }

            val status = jadwal.status_pengerjaan

            if (status == "2") {
                // Selesai
                tvStatusBadge.text = "● Selesai"
                tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_green_badge)
                tvStatusBadge.setTextColor(Color.parseColor("#15803D"))
                tvSubRentangWaktu.text = "Ujian telah selesai dikerjakan"

                val nilaiPg = jadwal.nilai_pg?.toDoubleOrNull() ?: 0.0
                val nilaiEsai = jadwal.nilai_esai?.toDoubleOrNull() ?: 0.0
                val totalNilai = nilaiPg + nilaiEsai
                val formattedNilai = if (totalNilai % 1.0 == 0.0) totalNilai.toInt().toString() else totalNilai.toString()

                tvBtnMulaiText.text = "NILAI ANDA: $formattedNilai"
                btnMulaiUjianCard.setCardBackgroundColor(Color.parseColor("#E2E8F0"))
                tvBtnMulaiText.setTextColor(Color.parseColor("#64748B"))
                ivBtnIconStart.visibility = View.GONE
                ivBtnIconEnd.visibility = View.GONE
                btnMulaiUjianCard.isEnabled = false

            } else if (isKedaluwarsa) {
                // Terlewat / Waktu Habis
                tvStatusBadge.text = "● Terlewat"
                tvStatusBadge.setBackgroundResource(R.drawable.bg_tag_status_red)
                tvStatusBadge.setTextColor(Color.parseColor("#DC2626"))
                tvSubRentangWaktu.text = "Waktu pengerjaan telah berakhir"

                tvBtnMulaiText.text = "WAKTU HABIS"
                btnMulaiUjianCard.setCardBackgroundColor(Color.parseColor("#FEE2E2"))
                tvBtnMulaiText.setTextColor(Color.parseColor("#DC2626"))
                ivBtnIconStart.visibility = View.GONE
                ivBtnIconEnd.visibility = View.GONE
                btnMulaiUjianCard.isEnabled = false

            } else if (isBelumMulai) {
                // Belum Mulai
                tvStatusBadge.text = "● Belum Mulai"
                tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_cyan_badge)
                tvStatusBadge.setTextColor(Color.parseColor("#0284C7"))
                tvSubRentangWaktu.text = "Menunggu jadwal ujian dimulai"

                tvBtnMulaiText.text = "BELUM WAKTUNYA"
                btnMulaiUjianCard.setCardBackgroundColor(Color.parseColor("#DBEAFE"))
                tvBtnMulaiText.setTextColor(Color.parseColor("#1D4ED8"))
                ivBtnIconStart.visibility = View.GONE
                ivBtnIconEnd.visibility = View.GONE
                btnMulaiUjianCard.isEnabled = false

            } else if (status == "1") {
                // Sedang Dikerjakan
                tvStatusBadge.text = "● Sedang Dikerjakan"
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_amber_soft)
                tvStatusBadge.setTextColor(Color.parseColor("#D97706"))
                tvSubRentangWaktu.text = "Sesi ujian Anda masih aktif"

                tvBtnMulaiText.text = "LANJUTKAN UJIAN SEKARANG"
                btnMulaiUjianCard.setCardBackgroundColor(Color.parseColor("#D97706"))
                tvBtnMulaiText.setTextColor(Color.WHITE)
                ivBtnIconStart.visibility = View.VISIBLE
                ivBtnIconEnd.visibility = View.VISIBLE
                btnMulaiUjianCard.isEnabled = true
                btnMulaiUjianCard.setOnClickListener { bukaKonfirmasi(jadwal) }

            } else {
                // Tersedia
                tvStatusBadge.text = "● Tersedia"
                tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_green_badge)
                tvStatusBadge.setTextColor(Color.parseColor("#15803D"))
                tvSubRentangWaktu.text = "Sisa waktu pengerjaan hari ini"

                tvBtnMulaiText.text = "MULAI UJIAN SEKARANG"
                btnMulaiUjianCard.setCardBackgroundColor(Color.parseColor("#1E1B4B"))
                tvBtnMulaiText.setTextColor(Color.WHITE)
                ivBtnIconStart.visibility = View.VISIBLE
                ivBtnIconEnd.visibility = View.VISIBLE
                btnMulaiUjianCard.isEnabled = true
                btnMulaiUjianCard.setOnClickListener { bukaKonfirmasi(jadwal) }
            }

            container.addView(itemView)
        }
    }

    private fun renderRiwayat(items: List<RiwayatUjianItem>) {
        val container = containerRiwayat ?: return
        container.removeAllViews()

        if (items.isEmpty()) {
            val emptyView = LayoutInflater.from(requireContext()).inflate(R.layout.item_cbt_riwayat_empty, container, false)
            container.addView(emptyView)
            return
        }

        val inflater = LayoutInflater.from(requireContext())
        for (item in items) {
            val itemView = inflater.inflate(R.layout.item_cbt_riwayat, container, false)

            val tvKategoriRiwayat = itemView.findViewById<TextView>(R.id.tvKategoriRiwayat)
            val tvMapelRiwayat = itemView.findViewById<TextView>(R.id.tvMapelRiwayat)
            val tvKkmTanggal = itemView.findViewById<TextView>(R.id.tvKkmTanggal)
            val tvNilaiRiwayat = itemView.findViewById<TextView>(R.id.tvNilaiRiwayat)
            val tvPredikatBadge = itemView.findViewById<TextView>(R.id.tvPredikatBadge)

            tvKategoriRiwayat.text = item.kategori ?: "SIMULASI CBT"
            tvMapelRiwayat.text = item.nama_mapel ?: "Mata Pelajaran"
            tvKkmTanggal.text = "KKM: ${item.kkm}  •  ${item.tanggal ?: "-"}"

            val nilaiVal = item.nilai ?: 0.0
            val formattedNilai = if (nilaiVal % 1.0 == 0.0) nilaiVal.toInt().toString() else String.format(Locale.US, "%.1f", nilaiVal)
            tvNilaiRiwayat.text = formattedNilai

            val pred = item.predikat ?: if (nilaiVal >= 75.0) "LULUS" else "REMEDIAL"
            tvPredikatBadge.text = pred

            if (nilaiVal < 75.0) {
                tvNilaiRiwayat.setTextColor(Color.parseColor("#E11D48"))
                tvPredikatBadge.setBackgroundResource(R.drawable.bg_badge_red_soft)
                tvPredikatBadge.setTextColor(Color.parseColor("#DC2626"))
            } else {
                tvNilaiRiwayat.setTextColor(Color.parseColor("#0F766E"))
                tvPredikatBadge.setBackgroundResource(R.drawable.bg_pill_green_badge)
                tvPredikatBadge.setTextColor(Color.parseColor("#16A34A"))
            }

            container.addView(itemView)
        }
    }

    private fun parseDate(dateStr: String?): Date? {
        if (dateStr.isNullOrBlank() || dateStr == "-") return null
        val formats = arrayOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd'T'HH:mm:ss"
        )
        for (fmt in formats) {
            try {
                val sdf = SimpleDateFormat(fmt, Locale.getDefault())
                return sdf.parse(dateStr)
            } catch (_: Exception) {}
        }
        return null
    }

    private fun bukaKonfirmasi(jadwal: JadwalUjian) {
        // Ambil nama jenis ujian yang diselenggarakan
        var jenisUjian = (jadwal.jenis_ujian ?: jadwal.kategori_ujian ?: "").trim()
        if (jenisUjian.isBlank() || jenisUjian.equals("UJIAN SEKOLAH", ignoreCase = true) || jenisUjian.equals("ASESMEN CBT", ignoreCase = true)) {
            val rawSource = jadwal.deskripsi ?: jadwal.judul_ujian ?: ""
            if (rawSource.contains("_")) {
                val firstToken = rawSource.split("_").firstOrNull()?.trim()
                if (!firstToken.isNullOrBlank()) {
                    jenisUjian = firstToken
                }
            } else if (rawSource.isNotBlank()) {
                jenisUjian = rawSource.trim()
            }
        }
        if (jenisUjian.isBlank()) {
            jenisUjian = "PENILAIAN SUMATIF"
        }

        val intent = Intent(requireContext(), KonfirmasiActivity::class.java).apply {
            putExtra("ID_JADWAL", jadwal.id_jadwal)
            putExtra("MAPEL", jadwal.nama_mapel ?: jadwal.judul_ujian)
            putExtra("DURASI", jadwal.durasi)
            putExtra("TOKEN_SERVER", jadwal.token)
            putExtra("SETTING_TOKEN", jadwal.setting_token)
            putExtra("JENIS_UJIAN", jenisUjian)
            putExtra("WAKTU_MULAI", jadwal.waktu_mulai)
            putExtra("WAKTU_SELESAI", jadwal.waktu_selesai ?: jadwal.waktu_berakhir)
            putExtra("TOTAL_SOAL", (jadwal.total_soal ?: 40).toString())
            putExtra("TIPE_SOAL", jadwal.tipe_soal ?: "PG, PGK, Isian & Menjodohkan")
            putExtra("SERVER_NODE", jadwal.server_node ?: "RTEKMI-Node01 • High-Speed Online")
            putExtra("DESKRIPSI", jadwal.deskripsi)
            putExtra("ID_UJIAN_SISWA", jadwal.id_ujian_siswa)
        }
        startActivity(intent)
    }
}
