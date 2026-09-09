package com.rtekmidev.smkrjsuperapps

import android.content.Context
import android.content.Intent
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
import com.bumptech.glide.Glide
import com.rtekmidev.smkrjsuperapps.api.RekapJurnalItem
import java.text.SimpleDateFormat
import java.util.Locale

class RekapMengajarAdapter(
    private val context: Context,
    private var listData: List<RekapJurnalItem>,
    private val onActionClick: (RekapJurnalItem) -> Unit
) : RecyclerView.Adapter<RekapMengajarAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cvMain: CardView = view.findViewById(R.id.cvMain)
        val tvDateCircle: TextView = view.findViewById(R.id.tvDateCircle)
        val tvTanggal: TextView = view.findViewById(R.id.tvTanggal)
        val tvJam: TextView = view.findViewById(R.id.tvJam)
        val llBadge: LinearLayout = view.findViewById(R.id.llBadge)
        val ivBadgeIcon: ImageView = view.findViewById(R.id.ivBadgeIcon)
        val tvBadgeText: TextView = view.findViewById(R.id.tvBadgeText)
        val tvMapel: TextView = view.findViewById(R.id.tvMapel)
        val tvKelas: TextView = view.findViewById(R.id.tvKelas)
        val llMateri: LinearLayout = view.findViewById(R.id.llMateri)
        val tvMateri: TextView = view.findViewById(R.id.tvMateri)
        val ivWarning: ImageView = view.findViewById(R.id.ivWarning)
        val llPresensi: LinearLayout = view.findViewById(R.id.llPresensi)
        val tvPresensi: TextView = view.findViewById(R.id.tvPresensi)
        val btnAction: LinearLayout = view.findViewById(R.id.btnAction)
        val tvActionText: TextView = view.findViewById(R.id.tvActionText)
        val llContainer: LinearLayout = view.findViewById(R.id.llContainer)
        val llFooter: LinearLayout = view.findViewById(R.id.llFooter)
        val ivActionIcon: ImageView = view.findViewById(R.id.ivActionIcon)
        
        val cvFotoKegiatan: View = view.findViewById(R.id.cvFotoKegiatan)
        val ivFotoKegiatan: ImageView = view.findViewById(R.id.ivFotoKegiatan)
        val llAlfa: LinearLayout = view.findViewById(R.id.llAlfa)
        val tvAlfaTitle: TextView = view.findViewById(R.id.tvAlfaTitle)
        val tvAlfaNames: TextView = view.findViewById(R.id.tvAlfaNames)
    }

    companion object {
        private val isoFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        private val displayDateFormat = SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("id", "ID"))
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_rekap_mengajar, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = listData[position]

        // Parse date for circle using cached formatter
        try {
            val dateStr = item.tanggal
            if (!dateStr.isNullOrEmpty()) {
                val date = synchronized(isoFormat) { isoFormat.parse(dateStr) }
                if (date != null) {
                    val cal = java.util.Calendar.getInstance()
                    cal.time = date
                    holder.tvDateCircle.text = cal.get(java.util.Calendar.DAY_OF_MONTH).toString()
                    
                    val fullDate = synchronized(displayDateFormat) { displayDateFormat.format(date) }
                    holder.tvTanggal.text = fullDate
                } else {
                    holder.tvTanggal.text = dateStr
                }
            }
        } catch (e: Exception) {
            holder.tvTanggal.text = item.tanggal
        }

        holder.tvJam.text = "Jam Ke: ${item.jam_ke}"
        holder.tvMapel.text = item.nama_mapel
        holder.tvKelas.text = item.nama_kelas
        
        // Reset base properties
        holder.llContainer.backgroundTintList = null
        holder.llFooter.visibility = View.VISIBLE
        holder.ivWarning.visibility = View.GONE
        holder.llPresensi.visibility = View.VISIBLE

        when (item.status) {
            "Terisi" -> {
                // Card Hijau (KBM Terisi)
                holder.cvMain.setCardBackgroundColor(Color.parseColor("#F0FDF4"))
                holder.llContainer.setBackgroundResource(R.drawable.bg_card_rekap_terisi)
                
                holder.tvDateCircle.setTextColor(Color.parseColor("#15803D"))
                holder.tvDateCircle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))

                holder.tvBadgeText.text = "Terisi"
                holder.tvBadgeText.setTextColor(Color.parseColor("#15803D"))
                holder.llBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DCFCE7"))
                holder.ivBadgeIcon.setImageResource(android.R.drawable.checkbox_on_background)
                holder.ivBadgeIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#15803D"))
                
                holder.llMateri.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
                holder.tvMateri.text = item.materi
                holder.tvMateri.setTextColor(Color.parseColor("#1E293B"))
                holder.ivWarning.visibility = View.GONE
                
                holder.tvPresensi.text = item.presensi_summary ?: "Siswa Hadir"
                holder.tvPresensi.setTextColor(Color.parseColor("#15803D"))
                holder.tvPresensi.textSize = 11f
                
                // Tombol "Lihat Jurnal" dihapus sesuai permintaan
                holder.btnAction.visibility = View.GONE

                // Lampirkan Foto KBM jika ada
                if (!item.foto_kegiatan.isNullOrEmpty()) {
                    holder.cvFotoKegiatan.visibility = View.VISIBLE
                    Glide.with(context)
                        .load(item.foto_kegiatan)
                        .placeholder(android.R.drawable.ic_menu_gallery)
                        .error(android.R.drawable.ic_menu_gallery)
                        .centerCrop()
                        .into(holder.ivFotoKegiatan)

                    holder.cvFotoKegiatan.setOnClickListener {
                        val viewerIntent = Intent(context, FileViewerActivity::class.java)
                        viewerIntent.putExtra("FILE_URL", item.foto_kegiatan)
                        viewerIntent.putExtra("TITLE", "Bukti Foto KBM - ${item.nama_kelas}")
                        context.startActivity(viewerIntent)
                    }
                } else {
                    holder.cvFotoKegiatan.visibility = View.GONE
                }

                // Detail Siswa Alfa
                val hasAlfa = (!item.alfa_names.isNullOrEmpty()) || (item.total_alfa ?: 0) > 0 || (item.siswa_alfa != null && item.siswa_alfa.isNotEmpty())
                if (hasAlfa) {
                    holder.llAlfa.visibility = View.VISIBLE
                    val countAlfa = item.total_alfa ?: item.siswa_alfa?.size ?: 0
                    holder.tvAlfaTitle.text = if (countAlfa > 0) "Siswa Alfa ($countAlfa Siswa):" else "Siswa Alfa (Tidak Hadir):"
                    val names = item.alfa_names ?: item.siswa_alfa?.joinToString(", ") ?: "-"
                    holder.tvAlfaNames.text = names
                } else {
                    holder.llAlfa.visibility = View.GONE
                }
            }
            "Belum Diisi" -> {
                // Card Merah (KBM Belum Diisi)
                holder.cvMain.setCardBackgroundColor(Color.parseColor("#FEF2F2"))
                holder.llContainer.setBackgroundResource(R.drawable.bg_card_rekap_belum)
                
                holder.tvDateCircle.setTextColor(Color.parseColor("#DC2626"))
                holder.tvDateCircle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))

                holder.tvBadgeText.text = "Belum Diisi"
                holder.tvBadgeText.setTextColor(Color.parseColor("#DC2626"))
                holder.llBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FEE2E2"))
                holder.ivBadgeIcon.setImageResource(android.R.drawable.stat_notify_error)
                holder.ivBadgeIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#DC2626"))
                
                holder.llMateri.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
                holder.tvMateri.text = "Belum diisi oleh pengajar"
                holder.tvMateri.setTextColor(Color.parseColor("#DC2626"))
                holder.ivWarning.visibility = View.VISIBLE
                holder.ivWarning.imageTintList = ColorStateList.valueOf(Color.parseColor("#DC2626"))
                
                holder.tvPresensi.text = "- Presensi belum terekam -"
                holder.tvPresensi.setTextColor(Color.parseColor("#94A3B8"))
                holder.tvPresensi.textSize = 10f
                
                holder.cvFotoKegiatan.visibility = View.GONE
                holder.llAlfa.visibility = View.GONE

                holder.btnAction.visibility = View.VISIBLE
                holder.btnAction.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#DC2626"))
                holder.tvActionText.text = "Isi Jurnal Sekarang"
                holder.ivActionIcon.setImageResource(android.R.drawable.ic_menu_edit)
            }
            else -> { // Mendatang
                // Card Putih (Jadwal Mendatang)
                holder.cvMain.setCardBackgroundColor(Color.parseColor("#FFFFFF"))
                holder.llContainer.setBackgroundResource(R.drawable.bg_card_rekap_mendatang)
                
                holder.tvDateCircle.setTextColor(Color.parseColor("#64748B"))
                holder.tvDateCircle.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))

                holder.tvBadgeText.text = "Mendatang"
                holder.tvBadgeText.setTextColor(Color.parseColor("#64748B"))
                holder.llBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F1F5F9"))
                holder.ivBadgeIcon.setImageResource(android.R.drawable.ic_menu_recent_history)
                holder.ivBadgeIcon.imageTintList = ColorStateList.valueOf(Color.parseColor("#64748B"))
                
                holder.llMateri.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F8FAFC"))
                holder.tvMateri.text = "Jadwal Mendatang"
                holder.tvMateri.setTextColor(Color.parseColor("#94A3B8"))
                holder.ivWarning.visibility = View.GONE
                
                holder.cvFotoKegiatan.visibility = View.GONE
                holder.llAlfa.visibility = View.GONE
                holder.llPresensi.visibility = View.GONE
                holder.btnAction.visibility = View.GONE
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
