package com.rtekmidev.marijancbt

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rtekmidev.marijancbt.api.SiswaJurnal

@android.annotation.SuppressLint("SetTextI18n", "NotifyDataSetChanged")
class PresensiSiswaAdapter(
    private var listSiswa: List<SiswaJurnal>
) : RecyclerView.Adapter<PresensiSiswaAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNamaSiswa: TextView = view.findViewById(R.id.tvNamaSiswa)
        val tvNis: TextView = view.findViewById(R.id.tvNis)
        val rgStatusAbsen: RadioGroup = view.findViewById(R.id.rgStatusAbsen)
        val rbHadir: RadioButton = view.findViewById(R.id.rbHadir)
        val rbSakit: RadioButton = view.findViewById(R.id.rbSakit)
        val rbIzin: RadioButton = view.findViewById(R.id.rbIzin)
        val rbAlpa: RadioButton = view.findViewById(R.id.rbAlpa)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_siswa_presensi, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val siswa = listSiswa[position]
        holder.tvNamaSiswa.text = siswa.nama_lengkap ?: "-"
        holder.tvNis.text = "NIS: ${siswa.nis ?: "-"}"

        // Remove listener temporarily to avoid trigger during setup
        holder.rgStatusAbsen.setOnCheckedChangeListener(null)

        when (siswa.status_absen?.uppercase()) {
            "H" -> holder.rbHadir.isChecked = true
            "S" -> holder.rbSakit.isChecked = true
            "I" -> holder.rbIzin.isChecked = true
            "A" -> holder.rbAlpa.isChecked = true
            else -> holder.rgStatusAbsen.clearCheck()
        }

        holder.rgStatusAbsen.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbHadir -> siswa.status_absen = "H"
                R.id.rbSakit -> siswa.status_absen = "S"
                R.id.rbIzin -> siswa.status_absen = "I"
                R.id.rbAlpa -> siswa.status_absen = "A"
            }
        }
    }

    override fun getItemCount() = listSiswa.size

    fun setSemuaStatus(status: String) {
        for (siswa in listSiswa) {
            siswa.status_absen = status
        }
        notifyDataSetChanged()
    }

    fun getData(): List<SiswaJurnal> {
        return listSiswa
    }
}
