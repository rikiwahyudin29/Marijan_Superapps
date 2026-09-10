package com.rtekmidev.smkrjsuperapps

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.cardview.widget.CardView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.rtekmidev.smkrjsuperapps.api.ApiClient
import com.rtekmidev.smkrjsuperapps.api.DataRiwayat
import com.rtekmidev.smkrjsuperapps.api.DataSanggahan
import com.rtekmidev.smkrjsuperapps.api.DataTagihan
import com.rtekmidev.smkrjsuperapps.api.RingkasanKeuangan
import com.rtekmidev.smkrjsuperapps.util.LoadingDialogHelper
import com.rtekmidev.smkrjsuperapps.util.RefreshableFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.net.URLEncoder
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KeuanganFragment : Fragment(), RefreshableFragment {

    private var nisnSiswa = ""
    private var namaSiswa = ""
    private var waKeuanganSekolah = "+6283101457709"
    private val allTagihanList = mutableListOf<DataTagihan>()
    private val allRiwayatList = mutableListOf<DataRiwayat>()
    private val allSanggahanList = mutableListOf<DataSanggahan>()
    private var selectedKategori = "Semua"
    private var currentRingkasan: RingkasanKeuangan? = null
    private var loadingDialog: Dialog? = null
    private var pendingMultipleImageCallback: ((List<Uri>) -> Unit)? = null

    private val pickMultipleImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            pendingMultipleImageCallback?.invoke(uris)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        LoadingDialogHelper.dismiss(loadingDialog)
        loadingDialog = null
    }

    private val paymentLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        view?.let { muatTagihan(it) }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_keuangan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisnSiswa = sharedPref.getString("nisn", "") ?: ""
        namaSiswa = sharedPref.getString("nama_siswa", null)
            ?: sharedPref.getString("nama", "Siswa")
            ?: "Siswa"

        view.findViewById<TextView>(R.id.tvNamaSiswaKeuangan)?.text = namaSiswa

        // Bottom nav auto-hide on scroll
        view.findViewById<View>(R.id.scrollViewKeuangan)?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 12) {
                (activity as? DashboardActivity)?.hideBottomNav()
            } else if (dy < -12 || scrollY <= 10) {
                (activity as? DashboardActivity)?.showBottomNav()
            }
        }

        // WhatsApp TU & Keuangan
        view.findViewById<View>(R.id.btnHubungiWaKeuangan)?.setOnClickListener {
            hubungiWaKeuangan(waKeuanganSekolah)
        }

        // Bayar Sekaligus click
        view.findViewById<View>(R.id.btnBayarSekaligus)?.setOnClickListener {
            val sisaTotal = currentRingkasan?.sisa_tagihan ?: 0.0
            if (sisaTotal <= 0.0) {
                Toast.makeText(requireContext(), "Semua tagihan Anda sudah lunas!", Toast.LENGTH_SHORT).show()
            } else {
                val itemCount = currentRingkasan?.item_aktif_count ?: allTagihanList.count { !it.status_bayar.equals("LUNAS", true) }
                showModalNominalBayar(
                    isBulk = true,
                    itemTitle = "Bayar Sekaligus ($itemCount Tagihan Aktif)",
                    sisaNominal = sisaTotal,
                    idTagihan = "ALL"
                )
            }
        }

        // Rincian Lengkap / Info
        view.findViewById<View>(R.id.btnRincianLengkap)?.setOnClickListener {
            Toast.makeText(requireContext(), "Menampilkan seluruh tagihan aktif & riwayat", Toast.LENGTH_SHORT).show()
        }

        view.findViewById<View>(R.id.btnLihatSemuaRiwayat)?.setOnClickListener {
            Toast.makeText(requireContext(), "Total ${allRiwayatList.size} transaksi pembayaran tercatat", Toast.LENGTH_SHORT).show()
        }

        // Ajukan Sanggahan Baru Button
        view.findViewById<View>(R.id.btnAjukanSanggahanBaru)?.setOnClickListener {
            showModalSanggahan(null)
        }

        muatTagihan(view)
    }

    override fun refreshData() {
        view?.let { muatTagihan(it) }
    }

    private fun muatTagihan(view: View) {
        val loading = view.findViewById<LinearLayout>(R.id.loadingKeuangan)
        loading?.visibility = View.VISIBLE
        LoadingDialogHelper.dismiss(loadingDialog)
        loadingDialog = LoadingDialogHelper.show(context, "Memuat data keuangan...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.getTagihan(nisnSiswa)
                withContext(Dispatchers.Main) {
                    loading?.visibility = View.GONE
                    LoadingDialogHelper.dismiss(loadingDialog)
                    loadingDialog = null
                    if (resp.isSuccessful && resp.body()?.status == true) {
                        val data = resp.body()!!

                        // 1. Update WhatsApp contact
                        if (!data.wa_keuangan.isNullOrEmpty()) {
                            waKeuanganSekolah = data.wa_keuangan
                        }

                        // 2. Bind Hero Card Badges & Info
                        view.findViewById<TextView>(R.id.tvBadgeTa)?.text =
                            data.tahun_ajaran ?: "● TA 2026/2027 • Ganjil"

                        view.findViewById<TextView>(R.id.tvTanggalHariIni)?.text =
                            "📅 " + (data.tanggal_hari_ini ?: getFormattedToday())

                        val studentName = data.siswa?.nama_lengkap ?: namaSiswa
                        view.findViewById<TextView>(R.id.tvNamaSiswaKeuangan)?.text = studentName

                        val studentClass = data.siswa?.nama_kelas ?: "Siswa"
                        val studentNis = data.siswa?.nis ?: nisnSiswa
                        view.findViewById<TextView>(R.id.tvKelasNisKeuangan)?.text =
                            "$studentClass • NIS: $studentNis"

                        // 3. Bind 3 Stats Cards
                        currentRingkasan = data.ringkasan
                        val totalTagihan = data.ringkasan?.total_tagihan ?: 0.0
                        val totalDibayar = data.ringkasan?.total_dibayar ?: 0.0
                        val sisaTunggakan = data.ringkasan?.sisa_tagihan ?: 0.0
                        val itemAktif = data.ringkasan?.item_aktif_count ?: 0
                        val persenLunas = data.ringkasan?.persen_lunas ?: 0

                        view.findViewById<TextView>(R.id.tvStatTotalTagihan)?.text = formatRupiah(totalTagihan)
                        view.findViewById<TextView>(R.id.tvStatItemAktif)?.text = "$itemAktif Item Aktif"

                        view.findViewById<TextView>(R.id.tvStatTotalDibayar)?.text = formatRupiah(totalDibayar)
                        view.findViewById<TextView>(R.id.tvStatPersenLunas)?.text = "$persenLunas% Lunas"

                        view.findViewById<TextView>(R.id.tvStatSisaTunggakan)?.text = formatRupiah(sisaTunggakan)
                        val tvStatusTunggakan = view.findViewById<TextView>(R.id.tvStatStatusTunggakan)
                        if (sisaTunggakan <= 0.0) {
                            tvStatusTunggakan?.text = "Lunas"
                            tvStatusTunggakan?.setTextColor(Color.parseColor("#059669"))
                            tvStatusTunggakan?.setBackgroundResource(R.drawable.bg_badge_green_soft)
                        } else {
                            tvStatusTunggakan?.text = "Belum Lunas"
                            tvStatusTunggakan?.setTextColor(Color.parseColor("#DC2626"))
                            tvStatusTunggakan?.setBackgroundResource(R.drawable.bg_badge_red_soft)
                        }

                        view.findViewById<TextView>(R.id.tvTotalTagihanCountBadge)?.text = "$itemAktif Item"

                        // 4. Update Tagihan List & Filter Chips
                        allTagihanList.clear()
                        data.tagihan?.let { allTagihanList.addAll(it) }

                        val kategoriList = mutableListOf("Semua")
                        data.kategori_list?.let { kategoriList.addAll(it) }
                        if (kategoriList.size <= 1) {
                            // Extract distinct from allTagihanList
                            allTagihanList.forEach { tag ->
                                val pos = tag.nama_pos?.trim()
                                if (!pos.isNullOrEmpty() && !kategoriList.contains(pos)) {
                                    kategoriList.add(pos)
                                }
                            }
                        }

                        setupFilterChips(view, kategoriList)
                        renderDaftarTagihan(view)

                        // 5. Update Riwayat Transaksi
                        allRiwayatList.clear()
                        data.riwayat?.let { allRiwayatList.addAll(it) }
                        renderDaftarRiwayat(view)

                        // 6. Update Sanggahan / Bukti Transfer
                        allSanggahanList.clear()
                        data.sanggahan?.let { allSanggahanList.addAll(it) }
                        renderDaftarSanggahan(view)

                    } else {
                        Toast.makeText(requireContext(), "Gagal memuat data keuangan.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading?.visibility = View.GONE
                    LoadingDialogHelper.dismiss(loadingDialog)
                    loadingDialog = null
                    Toast.makeText(requireContext(), "Koneksi terganggu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun setupFilterChips(view: View, categories: List<String>) {
        val wadahChips = view.findViewById<LinearLayout>(R.id.wadahFilterChips) ?: return
        wadahChips.removeAllViews()

        categories.forEach { kat ->
            val isSelected = kat.equals(selectedKategori, ignoreCase = true)
            val chip = TextView(requireContext()).apply {
                val countText = if (kat.equals("Semua", true)) " (${allTagihanList.size})" else ""
                text = "$kat$countText"
                textSize = 11f
                setPadding(dpToPx(12), dpToPx(7), dpToPx(12), dpToPx(7))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    marginEnd = dpToPx(8)
                }
                layoutParams = params
                isClickable = true
                isFocusable = true

                if (isSelected) {
                    setTextColor(Color.WHITE)
                    setBackgroundResource(R.drawable.bg_chip_filter_active)
                    paint.isFakeBoldText = true
                } else {
                    setTextColor(Color.parseColor("#475569"))
                    setBackgroundResource(R.drawable.bg_chip_filter_inactive)
                    paint.isFakeBoldText = false
                }

                setOnClickListener {
                    selectedKategori = kat
                    setupFilterChips(view, categories)
                    renderDaftarTagihan(view)
                }
            }
            wadahChips.addView(chip)
        }
    }

    private fun renderDaftarTagihan(view: View) {
        val wadah = view.findViewById<LinearLayout>(R.id.wadahTagihan) ?: return
        val wadahKosong = view.findViewById<View>(R.id.wadahTagihanKosong)
        wadah.removeAllViews()

        val filteredList = if (selectedKategori.equals("Semua", ignoreCase = true)) {
            allTagihanList
        } else {
            allTagihanList.filter { it.nama_pos.equals(selectedKategori, ignoreCase = true) }
        }

        if (filteredList.isEmpty()) {
            wadahKosong?.visibility = View.VISIBLE
            return
        }
        wadahKosong?.visibility = View.GONE

        filteredList.forEach { tagihan ->
            val card = layoutInflater.inflate(R.layout.item_tagihan, wadah, false) as CardView
            val nomTagihan = tagihan.nominal_tagihan?.toDoubleOrNull() ?: 0.0
            val nomTerbayar = tagihan.nominal_terbayar?.toDoubleOrNull() ?: 0.0
            val sisaNominal = (nomTagihan - nomTerbayar).coerceAtLeast(0.0)
            val isLunas = tagihan.status_bayar.equals("LUNAS", ignoreCase = true)
            val isCicil = !isLunas && nomTerbayar > 0

            // Title & Subtitle
            card.findViewById<TextView>(R.id.tvNamaTagihan)?.text = tagihan.nama_pos ?: "Tagihan"
            val bulanInfo = if (!tagihan.bulan_ke.isNullOrEmpty() && tagihan.bulan_ke != "0") tagihan.bulan_ke else "-"
            val ketInfo = if (!tagihan.keterangan.isNullOrEmpty()) tagihan.keterangan else "-"
            card.findViewById<TextView>(R.id.tvKeteranganTagihan)?.text = "Bulan ke-$bulanInfo | Keterangan: $ketInfo"

            // Bind Tahun Ajaran Badge
            val taText = if (!tagihan.tahun_ajaran.isNullOrEmpty()) {
                "TA ${tagihan.tahun_ajaran}" + (if (!tagihan.semester.isNullOrEmpty()) " • ${tagihan.semester}" else "")
            } else ""
            val tvBadgeTa = card.findViewById<TextView>(R.id.tvBadgeTahunAjaran)
            if (taText.isNotEmpty()) {
                tvBadgeTa?.visibility = View.VISIBLE
                tvBadgeTa?.text = taText
            } else {
                tvBadgeTa?.visibility = View.GONE
            }

            // Nominal
            card.findViewById<TextView>(R.id.tvNominalTagihan)?.text = formatRupiah(nomTagihan)
            card.findViewById<TextView>(R.id.tvDibayarTagihan)?.text = "Dibayar: " + formatRupiah(nomTerbayar)
            card.findViewById<TextView>(R.id.tvSisaTagihan)?.text = "Sisa: " + formatRupiah(sisaNominal)

            // Badge & Icon
            val tvBadge = card.findViewById<TextView>(R.id.tvBadgeStatus)
            val ivIcon = card.findViewById<ImageView>(R.id.ivIconTagihan)
            val boxIcon = card.findViewById<View>(R.id.boxIconTagihan)
            val btnBayar = card.findViewById<View>(R.id.btnBayarTagihan)

            if (isLunas) {
                tvBadge?.text = "LUNAS"
                tvBadge?.setTextColor(Color.parseColor("#059669"))
                tvBadge?.setBackgroundResource(R.drawable.bg_badge_green_soft)
                boxIcon?.setBackgroundResource(R.drawable.bg_badge_green_soft)
                ivIcon?.setImageResource(R.drawable.ic_check_circle_24)
                ivIcon?.setColorFilter(Color.parseColor("#10B981"))
                btnBayar?.visibility = View.GONE
            } else if (isCicil) {
                tvBadge?.text = "MENYICIL"
                tvBadge?.setTextColor(Color.parseColor("#D97706"))
                tvBadge?.setBackgroundResource(R.drawable.bg_badge_amber_soft)
                boxIcon?.setBackgroundResource(R.drawable.bg_badge_amber_soft)
                ivIcon?.setImageResource(R.drawable.ic_document_bill)
                ivIcon?.setColorFilter(Color.parseColor("#D97706"))
                btnBayar?.visibility = View.VISIBLE
            } else {
                tvBadge?.text = "BELUM"
                tvBadge?.setTextColor(Color.parseColor("#DC2626"))
                tvBadge?.setBackgroundResource(R.drawable.bg_badge_red_soft)
                boxIcon?.setBackgroundResource(R.drawable.bg_badge_red_soft)
                ivIcon?.setImageResource(R.drawable.ic_document_bill)
                ivIcon?.setColorFilter(Color.parseColor("#EF4444"))
                btnBayar?.visibility = View.VISIBLE
            }

            btnBayar?.setOnClickListener {
                showModalNominalBayar(
                    isBulk = false,
                    itemTitle = tagihan.nama_pos ?: "Tagihan",
                    sisaNominal = sisaNominal,
                    idTagihan = tagihan.id ?: ""
                )
            }

            // Sanggah Button
            val btnSanggah = card.findViewById<View>(R.id.btnSanggahTagihan)
            if (isLunas) {
                btnSanggah?.visibility = View.GONE
            } else {
                btnSanggah?.visibility = View.VISIBLE
                btnSanggah?.setOnClickListener {
                    showModalSanggahan(tagihan)
                }
            }

            wadah.addView(card)
        }
    }

    private fun renderDaftarRiwayat(view: View) {
        val wadahRiwayat = view.findViewById<LinearLayout>(R.id.wadahRiwayat) ?: return
        val layoutKosong = view.findViewById<View>(R.id.layoutRiwayatKosong)
        wadahRiwayat.removeAllViews()

        if (allRiwayatList.isEmpty()) {
            layoutKosong?.visibility = View.VISIBLE
            return
        }
        layoutKosong?.visibility = View.GONE

        allRiwayatList.forEach { riwayat ->
            val card = layoutInflater.inflate(R.layout.item_riwayat, wadahRiwayat, false) as CardView
            card.findViewById<TextView>(R.id.tvTanggalRiwayat)?.text = riwayat.created_at ?: "-"
            card.findViewById<TextView>(R.id.tvNamaRiwayat)?.text = riwayat.nama_pos ?: "Pembayaran Tagihan"

            val taStr = if (!riwayat.tahun_ajaran.isNullOrEmpty()) "TA ${riwayat.tahun_ajaran} • " else ""
            card.findViewById<TextView>(R.id.tvMetodeRiwayat)?.text = "$taStr${riwayat.payment_type ?: "QRIS"}"

            val nom = riwayat.jumlah_bayar?.toDoubleOrNull()
                ?: riwayat.total_bayar?.toDoubleOrNull()
                ?: 0.0
            card.findViewById<TextView>(R.id.tvNominalRiwayat)?.text = "+ " + formatRupiah(nom)

            val tvBadge = card.findViewById<TextView>(R.id.tvBadgeRiwayat)
            val st = riwayat.status_tagihan ?: riwayat.status_bayar ?: "LUNAS"
            if (st.equals("CICIL", ignoreCase = true)) {
                tvBadge?.text = "CICIL"
                tvBadge?.setTextColor(Color.parseColor("#D97706"))
                tvBadge?.setBackgroundResource(R.drawable.bg_badge_amber_soft)
            } else {
                tvBadge?.text = "LUNAS"
                tvBadge?.setTextColor(Color.parseColor("#059669"))
                tvBadge?.setBackgroundResource(R.drawable.bg_badge_green_soft)
            }

            val tvKodeTrx = card.findViewById<TextView>(R.id.tvKodeTrx)
            tvKodeTrx?.text = riwayat.id?.let { "TRX-$it" } ?: (riwayat.created_at ?: "-")

            val btnKwitansi = card.findViewById<View>(R.id.btnDownloadKwitansi)
            val kwitansiUrl = riwayat.kwitansi_url
                ?: (ApiClient.BASE_URL.trimEnd('/') + "/api/keuangan/kwitansi/" + (riwayat.id ?: "0"))

            btnKwitansi?.setOnClickListener {
                try {
                    val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(kwitansiUrl))
                    startActivity(browserIntent)
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Gagal membuka kwitansi: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }

            wadahRiwayat.addView(card)
        }
    }

    // =========================================================================
    // MODAL CICILAN PEMBAYARAN FLEKSIBEL (BOTTOM SHEET ALA WEB)
    // =========================================================================
    private fun showModalNominalBayar(
        isBulk: Boolean,
        itemTitle: String,
        sisaNominal: Double,
        idTagihan: String
    ) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_nominal_bayar, null)
        bottomSheet.setContentView(sheetView)

        val tvTitle = sheetView.findViewById<TextView>(R.id.tvModalTitle)
        val tvItemNama = sheetView.findViewById<TextView>(R.id.tvItemModalNama)
        val tvSisaTagihan = sheetView.findViewById<TextView>(R.id.tvModalSisaTagihan)
        val etNominal = sheetView.findViewById<EditText>(R.id.etNominalBayar)
        val tvError = sheetView.findViewById<TextView>(R.id.tvErrorNominal)
        val btnSubmit = sheetView.findViewById<Button>(R.id.btnSubmitModalQris)
        val btnBatal = sheetView.findViewById<Button>(R.id.btnBatalModalBayar)
        val btnClose = sheetView.findViewById<ImageView>(R.id.btnTutupModalBayar)

        tvTitle.text = if (isBulk) "Bayar Seluruh Tagihan" else "Nominal Pembayaran"
        tvItemNama.text = itemTitle
        tvSisaTagihan.text = formatRupiah(sisaNominal)

        // Default: Bayar penuh
        val sisaLong = sisaNominal.toLong()
        etNominal.setText(sisaLong.toString())
        etNominal.setSelection(etNominal.text.length)

        // Quick Preset Chips
        sheetView.findViewById<View>(R.id.btnPresetFull)?.setOnClickListener {
            etNominal.setText(sisaLong.toString())
            etNominal.setSelection(etNominal.text.length)
        }

        sheetView.findViewById<View>(R.id.btnPreset50k)?.setOnClickListener {
            val nom = 50000L.coerceAtMost(sisaLong)
            etNominal.setText(nom.toString())
            etNominal.setSelection(etNominal.text.length)
        }

        sheetView.findViewById<View>(R.id.btnPreset100k)?.setOnClickListener {
            val nom = 100000L.coerceAtMost(sisaLong)
            etNominal.setText(nom.toString())
            etNominal.setSelection(etNominal.text.length)
        }

        sheetView.findViewById<View>(R.id.btnPreset200k)?.setOnClickListener {
            val nom = 200000L.coerceAtMost(sisaLong)
            etNominal.setText(nom.toString())
            etNominal.setSelection(etNominal.text.length)
        }

        // Real-time Validation
        etNominal.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val raw = s?.toString()?.trim() ?: ""
                val inputVal = raw.toLongOrNull() ?: 0L

                if (raw.isEmpty()) {
                    tvError.text = "Nominal tidak boleh kosong."
                    tvError.setTextColor(Color.parseColor("#DC2626"))
                    btnSubmit.isEnabled = false
                } else if (inputVal < 10000) {
                    tvError.text = "Minimal pembayaran adalah Rp 10.000"
                    tvError.setTextColor(Color.parseColor("#DC2626"))
                    btnSubmit.isEnabled = false
                } else if (inputVal > sisaLong) {
                    tvError.text = "Nominal melebihi sisa tagihan (${formatRupiah(sisaNominal)})"
                    tvError.setTextColor(Color.parseColor("#DC2626"))
                    btnSubmit.isEnabled = false
                } else {
                    tvError.text = "Batas minimal Rp 10.000. Ketikkan angka tanpa titik."
                    tvError.setTextColor(Color.parseColor("#94A3B8"))
                    btnSubmit.isEnabled = true
                }
            }
        })

        btnBatal.setOnClickListener { bottomSheet.dismiss() }
        btnClose.setOnClickListener { bottomSheet.dismiss() }

        btnSubmit.setOnClickListener {
            val inputVal = etNominal.text.toString().trim().toLongOrNull() ?: 0L
            if (inputVal < 10000 || inputVal > sisaLong) {
                Toast.makeText(requireContext(), "Nominal pembayaran belum valid", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            bottomSheet.dismiss()
            eksekusiBayarQris(idTagihan, inputVal, itemTitle)
        }

        bottomSheet.show()
    }

    // =========================================================================
    // EKSEKUSI PEMBAYARAN KE API & BUKA DIALOG QRIS NATIVE SENADA
    // =========================================================================
    private fun eksekusiBayarQris(idTagihan: String, nominalBayar: Long, itemTitle: String) {
        val loading = view?.findViewById<LinearLayout>(R.id.loadingKeuangan)
        loading?.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.bayarTagihan(nisnSiswa, idTagihan, "QRIS", nominalBayar)
                withContext(Dispatchers.Main) {
                    loading?.visibility = View.GONE
                    if (resp.isSuccessful && resp.body()?.status == true) {
                        val body = resp.body()!!
                        val checkoutUrl = body.checkout_url
                        val qrData = body.qr_string ?: body.qr_url

                        if (!checkoutUrl.isNullOrEmpty()) {
                            // Buka halaman pembayaran resmi iPaymu (QRIS resmi berstandar BI yang dapat discan 100%)
                            val intent = Intent(requireContext(), PaymentWebViewActivity::class.java).apply {
                                putExtra("EXTRA_PAYMENT_URL", checkoutUrl)
                                putExtra("EXTRA_PAYMENT_TITLE", body.nama_pos ?: itemTitle)
                            }
                            paymentLauncher.launch(intent)
                        } else if (!qrData.isNullOrEmpty()) {
                            val ref = body.merchant_ref ?: body.reference ?: "REF-${System.currentTimeMillis()}"
                            val finalNominal = body.nominal ?: nominalBayar
                            val posName = body.nama_pos ?: itemTitle
                            showDialogNativeQris(qrData, ref, finalNominal, posName)
                        } else {
                            Toast.makeText(requireContext(), "Pembayaran berhasil disiapkan", Toast.LENGTH_SHORT).show()
                            refreshData()
                        }
                    } else {
                        val msg = resp.body()?.message ?: "Gagal menyiapkan pembayaran QRIS"
                        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading?.visibility = View.GONE
                    Toast.makeText(requireContext(), "Koneksi terganggu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // =========================================================================
    // NATIVE QRIS DIALOG (DESAIN SENADA, TANPA WEBVIEW TRIPAY/IPAYMU)
    // =========================================================================
    private fun showDialogNativeQris(
        qrPayload: String,
        merchantRef: String,
        nominal: Long,
        namaPos: String
    ) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_native_qris, null)
        dialog.setContentView(dialogView)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val tvItemNama = dialogView.findViewById<TextView>(R.id.tvQrisItemNama)
        val tvNominalBesar = dialogView.findViewById<TextView>(R.id.tvQrisNominalBesar)
        val tvRef = dialogView.findViewById<TextView>(R.id.tvQrisMerchantRef)
        val btnSalin = dialogView.findViewById<View>(R.id.btnSalinRef)
        val ivQr = dialogView.findViewById<ImageView>(R.id.ivQrCodeImage)
        val btnSimpan = dialogView.findViewById<Button>(R.id.btnSimpanQr)
        val btnSelesai = dialogView.findViewById<Button>(R.id.btnSelesaiBayar)
        val btnClose = dialogView.findViewById<ImageView>(R.id.btnTutupQrisDialog)

        tvItemNama.text = namaPos
        tvNominalBesar.text = formatRupiah(nominal.toDouble())
        tvRef.text = merchantRef

        // Generate Crisp QR Code Bitmap via ZXing
        var generatedBitmap: Bitmap? = null
        try {
            val barcodeEncoder = BarcodeEncoder()
            val bitmap = barcodeEncoder.encodeBitmap(qrPayload, com.google.zxing.BarcodeFormat.QR_CODE, 600, 600)
            generatedBitmap = bitmap
            ivQr.setImageBitmap(bitmap)
        } catch (_: Exception) {
            // Fallback load via Glide
            val qrUrl = "https://api.qrserver.com/v1/create-qr-code/?size=500x500&data=" + URLEncoder.encode(qrPayload, "UTF-8")
            Glide.with(this).load(qrUrl).into(ivQr)
        }

        btnSalin.setOnClickListener {
            val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Kode Referensi", merchantRef)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(requireContext(), "Nomor referensi disalin: $merchantRef", Toast.LENGTH_SHORT).show()
        }

        btnSimpan.setOnClickListener {
            if (generatedBitmap != null) {
                simpanGambarQr(generatedBitmap, "QRIS_$merchantRef")
            } else {
                Toast.makeText(requireContext(), "QR Code sedang dimuat...", Toast.LENGTH_SHORT).show()
            }
        }

        btnSelesai.setOnClickListener {
            dialog.dismiss()
            Toast.makeText(requireContext(), "Memperbarui status transaksi...", Toast.LENGTH_SHORT).show()
            refreshData()
        }

        btnClose.setOnClickListener {
            dialog.dismiss()
            refreshData()
        }

        dialog.show()
    }

    private fun simpanGambarQr(bitmap: Bitmap, filename: String) {
        try {
            val contentResolver = requireContext().contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, "$filename.jpg")
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/SMKRJ_QRIS")
                }
            }
            val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                }
                Toast.makeText(requireContext(), "Gambar QRIS berhasil disimpan ke Galeri Foto!", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(requireContext(), "Gagal menyimpan gambar ke galeri", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Gagal menyimpan: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun hubungiWaKeuangan(phoneRaw: String) {
        try {
            var phone = phoneRaw.replace("[^0-9]".toRegex(), "")
            if (phone.startsWith("0")) {
                phone = "62" + phone.substring(1)
            } else if (!phone.startsWith("62")) {
                phone = "62$phone"
            }

            val pesan = "Halo Admin TU dan Keuangan SMK RJ, saya ingin menanyakan perihal administrasi/verifikasi tagihan keuangan saya."
            val url = "https://api.whatsapp.com/send?phone=$phone&text=" + URLEncoder.encode(pesan, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (_: Exception) {
            Toast.makeText(requireContext(), "Aplikasi WhatsApp tidak ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatRupiah(angka: Double): String {
        val format = NumberFormat.getCurrencyInstance(Locale.Builder().setLanguage("id").setRegion("ID").build())
        return format.format(angka).replace(",00", "")
    }

    private fun getFormattedToday(): String {
        val sdf = SimpleDateFormat("EEEE, d MMM yyyy", Locale("id", "ID"))
        return sdf.format(Date())
    }

    private fun dpToPx(dp: Int): Int {
        val density = resources.displayMetrics.density
        return (dp * density).toInt()
    }

    // =========================================================================
    // SECTION SANGGAHAN & BUKTI TRANSFER
    // =========================================================================

    private fun renderDaftarSanggahan(view: View) {
        val wadah = view.findViewById<LinearLayout>(R.id.wadahSanggahan) ?: return
        val layoutKosong = view.findViewById<View>(R.id.layoutSanggahanKosong)
        val tvBadgeCount = view.findViewById<TextView>(R.id.tvCountSanggahanBadge)
        wadah.removeAllViews()

        tvBadgeCount?.text = "${allSanggahanList.size} Item"

        if (allSanggahanList.isEmpty()) {
            layoutKosong?.visibility = View.VISIBLE
            return
        }
        layoutKosong?.visibility = View.GONE

        allSanggahanList.forEach { sanggahan ->
            val card = layoutInflater.inflate(R.layout.item_sanggahan, wadah, false) as CardView

            // Tanggal & Kode
            card.findViewById<TextView>(R.id.tvTanggalSanggahan)?.text =
                sanggahan.tanggal_pengajuan_formatted ?: sanggahan.tanggal_pengajuan ?: "-"
            card.findViewById<TextView>(R.id.tvKodeSanggahan)?.text =
                sanggahan.kode_sanggahan ?: "SGH-${sanggahan.id ?: 0}"

            // Badge Status
            val tvBadge = card.findViewById<TextView>(R.id.tvBadgeStatusSanggahan)
            val status = sanggahan.status ?: "PENDING"
            when {
                status.equals("DISETUJUI", true) || status.equals("APPROVED", true) -> {
                    tvBadge?.text = "DISETUJUI"
                    tvBadge?.setTextColor(Color.parseColor("#059669"))
                    tvBadge?.setBackgroundResource(R.drawable.bg_badge_green_soft)
                }
                status.equals("DITOLAK", true) || status.equals("REJECTED", true) -> {
                    tvBadge?.text = "DITOLAK"
                    tvBadge?.setTextColor(Color.parseColor("#DC2626"))
                    tvBadge?.setBackgroundResource(R.drawable.bg_badge_red_soft)
                }
                else -> {
                    tvBadge?.text = "MENUNGGU REVIEW"
                    tvBadge?.setTextColor(Color.parseColor("#D97706"))
                    tvBadge?.setBackgroundResource(R.drawable.bg_badge_amber_soft)
                }
            }

            // Nama Tagihan & Keterangan
            card.findViewById<TextView>(R.id.tvNamaTagihanSanggahan)?.text =
                sanggahan.nama_pos ?: "Tagihan"
            card.findViewById<TextView>(R.id.tvKeteranganSiswa)?.text =
                sanggahan.keterangan ?: "-"

            // Nominal
            val nominal = sanggahan.nominal ?: 0.0
            card.findViewById<TextView>(R.id.tvNominalSanggahan)?.text =
                sanggahan.nominal_formatted ?: formatRupiah(nominal)

            // Thumbnail & Multi-foto Bukti Transfer
            val buktiUrls = sanggahan.bukti_urls?.filter { it.isNotBlank() }
                ?: listOfNotNull(sanggahan.bukti_pembayaran_url).filter { it.isNotBlank() }

            val ivThumb = card.findViewById<ImageView>(R.id.ivThumbnailBukti)
            val tvCount = card.findViewById<TextView>(R.id.tvCountFotoBukti)
            val scrollMulti = card.findViewById<HorizontalScrollView>(R.id.scrollItemBuktiMulti)
            val llMulti = card.findViewById<LinearLayout>(R.id.llItemBuktiMultiContainer)

            if (buktiUrls.isNotEmpty()) {
                if (ivThumb != null) {
                    Glide.with(this).load(buktiUrls[0]).centerCrop().into(ivThumb)
                    card.findViewById<View>(R.id.boxThumbnailBukti)?.setOnClickListener {
                        showPhotoPreviewDialog(buktiUrls[0])
                    }
                }

                if (buktiUrls.size > 1) {
                    tvCount?.visibility = View.VISIBLE
                    tvCount?.text = "${buktiUrls.size} foto"

                    scrollMulti?.visibility = View.VISIBLE
                    llMulti?.removeAllViews()

                    buktiUrls.forEachIndexed { idx, url ->
                        val thumbCard = CardView(requireContext()).apply {
                            radius = dpToPx(8).toFloat()
                            cardElevation = 0f
                            setCardBackgroundColor(Color.parseColor("#F1F5F9"))
                            val params = LinearLayout.LayoutParams(dpToPx(46), dpToPx(46)).apply {
                                marginEnd = dpToPx(6)
                            }
                            layoutParams = params
                        }

                        val img = ImageView(requireContext()).apply {
                            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                            scaleType = ImageView.ScaleType.CENTER_CROP
                        }
                        Glide.with(this).load(url).centerCrop().into(img)
                        thumbCard.addView(img)

                        thumbCard.setOnClickListener {
                            showPhotoPreviewDialog(url)
                        }
                        llMulti?.addView(thumbCard)
                    }
                } else {
                    tvCount?.visibility = View.GONE
                    scrollMulti?.visibility = View.GONE
                }
            } else {
                tvCount?.visibility = View.GONE
                scrollMulti?.visibility = View.GONE
            }

            // Catatan Petugas (jika ditolak/disetujui)
            val layoutCatatan = card.findViewById<LinearLayout>(R.id.layoutCatatanPetugas)
            val tvCatatan = card.findViewById<TextView>(R.id.tvCatatanPetugas)
            val ivIconCatatan = card.findViewById<ImageView>(R.id.ivIconCatatan)
            val catatanPetugas = sanggahan.catatan_petugas
            if (!catatanPetugas.isNullOrEmpty()) {
                layoutCatatan?.visibility = View.VISIBLE
                tvCatatan?.text = catatanPetugas
                if (status.equals("DITOLAK", true) || status.equals("REJECTED", true)) {
                    layoutCatatan?.setBackgroundColor(Color.parseColor("#FEF2F2"))
                    tvCatatan?.setTextColor(Color.parseColor("#991B1B"))
                    ivIconCatatan?.setColorFilter(Color.parseColor("#DC2626"))
                } else {
                    layoutCatatan?.setBackgroundColor(Color.parseColor("#F0FDF4"))
                    tvCatatan?.setTextColor(Color.parseColor("#166534"))
                    ivIconCatatan?.setColorFilter(Color.parseColor("#059669"))
                }
            } else {
                layoutCatatan?.visibility = View.GONE
            }

            wadah.addView(card)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun showModalSanggahan(tagihanAwal: DataTagihan? = null) {
        val bottomSheet = BottomSheetDialog(requireContext())
        val sheetView = layoutInflater.inflate(R.layout.bottom_sheet_sanggahan, null)
        bottomSheet.setContentView(sheetView)

        val selectedImages = mutableListOf<Pair<String, Bitmap>>()

        val spTagihan = sheetView.findViewById<Spinner>(R.id.spTagihanSanggahan)
        val etNominal = sheetView.findViewById<EditText>(R.id.etNominalSanggahan)
        val etCatatan = sheetView.findViewById<EditText>(R.id.etCatatanSanggahan)
        val boxPilihFoto = sheetView.findViewById<CardView>(R.id.boxPilihFotoBukti)
        val tvUploadPrompt = sheetView.findViewById<TextView>(R.id.tvUploadPrompt)
        val tvCountFotoTerpilih = sheetView.findViewById<TextView>(R.id.tvCountFotoTerpilih)
        val scrollPreview = sheetView.findViewById<HorizontalScrollView>(R.id.scrollPreviewSanggahan)
        val llPreviewContainer = sheetView.findViewById<LinearLayout>(R.id.llPreviewContainerSanggahan)
        val btnClose = sheetView.findViewById<ImageView>(R.id.btnCloseSheetSanggahan)
        val btnBatal = sheetView.findViewById<Button>(R.id.btnBatalSanggahan)
        val btnKirim = sheetView.findViewById<Button>(R.id.btnKirimSanggahan)

        // Populate spinner with active tagihan (belum lunas)
        val activeTagihan = allTagihanList.filter {
            !it.status_bayar.equals("LUNAS", ignoreCase = true)
        }
        val tagihanLabels = activeTagihan.map { tag ->
            val terbayar = tag.nominal_terbayar?.toDoubleOrNull() ?: 0.0
            "${tag.nama_pos ?: "Tagihan"} — Terbayar: ${formatRupiah(terbayar)}"
        }

        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, tagihanLabels)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spTagihan.adapter = adapter

        // Pre-select tagihan if opened from a specific card
        var selectedIdx = 0
        if (tagihanAwal != null) {
            val idx = activeTagihan.indexOfFirst { it.id == tagihanAwal.id }
            if (idx >= 0) {
                selectedIdx = idx
                spTagihan.setSelection(idx)
            }
        }

        // Auto-fill nominal based on selected tagihan (default to nominal_terbayar)
        fun updateNominalForIndex(index: Int) {
            if (index < 0 || index >= activeTagihan.size) return
            val tag = activeTagihan[index]
            val terbayar = tag.nominal_terbayar?.toDoubleOrNull() ?: 0.0
            etNominal.setText(terbayar.toLong().toString())
            etNominal.setSelection(etNominal.text.length)
        }

        updateNominalForIndex(selectedIdx)

        spTagihan.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, position: Int, id: Long) {
                updateNominalForIndex(position)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Helper to update thumbnail previews in bottom sheet
        fun refreshPhotoPreviews() {
            if (selectedImages.isEmpty()) {
                scrollPreview?.visibility = View.GONE
                tvCountFotoTerpilih?.text = "Bisa > 1 Foto"
                tvUploadPrompt?.text = "Ketuk untuk memilih foto bukti (bisa lebih dari 1)"
            } else {
                scrollPreview?.visibility = View.VISIBLE
                tvCountFotoTerpilih?.text = "${selectedImages.size} Foto Terpilih"
                tvUploadPrompt?.text = "+ Tambah Foto Bukti Lainnya"

                llPreviewContainer?.removeAllViews()
                selectedImages.forEachIndexed { idx, pair ->
                    val thumbView = createThumbnailView(pair.second, idx) { removeIdx ->
                        if (removeIdx in selectedImages.indices) {
                            selectedImages.removeAt(removeIdx)
                            refreshPhotoPreviews()
                        }
                    }
                    llPreviewContainer?.addView(thumbView)
                }
            }
        }

        // Photo picker (supports selecting multiple photos at once or incrementally)
        pendingMultipleImageCallback = { uris ->
            uris.forEach { uri ->
                processImageUri(uri)?.let { pair ->
                    selectedImages.add(pair)
                }
            }
            refreshPhotoPreviews()
        }

        boxPilihFoto?.setOnClickListener {
            pickMultipleImagesLauncher.launch("image/*")
        }

        btnClose.setOnClickListener { bottomSheet.dismiss() }
        btnBatal.setOnClickListener { bottomSheet.dismiss() }

        btnKirim.setOnClickListener {
            val posisiTagihan = spTagihan.selectedItemPosition
            if (posisiTagihan < 0 || posisiTagihan >= activeTagihan.size) {
                Toast.makeText(requireContext(), "Pilih tagihan terlebih dahulu", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val nominalStr = etNominal.text.toString().trim()
            val nominalVal = nominalStr.toLongOrNull() ?: 0L
            if (nominalVal < 0) {
                Toast.makeText(requireContext(), "Nominal yang sudah dibayar tidak boleh negatif", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (selectedImages.isEmpty()) {
                Toast.makeText(requireContext(), "Foto bukti pembayaran wajib dilampirkan (minimal 1 foto)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val tagTerpilih = activeTagihan[posisiTagihan]
            val keterangan = etCatatan.text.toString().trim().ifEmpty { null }

            val b64List = selectedImages.map { it.first }
            val jsonBukti = com.google.gson.Gson().toJson(b64List)

            bottomSheet.dismiss()
            submitSanggahan(
                idTagihan = tagTerpilih.id ?: "",
                nominal = nominalVal,
                keterangan = keterangan,
                buktiBase64 = jsonBukti
            )
        }

        bottomSheet.show()
    }

    private fun processImageUri(uri: Uri): Pair<String, Bitmap>? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri) ?: return null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()

            // Compress & resize to max 800px width
            val maxWidth = 800
            val scaledBitmap = if (originalBitmap.width > maxWidth) {
                val ratio = maxWidth.toFloat() / originalBitmap.width.toFloat()
                val newHeight = (originalBitmap.height * ratio).toInt()
                Bitmap.createScaledBitmap(originalBitmap, maxWidth, newHeight, true)
            } else {
                originalBitmap
            }

            val baos = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos)
            val imageBytes = baos.toByteArray()
            val base64 = "data:image/jpeg;base64," + Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            Pair(base64, scaledBitmap)
        } catch (e: Exception) {
            null
        }
    }

    private fun createThumbnailView(
        bitmap: Bitmap,
        index: Int,
        onDelete: (Int) -> Unit
    ): View {
        val root = RelativeLayout(requireContext()).apply {
            layoutParams = LinearLayout.LayoutParams(dpToPx(76), dpToPx(76)).apply {
                marginEnd = dpToPx(8)
            }
        }

        val card = CardView(requireContext()).apply {
            radius = dpToPx(10).toFloat()
            cardElevation = 0f
            setCardBackgroundColor(Color.parseColor("#F1F5F9"))
            val lp = RelativeLayout.LayoutParams(dpToPx(66), dpToPx(66)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_BOTTOM)
                addRule(RelativeLayout.ALIGN_PARENT_START)
            }
            layoutParams = lp
        }

        val img = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            scaleType = ImageView.ScaleType.CENTER_CROP
            setImageBitmap(bitmap)
        }
        card.addView(img)
        root.addView(card)

        // Delete badge button
        val btnDelete = CardView(requireContext()).apply {
            radius = dpToPx(10).toFloat()
            cardElevation = dpToPx(2).toFloat()
            setCardBackgroundColor(Color.parseColor("#EF4444"))
            val lp = RelativeLayout.LayoutParams(dpToPx(20), dpToPx(20)).apply {
                addRule(RelativeLayout.ALIGN_PARENT_TOP)
                addRule(RelativeLayout.ALIGN_PARENT_END)
            }
            layoutParams = lp
        }

        val iconX = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setImageResource(R.drawable.ic_close_24)
            setColorFilter(Color.WHITE)
            setPadding(dpToPx(4), dpToPx(4), dpToPx(4), dpToPx(4))
        }
        btnDelete.addView(iconX)
        btnDelete.setOnClickListener {
            onDelete(index)
        }
        root.addView(btnDelete)

        return root
    }

    private fun submitSanggahan(
        idTagihan: String,
        nominal: Long,
        keterangan: String?,
        buktiBase64: String
    ) {
        LoadingDialogHelper.dismiss(loadingDialog)
        loadingDialog = LoadingDialogHelper.show(context, "Mengirim sanggahan...")

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.submitSanggahan(
                    nisn = nisnSiswa,
                    idTagihan = idTagihan,
                    nominal = nominal,
                    keterangan = keterangan,
                    buktiBase64 = buktiBase64
                )
                withContext(Dispatchers.Main) {
                    LoadingDialogHelper.dismiss(loadingDialog)
                    loadingDialog = null
                    if (resp.isSuccessful && resp.body()?.status == true) {
                        Toast.makeText(requireContext(),
                            resp.body()?.message ?: "Sanggahan berhasil dikirim! Menunggu review keuangan.",
                            Toast.LENGTH_LONG).show()
                        refreshData()
                    } else {
                        Toast.makeText(requireContext(),
                            resp.body()?.message ?: "Gagal mengirim sanggahan",
                            Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    LoadingDialogHelper.dismiss(loadingDialog)
                    loadingDialog = null
                    Toast.makeText(requireContext(), "Koneksi terganggu: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun showPhotoPreviewDialog(imageUrl: String) {
        val dialog = Dialog(requireContext())
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        val dialogView = layoutInflater.inflate(R.layout.dialog_preview_foto, null)
        dialog.setContentView(dialogView)

        dialog.window?.apply {
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val ivPreviewFull = dialogView.findViewById<ImageView>(R.id.ivDialogPreviewFull)
        Glide.with(this).load(imageUrl).into(ivPreviewFull)

        dialogView.findViewById<View>(R.id.btnDialogTutupPreview)?.setOnClickListener {
            dialog.dismiss()
        }
        dialogView.findViewById<View>(R.id.btnDialogTutupBawah)?.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }
}
