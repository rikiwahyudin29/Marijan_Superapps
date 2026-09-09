package com.rtekmidev.smkrjsuperapps

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.rtekmidev.smkrjsuperapps.api.Soal

class NavigasiAdapter(
    private val context: Context,
    private val daftarSoal: List<Soal>,
    private val dbHelper: DatabaseHelper,
    var currentIndex: Int
) : BaseAdapter() {

    override fun getCount(): Int = daftarSoal.size
    override fun getItem(position: Int): Any = daftarSoal[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        val textView: TextView
        if (convertView == null) {
            textView = TextView(context)
            // Ukuran kotak nomor
            textView.layoutParams = ViewGroup.LayoutParams(130, 130)
            textView.gravity = Gravity.CENTER
            textView.textSize = 16f
            textView.setTypeface(null, android.graphics.Typeface.BOLD)
        } else {
            textView = convertView as TextView
        }

        // Set Nomor Soal
        textView.text = (position + 1).toString()

        val soal = daftarSoal[position]

        // Cek status di SQLite
        val jawaban = dbHelper.getJawabanSiswa(soal.id_soal)
        val isRagu = dbHelper.getRaguStatus(soal.id_soal)

        // Penentuan Warna Kotak
        var bgColor = Color.parseColor("#E0E0E0") // Abu-abu (Belum dijawab)
        var textColor = Color.parseColor("#333333")

        if (jawaban.isNotEmpty()) {
            bgColor = Color.parseColor("#1976D2") // Biru (Sudah dijawab)
            textColor = Color.WHITE
        }
        if (isRagu) {
            bgColor = Color.parseColor("#F57C00") // Orange (Ragu-ragu)
            textColor = Color.WHITE
        }

        // Gambar kotak dinamis (Biar tidak usah bikin file XML drawable lagi)
        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = 12f
        shape.setColor(bgColor)

        // Outline / Border Merah untuk soal yang sedang aktif dibuka
        if (position == currentIndex) {
            shape.setStroke(6, Color.parseColor("#D32F2F"))
        } else {
            shape.setStroke(2, Color.LTGRAY)
        }

        textView.background = shape
        textView.setTextColor(textColor)

        return textView
    }
}
