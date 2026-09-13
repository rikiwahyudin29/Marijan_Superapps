package com.rtekmidev.marijansuperapps

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import com.rtekmidev.marijansuperapps.api.ApiClient
import kotlinx.coroutines.*
import java.text.NumberFormat
import java.util.Locale

class KeuanganActivity : AppCompatActivity() {

    private var nisnSiswa = ""
    private var namaSiswa = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_keuangan)

        val sharedPref = getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisnSiswa = sharedPref.getString("nisn", "") ?: ""
        namaSiswa = sharedPref.getString("nama", "Siswa") ?: "Siswa"

        findViewById<TextView>(R.id.tvNamaSiswaKeuangan)?.text = namaSiswa

        muatTagihan()
    }

    private fun muatTagihan() {
        val wadah = findViewById<LinearLayout>(R.id.wadahTagihan)
        val loading = findViewById<LinearLayout>(R.id.loadingKeuangan)
        val tvTotalTunggakan = findViewById<TextView>(R.id.tvStatSisaTunggakan)

        loading.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.getTagihan(nisnSiswa)
                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE
                    wadah.removeAllViews()

                    if (resp.isSuccessful && resp.body()?.status == true) {
                        val listTagihan = resp.body()?.tagihan ?: emptyList()
                        val formatRupiah = NumberFormat.getCurrencyInstance(java.util.Locale.Builder().setLanguage("id").setRegion("ID").build())

                        var totalTunggakan = 0.0

                        if (listTagihan.isEmpty()) {
                            val tvKosong = TextView(this@KeuanganActivity)
                            tvKosong.text = "Tidak ada tagihan aktif."
                            tvKosong.setTextColor(Color.parseColor("#6B7280"))
                            wadah.addView(tvKosong)
                            tvTotalTunggakan.text = "Rp 0"
                            // ==========================================
                            // RENDER RIWAYAT TRANSAKSI
                            // ==========================================
                            val wadahRiwayat = findViewById<LinearLayout>(R.id.wadahRiwayat)
                            wadahRiwayat.removeAllViews()

                            val listRiwayat = resp.body()?.riwayat ?: emptyList()

                            if (listRiwayat.isEmpty()) {
                                val tvKosongRiwayat = TextView(this@KeuanganActivity)
                                tvKosongRiwayat.text = "Belum ada riwayat transaksi."
                                tvKosongRiwayat.setTextColor(Color.parseColor("#6B7280"))
                                wadahRiwayat.addView(tvKosongRiwayat)
                            } else {
                                listRiwayat.forEach { riwayat ->
                                    val cardHist = layoutInflater.inflate(R.layout.item_riwayat, null) as CardView

                                    cardHist.findViewById<TextView>(R.id.tvTanggalRiwayat).text = riwayat.created_at ?: "-"
                                    cardHist.findViewById<TextView>(R.id.tvNamaRiwayat).text = riwayat.nama_pos ?: "Pembayaran"
                                    cardHist.findViewById<TextView>(R.id.tvMetodeRiwayat).text = riwayat.payment_type ?: "TRIPAY"

                                    val nomRiwayat = riwayat.jumlah_bayar?.toDoubleOrNull()
                                        ?: riwayat.total_bayar?.toDoubleOrNull()
                                        ?: 0.0
                                    cardHist.findViewById<TextView>(R.id.tvNominalRiwayat).text = formatRupiah.format(nomRiwayat).replace(",00", "")

                                    val badge = cardHist.findViewById<TextView>(R.id.tvBadgeRiwayat)
                                    val statusTagihan = riwayat.status_tagihan ?: riwayat.status_bayar ?: "LUNAS"
                                    val tvKodeTrx = cardHist.findViewById<TextView>(R.id.tvKodeTrx)
                                    val btnKwitansi = cardHist.findViewById<View>(R.id.btnDownloadKwitansi)

                                    tvKodeTrx?.text = riwayat.kode_transaksi ?: "-"
                                    if (statusTagihan.equals("CICIL", true)) {
                                        badge?.text = "CICIL"
                                        badge?.setTextColor(Color.parseColor("#D97706"))
                                        badge?.setBackgroundColor(Color.parseColor("#FEF3C7"))
                                    } else {
                                        badge?.text = "LUNAS"
                                        badge?.setTextColor(Color.parseColor("#059669"))
                                        badge?.setBackgroundColor(Color.parseColor("#D1FAE5"))
                                    }

                                    btnKwitansi?.setOnClickListener {
                                        val url = riwayat.kwitansi_url
                                        if (!url.isNullOrEmpty()) {
                                            try {
                                                val i = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                                startActivity(i)
                                            } catch (e: Exception) {
                                                Toast.makeText(this@KeuanganActivity, "Tidak dapat membuka browser", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            Toast.makeText(this@KeuanganActivity, "Kwitansi belum tersedia", Toast.LENGTH_SHORT).show()
                                        }
                                    }

                                    wadahRiwayat.addView(cardHist)
                                }
                            }
                            return@withContext
                        }

                        listTagihan.forEach { item ->
                            val nominalInt = item.nominal_tagihan?.toDoubleOrNull() ?: 0.0
                            val statusBayar = item.status_bayar ?: "BELUM"

                            // Jumlahkan total jika belum lunas
                            if (!statusBayar.equals("LUNAS", true)) {
                                totalTunggakan += nominalInt
                            }

                            val card = layoutInflater.inflate(R.layout.item_tagihan, null) as CardView

                            card.findViewById<TextView>(R.id.tvNamaTagihan)?.text = item.nama_pos ?: "Tagihan"
                            card.findViewById<TextView>(R.id.tvNominalTagihan)?.text = formatRupiah.format(nominalInt).replace(",00", "")

                            val btnBayar = card.findViewById<View>(R.id.btnBayarTagihan)
                            val badge = card.findViewById<TextView>(R.id.tvBadgeStatus)

                            if (statusBayar.equals("LUNAS", true)) {
                                badge.text = "LUNAS"
                                badge.setTextColor(Color.parseColor("#059669")) // Emerald 600
                                badge.setBackgroundColor(Color.parseColor("#D1FAE5")) // Emerald 100

                                btnBayar.visibility = View.GONE
                            } else {
                                btnBayar.setOnClickListener {
                                    val idTagihan = item.id ?: ""
                                    if (idTagihan.isNotEmpty()) prosesPembayaranTripay(idTagihan)
                                }
                            }
                            wadah.addView(card)
                        }

                        // Set teks total tunggakan di Kartu Biru
                        tvTotalTunggakan.text = formatRupiah.format(totalTunggakan).replace(",00", "")

                    } else {
                        Toast.makeText(this@KeuanganActivity, "Gagal meload tagihan.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE
                    Toast.makeText(this@KeuanganActivity, "Koneksi ke server lambat", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun prosesPembayaranTripay(idTagihan: String) {
        val loading = findViewById<LinearLayout>(R.id.loadingKeuangan)
        loading.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.bayarTagihan(nisnSiswa, idTagihan, "QRIS")
                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE
                    if (resp.isSuccessful && resp.body()?.status == true) {
                        val urlTripay = resp.body()?.checkout_url
                        if (!urlTripay.isNullOrEmpty()) {
                            val intent = Intent(this@KeuanganActivity, PaymentActivity::class.java)
                            intent.putExtra("PAYMENT_URL", urlTripay)
                            startActivity(intent)
                        }
                    } else {
                        Toast.makeText(this@KeuanganActivity, "Gagal membuat transaksi", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE
                    Toast.makeText(this@KeuanganActivity, "Koneksi ke server error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
