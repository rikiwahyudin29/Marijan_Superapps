package com.rtekmidev.marijansuperapps

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.marijansuperapps.api.SiswaAbsenItem

class SiswaAbsenHarianAdapter(
    private var originalList: List<SiswaAbsenItem>,
    private val onStatusChanged: (SiswaAbsenItem, String) -> Unit
) : RecyclerView.Adapter<SiswaAbsenHarianAdapter.ViewHolder>() {

    var filteredList: MutableList<SiswaAbsenItem> = originalList.toMutableList()
        private set

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNomorAbsen: TextView = view.findViewById(R.id.tvNomorAbsen)
        val tvNamaSiswa: TextView = view.findViewById(R.id.tvNamaSiswa)
        val tvNisDanGender: TextView = view.findViewById(R.id.tvNisDanGender)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)

        val btnPilihHadir: TextView = view.findViewById(R.id.btnPilihHadir)
        val btnPilihSakit: TextView = view.findViewById(R.id.btnPilihSakit)
        val btnPilihIzin: TextView = view.findViewById(R.id.btnPilihIzin)
        val btnPilihAlpha: TextView = view.findViewById(R.id.btnPilihAlpha)

        val llKeteranganContainer: LinearLayout = view.findViewById(R.id.llKeteranganContainer)
        val tvKeteranganText: TextView = view.findViewById(R.id.tvKeteranganText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_siswa_absen_harian, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int = filteredList.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredList[position]

        holder.tvNomorAbsen.text = item.nomor_absen.toString()
        holder.tvNamaSiswa.text = item.nama_lengkap
        val gender = item.jenis_kelamin ?: "Laki-laki"
        val noInduk = item.nisn ?: item.nis ?: "-"
        holder.tvNisDanGender.text = "NISN: $noInduk • $gender"

        // Render Tombol & Status
        updateItemUi(holder, item)

        holder.btnPilihHadir.setOnClickListener {
            if (item.status_kehadiran != "Hadir") {
                item.status_kehadiran = "Hadir"
                updateItemUi(holder, item)
                onStatusChanged(item, "Hadir")
            }
        }

        holder.btnPilihSakit.setOnClickListener {
            if (item.status_kehadiran != "Sakit") {
                item.status_kehadiran = "Sakit"
                updateItemUi(holder, item)
                onStatusChanged(item, "Sakit")
            }
        }

        holder.btnPilihIzin.setOnClickListener {
            if (item.status_kehadiran != "Izin") {
                item.status_kehadiran = "Izin"
                updateItemUi(holder, item)
                onStatusChanged(item, "Izin")
            }
        }

        holder.btnPilihAlpha.setOnClickListener {
            if (item.status_kehadiran != "Alpha") {
                item.status_kehadiran = "Alpha"
                updateItemUi(holder, item)
                onStatusChanged(item, "Alpha")
            }
        }
    }

    private fun updateItemUi(holder: ViewHolder, item: SiswaAbsenItem) {
        val context = holder.itemView.context

        // Reset semua tombol ke inactive
        holder.btnPilihHadir.setBackgroundResource(R.drawable.bg_btn_absen_inactive)
        holder.btnPilihHadir.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

        holder.btnPilihSakit.setBackgroundResource(R.drawable.bg_btn_absen_inactive)
        holder.btnPilihSakit.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

        holder.btnPilihIzin.setBackgroundResource(R.drawable.bg_btn_absen_inactive)
        holder.btnPilihIzin.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

        holder.btnPilihAlpha.setBackgroundResource(R.drawable.bg_btn_absen_inactive)
        holder.btnPilihAlpha.setTextColor(ContextCompat.getColor(context, R.color.text_secondary))

        when (item.status_kehadiran) {
            "Hadir" -> {
                holder.btnPilihHadir.setBackgroundResource(R.drawable.bg_btn_absen_h_active)
                holder.btnPilihHadir.setTextColor(android.graphics.Color.WHITE)

                holder.tvStatusBadge.text = "Hadir"
                holder.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#059669"))
                holder.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#ECFDF5"))
            }
            "Sakit" -> {
                holder.btnPilihSakit.setBackgroundResource(R.drawable.bg_btn_absen_s_active)
                holder.btnPilihSakit.setTextColor(android.graphics.Color.WHITE)

                holder.tvStatusBadge.text = "Sakit"
                holder.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#059669"))
                holder.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#ECFDF5"))
            }
            "Izin" -> {
                holder.btnPilihIzin.setBackgroundResource(R.drawable.bg_btn_absen_i_active)
                holder.btnPilihIzin.setTextColor(android.graphics.Color.WHITE)

                holder.tvStatusBadge.text = "Izin"
                holder.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#D97706"))
                holder.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEF3C7"))
            }
            "Alpha" -> {
                holder.btnPilihAlpha.setBackgroundResource(R.drawable.bg_btn_absen_a_active)
                holder.btnPilihAlpha.setTextColor(android.graphics.Color.WHITE)

                holder.tvStatusBadge.text = "Alpha"
                holder.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#DC2626"))
                holder.tvStatusBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#FEF2F2"))
            }
        }

        // Keterangan jika ada surat izin atau catatan khusus
        if (!item.keterangan.isNullOrEmpty()) {
            holder.llKeteranganContainer.visibility = View.VISIBLE
            holder.tvKeteranganText.text = item.keterangan
        } else {
            holder.llKeteranganContainer.visibility = View.GONE
        }
    }

    fun updateData(newList: List<SiswaAbsenItem>) {
        originalList = newList
        filteredList = newList.toMutableList()
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        val q = query.trim().lowercase()
        filteredList = if (q.isEmpty()) {
            originalList.toMutableList()
        } else {
            originalList.filter {
                it.nama_lengkap.lowercase().contains(q) || 
                (it.nisn ?: "").lowercase().contains(q) || 
                (it.nis ?: "").lowercase().contains(q)
            }.toMutableList()
        }
        notifyDataSetChanged()
    }

    fun setAllStatus(newStatus: String) {
        for (item in originalList) {
            item.status_kehadiran = newStatus
        }
        notifyDataSetChanged()
    }

    fun sortBy(sortByNomorAbsen: Boolean) {
        if (sortByNomorAbsen) {
            filteredList.sortBy { it.nomor_absen }
        } else {
            filteredList.sortBy { it.nama_lengkap.lowercase() }
        }
        notifyDataSetChanged()
    }
}
