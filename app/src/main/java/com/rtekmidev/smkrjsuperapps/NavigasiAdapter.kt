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
        var bgColor = Color.parseColor("#F1F5F9") // Abu-abu muda (Belum dijawab)
        var textColor = Color.parseColor("#334155")
        var strokeColor = Color.parseColor("#CBD5E1")
        var strokeWidth = 2

        if (jawaban.isNotEmpty()) {
            bgColor = Color.parseColor("#1E1B4B") // Navy Gelap (Sudah dijawab)
            textColor = Color.WHITE
            strokeColor = Color.parseColor("#1E1B4B")
        }
        if (isRagu) {
            bgColor = Color.parseColor("#F59E0B") // Amber / Orange (Ragu-ragu)
            textColor = Color.WHITE
            strokeColor = Color.parseColor("#D97706")
        }

        // Outline tebal untuk soal yang sedang aktif dibuka
        if (position == currentIndex) {
            strokeColor = Color.parseColor("#2563EB")
            strokeWidth = 6
        }

        val shape = GradientDrawable()
        shape.shape = GradientDrawable.RECTANGLE
        shape.cornerRadius = 16f
        shape.setColor(bgColor)
        shape.setStroke(strokeWidth, strokeColor)

        textView.background = shape
        textView.setTextColor(textColor)

        return textView
    }
}
