package com.rtekmidev.marijancbt

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.cardview.widget.CardView
import com.rtekmidev.marijancbt.api.ApiClient
import kotlinx.coroutines.*
import java.text.NumberFormat
import java.util.Locale

class KeuanganFragment : Fragment() {

    private var nisnSiswa = ""
    private var namaSiswa = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.activity_keuangan, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sharedPref = requireActivity().getSharedPreferences("SesiUjian", Context.MODE_PRIVATE)
        nisnSiswa = sharedPref.getString("nisn", "") ?: ""
        namaSiswa = sharedPref.getString("nama", "Siswa") ?: "Siswa"

        view.findViewById<TextView>(R.id.tvNamaSiswaKeuangan).text = namaSiswa
        view.findViewById<ImageView>(R.id.btnBackKeuangan).setOnClickListener { 
            // In a fragment, back button might not just finish(), it might pop backstack or switch tab
            // For now, do nothing or switch to Beranda
        }

        view.findViewById<View>(R.id.scrollViewKeuangan)?.setOnScrollChangeListener { _, _, scrollY, _, oldScrollY ->
            val dy = scrollY - oldScrollY
            if (dy > 12) {
                (activity as? DashboardActivity)?.hideBottomNav()
            } else if (dy < -12 || scrollY <= 10) {
                (activity as? DashboardActivity)?.showBottomNav()
            }
        }

        muatTagihan(view)
    }

    private fun muatTagihan(view: View) {
        val wadah = view.findViewById<LinearLayout>(R.id.wadahTagihan)
        val loading = view.findViewById<LinearLayout>(R.id.loadingKeuangan)
        val tvTotalTunggakan = view.findViewById<TextView>(R.id.tvTotalTunggakanBesar)

        loading.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.getTagihan(nisnSiswa)
                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE
                    if (resp.isSuccessful && resp.body()?.status == true) {
                        var totalTunggakan = 0.0
                        val formatRupiah = NumberFormat.getCurrencyInstance(java.util.Locale.Builder().setLanguage("id").setRegion("ID").build())
                        val listTagihan = resp.body()?.tagihan ?: emptyList()

                        wadah.removeAllViews()

                        if (listTagihan.isEmpty()) {
                            val tvKosong = TextView(requireContext())
                            tvKosong.text = "Tidak ada tagihan bulan ini."
                            tvKosong.setTextColor(Color.parseColor("#6B7280"))
                            wadah.addView(tvKosong)
                            tvTotalTunggakan.text = "Rp 0"
                        } else {
                            listTagihan.forEach { item ->
                                val nominalInt = item.nominal_tagihan?.toDoubleOrNull() ?: 0.0
                                val statusBayar = item.status_bayar ?: "BELUM"

                                if (!statusBayar.equals("LUNAS", true)) {
                                    totalTunggakan += nominalInt
                                }

                                val card = layoutInflater.inflate(R.layout.item_tagihan, null) as CardView
                                card.findViewById<TextView>(R.id.tvKategoriTagihan).text = item.nama_pos ?: "TAGIHAN"
                                card.findViewById<TextView>(R.id.tvNamaTagihan).text = item.nama_pos ?: "Tagihan"
                                card.findViewById<TextView>(R.id.tvNominalTagihan).text = formatRupiah.format(nominalInt).replace(",00", "")

                                val btnBayar = card.findViewById<Button>(R.id.btnBayarTagihan)
                                val badge = card.findViewById<TextView>(R.id.tvBadgeStatus)

                                if (statusBayar.equals("LUNAS", true)) {
                                    badge.text = "LUNAS"
                                    badge.setTextColor(Color.parseColor("#059669"))
                                    badge.setBackgroundColor(Color.parseColor("#D1FAE5"))
                                    btnBayar.visibility = View.GONE
                                } else {
                                    btnBayar.setOnClickListener {
                                        val idTagihan = item.id ?: ""
                                        if (idTagihan.isNotEmpty()) prosesPembayaranTripay(view, idTagihan)
                                    }
                                }
                                wadah.addView(card)
                            }
                            tvTotalTunggakan.text = formatRupiah.format(totalTunggakan).replace(",00", "")
                        }

                        // RENDER RIWAYAT TRANSAKSI
                        val wadahRiwayat = view.findViewById<LinearLayout>(R.id.wadahRiwayat)
                        wadahRiwayat.removeAllViews()
                        val listRiwayat = resp.body()?.riwayat ?: emptyList()

                        if (listRiwayat.isEmpty()) {
                            val tvKosongRiwayat = TextView(requireContext())
                            tvKosongRiwayat.text = "Belum ada riwayat transaksi."
                            tvKosongRiwayat.setTextColor(Color.parseColor("#6B7280"))
                            wadahRiwayat.addView(tvKosongRiwayat)
                        } else {
                            listRiwayat.forEach { riwayat ->
                                val cardHist = layoutInflater.inflate(R.layout.item_riwayat, null) as CardView
                                cardHist.findViewById<TextView>(R.id.tvTanggalRiwayat).text = riwayat.created_at ?: "-"
                                cardHist.findViewById<TextView>(R.id.tvNamaRiwayat).text = riwayat.nama_pos ?: "Pembayaran"
                                cardHist.findViewById<TextView>(R.id.tvMetodeRiwayat).text = riwayat.payment_type ?: "TRIPAY"

                                val nomRiwayat = riwayat.total_bayar?.toDoubleOrNull() ?: 0.0
                                cardHist.findViewById<TextView>(R.id.tvNominalRiwayat).text = formatRupiah.format(nomRiwayat).replace(",00", "")

                                val badge = cardHist.findViewById<TextView>(R.id.tvBadgeRiwayat)
                                val statusTrans = riwayat.status_transaksi ?: "UNPAID"
                                val btnLanjut = cardHist.findViewById<TextView>(R.id.btnLanjutBayar)

                                if (statusTrans.equals("PAID", true) || statusTrans.equals("LUNAS", true)) {
                                    badge.text = "LUNAS"
                                    badge.setTextColor(Color.parseColor("#059669"))
                                    badge.setBackgroundColor(Color.parseColor("#D1FAE5"))
                                } else if (statusTrans.equals("UNPAID", true)) {
                                    badge.text = "MENUNGGU"
                                    badge.setTextColor(Color.parseColor("#D97706"))
                                    badge.setBackgroundColor(Color.parseColor("#FEF3C7"))
                                    if (!riwayat.checkout_url.isNullOrEmpty()) {
                                        btnLanjut.visibility = View.VISIBLE
                                        btnLanjut.setOnClickListener {
                                            val intent = Intent(requireContext(), PaymentActivity::class.java)
                                            intent.putExtra("PAYMENT_URL", riwayat.checkout_url)
                                            startActivity(intent)
                                        }
                                    }
                                } else {
                                    badge.text = statusTrans.uppercase()
                                    badge.setTextColor(Color.parseColor("#DC2626"))
                                    badge.setBackgroundColor(Color.parseColor("#FEE2E2"))
                                }
                                wadahRiwayat.addView(cardHist)
                            }
                        }
                    } else {
                        Toast.makeText(requireContext(), "Gagal meload tagihan.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE
                    Toast.makeText(requireContext(), "Koneksi ke server lambat", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun prosesPembayaranTripay(view: View, idTagihan: String) {
        val loading = view.findViewById<LinearLayout>(R.id.loadingKeuangan)
        loading.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val resp = ApiClient.instance.bayarTagihan(nisnSiswa, idTagihan, "QRIS")
                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE
                    if (resp.isSuccessful && resp.body()?.status == true) {
                        val urlTripay = resp.body()?.checkout_url
                        if (!urlTripay.isNullOrEmpty()) {
                            val intent = Intent(requireContext(), PaymentActivity::class.java)
                            intent.putExtra("PAYMENT_URL", urlTripay)
                            startActivity(intent)
                        }
                    } else {
                        Toast.makeText(requireContext(), "Gagal membuat transaksi", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    loading.visibility = View.GONE
                    Toast.makeText(requireContext(), "Koneksi ke server error", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
