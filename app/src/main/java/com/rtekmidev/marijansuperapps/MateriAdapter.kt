package com.rtekmidev.marijansuperapps

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
    private val onVideoClick: (String) -> Unit,
    private val onFileClick: (MateriBelajarActivity.DisplayItem) -> Unit
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
        val btnTontonVideo: Button = itemView.findViewById(R.id.btnTontonVideo)
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

        val hasVideo = !item.linkYoutube.isNullOrBlank()
        val hasFile = !item.urlFile.isNullOrBlank()
        val jenis = item.jenisFile?.lowercase() ?: ""
        val urlLower = item.urlFile?.lowercase() ?: ""

        // Setup base icon padding
        holder.btnTontonVideo.compoundDrawablePadding = 12
        holder.btnAksiMateri.compoundDrawablePadding = 12

        when {
            // Case 1: Keduanya ada (Video YouTube dan Berkas Dokumen/Gambar)
            hasVideo && hasFile -> {
                holder.tvFormatBadge.text = "VIDEO & BERKAS"
                holder.tvFormatBadge.setBackgroundResource(R.drawable.bg_badge_amber_soft)
                holder.tvFormatBadge.setTextColor(Color.parseColor("#D97706"))

                holder.ivIconJenis.setImageResource(R.drawable.ic_play_video)
                holder.ivIconJenis.imageTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
                holder.tvFileInfo.text = "Video YouTube & Berkas Lampiran"

                // Tombol Video
                holder.btnTontonVideo.visibility = View.VISIBLE
                holder.btnTontonVideo.text = "Video"
                holder.btnTontonVideo.setBackgroundResource(R.drawable.bg_btn_light_amber)
                holder.btnTontonVideo.backgroundTintList = null
                holder.btnTontonVideo.setTextColor(Color.parseColor("#B45309"))
                holder.btnTontonVideo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_video, 0, 0, 0)
                holder.btnTontonVideo.compoundDrawableTintList = ColorStateList.valueOf(Color.parseColor("#B45309"))
                holder.btnTontonVideo.setOnClickListener { onVideoClick(item.linkYoutube!!) }

                // Tombol Berkas
                holder.btnAksiMateri.visibility = View.VISIBLE
                holder.btnAksiMateri.text = "Berkas"
                holder.btnAksiMateri.setBackgroundResource(R.drawable.bg_button_dark_rounded)
                holder.btnAksiMateri.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1A1B41"))
                holder.btnAksiMateri.setTextColor(Color.WHITE)
                holder.btnAksiMateri.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search_24, 0, 0, 0)
                holder.btnAksiMateri.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
                holder.btnAksiMateri.setOnClickListener { onFileClick(item) }

                holder.itemView.setOnClickListener { onFileClick(item) }
            }

            // Case 2: Hanya Video YouTube
            hasVideo -> {
                holder.tvFormatBadge.text = "VIDEO"
                holder.tvFormatBadge.setBackgroundResource(R.drawable.bg_badge_amber_soft)
                holder.tvFormatBadge.setTextColor(Color.parseColor("#D97706"))

                holder.ivIconJenis.setImageResource(R.drawable.ic_play_video)
                holder.ivIconJenis.imageTintList = ColorStateList.valueOf(Color.parseColor("#D97706"))
                holder.tvFileInfo.text = "Video Pembelajaran YouTube"

                holder.btnTontonVideo.visibility = View.VISIBLE
                holder.btnTontonVideo.text = "Tonton Video"
                holder.btnTontonVideo.setBackgroundResource(R.drawable.bg_btn_light_amber)
                holder.btnTontonVideo.backgroundTintList = null
                holder.btnTontonVideo.setTextColor(Color.parseColor("#B45309"))
                holder.btnTontonVideo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_video, 0, 0, 0)
                holder.btnTontonVideo.compoundDrawableTintList = ColorStateList.valueOf(Color.parseColor("#B45309"))
                holder.btnTontonVideo.setOnClickListener { onVideoClick(item.linkYoutube!!) }

                holder.btnAksiMateri.visibility = View.GONE
                holder.itemView.setOnClickListener { onVideoClick(item.linkYoutube!!) }
            }

            // Case 3: Hanya Berkas Dokumen/Gambar
            hasFile -> {
                holder.btnTontonVideo.visibility = View.GONE
                holder.btnAksiMateri.visibility = View.VISIBLE
                holder.btnAksiMateri.text = "Buka Materi"
                holder.btnAksiMateri.setBackgroundResource(R.drawable.bg_button_dark_rounded)
                holder.btnAksiMateri.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#1A1B41"))
                holder.btnAksiMateri.setTextColor(Color.WHITE)
                holder.btnAksiMateri.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_search_24, 0, 0, 0)
                holder.btnAksiMateri.compoundDrawableTintList = ColorStateList.valueOf(Color.WHITE)
                holder.btnAksiMateri.setOnClickListener { onFileClick(item) }

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
                        holder.tvFormatBadge.setTextColor(Color.parseColor("#059669"))
                        holder.ivIconJenis.setImageResource(R.drawable.ic_word_document)
                        holder.ivIconJenis.imageTintList = ColorStateList.valueOf(Color.parseColor("#059669"))
                    }
                    urlLower.endsWith(".png") || urlLower.endsWith(".jpg") || urlLower.endsWith(".jpeg") || urlLower.endsWith(".webp") || jenis.contains("gambar") -> {
                        holder.tvFormatBadge.text = "GAMBAR"
                        holder.tvFormatBadge.setBackgroundResource(R.drawable.bg_badge_purple_soft)
                        holder.tvFormatBadge.setTextColor(Color.parseColor("#7C3AED"))
                        holder.ivIconJenis.setImageResource(R.drawable.ic_search_24)
                        holder.ivIconJenis.imageTintList = ColorStateList.valueOf(Color.parseColor("#7C3AED"))
                    }
                    else -> {
                        val label = if (item.jenisFile.isNullOrBlank()) "MODUL" else item.jenisFile.uppercase()
                        holder.tvFormatBadge.text = label
                        holder.tvFormatBadge.setBackgroundResource(R.drawable.bg_badge_blue_soft)
                        holder.tvFormatBadge.setTextColor(Color.parseColor("#064E3B"))
                        holder.ivIconJenis.setImageResource(R.drawable.ic_pdf_document)
                        holder.ivIconJenis.imageTintList = ColorStateList.valueOf(Color.parseColor("#064E3B"))
                    }
                }

                val sizeInfo = if (!item.ukuranFile.isNullOrBlank()) " • ${item.ukuranFile}" else ""
                holder.tvFileInfo.text = "Dokumen ${holder.tvFormatBadge.text}$sizeInfo"
                holder.itemView.setOnClickListener { onFileClick(item) }
            }

            // Case 4: Materi teks biasa (tanpa video & file)
            else -> {
                holder.tvFormatBadge.text = "CATATAN"
                holder.tvFormatBadge.setBackgroundResource(R.drawable.bg_badge_blue_soft)
                holder.tvFormatBadge.setTextColor(Color.parseColor("#064E3B"))
                holder.ivIconJenis.setImageResource(R.drawable.ic_search_24)
                holder.ivIconJenis.imageTintList = ColorStateList.valueOf(Color.parseColor("#064E3B"))
                holder.tvFileInfo.text = "Materi Teks & Ringkasan"
                holder.btnTontonVideo.visibility = View.GONE
                holder.btnAksiMateri.visibility = View.GONE
                holder.itemView.setOnClickListener(null)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    fun updateData(newItems: List<MateriBelajarActivity.DisplayItem>) {
        items = newItems
        notifyDataSetChanged()
    }
}
