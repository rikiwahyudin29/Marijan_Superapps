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
    private var currentFilterJenis: String = "SEMUA"

    // Views - Hero
    private var tvHeroStudentName: TextView? = null
    private var tvHeroStudentClassNis: TextView? = null
    private var tvHeroStudentSemester: TextView? = null
    private var tvStatTotalUjian: TextView? = null
    private var tvStatAktifUjian: TextView? = null
    private var tvStatSelesaiUjian: TextView? = null

    // Views - Filter Jenis Ujian Dinamis
    private var llFilterJenisUjianContainer: LinearLayout? = null

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

        // Bind Filter Jenis Ujian Container
        llFilterJenisUjianContainer = root.findViewById(R.id.llFilterJenisUjianContainer)

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

                        // 3. Render Filter Jenis Ujian Chips & List Jadwal
                        renderFilterChips()
                        applyCurrentFilter()

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

    private fun renderFilterChips() {
        val container = llFilterJenisUjianContainer ?: return
        container.removeAllViews()

        if (!isAdded) return

        val categories = mutableListOf("SEMUA")
        val uniqueTypes = fullJadwalList.mapNotNull { it.kategori_ujian }
            .filter { it.isNotBlank() }
            .distinct()
        categories.addAll(uniqueTypes)

        val context = requireContext()
        val density = resources.displayMetrics.density

        for (cat in categories) {
            val isSelected = cat.equals(currentFilterJenis, ignoreCase = true)
            val count = if (cat == "SEMUA") {
                fullJadwalList.size
            } else {
                fullJadwalList.count { it.kategori_ujian.equals(cat, ignoreCase = true) }
            }

            val card = CardView(context).apply {
                radius = 20 * density
                cardElevation = if (isSelected) 2 * density else 0f
                setCardBackgroundColor(if (isSelected) Color.parseColor("#1E1B4B") else Color.WHITE)
                isClickable = true
                isFocusable = true
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = (8 * density).toInt()
                }
            }

            val linear = LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                val padH = (14 * density).toInt()
                val padV = (8 * density).toInt()
                setPadding(padH, padV, padH, padV)
                if (!isSelected) {
                    setBackgroundResource(R.drawable.bg_chip_filter_inactive)
                }
            }

            val labelText = if (cat == "SEMUA") "Semua Ujian" else cat
            val tvLabel = TextView(context).apply {
                text = labelText
                setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#334155"))
                textSize = 13f
                setTypeface(null, Typeface.BOLD)
            }
            linear.addView(tvLabel)

            val tvBadge = TextView(context).apply {
                text = "$count"
                setTextColor(if (isSelected) Color.WHITE else Color.parseColor("#64748B"))
                textSize = 11f
                setTypeface(null, Typeface.BOLD)
                setBackgroundResource(if (isSelected) R.drawable.bg_pill_filter_badge else R.drawable.bg_pill_filter_badge_inactive)
                val padBH = (7 * density).toInt()
                val padBV = (2 * density).toInt()
                setPadding(padBH, padBV, padBH, padBV)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginStart = (6 * density).toInt()
                }
            }
            linear.addView(tvBadge)

            card.addView(linear)
            card.setOnClickListener {
                currentFilterJenis = cat
                renderFilterChips()
                applyCurrentFilter()
            }

            container.addView(card)
        }
    }

    private fun applyCurrentFilter() {
        val filteredList = if (currentFilterJenis == "SEMUA") {
            fullJadwalList
        } else {
            fullJadwalList.filter {
                it.kategori_ujian.equals(currentFilterJenis, ignoreCase = true)
            }
        }

        renderJadwal(filteredList)

        val filterLabel = if (currentFilterJenis == "SEMUA") "semua jenis ujian" else currentFilterJenis
        tvInfoJadwal?.text = "Menampilkan ${filteredList.size} jadwal ($filterLabel)"
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

            // Category & Titles
            val kategoriText = jadwal.kategori_ujian ?: "ASESMEN CBT"
            tvKategoriUjian.text = kategoriText.uppercase()
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
        val intent = Intent(requireContext(), KonfirmasiActivity::class.java).apply {
            putExtra("ID_JADWAL", jadwal.id_jadwal)
            putExtra("MAPEL", jadwal.nama_mapel ?: jadwal.judul_ujian)
            putExtra("DURASI", jadwal.durasi)
            putExtra("TOKEN_SERVER", jadwal.token)
            putExtra("SETTING_TOKEN", jadwal.setting_token)
        }
        startActivity(intent)
    }
}
