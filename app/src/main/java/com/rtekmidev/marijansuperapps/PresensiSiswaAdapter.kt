package com.rtekmidev.marijansuperapps

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.marijansuperapps.api.SiswaJurnal
import java.util.Locale

@android.annotation.SuppressLint("SetTextI18n", "NotifyDataSetChanged")
class PresensiSiswaAdapter(
    private var listSiswaFull: List<SiswaJurnal>,
    private val onStatusChanged: () -> Unit
) : RecyclerView.Adapter<PresensiSiswaAdapter.ViewHolder>() {

    private var listSiswa = listSiswaFull.toList()

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvUrut: TextView = view.findViewById(R.id.tvUrut)
        val tvNamaSiswa: TextView = view.findViewById(R.id.tvNamaSiswa)
        val tvNisSiswa: TextView = view.findViewById(R.id.tvNisSiswa)
        val tvGender: TextView = view.findViewById(R.id.tvGender)
        val tvStatusBadge: TextView = view.findViewById(R.id.tvStatusBadge)
        
        val btnHadir: Button = view.findViewById(R.id.btnHadir)
        val btnSakit: Button = view.findViewById(R.id.btnSakit)
        val btnIzin: Button = view.findViewById(R.id.btnIzin)
        val btnAlpa: Button = view.findViewById(R.id.btnAlpa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_siswa_presensi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val siswa = listSiswa[position]
        
        // Data binding
        holder.tvUrut.text = (position + 1).toString()
        holder.tvNamaSiswa.text = siswa.nama_lengkap?.uppercase(Locale.ROOT) ?: "-"
        val noInduk = siswa.nisn ?: siswa.nis ?: "-"
        holder.tvNisSiswa.text = "NISN: $noInduk"
        holder.tvGender.text = siswa.jenis_kelamin ?: "Laki-laki"

        // Setup Buttons and Badge based on status
        updateButtonStyles(holder, siswa.status_absen)

        // Click Listeners
        holder.btnHadir.setOnClickListener {
            siswa.status_absen = "H"
            updateButtonStyles(holder, "H")
            onStatusChanged()
        }
        holder.btnSakit.setOnClickListener {
            siswa.status_absen = "S"
            updateButtonStyles(holder, "S")
            onStatusChanged()
        }
        holder.btnIzin.setOnClickListener {
            siswa.status_absen = "I"
            updateButtonStyles(holder, "I")
            onStatusChanged()
        }
        holder.btnAlpa.setOnClickListener {
            siswa.status_absen = "A"
            updateButtonStyles(holder, "A")
            onStatusChanged()
        }
    }

    private fun updateButtonStyles(holder: ViewHolder, status: String?) {
        val ctx = holder.itemView.context
        
        // Reset all buttons to default outline style
        val defaultBg = ContextCompat.getColorStateList(ctx, R.color.bg_light_grey ?: android.R.color.transparent) // Fallbacks used if needed
        val defaultText = Color.parseColor("#475569")
        val defaultBgTint = ColorStateList.valueOf(Color.parseColor("#F8FAFC"))
        
        listOf(holder.btnHadir, holder.btnSakit, holder.btnIzin, holder.btnAlpa).forEach {
            it.setTextColor(defaultText)
            it.backgroundTintList = defaultBgTint
            it.setTypeface(null, android.graphics.Typeface.NORMAL)
        }
        
        // Hide badge by default unless selected
        holder.tvStatusBadge.visibility = View.VISIBLE

        when (status?.uppercase(Locale.ROOT)) {
            "H" -> {
                holder.btnHadir.setTextColor(Color.WHITE)
                holder.btnHadir.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#10B981"))
                holder.btnHadir.setTypeface(null, android.graphics.Typeface.BOLD)
                
                holder.tvStatusBadge.text = "Hadir"
                holder.tvStatusBadge.setTextColor(Color.parseColor("#047857"))
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_green_dark)
            }
            "S" -> {
                holder.btnSakit.setTextColor(Color.WHITE)
                holder.btnSakit.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#059669"))
                holder.btnSakit.setTypeface(null, android.graphics.Typeface.BOLD)
                
                holder.tvStatusBadge.text = "Sakit"
                holder.tvStatusBadge.setTextColor(Color.parseColor("#047857"))
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_light_blue) // Using existing or fallback
            }
            "I" -> {
                holder.btnIzin.setTextColor(Color.WHITE)
                holder.btnIzin.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#F59E0B"))
                holder.btnIzin.setTypeface(null, android.graphics.Typeface.BOLD)
                
                holder.tvStatusBadge.text = "Izin"
                holder.tvStatusBadge.setTextColor(Color.parseColor("#B45309"))
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_yellow_outline)
            }
            "A" -> {
                holder.btnAlpa.setTextColor(Color.WHITE)
                holder.btnAlpa.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
                holder.btnAlpa.setTypeface(null, android.graphics.Typeface.BOLD)
                
                holder.tvStatusBadge.text = "Alpa"
                holder.tvStatusBadge.setTextColor(Color.parseColor("#BE123C"))
                holder.tvStatusBadge.setBackgroundResource(R.drawable.bg_badge_red_outline)
            }
            else -> {
                holder.tvStatusBadge.visibility = View.GONE
            }
        }
    }

    override fun getItemCount() = listSiswa.size

    fun setSemuaStatus(status: String) {
        // Change on full list
        for (siswa in listSiswaFull) {
            siswa.status_absen = status
        }
        notifyDataSetChanged()
        onStatusChanged()
    }
    
    fun filter(query: String) {
        listSiswa = if (query.isEmpty()) {
            listSiswaFull.toList()
        } else {
            val lowerCaseQuery = query.lowercase(Locale.ROOT)
            listSiswaFull.filter {
                (it.nama_lengkap?.lowercase(Locale.ROOT)?.contains(lowerCaseQuery) == true) ||
                (it.nisn?.lowercase(Locale.ROOT)?.contains(lowerCaseQuery) == true) ||
                (it.nis?.lowercase(Locale.ROOT)?.contains(lowerCaseQuery) == true)
            }
        }
        notifyDataSetChanged()
    }

    fun getData(): List<SiswaJurnal> {
        return listSiswaFull
    }
    
    fun updateData(newList: List<SiswaJurnal>) {
        listSiswaFull = newList
        listSiswa = newList.toList()
        notifyDataSetChanged()
        onStatusChanged()
    }
}
