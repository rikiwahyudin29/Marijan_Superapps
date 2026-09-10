package com.rtekmidev.smkrjsuperapps

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.JenisUjianFilterItem
import com.rtekmidev.smkrjsuperapps.api.NilaiCbtItem
import com.rtekmidev.smkrjsuperapps.api.RingkasanCbt
import com.rtekmidev.smkrjsuperapps.api.TahunAjaranFilterItem
import com.rtekmidev.smkrjsuperapps.api.TranskripCbtResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class TranskripCbtActivity : AppCompatActivity() {

    private lateinit var ivStudentAvatar: ImageView
    private lateinit var tvStudentName: TextView
    private lateinit var tvStudentClassNis: TextView
    private lateinit var tvStudentJurusan: TextView
    private lateinit var spinnerTahunAjaran: Spinner
    private lateinit var spinnerJenisUjian: Spinner
    private lateinit var tvStatRataRata: TextView
    private lateinit var tvStatTotalUjian: TextView
    private lateinit var tvStatTertinggi: TextView
    private lateinit var tvStatLulusRemedial: TextView
    private lateinit var tvTotalItemBadge: TextView
    private lateinit var pbLoading: ProgressBar
    private lateinit var layoutEmptyState: LinearLayout
    private lateinit var rvTranskrip: RecyclerView
    private lateinit var btnRefresh: View
    private lateinit var ivRefreshIcon: ImageView

    private lateinit var adapter: TranskripCbtAdapter
    private var nisnSiswa: String = ""
    private var selectedTahunAjaranId: String = "semua"
    private var selectedJenisUjianId: String = "semua"
    private var isSpinnerInitialized = false

    private val listTahunAjaran = mutableListOf<TahunAjaranFilterItem>()
    private val listJenisUjian = mutableListOf<JenisUjianFilterItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_transkrip_cbt)

        val sharedPref = getSharedPreferences("user_pref", Context.MODE_PRIVATE)
        nisnSiswa = intent.getStringExtra("NISN")
            ?: sharedPref.getString("nisn", null)
            ?: sharedPref.getString("nis", null)
            ?: sharedPref.getString("username", "") ?: ""

        initViews()
        setupListeners()
        seedInitialProfile()

        fetchTranskrip(isInitial = true)
    }

    private fun initViews() {
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        btnRefresh = findViewById(R.id.btnRefresh)
        ivRefreshIcon = findViewById(R.id.ivRefreshIcon)

        ivStudentAvatar = findViewById(R.id.ivStudentAvatar)
        tvStudentName = findViewById(R.id.tvStudentName)
        tvStudentClassNis = findViewById(R.id.tvStudentClassNis)
        tvStudentJurusan = findViewById(R.id.tvStudentJurusan)
        spinnerTahunAjaran = findViewById(R.id.spinnerTahunAjaran)
        spinnerJenisUjian = findViewById(R.id.spinnerJenisUjian)
        tvStatRataRata = findViewById(R.id.tvStatRataRata)
        tvStatTotalUjian = findViewById(R.id.tvStatTotalUjian)
        tvStatTertinggi = findViewById(R.id.tvStatTertinggi)
        tvStatLulusRemedial = findViewById(R.id.tvStatLulusRemedial)
        tvTotalItemBadge = findViewById(R.id.tvTotalItemBadge)
        pbLoading = findViewById(R.id.pbLoading)
        layoutEmptyState = findViewById(R.id.layoutEmptyState)
        rvTranskrip = findViewById(R.id.rvTranskrip)

        rvTranskrip.layoutManager = LinearLayoutManager(this)
        adapter = TranskripCbtAdapter()
        rvTranskrip.adapter = adapter
    }

    private fun setupListeners() {
        btnRefresh.setOnClickListener {
            ivRefreshIcon.animate().rotationBy(360f).setDuration(500).start()
            fetchTranskrip(isInitial = false)
        }
    }

    private fun seedInitialProfile() {
        val sharedPref = getSharedPreferences("user_pref", Context.MODE_PRIVATE)
        val nama = sharedPref.getString("nama_siswa", null) ?: sharedPref.getString("nama", "Siswa")
        val kelas = sharedPref.getString("kelas", "12 TKJT 1")
        val nis = sharedPref.getString("nis", null) ?: nisnSiswa

        tvStudentName.text = nama
        tvStudentClassNis.text = "$kelas • NIS/NISN: $nis"
    }

    private fun fetchTranskrip(isInitial: Boolean) {
        pbLoading.visibility = View.VISIBLE
        layoutEmptyState.visibility = View.GONE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val taParam = if (selectedTahunAjaranId == "semua") null else selectedTahunAjaranId
                val juParam = if (selectedJenisUjianId == "semua") null else selectedJenisUjianId

                val response = ApiClient.instance.getTranskripCbt(
                    nisn = nisnSiswa,
                    idTahunAjaran = taParam,
                    idJenisUjian = juParam
                )

                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE

                    if (response.isSuccessful && response.body()?.status == true) {
                        val body = response.body()!!
                        renderStudentInfo(body)
                        renderSummary(body.ringkasan)

                        if (isInitial || !isSpinnerInitialized) {
                            setupSpinners(body)
                        }

                        val items = body.daftar_nilai
                        adapter.submitList(items)
                        tvTotalItemBadge.text = "${items.size} Ujian Selesai"

                        if (items.isEmpty()) {
                            layoutEmptyState.visibility = View.VISIBLE
                        } else {
                            layoutEmptyState.visibility = View.GONE
                        }
                    } else {
                        Toast.makeText(
                            this@TranskripCbtActivity,
                            response.body()?.message ?: "Gagal memuat transkrip CBT",
                            Toast.LENGTH_SHORT
                        ).show()
                        if (adapter.itemCount == 0) {
                            layoutEmptyState.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    pbLoading.visibility = View.GONE
                    Toast.makeText(
                        this@TranskripCbtActivity,
                        "Terjadi kesalahan: ${e.localizedMessage}",
                        Toast.LENGTH_SHORT
                    ).show()
                    if (adapter.itemCount == 0) {
                        layoutEmptyState.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun renderStudentInfo(body: TranskripCbtResponse) {
        body.student_info?.let { s ->
            if (!s.nama.isNullOrBlank()) tvStudentName.text = s.nama
            tvStudentClassNis.text = "${s.kelas ?: "-"} • NISN: ${s.nisn ?: s.nis ?: "-"}"
            tvStudentJurusan.text = s.jurusan ?: "Jurusan SMK Riyadhul Jannah"

            if (!s.foto.isNullOrBlank()) {
                Glide.with(this@TranskripCbtActivity)
                    .load(s.foto)
                    .circleCrop()
                    .placeholder(R.drawable.ic_modern_profile)
                    .error(R.drawable.ic_modern_profile)
                    .into(ivStudentAvatar)
            }
        }
    }

    private fun renderSummary(summary: RingkasanCbt?) {
        if (summary == null) return

        val rata = summary.rata_rata
        tvStatRataRata.text = if (rata % 1.0 == 0.0) rata.toInt().toString() else String.format(Locale.US, "%.1f", rata)
        tvStatTotalUjian.text = summary.total_ujian.toString()

        val high = summary.nilai_tertinggi
        tvStatTertinggi.text = if (high % 1.0 == 0.0) high.toInt().toString() else String.format(Locale.US, "%.1f", high)
        tvStatLulusRemedial.text = "${summary.lulus_count} / ${summary.remedial_count}"
    }

    private fun setupSpinners(body: TranskripCbtResponse) {
        val filterOptions = body.filter_options ?: return

        // 1. Setup Spinner Tahun Ajaran
        listTahunAjaran.clear()
        listTahunAjaran.add(
            TahunAjaranFilterItem(
                id = "semua",
                tahun_ajaran = "Semua",
                semester = "",
                label = "Semua Tahun Ajaran",
                is_aktif = false
            )
        )
        listTahunAjaran.addAll(filterOptions.tahun_ajaran)

        val taLabels = listTahunAjaran.map { it.label ?: it.tahun_ajaran ?: "Tahun Ajaran" }
        val taAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, taLabels)
        taAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTahunAjaran.adapter = taAdapter

        // 2. Setup Spinner Jenis Ujian
        listJenisUjian.clear()
        listJenisUjian.add(
            JenisUjianFilterItem(
                id = "semua",
                nama_jenis = "Semua",
                kode_jenis = "",
                label = "Semua Jenis Ujian"
            )
        )
        listJenisUjian.addAll(filterOptions.jenis_ujian)

        val juLabels = listJenisUjian.map { it.label ?: it.nama_jenis ?: "Jenis Ujian" }
        val juAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, juLabels)
        juAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerJenisUjian.adapter = juAdapter

        isSpinnerInitialized = true

        spinnerTahunAjaran.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in listTahunAjaran.indices) {
                    val newTaId = listTahunAjaran[position].id
                    if (newTaId != selectedTahunAjaranId) {
                        selectedTahunAjaranId = newTaId
                        fetchTranskrip(isInitial = false)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        spinnerJenisUjian.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position in listJenisUjian.indices) {
                    val newJuId = listJenisUjian[position].id
                    if (newJuId != selectedJenisUjianId) {
                        selectedJenisUjianId = newJuId
                        fetchTranskrip(isInitial = false)
                    }
                }
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
}

// ==========================================================
// RECYCLERVIEW ADAPTER FOR CBT TRANSCRIPT CARDS
// ==========================================================
class TranskripCbtAdapter : RecyclerView.Adapter<TranskripCbtAdapter.ViewHolder>() {

    private val items = mutableListOf<NilaiCbtItem>()

    fun submitList(newItems: List<NilaiCbtItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transkrip_cbt, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvJenisUjianBadge: TextView = itemView.findViewById(R.id.tvJenisUjianBadge)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        private val tvMapel: TextView = itemView.findViewById(R.id.tvMapel)
        private val tvNamaUjian: TextView = itemView.findViewById(R.id.tvNamaUjian)
        private val tvNilaiPg: TextView = itemView.findViewById(R.id.tvNilaiPg)
        private val tvNilaiEsai: TextView = itemView.findViewById(R.id.tvNilaiEsai)
        private val tvKkmDanPredikat: TextView = itemView.findViewById(R.id.tvKkmDanPredikat)
        private val tvSkorTotal: TextView = itemView.findViewById(R.id.tvSkorTotal)
        private val tvTanggalSelesai: TextView = itemView.findViewById(R.id.tvTanggalSelesai)
        private val tvDurasiPengerjaan: TextView = itemView.findViewById(R.id.tvDurasiPengerjaan)

        fun bind(item: NilaiCbtItem) {
            val kodeOrJenis = if (!item.kode_jenis.isNullOrBlank()) item.kode_jenis else (item.jenis_ujian ?: "CBT")
            val taSem = if (!item.tahun_ajaran.isNullOrBlank()) " • ${item.tahun_ajaran} ${item.semester ?: ""}".trim() else ""
            tvJenisUjianBadge.text = "$kodeOrJenis$taSem"

            tvMapel.text = item.nama_mapel ?: "Mata Pelajaran"
            tvNamaUjian.text = item.nama_ujian ?: "-"

            val pgVal = item.nilai_pg
            val esaiVal = item.nilai_esai
            tvNilaiPg.text = if (pgVal % 1.0 == 0.0) pgVal.toInt().toString() else String.format(Locale.US, "%.1f", pgVal)
            tvNilaiEsai.text = if (esaiVal % 1.0 == 0.0) esaiVal.toInt().toString() else String.format(Locale.US, "%.1f", esaiVal)

            val predikat = item.predikat ?: if (item.total_nilai >= item.kkm) "A (Sangat Baik)" else "REMEDIAL"
            tvKkmDanPredikat.text = "KKM: ${item.kkm} • Predikat: $predikat"

            val totalVal = item.total_nilai
            val formattedTotal = if (totalVal % 1.0 == 0.0) totalVal.toInt().toString() else String.format(Locale.US, "%.1f", totalVal)
            tvSkorTotal.text = formattedTotal

            val isLulus = item.status_kelulusan.equals("LULUS", ignoreCase = true) || totalVal >= item.kkm

            if (isLulus) {
                tvStatusBadge.text = "LULUS"
                tvStatusBadge.setBackgroundResource(R.drawable.bg_pill_green_badge)
                tvStatusBadge.setTextColor(Color.parseColor("#15803D"))
                tvSkorTotal.setTextColor(Color.parseColor("#0F766E"))
            } else {
                tvStatusBadge.text = "REMEDIAL"
                tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_red_soft)
                tvStatusBadge.setTextColor(Color.parseColor("#DC2626"))
                tvSkorTotal.setTextColor(Color.parseColor("#DC2626"))
            }

            tvTanggalSelesai.text = item.tanggal_selesai ?: "-"
            tvDurasiPengerjaan.text = item.durasi_pengerjaan ?: "Selesai"
        }
    }
}
