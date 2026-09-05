package com.rtekmidev.marijancbt

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.marijancbt.api.RekapKehadiranSiswaItem
import java.util.Locale

@SuppressLint("SetTextI18n", "NotifyDataSetChanged")
class RekapSiswaBinaanAdapter(
    private var listFull: List<RekapKehadiranSiswaItem>,
    private val onCetakClick: (RekapKehadiranSiswaItem) -> Unit
) : RecyclerView.Adapter<RekapSiswaBinaanAdapter.ViewHolder>() {

    private var listFiltered: MutableList<RekapKehadiranSiswaItem> = listFull.toMutableList()
    private var currentFilterType: String = "Semua"
    private var currentSearchQuery: String = ""
    private var isSortByNomorAbsen: Boolean = true

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUrut: TextView = view.findViewById(R.id.tvUrut)
        val tvNamaSiswa: TextView = view.findViewById(R.id.tvNamaSiswa)
        val tvBadgePerhatian: TextView = view.findViewById(R.id.tvBadgePerhatian)
        val tvNisDanGender: TextView = view.findViewById(R.id.tvNisDanGender)
        val btnCetakSiswa: View = view.findViewById(R.id.btnCetakSiswa)

        val tvCountHadir: TextView = view.findViewById(R.id.tvCountHadir)
        val tvCountSakit: TextView = view.findViewById(R.id.tvCountSakit)
        val tvCountIzin: TextView = view.findViewById(R.id.tvCountIzin)
        val tvCountAlpha: TextView = view.findViewById(R.id.tvCountAlpha)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_rekap_siswa_binaan, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listFiltered[position]

        holder.tvUrut.text = (item.nomor_absen.takeIf { it > 0 } ?: (position + 1)).toString()
        holder.tvNamaSiswa.text = item.nama_siswa.uppercase(Locale.ROOT)

        val noInduk = item.nisn ?: item.nis ?: "-"
        val gender = item.jenis_kelamin ?: "Laki-laki"
        holder.tvNisDanGender.text = "NISN: $noInduk • $gender"

        if (item.alpha > 1 || item.is_perhatian) {
            holder.tvBadgePerhatian.visibility = View.VISIBLE
        } else {
            holder.tvBadgePerhatian.visibility = View.GONE
        }

        holder.tvCountHadir.text = item.hadir.toString()
        holder.tvCountSakit.text = item.sakit.toString()
        holder.tvCountIzin.text = item.izin.toString()
        holder.tvCountAlpha.text = item.alpha.toString()

        holder.btnCetakSiswa.setOnClickListener {
            onCetakClick(item)
        }
        holder.itemView.setOnClickListener {
            onCetakClick(item)
        }
    }

    override fun getItemCount(): Int = listFiltered.size

    fun updateData(newList: List<RekapKehadiranSiswaItem>) {
        listFull = newList
        applyFilterAndSort()
    }

    fun filterBySearch(query: String) {
        currentSearchQuery = query
        applyFilterAndSort()
    }

    fun filterByCategory(filterType: String) {
        currentFilterType = filterType
        applyFilterAndSort()
    }

    fun sortBy(byNomorAbsen: Boolean) {
        isSortByNomorAbsen = byNomorAbsen
        applyFilterAndSort()
    }

    private fun applyFilterAndSort() {
        val q = currentSearchQuery.lowercase(Locale.ROOT).trim()

        var result = listFull.filter { item ->
            val matchQuery = q.isEmpty() ||
                item.nama_siswa.lowercase(Locale.ROOT).contains(q) ||
                (item.nisn?.lowercase(Locale.ROOT)?.contains(q) == true) ||
                (item.nis?.lowercase(Locale.ROOT)?.contains(q) == true)

            if (!matchQuery) return@filter false

            when (currentFilterType) {
                "Alpha > 1" -> item.alpha > 1
                "100% Hadir" -> item.alpha == 0 && item.sakit == 0 && item.izin == 0 && item.hadir > 0
                "Sakit/Izin" -> item.sakit > 0 || item.izin > 0
                else -> true
            }
        }

        result = if (isSortByNomorAbsen) {
            result.sortedBy { it.nomor_absen }
        } else {
            result.sortedBy { it.nama_siswa }
        }

        listFiltered.clear()
        listFiltered.addAll(result)
        notifyDataSetChanged()
    }
}
