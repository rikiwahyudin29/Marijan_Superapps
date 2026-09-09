package com.rtekmidev.smkrjsuperapps

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.core.text.HtmlCompat
import androidx.recyclerview.widget.RecyclerView

class MateriAdapter(
    private var items: List<MateriBelajarActivity.DisplayItem>,
    private val onItemClick: (MateriBelajarActivity.DisplayItem) -> Unit
) : RecyclerView.Adapter<MateriAdapter.MateriViewHolder>() {

    inner class MateriViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMapel: TextView = itemView.findViewById(R.id.tvMapel)
        val tvFormatBadge: TextView = itemView.findViewById(R.id.tvFormatBadge)
        val tvJudulMateri: TextView = itemView.findViewById(R.id.tvJudulMateri)
        val tvGuru: TextView = itemView.findViewById(R.id.tvGuru)
        val tvTanggal: TextView = itemView.findViewById(R.id.tvTanggal)
        val tvDeskripsi: TextView = itemView.findViewById(R.id.tvDeskripsi)
        val ivIconJenis: ImageView = itemView.findViewById(R.id.ivIconJenis)
        val tvFileInfo: TextView = itemView.findViewById(R.id.tvFileInfo)
        val btnAksiMateri: Button = itemView.findViewById(R.id.btnAksiMateri)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MateriViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_materi, parent, false)
        return MateriViewHolder(view)
    }

    override fun onBindViewHolder(holder: MateriViewHolder, position: Int) {
        val item = items[position]

        holder.tvMapel.text = item.mapel
        holder.tvJudulMateri.text = item.judul
        holder.tvGuru.text = if (item.guru.isNotBlank() && item.guru != "-") "Oleh: ${item.guru}" else "Guru Mapel"
        holder.tvTanggal.text = if (item.waktu.isNotBlank() && item.waktu != "-") item.waktu else "Terbaru"

        val cleanDesc = HtmlCompat.fromHtml(item.deskripsi ?: "", HtmlCompat.FROM_HTML_MODE_COMPACT).toString().trim()
        if (cleanDesc.isNotEmpty() && cleanDesc != "null") {
            holder.tvDeskripsi.text = cleanDesc
            holder.tvDeskripsi.visibility = View.VISIBLE
        } else {
            holder.tvDeskripsi.visibility = View.GONE
        }

        val jenis = item.jenisFile?.lowercase() ?: ""
        val urlLower = item.urlFile?.lowercase() ?: ""
        val isVideo = jenis == "youtube" || jenis == "video" || !item.linkYoutube.isNullOrEmpty()

        if (isVideo) {
            holder.tvFormatBadge.text = "VIDEO"
            holder.tvFormatBadge.setBackgroundResource(R.drawable.bg_badge_amber_soft)
            holder.tvFormatBadge.setTextColor(Color.parseColor("#D97706"))

            holder.ivIconJenis.setImageResource(R.drawable.ic_modern_clock)
            holder.ivIconJenis.imageTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
            holder.tvFileInfo.text = "Video Pembelajaran Online"

            holder.btnAksiMateri.text = "Tonton Video"
            holder.btnAksiMateri.setBackgroundResource(R.drawable.bg_btn_light_amber)
            holder.btnAksiMateri.backgroundTintList = null
            holder.btnAksiMateri.setTextColor(Color.parseColor("#B45309"))
            holder.btnAksiMateri.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search_24, 0, 0, 0)
            holder.btnAksiMateri.compoundDrawableTintList = ColorStateList.valueOf(Color.parseColor("#B45309"))
        } else {
            when {
                urlLower.endsWith(".pdf") || jenis.contains("pdf") -> {
                    holder.tvFormatBadge.text = "PDF"
                    holder.tvFormatBadge.setBackgroundResource(R.drawable.bg_badge_red_soft)
                    holder.tvFormatBadge.setTextColor(Color.parseColor("#E11D48"))
                    holder.ivIconJenis.setImageResource(R.drawable.ic_pdf_document)
                    holder.ivIconJenis.imageTintList = ColorStateList.valueOf(Color.parseColor("#E11D48"))
                }
                urlLower.endsWith(".doc") || urlLower.endsWith(".docx") || jenis.contains("word") -> {
                    holder.tvFormatBadge.text = "WORD"
                    holder.tvFormatBadge.setBackgroundResource(R.drawable.bg_badge_blue_soft)
                    holder.tvFormatBadge.setTextColor(Color.parseColor("#2563EB"))
                    holder.ivIconJenis.setImageResource(R.drawable.ic_word_document)
                    holder.ivIconJenis.imageTintList = ColorStateList.valueOf(Color.parseColor("#2563EB"))
                }
                else -> {
                    val label = if (item.jenisFile.isNullOrBlank()) "MODUL" else item.jenisFile.uppercase()
                    holder.tvFormatBadge.text = label
                    holder.tvFormatBadge.setBackgroundResource(R.drawable.bg_badge_blue_soft)
                    holder.tvFormatBadge.setTextColor(Color.parseColor("#1E3A8A"))
                    holder.ivIconJenis.setImageResource(R.drawable.ic_pdf_document)
                    holder.ivIconJenis.imageTintList = ColorStateList.valueOf(Color.parseColor("#1E3A8A"))
                }
            }

            val sizeInfo = if (!item.ukuranFile.isNullOrBlank()) " • ${item.ukuranFile}" else ""
            holder.tvFileInfo.text = "Dokumen ${holder.tvFormatBadge.text}$sizeInfo"

            holder.btnAksiMateri.text = "Buka Materi"
            holder.btnAksiMateri.setBackgroundResource(R.drawable.bg_button_dark_rounded)
            holder.btnAksiMateri.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1A1B41"))
            holder.btnAksiMateri.setTextColor(Color.WHITE)
            holder.btnAksiMateri.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search_24, 0, 0, 0)
            holder.btnAksiMateri.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
        }

        holder.btnAksiMateri.setOnClickListener { onItemClick(item) }
        holder.itemView.setOnClickListener { onItemClick(item) }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<MateriBelajarActivity.DisplayItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
