package com.rtekmidev.smkrjsuperapps

import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.JadwalUjian
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
    private var currentFilter: String = "SEMUA"

    // Views
    private var pbLoading: View? = null
    private var layoutEmptyState: View? = null
    private var containerJadwal: LinearLayout? = null
    private var tvInfoJadwal: TextView? = null
    private var ivRefreshJadwal: ImageView? = null

    // Stats Views
    private var tvStatTotalUjian: TextView? = null
    private var tvStatAktifUjian: TextView? = null
    private var tvStatSelesaiUjian: TextView? = null

    // Filter Views
    private var cvFilterSemua: CardView? = null
    private var tvFilterSemua: TextView? = null
    private var cvFilterAktif: CardView? = null
    private var tvFilterAktif: TextView? = null
    private var cvFilterMendatang: CardView? = null
    private var tvFilterMendatang: TextView? = null
    private var cvFilterSelesai: CardView? = null
    private var tvFilterSelesai: TextView? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_cbt, container, false)

        val sharedPref = requireContext().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisnSiswa = sharedPref.getString("nisn", "") ?: ""

        // Bind Views
        pbLoading = root.findViewById(R.id.pbLoading)
        layoutEmptyState = root.findViewById(R.id.layoutEmptyState)
        containerJadwal = root.findViewById(R.id.containerJadwal)
        tvInfoJadwal = root.findViewById(R.id.tvInfoJadwal)
        ivRefreshJadwal = root.findViewById(R.id.ivRefreshJadwal)

        tvStatTotalUjian = root.findViewById(R.id.tvStatTotalUjian)
        tvStatAktifUjian = root.findViewById(R.id.tvStatAktifUjian)
        tvStatSelesaiUjian = root.findViewById(R.id.tvStatSelesaiUjian)

        cvFilterSemua = root.findViewById(R.id.cvFilterSemua)
        tvFilterSemua = root.findViewById(R.id.tvFilterSemua)
        cvFilterAktif = root.findViewById(R.id.cvFilterAktif)
        tvFilterAktif = root.findViewById(R.id.tvFilterAktif)
        cvFilterMendatang = root.findViewById(R.id.cvFilterMendatang)
        tvFilterMendatang = root.findViewById(R.id.tvFilterMendatang)
        cvFilterSelesai = root.findViewById(R.id.cvFilterSelesai)
        tvFilterSelesai = root.findViewById(R.id.tvFilterSelesai)

        // Setup Filter Click Listeners
        cvFilterSemua?.setOnClickListener { setFilter("SEMUA") }
        cvFilterAktif?.setOnClickListener { setFilter("AKTIF") }
        cvFilterMendatang?.setOnClickListener { setFilter("MENDATANG") }
        cvFilterSelesai?.setOnClickListener { setFilter("SELESAI") }

        // Setup Refresh Button
        root.findViewById<View>(R.id.btnRefreshJadwal)?.setOnClickListener {
            ivRefreshJadwal?.animate()?.rotationBy(360f)?.setDuration(500)?.start()
            fetchJadwal(isSilent = false)
        }

        fetchJadwal(isSilent = false)

        return root
    }

    override fun onResume() {
        super.onResume()
        if (nisnSiswa.isNotEmpty()) {
            fetchJadwal(isSilent = true)
        }
    }

    override fun refreshData() {
        fetchJadwal(isSilent = false)
    }

    private fun fetchJadwal(isSilent: Boolean = false) {
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
                        fullJadwalList = response.body()?.data ?: emptyList()
                        updateStats()
                        applyCurrentFilter()
                    } else {
                        if (!isSilent) {
                            Toast.makeText(
                                requireContext(),
                                response.body()?.message ?: "Gagal memuat jadwal ujian.",
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
                            "Koneksi bermasalah: ${e.localizedMessage ?: "Cek sinyal internet"}",
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

    private fun updateStats() {
        val sekarang = Calendar.getInstance().time

        val totalCount = fullJadwalList.size
        var aktifCount = 0
        var selesaiCount = 0

        for (item in fullJadwalList) {
            if (item.status_pengerjaan == "2") {
                selesaiCount++
            } else {
                val waktuSelesai = parseDate(item.waktu_selesai ?: item.waktu_berakhir)
                val isExpired = (waktuSelesai != null && sekarang.after(waktuSelesai))
                if (!isExpired) {
                    aktifCount++
                }
            }
        }

        tvStatTotalUjian?.text = "$totalCount"
        tvStatAktifUjian?.text = "$aktifCount"
        tvStatSelesaiUjian?.text = "$selesaiCount"
    }

    private fun setFilter(tipe: String) {
        currentFilter = tipe

        val filterCards = listOf(cvFilterSemua, cvFilterAktif, cvFilterMendatang, cvFilterSelesai)
        val filterTexts = listOf(tvFilterSemua, tvFilterAktif, tvFilterMendatang, tvFilterSelesai)
        val filterKeys = listOf("SEMUA", "AKTIF", "MENDATANG", "SELESAI")

        for (i in filterKeys.indices) {
            val isCurrent = (filterKeys[i] == tipe)
            if (isCurrent) {
                filterCards[i]?.setCardBackgroundColor(Color.parseColor("#1E1B4B"))
                filterTexts[i]?.setTextColor(Color.WHITE)
            } else {
                filterCards[i]?.setCardBackgroundColor(Color.WHITE)
                filterTexts[i]?.setTextColor(Color.parseColor("#64748B"))
            }
        }

        applyCurrentFilter()
    }

    private fun applyCurrentFilter() {
        val sekarang = Calendar.getInstance().time

        val filteredList = when (currentFilter) {
            "AKTIF" -> fullJadwalList.filter {
                val waktuMulai = parseDate(it.waktu_mulai)
                it.status_pengerjaan != "2" && waktuMulai != null && (waktuMulai.before(sekarang) || waktuMulai.time == sekarang.time)
            }
            "MENDATANG" -> fullJadwalList.filter {
                val waktuMulai = parseDate(it.waktu_mulai)
                it.status_pengerjaan != "2" && waktuMulai != null && waktuMulai.after(sekarang)
            }
            "SELESAI" -> fullJadwalList.filter {
                it.status_pengerjaan == "2"
            }
            else -> fullJadwalList // SEMUA
        }

        renderJadwal(filteredList)
        tvInfoJadwal?.text = "Menampilkan ${filteredList.size} jadwal ujian"
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
            val itemView = inflater.inflate(R.layout.item_jadwal, container, false)

            val viewAksen = itemView.findViewById<View>(R.id.viewAksen)
            val tvMapel = itemView.findViewById<TextView>(R.id.tvMapel)
            val tvJudulUjian = itemView.findViewById<TextView>(R.id.tvJudulUjian)
            val tvRentangWaktu = itemView.findViewById<TextView>(R.id.tvRentangWaktu)
            val tvDurasi = itemView.findViewById<TextView>(R.id.tvDurasi)
            val tvStatusBadge = itemView.findViewById<TextView>(R.id.tvStatusBadge)
            val btnKerjakan = itemView.findViewById<Button>(R.id.btnKerjakan)

            // Set Informasi
            tvMapel.text = jadwal.nama_mapel ?: "Mata Pelajaran"
            tvJudulUjian.text = jadwal.judul_ujian.ifEmpty { "Ujian Sekolah Digital" }

            val waktuMulaiStr = jadwal.waktu_mulai ?: "-"
            val waktuSelesaiStr = jadwal.waktu_selesai ?: jadwal.waktu_berakhir ?: "-"

            val mulaiFormat = if (waktuMulaiStr != "-") waktuMulaiStr.take(16) else "-"
            val akhirFormat = if (waktuSelesaiStr != "-") waktuSelesaiStr.takeLast(8).take(5) else "-"
            tvRentangWaktu.text = "$mulaiFormat s/d $akhirFormat WIB"

            tvDurasi.text = "Durasi: ${jadwal.durasi.ifEmpty { "90" }} Menit"

            // Status & Timing Check
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
                // SUDAH SELESAI
                tvStatusBadge.text = "Selesai"
                tvStatusBadge.setTextColor(Color.parseColor("#10B981"))
                viewAksen.setBackgroundColor(Color.parseColor("#94A3AF"))

                btnKerjakan.isEnabled = false
                btnKerjakan.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#E2E8F0"))
                btnKerjakan.setTextColor(Color.parseColor("#64748B"))

                val nilaiPg = jadwal.nilai_pg?.toDoubleOrNull() ?: 0.0
                val nilaiEsai = jadwal.nilai_esai?.toDoubleOrNull() ?: 0.0
                val totalNilai = nilaiPg + nilaiEsai
                val formattedNilai = if (totalNilai % 1.0 == 0.0) totalNilai.toInt().toString() else totalNilai.toString()
                btnKerjakan.text = "NILAI ANDA: $formattedNilai"

            } else if (isKedaluwarsa) {
                // JADWAL SUDAH KEDALUWARSA / LEWAT
                tvStatusBadge.text = "Terlewat"
                tvStatusBadge.setTextColor(Color.parseColor("#EF4444"))
                viewAksen.setBackgroundColor(Color.parseColor("#EF4444"))

                btnKerjakan.text = "WAKTU HABIS"
                btnKerjakan.isEnabled = false
                btnKerjakan.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                btnKerjakan.setTextColor(Color.parseColor("#DC2626"))

            } else if (isBelumMulai) {
                // JADWAL BELUM MULAI
                tvStatusBadge.text = "Belum Mulai"
                tvStatusBadge.setTextColor(Color.parseColor("#3B82F6"))
                viewAksen.setBackgroundColor(Color.parseColor("#3B82F6"))

                btnKerjakan.text = "BELUM WAKTUNYA"
                btnKerjakan.isEnabled = false
                btnKerjakan.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DBEAFE"))
                btnKerjakan.setTextColor(Color.parseColor("#1D4ED8"))

            } else if (status == "1") {
                // SEDANG DIKERJAKAN
                tvStatusBadge.text = "Sedang Dikerjakan"
                tvStatusBadge.setTextColor(Color.parseColor("#F59E0B"))
                viewAksen.setBackgroundColor(Color.parseColor("#F59E0B"))

                btnKerjakan.text = "LANJUTKAN UJIAN"
                btnKerjakan.isEnabled = true
                btnKerjakan.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F59E0B"))
                btnKerjakan.setTextColor(Color.WHITE)
                btnKerjakan.setOnClickListener { bukaKonfirmasi(jadwal) }

            } else {
                // TERSEDIA UNTUK DIKERJAKAN
                tvStatusBadge.text = "Tersedia"
                tvStatusBadge.setTextColor(Color.parseColor("#10B981"))
                viewAksen.setBackgroundColor(Color.parseColor("#4F46E5"))

                btnKerjakan.text = "MULAI UJIAN"
                btnKerjakan.isEnabled = true
                btnKerjakan.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#4F46E5"))
                btnKerjakan.setTextColor(Color.WHITE)
                btnKerjakan.setOnClickListener { bukaKonfirmasi(jadwal) }
            }

            container.addView(itemView)
        }
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
