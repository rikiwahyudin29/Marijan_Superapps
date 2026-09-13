package com.rtekmidev.marijansuperapps

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.marijansuperapps.api.KeuanganSiswaItem
import java.text.NumberFormat
import java.util.Locale

class KeuanganSiswaBinaanAdapter(
    private val onCetakTagihanClick: (KeuanganSiswaItem) -> Unit,
    private val onWaOrtuClick: (KeuanganSiswaItem) -> Unit
) : RecyclerView.Adapter<KeuanganSiswaBinaanAdapter.SiswaViewHolder>() {

    private val masterList = mutableListOf<KeuanganSiswaItem>()
    private val displayList = mutableListOf<KeuanganSiswaItem>()
    private val expandedSiswaIds = mutableSetOf<Int>()

    private var filterStatus = "ALL" // "ALL", "BELUM_LUNAS", "LUNAS"
    private var searchQuery = ""
    private var sortMode = "ABSEN" // "ABSEN", "NAMA_ASC", "TUNGGAKAN_DESC"

    fun setMasterData(newList: List<KeuanganSiswaItem>) {
        masterList.clear()
        masterList.addAll(newList)
        applyFilters()
    }

    fun setFilterStatus(status: String) {
        filterStatus = status
        applyFilters()
    }

    fun setSearchQuery(query: String) {
        searchQuery = query.trim()
        applyFilters()
    }

    fun setSortMode(mode: String) {
        sortMode = mode
        applyFilters()
    }

    fun getCounts(): Triple<Int, Int, Int> {
        val total = masterList.size
        val belumLunas = masterList.count { it.status.contains("Belum", ignoreCase = true) || it.sisa_tunggakan > 0 }
        val lunas = masterList.count { it.status.equals("Lunas", ignoreCase = true) && it.sisa_tunggakan <= 0 }
        return Triple(total, belumLunas, lunas)
    }

    fun getFilteredItemCount(): Int = displayList.size

    private fun applyFilters() {
        var filtered = masterList.filter { item ->
            // Filter Status
            val matchStatus = when (filterStatus) {
                "BELUM_LUNAS" -> item.status.contains("Belum", ignoreCase = true) || item.sisa_tunggakan > 0
                "LUNAS" -> item.status.equals("Lunas", ignoreCase = true) && item.sisa_tunggakan <= 0
                else -> true
            }

            // Search Query
            val matchSearch = if (searchQuery.isEmpty()) {
                true
            } else {
                val queryLower = searchQuery.lowercase()
                item.nama_siswa.lowercase().contains(queryLower) ||
                        (item.nis ?: "").lowercase().contains(queryLower) ||
                        (item.nisn ?: "").lowercase().contains(queryLower)
            }

            matchStatus && matchSearch
        }

        // Sorting
        filtered = when (sortMode) {
            "NAMA_ASC" -> filtered.sortedBy { it.nama_siswa.lowercase() }
            "TUNGGAKAN_DESC" -> filtered.sortedByDescending { it.sisa_tunggakan }
            else -> filtered.sortedBy { if (it.nomor_absen > 0) it.nomor_absen else Int.MAX_VALUE }
        }

        displayList.clear()
        displayList.addAll(filtered)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiswaViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_keuangan_siswa, parent, false)
        return SiswaViewHolder(view)
    }

    override fun onBindViewHolder(holder: SiswaViewHolder, position: Int) {
        val item = displayList[position]
        val isExpanded = expandedSiswaIds.contains(item.siswa_id)
        holder.bind(item, position, isExpanded)
    }

    override fun getItemCount(): Int = displayList.size

    inner class SiswaViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val layoutCardHeader: LinearLayout = itemView.findViewById(R.id.layoutCardHeader)
        private val tvNomorAbsen: TextView = itemView.findViewById(R.id.tvNomorAbsen)
        private val tvNamaSiswa: TextView = itemView.findViewById(R.id.tvNamaSiswa)
        private val tvNisDanGender: TextView = itemView.findViewById(R.id.tvNisDanGender)
        private val tvStatusBadge: TextView = itemView.findViewById(R.id.tvStatusBadge)
        private val btnQuickCetak: LinearLayout = itemView.findViewById(R.id.btnQuickCetak)
        private val ivChevron: ImageView = itemView.findViewById(R.id.ivChevron)

        private val tvStatTagihan: TextView = itemView.findViewById(R.id.tvStatTagihan)
        private val tvStatTerbayar: TextView = itemView.findViewById(R.id.tvStatTerbayar)
        private val tvStatTunggakan: TextView = itemView.findViewById(R.id.tvStatTunggakan)

        private val layoutExpandedSection: LinearLayout = itemView.findViewById(R.id.layoutExpandedSection)
        private val tvPosCountBadge: TextView = itemView.findViewById(R.id.tvPosCountBadge)
        private val containerPosBreakdown: LinearLayout = itemView.findViewById(R.id.containerPosBreakdown)
        private val btnWaOrtu: LinearLayout = itemView.findViewById(R.id.btnWaOrtu)
        private val btnCetakTagihanExpanded: LinearLayout = itemView.findViewById(R.id.btnCetakTagihanExpanded)

        fun bind(siswa: KeuanganSiswaItem, index: Int, isExpanded: Boolean) {
            val nomorUrut = if (siswa.nomor_absen > 0) siswa.nomor_absen else (index + 1)
            tvNomorAbsen.text = nomorUrut.toString()
            tvNamaSiswa.text = siswa.nama_siswa

            val genderText = when (siswa.jenis_kelamin?.uppercase()) {
                "L", "LAKI-LAKI" -> "Laki-laki"
                "P", "PEREMPUAN" -> "Perempuan"
                else -> siswa.jenis_kelamin ?: "-"
            }
            tvNisDanGender.text = "NIS: ${siswa.nis ?: "-"} • $genderText"

            tvStatTagihan.text = formatRupiah(siswa.total_tagihan)
            tvStatTerbayar.text = formatRupiah(siswa.total_terbayar)
            tvStatTunggakan.text = formatRupiah(siswa.sisa_tunggakan)

            val isLunas = siswa.sisa_tunggakan <= 0 || siswa.status.equals("Lunas", ignoreCase = true)
            if (isLunas) {
                tvStatTunggakan.setTextColor(Color.parseColor("#059669"))
            } else {
                tvStatTunggakan.setTextColor(Color.parseColor("#DC2626"))
            }

            // Header Click & Chevron Click to toggle Expand
            val toggleExpandListener = View.OnClickListener {
                if (expandedSiswaIds.contains(siswa.siswa_id)) {
                    expandedSiswaIds.remove(siswa.siswa_id)
                } else {
                    expandedSiswaIds.add(siswa.siswa_id)
                }
                notifyItemChanged(bindingAdapterPosition)
            }

            layoutCardHeader.setOnClickListener(toggleExpandListener)
            ivChevron.setOnClickListener(toggleExpandListener)

            // Quick Print on Collapsed State
            btnQuickCetak.setOnClickListener {
                onCetakTagihanClick(siswa)
            }

            // Expanded Actions
            btnCetakTagihanExpanded.setOnClickListener {
                onCetakTagihanClick(siswa)
            }

            btnWaOrtu.setOnClickListener {
                onWaOrtuClick(siswa)
            }

            // Accordion State
            if (isExpanded) {
                btnQuickCetak.visibility = View.GONE
                tvStatusBadge.visibility = View.VISIBLE
                ivChevron.setImageResource(R.drawable.ic_chevron_up)
                layoutExpandedSection.visibility = View.VISIBLE

                if (isLunas) {
                    tvStatusBadge.text = "Lunas"
                    tvStatusBadge.setTextColor(Color.parseColor("#059669"))
                    tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECFDF5"))
                    btnWaOrtu.visibility = View.GONE
                } else {
                    tvStatusBadge.text = "Belum Lunas"
                    tvStatusBadge.setTextColor(Color.parseColor("#DC2626"))
                    tvStatusBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                    btnWaOrtu.visibility = View.VISIBLE
                }

                // Populate Pos Breakdown
                containerPosBreakdown.removeAllViews()
                val rincianList = siswa.rincian ?: emptyList()
                tvPosCountBadge.text = "${rincianList.size} Pos Bayar"

                val inflater = LayoutInflater.from(itemView.context)
                for (pos in rincianList) {
                    val posRow = inflater.inflate(R.layout.item_keuangan_pos_row, containerPosBreakdown, false)
                    val tvRowNamaPos = posRow.findViewById<TextView>(R.id.tvRowNamaPos)
                    val tvRowTahunAjaran = posRow.findViewById<TextView>(R.id.tvRowTahunAjaran)
                    val tvRowStatusPos = posRow.findViewById<TextView>(R.id.tvRowStatusPos)
                    val tvRowTagihan = posRow.findViewById<TextView>(R.id.tvRowTagihan)
                    val tvRowTerbayar = posRow.findViewById<TextView>(R.id.tvRowTerbayar)
                    val tvRowSisa = posRow.findViewById<TextView>(R.id.tvRowSisa)

                    tvRowNamaPos.text = pos.nama_pos

                    val taLengkap = pos.tahun_ajaran_lengkap
                    if (!taLengkap.isNullOrBlank()) {
                        tvRowTahunAjaran.visibility = View.VISIBLE
                        tvRowTahunAjaran.text = "📅 $taLengkap"
                    } else if (!pos.tahun_ajaran.isNullOrBlank()) {
                        tvRowTahunAjaran.visibility = View.VISIBLE
                        tvRowTahunAjaran.text = "📅 TA ${pos.tahun_ajaran}" + (if (!pos.semester.isNullOrBlank()) " • ${pos.semester}" else "")
                    } else {
                        tvRowTahunAjaran.visibility = View.GONE
                    }

                    tvRowTagihan.text = "Tagihan: " + formatRupiah(pos.nominal_tagihan)
                    tvRowTerbayar.text = "Bayar: " + formatRupiah(pos.nominal_terbayar)
                    tvRowSisa.text = "Sisa: " + formatRupiah(pos.sisa)

                    val posLunas = pos.sisa <= 0 || pos.status.equals("LUNAS", ignoreCase = true)
                    if (posLunas) {
                        tvRowStatusPos.text = "Lunas"
                        tvRowStatusPos.setTextColor(Color.parseColor("#059669"))
                        tvRowStatusPos.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECFDF5"))
                        tvRowSisa.setTextColor(Color.parseColor("#059669"))
                    } else {
                        tvRowStatusPos.text = "Belum Lunas"
                        tvRowStatusPos.setTextColor(Color.parseColor("#DC2626"))
                        tvRowStatusPos.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                        tvRowSisa.setTextColor(Color.parseColor("#DC2626"))
                    }

                    containerPosBreakdown.addView(posRow)
                }

            } else {
                btnQuickCetak.visibility = View.VISIBLE
                tvStatusBadge.visibility = View.GONE
                ivChevron.setImageResource(R.drawable.ic_chevron_down)
                layoutExpandedSection.visibility = View.GONE
            }
        }

        private fun formatRupiah(amount: Long): String {
            return try {
                "Rp " + NumberFormat.getNumberInstance(Locale("id", "ID")).format(amount)
            } catch (e: Exception) {
                "Rp $amount"
            }
        }
    }
}
