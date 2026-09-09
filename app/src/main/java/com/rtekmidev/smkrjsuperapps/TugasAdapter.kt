package com.rtekmidev.smkrjsuperapps

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.smkrjsuperapps.api.DataTugas

class TugasAdapter(
    private var taskList: List<DataTugas>,
    private val onTaskClick: (DataTugas, String) -> Unit
) : RecyclerView.Adapter<TugasAdapter.TugasViewHolder>() {

    inner class TugasViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvMapel: TextView = itemView.findViewById(R.id.tvMapel)
        val tvStatus: TextView = itemView.findViewById(R.id.tvStatus)
        val tvJudulTugas: TextView = itemView.findViewById(R.id.tvJudulTugas)
        val tvDeadline: TextView = itemView.findViewById(R.id.tvDeadline)
        val btnAksi: Button = itemView.findViewById(R.id.btnAksi)
        
        val layNilaiKomentar: View = itemView.findViewById(R.id.layNilaiKomentar)
        val tvNilai: TextView = itemView.findViewById(R.id.tvNilai)
        val tvKomentar: TextView = itemView.findViewById(R.id.tvKomentar)
        val btnPerbaruiJawaban: Button = itemView.findViewById(R.id.btnPerbaruiJawaban)

        fun bind(task: DataTugas) {
            tvMapel.text = task.mapel ?: "Mata Pelajaran"
            tvJudulTugas.text = task.judul ?: "Judul Tugas"
            
            // Sama seperti MateriTugasActivity, kalau bukan "Belum Selesai" berarti selesai
            val isCompleted = !(task.status?.equals("Belum Selesai", ignoreCase = true) == true)
            
            if (isCompleted) {
                // Selesai
                tvStatus.text = "SUDAH SELESAI"
                tvStatus.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                tvStatus.setBackgroundResource(R.drawable.bg_tag_status_grey)
                
                tvDeadline.text = "Selesai dikerjakan"
                tvDeadline.setTextColor(ContextCompat.getColor(itemView.context, android.R.color.darker_gray))
                tvDeadline.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
                
                layNilaiKomentar.visibility = View.VISIBLE
                tvNilai.text = task.nilai ?: "Belum dinilai"
                tvKomentar.text = task.komentar_guru ?: "Tidak ada komentar"
                btnPerbaruiJawaban.visibility = View.VISIBLE
                
                btnAksi.text = "Lihat Jawaban"
                btnAksi.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.bg_light_grey)
                btnAksi.setTextColor(ContextCompat.getColor(itemView.context, R.color.text_blue))
                
                btnAksi.setOnClickListener {
                    onTaskClick(task, "LihatJawaban")
                }
                btnPerbaruiJawaban.setOnClickListener {
                    onTaskClick(task, "Perbarui")
                }
            } else {
                // Belum Selesai
                tvStatus.text = "Belum Selesai"
                tvStatus.setTextColor(Color.parseColor("#E11D48")) // Red
                tvStatus.setBackgroundResource(R.drawable.bg_tag_status_red)
                
                tvDeadline.text = task.deadline ?: "-"
                tvDeadline.setTextColor(Color.parseColor("#E11D48"))
                tvDeadline.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_modern_tugas, 0, 0, 0)
                
                layNilaiKomentar.visibility = View.GONE
                btnPerbaruiJawaban.visibility = View.GONE
                
                btnAksi.text = "Kerjakan Sekarang"
                btnAksi.backgroundTintList = ContextCompat.getColorStateList(itemView.context, R.color.text_blue)
                btnAksi.setTextColor(ContextCompat.getColor(itemView.context, R.color.card_bg))
                
                btnAksi.setOnClickListener {
                    onTaskClick(task, "Kerjakan")
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TugasViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_tugas, parent, false)
        return TugasViewHolder(view)
    }

    override fun onBindViewHolder(holder: TugasViewHolder, position: Int) {
        holder.bind(taskList[position])
    }

    override fun getItemCount(): Int = taskList.size
    
    fun updateData(newList: List<DataTugas>) {
        taskList = newList
        notifyDataSetChanged()
    }
}

