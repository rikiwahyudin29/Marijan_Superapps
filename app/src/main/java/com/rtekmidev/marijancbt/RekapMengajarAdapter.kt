package com.rtekmidev.marijancbt

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.marijancbt.api.RekapJurnalItem
import java.text.SimpleDateFormat
import java.util.Locale

class RekapMengajarAdapter(
    private val context: Context,
    private var listData: List<RekapJurnalItem>,
    private val onActionClick: (RekapJurnalItem) -> Unit
) : RecyclerView.Adapter<RekapMengajarAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvDateCircle: TextView = view.findViewById(R.id.tvDateCircle)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvJam: TextView = view.findViewById(R.id.tvJam)
        val llBadge: LinearLayout = view.findViewById(R.id.llBadge)
        val ivBadgeIcon: ImageView = view.findViewById(R.id.ivBadgeIcon)
        val tvBadgeText: TextView = view.findViewById(R.id.tvBadgeText)
        val tvMapel: TextView = view.findViewById(R.id.tvMapel)
        val tvKelas: TextView = view.findViewById(R.id.tvKelas)
        val tvMateri: TextView = view.findViewById(R.id.tvMateri)
        val ivWarning: ImageView = view.findViewById(R.id.ivWarning)
        val llPresensi: LinearLayout = view.findViewById(R.id.llPresensi)
        val tvPresensi: TextView = view.findViewById(R.id.tvPresensi)
        val btnAction: LinearLayout = view.findViewById(R.id.btnAction)
        val tvActionText: TextView = view.findViewById(R.id.tvActionText)
        val llContainer: LinearLayout = view.findViewById(R.id.llContainer)
        val llFooter: LinearLayout = view.findViewById(R.id.llFooter)
        val ivActionIcon: ImageView = view.findViewById(R.id.ivActionIcon)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_rekap_mengajar, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listData[position]

        // Parse date for circle
        try {
            val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(item.tanggal ?: "")
            if (date != null) {
                val cal = java.util.Calendar.getInstance()
                cal.time = date
                holder.tvDateCircle.text = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()
                
                // Format full date: Kamis, 03 September 2026
                val fullDate = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID")).format(date)
                holder.tvTanggal.text = fullDate
            }
        } catch (e: Exception) {
            holder.tvTanggal.text = item.tanggal
        }

        holder.tvJam.text = "Jam Ke: ${item.jam_ke}"
        holder.tvMapel.text = item.nama_mapel
        holder.tvKelas.text = item.nama_kelas
        
        // Reset styles first
        holder.llContainer.setBackgroundResource(R.drawable.bg_rounded_border_outline)
        holder.btnAction.visibility = View.VISIBLE
        holder.llFooter.visibility = View.VISIBLE
        holder.ivWarning.visibility = View.GONE
        holder.llPresensi.visibility = View.VISIBLE

        when (item.status) {
            "Terisi" -> {
                holder.tvBadgeText.text = "Terisi"
                holder.tvBadgeText.setTextColor(Color.parseColor("#0d9488"))
                holder.llBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#ccfbf1"))
                holder.ivBadgeIcon.setImageResource(android.R.drawable.checkbox_on_background)
                holder.ivBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#0d9488"))
                
                holder.tvMateri.text = item.materi
                holder.tvMateri.setTextColor(Color.parseColor("#1e293b"))
                holder.tvPresensi.text = item.presensi_summary ?: "Siswa Hadir"
                
                holder.btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1E1B4B"))
                holder.tvActionText.text = "Lihat Jurnal"
                holder.ivActionIcon.setImageResource(android.R.drawable.ic_menu_view)
            }
            "Belum Diisi" -> {
                holder.tvBadgeText.text = "Belum Diisi"
                holder.tvBadgeText.setTextColor(Color.parseColor("#ef4444"))
                holder.llBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#fee2e2"))
                holder.ivBadgeIcon.setImageResource(android.R.drawable.stat_notify_error)
                holder.ivBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#ef4444"))
                
                holder.llContainer.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#fef2f2")) // light red outline/bg
                
                holder.tvMateri.text = "Belum diisi oleh pengajar"
                holder.tvMateri.setTextColor(Color.parseColor("#ef4444"))
                holder.ivWarning.visibility = View.VISIBLE
                
                holder.tvPresensi.text = "- Presensi belum terekam -"
                holder.tvPresensi.setTextColor(Color.parseColor("#94a3b8"))
                holder.tvPresensi.textSize = 10f
                
                holder.btnAction.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#1E1B4B"))
                holder.tvActionText.text = "Isi Jurnal Sekarang"
                holder.ivActionIcon.setImageResource(android.R.drawable.ic_menu_edit)
            }
            else -> { // Mendatang
                holder.tvBadgeText.text = "Mendatang"
                holder.tvBadgeText.setTextColor(Color.parseColor("#64748b"))
                holder.llBadge.backgroundTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#f1f5f9"))
                holder.ivBadgeIcon.setImageResource(android.R.drawable.ic_menu_recent_history)
                holder.ivBadgeIcon.imageTintList = android.content.res.ColorStateList.valueOf(Color.parseColor("#64748b"))
                
                holder.tvMateri.text = "Jadwal Mendatang"
                holder.tvMateri.setTextColor(Color.parseColor("#94a3b8"))
                
                holder.llPresensi.visibility = View.GONE
                holder.btnAction.visibility = View.GONE
                
                // Set "KBM Belum Mulai" to the right of footer if we want, or just hide action.
            }
        }
        
        holder.btnAction.setOnClickListener {
            onActionClick(item)
        }
    }

    override fun getItemCount(): Int = listData.size

    fun updateData(newData: List<RekapJurnalItem>) {
        listData = newData
        notifyDataSetChanged()
    }
}
